package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

internal fun classifiedMentionWire(literal: String, purpose: String = "SIGHTSEEING",
    granularity: String = "DESTINATION", quote: String = literal) =
    """${mentionWire(literal).dropLast(1)},"classification":{"purpose":"$purpose","granularity":"$granularity","independentVisit":true,"evidenceQuote":"$quote"}}"""

class GuideEntityClassificationTest {
    private val context = GuideContext("", "北城", null)
    private fun groupWire(member: String) =
        """{"members":[$member],"relation":"SAME_LITERAL","evidenceRefs":[${member.substringAfter("[").substringBefore(",")}],"frozenAnchors":[]}"""
    private fun classification(purpose: String, level: String, quote: String) =
        """"classification":{"purpose":"$purpose","granularity":"$level","independentVisit":true,"evidenceQuote":"$quote"}"""

    @Test fun evidenceProtocolFiltersLodgingAndInternalStopsWithoutLosingIndependentMuseum() {
        val evidence = listOf(fixtureEvidence("历史博物馆", 0), fixtureEvidence("推荐山间客栈", 1),
            fixtureEvidence("内部舞台", 2))
        val args = """{"evidenceResults":[
            {"e":0,"status":"PARSED","mentions":[${mentionWire("历史博物馆").dropLast(1)},${classification("SIGHTSEEING", "DESTINATION", "历史博物馆")}}]},
            {"e":1,"status":"PARSED","mentions":[${mentionWire("山间客栈").dropLast(1)},${classification("LODGING", "DESTINATION", "推荐山间客栈")}}]},
            {"e":2,"status":"EXCLUDED","reason":"NOT_PLACE",${classification("OTHER", "INTERNAL_DETAIL", "内部舞台")}}
        ],"groups":[${groupWire("[0,0]")},${groupWire("[1,0]")}]}"""
        val result = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList(), requireClassification = true)
        assertEquals(listOf("历史博物馆"), result.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e1", "e2"), result.excludedEvidenceIds)
        assertTrue(result.pending.isEmpty())
    }

    @Test fun missingClassificationIsIsolatedAndCannotBypassProductionFiltering() {
        val evidence = listOf(fixtureEvidence("历史博物馆", 0), fixtureEvidence("未知场馆", 1))
        val args = """{"evidenceResults":[
            {"e":0,"status":"PARSED","mentions":[${mentionWire("历史博物馆").dropLast(1)},${classification("SIGHTSEEING", "DESTINATION", "历史博物馆")}}]},
            {"e":1,"status":"PARSED","mentions":[${mentionWire("未知场馆")}]}
        ],"groups":[${groupWire("[0,0]")},${groupWire("[1,0]")}]}"""
        val result = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList(), requireClassification = true)
        assertEquals(listOf("历史博物馆"), result.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e1"), result.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun unknownDiscardCannotLoseAnAuthorDestination() {
        val evidence = listOf(fixtureEvidence("历史博物馆", 0))
        val args = """{"evidenceResults":[{"e":0,"status":"EXCLUDED","reason":"NOT_PLACE",
            ${classification("UNKNOWN", "UNKNOWN", "历史博物馆")}}],"groups":[]}"""
        val result = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList(), requireClassification = true)
        assertTrue(result.excludedEvidenceIds.isEmpty())
        assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun unknownAndDroppedMentionsInOneExcerptDoNotBlockItsIndependentDestination() {
        val evidence = listOf(fixtureEvidence("历史博物馆、推荐山间客栈、待查场馆", 0))
        val args = """{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[
            ${classifiedMentionWire("历史博物馆")},
            ${classifiedMentionWire("山间客栈", "LODGING", quote = "推荐山间客栈")},
            ${mentionWire("待查场馆")}]}],"groups":[
            ${groupWire("[0,0]")},${groupWire("[0,1]")},${groupWire("[0,2]")}]}"""
        val result = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList(), true)
        assertEquals(listOf("历史博物馆"), result.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
        assertTrue(result.excludedEvidenceIds.isEmpty())
    }

    @Test fun inventedDropCitationIsPendingRatherThanSilentlyDeleted() {
        val evidence = listOf(fixtureEvidence("历史博物馆", 0))
        val args = """{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[
            ${classifiedMentionWire("历史博物馆", "LODGING", quote = "虚构客栈")}]}],
            "groups":[${groupWire("[0,0]")}]}"""
        val result = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList(), true)
        assertTrue(result.groups.isEmpty())
        assertTrue(result.excludedEvidenceIds.isEmpty())
        assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun fullyDroppedMixedExcerptIsExcludedWithoutRequiringDestinationGroups() {
        val evidence = listOf(fixtureEvidence("山间客栈、普通南门", 0))
        val args = """{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[
            ${classifiedMentionWire("山间客栈", "LODGING")},
            ${classifiedMentionWire("普通南门", granularity = "INTERNAL_DETAIL")}]}],"groups":[]}"""
        val result = GuideEntityProtocol.decode(entityReply(args), evidence, context, emptyList(), true)
        assertTrue(result.groups.isEmpty())
        assertTrue(result.pending.isEmpty())
        assertEquals(setOf("e0"), result.excludedEvidenceIds)
    }
}
