package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.workspace.MapMarkerKind
import com.yangchengwei.easytrip.workspace.MapMarkerUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import com.yangchengwei.easytrip.workspace.MapViewportRequest
import com.yangchengwei.easytrip.workspace.SEARCH_FOCUS_ZOOM
import com.yangchengwei.easytrip.workspace.ViewportReason

internal fun searchDetailMapModel(
    candidate: PlaceCandidate,
    requestId: Long,
    savedPlace: SavedPlace? = null,
): MapUiModel? {
    val point = candidate.point ?: return null
    val markerKey = "search-${candidate.poiId}"
    return MapUiModel(
        markers = listOf(
            MapMarkerUi(
                key = markerKey,
                point = point,
                label = candidate.name,
                occurrences = emptyList(),
                kind = if (savedPlace == null) MapMarkerKind.UNSAVED_SEARCH else MapMarkerKind.SAVED_PLACE_POOL,
                savedPlaceId = savedPlace?.id,
                category = savedPlace?.category,
                isFocused = true,
            ),
        ),
        highlightedMarkerKey = markerKey,
        viewportRequest = MapViewportRequest(
            id = requestId,
            reason = ViewportReason.SEARCH_FOCUS,
            points = listOf(point),
            singlePointZoom = SEARCH_FOCUS_ZOOM,
        ),
    )
}
