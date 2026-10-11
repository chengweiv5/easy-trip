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

    @Test fun assistantAcceptsOnlyOneConfirmedCityInTripTitle() {
        val xuzhou = TripCity("320300", "徐州市", GeoPoint(34.26, 117.18))
        val cities = catalog + xuzhou
        assertEquals(xuzhou, uniqueTripCity("徐州", cities))
        assertEquals(xuzhou, uniqueTripCity("徐州周末游", cities))
        assertNull(uniqueTripCity("徐州·苏州", cities))
        assertNull(uniqueTripCity("周末出游", cities))
        assertNull(uniqueTripCity("徐州", cities + xuzhou.copy(code = "ambiguous")))
    }

    @Test fun searchCityMatchingUsesLongestNamesAndRejectsAmbiguousAliases() {
        val jilin = TripCity("220200", "吉林市", GeoPoint(43.8, 126.5))
        assertEquals(listOf(suzhou, hangzhou), citiesInText("苏州市→杭州→苏州", catalog))
        assertEquals(emptyList<TripCity>(), citiesInText("吉林省之旅", listOf(jilin)))
        assertEquals(listOf(jilin), citiesInText("吉林省·吉林市", listOf(jilin)))
        assertEquals(emptyList<TripCity>(), citiesInText("苏州", catalog + suzhou.copy(code = "ambiguous")))
        val changchun = TripCity("220100", "长春市", GeoPoint(43.8, 125.3))
        val yichang = TripCity("420500", "宜昌市", GeoPoint(30.7, 111.2))
        assertEquals(listOf(yichang, changchun), citiesInText("宜昌长春", listOf(changchun, yichang)))
    }
}
