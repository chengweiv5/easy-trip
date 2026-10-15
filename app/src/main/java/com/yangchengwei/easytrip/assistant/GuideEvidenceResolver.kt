package com.yangchengwei.easytrip.assistant

fun interface GuideEvidenceResolver {
    suspend fun resolve(evidence: List<GuideEvidence>, context: GuideContext,
        frozen: List<GuideFrozenPlace>): GuideResolution
}

class ModelGuideEvidenceResolver(private val config: () -> ProviderConfig?,
    private val transport: AssistantTransport) : GuideEvidenceResolver {
    override suspend fun resolve(evidence: List<GuideEvidence>, context: GuideContext,
        frozen: List<GuideFrozenPlace>): GuideResolution {
        if (evidence.isEmpty()) return GuideResolution(emptyList(), emptyList(), emptySet())
        val current = config()?.validated() ?: throw AssistantFailure("请先配置模型。")
        if (!GuideEvidenceBudget.supports(current)) throw AssistantFailure(
            "当前服务尚未核验此攻略整理预算，原素材已保留，请检查模型设置。")
        val decoded = transport.decodeWithRepair(current,
            GuideEntityProtocol.request(current, evidence, context, frozen),
            AssistantProtocolStage.NORMALIZATION, policy = GuideEvidenceBudget.transport) {
            GuideEntityProtocol.decode(it, evidence, context, frozen, requireClassification = true)
        }
        val byId = evidence.associateBy { it.id }
        val places = decoded.groups.map { group ->
            val confirmed = group.frozenAnchorIds.singleOrNull()?.let { anchor ->
                frozen.single { it.anchorId == anchor }.item.intent.query
            }
            val decision = GuideNameSelector.select(group, evidence, confirmed)
            val selected = group.members.find { it.literal == decision.literal } ?: group.members.first()
            val sources = group.members.map { m ->
                val e = byId.getValue(m.evidenceId)
                if (e.ordinal == null) IntakeSource.Text(e.rawQuote)
                else IntakeSource.Image(e.sourceId, e.ordinal, m.literal,
                    ImageTargetEvidence(e.basis, e.detail, m.role, m.parentLiteral))
            }.distinct()
            val intent = PlaceIntent(decision.literal.replace(Regex("\\s+"), " ").trim(),
                selected.city, byId.getValue(selected.evidenceId).rawQuote, selected.usesDefaultCity,
                sources, decision.needsReview, group.members.map { it.id }.toSet())
            GuideResolvedPlace(intent, group.members, decision, group.relation,
                group.relationEvidenceIds, group.frozenAnchorIds)
        }
        return GuideResolution(places, decoded.pending, decoded.excludedEvidenceIds)
    }
}
