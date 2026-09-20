package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMarkerRenderingPolicyTest {
    @Test fun `scheduled place pool marker uses number without bookmark`() {
        val rendering = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL, scheduled = true),
        )

        assertEquals(0xFF2D5E3A.toInt(), rendering.backgroundColor)
        assertEquals(0xFFFFFFFF.toInt(), rendering.foregroundColor)
        assertEquals(emptyList<NormalizedPoint>(), rendering.geometry)
        assertTrue(rendering.solid)
    }

    @Test fun `saved only place pool marker uses white surface with primary bookmark and border`() {
        val rendering = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL),
        )

        assertEquals(0xFFFFFFFF.toInt(), rendering.backgroundColor)
        assertEquals(0xFF2D5E3A.toInt(), rendering.foregroundColor)
        assertEquals(0xFF2D5E3A.toInt(), rendering.borderColor)
        assertEquals(BookmarkGeometry, rendering.geometry)
        assertTrue(!rendering.solid)
    }

    @Test fun `unsaved search marker keeps dot glyph without bookmark geometry`() {
        val rendering = mapMarkerRendering(
            MapMarkerUi("search-1", point(), "地点", emptyList(), MapMarkerKind.UNSAVED_SEARCH),
        )

        assertEquals("●", rendering.glyph)
        assertEquals(emptyList<NormalizedPoint>(), rendering.geometry)
    }

    @Test fun `itinerary marker uses only its ordinal glyph`() {
        val rendering = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_ITINERARY, badgeText = "1·3", scheduled = true),
        )

        assertEquals(emptyList<NormalizedPoint>(), rendering.geometry)
        assertEquals("1·3", rendering.glyph)
    }

    @Test fun `focus changes emphasis without changing scheduled fill semantics`() {
        val normal = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL, scheduled = true),
        )
        val focused = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL, isFocused = true, scheduled = true),
        )

        assertEquals(normal.backgroundColor, focused.backgroundColor)
        assertEquals(normal.foregroundColor, focused.foregroundColor)
        assertTrue(focused.borderWidth > normal.borderWidth)
    }

    @Test fun `focused marker renders above stable nonfocused order for same coordinate clicks`() {
        val focused = MapMarkerUi(
            "place-a",
            point(),
            "地点 A",
            emptyList(),
            MapMarkerKind.SAVED_PLACE_POOL,
            isFocused = true,
            savedPlaceId = "a",
        )
        val peer = MapMarkerUi(
            "place-b",
            point(),
            "地点 B",
            emptyList(),
            MapMarkerKind.SAVED_PLACE_POOL,
            savedPlaceId = "b",
        )

        assertEquals(listOf(peer, focused), markerRenderOrder(listOf(focused, peer)))
    }

    private fun point() = com.yangchengwei.easytrip.core.model.GeoPoint(39.9, 116.4)
}
