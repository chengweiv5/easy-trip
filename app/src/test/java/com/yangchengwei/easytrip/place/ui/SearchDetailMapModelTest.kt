package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.workspace.MapMarkerKind
import com.yangchengwei.easytrip.workspace.SEARCH_FOCUS_ZOOM
import com.yangchengwei.easytrip.workspace.ViewportReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDetailMapModelTest {
    private val candidate = PlaceCandidate(
        poiId = "poi-1",
        name = "故宫博物院",
        address = "北京市东城区景山前街4号",
        point = GeoPoint(39.916, 116.397),
        cityCode = "010",
    )

    @Test fun selectedCandidateMapsToExactlyOneSearchMarker() {
        val model = searchDetailMapModel(candidate, requestId = 7L)

        val marker = requireNotNull(model).markers.single()
        assertEquals("search-poi-1", marker.key)
        assertEquals(candidate.point, marker.point)
        assertEquals("故宫博物院", marker.label)
        assertEquals(MapMarkerKind.UNSAVED_SEARCH, marker.kind)
        assertTrue(marker.isFocused)
        assertTrue(marker.occurrences.isEmpty())
        assertTrue(model.polylines.isEmpty())
        assertTrue(model.routeLabels.isEmpty())
        assertTrue(model.corruptRoutes.isEmpty())
        assertEquals("search-poi-1", model.highlightedMarkerKey)
    }

    @Test fun candidateWithoutCoordinatesProducesNoMapModel() {
        assertNull(searchDetailMapModel(candidate.copy(point = null), requestId = 1L))
    }

    @Test fun selectedCandidateProducesSearchFocusViewportRequest() {
        val request = requireNotNull(searchDetailMapModel(candidate, requestId = 7L)).viewportRequest

        assertEquals(7L, request?.id)
        assertEquals(ViewportReason.SEARCH_FOCUS, request?.reason)
        assertEquals(listOf(candidate.point), request?.points)
        assertEquals(SEARCH_FOCUS_ZOOM, request?.singlePointZoom)
    }
}
