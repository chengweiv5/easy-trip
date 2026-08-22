package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapInteractionReducerTest {
    private val first = GeoPoint(39.9, 116.4)
    private val second = GeoPoint(31.2, 121.5)

    @Test fun `clicking a search result highlights its marker and emits one focus request`() {
        val state = reduceMapInteraction(
            MapInteractionState(),
            MapInteractionAction.FocusSearchResult("poi-2", first),
        )

        assertEquals("search-poi-2", state.highlightedMarkerKey)
        assertEquals(1L, state.viewportRequest?.id)
        assertEquals(ViewportReason.SEARCH_FOCUS, state.viewportRequest?.reason)
        assertEquals(listOf(first), state.viewportRequest?.points)
        assertEquals(SEARCH_FOCUS_ZOOM, state.viewportRequest?.singlePointZoom)
    }

    @Test fun `each explicit result click increments request exactly once and replaces highlight`() {
        val firstClick = reduceMapInteraction(
            MapInteractionState(),
            MapInteractionAction.FocusSearchResult("poi-1", first),
        )
        val secondClick = reduceMapInteraction(
            firstClick,
            MapInteractionAction.FocusSearchResult("poi-2", second),
        )

        assertEquals(firstClick.viewportRequest!!.id + 1, secondClick.viewportRequest?.id)
        assertEquals("search-poi-2", secondClick.highlightedMarkerKey)
        assertEquals(listOf(second), secondClick.viewportRequest?.points)
    }

    @Test fun `removing focused result clears highlight without issuing a viewport request`() {
        val focused = reduceMapInteraction(
            MapInteractionState(),
            MapInteractionAction.FocusSearchResult("poi-2", first),
        )

        val reconciled = reduceMapInteraction(
            focused,
            MapInteractionAction.ReconcileSearchResults(setOf("poi-1")),
        )

        assertNull(reconciled.focusedPoiId)
        assertNull(reconciled.highlightedMarkerKey)
        assertEquals(focused.viewportRequest, reconciled.viewportRequest)
    }
}
