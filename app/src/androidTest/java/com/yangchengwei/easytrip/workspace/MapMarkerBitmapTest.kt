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
                assertEquals(diameter, bitmap.width)
                assertEquals(diameter, bitmap.height)
                assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
                assertTrue(Color.alpha(bitmap.getPixel(diameter / 2, 0)) > 0)
                val interiorX = (6 * density).roundToInt()
                val expected = if (scheduled) 0xFF2D5E3A.toInt() else Color.WHITE
                assertEquals("scheduled=$scheduled focused=$focused", expected, bitmap.getPixel(interiorX, diameter / 2))
                bitmap.recycle()
            }
        }
    }

    @Test fun itineraryBadgeKeepsCompactCircleAndRendersBesideIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        val marker = MapMarkerUi(
            "place", GeoPoint(30.25, 120.15), "西湖", emptyList(),
            MapMarkerKind.SAVED_ITINERARY, isFocused = true, badgeText = "1·3", scheduled = true,
        )
        val bitmap = render(MarkerIconView(context, marker))
        assertEquals((56 * density).toInt(), bitmap.width)
        assertEquals((28 * density).toInt(), bitmap.height)
        assertEquals(0xFF2D5E3A.toInt(), bitmap.getPixel((30 * density).toInt(), bitmap.height / 2))
        bitmap.recycle()
    }

    private fun render(view: View): Bitmap {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
            view.draw(Canvas(it))
        }
    }
}
