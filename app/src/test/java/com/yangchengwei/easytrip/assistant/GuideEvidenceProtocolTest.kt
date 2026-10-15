package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

internal fun evidenceReply(args: String) = guideReply(args).replace("extract_guide_sources", "extract_guide_evidence")

class GuideEvidenceProtocolTest {
    private val binding = GuideBatchBinding("batch-a", 0, "北市")
    private val image = GuidePreparedSource(GuideSource("image_1", "img-a", 1), null,
        sourceFingerprint = "a".repeat(64))

    @Test fun textualKindDoesNotOverrideVerifiedSourceIdentity() {
        val result = GuideEvidenceProtocol.decode(evidenceReply("""{"sources":[
          {"sourceId":"text","status":"OK","evidence":[
           {"rawQuote":"北湖","kind":"AUTHOR_LABEL","basis":"RECOMMENDED","uncertain":false,"detail":""}]},
          {"sourceId":"image_1","status":"OK","evidence":[
           {"rawQuote":"园内有纪念祠","kind":"BODY_EXCERPT","basis":"RECOMMENDED","uncertain":false,"detail":""}]}]}"""),
            GuideContext("北湖", "北市", null), listOf(
                GuidePreparedSource(GuideSource("text", "text", null), null, sourceFingerprint = "b".repeat(64)),
                image), binding)
        assertTrue(result.sources.all { it.failure == null })
        assertNull(result.sources[0].evidence.single().ordinal)
        assertEquals(1, result.sources[1].evidence.single().ordinal)
        assertTrue(result.repairableSourceIds.isEmpty())
    }

    @Test fun completeBracketLabelIsEvidenceNotPlaceholderPlace() {
        val args = """{"sources":[{"sourceId":"image_1","status":"OK","evidence":[
          {"rawQuote":"北湖（北山南门）","kind":"AUTHOR_LABEL","basis":"AUTHOR_MARK",
           "uncertain":false,"detail":"作者路线标签"}]}]}"""
        val result = GuideEvidenceProtocol.decode(evidenceReply(args), GuideContext("", "北市", null),
            listOf(image), binding).sources.single()
        assertNull(result.failure)
        assertTrue(result.intents.isEmpty())
        assertEquals("北湖（北山南门）", result.evidence.single().rawQuote)
        assertEquals("img-a", result.evidence.single().sourceId)
    }

    @Test fun inventedBodyQuoteCannotAcquireIdentityOrTriggerRepairAndOtherSourceSurvives() {
        val args = """{"sources":[
          {"sourceId":"text","status":"OK","evidence":[
           {"rawQuote":"不存在的地点","kind":"BODY_EXCERPT","basis":"RECOMMENDED","uncertain":false,"detail":""}]},
          {"sourceId":"image_1","status":"NO_PLACES","evidence":[]}]}"""
        val result = GuideEvidenceProtocol.decode(evidenceReply(args), GuideContext("真实正文", "", null),
            listOf(GuidePreparedSource(GuideSource("text", "text", null), null,
                sourceFingerprint = "b".repeat(64)), image), binding)
        assertNotNull(result.sources[0].failure)
        assertTrue(result.sources[0].evidence.isEmpty())
        assertNull(result.sources[1].failure)
        assertTrue(result.repairableSourceIds.isEmpty())
    }

    @Test fun repeatedQuoteDoesNotAddSupportOrEraseUncertaintyAndRevisionChangesIdentity() {
        val args = """{"sources":[{"sourceId":"image_1","status":"OK","evidence":[
          {"rawQuote":"北湖","kind":"AUTHOR_LABEL","basis":"AUTHOR_MARK","uncertain":false,"detail":""},
          {"rawQuote":"北湖","kind":"AUTHOR_LABEL","basis":"AUTHOR_MARK","uncertain":true,"detail":"字迹模糊"}]}]}"""
        fun decode(b: GuideBatchBinding) = GuideEvidenceProtocol.decode(evidenceReply(args),
            GuideContext("", "", null), listOf(image), b).sources.single().evidence
        val a = decode(binding)
        assertEquals(1, a.size)
        assertTrue(a.single().uncertain)
        assertEquals(a.single().id, decode(binding).single().id)
        assertNotEquals(a.single().id, decode(binding.copy(revision = 1)).single().id)
    }

    @Test fun source101IsExplicitOverflowNotTruncatedAndValidEmptySourceSurvives() {
        val entries = (0..100).joinToString(",") {
            """{"rawQuote":"公园$it","kind":"AUTHOR_LABEL","basis":"AUTHOR_MARK","uncertain":false,"detail":""}"""
        }
        val result = GuideEvidenceProtocol.decode(evidenceReply("""{"sources":[
          {"sourceId":"image_1","status":"OK","evidence":[$entries]},
          {"sourceId":"text","status":"NO_PLACES","evidence":[]}]}"""),
            GuideContext("正文", "", null), listOf(image,
                GuidePreparedSource(GuideSource("text", "text", null), null)), binding)
        assertEquals(AssistantFailureCode.OVERFLOW, result.sources[0].failure?.code)
        assertTrue(result.sources[0].evidence.isEmpty())
        assertNull(result.sources[1].failure)
        assertTrue(result.repairableSourceIds.isEmpty())
    }

    @Test fun metadataRequestsFullQuotesRatherThanPlacesAndDoesNotLeakActualTravelIntoSystem() {
        val metadata = GuideEvidenceProtocol.metadata(ProviderConfig("https://example.invalid", "fixture", "not-real"),
            GuideContext("用户资料", "某城市", null), listOf(image.source), GuideBatchPolicy(100000, 32768), false)
        val tool = metadata.getValue("tools").jsonArray.single().jsonObject.getValue("function").jsonObject
        assertEquals("extract_guide_evidence", tool.text("name"))
        val system = metadata.getValue("messages").jsonArray.first().jsonObject.text("content")
        assertFalse(system.contains("某城市"))
        assertFalse(system.contains("用户资料"))
        assertTrue(tool.toString().contains("rawQuote"))
        assertFalse(tool.toString().contains("\"query\""))
    }
}
