package com.yangchengwei.easytrip.workspace

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentLocationIconTest {
    @Test fun currentLocationUsesBlueDotWhiteRingHaloAndSeparateLabel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = CurrentLocationIconView(context)
        icon.measure(0, 0)
        icon.layout(0, 0, icon.measuredWidth, icon.measuredHeight)
        val bitmap = Bitmap.createBitmap(icon.width, icon.height, Bitmap.Config.ARGB_8888)
        icon.draw(Canvas(bitmap))
        val density = context.resources.displayMetrics.density
        val x = icon.width / 2
        val y = (20 * density).toInt()
        assertEquals(Color.rgb(23, 92, 211), bitmap.getPixel(x, y))
        assertEquals(Color.WHITE, bitmap.getPixel(x + (10 * density).toInt(), y))
        assertTrue(Color.alpha(bitmap.getPixel(x + (17 * density).toInt(), y)) in 30..60)
        assertEquals("我的位置", icon.contentDescription)
        assertTrue(icon.anchorY in 0f..1f)
        java.io.File(context.getExternalFilesDir(null), "current-location-icon.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
