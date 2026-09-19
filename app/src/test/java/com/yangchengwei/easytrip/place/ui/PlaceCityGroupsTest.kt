package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.RawPlace
import com.yangchengwei.easytrip.place.amap.parsePlaces
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.administrativeCity
import org.junit.Assert.*
import org.junit.Test

class PlaceCityGroupsTest {
    @Test fun districtsMergeIntoTheirAdministrativeCityAndTelephoneCodesDoNotDefineGroups() {
        assertEquals(administrativeCity("杭州市", "330106"), administrativeCity("杭州市", "330102"))
        val rows = listOf(row("西安", "西安市", "610102", "029"), row("咸阳", "咸阳市", "610402", "029"))
        assertEquals(setOf("610100", "610400"), placeCityGroups(rows).map { it.key }.toSet())
    }

    @Test fun municipalityDistrictsAndCountiesShareOneGroup() {
        assertEquals(administrativeCity("重庆城区", "500103"), administrativeCity("重庆郊县", "500236"))
        assertEquals("北京市", administrativeCity(null, "110101")?.name)
    }

    @Test fun directlyAdministeredCountiesKeepTheirNamesAndFullCodes() {
        val places = parsePlaces(listOf(
            RawPlace("a", "景点", "地址", null, "0391", "省直辖县级行政区划", "419001", "济源市"),
            RawPlace("b", "景点", "地址", null, "0993", "自治区直辖县级行政区划", "659001", "石河子市"),
        ))
        assertEquals(listOf("济源市", "石河子市"), places.map { it.cityName })
        assertEquals(listOf("419001", "659001"), places.map { it.cityAdCode })
    }

    @Test fun unknownCityRemainsVisibleLastAndCombinedSearchHonorsCity() {
        val rows = listOf(row("湖", "杭州市", "330106"), row("湖畔", "上海市", "310101"), row("无城市", null, null))
        assertEquals(UNKNOWN_CITY_KEY, placeCityGroups(rows).last().key)
        assertEquals(listOf("湖"), filterPlaceCityGroups(rows, "330100", "湖").flatMap { it.rows }.map { it.name })
        assertEquals(listOf("湖畔"), filterPlaceCityGroups(rows, null, "上海").flatMap { it.rows }.map { it.name })
        assertTrue(filterPlaceCityGroups(rows, "330100", "上海").isEmpty())
        assertEquals(3, placeCityGroups(rows).sumOf { it.rows.size })
    }

    private fun row(name: String, city: String?, code: String?, telephone: String? = null) = SavedPlaceRowUi(
        SavedPlace(name, "trip", name, name, "地址", GeoPoint(30.0, 120.0), "", emptyList(), city, code, telephone), 0, false,
    )
}
