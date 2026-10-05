package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint

class MapViewportController {
    private var nextRequestId = 1L
    private var observedNonemptyPlaces = false
    private var placeIdentity: Set<GeoPoint> = emptySet()
    private var scope: MapScope? = null
    private var selectedDayId: String? = null
    private var visibleIdentity: Set<GeoPoint> = emptySet()
    private var retainedPlaceChange: GeoPoint? = null
    private var userMovedViewport = false
    private var placeFilter = PlacePoolMapFilter()
    private var cameraRetained = false
    private var retainNextViewportUpdate = false

    var currentRequest: MapViewportRequest? = null
        private set

    fun update(
        placePoints: List<GeoPoint>,
        scope: MapScope,
        visiblePoints: List<GeoPoint>,
        selectedDayId: String? = null,
        placeFilter: PlacePoolMapFilter = PlacePoolMapFilter(),
        retainCamera: Boolean = false,
    ): MapViewportRequest? {
        val normalizedPlaces = placePoints.toSet()
        val normalizedVisible = visiblePoints.toSet()
        val revealItinerary = cameraRetained && !retainCamera && scope != MapScope.PLACE_POOL
        cameraRetained = retainCamera
        val retainedChange = retainedPlaceChange?.takeIf { point ->
            normalizedPlaces == placeIdentity + point || normalizedPlaces == placeIdentity - point
        }
        if (retainedChange != null) retainedPlaceChange = null
        val reason = when {
            retainNextViewportUpdate -> null
            revealItinerary -> ViewportReason.SCOPE_CHANGED
            !observedNonemptyPlaces && normalizedPlaces.isNotEmpty() -> ViewportReason.INITIAL
            retainedChange != null -> null
            observedNonemptyPlaces && normalizedPlaces != placeIdentity -> ViewportReason.PLACE_SET_CHANGED
            this.scope != null && this.scope != scope -> ViewportReason.SCOPE_CHANGED
            this.scope == scope && this.selectedDayId != selectedDayId -> ViewportReason.VISIBLE_SET_CHANGED
            scope == MapScope.PLACE_POOL && this.placeFilter != placeFilter -> ViewportReason.VISIBLE_SET_CHANGED
            userMovedViewport -> null
            this.scope == scope && scope != MapScope.PLACE_POOL && normalizedVisible != visibleIdentity -> ViewportReason.VISIBLE_SET_CHANGED
            else -> null
        }
        retainNextViewportUpdate = false
        if (reason == ViewportReason.INITIAL || reason == ViewportReason.PLACE_SET_CHANGED || reason == ViewportReason.SCOPE_CHANGED || this.selectedDayId != selectedDayId || this.placeFilter != placeFilter) {
            userMovedViewport = false
        }
        if (normalizedPlaces.isNotEmpty()) observedNonemptyPlaces = true
        placeIdentity = normalizedPlaces
        visibleIdentity = normalizedVisible
        this.scope = scope
        this.selectedDayId = selectedDayId
        this.placeFilter = placeFilter
        // Keep observing while covered. Revealing the map fits the current projection,
        // not a stale date/route request from when the drawer was expanded.
        if (retainCamera && reason != null) {
            currentRequest = null
            userMovedViewport = true
            return null
        }
        if (visiblePoints.isEmpty() && currentRequest?.reason != ViewportReason.SEARCH_FOCUS) {
            currentRequest = null
        }
        return emit(reason, visiblePoints, scope = scope, selectedDayId = selectedDayId)
    }

    fun onUserGesture() {
        userMovedViewport = true
        currentRequest = null
    }

    /** Reconcile changes made under the search layer without moving the camera on dismissal. */
    fun retainViewportOnNextUpdate() {
        onUserGesture()
        retainNextViewportUpdate = true
    }

    fun retainViewportForPlaceChange(point: GeoPoint) {
        retainedPlaceChange = point
    }

    fun focusSearchResult(point: GeoPoint): MapViewportRequest =
        requireNotNull(emit(ViewportReason.SEARCH_FOCUS, listOf(point), SEARCH_FOCUS_ZOOM))

    fun showSearchResults(points: List<GeoPoint>) {
        currentRequest = null
        emit(ViewportReason.SEARCH_RESULTS, points)
    }

    private fun emit(
        reason: ViewportReason?,
        points: List<GeoPoint>,
        singlePointZoom: Float? = null,
        scope: MapScope? = null,
        selectedDayId: String? = null,
    ): MapViewportRequest? {
        if (reason == null) return null
        if (points.isEmpty()) {
            if (currentRequest?.reason != ViewportReason.SEARCH_FOCUS) currentRequest = null
            return null
        }
        return MapViewportRequest(
            id = nextRequestId++,
            reason = reason,
            points = points.distinct(),
            singlePointZoom = singlePointZoom,
            scope = scope,
            selectedDayId = selectedDayId,
        ).also {
            currentRequest = it
        }
    }
}
