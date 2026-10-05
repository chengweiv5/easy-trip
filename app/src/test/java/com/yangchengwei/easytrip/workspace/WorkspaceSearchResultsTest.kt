package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import org.junit.Assert.*
import org.junit.Test

class WorkspaceSearchResultsTest {
    @Test fun snapshotRoundTripPreservesResultsWithoutCoordinatesAndCityMetadata() {
        val snapshot = WorkspaceSearchResults("西湖", listOf(
            PlaceCandidate("a", "甲", "街道\n1号", GeoPoint(30.2, 120.1), "0571", "杭州市", "330100", 1),
            PlaceCandidate("b", "乙", "", null, null),
        ))
        assertEquals(snapshot, WorkspaceSearchResults.restore(snapshot.save()))
        assertEquals(listOf("a"), snapshot.mappedPlaces.map { it.poiId })
        assertNull(WorkspaceSearchResults.restore(listOf("broken", "one field")))
    }

    @Test fun invalidCoordinatesAreNeverSentToMapAndRepeatedPoiIdsAreDeduplicated() {
        val snapshot = WorkspaceSearchResults("公园", listOf(
            PlaceCandidate("a", "甲", "", GeoPoint(30.0, 120.0), null),
            PlaceCandidate("a", "重复", "", GeoPoint(30.0, 120.0), null),
            PlaceCandidate("b", "无效", "", GeoPoint(Double.NaN, 120.0), null),
            PlaceCandidate("c", "越界", "", GeoPoint(91.0, 120.0), null),
        ))
        assertEquals(listOf("a"), snapshot.mappedPlaces.map { it.poiId })
    }

    @Test fun drawerGeometryNeverChangesSearchFitAreaOrSummaryPosition() {
        val layouts = WorkspaceSheetLevel.entries.map { level ->
            val metrics = workspaceLayoutMetrics(782.dp, workspaceSheetAnchors(782.dp), level, availableWidth = 390.dp)
            workspaceSearchMapLayout(metrics, 3f) to workspaceSearchMapMetrics(metrics)
        }
        assertTrue(layouts.all { it == layouts.first() })
        assertEquals(96, layouts.first().first.fitInsets.leftPx)
        assertEquals(192, layouts.first().first.fitInsets.rightPx)
    }
}
