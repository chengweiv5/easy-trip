package com.yangchengwei.easytrip.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.yangchengwei.easytrip.workspace.*
import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.*
import org.junit.Test

class ThemePaletteTest {
    @Test fun palettesMeetContrastAndKeepSemanticColors() {
        val routeColors = listOf(0xFF2766AA, 0xFFAD5C2D, 0xFF287A72, 0xFF82549B, 0xFFB74562, 0xFF667526, 0xFF197C92, 0xFF6958AE)
        ThemePalette.entries.forEach { palette ->
            val c = palette.colors
            listOf(c.onBackground to c.background, c.onSurfaceVariant to c.background,
                c.onSurfaceVariant to c.primaryContainer, Color.White to c.primary,
                c.primary to c.primaryContainer, c.tertiary to c.surface).forEach { (fg, bg) ->
                val a = fg.luminance(); val b = bg.luminance()
                assertTrue("${palette.id}: $fg on $bg", (maxOf(a,b)+.05)/(minOf(a,b)+.05) >= 4.5)
            }
            assertEquals(EasyTripDanger, c.error)
            assertEquals(EasyTripErrorSurface, c.errorContainer)
            assertEquals(routeColors, routePalette())
        }
    }

    @Test fun nativeMarkersUseCurrentThemeAndKeepDayBadgeSegments() {
        ThemePalette.entries.forEach { palette ->
            val marker = MapMarkerUi("place", GeoPoint(30.2,120.1), "西湖", emptyList(), MapMarkerKind.SAVED_PLACE_POOL)
            assertEquals(palette.colors.primary.toArgb(), mapMarkerRendering(marker,palette).foregroundColor)
            assertEquals(palette.colors.tertiary.toArgb(), mapMarkerRendering(marker.copy(isFocused=true),palette).borderColor)
            val scheduled = marker.copy(kind=MapMarkerKind.SAVED_ITINERARY, scheduled=true, badgeSegments=listOf(MapMarkerBadgeSegment("2", routeColorForDay(1))))
            assertEquals(Color.White.toArgb(), mapMarkerRendering(scheduled,palette).foregroundColor)
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            val view = MarkerIconView(context, scheduled, palette)
            val bitmap = android.graphics.Bitmap.createBitmap(view.layoutParams.width, view.layoutParams.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.layout(0,0,bitmap.width,bitmap.height)
            view.draw(android.graphics.Canvas(bitmap))
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
            assertTrue("Day badge must retain its route color in ${palette.id}", pixels.count { it == routeColorForDay(1).toInt() } > 50)
            bitmap.recycle()
        }
    }
}
