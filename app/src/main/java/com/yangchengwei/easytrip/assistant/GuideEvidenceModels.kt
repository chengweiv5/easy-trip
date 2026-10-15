package com.yangchengwei.easytrip.assistant

import java.nio.ByteBuffer
import java.security.MessageDigest

enum class GuideProtocolVersion { LEGACY_V1, EVIDENCE_V2 }
enum class GuideEvidenceKind { AUTHOR_LABEL, BODY_EXCERPT, CAPTION_EXCERPT }
enum class GuideAliasKind { SAME_LITERAL, CONTEXT_ALIAS, SPELLING_VARIANT }
enum class GuidePartKind { ENTRANCE, EXIT, GATE, OTHER }
data class GuideBatchBinding(val batchId: String, val revision: Long, val cityScopeKey: String)
data class GuideEvidence(
    val id: String, val sourceId: String, val ordinal: Int?,
    val sourceFingerprint: String, val binding: GuideBatchBinding,
    val rawQuote: String, val kind: GuideEvidenceKind,
    val basis: ImageTargetBasis, val uncertain: Boolean, val detail: String,
)
data class GuideMention(
    val id: String, val evidenceId: String, val literal: String, val start: Int, val endExclusive: Int,
    val city: String, val usesDefaultCity: Boolean, val cityEvidenceIds: Set<String>,
    val role: ImageEntityRole, val parentLiteral: String, val partKind: GuidePartKind?, val uncertain: Boolean,
)
data class GuideAliasGroup(val members: List<GuideMention>, val relation: GuideAliasKind,
    val relationEvidenceIds: Set<String>, val frozenAnchorIds: Set<String> = emptySet())
data class GuidePendingEvidence(val evidenceIds: Set<String>, val reason: String)
data class GuideEntityDecode(val groups: List<GuideAliasGroup>, val pending: List<GuidePendingEvidence>,
    val excludedEvidenceIds: Set<String>)
data class GuideEntityShape(val role: ImageEntityRole, val parent: String, val part: GuidePartKind?,
    val direction: String = "")
data class GuideFrozenPlace(val anchorId: String, val item: IntakeItem,
    val materialKeys: Set<String>, val committed: Boolean, val shapes: Set<GuideEntityShape> = emptySet())
data class GuideVote(val literal: String, val clearTargetSources: Int, val clearContextSources: Int)
data class GuideNameDecision(val literal: String, val needsReview: Boolean, val reason: String,
    val votes: List<GuideVote>)
data class GuideResolvedPlace(val intent: PlaceIntent, val mentions: List<GuideMention>,
    val decision: GuideNameDecision, val relation: GuideAliasKind,
    val relationEvidenceIds: Set<String>, val frozenAnchorIds: Set<String>)
data class GuideResolution(val places: List<GuideResolvedPlace>, val pending: List<GuidePendingEvidence>,
    val excludedEvidenceIds: Set<String>)
data class GuideEvidenceCheckpoint(val binding: GuideBatchBinding, val sources: List<ParsedSource>,
    val resolution: GuideResolution, val frozen: List<GuideFrozenPlace>)
data class GuideApplyResult(val additions: List<PlaceIntent>, val absorbedMaterialKeys: Map<String, Set<String>>)

internal fun guideEntranceDirection(literal: String): String =
    Regex("(东北|东南|西北|西南|东|南|西|北)(?:大门|城门|门|入口|出口)[）)]?$")
        .find(literal)?.groupValues?.get(1).orEmpty()
internal fun GuideMention.entityShape() = GuideEntityShape(role, parentLiteral, partKind,
    guideEntranceDirection(literal))
internal fun GuideFrozenPlace.accepts(members: List<GuideMention>): Boolean {
    val known = shapes.ifEmpty {
        item.intent.sources.filterIsInstance<IntakeSource.Image>().mapNotNull { s ->
            s.target?.let { GuideEntityShape(it.role, it.parentName, null, guideEntranceDirection(s.excerpt)) }
        }.toSet()
    }
    // Older v2 text snapshots lacked shapes. Do not invent identity for a new entrance.
    if (known.isEmpty()) return members.all { it.role == ImageEntityRole.PLACE }
    return members.all { m -> known.all { shape ->
        shape.role == m.role && (m.role == ImageEntityRole.PLACE ||
            shape.parent.isNotEmpty() && shape.parent == m.parentLiteral &&
                (shape.part == null || shape.part == m.partKind) &&
                shape.direction == guideEntranceDirection(m.literal))
    } }
}

internal fun guideIdentity(vararg fields: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    fields.forEach {
        val bytes = it.toByteArray(Charsets.UTF_8)
        digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
        digest.update(bytes)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun guideEvidenceId(binding: GuideBatchBinding, sourceId: String, fingerprint: String,
    kind: GuideEvidenceKind, rawQuote: String): String = guideIdentity(
    "guide-evidence-v2", binding.batchId, binding.revision.toString(), binding.cityScopeKey,
    sourceId, fingerprint, kind.name, rawQuote)
