package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.graphics.toArgb
import com.yangchengwei.easytrip.core.ui.PlaceBadgeStyle
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMarkerRenderingPolicyTest {
    @Test fun `scheduled place pool marker keeps category and adds separate check`() {
        val rendering = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL, scheduled = true),
        )

        assertEquals(0xFFF0F3F4.toInt(), rendering.backgroundColor)
        assertEquals(0xFF596A70.toInt(), rendering.foregroundColor)
        assertEquals(emptyList<NormalizedPoint>(), rendering.geometry)
        assertTrue(rendering.showScheduledCheck)
        assertEquals("", rendering.glyph)
    }

    @Test fun `saved only place pool marker uses category color and icon`() {
        val rendering = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL),
        )

        assertEquals(0xFFF0F3F4.toInt(), rendering.backgroundColor)
        assertEquals(0xFF596A70.toInt(), rendering.foregroundColor)
        assertEquals(0xFF596A70.toInt(), rendering.borderColor)
        assertEquals(emptyList<NormalizedPoint>(), rendering.geometry)
        assertEquals(com.yangchengwei.easytrip.R.drawable.ic_place_other, rendering.categoryIconRes)
        assertTrue(!rendering.solid)
    }

    @Test fun `search without original badge uses the same category icon as saved places`() {
        for (kind in listOf(MapMarkerKind.UNSAVED_SEARCH, MapMarkerKind.SEARCH_RESULT)) {
            for (focused in listOf(false, true)) {
                val marker = MapMarkerUi("search-1", point(), "地点", emptyList(), kind, isFocused = focused)
                assertEquals(
                    mapMarkerRendering(marker.copy(kind = MapMarkerKind.SAVED_PLACE_POOL)),
                    mapMarkerRendering(marker),
                )
            }
        }
    }

    @Test fun `itinerary marker uses only its ordinal glyph`() {
        val rendering = mapMarkerRendering(
            MapMarkerUi("place-1", point(), "地点", emptyList(), MapMarkerKind.SAVED_ITINERARY, badgeText = "1·3", scheduled = true),
        )

        assertEquals(emptyList<NormalizedPoint>(), rendering.geometry)
        assertEquals("1·3", rendering.glyph)
    }

    @Test fun `assistant badges use original labels without icons in every palette`() {
        for (palette in ThemePalette.entries) {
            for (label in listOf("A", "AA", "CV", "A1", "CV1")) {
                for (focused in listOf(false, true)) {
                    val rendering = mapMarkerRendering(MapMarkerUi(
                        "candidate", point(), "地点", emptyList(), MapMarkerKind.UNSAVED_SEARCH,
                        badgeText = label, isFocused = focused,
                    ), palette)
                    assertEquals(label, rendering.glyph)
                    assertEquals(null, rendering.categoryIconRes)
                    assertEquals(palette.colors.primary.toArgb(), rendering.foregroundColor)
                    assertEquals(palette.colors.secondaryContainer.toArgb(), rendering.backgroundColor)
                    assertEquals(if (focused) 3 else 1, rendering.borderWidth)
                    assertTrue(!rendering.showScheduledCheck)
                }
            }
        }
    }

    @Test fun `one letter is 14dp two characters are 11dp and longer candidates fit`() {
        assertEquals(14f, PlaceBadgeStyle.textSizeDp("A"), 0f)
        for (label in listOf("AA", "CV", "A1")) {
            assertEquals(11f, PlaceBadgeStyle.textSizeDp(label), 0f)
        }
        assertEquals(11f, PlaceBadgeStyle.fittedTextSizeDp("AA", 16f), 0f)
        assertEquals(8.8f, PlaceBadgeStyle.fittedTextSizeDp("CV1", 25f), .01f)
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
