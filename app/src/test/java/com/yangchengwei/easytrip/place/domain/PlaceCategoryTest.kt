package com.yangchengwei.easytrip.place.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceCategoryTest {
    @Test fun onlyFiveCategoriesAndOldPlacesDefaultToOther() {
        assertEquals(listOf("景点", "住宿", "餐饮", "交通", "其他"), PlaceCategory.entries.map { it.label })
        assertEquals(listOf("attraction", "lodging", "food", "transport", "other"),
            PlaceCategory.entries.map { it.storageKey })
        listOf(null, "", "unknown").forEach {
            assertEquals(PlaceCategory.OTHER, PlaceCategory.fromStorageKey(it))
        }
        assertEquals(PlaceCategory.OTHER,
            SavedPlace("p", "t", "poi", "地点", "", GeoPoint(1.0, 2.0), "", emptyList()).category)
    }

    @Test fun everyStoredCategoryRoundTrips() {
        PlaceCategory.entries.forEach { assertEquals(it, PlaceCategory.fromStorageKey(it.storageKey)) }
    }
}
