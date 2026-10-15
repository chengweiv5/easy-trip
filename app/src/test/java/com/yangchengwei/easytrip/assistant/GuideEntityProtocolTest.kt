package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

internal fun entityReply(args: String) = guideReply(args).replace("extract_guide_sources", "resolve_guide_entities")
internal fun fixtureEvidence(quote: String, index: Int, image: Boolean = true,
    uncertain: Boolean = false) = GuideEvidence(
    "e$index", if (image) "img$index" else "text", if (image) index + 1 else null,
    index.toString().padStart(64, '0'), GuideBatchBinding("b", 0, "北市"), quote,
    if (image) GuideEvidenceKind.AUTHOR_LABEL else GuideEvidenceKind.BODY_EXCERPT,
    ImageTargetBasis.AUTHOR_MARK, uncertain, "")
internal fun mentionWire(literal: String, role: String = "PLACE", parent: String = "", city: String = "") =
    """{"literal":"$literal","occurrence":0,"city":"$city","cityEvidenceRefs":[],
        "role":"$role","parentLiteral":"$parent","partKind":${if (role == "PLACE") "null" else "\"GATE\""},"uncertain":false}"""

class GuideEntityProtocolTest {
    private val context = GuideContext("", "北市", null)
    @Test fun oppositeBracketedEntrancesCannotBecomeOneAliasGroup() {
        for ((left, right) in listOf("甲园（南门）" to "甲园（北门）",
            "甲园(东门)" to "甲园(西门)", "甲园东城门" to "甲园西城门")) {
            val es = listOf(fixtureEvidence(left, 0), fixtureEvidence(right, 1))
            val decoded = GuideEntityProtocol.decode(entityReply("""{"evidenceResults":[
              {"e":0,"status":"PARSED","mentions":[${mentionWire(left, "DISTINCT_PART", "甲园")}]},
              {"e":1,"status":"PARSED","mentions":[${mentionWire(right, "DISTINCT_PART", "甲园")}]}],
              "groups":[{"members":[[0,0],[1,0]],"relation":"CONTEXT_ALIAS","evidenceRefs":[0,1],"frozenAnchors":[]}]}"""),
                es, context, emptyList())
            assertTrue("$left and $right cannot merge", decoded.groups.isEmpty())
        }
    }
    @Test fun oneBadAliasDoesNotCascadeThroughOtherMentionsOfSameRoute() {
        val evidence = listOf(fixtureEvidence("公园—博物馆", 0), fixtureEvidence("无地点的说明", 1))
        val args = """{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${mentionWire("公园")},${mentionWire("博物馆")}]},
          {"e":1,"status":"PARSED","mentions":[${mentionWire("公园")}]}],
          "groups":[{"members":[[0,0],[1,0]],"relation":"SAME_LITERAL","evidenceRefs":[0,1],"frozenAnchors":[]},
                    {"members":[[0,1]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]}]}"""
        val decoded = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList())
        assertEquals(listOf("博物馆"), decoded.groups.flatMap { it.members }.map { it.literal })
        assertTrue(decoded.pending.isNotEmpty())
    }
    @Test fun stringNumbersAndMalformedGroupsCannotHideLinkedEvidence() {
        val evidence = listOf(fixtureEvidence("公园", 0), fixtureEvidence("美术馆", 1),
            fixtureEvidence("博物馆", 2))
        val args = """{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${mentionWire("公园").replace("\"occurrence\":0", "\"occurrence\":\"0\"")}]},
          {"e":1,"status":"PARSED","mentions":[${mentionWire("美术馆")}]},
          {"e":2,"status":"PARSED","mentions":[${mentionWire("博物馆")}]}],
          "groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]},
            {"members":[[999,0],[1,0]],"relation":"CONTEXT_ALIAS","evidenceRefs":[1],"frozenAnchors":[]},
            {"members":[[1,0]],"relation":"SAME_LITERAL","evidenceRefs":[1],"frozenAnchors":[]},
            {"members":[[2,0]],"relation":"SAME_LITERAL","evidenceRefs":[2],"frozenAnchors":[]}]}"""
        val d = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList())
        assertEquals(listOf("博物馆"), d.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e0", "e1"), d.pending.flatMap { it.evidenceIds }.toSet())
    }
    @Test fun frozenCityAndParentCannotBeReassignedByModel() {
        val e = fixtureEvidence("乙园南门", 0)
        val frozen = GuideFrozenPlace("f1", IntakeItem("i", "A",
            PlaceIntent("南门", "南市", "甲园南门", sources = listOf(IntakeSource.Image("img",
                1, "南门", ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "",
                    ImageEntityRole.DISTINCT_PART, "甲园"))))), emptySet(), true)
        val args = """{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[
          ${mentionWire("南门", "DISTINCT_PART", "乙园")}]}],
          "groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":["f1"]}]}"""
        val d = GuideEntityProtocol.decode(entityReply(args), listOf(e), context, listOf(frozen))
        assertTrue(d.groups.isEmpty())
        assertEquals(setOf("e0"), d.pending.flatMap { it.evidenceIds }.toSet())
    }
    @Test fun oneCombinedOrMissingEvidenceDoesNotEraseAnotherValidGroup() {
        val evidence = listOf(fixtureEvidence("北湖（北山南门）", 0), fixtureEvidence("美术馆", 1),
            fixtureEvidence("公园", 2))
        val args = """{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${mentionWire("北湖（北山南门）")}]},
          {"e":1,"status":"PARSED","mentions":[${mentionWire("美术馆")}]}],
          "groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]},
                    {"members":[[1,0]],"relation":"SAME_LITERAL","evidenceRefs":[1],"frozenAnchors":[]}]}"""
        val decoded = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList())
        assertEquals(listOf("美术馆"), decoded.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e0", "e2"), decoded.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun overlappingGroupsAndMissingMentionAreNotSilentlyAccepted() {
        val evidence = listOf(fixtureEvidence("美术馆公园", 0), fixtureEvidence("博物馆", 1))
        val args = """{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${mentionWire("美术馆")},${mentionWire("公园")}]},
          {"e":1,"status":"PARSED","mentions":[${mentionWire("博物馆")}]}],
          "groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]},
                    {"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]},
                    {"members":[[1,0]],"relation":"SAME_LITERAL","evidenceRefs":[1],"frozenAnchors":[]}]}"""
        val d = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList())
        assertEquals(listOf("博物馆"), d.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e0"), d.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun twoParentsSouthGatesDoNotMergeAndUnrelatedCityReferencesAreRejected() {
        val evidence = listOf(fixtureEvidence("甲园南门", 0), fixtureEvidence("乙园南门", 1),
            fixtureEvidence("博物馆", 2))
        val args = """{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${mentionWire("南门", "DISTINCT_PART", "甲园")}]},
          {"e":1,"status":"PARSED","mentions":[${mentionWire("南门", "DISTINCT_PART", "乙园")}]},
          {"e":2,"status":"PARSED","mentions":[${mentionWire("博物馆", city = "异地")}]}],
          "groups":[{"members":[[0,0],[1,0]],"relation":"SAME_LITERAL","evidenceRefs":[0,1],"frozenAnchors":[]},
                    {"members":[[2,0]],"relation":"SAME_LITERAL","evidenceRefs":[2],"frozenAnchors":[]}]}"""
        val d = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList())
        assertTrue(d.groups.isEmpty())
        assertEquals(setOf("e0", "e1", "e2"), d.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun diningCanBeExcludedButAuthorTargetCannotDisappearAsBackground() {
        val evidence = listOf(fixtureEvidence("餐厅", 0), fixtureEvidence("公园", 1))
        val args = """{"evidenceResults":[{"e":0,"status":"EXCLUDED","reason":"FOOD_OR_DINING"},
            {"e":1,"status":"EXCLUDED","reason":"BACKGROUND"}],"groups":[]}"""
        val d = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList())
        assertEquals(setOf("e0"), d.excludedEvidenceIds)
        assertEquals(setOf("e1"), d.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun literalCannotClearVisualUncertaintyAndSource101CannotHideAsPending() {
        val e = fixtureEvidence("公园", 0, uncertain = true)
        val parsed = """{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[${mentionWire("公园")}]}],
          "groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]}]}"""
        assertTrue(GuideEntityProtocol.decode(entityReply(parsed), listOf(e), context, emptyList())
            .groups.single().members.single().uncertain)
        val many = """{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[${
            List(101) { mentionWire("公园") }.joinToString(",")}]}],"groups":[]}"""
        try {
            GuideEntityProtocol.decode(entityReply(many), listOf(e), context, emptyList())
            fail("expected explicit overflow")
        } catch (failure: AssistantFailure) { assertEquals(AssistantFailureCode.OVERFLOW, failure.code) }
    }
}
