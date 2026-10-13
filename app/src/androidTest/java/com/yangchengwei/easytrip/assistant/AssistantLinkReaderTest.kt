package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.webkit.CookieManager
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class AssistantLinkReaderTest {
    @Test fun twentyImportedImagesCanEachBePreparedAndTwentyFirstIsRejected() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "link-store-test-${UUID.randomUUID()}")
        val session = UUID.randomUUID().toString()
        try {
            val store = AndroidAssistantImageStore(context, root)
            val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            val images = mutableListOf<AssistantImage>()
            repeat(20) { images += store.importBytes(session, output.toByteArray(), images) }
            assertEquals((1..20).toList(), images.map { it.ordinal })
            images.forEach { assertTrue(store.prepare(session, it.id).bytes.isNotEmpty()) }
            try {
                store.importBytes(session, output.toByteArray(), images)
                fail("21st image must be rejected")
            } catch (_: AssistantFailure) {}
            store.release(session)
            assertFalse(File(root, session).exists())
        } finally { root.deleteRecursively() }
    }

    /** Opt-in only, isolated emulator; no model, POI or collection writes. */
    @Test fun originalShareLinkReadsBodyAndAllSixImagesUsingProductionPipeline() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveAssistantLink") == "true")
        val input = requireNotNull(AssistantLinkInput.parse(requireNotNull(args.getString("shareUrl"))))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "link-live-test-${UUID.randomUUID()}")
        val session = UUID.randomUUID().toString()
        try {
            // Clean anonymous browser control, restricted to this opt-in isolated test.
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    CookieManager.getInstance().removeAllCookies { if (continuation.isActive) continuation.resume(Unit) }
                }
            }
            val reader = AndroidAssistantLinkReader(context) { android.util.Log.i("AssistantLinkTest", it) }
            val document = reader.read(input).validated(input)
            File(context.cacheDir, "assistant-link-test-${input.platform}.txt")
                .writeText(document.title + "\n" + document.body)
            assertEquals(6, document.expectedImages)
            assertEquals(6, document.imageUrls.size)
            assertTrue(document.body.contains("青州"))
            assertTrue(document.body.contains("井塘古村"))
            assertFalse(document.body.contains("猜你喜欢"))
            assertFalse(document.body.contains("共 63 条评论"))
            assertFalse(document.body.contains("携程推荐"))
            if (input.platform == AssistantLinkPlatform.CTRIP)
                assertEquals(1, Regex("挖到宝藏了").findAll(document.body).count())
            val store = AndroidAssistantImageStore(context, root)
            val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document }, store)
            val loaded = loader.load(session, input, emptyList()) {}
            assertEquals(6, loaded.images.size)
            loaded.images.forEach {
                val prepared = store.prepare(session, it.id)
                assertTrue(prepared.bytes.isNotEmpty())
                if (args.getString("retainLastImageEvidence") == "true" && it.ordinal == 6)
                    File(context.cacheDir, "assistant-link-last-image.jpg").writeBytes(prepared.bytes)
            }
            android.util.Log.i("AssistantLinkTest", "PASS platform=${input.platform} title=${document.title} bodyChars=${document.body.length} images=${loaded.images.size} bytes=${loaded.images.sumOf { it.byteCount }}")
            loader.discard(session, loaded.images)
        } finally { root.deleteRecursively() }
    }
}
