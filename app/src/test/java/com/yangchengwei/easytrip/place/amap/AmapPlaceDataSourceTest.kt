package com.yangchengwei.easytrip.place.amap

import com.amap.api.services.core.AMapException
import com.amap.api.services.core.SuggestionCity
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.yangchengwei.easytrip.amap.AmapServiceException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AmapPlaceDataSourceTest {
    @Test fun successfulEmptyPoisWithSuggestionsReturnEmptyResults() {
        val result = PoiResult.createPagedResult(
            PoiSearch.Query("不存在地点", "", ""),
            null,
            emptyList(),
            listOf(SuggestionCity("北京", "010", "110000", 1)),
            1,
            0,
            arrayListOf(),
        )

        assertEquals(emptyList<PlaceCandidate>(), parsePoiSearchResponse(result to AMapException.CODE_AMAP_SUCCESS))
    }

    @Test fun nonSuccessCodeRemainsPoiSearchFailure() {
        val error = assertThrows(AmapServiceException::class.java) {
            parsePoiSearchResponse(null to 10001)
        }

        assertEquals("POI_SEARCH", error.operation)
        assertEquals(10001, error.errorCode)
    }
}
