package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GuideEvidenceControllerTest {
    @Test fun independentSameNameEntrancesAreNotCollapsedAfterEntityResolution() = runTest {
        var searches = 0
        val evidence = listOf(fixtureEvidence("甲园南门", 0, image = false),
            fixtureEvidence("乙园南门", 1, image = false))
        val groups = GuideEntityProtocol.decode(entityReply("""{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${mentionWire("南门", "DISTINCT_PART", "甲园")}]},
          {"e":1,"status":"PARSED","mentions":[${mentionWire("南门", "DISTINCT_PART", "乙园")}]}],
          "groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]},
                    {"members":[[1,0]],"relation":"SAME_LITERAL","evidenceRefs":[1],"frozenAnchors":[]}]}"""),
            evidence, GuideContext("", "北市", null), emptyList()).groups
        val resolution = GuideResolution(groups.map { g ->
            GuideResolvedPlace(PlaceIntent("南门", "北市", evidence.first { it.id == g.members[0].evidenceId }.rawQuote,
                materialKeys = g.members.map { it.id }.toSet()), g.members,
                GuideNameSelector.select(g, evidence), g.relation, g.relationEvidenceIds, emptySet())
        }, emptyList(), emptySet())
        val text = PlaceIntentParser { _, _ -> error("no extraction expected") }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> error("unused") },
            GuideTestImages(), normalizer = PlaceIntentNormalizer { _, _, _ -> error("unused") },
            guideParser = object : GuideMaterialParser {
                override suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
                    onProgress: (SourceProgress) -> Unit) = ParsedInputBatch(emptyList(), false)
            }, evidenceResolver = GuideEvidenceResolver { _, _, _ -> resolution })
        val c = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searches++
                return listOf(PlaceCandidate("poi-$searches", "南门", "地址",
                    GeoPoint(30.2 + searches * 0.01, 120.1), null, "北市"))
            }
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("unused")
        }, processor = { runner })
        val source = ParsedSource("text", emptyList(), evidence = evidence)
        c.restoreGuideState(PlaceAssistantState(input = "原攻略", city = "北市",
            guideVersion = GuideProtocolVersion.EVIDENCE_V2,
            guideCheckpoint = GuideEvidenceCheckpoint(evidence[0].binding, listOf(source),
                GuideResolution(emptyList(), emptyList(), emptySet()), emptyList()),
            parsedMaterials = listOf(source), parsedMaterialCity = "北市", normalizationPending = true),
            "operation", null, LoadedAssistantLink("甲园南门 乙园南门", emptyList()))
        c.environment(true, true, true); c.retryNormalization(); advanceUntilIdle()
        assertEquals(2, c.state.value.items.size)
        assertEquals(2, searches)
    }

    @Test fun changingAttachmentsOrDeletingTripInvalidatesOldEvidence() = runTest {
        for (action in listOf("add", "remove", "invalidate")) {
            val c = PlaceAssistantController("trip", this, PlaceIntentParser { _, _ -> emptyList() }, null,
                object : PlaceImport {
                    override suspend fun receipt(operationId: String): ImportReceipt? = null
                    override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("unused")
                })
            val e = fixtureEvidence("公园", 0)
            val source = ParsedSource(e.sourceId, emptyList(), evidence = listOf(e))
            val checkpoint = GuideEvidenceCheckpoint(e.binding, listOf(source),
                GuideResolution(emptyList(), listOf(GuidePendingEvidence(setOf(e.id), "待整理")), emptySet()), emptyList())
            c.restoreGuideState(PlaceAssistantState(input = "原攻略",
                images = listOf(AssistantImage("old", 1, "image/jpeg", 1, 1, 1)),
                parsedMaterials = listOf(source), guideVersion = GuideProtocolVersion.EVIDENCE_V2,
                guideCheckpoint = checkpoint), "operation", null, LoadedAssistantLink("原攻略", emptyList()))
            when (action) {
                "add" -> c.addImages(listOf(AssistantImage("new", 2, "image/jpeg", 1, 1, 1)))
                "remove" -> c.removeImage("old")
                else -> c.invalidateTrip()
            }
            assertNull("$action must invalidate old evidence", c.state.value.guideCheckpoint)
            assertTrue(c.state.value.parsedMaterials.isEmpty())
        }
    }

    @Test fun pendingEvidenceCanRetryBeforeSaveAndSingleSaveFinishesTheRound() = runTest {
        var parses = 0
        var resolves = 0
        var searches = 0
        var saves = 0
        val guide = object : GuideMaterialParser {
            override suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
                onProgress: (SourceProgress) -> Unit): ParsedInputBatch {
                if (request.retrySourceIds?.isEmpty() == true) return ParsedInputBatch(emptyList(), false)
                parses++
                assertEquals(GuideProtocolVersion.EVIDENCE_V2, request.guideVersion)
                val e = fixtureEvidence("公园", 0).copy(binding = requireNotNull(request.guideBinding))
                val s = ParsedSource("img0", emptyList(), evidence = listOf(e))
                onSource(s)
                return ParsedInputBatch(listOf(s), false)
            }
        }
        val resolver = GuideEvidenceResolver { es, _, frozen ->
            resolves++
            fixtureResolution(es.single(), frozen.singleOrNull()?.anchorId).copy(
                pending = listOf(GuidePendingEvidence(setOf(es.single().id), "待整理")))
        }
        val text = PlaceIntentParser { _, _ -> error("legacy not expected") }
        val runner = AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> error("unused") },
            GuideTestImages(), normalizer = PlaceIntentNormalizer { _, _, _ -> error("legacy normalization") },
            guideParser = guide, evidenceResolver = resolver)
        val loader = object : AssistantLinkLoader {
            override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                onProgress: (String) -> Unit) = LoadedAssistantLink("正文", listOf(AssistantImage("img0", 1, "image/jpeg", 1, 1, 1)))
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
        }
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searches++
                return listOf(PlaceCandidate("p1", "公园", "地址", GeoPoint(30.2, 120.1), null, "北市"))
            }
        }
        val importer = object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                saves++
                return ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest,
                    confirmation.items.map { ImportItemResult(it.itemId, it.poi.poiId, "saved", true) })
            }
        }
        val c = PlaceAssistantController("trip", this, text, source, importer, processor = { runner }, linkLoader = loader)
        c.environment(true, true, true); c.editCity("北市")
        c.editInput("https://xhslink.cn/o/fixture"); c.submit(); advanceUntilIdle()
        assertEquals(1, parses); assertEquals(1, resolves); assertEquals(1, searches)
        assertEquals(GuideProtocolVersion.EVIDENCE_V2, c.state.value.guideVersion)
        assertTrue(c.state.value.normalizationPending)
        assertEquals("部分地点整理未完成，成功结果可继续标记和收藏。", c.state.value.partialNotice)
        assertEquals(1, c.state.value.eligible.size)
        c.category(c.state.value.items.single().id, com.yangchengwei.easytrip.place.domain.PlaceCategory.OTHER)
        c.retryNormalization(); advanceUntilIdle()
        assertEquals(1, parses); assertEquals(2, resolves); assertEquals(1, searches)
        assertEquals(1, c.state.value.items.size)
        assertFalse(c.state.value.guideCheckpoint!!.frozen.single().committed)
        c.selectAll(); c.saveSelected(); advanceUntilIdle()
        assertEquals(1, saves)
        assertFalse(c.state.value.hasDraft)
        assertNull(c.state.value.guideCheckpoint)
        c.editInput("https://xhslink.cn/o/next"); c.submit(); advanceUntilIdle()
        assertEquals(GuideProtocolVersion.EVIDENCE_V2, c.state.value.guideVersion)
        assertTrue(c.state.value.guideCheckpoint!!.frozen.isEmpty())
    }
    @Test fun recoveredEvidenceCannotSilentlyDowngradeWhenProviderChanges() = runTest {
        var legacyCalls = 0
        val text = PlaceIntentParser { _, _ -> legacyCalls++; emptyList() }
        val c = PlaceAssistantController("trip", this, text, null, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("unused")
        }, processor = { AssistantInputProcessor(text, ImageIntentParser { _, _, _, _, _ -> emptyList() },
            GuideTestImages(), normalizer = PlaceIntentNormalizer { _, _, _ -> legacyCalls++; emptyList() }) })
        val e = fixtureEvidence("公园", 0, image = false)
        val s = ParsedSource("text", emptyList(), evidence = listOf(e))
        val checkpoint = GuideEvidenceCheckpoint(e.binding, listOf(s),
            GuideResolution(emptyList(), listOf(GuidePendingEvidence(setOf(e.id), "待整理")), emptySet()), emptyList())
        c.restoreGuideState(PlaceAssistantState(input = "原攻略", city = "北市", parsedMaterials = listOf(s),
            normalizationPending = true, guideVersion = GuideProtocolVersion.EVIDENCE_V2, guideCheckpoint = checkpoint),
            "operation", null, LoadedAssistantLink("公园", emptyList()))
        c.environment(true, true, true)
        c.retryNormalization(); advanceUntilIdle()
        assertEquals(0, legacyCalls)
        assertEquals(checkpoint, c.state.value.guideCheckpoint)
        assertTrue(c.state.value.normalizationPending)
        assertNotNull(c.state.value.error)
    }
}
