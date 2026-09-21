package com.yangchengwei.easytrip.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ShareImageExportTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun file(name:String) = File(context.getExternalFilesDir(null),name)

    @Test fun rendersFullLongNotesMapsAndFooterWithoutClipping() = runBlocking {
        val fixture=shareFixture()
        val map=file("share-fixture-map.png")
        val bitmap=Bitmap.createBitmap(684,342,Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(200,225,215))
        map.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        val maps=fixture.days.associate { it.id to ShareDayMap(map) }
        val renderer=ShareImageRenderer(24_000_000)
        val full=renderer.render(fixture,ShareOptions(),maps,file("share-full.png"))
        val hidden=renderer.render(fixture,ShareOptions(includeNotes=false),maps,file("share-hidden.png"))
        val day=renderer.render(fixture,ShareOptions("d1"),maps,file("share-day.png"))
        assertEquals(1080,full.width)
        assertTrue(full.height>hidden.height)
        assertTrue(hidden.height>day.height)
        val longTrip=fixture.copy(days=fixture.days.take(1).map { d -> d.copy(stops=d.stops.take(1).map { it.copy(note="长备注需要完整显示。".repeat(160)) }) })
        val long=renderer.render(longTrip,ShareOptions(),maps,file("share-long-note.png"))
        assertTrue(long.height>day.height)
        BitmapFactory.decodeFile(full.file.absolutePath).let { result ->
            assertEquals(full.height,result.height)
            assertEquals(Color.rgb(8,111,118),result.getPixel(0,0))
            assertEquals(Color.WHITE,result.getPixel(0,result.height-1))
            assertTrue("footer text near bottom", (result.height-180 until result.height-40).any { y ->
                (250 until 900 step 5).any { x -> result.getPixel(x,y)!=Color.WHITE }
            })
            assertTrue("map fixture pixels",(0 until result.height step 5).any { y -> result.getPixel(100,y)==Color.rgb(200,225,215) })
            result.recycle()
        }
    }

    @Test fun rejectsOversizeBeforeWritingPartialImage() = runBlocking {
        val output=file("share-too-long.png")
        try {
            ShareImageRenderer(10).render(shareFixture(),ShareOptions(),emptyMap(),output)
            fail("expected size limit")
        } catch(_:ShareImageTooLongException) { assertFalse(output.exists()) }
    }

    @Test fun providerAndGalleryContainExactlyThePreviewPng() = runBlocking {
        val output=ShareImageStorage.newOutput(context)
        val image=ShareImageRenderer().render(shareFixture().copy(days=shareFixture().days.take(1)),ShareOptions(),emptyMap(),output)
        val send=ShareImageStorage.shareIntent(context,image)
        assertEquals(Intent.ACTION_SEND,send.action)
        assertEquals("image/png",send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        @Suppress("DEPRECATION") val uri=send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content",uri.scheme)
        assertEquals(uri,send.clipData!!.getItemAt(0).uri)
        assertArrayEquals(output.readBytes(),context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
        val saved=ShareImageStorage.save(context,image)
        try {
            assertArrayEquals(output.readBytes(),context.contentResolver.openInputStream(saved)!!.use { it.readBytes() })
            context.contentResolver.query(saved,arrayOf(MediaStore.Images.Media.IS_PENDING,MediaStore.Images.Media.MIME_TYPE),null,null,null)!!.use {
                assertTrue(it.moveToFirst());assertEquals(0,it.getInt(0));assertEquals("image/png",it.getString(1))
            }
        } finally { context.contentResolver.delete(saved,null,null);output.delete() }
    }
}
