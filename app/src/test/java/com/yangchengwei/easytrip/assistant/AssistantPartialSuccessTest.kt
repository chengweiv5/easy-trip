package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.net.Uri
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.assistant.cityfirst.GuideResultCodec
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.test.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantPartialSuccessTest {
    private class Images : AssistantImageStore {
        override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>) =
            ImageSelectionResult(emptyList(), emptyList())
        override suspend fun prepare(sessionId: String, imageId: String) = PreparedImage(imageId, "image/png", byteArrayOf(1))
        override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("unused")
        override suspend fun remove(sessionId: String, imageId: String) {}
        override suspend fun release(sessionId: String) {}
    }
    private class Importer : PlaceImport {
        val commits = mutableListOf<ImportConfirmation>()
        override suspend fun receipt(operationId: String): ImportReceipt? = null
        override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
            commits += confirmation
            return ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest,
                confirmation.items.map { ImportItemResult(it.itemId, it.poi.poiId, "saved-${it.poi.poiId}", true) })
        }
    }
    private fun images() = listOf(AssistantImage("11111111-1111-4111-8111-111111111111", 1, "image/png", 10, 10, 10))
    private fun loader() = object : AssistantLinkLoader {
        override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>,
            onProgress: (String) -> Unit) = LoadedAssistantLink("北城美术馆", images())
        override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
    }
    private fun source(searches: MutableList<String>) = object : PlaceSearchDataSource {
        override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
            searches += keyword
            val name = keyword.removePrefix("北城")
            return listOf(PlaceCandidate(name, name, "地址", GeoPoint(30.2, 120.1), null, "北城"))
        }
    }
    private fun start(c: PlaceAssistantController) {
        c.environment(true, true, true); c.editCity("北城")
        c.editInput("北城 https://xhslink.cn/o/fixture"); c.submit()
    }

    @Test fun unsupportedPicturesDoNotBlockText() = runTest {
        val searched = mutableListOf<String>()
        val text = PlaceIntentParser { _, _ -> listOf(PlaceIntent("美术馆", "北城", "美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> error("must not send image") }, Images())
        val c = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, imageAllowed = { false })
        c.environment(true, true, true); c.editCity("北城"); c.addImages(images()); c.editInput("美术馆")
        c.submit(); advanceUntilIdle()
        assertEquals(SourceStage.COMPLETE, c.state.value.sourceProgress.single { it.sourceId == "text" }.stage)
        assertEquals(SourceStage.SKIPPED, c.state.value.sourceProgress.single { it.sourceId != "text" }.stage)
        assertTrue(c.state.value.eligible.isNotEmpty())
        assertFalse(c.state.value.hasIncompleteMaterials)
    }

    @Test fun retryCannotBypassCombinedFourHundredCandidateBudgetUsingCachedSources() = runTest {
        val searched = mutableListOf<String>()
        var normalizations = 0
        var attempts = 0
        val allImages = (1..5).map { images().single().copy(id = "image-$it", ordinal = it) }
        val materialLoader = object : AssistantLinkLoader {
            override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>,
                onProgress: (String) -> Unit) = LoadedAssistantLink("原攻略", allImages)
            override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
        }
        val guide = object : GuideMaterialParser {
            override suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
                onProgress: (SourceProgress) -> Unit): ParsedInputBatch {
                if (request.images.isNotEmpty()) attempts++
                val sources = request.images.filter { request.retrySourceIds == null || it.id in request.retrySourceIds }.map { image ->
                    if (attempts == 1 && image.ordinal == 5) ParsedSource(image.id, emptyList(), AssistantFailure("未读清"))
                    else ParsedSource(image.id, (1..100).map { index ->
                        val name = "公园${image.ordinal}_$index"
                        PlaceIntent(name, "北城", name, sources = listOf(IntakeSource.Image(image.id, image.ordinal, name)))
                    })
                } + if (request.retrySourceIds == null) listOf(ParsedSource("text", emptyList())) else emptyList()
                sources.forEach(onSource)
                return ParsedInputBatch(sources, false)
            }
        }
        val text = PlaceIntentParser { _, _ -> error("guide only") }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> error("guide only") },
            Images(), normalizer = PlaceIntentNormalizer { values, _, _ ->
                normalizations++
                listOf(values.first())
            }, guideParser = guide)
        val c = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, linkLoader = materialLoader)
        start(c); advanceUntilIdle()
        assertEquals(400, c.state.value.parsedMaterials.sumOf { it.intents.size })
        assertEquals(1, normalizations)
        val queriesBefore = searched.size
        c.retryImages(); advanceUntilIdle()
        assertEquals(2, attempts)
        assertEquals(1, normalizations)
        assertEquals(queriesBefore, searched.size)
        assertEquals(400, c.state.value.parsedMaterials.sumOf { it.intents.size })
        assertEquals(1, c.state.value.items.size)
    }

    @Test fun retryRestoresOriginalSourceOrderInsteadOfPuttingCachedImagesBeforeText() = runTest {
        val order = mutableListOf<List<String>>()
        var attempt = 0
        val picture = ParsedSource(images().single().id, listOf(PlaceIntent("公园", "北城", "公园",
            sources = listOf(IntakeSource.Image(images().single().id, 1, "公园")))))
        val guide = object : GuideMaterialParser {
            override suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
                onProgress: (SourceProgress) -> Unit): ParsedInputBatch {
                attempt++
                val text = if (attempt == 1) ParsedSource("text", emptyList(), AssistantFailure("未读清"))
                    else ParsedSource("text", listOf(PlaceIntent("美术馆", "北城", "美术馆",
                        sources = listOf(IntakeSource.Text("美术馆")))))
                val values = if (attempt == 1) listOf(text, picture) else listOf(text)
                values.forEach(onSource)
                return ParsedInputBatch(values, false)
            }
        }
        val text = PlaceIntentParser { _, _ -> error("must use guide") }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> error("must use guide") },
            Images(), normalizer = PlaceIntentNormalizer { values, _, _ ->
                order += values.map { it.query }; values
            }, guideParser = guide)
        val c = PlaceAssistantController("trip", this, text, source(mutableListOf()), Importer(),
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle()
        c.retryImages(); advanceUntilIdle()
        assertEquals(listOf("美术馆", "公园"), order.last())
        assertEquals(listOf("text", images().single().id), c.state.value.parsedMaterials.map { it.sourceId })
    }

    @Test fun failedImageDoesNotBlockSuccessfulTextFromNormalizationAndMapQuery() = runTest {
        val searched = mutableListOf<String>()
        var normalized = 0
        val text = PlaceIntentParser { _, _ -> listOf(PlaceIntent("美术馆", "北城", "北城美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ ->
            throw AssistantFailure("坏格式", AssistantFailureCode.PROTOCOL)
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ -> normalized++; candidates })
        val importer = Importer()
        val c = PlaceAssistantController("trip", this, text, source(searched), importer,
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle()
        assertEquals(1, normalized)
        assertEquals(1, searched.size)
        assertEquals(IntakeStatus.READY, c.state.value.items.single().status)
        assertEquals(SourceStage.SKIPPED, c.state.value.sourceProgress.single { it.sourceId == "11111111-1111-4111-8111-111111111111" }.stage)
        assertTrue(importer.commits.isEmpty())
    }

    @Test fun classifiedReviewAndExcludedLodgingCannotBlockAValidAttractionFromQueryOrCollection() = runTest {
        val searched = mutableListOf<String>()
        val input = "美术馆，山间客栈，不明场馆"
        val args = """{"overflow":false,"items":[
            {"query":"美术馆","city":"北城","sourceSpan":"美术馆","classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION","independentVisit":true,"evidenceQuote":"美术馆"}},
            {"query":"山间客栈","city":"北城","sourceSpan":"山间客栈","classification":{"purpose":"LODGING","granularity":"DESTINATION","independentVisit":true,"evidenceQuote":"山间客栈"}},
            {"query":"不明场馆","city":"北城","sourceSpan":"不明场馆","classification":{"purpose":"UNKNOWN","granularity":"UNKNOWN","independentVisit":false,"evidenceQuote":"不明场馆"}}
        ]}"""
        val response = """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"type":"function","function":{"name":"search_place_batch","arguments":${JsonPrimitive(args)}}}]}}]}"""
        val text = PlaceIntentParser { _, _ -> BatchToolProtocol.decode(response, input, "北城", travelMaterials = true) }
        val importer = Importer()
        val c = PlaceAssistantController("trip", this, text, source(searched), importer)
        c.environment(true, true, true); c.editCity("北城"); c.editInput(input); c.submit()
        advanceUntilIdle()
        assertEquals(listOf("北城美术馆"), searched)
        assertEquals(listOf(IntakeStatus.READY, IntakeStatus.NEED_CITY), c.state.value.items.map { it.status })
        assertTrue(c.state.value.items.none { it.intent.query == "山间客栈" })
        val restored = GuideResultCodec.decode(GuideResultCodec.encode(c.state.value))
        assertEquals(listOf(false, true), restored.items.map { it.intent.needsReview })
        c.toggle(c.state.value.items.first().id); c.saveSelected(); advanceUntilIdle()
        assertEquals(listOf("美术馆"), importer.commits.single().items.map { it.poi.name })
    }

    @Test fun collectingSuccessFinishesRoundAndDiscardsFailedMaterial() = runTest {
        val searched = mutableListOf<String>()
        var textCalls = 0
        var imageCalls = 0
        var finished = 0
        val text = PlaceIntentParser { _, _ -> textCalls++; listOf(PlaceIntent("美术馆", "北城", "北城美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            imageCalls++
            if (imageCalls == 1) throw AssistantFailure("坏格式", AssistantFailureCode.PROTOCOL)
            listOf("美术馆", "公园").map { PlaceIntent(it, "北城", it,
                sources = listOf(IntakeSource.Image(image.id, ordinal, it))) }
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ -> mergeIdenticalPlaceIntents(candidates) })
        val importer = Importer()
        val c = PlaceAssistantController("trip", this, text, source(searched), importer,
            processor = { runner }, linkLoader = loader(), onBatchSaved = { finished++ })
        start(c); advanceUntilIdle()
        c.selectAll(); c.saveSelected(); advanceUntilIdle()
        assertEquals(1, importer.commits.size)
        assertEquals(1, finished)
        assertFalse(c.state.value.hasDraft)
        assertTrue(c.state.value.items.isEmpty())
        assertTrue(c.state.value.images.isEmpty())
        assertTrue(c.state.value.sourceProgress.isEmpty())
        c.retryImages(); advanceUntilIdle()
        assertEquals(1, textCalls)
        assertEquals(1, imageCalls)
        assertEquals(1, searched.size)
        c.selectAll(); c.saveSelected(); advanceUntilIdle()
        assertEquals(1, importer.commits.size)
        assertEquals(listOf("美术馆"), importer.commits.flatMap { it.items }.map { it.poi.name })
        assertEquals(1, finished)
        assertFalse(c.state.value.hasDraft)
    }

    @Test fun retryAliasKeepsOriginalSelectionAndManualNameCityAndCategory() = runTest {
        val searched = mutableListOf<String>()
        var imageCalls = 0
        val text = PlaceIntentParser { _, _ -> listOf(PlaceIntent("美术馆", "北城", "北城美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            if (++imageCalls == 1) throw AssistantFailure("坏格式", AssistantFailureCode.PROTOCOL)
            listOf(PlaceIntent("北城美术馆", "北城", "北城美术馆",
                sources = listOf(IntakeSource.Image(image.id, ordinal, "北城美术馆"))))
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ ->
            val members = candidates.indices.joinToString(",")
            val args = """{"groups":[{"canonical":${candidates.lastIndex},"members":[$members]}],"discarded":[]}"""
            PlaceNormalizationProtocol.decode(
                """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"type":"function","function":{"name":"normalize_place_candidates","arguments":${JsonPrimitive(args)}}}]}}]}""",
                candidates)
        })
        val c = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle()
        val id = c.state.value.items.single().id
        c.editItem(id, "美术馆东馆", "北城")
        c.category(id, PlaceCategory.OTHER)
        c.retry(setOf(id)); advanceUntilIdle()
        c.toggle(id)
        val before = c.state.value.items.single()
        val selected = c.state.value.selected
        val queries = searched.size
        c.retryImages(); advanceUntilIdle()
        assertEquals(1, c.state.value.items.size)
        val after = c.state.value.items.single()
        assertEquals(id, after.id)
        assertEquals(before.intent.query, after.intent.query)
        assertEquals(before.intent.city, after.intent.city)
        assertEquals(before.category, after.category)
        assertEquals(before.poi, after.poi)
        assertEquals(before.generation, after.generation)
        assertEquals(selected, c.state.value.selected)
        assertEquals(queries, searched.size)
        assertTrue(after.sources.any { it is IntakeSource.Image })
    }

    @Test fun unsavedPartialBatchRestoresFailedSourceAndSuccessfulExtractionWithoutResending() = runTest {
        val searched = mutableListOf<String>()
        var textCalls = 0
        var imageCalls = 0
        val text = PlaceIntentParser { _, _ -> textCalls++; listOf(PlaceIntent("美术馆", "北城", "北城美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            if (++imageCalls == 1) throw AssistantFailure("坏格式", AssistantFailureCode.PROTOCOL)
            listOf(PlaceIntent("公园", "北城", "公园", sources = listOf(IntakeSource.Image(image.id, ordinal, "公园"))))
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ -> candidates })
        val importer = Importer()
        fun controller() = PlaceAssistantController("trip", this, text, source(searched), importer,
            processor = { runner }, linkLoader = loader())
        val first = controller()
        start(first); advanceUntilIdle()
        val recovered = GuideResultCodec.decode(GuideResultCodec.encode(first.state.value))
        assertEquals(SourceStage.SKIPPED, recovered.sourceProgress.single { it.sourceId == "11111111-1111-4111-8111-111111111111" }.stage)
        val second = controller()
        second.environment(true, true, true)
        second.restoreGuideState(recovered, "operation", AssistantCityScope.parse("北城"),
            LoadedAssistantLink("北城美术馆", images()))
        second.retryImages(); advanceUntilIdle()
        assertEquals(1, textCalls)
        assertEquals(2, imageCalls)
        assertEquals(2, second.state.value.items.size)
        assertEquals(IntakeStatus.READY, second.state.value.items.first().status)
        second.selectAll(); second.saveSelected(); advanceUntilIdle()
        assertFalse(second.state.value.hasDraft)
        assertEquals(2, importer.commits.single().items.size)
    }

    @Test fun sourceOverflowIsLocalButCombinedLimitDoesNotTruncateOrQueryNewResults() = runTest {
        val searched = mutableListOf<String>()
        var imageCalls = 0
        val text = PlaceIntentParser { _, _ -> (1..100).map { PlaceIntent("公园$it", "北城", "公园$it") } }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            if (++imageCalls == 1) throw AssistantFailure("本图候选超限", AssistantFailureCode.OVERFLOW)
            listOf(PlaceIntent("新公园", "北城", "新公园", sources = listOf(IntakeSource.Image(image.id, ordinal, "新公园"))))
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ -> candidates })
        val c = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle()
        assertEquals(100, searched.size)
        assertEquals(100, c.state.value.items.size)
        c.retryImages(); advanceUntilIdle()
        assertEquals(100, searched.size)
        assertEquals(100, c.state.value.items.size)
        assertTrue(c.state.value.error.orEmpty().contains("101"))
        assertTrue(c.state.value.normalizationPending)
    }

    @Test fun failedIncrementalNormalizationKeepsPreviousSelectionAndRetriesOnlyOrganization() = runTest {
        val searched = mutableListOf<String>()
        var textCalls = 0
        var imageCalls = 0
        var normalized = 0
        val text = PlaceIntentParser { _, _ -> textCalls++; listOf(PlaceIntent("美术馆", "北城", "北城美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            if (++imageCalls == 1) throw AssistantFailure("坏格式", AssistantFailureCode.PROTOCOL)
            listOf(PlaceIntent("公园", "北城", "公园", sources = listOf(IntakeSource.Image(image.id, ordinal, "公园"))))
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ ->
            if (++normalized == 2) throw AssistantFailure("分组不完整", AssistantFailureCode.PROTOCOL)
            candidates
        })
        val c = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle(); c.selectAll()
        val selected = c.state.value.selected
        c.retryImages(); advanceUntilIdle()
        assertEquals(selected, c.state.value.selected)
        assertEquals(1, c.state.value.items.size)
        assertEquals(1, searched.size)
        assertTrue(c.state.value.normalizationPending)
        c.retryNormalization(); advanceUntilIdle()
        assertEquals(1, textCalls)
        assertEquals(2, imageCalls)
        assertEquals(3, normalized)
        assertEquals(2, searched.size)
        assertEquals(selected, c.state.value.selected)
        assertFalse(c.state.value.normalizationPending)
    }

    @Test fun allSourcesFailWithoutNormalizationOrMapQueriesAndLegacyResultsStillRead() = runTest {
        val searched = mutableListOf<String>()
        var normalized = 0
        val text = PlaceIntentParser { _, _ -> throw AssistantFailure("正文失败") }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ ->
            throw AssistantFailure("图片失败")
        }, Images(), normalizer = PlaceIntentNormalizer { _, _, _ -> normalized++; emptyList() })
        val c = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle()
        assertTrue(c.state.value.items.isEmpty())
        assertEquals(1, c.state.value.unfinishedSources.size)
        assertEquals(1, c.state.value.skippedImageSources.size)
        assertEquals(0, normalized); assertTrue(searched.isEmpty())
        val old = """{"version":1,"input":"原攻略","city":"北城","error":null,"images":[],"selected":[],"items":[]}"""
        assertEquals("原攻略", GuideResultCodec.decode(old).input)
        assertTrue(GuideResultCodec.decode(old).sourceProgress.isEmpty())
    }

    @Test fun editingUnresolvedInputClearsOldMaterialCheckpoints() = runTest {
        val text = PlaceIntentParser { _, _ -> listOf(PlaceIntent("美术馆", "北城", "北城美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> emptyList() },
            Images(), normalizer = PlaceIntentNormalizer { _, _, _ -> throw AssistantFailure("整理失败") })
        val c = PlaceAssistantController("trip", this, text, source(mutableListOf()), Importer(),
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle()
        assertTrue(c.state.value.normalizationPending)
        assertTrue(c.state.value.parsedMaterials.isNotEmpty())
        c.removeImage(images().single().id)
        assertTrue(c.state.value.parsedMaterials.isEmpty())
        assertFalse(c.state.value.normalizationPending)
        c.editInput("另一份攻略")
        val restored = GuideResultCodec.decode(GuideResultCodec.encode(c.state.value))
        assertEquals("另一份攻略", restored.input)
        assertTrue(restored.parsedMaterials.isEmpty())
        assertFalse(restored.normalizationPending)
    }

    @Test fun changedCityRecoveryDoesNotReuseExtractionBoundToPreviousCity() = runTest {
        val extractedCities = mutableListOf<String>()
        val normalizedCities = mutableListOf<String>()
        var imageCalls = 0
        val text = PlaceIntentParser { _, city ->
            extractedCities += city
            listOf(PlaceIntent("美术馆", city, "美术馆", usesDefaultCity = true))
        }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ ->
            if (++imageCalls == 1) throw AssistantFailure("图片失败")
            emptyList()
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ ->
            normalizedCities += candidates.map { it.city }; candidates
        })
        val c = PlaceAssistantController("trip", this, text, source(mutableListOf()), Importer(),
            processor = { runner }, linkLoader = loader())
        start(c); advanceUntilIdle()
        c.editCity("南城")
        val restored = GuideResultCodec.decode(GuideResultCodec.encode(c.state.value))
        val next = PlaceAssistantController("trip", this, text, source(mutableListOf()), Importer(),
            processor = { runner }, linkLoader = loader())
        next.environment(true, true, true)
        next.restoreGuideState(restored, "operation", AssistantCityScope(listOf("南城")),
            LoadedAssistantLink("北城美术馆", images()))
        next.retryImages(); advanceUntilIdle()
        assertEquals(listOf("北城", "南城"), extractedCities)
        assertEquals(listOf("北城", "南城"), normalizedCities)
        assertEquals(2, imageCalls)
    }

    @Test fun cancelledRetryCheckpointsCompletedImageAndAllowsOrganizationAfterRestore() = runTest {
        val searched = mutableListOf<String>()
        val secondImage = AssistantImage("22222222-2222-4222-8222-222222222222", 2, "image/png", 10, 10, 10)
        val allImages = images() + secondImage
        val gate = CompletableDeferred<Unit>()
        var firstCalls = 0
        var secondCalls = 0
        val text = PlaceIntentParser { _, _ -> listOf(PlaceIntent("美术馆", "北城", "北城美术馆")) }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            if (ordinal == 1) {
                if (++firstCalls == 1) throw AssistantFailure("图1失败")
            } else {
                if (++secondCalls == 1) throw AssistantFailure("图2失败")
                gate.await()
            }
            listOf(PlaceIntent("公园$ordinal", "北城", "公园$ordinal",
                sources = listOf(IntakeSource.Image(image.id, ordinal, "公园$ordinal"))))
        }, Images(), normalizer = PlaceIntentNormalizer { candidates, _, _ -> candidates })
        val loader = object : AssistantLinkLoader {
            override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>,
                onProgress: (String) -> Unit) = LoadedAssistantLink("北城美术馆", allImages)
            override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
        }
        val c = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, linkLoader = loader)
        start(c); advanceUntilIdle()
        c.retryImages(); runCurrent()
        c.cancel(); runCurrent()
        val restored = GuideResultCodec.decode(GuideResultCodec.encode(c.state.value))
        assertTrue(restored.normalizationPending)
        assertEquals(listOf("公园1"), restored.parsedMaterials.single { it.sourceId == allImages[0].id }.intents.map { it.query })
        val next = PlaceAssistantController("trip", this, text, source(searched), Importer(),
            processor = { runner }, linkLoader = loader)
        next.environment(true, true, true)
        next.restoreGuideState(restored, "operation", AssistantCityScope.parse("北城"),
            LoadedAssistantLink("北城美术馆", allImages))
        next.retryNormalization(); advanceUntilIdle()
        assertEquals(2, next.state.value.items.size)
        assertEquals(2, firstCalls)
        assertEquals(2, secondCalls)
        gate.complete(Unit)
        next.retryImages(); advanceUntilIdle()
        assertEquals(3, next.state.value.items.size)
        assertEquals(2, firstCalls)
        assertEquals(3, secondCalls)
    }
}
