package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

internal fun fixtureMention(e: GuideEvidence, literal: String = e.rawQuote) = GuideMention(
    "m${e.id}", e.id, literal, e.rawQuote.indexOf(literal), e.rawQuote.indexOf(literal) + literal.length,
    "北市", true, emptySet(), ImageEntityRole.PLACE, "", null, e.uncertain)

class GuideNameSelectorTest {
    @Test fun twoClearImagesBeatOneBodyRegardlessOfOrderAndDuplicatedImageBytesDoNotAddVotes() {
        val evidence = listOf(fixtureEvidence("回笼窝", 0, false), fixtureEvidence("回龙窝", 1),
            fixtureEvidence("回龙窝", 2))
        for (sources in listOf(evidence, evidence.reversed())) {
            val group = GuideAliasGroup(sources.map(::fixtureMention), GuideAliasKind.SPELLING_VARIANT,
                sources.map { it.id }.toSet())
            val d = GuideNameSelector.select(group, sources)
            assertEquals("回龙窝", d.literal)
            assertFalse(d.needsReview)
            assertEquals(2, d.votes.single { it.literal == "回龙窝" }.clearTargetSources)
        }
        val copies = evidence.map { if (it.id == "e2") it.copy(sourceFingerprint = evidence[1].sourceFingerprint) else it }
        val tie = GuideNameSelector.select(GuideAliasGroup(copies.map(::fixtureMention),
            GuideAliasKind.SPELLING_VARIANT, copies.map { it.id }.toSet()), copies)
        assertTrue(tie.needsReview)
        assertEquals(1, tie.votes.single { it.literal == "回龙窝" }.clearTargetSources)
    }

    @Test fun tiedKnownSuffixUsesExistingFullNameButArbitraryLongestDoesNotWin() {
        fun select(a: String, b: String, confirmed: String? = null): GuideNameDecision {
            val es = listOf(fixtureEvidence(a, 0), fixtureEvidence(b, 1))
            return GuideNameSelector.select(GuideAliasGroup(es.map(::fixtureMention),
                GuideAliasKind.CONTEXT_ALIAS, es.map { it.id }.toSet()), es, confirmed)
        }
        assertEquals("北山风景区", select("北山", "北山风景区").literal)
        assertFalse(select("北山", "北山风景区").needsReview)
        assertTrue(select("北山", "北山历史展览纪念文化中心").needsReview)
        assertEquals("手动名称", select("北山", "北山风景区", "手动名称").literal)
        val e = fixtureEvidence("公园", 0, uncertain = true)
        assertTrue(GuideNameSelector.select(GuideAliasGroup(listOf(fixtureMention(e)),
            GuideAliasKind.SAME_LITERAL, setOf(e.id)), listOf(e)).needsReview)
    }
}
