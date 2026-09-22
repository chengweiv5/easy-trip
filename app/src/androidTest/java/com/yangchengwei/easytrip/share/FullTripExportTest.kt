package com.yangchengwei.easytrip.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

internal fun multiDayShareFixture(count: Int): ShareTrip {
    val source = shareFixture().days.first()
    return shareFixture().copy(days = (0 until count).map { index ->
        source.copy(id = "day-$index", index = index, date = source.date?.plusDays(index.toLong()),
            stops = (0 until 6).map { number ->
                source.stops[number % 3].copy(id = "$index-$number", number = number + 1,
                    name = "第 ${index + 1} 天 · 第 ${number + 1} 站",
                    note = "完整保留当天的预约、入口和休息安排。\n出发前再次确认开放时间。")
            })
    })
}

class FullTripExportTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun originalFourteenDayReproductionNowGenerates(): Unit = runBlocking {
        val source = shareFixture().days.first()
        val trip = shareFixture().copy(days = (0 until 14).map { index ->
            source.copy(id = "day-$index", index = index, date = source.date?.plusDays(index.toLong()),
                stops = source.stops.map { it.copy(id = "$index-${it.id}") })
        })
        val file = File(context.cacheDir, "share-original-fourteen-days.png")
        val renderer = ShareImageRenderer()
        val measured = renderer.render(trip, ShareOptions(), emptyMap(), file, measureOnly = true)
        val image = renderer.render(trip, ShareOptions(), emptyMap(), file)
        assertEquals(measured.height, image.height)
        assertTrue(file.length() > 0)
        file.delete()
    }

    @Test fun drawingFailureRemovesPartialPng(): Unit = runBlocking {
        val file = File(context.cacheDir, "share-failed.png")
        val missing = File(context.cacheDir, "missing-map-${java.util.UUID.randomUUID()}.png")
        try {
            ShareImageRenderer().render(shareFixture(), ShareOptions(), mapOf("d0" to ShareDayMap(missing)), file)
            fail("expected missing map failure")
        } catch (_: IllegalStateException) {
            assertFalse("failed output must be removed", file.exists())
        }
    }

    @Test fun fourteenDaysRemainOneCompleteImage(): Unit = runBlocking { verifyCompleteImage(14) }
    @Test fun thirtyDaysRemainOneCompleteImage(): Unit = runBlocking { verifyCompleteImage(30) }

    private suspend fun verifyCompleteImage(days: Int) {
        val trip = multiDayShareFixture(days)
        val map = File(context.cacheDir, "long-image-map.png")
        val mapColor = Color.rgb(200, 225, 215)
        Bitmap.createBitmap(684, 342, Bitmap.Config.ARGB_8888).apply {
            eraseColor(mapColor)
            map.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val maps = trip.days.associate { it.id to ShareDayMap(map) }
        val file = File(context.getExternalFilesDir(null), "share-$days-days.png")
        val renderer = ShareImageRenderer()
        val measured = renderer.render(trip, ShareOptions(), maps, file, measureOnly = true)
        assertTrue("fixture must exceed old 30000px ceiling", measured.height > 30_000)
        val image = renderer.render(trip, ShareOptions(), maps, file)
        assertEquals(measured.height, image.height)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        assertEquals(1080, bounds.outWidth)
        assertEquals(image.height, bounds.outHeight)
        @Suppress("DEPRECATION")
        val decoder = requireNotNull(BitmapRegionDecoder.newInstance(file.absolutePath, false))
        try {
            var mapCount = 0
            var inMap = false
            var mapRows = 0
            var footerInk = false
            // Decode every region: catches invalid PNG chunks, missing days and strip seams.
            for (top in 0 until image.height step 1200) {
                val height = minOf(1200, image.height - top)
                val bitmap = requireNotNull(decoder.decodeRegion(Rect(0, top, image.width, top + height), BitmapFactory.Options()))
                if (top == 0) assertEquals(Color.rgb(8,111,118), bitmap.getPixel(0,0))
                for (y in 0 until height) {
                    val isMap = bitmap.getPixel(500, y) == mapColor
                    if (isMap && !inMap) { mapCount++; mapRows = 0 }
                    if (isMap) mapRows++
                    if (!isMap && inMap) assertTrue("map must be continuous across strips", mapRows >= 470)
                    inMap = isMap
                    if (top + y in image.height - 180 until image.height - 40) {
                        footerInk = footerInk || (250 until 900 step 5).any { x -> bitmap.getPixel(x,y) != Color.WHITE }
                    }
                }
                if (top + height == image.height) assertEquals(Color.WHITE, bitmap.getPixel(0, height - 1))
                bitmap.recycle()
            }
            assertEquals("every day's map must be present", days, mapCount)
            assertTrue("footer must be present after last day", footerInk)
        } finally { decoder.recycle(); map.delete() }
    }

    @Test fun cancellationRemovesPartiallyWrittenPng(): Unit = runBlocking {
        val file = File(context.cacheDir, "share-cancelled.png")
        file.delete()
        val job = launch { ShareImageRenderer().render(multiDayShareFixture(30), ShareOptions(), emptyMap(), file) }
        withTimeout(20_000) { while (file.length() < 4096) delay(5) }
        assertTrue("cancel during generation", job.isActive)
        job.cancelAndJoin()
        assertFalse("cancelled PNG must not be offered for sharing", file.exists())
    }
}
