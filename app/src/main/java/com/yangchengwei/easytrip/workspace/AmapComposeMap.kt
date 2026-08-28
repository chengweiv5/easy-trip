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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
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
import com.amap.api.maps.model.Poi
import com.amap.api.maps.model.PolylineOptions
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.model.GeoPoint
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

internal class MapHostCallbackGuard {
    private var active = true
    private var terminalFailure = false
    private var mapErrorReported = false
    private var readyReported = false
    private var disposalFailureReported = false
    private var renderGeneration = 0L

    fun beginRender(): Long = ++renderGeneration

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
        if (!active || terminalFailure || generation != renderGeneration) return
        terminalFailure = true
        callback(error)
    }

    fun reportHostError(error: Throwable) {
        if (!active || terminalFailure) return
        terminalFailure = true
    }

    fun consumeMapError(): Boolean {
        if (mapErrorReported) return false
        mapErrorReported = true
        return true
    }

    fun reportDisposalError(error: Throwable, callback: (Throwable) -> Unit) {
        if (terminalFailure || disposalFailureReported) return
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
    fun setOnReadyListener(listener: (() -> Unit)?) { listener?.invoke() }
    fun onCreate()
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun zoomIn() = Unit
    fun zoomOut() = Unit
    fun showCurrentLocation() = Unit
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

internal class RealAmapMapHost(context: android.content.Context) : AmapMapHost {
    companion object {
        fun create(context: android.content.Context): AmapMapHost = RealAmapMapHost(context)
    }
    private val mapView = MapView(context)
    private var onReadyListener: (() -> Unit)? = null
    private val mapLoadedListener = AMap.OnMapLoadedListener { onReadyListener?.invoke() }
    private var renderedOverlays: MapUiModel? = null
    private var consumedViewportId: Long? = null
    private var appliedLayer: MapLayer? = null
    override val view: View = mapView
    override fun setOnReadyListener(listener: (() -> Unit)?) {
        onReadyListener = listener
        mapView.map.setOnMapLoadedListener(if (listener == null) null else mapLoadedListener)
    }
    override fun onCreate() {
        mapView.onCreate(null)
        mapView.map.uiSettings.isZoomControlsEnabled = false
    }
    override fun onResume() = mapView.onResume()
    override fun onPause() = mapView.onPause()
    override fun onDestroy() {
        setOnReadyListener(null)
        mapView.onDestroy()
    }
    override fun zoomIn() = mapView.map.animateCamera(CameraUpdateFactory.zoomIn())
    override fun zoomOut() = mapView.map.animateCamera(CameraUpdateFactory.zoomOut())
    override fun showCurrentLocation() {
        mapView.map.isMyLocationEnabled = true
        mapView.map.myLocation?.let { location ->
            mapView.map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f),
            )
        }
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
        mapView.map.setOnPOIClickListener { poi ->
            poi.toMapPoiUi()?.let(onMapPoiClick)
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
            mapView.map.addMarker(
                MarkerOptions()
                    .position(LatLng(marker.point.latitude, marker.point.longitude))
                    .title(marker.label)
                    .icon(markerIcon(marker)),
            ).`object` = marker.key
        }
    }

    private fun markerIcon(marker: MapMarkerUi) = BitmapDescriptorFactory.fromView(
        TextView(mapView.context).apply {
            text = when (marker.kind) {
                MapMarkerKind.UNSAVED_SEARCH -> "●"
                MapMarkerKind.SAVED_PLACE_POOL -> "★"
                MapMarkerKind.SAVED_ITINERARY -> marker.badgeText?.let { "★ $it" } ?: "★"
            }
            setTextColor(Color.WHITE)
            textSize = if (marker.isFocused) 18f else 14f
            gravity = Gravity.CENTER
            val horizontalPadding = if (marker.isFocused) 22 else 16
            val verticalPadding = if (marker.isFocused) 14 else 10
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(
                    when (marker.kind) {
                        MapMarkerKind.UNSAVED_SEARCH -> Color.rgb(216, 67, 21)
                        MapMarkerKind.SAVED_PLACE_POOL, MapMarkerKind.SAVED_ITINERARY -> Color.rgb(25, 118, 210)
                    },
                )
                setStroke(if (marker.isFocused) 6 else 3, if (marker.isFocused) Color.YELLOW else Color.WHITE)
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
}

@Composable
fun AmapComposeMap(
    model: MapUiModel,
    onMarkerClick: (String) -> Unit,
    consent: AmapConsentToken,
    onMapPoiClick: (MapPoiUi) -> Unit = {},
    layer: MapLayer = MapLayer.STANDARD,
    locateRequest: Int = 0,
    modifier: Modifier = Modifier,
    hostFactory: (android.content.Context) -> AmapMapHost = ::RealAmapMapHost,
    onLayerError: (Throwable, MapLayer) -> Unit = { _, _ -> },
    onMapError: (Throwable) -> Unit = {},
    onMapReady: () -> Unit = {},
    retryKey: Int = 0,
) {
    val consentSnapshot by consent.active.collectAsState()
    val context = LocalContext.current
    if (!consent.isActive(consentSnapshot)) return
    consent.validateActive()
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapFailureState = remember(context, consent, lifecycleOwner, retryKey) { mutableStateOf<Throwable?>(null) }
    var mapReady by remember(context, consent, lifecycleOwner, retryKey) { mutableStateOf(false) }
    val callbackGuard = remember(context, consent, lifecycleOwner, retryKey) { MapHostCallbackGuard() }
    val host = remember(context, consent, lifecycleOwner, retryKey) {
        var createdHost: AmapMapHost? = null
        runCatching {
            consent.validateActive()
            hostFactory(context).also { host ->
                createdHost = host
                host.setOnReadyListener {
                    callbackGuard.dispatchHost { mapReady = true }
                }
                host.onCreate()
            }
        }.onFailure { error ->
            callbackGuard.reportHostError(error)
            mapFailureState.value = error
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
    LaunchedEffect(host, locateRequest) {
        if (locateRequest > 0) host.showCurrentLocation()
    }
    DisposableEffect(lifecycleOwner, host) {
        val lifecycleError: (Throwable) -> Unit = { error ->
            callbackGuard.reportHostError(error)
            mapFailureState.value = error
        }
        val controller = MapLifecycleController(
            resume = { runCatching(host::onResume).onFailure(lifecycleError) },
            pause = { runCatching(host::onPause).onFailure(lifecycleError) },
            destroy = {
                runCatching(host::onDestroy).onFailure { error ->
                    mapFailureState.value = error
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
            callbackGuard.deactivate()
            host.setOnReadyListener(null)
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.dispose()
        }
    }
    Box(modifier) {
        AndroidView(
            factory = { host.view },
            modifier = Modifier.fillMaxSize(),
            update = {
                if (mapReady) {
                    val renderGeneration = callbackGuard.beginRender()
                    runCatching {
                        consent.validateActive()
                        var layerFailure: Throwable? = null
                        host.render(
                            model,
                            layer,
                            { markerKey -> callbackGuard.dispatch(renderGeneration) { onMarkerClick(markerKey) } },
                            { poi -> callbackGuard.dispatch(renderGeneration) { onMapPoiClick(poi) } },
                        ) { error, retainedLayer ->
                            layerFailure = error
                            callbackGuard.reportError(renderGeneration, error) { onLayerError(it, retainedLayer) }
                        }
                        if (layerFailure == null) callbackGuard.reportReady(renderGeneration, onMapReady)
                    }.onFailure { error ->
                        callbackGuard.reportError(renderGeneration, error) { mapFailureState.value = it }
                    }
                }
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
