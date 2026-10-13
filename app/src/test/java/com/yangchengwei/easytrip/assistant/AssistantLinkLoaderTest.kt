package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AssistantLinkLoaderTest {
    private val input = AssistantLinkInput.parse("https://m.ctrip.com/webapp/you/community/detail?articleId=123")!!
    private fun document(count: Int) = AssistantLinkDocument(input.platform, input.url, "青州", "青州古城",
        (1..count).map { "https://dimg04.c-ctrip.com/images/$it.jpg" }, count)
    private class Store : AssistantImageStore {
        val added = mutableListOf<AssistantImage>()
        val removed = mutableListOf<String>()
        override suspend fun importBytes(sessionId: String, bytes: ByteArray, existing: List<AssistantImage>): AssistantImage {
            AssistantImagePolicy.rejection(existing.size, existing.sumOf { it.byteCount }, bytes.size.toLong())
                ?.let { throw AssistantFailure(it) }
            return AssistantImage("image${added.size}", existing.size + 1, "image/png", bytes.size.toLong(), 80, 60).also { added += it }
        }
        override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>) = error("unused")
        override suspend fun prepare(sessionId: String, imageId: String) = PreparedImage(imageId, "image/jpeg", byteArrayOf(1))
        override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("unused")
        override suspend fun remove(sessionId: String, imageId: String) { removed += imageId }
        override suspend fun release(sessionId: String) {}
    }
    @Test fun downloadsAllTwentyInOrderAndDoesNotExposeShareUrlToModel() = runTest {
        val store = Store()
        val requested = mutableListOf<String>()
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(20) }, store,
            AssistantLinkImageFetcher { _, url -> requested += url; byteArrayOf(1) })
        val result = loader.load("session", input, emptyList()) {}
        assertEquals(20, result.images.size)
        assertEquals(document(20).imageUrls, requested)
        assertEquals((1..20).toList(), result.images.map { it.ordinal })
        assertTrue(result.modelText.contains("青州古城"))
        assertFalse(result.modelText.contains("https://"))
        assertTrue(store.removed.isEmpty())
    }
    @Test fun failedOrCancelledDownloadsRollBackOnlyNewImages() = runTest {
        for (cancel in listOf(false, true)) {
            val store = Store()
            var calls = 0
            val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(3) }, store,
                AssistantLinkImageFetcher { _, _ ->
                    if (++calls == 3) {
                        if (cancel) throw CancellationException()
                        throw AssistantFailure("download failed")
                    }
                    byteArrayOf(1)
                })
            try {
                loader.load("session", input, listOf(AssistantImage("previous", 1, "image/png", 1, 80, 60))) {}
                fail("should fail atomically")
            } catch (_: CancellationException) {} catch (_: AssistantFailure) {}
            assertEquals(listOf("image0", "image1"), store.removed)
            assertFalse("previous" in store.removed)
        }
    }
    @Test fun countOverflowIsRejectedBeforeAnyDownload() = runTest {
        val store = Store()
        var downloads = 0
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(20) }, store,
            AssistantLinkImageFetcher { _, _ -> downloads++; byteArrayOf(1) })
        try {
            loader.load("session", input, listOf(AssistantImage("previous", 1, "image/png", 1, 80, 60))) {}
            fail("total 21 must be rejected")
        } catch (_: AssistantFailure) {}
        assertEquals(0, downloads)
        assertTrue(store.added.isEmpty())
    }
}
