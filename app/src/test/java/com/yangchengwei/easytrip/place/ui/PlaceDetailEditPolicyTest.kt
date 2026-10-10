package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.domain.PlaceCategory
import org.junit.Assert.*
import org.junit.Test

class PlaceDetailEditPolicyTest {
    @Test fun categoryAndUnaddedTagAreUnsavedChanges() {
        val original = PlaceDetailValues(PlaceCategory.OTHER, "", emptySet())
        assertFalse(hasPlaceDetailChanges(original, original, ""))
        assertTrue(hasPlaceDetailChanges(original.copy(category = PlaceCategory.FOOD), original, ""))
        assertTrue(hasPlaceDetailChanges(original, original, "朋友推荐"))
        assertEquals(PlaceDetailCloseDecision.CONFIRM_DISCARD, placeDetailCloseDecision(false, true))
        assertEquals(PlaceDetailCloseDecision.IGNORE, placeDetailCloseDecision(true, true))
        assertEquals(PlaceDetailCloseDecision.CLOSE, placeDetailCloseDecision(false, false))
    }
}
