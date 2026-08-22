package com.yangchengwei.easytrip.workspace

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSelectionConsumptionTest {
    @Test fun selectionIsConsumedOnceAndRemovedFromSavedState() {
        val payload = SearchSelectionPayload("poi-1", "故宫", "地址", 39.9, 116.4)
        val handle = SavedStateHandle(mapOf(SEARCH_SELECTION_RESULT to payload))
        val selected = mutableListOf<com.yangchengwei.easytrip.place.amap.PlaceCandidate>()

        assertTrue(consumeSearchSelection(handle, selected::add))
        assertFalse(consumeSearchSelection(handle, selected::add))
        assertEquals(listOf("poi-1"), selected.map { it.poiId })
        assertNull(handle.get<SearchSelectionPayload>(SEARCH_SELECTION_RESULT))
    }
}
