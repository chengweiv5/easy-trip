package com.yangchengwei.easytrip.workspace

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
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
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.AmapPrivacyGate


sealed interface ViewportCommand {
    data class SinglePoint(val point: com.yangchengwei.easytrip.core.model.GeoPoint, val zoom: Float) : ViewportCommand
    data class Bounds(val points: List<com.yangchengwei.easytrip.core.model.GeoPoint>, val paddingPx: Int) : ViewportCommand
}

data class ViewportRendering(val consumedRequestId: Long?, val command: ViewportCommand?)

fun viewportRendering(consumedRequestId: Long?, request: MapViewportRequest?): ViewportRendering {
    if (request == null || request.id == consumedRequestId) return ViewportRendering(consumedRequestId, null)
    val command = when (request.points.size) {
        0 -> null
        1 -> ViewportCommand.SinglePoint(request.points.single(), request.singlePointZoom ?: 15f)
        else -> ViewportCommand.Bounds(request.points, 96)
    }
    return ViewportRendering(request.id, command)
}

data class MapLayerRendering(val mapType: Int, val showMapText: Boolean)

fun mapLayerRendering(applied: MapLayer?, requested: MapLayer): MapLayerRendering? =
    if (applied == requested) null else when (requested) {
        MapLayer.STANDARD -> MapLayerRendering(AMap.MAP_TYPE_NORMAL, true)
        MapLayer.SATELLITE_ROAD -> MapLayerRendering(AMap.MAP_TYPE_SATELLITE, true)
    }

interface AmapMapHost {
    val view: View
    fun onCreate()
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun zoomIn() = Unit
    fun zoomOut() = Unit
    fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit = { _, _ -> })
}

internal class RealAmapMapHost(context: android.content.Context) : AmapMapHost {
    companion object {
        fun create(context: android.content.Context): AmapMapHost = RealAmapMapHost(context)
    }
    private val mapView = MapView(context)
    private var renderedOverlays: MapUiModel? = null
    private var consumedViewportId: Long? = null
    private var appliedLayer: MapLayer? = null
    override val view: View = mapView
    override fun onCreate() {
        mapView.onCreate(null)
        mapView.map.uiSettings.isZoomControlsEnabled = false
    }
    override fun onResume() = mapView.onResume()
    override fun onPause() = mapView.onPause()
    override fun onDestroy() = mapView.onDestroy()
    override fun zoomIn() = mapView.map.animateCamera(CameraUpdateFactory.zoomIn())
    override fun zoomOut() = mapView.map.animateCamera(CameraUpdateFactory.zoomOut())
    override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
        mapLayerRendering(appliedLayer, layer)?.let { rendering ->
            runCatching {
                mapView.map.mapType = rendering.mapType
                mapView.map.showMapText(rendering.showMapText)
            }.onSuccess {
                appliedLayer = layer
            }.onFailure { error ->
                appliedLayer?.let { previous ->
                    mapLayerRendering(null, previous)?.let { rollback ->
                        runCatching {
                            mapView.map.mapType = rollback.mapType
                            mapView.map.showMapText(rollback.showMapText)
                        }
                    }
                }
                onLayerError(error, appliedLayer ?: MapLayer.STANDARD)
            }
        }
        mapView.map.setOnMarkerClickListener { marker ->
            (marker.`object` as? String)?.let(onMarkerClick)
            true
        }
        viewportRendering(consumedViewportId, model.viewportRequest).let { rendering ->
            consumedViewportId = rendering.consumedRequestId
            when (val command = rendering.command) {
                null -> Unit
                is ViewportCommand.SinglePoint -> mapView.map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(command.point.latitude, command.point.longitude),
                        command.zoom,
                    ),
                )
                is ViewportCommand.Bounds -> {
                    val bounds = LatLngBounds.Builder().apply {
                        command.points.forEach { include(LatLng(it.latitude, it.longitude)) }
                    }.build()
                    mapView.map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, command.paddingPx))
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
        model.markers.forEach { marker ->
            val options = MarkerOptions()
                .position(LatLng(marker.point.latitude, marker.point.longitude))
                .title(marker.label)
            when {
                marker.key == model.highlightedMarkerKey -> options.icon(highlightedMarker())
                marker.occurrences.isNotEmpty() -> options.icon(numberedMarker(marker.label))
            }
            mapView.map.addMarker(options).`object` = marker.key
        }
    }

    private fun highlightedMarker() = BitmapDescriptorFactory.fromView(
        TextView(mapView.context).apply {
            text = "●"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(22, 14, 22, 14)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(216, 67, 21))
                setStroke(5, Color.WHITE)
            }
        },
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
    layer: MapLayer = MapLayer.STANDARD,
    modifier: Modifier = Modifier,
    hostFactory: (android.content.Context) -> AmapMapHost = ::RealAmapMapHost,
    onLayerError: (Throwable, MapLayer) -> Unit = { _, _ -> },
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
    Box(modifier) {
        AndroidView(
            factory = { host.view },
            modifier = Modifier.fillMaxSize(),
            update = {
                consent.validateActive()
                host.render(model, layer, onMarkerClick, onLayerError)
            },
        )
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapZoomButton("+", "zoom-in", host::zoomIn)
            MapZoomButton("−", "zoom-out", host::zoomOut)
        }
    }
}

@Composable
private fun MapZoomButton(label: String, tag: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(24.dp).testTag(tag).clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val color = androidx.compose.ui.graphics.Color.Black
            val stroke = 2.dp.toPx()
            val inset = size.width * 0.28f
            drawLine(color, androidx.compose.ui.geometry.Offset(inset, size.height / 2), androidx.compose.ui.geometry.Offset(size.width - inset, size.height / 2), stroke)
            if (label == "+") drawLine(color, androidx.compose.ui.geometry.Offset(size.width / 2, inset), androidx.compose.ui.geometry.Offset(size.width / 2, size.height - inset), stroke)
        }
    }
}
