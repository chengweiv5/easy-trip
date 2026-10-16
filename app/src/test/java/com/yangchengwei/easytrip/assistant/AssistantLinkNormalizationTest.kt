package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantLinkNormalizationTest {
    private class Images : AssistantImageStore {
        override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>) = ImageSelectionResult(emptyList(), emptyList())
        override suspend fun prepare(sessionId: String, imageId: String) = PreparedImage(imageId, "image/jpeg", byteArrayOf(1))
        override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("unused")
        override suspend fun remove(sessionId: String, imageId: String) {}
        override suspend fun release(sessionId: String) {}
    }
    private val share = "青州 https://xhslink.cn/o/onUvwErqYX"
    private val importer = object : PlaceImport {
        override suspend fun receipt(operationId: String): ImportReceipt? = null
        override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("never auto-save")
    }
    private fun loader() = object : AssistantLinkLoader {
        override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit) =
            LoadedAssistantLink("青州博物馆与别名说明", listOf(AssistantImage("image", 1, "image/png", 10, 10, 10)))
        override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
    }

    @Test fun normalizesMoreThanHundredRawCandidatesBeforeFinalLimitAndRetriesOnlyNormalization() = runTest {
        var textCalls = 0
        var imageCalls = 0
        var normalizationCalls = 0
        val raw = (1..120).map { PlaceIntent("别名$it", "青州", "别名$it") }
        val text = PlaceIntentParser { _, _ -> textCalls++; raw.take(60) }
        val normalizer = PlaceIntentNormalizer { candidates, _, _ ->
            normalizationCalls++
            assertEquals(120, candidates.size)
            if (normalizationCalls == 1) throw AssistantFailure("temporary normalization failure")
            candidates.take(100)
        }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> imageCalls++; raw.drop(60) },
            Images(), normalizer = normalizer)
        val controller = PlaceAssistantController("trip", this, text, null, importer,
            processor = { runner }, linkLoader = loader())
        controller.environment(true, true, true)
        controller.editInput(share); controller.submit(); advanceUntilIdle()
        assertTrue(controller.state.value.items.isEmpty())
        assertNotNull(controller.state.value.error)
        controller.submit(); advanceUntilIdle()
        assertEquals(1, textCalls)
        assertEquals(1, imageCalls)
        assertEquals(2, normalizationCalls)
        assertEquals(100, controller.state.value.items.size)
        assertEquals("CV", controller.state.value.items.last().label)
        assertEquals(share, controller.state.value.input)
    }

    @Test fun cancellationDuringNormalizationNeverPublishesUnnormalizedCandidatesOrLateResult() = runTest {
        val gate = CompletableDeferred<Unit>()
        val text = PlaceIntentParser { _, _ -> listOf(PlaceIntent("青州糗糕", "青州", "青州糗糕")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> emptyList() }, Images(),
            normalizer = PlaceIntentNormalizer { candidates, _, _ ->
                withContext(NonCancellable) { gate.await() }
                candidates
            })
        val controller = PlaceAssistantController("trip", this, text, null, importer,
            processor = { runner }, linkLoader = loader())
        controller.environment(true, true, true)
        controller.editInput(share); controller.submit(); runCurrent()
        try {
            assertEquals("正在整理独立景点、合并同一地点…", controller.state.value.linkProgress)
            controller.cancel()
            assertTrue(controller.state.value.items.isEmpty())
        } finally {
            controller.cancel()
            gate.complete(Unit)
        }
        advanceUntilIdle()
        assertTrue(controller.state.value.items.isEmpty())
    }

    @Test fun hundredNormalizedPlacesProceedAndHundredOneBlocksWithoutTruncating() = runTest {
        for (count in listOf(100, 101)) {
            var searches = 0
            val places = (1..count).map { PlaceIntent("公园$it", "青州", "公园$it") }
            val text = PlaceIntentParser { _, _ -> places.take(60) }
            val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> places.drop(60) }, Images(),
                normalizer = PlaceIntentNormalizer { candidates, _, _ -> candidates })
            val controller = PlaceAssistantController("trip", this, text,
                object : com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource {
                    override suspend fun search(keyword: String, city: String?): List<com.yangchengwei.easytrip.place.amap.PlaceCandidate> {
                        searches++
                        return emptyList()
                    }
                }, importer, processor = { runner }, linkLoader = loader())
            controller.environment(true, true, true)
            controller.editInput(share); controller.submit(); advanceUntilIdle()
            if (count == 100) {
                assertEquals(100, searches)
                assertEquals("CV", controller.state.value.items.last().label)
            } else {
                assertEquals(0, searches)
                assertTrue(controller.state.value.items.isEmpty())
                assertTrue(controller.state.value.error!!.contains("101"))
                assertTrue(controller.state.value.error!!.contains("最多 100"))
            }
        }
    }

    @Test fun changedCityReextractsCachedMaterialsBeforeNormalizationRetry() = runTest {
        for (manual in listOf(true, false)) {
            var defaultCity = "青州"
            val textCities = mutableListOf<String>()
            val normalizationCities = mutableListOf<String>()
            var imageCalls = 0
            val text = PlaceIntentParser { _, city ->
                textCities += city
                listOf(PlaceIntent("博物馆", city, "博物馆"))
            }
            val runner = AssistantInputProcessor(text,
                ImageIntentParser { _, _, _, _, _ -> imageCalls++; emptyList() }, Images(),
                normalizer = PlaceIntentNormalizer { candidates, _, _ ->
                    normalizationCities += candidates.single().city
                    if (normalizationCities.size == 1) throw AssistantFailure("retry normalization")
                    candidates
                })
            val controller = PlaceAssistantController("trip", this, text, null, importer,
                processor = { runner }, linkLoader = loader(), resolveDefaultCity = { defaultCity })
            controller.environment(true, true, true)
            controller.editInput(share); controller.submit(); advanceUntilIdle()
            if (manual) controller.editCity("杭州") else defaultCity = "杭州"
            controller.submit(); advanceUntilIdle()
            assertEquals(listOf("青州", "杭州"), textCities)
            assertEquals(listOf("青州", "杭州"), normalizationCities)
            assertEquals(2, imageCalls)
            assertEquals("杭州", controller.state.value.items.single().intent.city)
        }
    }

    @Test fun linkExtractionHasSeparateCandidateBudgetAndFiltersFoodAndMapBackgroundInPrompt() {
        val config = ProviderConfig("https://example.test", "model", "secret")
        val input = (0..34).joinToString("、") { "地点$it" }
        val items = (0..34).joinToString(",") { """{"query":"地点$it","city":"青州","sourceSpan":"地点$it"}""" }
        val args = """{"items":[$items],"overflow":false}"""
        val response = """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"type":"function","function":{"name":"search_place_batch","arguments":${JsonPrimitive(args)}}}]}}]}"""
        assertEquals(35, BatchToolProtocol.decode(response, input, "青州", maxPlaces = 100).size)
        assertEquals(35, BatchToolProtocol.decode(response, input, "青州").size)
        val text = BatchToolProtocol.request(config, input, "青州", maxPlaces = 100, travelMaterials = true)
        val image = ImageToolProtocol.request(config, PreparedImage("id", "image/jpeg", byteArrayOf(1)), 1, input, "青州",
            maxPlaces = 100, travelMaterials = true)
        assertFalse("Link body must not be mistaken for text read from this image", image.contains(input))
        listOf(text, image).forEach {
            assertTrue(it.contains("食品"))
            assertTrue(it.contains("背景"))
            assertTrue(it.contains("100"))
        }
    }
}
