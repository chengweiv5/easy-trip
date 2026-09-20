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
                val expected = if (scheduled) 0xFF2D5E3A.toInt() else Color.WHITE
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

    @Test fun scheduledPlacePoolAndItineraryHaveIdenticalBoldNameBitmaps() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (focused in listOf(false, true)) {
            val marker = MapMarkerUi("place", GeoPoint(30.25, 120.15), "西湖天地", emptyList(),
                MapMarkerKind.SAVED_PLACE_POOL, badgeText = "1", scheduled = true, isFocused = focused)
            val pool = render(MarkerIconView(context, marker))
            val itinerary = render(MarkerIconView(context, marker.copy(kind = MapMarkerKind.SAVED_ITINERARY)))
            assertTrue("Both tabs must use the same bold name rendering", pool.sameAs(itinerary))
            pool.recycle()
            itinerary.recycle()
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
