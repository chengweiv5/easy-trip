package com.yangchengwei.easytrip.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.FrameLayout
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.yangchengwei.easytrip.amap.AmapConsentToken
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private class ShareConsentRevoked : Exception()

/** One attached SDK view at a time. Only the screenshot is retained after cleanup. */
class ShareMapCapture(private val host: FrameLayout, private val consent: AmapConsentToken?) {
    suspend fun capture(day: ShareDay): ShareDayMap {
        if (day.points.isEmpty()) return ShareDayMap(message = "地点位置不全，暂无法展示地图")
        if (consent?.isActive() != true) return ShareDayMap(message = "地图未授权，行程清单已保留")
        val signature = "v3|${day.color}|${day.stops.map { listOf(it.number, it.point, it.leg?.points, it.leg?.schematic) }}"
        val key = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        val cached = File(host.context.cacheDir, "itinerary-maps/$key.png")
        return try {
            val valid = withContext(Dispatchers.IO) {
                val info = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                if (cached.isFile) BitmapFactory.decodeFile(cached.absolutePath, info)
                info.outWidth > 0 && info.outHeight > 0
            }
            consent.validateActive()
            if (valid) return ShareDayMap(cached)
            val captured = withContext(Dispatchers.Main.immediate) {
                withTimeoutOrNull(12_000) { captureTo(day, cached); true } ?: false
            }
            if (captured && consent.isActive()) ShareDayMap(cached)
            else ShareDayMap(message = "地图暂不可用，行程清单已保留")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { android.util.Log.w("ShareMapCapture", "capture failed: ${error.javaClass.simpleName}"); ShareDayMap(message = "地图暂不可用，行程清单已保留") }
    }

    private suspend fun captureTo(day: ShareDay, output: File) = coroutineScope {
        checkNotNull(consent).validateActive()
        while (host.width == 0 || host.height == 0) { ensureActive(); delay(16) }
        val view = TextureMapView(host.context)
        val ready = CompletableDeferred<Unit>()
        val imageReady = CompletableDeferred<Unit>()
        val markerBitmaps = mutableListOf<Bitmap>()
        val lock = Any()
        var alive = true
        var snapshot: Bitmap? = null
        var resumed = false
        val watcher = launch {
            consent.active.first { !consent.isActive(it) }
            throw ShareConsentRevoked()
        }
        try {
            view.onCreate(null)
            host.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            val map = view.map
            map.setOnMapLoadedListener { if (alive) ready.complete(Unit) }
            view.onResume(); resumed = true
            map.uiSettings.apply {
                isZoomControlsEnabled = false; isCompassEnabled = false; isScaleControlsEnabled = false
                setAllGesturesEnabled(false)
            }
            map.isMyLocationEnabled = false
            val density = host.resources.displayMetrics.density
            for (stop in day.stops) stop.leg?.takeIf { it.points.size >= 2 }?.let { leg ->
                map.addPolyline(PolylineOptions().addAll(leg.points.map { LatLng(it.latitude, it.longitude) })
                    .color(day.color).width(3 * density).setDottedLine(leg.schematic))
            }
            val points = (day.points + day.stops.flatMap { it.leg?.points.orEmpty() }).distinct()
            if (points.size == 1) map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(points[0].latitude, points[0].longitude), 14f))
            else map.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().apply {
                points.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build(), host.width, host.height, (36*density).toInt()))
            ready.await()
            ensureActive(); consent.validateActive()
            delay(350)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 11*density
                typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            }
            fun rows(stops: List<ShareStop>) = stops.sortedBy { it.number }.map { it.number }.chunked(4).map { it.joinToString("·") }
            fun center(stops: List<ShareStop>) = LatLng(stops.map { it.point!!.latitude }.average(), stops.map { it.point!!.longitude }.average())
            fun bounds(stops: List<ShareStop>): android.graphics.RectF {
                val position = map.projection.toScreenLocation(center(stops))
                val labels = rows(stops)
                val width = maxOf(labels.maxOf(paint::measureText)+14*density,24*density)
                val height = (labels.size*16+8)*density
                return android.graphics.RectF(position.x-width/2,position.y-height/2,position.x+width/2,position.y+height/2)
            }
            // Merge overlapping number labels in screen space, retaining every visit ordinal.
            val clusters = day.stops.filter { it.point != null }.groupBy { it.point!! }.values.map { it.toList() }.toMutableList()
            var changed = true
            while (changed) {
                changed = false
                outer@ for (i in clusters.indices) for (j in i+1 until clusters.size) {
                    val a = bounds(clusters[i]).apply { inset(-3*density,-3*density) }
                    if (android.graphics.RectF.intersects(a,bounds(clusters[j]))) {
                        clusters[i] = clusters[i]+clusters[j];clusters.removeAt(j);changed=true;break@outer
                    }
                }
            }
            for (stops in clusters) {
                val labels = rows(stops)
                val box = bounds(stops)
                check(box.left >= 0 && box.top >= 0 && box.right <= host.width && box.bottom <= host.height) { "地图点位过于密集" }
                val width = kotlin.math.ceil(box.width()).toInt()
                val height = kotlin.math.ceil(box.height()).toInt()
                val bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
                markerBitmaps += bitmap
                val canvas = Canvas(bitmap)
                canvas.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),12*density,12*density,Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE })
                canvas.drawRoundRect(2*density,2*density,width-2*density,height-2*density,10*density,10*density,Paint(Paint.ANTI_ALIAS_FLAG).apply { color=day.color })
                labels.forEachIndexed { i,row -> canvas.drawText(row,width/2f,(12+i*16)*density-(paint.ascent()+paint.descent())/2,paint) }
                map.addMarker(MarkerOptions().position(center(stops)).anchor(.5f,.5f).icon(BitmapDescriptorFactory.fromBitmap(bitmap)))
            }
            delay(350)
            map.getMapScreenShot(object : AMap.OnMapScreenShotListener {
                override fun onMapScreenShot(bitmap: Bitmap?) = Unit
                override fun onMapScreenShot(bitmap: Bitmap?, status: Int) {
                    synchronized(lock) {
                        if (alive && consent.isActive() && bitmap != null && status == 1 && snapshot == null) {
                            snapshot = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            if (snapshot != null) imageReady.complete(Unit)
                        }
                    }
                }
            })
            imageReady.await()
            withContext(Dispatchers.IO) {
                consent.validateActive()
                output.parentFile?.mkdirs()
                val temp = File(output.parentFile, "${output.name}.${java.util.UUID.randomUUID()}.tmp")
                try {
                    temp.outputStream().use { check(checkNotNull(snapshot).compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    ensureActive(); consent.validateActive()
                    check(temp.renameTo(output))
                } finally { temp.delete() }
            }
        } finally {
            synchronized(lock) { alive = false; snapshot?.recycle() }
            watcher.cancel()
            runCatching { view.map.setOnMapLoadedListener(null) }
            if (resumed) runCatching { view.onPause() }
            host.removeView(view)
            runCatching { view.onDestroy() }
            markerBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        }
    }
}
