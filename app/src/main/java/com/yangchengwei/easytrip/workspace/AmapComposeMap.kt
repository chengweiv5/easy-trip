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
import android.content.Context
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
    if (request == null || (request.id == consumedRequestId && request.safeInsets == consumedSafeInsets)) {
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
    val primary = 0xFF2D5E3A.toInt()
    val focusedBorder = 0xFFD96F3B.toInt()
    return when (marker.kind) {
        MapMarkerKind.UNSAVED_SEARCH -> MapMarkerRendering(
            glyph = "●",
            foregroundColor = 0xFFFFFFFF.toInt(),
            backgroundColor = 0xFFD84315.toInt(),
            borderColor = if (marker.isFocused) focusedBorder else 0xFFFFFFFF.toInt(),
            borderWidth = if (marker.isFocused) 6 else 3,
            solid = true,
        )
        MapMarkerKind.SAVED_PLACE_POOL -> MapMarkerRendering(
            glyph = "",
            geometry = BookmarkGeometry,
            foregroundColor = if (marker.scheduled) 0xFFFFFFFF.toInt() else primary,
            backgroundColor = if (marker.scheduled) primary else 0xFFFFFFFF.toInt(),
            borderColor = if (marker.isFocused) focusedBorder else primary,
            borderWidth = if (marker.isFocused) 6 else 3,
            solid = marker.scheduled,
        )
        MapMarkerKind.SAVED_ITINERARY -> MapMarkerRendering(
            glyph = marker.badgeText.orEmpty(),
            geometry = BookmarkGeometry,
            foregroundColor = 0xFFFFFFFF.toInt(),
            backgroundColor = primary,
            borderColor = if (marker.isFocused) focusedBorder else 0xFFFFFFFF.toInt(),
            borderWidth = if (marker.isFocused) 6 else 3,
            solid = true,
            badgeBackgroundColor = primary,
            badgeForegroundColor = 0xFFFFFFFF.toInt(),
        )
    }
}

private class MarkerIconView(context: Context, private val marker: MapMarkerUi) : View(context) {
    private val rendering = mapMarkerRendering(marker)
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        val size = if (marker.isFocused) 52 else 44
        val width = if (marker.badgeText != null) size + 28 else size
        layoutParams = android.view.ViewGroup.LayoutParams((width * density).toInt(), (size * density).toInt())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(layoutParams.width, layoutParams.height)
    }

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val badgeWidth = if (marker.badgeText != null) 28f * density else 0f
        val iconAreaWidth = width - badgeWidth
        val cx = iconAreaWidth / 2f
        val cy = height / 2f
        val radius = minOf(iconAreaWidth, height.toFloat()) * 0.44f
        paint.style = Paint.Style.FILL
        paint.color = rendering.backgroundColor
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = rendering.borderWidth * density
        paint.color = rendering.borderColor
        canvas.drawCircle(cx, cy, radius, paint)

        paint.color = rendering.foregroundColor
        if (rendering.geometry.isEmpty()) {
            paint.style = Paint.Style.FILL
            paint.textSize = radius
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(rendering.glyph, cx, cy + paint.textSize * 0.35f, paint)
        } else {
            val iconSize = radius * 0.92f
            val left = cx - iconSize / 2f
            val top = cy - iconSize / 2f
            val path = Path()
            rendering.geometry.forEachIndexed { index, point ->
                val x = left + point.x * iconSize
                val y = top + point.y * iconSize
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            paint.style = if (rendering.solid) Paint.Style.FILL else Paint.Style.STROKE
            paint.strokeWidth = 2f * density
            canvas.drawPath(path, paint)
        }

        marker.badgeText?.let { badge ->
            val badgeCenterX = iconAreaWidth + badgeWidth / 2f
            paint.style = Paint.Style.FILL
            paint.color = rendering.badgeBackgroundColor
            canvas.drawRoundRect(
                iconAreaWidth,
                cy - 10f * density,
                width.toFloat(),
                cy + 10f * density,
                10f * density,
                10f * density,
                paint,
            )
            paint.color = rendering.badgeForegroundColor
            paint.textSize = 11f * density
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(badge, badgeCenterX, cy + paint.textSize * 0.35f, paint)
        }
    }
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
    private var consumedViewportInsets = MapViewportInsets()
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
            mapView.map.addMarker(
                MarkerOptions()
                    .position(LatLng(marker.point.latitude, marker.point.longitude))
                    .title(marker.label)
                    .icon(markerIcon(marker))
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
                        host.render(
                            model,
                            layer,
                            { markerKey -> callbackGuard.dispatch(renderGeneration) { onMarkerClick(markerKey) } },
                            { poi -> callbackGuard.dispatch(renderGeneration) { onMapPoiClick(poi) } },
                        ) { error, retainedLayer ->
                            callbackGuard.dispatch(renderGeneration) { onLayerError(error, retainedLayer) }
                        }
                        callbackGuard.reportReady(renderGeneration, onMapReady)
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
