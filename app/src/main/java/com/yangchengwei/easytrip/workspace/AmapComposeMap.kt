package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.graphics.toArgb
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimary
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimaryDark
import com.yangchengwei.easytrip.core.ui.theme.EasyTripAccent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import android.content.Context
import android.location.Location
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.graphics.withClip
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.Poi
import com.amap.api.maps.model.PolylineOptions
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.amap.AmapPrivacyGate


data class ViewportCenterOffset(val xPx: Int = 0, val yPx: Int = 0)

sealed interface ViewportCommand {
    data class SinglePoint(
        val point: com.yangchengwei.easytrip.core.model.GeoPoint,
        val zoom: Float,
        val centerOffset: ViewportCenterOffset = ViewportCenterOffset(),
    ) : ViewportCommand
    data class Bounds(
        val points: List<com.yangchengwei.easytrip.core.model.GeoPoint>,
        val safeInsets: MapViewportInsets,
    ) : ViewportCommand
}

data class ViewportRendering(
    val consumedRequestId: Long?,
    val consumedSafeInsets: MapViewportInsets = MapViewportInsets(),
    val command: ViewportCommand?,
)

fun viewportRendering(
    consumedRequestId: Long?,
    request: MapViewportRequest?,
    consumedSafeInsets: MapViewportInsets = MapViewportInsets(),
): ViewportRendering {
    // Drawer geometry changes the safe area, not the user's requested camera position.
    if (request == null || request.id == consumedRequestId) {
        return ViewportRendering(consumedRequestId, consumedSafeInsets, null)
    }
    val command = when (request.points.size) {
        0 -> null
        1 -> ViewportCommand.SinglePoint(
            point = request.points.single(),
            zoom = request.singlePointZoom ?: 15f,
            centerOffset = ViewportCenterOffset(
                xPx = (request.safeInsets.rightPx - request.safeInsets.leftPx) / 2,
                yPx = (request.safeInsets.bottomPx - request.safeInsets.topPx) / 2,
            ),
        )
        else -> ViewportCommand.Bounds(request.points, request.safeInsets)
    }
    return ViewportRendering(request.id, request.safeInsets, command)
}

data class MapLayerRendering(val mapType: Int, val showMapText: Boolean)

data class MapLayerApplicationFailure(val error: Throwable, val retainedLayer: MapLayer)

fun mapLayerRendering(applied: MapLayer?, requested: MapLayer): MapLayerRendering? =
    if (applied == requested) null else when (requested) {
        MapLayer.STANDARD -> MapLayerRendering(AMap.MAP_TYPE_NORMAL, true)
        MapLayer.SATELLITE -> MapLayerRendering(AMap.MAP_TYPE_SATELLITE, false)
        MapLayer.SATELLITE_ROAD -> MapLayerRendering(AMap.MAP_TYPE_SATELLITE, true)
    }

internal class MapLayerApplicationController(
    private val updateSdk: (MapLayerRendering) -> Unit,
) {
    private var appliedLayer: MapLayer? = null

    fun apply(requested: MapLayer): MapLayerApplicationFailure? {
        val rendering = mapLayerRendering(appliedLayer, requested) ?: return null
        return runCatching { updateSdk(rendering) }.fold(
            onSuccess = {
                appliedLayer = requested
                null
            },
            onFailure = { error ->
                val retainedLayer = appliedLayer ?: MapLayer.STANDARD
                val rollbackSucceeded = runCatching {
                    mapLayerRendering(null, retainedLayer)?.let(updateSdk)
                }.isSuccess
                if (!rollbackSucceeded) appliedLayer = null
                MapLayerApplicationFailure(error, retainedLayer)
            },
        )
    }
}

data class MapPoiUi(
    val poiId: String?,
    val name: String,
    val address: String,
    val point: GeoPoint,
)

internal fun Poi.toMapPoiUi(address: String = ""): MapPoiUi? {
    val coordinate = coordinate ?: return null
    return MapPoiUi(
        poiId = poiId?.takeIf(String::isNotBlank),
        name = name.orEmpty(),
        address = address,
        point = GeoPoint(coordinate.latitude, coordinate.longitude),
    )
}

internal fun performUserViewportOperation(
    onUserViewportOperation: () -> Unit,
    operation: () -> Unit,
) {
    onUserViewportOperation()
    operation()
}

internal class MapZoomRequestController(
    initialZoomInRequest: Int = 0,
    initialZoomOutRequest: Int = 0,
) {
    private var consumedZoomInRequest = initialZoomInRequest
    private var consumedZoomOutRequest = initialZoomOutRequest

    fun consumeZoomIn(request: Int): Boolean {
        if (request <= consumedZoomInRequest) return false
        consumedZoomInRequest++
        return true
    }

    fun consumeZoomOut(request: Int): Boolean {
        if (request <= consumedZoomOutRequest) return false
        consumedZoomOutRequest++
        return true
    }
}

internal class MapTouchInteractionDetector(
    private val touchSlop: Float,
    private val onInteraction: () -> Unit,
) {
    private var active = true
    private var armed = false
    private var reported = false
    private var downX = 0f
    private var downY = 0f

    fun onDown(x: Float, y: Float) {
        if (!active) return
        armed = true
        reported = false
        downX = x
        downY = y
    }

    fun onMove(x: Float, y: Float) {
        if (armed && !reported && exceedsTouchSlop(x, y)) reportInteraction()
    }

    fun onPointerDown() {
        if (armed && !reported) reportInteraction()
    }

    fun onUp() = resetSequence()

    fun onCancel() = resetSequence()

    fun dispose() {
        active = false
        resetSequence()
    }

    private fun exceedsTouchSlop(x: Float, y: Float): Boolean =
        (x - downX) * (x - downX) + (y - downY) * (y - downY) > touchSlop * touchSlop

    private fun reportInteraction() {
        reported = true
        onInteraction()
    }

    private fun resetSequence() {
        armed = false
        reported = false
    }
}

internal class MapHostCallbackGuard {
    private var active = true
    private var terminalFailure = false
    private var mapErrorReported = false
    private var readyReported = false
    private var disposalFailureReported = false
    private var renderGeneration = 0L
    private var hostCallback: () -> Unit = {}

    fun beginRender(): Long = ++renderGeneration

    fun updateHostCallback(callback: () -> Unit) {
        hostCallback = callback
    }

    fun dispatchHost() {
        if (active && !terminalFailure) hostCallback()
    }

    fun dispatchHost(callback: () -> Unit) {
        if (active && !terminalFailure) callback()
    }

    fun dispatch(generation: Long, callback: () -> Unit) {
        if (active && !terminalFailure && generation == renderGeneration) callback()
    }

    fun reportReady(generation: Long, callback: () -> Unit) {
        if (!active || terminalFailure || readyReported || generation != renderGeneration) return
        readyReported = true
        callback()
    }

    fun reportError(generation: Long, error: Throwable, callback: (Throwable) -> Unit) {
        if (!active || terminalFailure || readyReported || generation != renderGeneration) return
        terminalFailure = true
        callback(error)
    }

    fun reportHostError(error: Throwable, callback: (Throwable) -> Unit = {}): Boolean {
        if (!active || terminalFailure || readyReported) return false
        terminalFailure = true
        callback(error)
        return true
    }

    fun consumeMapError(): Boolean {
        if (mapErrorReported) return false
        mapErrorReported = true
        return true
    }

    fun reportDisposalError(error: Throwable, callback: (Throwable) -> Unit) {
        if (!active || terminalFailure || readyReported || disposalFailureReported) return
        terminalFailure = true
        disposalFailureReported = true
        mapErrorReported = true
        callback(error)
    }

    fun deactivate() {
        active = false
    }
}

interface AmapMapHost {
    val view: View
    fun setOnReadyListener(listener: (() -> Unit)?) = Unit
    fun canRenderBeforeReady(): Boolean = false
    fun onCreate()
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun zoomIn() = Unit
    fun zoomOut() = Unit
    fun resetNorth() = Unit
    fun setOnBearingChangedListener(listener: ((Float) -> Unit)?) = Unit
    fun showCurrentLocation() = Unit
    fun setVisibleViewportInsets(insets: MapViewportInsets) = Unit
    fun setOnUserGestureListener(listener: (() -> Unit)?) = Unit
    fun render(
        model: MapUiModel,
        layer: MapLayer,
        onMarkerClick: (String) -> Unit,
        onLayerError: (Throwable, MapLayer) -> Unit = { _, _ -> },
    ) = Unit
    fun render(
        model: MapUiModel,
        layer: MapLayer,
        onMarkerClick: (String) -> Unit,
        onMapPoiClick: (MapPoiUi) -> Unit,
        onLayerError: (Throwable, MapLayer) -> Unit = { _, _ -> },
    ) = render(model, layer, onMarkerClick, onLayerError)
}

internal data class NormalizedPoint(val x: Float, val y: Float)

internal val BookmarkGeometry = listOf(
    NormalizedPoint(0.20f, 0.08f),
    NormalizedPoint(0.80f, 0.08f),
    NormalizedPoint(0.80f, 0.92f),
    NormalizedPoint(0.50f, 0.70f),
    NormalizedPoint(0.20f, 0.92f),
)

internal data class MapMarkerRendering(
    val glyph: String,
    val geometry: List<NormalizedPoint> = emptyList(),
    val foregroundColor: Int,
    val backgroundColor: Int,
    val borderColor: Int,
    val borderWidth: Int,
    val solid: Boolean,
    val badgeBackgroundColor: Int = 0x00000000,
    val badgeForegroundColor: Int = 0xFFFFFFFF.toInt(),
)

private const val DEFAULT_MARKER_Z_INDEX = 0f
private const val FOCUSED_MARKER_Z_INDEX = 1f

internal fun markerRenderOrder(markers: List<MapMarkerUi>): List<MapMarkerUi> =
    markers.filterNot(MapMarkerUi::isFocused) + markers.filter(MapMarkerUi::isFocused)

internal fun mapMarkerRendering(marker: MapMarkerUi): MapMarkerRendering {
    val primary = EasyTripPrimary.toArgb()
    val focusedBorder = EasyTripAccent.toArgb()
    return when (marker.kind) {
        MapMarkerKind.UNSAVED_SEARCH -> MapMarkerRendering(
            glyph = "●",
            foregroundColor = 0xFFFFFFFF.toInt(),
            backgroundColor = EasyTripAccent.toArgb(),
            borderColor = if (marker.isFocused) focusedBorder else 0xFFFFFFFF.toInt(),
            borderWidth = if (marker.isFocused) 6 else 3,
            solid = true,
        )
        MapMarkerKind.SAVED_PLACE_POOL, MapMarkerKind.SAVED_ITINERARY -> MapMarkerRendering(
            glyph = if (marker.scheduled) marker.badgeText.orEmpty() else "",
            geometry = if (marker.scheduled) emptyList() else BookmarkGeometry,
            foregroundColor = if (marker.scheduled) 0xFFFFFFFF.toInt() else primary,
            backgroundColor = if (marker.scheduled) primary else 0xFFFFFFFF.toInt(),
            borderColor = if (marker.isFocused) focusedBorder else primary,
            borderWidth = if (marker.isFocused) 3 else if (marker.scheduled) 0 else 2,
            solid = marker.scheduled,
        )

    }
}

internal class MarkerIconView(context: Context, private val marker: MapMarkerUi) : View(context) {
    private val rendering = mapMarkerRendering(marker)
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val name = marker.label.takeIf {
        marker.kind == MapMarkerKind.SAVED_ITINERARY || marker.kind == MapMarkerKind.SAVED_PLACE_POOL
    }?.let { if (it.length > 18) it.take(17) + "…" else it }
    private val diameter = if (marker.kind == MapMarkerKind.UNSAVED_SEARCH) 44f else 28f
    private val textSizePx = 15f * density
    private val segments = marker.badgeSegments.takeIf {
        marker.kind == MapMarkerKind.SAVED_ITINERARY && marker.scheduled
    }.orEmpty()
    private val segmentWidths: List<Float>
    private val badgeWidth: Float
    private val badgeBounds = RectF()
    private val badgeClip = Path()
    val anchorX: Float get() = 0.5f
    val anchorY: Float get() = diameter * density / 2f / layoutParams.height

    init {
        paint.textSize = textSizePx
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        segmentWidths = segments.map { maxOf(diameter * density, paint.measureText(it.text) + 12f * density) }
        badgeWidth = if (segments.isEmpty()) {
            maxOf(diameter * density, paint.measureText(rendering.glyph) + 12f * density)
        } else segmentWidths.sum()
        val nameWidth = name?.let { paint.measureText(it) + 8f * density } ?: 0f
        layoutParams = android.view.ViewGroup.LayoutParams(kotlin.math.ceil(maxOf(badgeWidth, nameWidth)).toInt(), ((diameter + if (name != null) 24f else 0f) * density).toInt())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = setMeasuredDimension(layoutParams.width, layoutParams.height)

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = diameter * density / 2f
        val radius = (diameter - rendering.borderWidth) * density / 2f
        paint.style = Paint.Style.FILL
        paint.color = rendering.backgroundColor
        badgeBounds.set(cx - badgeWidth / 2f + rendering.borderWidth * density / 2f,
            cy - radius, cx + badgeWidth / 2f - rendering.borderWidth * density / 2f, cy + radius)
        if (segments.isEmpty()) {
            canvas.drawRoundRect(badgeBounds, radius, radius, paint)
        } else {
            badgeClip.reset()
            badgeClip.addRoundRect(badgeBounds, radius, radius, Path.Direction.CW)
            canvas.withClip(badgeClip) {
                var left = cx - badgeWidth / 2f
                segments.forEachIndexed { index, segment ->
                    paint.color = segment.colorArgb.toInt()
                    drawRect(left, badgeBounds.top, left + segmentWidths[index], badgeBounds.bottom, paint)
                    left += segmentWidths[index]
                }
            }
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = rendering.borderWidth * density
        paint.color = rendering.borderColor
        if (rendering.borderWidth > 0) canvas.drawRoundRect(badgeBounds, radius, radius, paint)
        paint.color = rendering.foregroundColor
        if (rendering.geometry.isEmpty()) {
            paint.style = Paint.Style.FILL
            paint.textSize = textSizePx
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            val baseline = cy - (paint.ascent() + paint.descent()) / 2f
            if (segments.isEmpty()) {
                canvas.drawText(rendering.glyph, cx, baseline, paint)
            } else {
                var left = cx - badgeWidth / 2f
                segments.forEachIndexed { index, segment ->
                    canvas.drawText(segment.text, left + segmentWidths[index] / 2f, baseline, paint)
                    left += segmentWidths[index]
                }
            }
        } else {
            val iconSize = 14f * density
            val path = Path()
            rendering.geometry.forEachIndexed { index, point ->
                val x = cx - iconSize / 2f + point.x * iconSize
                val y = cy - iconSize / 2f + point.y * iconSize
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            paint.style = if (rendering.solid) Paint.Style.FILL else Paint.Style.STROKE
            paint.strokeWidth = 2f * density
            canvas.drawPath(path, paint)
        }
        name?.let {
            val label = it
            paint.textSize = textSizePx
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            val baseline = diameter * density + 18f * density
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * density
            paint.color = android.graphics.Color.WHITE
            canvas.drawText(label, cx, baseline, paint)
            paint.style = Paint.Style.FILL
            paint.color = EasyTripPrimaryDark.toArgb()
            canvas.drawText(label, cx, baseline, paint)
        }
    }
}

internal fun northUpCameraPosition(position: CameraPosition): CameraPosition =
    CameraPosition.builder(position).bearing(0f).build()

internal class RealAmapMapHost(
    context: android.content.Context,
    private val releaseScheduler: MapReleaseScheduler = AndroidMapReleaseScheduler,
) : AmapMapHost {
    companion object {
        fun create(context: android.content.Context): AmapMapHost = RealAmapMapHost(context)
    }
    private val mapView = MapView(context)
    private var onReadyListener: (() -> Unit)? = null
    private var onBearingChanged: ((Float) -> Unit)? = null
    private val cameraChangeListener = object : AMap.OnCameraChangeListener {
        override fun onCameraChange(position: CameraPosition) {
            onBearingChanged?.invoke(position.bearing)
        }
        override fun onCameraChangeFinish(position: CameraPosition) {
            onBearingChanged?.invoke(position.bearing)
        }
    }
    private val mapLoadedListener = AMap.OnMapLoadedListener { onReadyListener?.invoke() }
    private var renderedOverlays: MapUiModel? = null
    private var consumedViewportId: Long? = null
    private var consumedViewportInsets = MapViewportInsets()
    private var touchInteractionDetector: MapTouchInteractionDetector? = null
    private var visibleViewportInsets = MapViewportInsets()
    private var pendingLocationCenter = false
    private var destroyed = false
    private val locationChangeListener = AMap.OnMyLocationChangeListener { location ->
        centerPendingLocation(location)
    }
    private val layerController = MapLayerApplicationController { rendering ->
        mapView.map.mapType = rendering.mapType
        mapView.map.showMapText(rendering.showMapText)
    }
    override val view: View = mapView
    override fun setOnReadyListener(listener: (() -> Unit)?) {
        onReadyListener = listener
        mapView.map.setOnMapLoadedListener(if (listener == null) null else mapLoadedListener)
    }
    override fun onCreate() {
        mapView.onCreate(null)
        mapView.map.uiSettings.isZoomControlsEnabled = false
        mapView.map.uiSettings.isCompassEnabled = false
        mapView.map.setOnMyLocationChangeListener(locationChangeListener)
    }
    override fun canRenderBeforeReady() = true
    override fun onResume() = mapView.onResume()
    override fun onPause() = mapView.onPause()
    override fun onDestroy() {
        if (destroyed) return
        destroyed = true
        pendingLocationCenter = false
        mapView.map.setOnMyLocationChangeListener(null)
        setOnReadyListener(null)
        setOnUserGestureListener(null)
        setOnBearingChangedListener(null)
        mapView.map.isMyLocationEnabled = false
        // AMap waits for its GL thread during destruction. Let the destination draw first,
        // then release on the UI thread, outside Compose's removal/layout frame.
        releaseScheduler.afterNextFrame { mapView.onDestroy() }
    }
    override fun zoomIn() = mapView.map.animateCamera(CameraUpdateFactory.zoomIn())
    override fun zoomOut() = mapView.map.animateCamera(CameraUpdateFactory.zoomOut())
    override fun resetNorth() {
        mapView.map.animateCamera(CameraUpdateFactory.newCameraPosition(northUpCameraPosition(mapView.map.cameraPosition)))
    }
    override fun setOnBearingChangedListener(listener: ((Float) -> Unit)?) {
        onBearingChanged = listener
        mapView.map.setOnCameraChangeListener(if (listener == null) null else cameraChangeListener)
        listener?.invoke(mapView.map.cameraPosition.bearing)
    }
    override fun setVisibleViewportInsets(insets: MapViewportInsets) {
        visibleViewportInsets = insets
    }
    override fun showCurrentLocation() {
        pendingLocationCenter = true
        // The SDK centers the full MapView; keep the location marker but position the camera ourselves.
        mapView.map.myLocationStyle = MyLocationStyle()
            .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        mapView.map.isMyLocationEnabled = true
        centerPendingLocation(mapView.map.myLocation)
    }
    private fun centerPendingLocation(location: Location?) {
        if (!pendingLocationCenter || location == null) return
        pendingLocationCenter = false
        mapView.map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f),
        )
        val insets = visibleViewportInsets
        if (insets != MapViewportInsets()) {
            mapView.map.moveCamera(
                CameraUpdateFactory.scrollBy(
                    (insets.rightPx - insets.leftPx) / 2f,
                    (insets.bottomPx - insets.topPx) / 2f,
                ),
            )
        }
    }
    override fun setOnUserGestureListener(listener: (() -> Unit)?) {
        touchInteractionDetector?.dispose()
        val detector = listener?.let { onUserGesture ->
            MapTouchInteractionDetector(
                touchSlop = ViewConfiguration.get(mapView.context).scaledTouchSlop.toFloat(),
                onInteraction = {
                    // A delayed first fix must not override a user's new viewport.
                    pendingLocationCenter = false
                    onUserGesture()
                },
            )
        }
        touchInteractionDetector = detector
        mapView.map.setOnMapTouchListener(
            detector?.let {
                AMap.OnMapTouchListener { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> it.onDown(event.x, event.y)
                        MotionEvent.ACTION_MOVE -> it.onMove(event.x, event.y)
                        MotionEvent.ACTION_POINTER_DOWN -> it.onPointerDown()
                        MotionEvent.ACTION_UP -> it.onUp()
                        MotionEvent.ACTION_CANCEL -> it.onCancel()
                    }
                }
            },
        )
    }
    override fun render(
        model: MapUiModel,
        layer: MapLayer,
        onMarkerClick: (String) -> Unit,
        onLayerError: (Throwable, MapLayer) -> Unit,
    ) = render(model, layer, onMarkerClick, {}, onLayerError)

    override fun render(
        model: MapUiModel,
        layer: MapLayer,
        onMarkerClick: (String) -> Unit,
        onMapPoiClick: (MapPoiUi) -> Unit,
        onLayerError: (Throwable, MapLayer) -> Unit,
    ) {
        layerController.apply(layer)?.let { failure ->
            onLayerError(failure.error, failure.retainedLayer)
        }
        mapView.map.setOnMarkerClickListener { marker ->
            (marker.`object` as? String)?.let(onMarkerClick)
            true
        }
        mapView.map.setOnPOIClickListener { poi ->
            poi.toMapPoiUi()?.let(onMapPoiClick)
        }
        viewportRendering(consumedViewportId, model.viewportRequest, consumedViewportInsets).let { rendering ->
            consumedViewportId = rendering.consumedRequestId
            consumedViewportInsets = rendering.consumedSafeInsets
            when (val command = rendering.command) {
                null -> Unit
                is ViewportCommand.SinglePoint -> {
                    mapView.map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(command.point.latitude, command.point.longitude),
                            command.zoom,
                        ),
                    )
                    if (command.centerOffset.xPx != 0 || command.centerOffset.yPx != 0) {
                        mapView.map.moveCamera(
                            CameraUpdateFactory.scrollBy(
                                command.centerOffset.xPx.toFloat(),
                                command.centerOffset.yPx.toFloat(),
                            ),
                        )
                    }
                }
                is ViewportCommand.Bounds -> {
                    val bounds = LatLngBounds.Builder().apply {
                        command.points.forEach { include(LatLng(it.latitude, it.longitude)) }
                    }.build()
                    mapView.map.moveCamera(
                        CameraUpdateFactory.newLatLngBoundsRect(
                            bounds,
                            command.safeInsets.leftPx,
                            command.safeInsets.rightPx,
                            command.safeInsets.topPx,
                            command.safeInsets.bottomPx,
                        ),
                    )
                }
            }
        }
        val overlays = model.copy(viewportRequest = null)
        if (overlays == renderedOverlays) return
        renderedOverlays = overlays
        mapView.map.clear()
        model.polylines.forEach { line ->
            mapView.map.addPolyline(
                PolylineOptions()
                    .addAll(line.points.map { LatLng(it.latitude, it.longitude) })
                    .color(line.colorArgb.toInt())
                    .width(10f),
            )
        }
        model.routeLabels.forEach { label ->
            mapView.map.addMarker(
                MarkerOptions()
                    .position(LatLng(label.point.latitude, label.point.longitude))
                    .title(label.label)
                    .icon(routeLabelMarker(label.label, label.colorArgb.toInt())),
            )
        }
        markerRenderOrder(model.markers).forEach { marker ->
            val iconView = MarkerIconView(mapView.context, marker)
            mapView.map.addMarker(
                MarkerOptions()
                    .position(LatLng(marker.point.latitude, marker.point.longitude))
                    .title(marker.label)
                    .icon(BitmapDescriptorFactory.fromView(iconView))
                    .anchor(iconView.anchorX, iconView.anchorY)
                    .zIndex(if (marker.isFocused) FOCUSED_MARKER_Z_INDEX else DEFAULT_MARKER_Z_INDEX),
            ).`object` = marker.key
        }
    }

    private fun markerIcon(marker: MapMarkerUi) = BitmapDescriptorFactory.fromView(
        MarkerIconView(mapView.context, marker),
    )

    private fun routeLabelMarker(label: String, color: Int) = BitmapDescriptorFactory.fromView(
        TextView(mapView.context).apply {
            text = label
            setTextColor(color)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(Color.argb(230, 255, 255, 255))
                setStroke(3, color)
            }
        },
    )
}

internal const val DEFAULT_MAP_READY_TIMEOUT_MILLIS = 20_000L

@Composable
fun AmapComposeMap(
    model: MapUiModel,
    onMarkerClick: (String) -> Unit,
    consent: AmapConsentToken,
    onMapPoiClick: (MapPoiUi) -> Unit = {},
    layer: MapLayer = MapLayer.STANDARD,
    locateRequest: Int = 0,
    initialLocateRequest: Int = 0,
    modifier: Modifier = Modifier,
    hostFactory: (android.content.Context) -> AmapMapHost = ::RealAmapMapHost,
    onLayerError: (Throwable, MapLayer) -> Unit = { _, _ -> },
    onLocateRequestConsumed: (Int) -> Unit = {},
    onMapError: (Throwable) -> Unit = {},
    onMapReady: () -> Unit = {},
    onUserGesture: () -> Unit = {},
    zoomInRequest: Int = 0,
    zoomOutRequest: Int = 0,
    resetNorthRequest: Int = 0,
    onBearingChanged: (Float) -> Unit = {},
    retryKey: Int = 0,
    readyTimeoutMillis: Long = DEFAULT_MAP_READY_TIMEOUT_MILLIS,
    visibleInsets: MapViewportInsets = MapViewportInsets(),
) {
    val consentSnapshot by consent.active.collectAsState()
    val context = LocalContext.current
    if (!consent.isActive(consentSnapshot)) return
    consent.validateActive()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnUserGesture by rememberUpdatedState(onUserGesture)
    val currentOnBearingChanged by rememberUpdatedState(onBearingChanged)
    val attemptKey = remember(retryKey, readyTimeoutMillis) { retryKey to readyTimeoutMillis }
    val mapFailureState = remember(context, consent, lifecycleOwner, attemptKey) { mutableStateOf<Throwable?>(null) }
    var mapReady by remember(context, consent, lifecycleOwner, attemptKey) { mutableStateOf(false) }
    var lifecycleResumed by remember(context, consent, lifecycleOwner, attemptKey) { mutableStateOf(false) }
    var consumedLocateRequest by remember(context, consent, lifecycleOwner, attemptKey) { mutableIntStateOf(initialLocateRequest) }
    val zoomRequestController = remember(context, consent, lifecycleOwner, attemptKey) {
        MapZoomRequestController(zoomInRequest, zoomOutRequest)
    }
    var consumedResetNorthRequest by remember(context, consent, lifecycleOwner, attemptKey) { mutableIntStateOf(resetNorthRequest) }
    var watchdogRevision by remember(context, consent, lifecycleOwner, attemptKey) { mutableIntStateOf(0) }
    val callbackGuard = remember(context, consent, lifecycleOwner, attemptKey) { MapHostCallbackGuard() }
    callbackGuard.updateHostCallback(currentOnUserGesture)
    val watchdog = remember(context, consent, lifecycleOwner, attemptKey) {
        MapReadyWatchdog(readyTimeoutMillis) {
            callbackGuard.reportHostError(MapReadyTimeoutException()) { mapFailureState.value = it }
        }
    }
    val host = remember(context, consent, lifecycleOwner, attemptKey) {
        var createdHost: AmapMapHost? = null
        runCatching {
            consent.validateActive()
            hostFactory(context).also { host ->
                createdHost = host
                host.setOnReadyListener {
                    callbackGuard.dispatchHost {
                        watchdog.ready()
                        mapReady = true
                    }
                }
                host.setOnUserGestureListener(callbackGuard::dispatchHost)
                host.onCreate()
                host.setOnBearingChangedListener { bearing ->
                    callbackGuard.dispatchHost { currentOnBearingChanged(bearing) }
                }
            }
        }.onFailure { error ->
            callbackGuard.reportHostError(error) { mapFailureState.value = it }
            callbackGuard.deactivate()
            createdHost?.let { failedHost ->
                runCatching(failedHost::onDestroy).onFailure { callbackGuard.reportDisposalError(it, onMapError) }
            }
        }.getOrNull()
    }
    LaunchedEffect(mapFailureState.value) {
        mapFailureState.value?.takeIf { callbackGuard.consumeMapError() }?.let(onMapError)
    }
    if (host == null || mapFailureState.value != null) return
    LaunchedEffect(host, locateRequest, lifecycleResumed, mapReady) {
        if (locateRequest > consumedLocateRequest && lifecycleResumed && mapReady) {
            consumedLocateRequest = locateRequest
            onLocateRequestConsumed(locateRequest)
            runCatching {
                host.setVisibleViewportInsets(visibleInsets)
                host.showCurrentLocation()
            }.onFailure(onMapError)
        }
    }
    val currentOnMapError by rememberUpdatedState(onMapError)
    SideEffect {
        runCatching { host.setVisibleViewportInsets(visibleInsets) }.onFailure(currentOnMapError)
        if (lifecycleResumed && mapReady) {
            if (resetNorthRequest > consumedResetNorthRequest) {
                consumedResetNorthRequest = resetNorthRequest
                runCatching(host::resetNorth).onFailure(currentOnMapError)
            }
            while (zoomRequestController.consumeZoomIn(zoomInRequest)) {
                runCatching(host::zoomIn).onFailure(currentOnMapError)
            }
            while (zoomRequestController.consumeZoomOut(zoomOutRequest)) {
                runCatching(host::zoomOut).onFailure(currentOnMapError)
            }
        }
    }
    DisposableEffect(lifecycleOwner, host) {
        val lifecycleError: (Throwable) -> Unit = { error ->
            callbackGuard.reportHostError(error) { mapFailureState.value = it }
        }
        val controller = MapLifecycleController(
            resume = {
                runCatching(host::onResume).fold(
                    onSuccess = {
                        lifecycleResumed = true
                        watchdog.resume()
                        watchdogRevision++
                    },
                    onFailure = lifecycleError,
                )
            },
            pause = {
                lifecycleResumed = false
                watchdog.pause()
                watchdogRevision++
                runCatching(host::onPause).onFailure(lifecycleError)
            },
            destroy = {
                lifecycleResumed = false
                watchdog.cancel()
                runCatching(host::onDestroy).onFailure { error ->
                    callbackGuard.reportDisposalError(error, onMapError)
                }
            },
        )
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.syncResumed(true)
                Lifecycle.Event.ON_PAUSE -> controller.syncResumed(false)
                Lifecycle.Event.ON_DESTROY -> controller.dispose()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        controller.syncResumed(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            lifecycleResumed = false
            callbackGuard.deactivate()
            watchdog.cancel()
            host.setOnReadyListener(null)
            host.setOnUserGestureListener(null)
            host.setOnBearingChangedListener(null)
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.dispose()
        }
    }
    LaunchedEffect(host, mapReady, readyTimeoutMillis, watchdogRevision) {
        if (!mapReady && mapFailureState.value == null && !watchdog.isTerminal) {
            delay(watchdog.remainingMillis().coerceAtLeast(1))
            watchdog.timeoutIfElapsed()
        }
    }
    AndroidView(
        factory = { host.view },
        modifier = modifier,
        update = {
            if (lifecycleResumed && (mapReady || host.canRenderBeforeReady())) {
                val renderGeneration = callbackGuard.beginRender()
                runCatching {
                    consent.validateActive()
                    host.render(
                        model,
                        layer,
                        { markerKey -> callbackGuard.dispatch(renderGeneration) { onMarkerClick(markerKey) } },
                        { poi -> callbackGuard.dispatch(renderGeneration) { onMapPoiClick(poi) } },
                    ) { error, retainedLayer ->
                        callbackGuard.dispatch(renderGeneration) { onLayerError(error, retainedLayer) }
                    }
                    if (!mapReady) {
                        callbackGuard.reportReady(renderGeneration) {
                            watchdog.ready()
                            mapReady = true
                            onMapReady()
                        }
                    } else {
                        callbackGuard.reportReady(renderGeneration, onMapReady)
                    }
                }.onFailure { error ->
                    callbackGuard.reportError(renderGeneration, error) { mapFailureState.value = it }
                }
            }
        },
    )
}
