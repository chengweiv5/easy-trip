package com.yangchengwei.easytrip.workspace

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class MapMarkerBitmapTest {
    @Test fun exportActualMarkerArtworkForVisualReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        val width = (360 * density).toInt()
        val sheet = Bitmap.createBitmap(width, (210 * density).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(237, 242, 238))
        val markers = listOf(
            MapMarkerUi("a", GeoPoint(30.25, 120.15), "青州博物馆", emptyList(), MapMarkerKind.UNSAVED_SEARCH, badgeText = "A", isFocused = true),
            MapMarkerUi("aa", GeoPoint(30.25, 120.15), "青州古城", emptyList(), MapMarkerKind.UNSAVED_SEARCH, badgeText = "AA"),
            MapMarkerUi("cv1", GeoPoint(30.25, 120.15), "云门山", emptyList(), MapMarkerKind.UNSAVED_SEARCH, badgeText = "CV1", isFocused = true),
            MapMarkerUi("search", GeoPoint(30.25, 120.15), "西湖天地", emptyList(), MapMarkerKind.UNSAVED_SEARCH, isFocused = true),
            MapMarkerUi("saved", GeoPoint(30.25, 120.15), "已收藏地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL,
                category = com.yangchengwei.easytrip.place.domain.PlaceCategory.ATTRACTION),
            MapMarkerUi("scheduled", GeoPoint(30.25, 120.15), "已安排地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL, scheduled = true),
        )
        markers.forEachIndexed { index, marker ->
            val bitmap = render(MarkerIconView(context, marker))
            val cx = (60 + index % 3 * 120) * density
            canvas.drawBitmap(bitmap, cx - bitmap.width / 2f, (24 + index / 3 * 100) * density, null)
            bitmap.recycle()
        }
        val output = java.io.File(context.getExternalFilesDir(null), "marker-artwork.png")
        output.outputStream().use { assertTrue(sheet.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        sheet.recycle()
    }

    @Test fun searchAndAssistantBadgesStayCircularWithNamesAndCenteredAnchors() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        for (kind in listOf(MapMarkerKind.UNSAVED_SEARCH, MapMarkerKind.SEARCH_RESULT)) {
            for (badge in listOf(null, "A", "AA", "CV", "A1", "CV1")) {
                for (focused in listOf(false, true)) {
                    val view = MarkerIconView(context, MapMarkerUi(
                        "search", GeoPoint(30.25, 120.15), "西湖天地", emptyList(), kind,
                        badgeText = badge, isFocused = focused,
                    ))
                    val bitmap = render(view)
                    assertEquals("$kind/$badge height", (52 * density).toInt(), bitmap.height)
                    assertEquals(14f * density, view.anchorY * bitmap.height, 1f)
                    val y = (14 * density).toInt()
                    val visible = (0 until bitmap.width).filter { Color.alpha(bitmap.getPixel(it, y)) > 0 }
                    assertEquals("$kind/$badge circle width", 28 * density, (visible.last() - visible.first() + 1).toFloat(), 2f)
                    assertTrue("$kind/$badge name below circle", (30 * density).toInt().until(bitmap.height).any { row ->
                        (0 until bitmap.width).any { x -> Color.alpha(bitmap.getPixel(x, row)) > 0 }
                    })
                    bitmap.recycle()
                }
            }
        }
    }

    @Test fun ordinarySearchUsesSavedCategoryArtworkWithoutInventingABadge() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (palette in com.yangchengwei.easytrip.core.ui.theme.ThemePalette.entries) {
            for (kind in listOf(MapMarkerKind.UNSAVED_SEARCH, MapMarkerKind.SEARCH_RESULT)) {
                val marker = MapMarkerUi("search", GeoPoint(30.25, 120.15), "西湖天地", emptyList(), kind, isFocused = true)
                val search = render(MarkerIconView(context, marker, palette))
                val saved = render(MarkerIconView(context, marker.copy(kind = MapMarkerKind.SAVED_PLACE_POOL), palette))
                assertTrue("$palette/$kind", search.sameAs(saved))
                search.recycle()
                saved.recycle()
            }
        }
    }

    @Test fun searchLongNamesUseExistingEllipsisRule() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = MapMarkerUi("search", GeoPoint(30.25, 120.15), "一二三四五六七八九十一二三四五六七八九十", emptyList(),
            MapMarkerKind.UNSAVED_SEARCH, badgeText = "AA")
        val original = render(MarkerIconView(context, marker))
        val shortened = render(MarkerIconView(context, marker.copy(label = "一二三四五六七八九十一二三四五六七…")))
        assertTrue(original.sameAs(shortened))
        original.recycle()
        shortened.recycle()
    }

    @Test fun savedMarkersKeepLayerButtonDiameterAndVisibleFillWhenFocused() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        val diameter = (28 * density).toInt()
        for (scheduled in listOf(false, true)) {
            for (focused in listOf(false, true)) {
                val marker = MapMarkerUi(
                    "place", GeoPoint(30.25, 120.15), "西湖", emptyList(),
                    MapMarkerKind.SAVED_PLACE_POOL, isFocused = focused, scheduled = scheduled,
                )
                val view = MarkerIconView(context, marker)
                val bitmap = render(view)
                assertTrue(bitmap.width >= diameter)
                assertEquals((52 * density).toInt(), bitmap.height)
                assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
                assertTrue(Color.alpha(bitmap.getPixel(bitmap.width / 2, 0)) > 0)
                val interiorX = bitmap.width / 2 - (8 * density).roundToInt()
                val expected = 0xFFF0F3F4.toInt()
                assertEquals("scheduled=$scheduled focused=$focused", expected, bitmap.getPixel(interiorX, diameter / 2))
                bitmap.recycle()
            }
        }
    }

    @Test fun itineraryOrdinalRendersInsideSingleCircle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        val marker = MapMarkerUi(
            "place", GeoPoint(30.25, 120.15), "西湖", emptyList(),
            MapMarkerKind.SAVED_ITINERARY, isFocused = true, badgeText = "1·3", scheduled = true,
        )
        val bitmap = render(MarkerIconView(context, marker))
        assertTrue(bitmap.width >= (28 * density).toInt())
        assertEquals((52 * density).toInt(), bitmap.height)
        assertEquals(0, Color.alpha(bitmap.getPixel(0,0)))
        assertTrue(mapMarkerRendering(marker).geometry.isEmpty())
        bitmap.recycle()
    }

    @Test fun repeatedWholeTripOrdinalsKeep28DpBadgeHeight() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        for (badge in listOf("1·12·23", "1 +12", "1", "12")) {
            val view = MarkerIconView(context, MapMarkerUi(
                "repeated", GeoPoint(30.25, 120.15), "酒店", emptyList(),
                MapMarkerKind.SAVED_ITINERARY, badgeText = badge, scheduled = true,
            ))
            val bitmap = render(view)
            assertEquals("badge=$badge anchor center", 14f * density, view.anchorY * bitmap.height, 1f)
            assertEquals("badge=$badge total height with name", (52 * density).toInt(), bitmap.height)
            assertTrue("name must render below badge", (28 * density).toInt().until(bitmap.height).any { y ->
                (0 until bitmap.width).any { x -> Color.alpha(bitmap.getPixel(x, y)) != 0 }
            })
            bitmap.recycle()
        }
    }

    @Test fun scheduledPlacePoolAndItineraryKeepBoldNamesButHaveDifferentCategoryAndOrdinalBadges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (focused in listOf(false, true)) {
            val marker = MapMarkerUi("place", GeoPoint(30.25, 120.15), "西湖天地", emptyList(),
                MapMarkerKind.SAVED_PLACE_POOL, badgeText = "1", scheduled = true, isFocused = focused)
            val pool = render(MarkerIconView(context, marker))
            val itinerary = render(MarkerIconView(context, marker.copy(kind = MapMarkerKind.SAVED_ITINERARY)))
            assertTrue("Pool category must not be replaced by the itinerary ordinal", !pool.sameAs(itinerary))
            assertEquals(pool.height, itinerary.height)
            pool.recycle()
            itinerary.recycle()
        }
    }

    @Test fun allCategoriesRenderDistinctIconsAndItineraryAccessoryPreservesDateSegments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val poolBitmaps = com.yangchengwei.easytrip.place.domain.PlaceCategory.entries.map { category ->
            render(MarkerIconView(context, MapMarkerUi(
                "place", GeoPoint(30.25, 120.15), "分类地点", emptyList(), MapMarkerKind.SAVED_PLACE_POOL,
                scheduled = true, isFocused = true, category = category)))
        }
        poolBitmaps.forEachIndexed { i, bitmap ->
            poolBitmaps.drop(i + 1).forEach { assertTrue(!bitmap.sameAs(it)) }
            assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        }
        val marker = MapMarkerUi("day", GeoPoint(30.25, 120.15), "酒店", emptyList(),
            MapMarkerKind.SAVED_ITINERARY, badgeText = "1", scheduled = true,
            category = com.yangchengwei.easytrip.place.domain.PlaceCategory.LODGING,
            badgeSegments = listOf(MapMarkerBadgeSegment("1", 0xFF2766AA), MapMarkerBadgeSegment("2", 0xFFAD5C2D)))
        val view = MarkerIconView(context, marker)
        val bitmap = render(view)
        val density = context.resources.displayMetrics.density
        assertEquals((70 * density).toInt(), bitmap.height)
        assertEquals(14 * density, view.anchorY * bitmap.height, 1f)
        assertEquals("1", mapMarkerRendering(marker).glyph)
        assertTrue(mapMarkerRendering(marker).showCategoryAccessory)
        bitmap.recycle()
        poolBitmaps.forEach { it.recycle() }
    }

    @Test fun dateSegmentsRenderActualRouteColorsWhiteNumbersAndFocusOutline() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        for (focused in listOf(false, true)) {
            for (segments in listOf(
                listOf(MapMarkerBadgeSegment("1", routeColorForDay(0))),
                listOf(MapMarkerBadgeSegment("1", routeColorForDay(0)), MapMarkerBadgeSegment("1", routeColorForDay(1))),
                listOf(MapMarkerBadgeSegment("1", routeColorForDay(0)), MapMarkerBadgeSegment("3", routeColorForDay(1)), MapMarkerBadgeSegment("8", routeColorForDay(2))),
            )) {
                val marker = MapMarkerUi("place", GeoPoint(30.25, 120.15), "酒店", emptyList(),
                    MapMarkerKind.SAVED_ITINERARY, badgeText = "1", scheduled = true, isFocused = focused, badgeSegments = segments)
                val view = MarkerIconView(context, marker)
                val bitmap = render(view)
                assertEquals((52 * density).toInt(), bitmap.height)
                assertEquals(14f * density, view.anchorY * bitmap.height, 1f)
                val left = bitmap.width / 2f - segments.size * 28f * density / 2f
                segments.forEachIndexed { index, segment ->
                    val start = left + index * 28f * density
                    assertEquals(segment.colorArgb.toInt(), bitmap.getPixel((start + 7f * density).roundToInt(), (14f * density).roundToInt()))
                    assertTrue("white number in each date segment", (start + 10f * density).roundToInt().until((start + 19f * density).roundToInt()).any { x ->
                        (7f * density).roundToInt().until((21f * density).roundToInt()).any { y -> bitmap.getPixel(x, y) == Color.WHITE }
                    })
                }
                if (focused) assertEquals(0xFFA3532B.toInt(), bitmap.getPixel(bitmap.width / 2, density.roundToInt()))
                bitmap.recycle()
            }
        }
    }

    private fun render(view: View): Bitmap {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
            view.draw(Canvas(it))
        }
    }
}
