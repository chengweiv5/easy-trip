package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.GuideResultCodec
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GuideEvidenceRecoveryTest {
    @Test fun optionalFilelessSourceKeepsFailureAndLegacyEnvelopeStillReads() {
        val id = "11111111-1111-4111-8111-111111111111"
        val state = PlaceAssistantState(input = "正文", optionalImages = OptionalImagePlan(listOf(
            OptionalImageAsset(id, 3, 7, null, OptionalImageStatus.SKIPPED, skip = OptionalImageSkip.MISSING))),
            sourceProgress = listOf(SourceProgress(id, SourceStage.SKIPPED)),
            parsedMaterials = listOf(ParsedSource(id, emptyList(), AssistantFailure("missing"))))
        val restored = GuideResultCodec.decode(GuideResultCodec.encode(state))
        assertEquals(state.optionalImages, restored.optionalImages)
        assertEquals(SourceStage.SKIPPED, restored.sourceProgress.single().stage)
        assertNotNull(restored.parsedMaterials.single().failure)
        val plain = Json.parseToJsonElement(GuideResultCodec.encode(PlaceAssistantState(input = "旧正文"))).jsonObject
        val old = JsonObject(plain - "protocolVersion" - "optionalImages" + ("version" to JsonPrimitive(1)))
        assertEquals("旧正文", GuideResultCodec.decode(old.toString()).input)
    }
    @Test fun v2RoundTripKeepsEvidencePendingAndDecisionWhileV1StaysLegacyAndUnknownVersionFails() {
        val imageId = "11111111-1111-4111-8111-111111111111"
        val raw = fixtureEvidence("公园", 0).copy(sourceId = imageId)
        val e = raw.copy(id = guideEvidenceId(raw.binding, raw.sourceId, raw.sourceFingerprint, raw.kind, raw.rawQuote))
        val resolved = GuideEntityProtocol.decode(entityReply("""{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[
            ${mentionWire("公园")}]}],"groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]}]}"""),
            listOf(e), GuideContext("", "北市", null), emptyList()).groups.single()
        val decision = GuideNameSelector.select(resolved, listOf(e))
        val resolution = GuideResolution(listOf(GuideResolvedPlace(
            PlaceIntent("公园", "北市", e.rawQuote, materialKeys = resolved.members.map { it.id }.toSet()),
            resolved.members, decision, resolved.relation, resolved.relationEvidenceIds, emptySet())),
            listOf(GuidePendingEvidence(setOf(e.id), "待整理")), emptySet())
        val source = ParsedSource(e.sourceId, emptyList(), evidence = listOf(e))
        val checkpoint = GuideEvidenceCheckpoint(e.binding, listOf(source), resolution, emptyList())
        val state = PlaceAssistantState(input = "原攻略", city = "北市",
            images = listOf(AssistantImage(imageId, 1, "image/jpeg", 1, 1, 1)),
            guideVersion = GuideProtocolVersion.EVIDENCE_V2, guideCheckpoint = checkpoint,
            parsedMaterials = listOf(source), normalizationPending = true)
        val encoded = GuideResultCodec.encode(state)
        assertTrue("v2 evidence must never be silently downgraded",
            runCatching { GuideResultCodec.decode(encoded.replace("\"protocolVersion\":2", "\"protocolVersion\":1")) }.isFailure)
        assertEquals(3, Json.parseToJsonElement(encoded).jsonObject["version"]!!.jsonPrimitive.int)
        val restored = GuideResultCodec.decode(encoded)
        assertEquals(GuideProtocolVersion.EVIDENCE_V2, restored.guideVersion)
        assertEquals("公园", restored.guideCheckpoint!!.sources.single().evidence.single().rawQuote)
        assertEquals(resolution, restored.guideCheckpoint!!.resolution)
        assertTrue(restored.normalizationPending)
        assertEquals(restored.guideCheckpoint!!.sources, restored.parsedMaterials)
        assertEquals(GuideProtocolVersion.LEGACY_V1, GuideResultCodec.decode(
            GuideResultCodec.encode(PlaceAssistantState(input = "旧批次"))).guideVersion)
        try {
            GuideResultCodec.decode(encoded.replace("\"version\":3", "\"version\":99"))
            fail("future version must not be downgraded")
        } catch (_: IllegalArgumentException) {}
        val corrupted = encoded.replace(e.id, "f".repeat(64))
        assertTrue("invalid evidence identities cannot survive restart",
            runCatching { GuideResultCodec.decode(corrupted) }.isFailure)
        val mentionId = resolution.places.single().mentions.single().id
        assertTrue(runCatching { GuideResultCodec.decode(encoded.replace(mentionId, "e".repeat(64))) }.isFailure)
        assertTrue(runCatching { GuideResultCodec.decode(encoded.replace("\"version\":3", "\"version\":\"2\"")) }.isFailure)
        assertTrue("source ordinal must still identify its actual saved image",
            runCatching { GuideResultCodec.decode(encoded.replaceFirst("\"ordinal\":1", "\"ordinal\":2")) }.isFailure)
    }
    @Test fun resultRejectsConflictingImageSlotsDescriptorsAndCombinedLimits() {
        val id = "11111111-1111-4111-8111-111111111111"
        val image = AssistantImage(id, 1, "image/jpeg", 1, 1, 1)
        fun rejects(plan: OptionalImagePlan) {
            val state = PlaceAssistantState(input = "正文", images = listOf(image), optionalImages = plan)
            assertTrue(runCatching { GuideResultCodec.decode(GuideResultCodec.encode(state)) }.isFailure)
        }
        rejects(OptionalImagePlan.local(listOf(image.copy(ordinal = 2))))
        rejects(OptionalImagePlan.local(listOf(image.copy(byteCount = 2))))
        rejects(OptionalImagePlan(listOf(OptionalImageAsset(
            id, 1, null, null, OptionalImageStatus.SKIPPED, skip = OptionalImageSkip.MISSING))))
        val other = "22222222-2222-4222-8222-222222222222"
        rejects(OptionalImagePlan.local(listOf(image.copy(id = other))))
        val twenty = (1..20).map { slot ->
            OptionalImageAsset("00000000-0000-4000-8000-${slot.toString().padStart(12, '0')}", slot,
                null, null, OptionalImageStatus.SKIPPED, skip = OptionalImageSkip.MISSING)
        }
        rejects(OptionalImagePlan(twenty))
    }
}
