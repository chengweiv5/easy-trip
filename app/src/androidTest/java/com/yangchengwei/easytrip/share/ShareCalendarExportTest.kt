package com.yangchengwei.easytrip.share

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ShareCalendarExportTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun output(name: String) = File(context.getExternalFilesDir(null), "calendar-$name.png")
    private data class Label(val value: String, val x: Float, val y: Float, val width: Float, val height: Float)
    private class Recorder : ShareCalendarDrawing {
        val labels = mutableListOf<Label>()
        var checked = 0f
        override fun text(value: String, x: Float, y: Float, width: Float, size: Float, color: Int, bold: Boolean, draw: Boolean): Float {
            val paint = TextPaint().apply { textSize = size;typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL) }
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width.toInt().coerceAtLeast(1))
                .setIncludePad(false).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(size * .35f, 1f).build()
            if (draw) labels += Label(value, x, y, width, layout.height.toFloat())
            return layout.height.toFloat()
        }
        override fun rect(x: Float, y: Float, width: Float, height: Float, color: Int) = Unit
        override fun outline(x: Float, y: Float, width: Float, height: Float, color: Int, dashed: Boolean) = Unit
        override fun checkpoint(bottom: Float) { checked = maxOf(checked,bottom) }
    }
    private fun assertLabelsInsideAndSeparated(recorder: Recorder, bottom: Float) {
        recorder.labels.forEach { label ->
            assertTrue("${label.value} exceeds width",label.x>=0 && label.x+label.width<=390)
            assertTrue("${label.value} exceeds height",label.y>=0 && label.y+label.height<=bottom)
        }
        for ((i,a) in recorder.labels.withIndex()) for (b in recorder.labels.drop(i+1)) {
            val overlap = a.x < b.x+b.width && b.x < a.x+a.width && a.y < b.y+b.height && b.y < a.y+a.height
            assertFalse("overlap: ${a.value} / ${b.value}",overlap)
        }
    }
    @Test fun actualTextMetricsFitCalendarWithDenseVisitsAndLongNames() {
        val fixture = shareFixture()
        val dense = fixture.copy(days = fixture.days.map { day ->
            day.copy(stops = day.stops.mapIndexed { i, stop ->
                stop.copy(
                    name = "超长地点名称及完整到访说明".repeat(8),
                    arrival = LocalTime.of(9, i * 5),
                    stayMinutes = if (i == 0) null else 5,
                )
            })
        })
        val variants = listOf(fixture, dense)
        variants.forEach { trip ->
            val recorder=Recorder()
            val end=ShareCalendarPainter(recorder).draw(projectShareCalendar(trip,ShareOptions()),0f)
            assertLabelsInsideAndSeparated(recorder,end)
            assertTrue(recorder.labels.any { it.value.contains("全程日历") })
            assertTrue(recorder.labels.any { it.value.contains("交通") })
        }
    }
    @Test fun generatedPngIncludesTrafficAndCalendarAndSharesMeasuredHeight(): Unit = runBlocking {
        val trip=shareFixture()
        val renderer=ShareImageRenderer(24_000_000)
        val image=renderer.render(trip,ShareOptions(),emptyMap(),output("normal"))
        val measured=renderer.render(trip,ShareOptions(),emptyMap(),output("measure"),measureOnly=true)
        assertEquals(measured.height,image.height)
        assertFalse(measured.file.exists())
        val bitmap=BitmapFactory.decodeFile(image.file.absolutePath)
        try {
            val grey=Color.rgb(228,235,237)
            assertTrue("traffic fill within calendar",(600 until minOf(3500,bitmap.height) step 2).any { y ->
                (140 until 950 step 3).any { x -> bitmap.getPixel(x,y)==grey }
            })
            assertEquals(Color.WHITE,bitmap.getPixel(0,bitmap.height-1))
        } finally { bitmap.recycle() }
    }
    @Test fun generatesSevenDaysConflictsAndSingleDayContinuation(): Unit = runBlocking {
        val fixture=shareFixture()
        val renderer=ShareImageRenderer(24_000_000)
        val week=fixture.copy(days=(0..6).map { n -> fixture.days[0].copy(id="week$n",index=n,date=fixture.days[0].date?.plusDays(n.toLong()),stops=fixture.days[0].stops.take(1).map { it.copy(id="visit$n",leg=null) }) })
        renderer.render(week,ShareOptions(),emptyMap(),output("week"))
        val conflict=fixture.copy(days=fixture.days.take(1).map { day -> day.copy(stops=day.stops.mapIndexed { i,s -> if(i==1)s.copy(arrival=LocalTime.of(10,35)) else s }) })
        renderer.render(conflict,ShareOptions(),emptyMap(),output("conflict"))
        val recorder=Recorder()
        val end=ShareCalendarPainter(recorder).draw(projectShareCalendar(conflict,ShareOptions()),0f)
        assertTrue(recorder.labels.any { "交通可能来不及" in it.value })
        assertLabelsInsideAndSeparated(recorder,end)
        val overnight=fixture.copy(days=fixture.days.take(2).mapIndexed { i,day -> day.copy(stops=if(i==1)emptyList() else day.stops.take(1).map { it.copy(arrival=LocalTime.of(23,30),stayMinutes=120,leg=null) }) })
        renderer.render(overnight,ShareOptions("d1"),emptyMap(),output("continuation"))
        val pending=fixture.copy(days=fixture.days.take(1).map { d -> d.copy(stops=d.stops.map { it.copy(arrival=null) }) })
        renderer.render(pending,ShareOptions(),emptyMap(),output("untimed"))
        val dense = fixture.copy(days = fixture.days.take(1).map { day ->
            day.copy(stops = day.stops.mapIndexed { i, stop ->
                stop.copy(name = "超长地点名称需要完整保留".repeat(4), arrival = LocalTime.of(9, i * 5), stayMinutes = 15)
            })
        })
        renderer.render(dense, ShareOptions(), emptyMap(), output("dense"))
    }
    @Test fun budgetIncludesCalendarBeforeAnyOutputIsWritten(): Unit = runBlocking {
        val trip=shareFixture().copy(days=shareFixture().days.take(1).map { d -> d.copy(stops=d.stops.take(1).map { it.copy(arrival=LocalTime.MIDNIGHT,stayMinutes=1440,leg=null) }) })
        val file=output("budget")
        try {
            ShareImageRenderer(2_000_000).render(trip,ShareOptions(),emptyMap(),file)
            fail("24 hour calendar must count against budget")
        } catch (_:ShareImageTooLongException) { assertFalse(file.exists()) }
    }
}
