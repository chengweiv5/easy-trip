package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint

class MapViewportController {
    private var nextRequestId = 1L
    private var observedNonemptyPlaces = false
    private var placeIdentity: Set<GeoPoint> = emptySet()
    private var scope: MapScope? = null

    var currentRequest: MapViewportRequest? = null
        private set

    fun update(
        placePoints: List<GeoPoint>,
        scope: MapScope,
        visiblePoints: List<GeoPoint>,
    ): MapViewportRequest? {
        val normalizedPlaces = placePoints.toSet()
        val reason = when {
            !observedNonemptyPlaces && normalizedPlaces.isNotEmpty() -> ViewportReason.INITIAL
            observedNonemptyPlaces && normalizedPlaces != placeIdentity -> ViewportReason.PLACE_SET_CHANGED
            this.scope != null && this.scope != scope -> ViewportReason.SCOPE_CHANGED
            else -> null
        }
        if (normalizedPlaces.isNotEmpty()) observedNonemptyPlaces = true
        placeIdentity = normalizedPlaces
        this.scope = scope
        if (visiblePoints.isEmpty() && currentRequest?.reason != ViewportReason.SEARCH_FOCUS) {
            currentRequest = null
        }
        return emit(reason, visiblePoints)
    }

    fun focusSearchResult(point: GeoPoint): MapViewportRequest =
        requireNotNull(emit(ViewportReason.SEARCH_FOCUS, listOf(point), SEARCH_FOCUS_ZOOM))

    private fun emit(
        reason: ViewportReason?,
        points: List<GeoPoint>,
        singlePointZoom: Float? = null,
    ): MapViewportRequest? {
        if (reason == null) return null
        if (points.isEmpty()) {
            if (currentRequest?.reason != ViewportReason.SEARCH_FOCUS) currentRequest = null
            return null
        }
        return MapViewportRequest(nextRequestId++, reason, points.distinct(), singlePointZoom).also {
            currentRequest = it
        }
    }
}
