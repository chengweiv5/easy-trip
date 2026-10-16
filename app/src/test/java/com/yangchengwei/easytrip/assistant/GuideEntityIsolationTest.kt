package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Exercises the production protocol boundary, not a second implementation of its rules. */
class GuideEntityIsolationTest {
    private val context = GuideContext("", "徐州市", null)

    @Test fun invalidExcludedExitDoesNotSuppressLaterDestinationInTheSameExcerpt() {
        val quote = "徐博物馆出口→徐州饮食文化博物馆"
        val evidence = listOf(fixtureEvidence(quote, 0, image = false))
        val result = GuideEntityProtocol.decode(entityReply(buildJsonObject {
            putJsonArray("evidenceResults") {
                add(entry(0, badExit(quote), destination("徐州饮食文化博物馆", quote)))
            }
            putJsonArray("groups") { add(group(0 to 1)) }
        }.toString()), evidence, context, emptyList(), true)

        assertEquals(listOf("徐州饮食文化博物馆"),
            result.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
        assertTrue(result.excludedEvidenceIds.isEmpty())
    }

    @Test fun invalidAliasOccurrenceDoesNotSuppressIndependentImageEvidence() {
        val evidence = listOf(
            fixtureEvidence("徐州饮食文化博物馆", 0, image = false),
            fixtureEvidence("徐州饮食文化博物馆", 1),
        )
        val invalid = buildJsonObject {
            destination("徐州饮食文化博物馆").forEach { (key, value) -> put(key, value) }
            put("occurrence", 5) // Only this occurrence is unsupported by the original.
        }
        val result = decode(evidence, listOf(entry(0, invalid),
            entry(1, destination("徐州饮食文化博物馆"))), group(0 to 0, 1 to 0))

        assertEquals(listOf("e1"), result.groups.single().members.map { it.evidenceId })
        assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun malformedOccurrencesKeepTheirOriginalIndicesAndNeverEnterResults() {
        val quote = "甲馆→乙馆→丙馆"
        val evidence = listOf(fixtureEvidence(quote, 0, image = false))
        val malformed = buildJsonObject {
            destination("乙馆", quote).forEach { (key, value) -> put(key, value) }
            put("cityEvidenceRefs", JsonArray(listOf(JsonPrimitive(999))))
        }
        val result = decode(evidence, listOf(entry(0, destination("甲馆", quote), malformed,
            destination("丙馆", quote))), group(0 to 0), group(0 to 1), group(0 to 2))

        assertEquals(listOf("甲馆", "丙馆"), result.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun malformedFirstOrLastOccurrenceDoesNotTaintAnotherOriginalOccurrence() {
        val quote = "甲馆→乙馆"
        val malformed = buildJsonObject {
            destination("甲馆", quote).forEach { (key, value) -> put(key, value) }
            put("occurrence", "0")
        }
        for (first in listOf(true, false)) {
            val mentions = if (first) arrayOf(malformed, destination("乙馆", quote))
                else arrayOf(destination("乙馆", quote), malformed)
            val result = decode(listOf(fixtureEvidence(quote, 0, image = false)),
                listOf(entry(0, *mentions)), group(0 to if (first) 1 else 0))
            assertEquals(listOf("乙馆"), result.groups.flatMap { it.members }.map { it.literal })
            assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
        }
    }

    @Test fun isolatingBadOccurrenceDoesNotAllowCrossCityAliasOrOverwriteFrozenPlace() {
        val evidence = listOf(fixtureEvidence("坏字段", 0), fixtureEvidence("徐州市历史馆", 1),
            fixtureEvidence("南京市历史馆", 2))
        val invalid = destination("原文不存在的名称")
        fun inCity(city: String, e: Int) = buildJsonObject {
            destination("历史馆", "${city}历史馆").forEach { (key, value) -> put(key, value) }
            put("city", city); put("cityEvidenceRefs", JsonArray(listOf(JsonPrimitive(e))))
        }
        val result = decode(evidence, listOf(entry(0, invalid), entry(1, inCity("徐州市", 1)),
            entry(2, inCity("南京市", 2))), group(0 to 0, 1 to 0, 2 to 0))
        assertTrue(result.groups.isEmpty())
        assertEquals(setOf("e0", "e1", "e2"), result.pending.flatMap { it.evidenceIds }.toSet())

        val frozen = GuideFrozenPlace("frozen", IntakeItem("saved", "A",
            PlaceIntent("历史馆", "南京市", "南京市历史馆")), emptySet(), true)
        val anchored = buildJsonObject {
            group(0 to 0, 1 to 0).forEach { (key, value) -> put(key, value) }
            put("frozenAnchors", JsonArray(listOf(JsonPrimitive("frozen"))))
        }
        val anchoredResult = GuideEntityProtocol.decode(entityReply(buildJsonObject {
            put("evidenceResults", JsonArray(listOf(entry(0, invalid), entry(1, inCity("徐州市", 1)))))
            put("groups", JsonArray(listOf(anchored)))
        }.toString()), evidence.take(2), context, listOf(frozen), true)
        assertTrue(anchoredResult.groups.isEmpty())
    }

    @Test fun badOccurrenceCannotHideParentChildConflictOrDuplicateGroups() {
        val evidence = listOf(fixtureEvidence("错误项", 0), fixtureEvidence("甲园", 1),
            fixtureEvidence("甲园独立历史城门", 2))
        val part = buildJsonObject {
            destination("独立历史城门", "甲园独立历史城门").forEach { (key, value) -> put(key, value) }
            put("role", "DISTINCT_PART"); put("parentLiteral", "甲园"); put("partKind", "GATE")
        }
        val mixed = buildJsonObject {
            group(0 to 0, 1 to 0, 2 to 0).forEach { (key, value) -> put(key, value) }
            put("relation", "CONTEXT_ALIAS")
        }
        val result = decode(evidence, listOf(entry(0, destination("不存在")),
            entry(1, destination("甲园")), entry(2, part)), mixed)
        assertTrue(result.groups.isEmpty())

        val duplicate = decode(evidence.take(2), listOf(entry(0, destination("不存在")),
            entry(1, destination("甲园"))), group(0 to 0, 1 to 0), group(1 to 0))
        assertTrue(duplicate.groups.isEmpty())
    }

    @Test fun unrecognizedGroupReferenceAndConflictingEvidenceReceiptsStillFailClosed() {
        val evidence = listOf(fixtureEvidence("甲馆", 0), fixtureEvidence("乙馆", 1))
        val unknown = decode(evidence, listOf(entry(0, destination("甲馆")), entry(1, destination("乙馆"))),
            group(0 to 0, 0 to 99), group(1 to 0))
        assertEquals(listOf("乙馆"), unknown.groups.flatMap { it.members }.map { it.literal })

        val duplicateReceipt = decode(evidence, listOf(entry(0, destination("甲馆")),
            entry(0, destination("甲馆")), entry(1, destination("乙馆"))), group(0 to 0), group(1 to 0))
        assertEquals(listOf("乙馆"), duplicateReceipt.groups.flatMap { it.members }.map { it.literal })
        assertEquals(setOf("e0"), duplicateReceipt.pending.flatMap { it.evidenceIds }.toSet())
    }

    @Test fun solelyMalformedOccurrenceIsPendingRatherThanSilentlyExcluded() {
        val result = decode(listOf(fixtureEvidence("徐博物馆出口", 0)),
            listOf(entry(0, badExit("徐博物馆出口"))), group(0 to 0))
        assertTrue(result.groups.isEmpty())
        assertTrue(result.excludedEvidenceIds.isEmpty())
        assertEquals(setOf("e0"), result.pending.flatMap { it.evidenceIds }.toSet())
    }

    private fun badExit(quote: String) = buildJsonObject {
        destination("徐博物馆出口", quote).forEach { (key, value) -> put(key, value) }
        put("role", "DISTINCT_PART")
        // The real model supplied this parent, but it is not present in the original quote.
        put("parentLiteral", "徐州博物馆")
        put("partKind", "EXIT")
        put("classification", classification("TRANSPORT", "INTERNAL_DETAIL", false, quote))
    }

    private fun destination(literal: String, quote: String = literal) = buildJsonObject {
        put("literal", literal); put("occurrence", 0); put("city", "")
        putJsonArray("cityEvidenceRefs") {}
        put("role", "PLACE"); put("parentLiteral", ""); put("partKind", JsonNull)
        put("uncertain", false)
        put("classification", classification("SIGHTSEEING", "DESTINATION", true, quote))
    }

    private fun classification(purpose: String, level: String, independent: Boolean, quote: String) =
        buildJsonObject {
            put("purpose", purpose); put("granularity", level)
            put("independentVisit", independent); put("evidenceQuote", quote)
        }

    private fun entry(index: Int, vararg mentions: JsonObject) = buildJsonObject {
        put("e", index); put("status", "PARSED")
        put("mentions", JsonArray(mentions.toList()))
    }

    private fun group(vararg members: Pair<Int, Int>) = buildJsonObject {
        put("members", JsonArray(members.map { (e, m) -> JsonArray(listOf(JsonPrimitive(e), JsonPrimitive(m))) }))
        put("relation", "SAME_LITERAL")
        put("evidenceRefs", JsonArray(members.map { it.first }.distinct().map(::JsonPrimitive)))
        putJsonArray("frozenAnchors") {}
    }

    private fun decode(evidence: List<GuideEvidence>, entries: List<JsonObject>, vararg groups: JsonObject) =
        GuideEntityProtocol.decode(entityReply(buildJsonObject {
            put("evidenceResults", JsonArray(entries))
            put("groups", JsonArray(groups.toList()))
        }.toString()), evidence, context, emptyList(), true)
}
