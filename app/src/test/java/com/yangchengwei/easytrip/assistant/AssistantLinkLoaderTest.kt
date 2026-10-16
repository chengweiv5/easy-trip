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
        override suspend fun importSource(sessionId: String, sourceId: String, ordinal: Int,
            bytes: ByteArray, existing: List<AssistantImage>): AssistantImage {
            AssistantImagePolicy.rejection(existing.size, existing.sumOf { it.byteCount }, bytes.size.toLong())
                ?.let { throw AssistantFailure(it) }
            return AssistantImage(sourceId, ordinal, "image/png", bytes.size.toLong(), 80, 60).also { added += it }
        }
        override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>) = error("unused")
        override suspend fun prepare(sessionId: String, imageId: String) = PreparedImage(imageId, "image/jpeg", byteArrayOf(1))
        override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("unused")
        override suspend fun remove(sessionId: String, imageId: String) { removed += imageId }
        override suspend fun release(sessionId: String) {}
    }
    @Test fun readingTextDoesNotDownloadImages() = runTest {
        var calls = 0
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(3) }, Store(),
            AssistantLinkImageFetcher { _, _ -> calls++; byteArrayOf(1) })
        val loaded = loader.read(input, emptyList()) {}
        assertEquals(0, calls)
        assertEquals(3, loaded.imagePlan.assets.size)
        assertTrue(loaded.images.isEmpty())
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
    @Test fun badImageDoesNotRollBackGoodImagesAndRetryKeepsItsId() = runTest {
        val store = Store()
        var calls = 0
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(3) }, store,
            AssistantLinkImageFetcher { _, _ -> if (++calls == 2) throw AssistantFailure("bad") else byteArrayOf(1) })
        val result = loader.load("session", input, emptyList()) {}
        assertEquals(listOf(1, 3), result.images.map { it.ordinal })
        val skipped = result.imagePlan.assets.single { it.status == OptionalImageStatus.SKIPPED }
        val retried = loader.prepare("session", input, emptyList(), result, setOf(skipped.id)) {}
        assertEquals(listOf(1, 2, 3), retried.images.map { it.ordinal })
        assertEquals(skipped.id, retried.images[1].id)
        assertEquals(4, calls)
    }
    @Test fun cancelledDownloadsRollBackOnlyNewImages() = runTest {
        val store = Store()
        var calls = 0
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(3) }, store,
            AssistantLinkImageFetcher { _, _ -> if (++calls == 3) throw CancellationException() else byteArrayOf(1) })
        try { loader.load("session", input, emptyList()) {}; fail("cancel must propagate") }
        catch (_: CancellationException) {}
        assertEquals(store.added.map { it.id }, store.removed)
    }
    @Test fun twoDownloadsOverlapButCompletionOrderCannotRenumberSources() = runTest {
        var active = 0
        var maximum = 0
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(3) }, Store(),
            AssistantLinkImageFetcher { _, url ->
                active++; maximum = maxOf(maximum, active)
                delay(if (url.endsWith("1.jpg")) 200 else 100)
                active--; byteArrayOf(1)
            })
        val read = loader.read(input, emptyList()) {}
        val result = loader.prepare("session", input, emptyList(), read) {}
        assertEquals(2, maximum)
        assertEquals(read.imagePlan.assets.map { it.id }, result.images.map { it.id })
        assertEquals(listOf(1, 2, 3), result.images.map { it.ordinal })
    }

    @Test fun downloadBudgetRetainsTextAndMarksUnfinishedImages() = runTest {
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(3) }, Store(),
            AssistantLinkImageFetcher { _, _ -> delay(60_000); byteArrayOf(1) })
        val result = loader.load("session", input, emptyList()) {}
        assertTrue(result.modelText.contains("青州古城"))
        assertEquals(3, result.imagePlan.assets.count { it.skip == OptionalImageSkip.TIMEOUT })
    }
    @Test fun failedPrepareCleansOnlyItsNewImageWithConcurrentWorkers() = runTest {
        val store = Store()
        val slots = java.util.concurrent.ConcurrentHashMap<String, Int>()
        val failingStore = object : AssistantImageStore by store {
            override suspend fun importSource(sessionId: String, sourceId: String, ordinal: Int,
                bytes: ByteArray, existing: List<AssistantImage>): AssistantImage {
                slots[sourceId] = ordinal
                return store.importSource(sessionId, sourceId, ordinal, bytes, existing)
            }
            override suspend fun prepare(sessionId: String, imageId: String): PreparedImage {
                if (slots[imageId] == 2) throw IllegalArgumentException("bad image")
                return store.prepare(sessionId, imageId)
            }
        }
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(20) }, failingStore,
            AssistantLinkImageFetcher { _, _ -> delay(1); byteArrayOf(1) })
        val result = withContext(Dispatchers.Default) { loader.load("session", input, emptyList()) {} }
        assertEquals((1..20).filterNot { it == 2 }, result.images.map { it.ordinal })
        val failed = result.imagePlan.assets.single { it.status == OptionalImageStatus.SKIPPED }
        assertEquals(2, failed.slot)
        assertEquals(OptionalImageSkip.DECODE, failed.skip)
        assertEquals(listOf(failed.id), store.removed)
    }
    @Test fun existingSelectionsUseSlotsButNeverBlockText() = runTest {
        val store = Store()
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { document(20) }, store,
            AssistantLinkImageFetcher { _, _ -> byteArrayOf(1) })
        val result = loader.load("session", input, listOf(AssistantImage("previous", 1, "image/png", 1, 80, 60))) {}
        assertEquals(19, result.images.size)
        assertEquals(1, result.imagePlan.omittedKnownCount)
        assertEquals((2..20).toList(), result.images.map { it.ordinal })
    }
}
