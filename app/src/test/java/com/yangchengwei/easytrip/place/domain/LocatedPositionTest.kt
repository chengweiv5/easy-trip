package com.yangchengwei.easytrip.place.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import org.junit.Assert.*
import org.junit.Test

class LocatedPositionTest {
    @Test fun distanceUsesMetersKilometersAndExplicitStraightLineLabel() {
        val here = GeoPoint(30.0, 120.0)
        assertEquals("距我 100 米内 · 直线", distanceFromMeLabel(here, here))
        assertEquals("距我 560 米 · 直线", distanceFromMeLabel(here, GeoPoint(30.005, 120.0)))
        assertEquals("距我 1.1 公里 · 直线", distanceFromMeLabel(here, GeoPoint(30.01, 120.0)))
        assertEquals("距我 111.2 公里 · 直线", distanceFromMeLabel(here, GeoPoint(31.0, 120.0)))
    }

    @Test fun missingInvalidOrZeroCoordinatesDoNotInventDistance() {
        for (invalid in listOf(null, GeoPoint(0.0, 0.0), GeoPoint(Double.NaN, 120.0), GeoPoint(91.0, 120.0), GeoPoint(30.0, Double.POSITIVE_INFINITY))) {
            assertNull(distanceFromMeLabel(invalid, GeoPoint(30.0, 120.0)))
            assertNull(distanceFromMeLabel(GeoPoint(30.0, 120.0), invalid))
        }
    }

    @Test fun cityUsesAdministrativeIdentityRatherThanSharedTelephoneCode() {
        val result = PlaceCandidate("p", "少林寺", "", GeoPoint(34.5, 113.0), "0371", "登封市", "410185", 1)
        assertTrue(result.isInCity(PlaceCity("登封市", "410185")))
        assertFalse(result.isInCity(PlaceCity("郑州市", "410100")))
        assertFalse(result.copy(cityName = "登封市", cityAdCode = "410100").isInCity(PlaceCity("登封市", "410185")))
        assertTrue(result.copy(cityAdCode = null).isInCity(PlaceCity("登封市", null)))
        assertFalse(result.copy(cityName = null, cityAdCode = null).isInCity(PlaceCity("登封市", "410185")))
    }

    @Test fun legacyDistrictMetadataNormalizesToCityAndMunicipality() {
        val result = PlaceCandidate("p", "地点", "", GeoPoint(30.0, 120.0), null, "杭州市", "330106")
        assertTrue(result.isInCity(PlaceCity("杭州市", "330100")))
        assertTrue(result.copy(cityName = "北京市", cityAdCode = "110101").isInCity(PlaceCity("北京市", "110000")))
        assertFalse(result.isInCity(PlaceCity("宁波市", "330200")))
    }
}
