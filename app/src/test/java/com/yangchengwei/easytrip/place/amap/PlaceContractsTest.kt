package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaceContractsTest {
    @Test fun blankIdsAndTitlesAreFiltered() {
        val result = parsePlaces(listOf(RawPlace("", "bad", "", GeoPoint(1.0, 2.0), null), RawPlace("id", "", "", GeoPoint(1.0, 2.0), null), RawPlace("id", "ok", "", GeoPoint(1.0, 2.0), null)), emptyList())
        assertEquals(listOf("ok"), result.map { it.name })
    }

    @Test fun emptyPlacesWithSuggestionsAreStructuredFailure() {
        val error = assertThrows(AmapServiceException::class.java) { parsePlaces(emptyList(), listOf("北京")) }
        assertEquals("POI_EMPTY", error.operation)
    }
}
