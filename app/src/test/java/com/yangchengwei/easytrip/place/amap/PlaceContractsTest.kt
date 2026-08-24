package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceContractsTest {
    @Test fun blankIdsAndTitlesAreFiltered() {
        val result = parsePlaces(listOf(RawPlace("", "bad", "", GeoPoint(1.0, 2.0), null), RawPlace("id", "", "", GeoPoint(1.0, 2.0), null), RawPlace("id", "ok", "", GeoPoint(1.0, 2.0), null)))
        assertEquals(listOf("ok"), result.map { it.name })
    }

    @Test fun placesWithoutCoordinatesRemainVisible() {
        val result = parsePlaces(listOf(RawPlace("id", "无坐标地点", "地址", null, null)))
        assertEquals(1, result.size)
        assertNull(result.single().point)
    }

    @Test fun emptyPlacesReturnEmptyResults() {
        assertEquals(emptyList<PlaceCandidate>(), parsePlaces(emptyList()))
    }
}
