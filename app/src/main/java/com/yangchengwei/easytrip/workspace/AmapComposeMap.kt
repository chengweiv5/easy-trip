package com.yangchengwei.easytrip.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.AmapPrivacyGate

interface AmapMapHost {
    val view: View
    fun onCreate()
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun render(model: MapUiModel, onMarkerClick: (String) -> Unit)
}

private class RealAmapMapHost(context: android.content.Context) : AmapMapHost {
    private val mapView = MapView(context)
    private var renderedModel: MapUiModel? = null
    override val view: View = mapView
    override fun onCreate() = mapView.onCreate(null)
    override fun onResume() = mapView.onResume()
    override fun onPause() = mapView.onPause()
    override fun onDestroy() = mapView.onDestroy()
    override fun render(model: MapUiModel, onMarkerClick: (String) -> Unit) {
        mapView.map.setOnMarkerClickListener { marker ->
            (marker.`object` as? String)?.let(onMarkerClick)
            true
        }
        if (model == renderedModel) return
        renderedModel = model
        mapView.map.clear()
        model.polylines.forEach { line ->
            mapView.map.addPolyline(
                PolylineOptions()
                    .addAll(line.points.map { LatLng(it.latitude, it.longitude) })
                    .color(line.colorArgb.toInt())
                    .width(10f),
            )
        }
        model.markers.forEach { marker ->
            val options = MarkerOptions()
                .position(LatLng(marker.point.latitude, marker.point.longitude))
                .title(marker.label)
            if (marker.occurrences.isNotEmpty()) options.icon(numberedMarker(marker.label))
            mapView.map.addMarker(options).`object` = marker.key
        }
    }

    private fun numberedMarker(label: String) = BitmapDescriptorFactory.fromView(
        TextView(mapView.context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(16, 10, 16, 10)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(Color.rgb(25, 118, 210))
                setStroke(3, Color.WHITE)
            }
        },
    )
}

@Composable
fun AmapComposeMap(
    model: MapUiModel,
    onMarkerClick: (String) -> Unit,
    consent: AmapConsentToken,
    modifier: Modifier = Modifier,
    hostFactory: (android.content.Context) -> AmapMapHost = ::RealAmapMapHost,
) {
    val consentSnapshot by consent.active.collectAsState()
    val context = LocalContext.current
    if (!consent.isActive(consentSnapshot)) return
    consent.validateActive()
    val lifecycleOwner = LocalLifecycleOwner.current
    val host = remember(context, consent, lifecycleOwner) {
        consent.validateActive()
        hostFactory(context).apply { onCreate() }
    }
    DisposableEffect(lifecycleOwner, host) {
        val controller = MapLifecycleController(host::onResume, host::onPause, host::onDestroy)
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
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.dispose()
        }
    }
    AndroidView(
        factory = { host.view },
        modifier = modifier,
        update = {
            consent.validateActive()
            host.render(model, onMarkerClick)
        },
    )
}
