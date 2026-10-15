package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

internal fun fixtureResolution(e: GuideEvidence, anchor: String? = null): GuideResolution {
    val m = fixtureMention(e)
    val d = GuideNameDecision(m.literal, false, "", emptyList())
    return GuideResolution(listOf(GuideResolvedPlace(PlaceIntent(m.literal, m.city, e.rawQuote,
        materialKeys = setOf(m.id)), listOf(m), d, GuideAliasKind.SAME_LITERAL, setOf(e.id),
        setOfNotNull(anchor))), emptyList(), emptySet())
}

class GuideEvidenceSessionTest {
    @Test fun frozenTextEntranceRetainsParentAcrossCheckpointAndRejectsAnotherParent() {
        val raw = fixtureEvidence("甲园南门", 0, image = false)
        val e = raw.copy(id = guideEvidenceId(raw.binding, raw.sourceId, raw.sourceFingerprint, raw.kind, raw.rawQuote))
        val context = GuideContext("", "北市", null)
        fun wire(parent: String, anchor: String = "") = entityReply("""{"evidenceResults":[
          {"e":0,"status":"PARSED","mentions":[${mentionWire("南门", "DISTINCT_PART", parent)}]}],
          "groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],
          "frozenAnchors":[${if (anchor.isEmpty()) "" else "\"$anchor\""}]}]}""")
        val group = GuideEntityProtocol.decode(wire("甲园"), listOf(e), context, emptyList()).groups.single()
        val intent = PlaceIntent("南门", "北市", e.rawQuote, materialKeys = group.members.map { it.id }.toSet(),
            sources = listOf(IntakeSource.Text(e.rawQuote)))
        val resolution = GuideResolution(listOf(GuideResolvedPlace(intent, group.members,
            GuideNameSelector.select(group, listOf(e)), group.relation, group.relationEvidenceIds, emptySet())),
            emptyList(), emptySet())
        val session = GuideEvidenceSession(GuideEvidenceCheckpoint(e.binding,
            listOf(ParsedSource("text", emptyList(), evidence = listOf(e))), resolution, emptyList()))
        val original = IntakeItem("original", "A", intent)
        session.freeze(original, true)
        val restored = com.yangchengwei.easytrip.assistant.cityfirst.GuideEvidenceCheckpointCodec.decode(
            com.yangchengwei.easytrip.assistant.cityfirst.GuideEvidenceCheckpointCodec.encode(session.checkpoint))
        val anchor = restored.frozen.single().anchorId
        val other = fixtureEvidence("乙园南门", 1, image = false)
        val decoded = GuideEntityProtocol.decode(wire("乙园", anchor), listOf(other), context, restored.frozen)
        assertTrue("text parent must not be lost on freezing", decoded.groups.isEmpty())
        assertEquals(1, decoded.pending.size)
        assertEquals(1, GuideEntityProtocol.decode(wire("甲园", anchor), listOf(e), context, restored.frozen).groups.size)
        val north = fixtureEvidence("甲园北门", 2, image = false)
        assertTrue("same park's north gate is not the frozen south gate",
            GuideEntityProtocol.decode(wire("甲园", anchor).replace("南门", "北门"),
                listOf(north), context, restored.frozen).groups.isEmpty())
        val wrong = resolution.copy(places = resolution.places.map { p -> p.copy(
            mentions = p.mentions.map { it.copy(parentLiteral = "乙园") }, frozenAnchorIds = setOf(anchor)) })
        val recoveredSession = GuideEvidenceSession(restored)
        assertTrue(recoveredSession.apply(wrong, listOf(original)).absorbedMaterialKeys.isEmpty())
    }

    @Test fun reusedMaterialIdentityCannotMergeDifferentCities() {
        val e = fixtureEvidence("公园", 0)
        val first = fixtureResolution(e)
        val original = IntakeItem("original", "A", first.places.single().intent.copy(city = "甲城市"))
        val session = GuideEvidenceSession(GuideEvidenceCheckpoint(e.binding, emptyList(),
            first, emptyList()))
        session.freeze(original, committed = true)
        val other = first.copy(places = first.places.map {
            it.copy(intent = it.intent.copy(city = "乙城市"))
        })
        val applied = session.apply(other, listOf(original))
        assertTrue(applied.additions.isEmpty())
        assertTrue(applied.absorbedMaterialKeys.isEmpty())
        assertEquals(1, session.checkpoint.resolution.pending.size)
        assertEquals("甲城市", session.checkpoint.frozen.single().item.intent.city)
    }

    @Test fun successfulPlaceSurvivesPendingAndFrozenManualNameAbsorbsNewIdentityWithoutDuplicate() {
        val e = fixtureEvidence("公园", 0)
        val session = GuideEvidenceSession(GuideEvidenceCheckpoint(e.binding, emptyList(),
            GuideResolution(emptyList(), emptyList(), emptySet()), emptyList()))
        session.accept(ParsedSource(e.sourceId, emptyList(), evidence = listOf(e)))
        val r = fixtureResolution(e).copy(pending = listOf(GuidePendingEvidence(setOf("e1"), "待整理")))
        assertEquals(listOf("公园"), session.apply(r, emptyList()).additions.map { it.query })
        assertEquals(r.pending, session.checkpoint.resolution.pending)
        val item = IntakeItem("i1", "A", r.places.single().intent.copy(query = "手动名称"), generation = 1)
        session.freeze(item, committed = true)
        val anchor = session.checkpoint.frozen.single().anchorId
        val next = fixtureResolution(fixtureEvidence("公园全称", 2), anchor)
        val applied = session.apply(next, listOf(item))
        assertTrue(applied.additions.isEmpty())
        assertTrue(applied.absorbedMaterialKeys.getValue("i1").contains("me2"))
        assertEquals("手动名称", session.checkpoint.frozen.single().item.intent.query)
        assertTrue(session.checkpoint.frozen.single().committed)
    }
}
