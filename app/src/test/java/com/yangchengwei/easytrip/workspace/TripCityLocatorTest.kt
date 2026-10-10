package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.*
import org.junit.Test

class TripCityLocatorTest {
    private val hangzhou = TripCity("330100", "杭州市", GeoPoint(30.27, 120.15))
    private val suzhou = TripCity("320500", "苏州市", GeoPoint(31.30, 120.58))
    private val catalog = listOf(suzhou, hangzhou)

    @Test fun usesFirstCityInTitleRatherThanCatalogOrder() {
        assertEquals(hangzhou, firstTripCity("周末杭州·苏州五日游", catalog))
        assertEquals(suzhou, firstTripCity("苏州市 → 杭州市", catalog))
        assertNull(firstTripCity("周末去远一点", catalog))
    }

    @Test fun longestCityNameWinsAndAmbiguousShortNamesAreNotGuessed() {
        val jilin = TripCity("220200", "吉林市", GeoPoint(43.8, 126.5))
        assertNull(firstTripCity("吉林省之旅", listOf(jilin)))
        assertEquals(jilin, firstTripCity("吉林市之旅", listOf(jilin)))
        assertNull(firstTripCity("苏州游", catalog + suzhou.copy(code = "other")))
    }
}
