package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantLinkControllerTest {
    private val share = "青州攻略 https://xhslink.cn/o/onUvwErqYX"
    private class Images : AssistantImageStore {
        override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>) = ImageSelectionResult(emptyList(), emptyList())
        override suspend fun prepare(sessionId: String, imageId: String) = PreparedImage(imageId, "image/jpeg", byteArrayOf(1))
        override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("unused")
        override suspend fun remove(sessionId: String, imageId: String) {}
        override suspend fun release(sessionId: String) {}
    }
    private val importer = object : PlaceImport {
        override suspend fun receipt(operationId: String): ImportReceipt? = null
        override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("never auto-save")
    }
    @Test fun readsOnlyOnSendAndKeepsShareInputWhileModelReceivesBody() = runTest {
        var reads = 0
        val texts = mutableListOf<String>()
        val text = PlaceIntentParser { input, _ -> texts += input; emptyList() }
        val loader = object : AssistantLinkLoader {
            override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink {
                reads++; onProgress("读取中")
                return LoadedAssistantLink("完整笔记正文".repeat(500), emptyList())
            }
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
        }
        val controller = PlaceAssistantController("trip", this, text, null, importer,
            processor = { AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> emptyList() }, Images()) },
            linkLoader = loader)
        controller.environment(true, true, true)
        controller.editInput(share)
        assertEquals(0, reads)
        controller.submit(); advanceUntilIdle()
        assertEquals(share, controller.state.value.input)
        assertEquals(listOf("完整笔记正文".repeat(500)), texts)
        controller.submit(); advanceUntilIdle()
        assertEquals(1, reads)
        controller.clear()
        controller.editInput(share); controller.submit(); advanceUntilIdle()
        assertEquals(2, reads)
    }
    @Test fun twentyImagesWithRepeatedPlacesDoNotOverflowBeforePlaceDeduplication() = runTest {
        var calls = 0
        val text = PlaceIntentParser { _, _ -> listOf(PlaceIntent("青州古城", "青州", "青州古城")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            calls++
            listOf(PlaceIntent("青州古城", "青州", "青州古城",
                sources = listOf(IntakeSource.Image(image.id, ordinal, "青州古城"))))
        }, Images())
        val images = (1..20).map { AssistantImage("i$it", it, "image/png", 100, 80, 60) }
        val batch = runner.parse(AssistantInputRequest("session", "青州古城", "青州", images)) {}
        assertEquals(20, calls)
        assertFalse(batch.overflow)
        val controller = PlaceAssistantController("trip", this, text, null, importer, processor = { runner })
        controller.environment(true, true, true)
        controller.editCity("青州"); controller.editInput("青州古城"); controller.addImages(images)
        controller.submit(); advanceUntilIdle()
        assertEquals(1, controller.state.value.items.size)
        assertEquals(21, controller.state.value.items.single().sources.size)
    }

    @Test fun cancellingLateLinkResultDiscardsOnlyItsFilesAndNeverRunsModel() = runTest {
        val delayed = CompletableDeferred<Unit>()
        val removed = mutableListOf<String>()
        var models = 0
        val text = PlaceIntentParser { _, _ -> models++; emptyList() }
        val loader = object : AssistantLinkLoader {
            override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink {
                withContext(NonCancellable) { delayed.await() }
                return LoadedAssistantLink("青州", listOf(AssistantImage("old", 1, "image/png", 10, 10, 10)))
            }
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) { removed += images.map { it.id } }
        }
        val controller = PlaceAssistantController("trip", this, text, null, importer,
            processor = { AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> emptyList() }, Images()) },
            linkLoader = loader)
        controller.environment(true, true, true); controller.editInput(share); controller.submit(); runCurrent()
        controller.clear(); controller.editInput("新输入")
        controller.addImages(listOf(AssistantImage("new", 1, "image/png", 10, 10, 10)))
        delayed.complete(Unit); advanceUntilIdle()
        assertEquals(0, models)
        assertEquals(listOf("old"), removed)
        assertEquals("新输入", controller.state.value.input)
        assertEquals(listOf("new"), controller.state.value.images.map { it.id })
        assertFalse(controller.state.value.busy)
    }
    @Test fun readFailureNeverSendsShareTitleAndImageRetryDoesNotRefetchNote() = runTest {
        var reads = 0
        var textCalls = 0
        var imageCalls = 0
        val text = PlaceIntentParser { _, _ -> textCalls++; emptyList() }
        val loader = object : AssistantLinkLoader {
            override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink {
                if (++reads == 1) throw AssistantFailure("读取失败")
                return LoadedAssistantLink("青州古城", listOf(AssistantImage("linked", 1, "image/png", 1, 10, 10)))
            }
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
        }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            if (++imageCalls == 1) throw AssistantFailure("模型失败")
            listOf(PlaceIntent("青州古城", "青州", "青州古城", sources = listOf(IntakeSource.Image(image.id, ordinal, "青州古城"))))
        }, Images())
        val controller = PlaceAssistantController("trip", this, text, null, importer,
            processor = { runner }, linkLoader = loader)
        controller.environment(true, true, true); controller.editInput(share)
        controller.submit(); advanceUntilIdle()
        assertEquals(0, textCalls); assertEquals(0, imageCalls)
        assertEquals(share, controller.state.value.input)
        controller.submit(); advanceUntilIdle()
        assertEquals(2, reads); assertEquals(1, textCalls); assertEquals(1, imageCalls)
        controller.retryImages(); advanceUntilIdle()
        assertEquals(2, reads); assertEquals(1, textCalls); assertEquals(2, imageCalls)
        assertEquals(1, controller.state.value.items.size)
    }
}
