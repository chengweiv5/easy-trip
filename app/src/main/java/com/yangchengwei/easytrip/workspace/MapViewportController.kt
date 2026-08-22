package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint

class MapViewportController {
    private var nextRequestId = 1L
    private var observedNonemptyPlaces = false
    private var placeIdentity: Set<GeoPoint> = emptySet()
    private var scope: MapScope? = null
    private var visibleIdentity: Set<GeoPoint> = emptySet()
    private var retainedPlaceChange: GeoPoint? = null

    var currentRequest: MapViewportRequest? = null
        private set

    fun update(
        placePoints: List<GeoPoint>,
        scope: MapScope,
        visiblePoints: List<GeoPoint>,
    ): MapViewportRequest? {
        val normalizedPlaces = placePoints.toSet()
        val normalizedVisible = visiblePoints.toSet()
        val retainedChange = retainedPlaceChange?.takeIf { point ->
            normalizedPlaces == placeIdentity + point || normalizedPlaces == placeIdentity - point
        }
        if (retainedChange != null) retainedPlaceChange = null
        val reason = when {
            !observedNonemptyPlaces && normalizedPlaces.isNotEmpty() -> ViewportReason.INITIAL
            retainedChange != null -> null
            observedNonemptyPlaces && normalizedPlaces != placeIdentity -> ViewportReason.PLACE_SET_CHANGED
            this.scope != null && this.scope != scope -> ViewportReason.SCOPE_CHANGED
            this.scope == scope && scope != MapScope.PLACE_POOL && normalizedVisible != visibleIdentity -> ViewportReason.VISIBLE_SET_CHANGED
            else -> null
        }
        if (normalizedPlaces.isNotEmpty()) observedNonemptyPlaces = true
        placeIdentity = normalizedPlaces
        visibleIdentity = normalizedVisible
        this.scope = scope
        if (visiblePoints.isEmpty() && currentRequest?.reason != ViewportReason.SEARCH_FOCUS) {
            currentRequest = null
        }
        return emit(reason, visiblePoints)
    }

    fun retainViewportForPlaceChange(point: GeoPoint) {
        retainedPlaceChange = point
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
