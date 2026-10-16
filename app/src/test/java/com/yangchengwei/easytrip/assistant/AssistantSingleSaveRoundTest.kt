package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantSingleSaveRoundTest {
    @Test fun successfulSaveDiscardsFailedSourcesAndEndsTheWholeRound() = runTest {
        exercise(evidence = false)
    }

    @Test fun successfulSaveDiscardsPendingEvidenceAndEndsTheWholeRound() = runTest {
        exercise(evidence = true)
    }

    @Test fun failedWriteRetainsPartialRoundAndRetriesTheSameOperation() = runTest {
        exercise(evidence = true, failFirst = true)
    }

    private suspend fun TestScope.exercise(evidence: Boolean, failFirst: Boolean = false) {
        val confirmations = mutableListOf<ImportConfirmation>()
        var finished = 0
        val image = AssistantImage("img0", 1, "image/jpeg", 1, 1, 1)
        val e = fixtureEvidence("公园", 0)
        val source = ParsedSource("img0", emptyList(), evidence = listOf(e))
        val checkpoint = GuideEvidenceCheckpoint(e.binding, listOf(source),
            fixtureResolution(e).copy(pending = listOf(GuidePendingEvidence(setOf(e.id), "待整理"))), emptyList())
        val c = PlaceAssistantController("trip", this, PlaceIntentParser { _, _ -> error("no model calls") },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) = error("no new queries")
            }, object : PlaceImport {
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                    confirmations += confirmation
                    if (failFirst && confirmations.size == 1) error("write failed")
                    return ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest,
                        confirmation.items.map { ImportItemResult(it.itemId, it.poi.poiId, "saved", true) })
                }
            }, onBatchSaved = { assertEquals(listOf(image.id), it); finished++ })
        val item = IntakeItem("item", "A", PlaceIntent("公园", "北市", "公园"),
            status = IntakeStatus.READY,
            poi = PlaceCandidate("park", "公园", "地址", GeoPoint(30.2, 120.1), null, "北市"))
        c.restoreGuideState(PlaceAssistantState(input = "原攻略", images = listOf(image), city = "北市",
            items = listOf(item), selected = setOf(item.id),
            sourceProgress = if (evidence) emptyList() else listOf(SourceProgress("img0", SourceStage.FAILED)),
            normalizationPending = evidence,
            guideVersion = if (evidence) GuideProtocolVersion.EVIDENCE_V2 else GuideProtocolVersion.LEGACY_V1,
            guideCheckpoint = if (evidence) checkpoint else null),
            "guide-operation", null, LoadedAssistantLink("原攻略", listOf(image)))
        c.environment(true, true, true)
        c.saveSelected()
        c.saveSelected()
        advanceUntilIdle()
        if (failFirst) {
            assertEquals(0, finished)
            assertTrue(c.state.value.hasDraft)
            assertEquals(setOf(item.id), c.state.value.selected)
            assertNotNull(c.state.value.error)
            c.saveSelected(); advanceUntilIdle()
            assertEquals(confirmations[0].operationId, confirmations[1].operationId)
        }
        assertTrue("One successful save must clear all candidates", c.state.value.items.isEmpty())
        assertFalse(c.state.value.hasDraft)
        assertFalse(c.state.value.hasIncompleteMaterials)
        assertNull(c.state.value.guideCheckpoint)
        assertEquals("", c.state.value.input)
        assertEquals("北市", c.state.value.city)
        assertEquals(1, c.state.value.receipt?.addedCount)
        assertEquals(1, finished)
        assertTrue(confirmations.last().operationId.startsWith("guide:guide-operation:"))
        assertFalse(c.ownsGuideBatch("guide-operation"))
        c.retryImages(); c.retryNormalization(); advanceUntilIdle()
        assertFalse(c.state.value.hasDraft)
        c.editInput("下一批")
        assertEquals("下一批", c.state.value.input)
    }
}
