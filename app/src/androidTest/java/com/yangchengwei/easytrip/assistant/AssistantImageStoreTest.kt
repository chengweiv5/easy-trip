package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AssistantImageStoreTest {
    @Test fun emptySessionDoesNotClaimLostImageOnRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "image-test-${UUID.randomUUID()}")
        try {
            val first = AndroidAssistantImageStore(context, root)
            assertFalse(first.startSession(UUID.randomUUID().toString(), "text-only-trip"))
            val restarted = AndroidAssistantImageStore(context, root)
            assertFalse(restarted.startSession(UUID.randomUUID().toString(), "text-only-trip"))
        } finally { root.deleteRecursively() }
    }
    @Test fun selectedImageIsPrivatePreparedAndReleased() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "image-test-${UUID.randomUUID()}")
        val source = File(context.cacheDir, "fixture-${UUID.randomUUID()}.png")
        try {
            val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.RED)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val store = AndroidAssistantImageStore(context, root)
            val session = UUID.randomUUID().toString()
            val result = store.select(session, listOf(Uri.fromFile(source)), emptyList())
            assertTrue(result.rejected.isEmpty())
            val image = result.accepted.single()
            assertEquals("image/png", image.mimeType)
            val prepared = store.prepare(session, image.id)
            assertEquals("image/jpeg", prepared.mimeType)
            assertTrue(prepared.bytes.size <= AssistantImagePolicy.MAX_PREPARED_BYTES)
            assertFalse(prepared.toString().contains("base64"))
            store.release(session)
            assertFalse(File(root, session).exists())
        } finally { source.delete(); root.deleteRecursively() }
    }
    @Test fun rotationIsBakedIntoPixelsAndPrivateMetadataIsNotCopied() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "image-test-${UUID.randomUUID()}")
        val source = File(context.cacheDir, "fixture-${UUID.randomUUID()}.jpg")
        try {
            val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.BLUE)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bitmap.recycle()
            ExifInterface(source).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, "6")
                setAttribute(ExifInterface.TAG_USER_COMMENT, "private fixture comment")
                setLatLong(30.2, 120.1)
                saveAttributes()
            }
            val store = AndroidAssistantImageStore(context, root)
            val session = UUID.randomUUID().toString()
            val image = store.select(session, listOf(Uri.fromFile(source)), emptyList()).accepted.single()
            val prepared = store.prepare(session, image.id)
            val decoded = BitmapFactory.decodeByteArray(prepared.bytes, 0, prepared.bytes.size)
            assertEquals(40, decoded.width); assertEquals(80, decoded.height); decoded.recycle()
            val exif = ExifInterface(prepared.bytes.inputStream())
            assertNull(exif.latLong)
            assertNull(exif.getAttribute(ExifInterface.TAG_USER_COMMENT))
            assertTrue(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                in setOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL))
            store.release(session)
        } finally { source.delete(); root.deleteRecursively() }
    }
}
