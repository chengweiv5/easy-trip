package com.yangchengwei.easytrip.assistant

/** Pure batch state. Frozen user decisions are never replaced by automatic resolution. */
class GuideEvidenceSession(initial: GuideEvidenceCheckpoint) {
    var checkpoint = initial
        private set

    fun accept(source: ParsedSource) {
        require(source.intents.isEmpty() && source.evidence.all { it.binding == checkpoint.binding })
        val old = checkpoint.sources.find { it.sourceId == source.sourceId }
        if (old != null && old.failure == null && source.failure != null) return
        val sources = checkpoint.sources.filterNot { it.sourceId == source.sourceId } + source
        if (sources.sumOf { it.evidence.size } > AssistantMaterialPolicy.MAX_CANDIDATES)
            throw AssistantFailure("本批超过400份原文证据，请分批；已有结果保留。", AssistantFailureCode.OVERFLOW)
        checkpoint = checkpoint.copy(sources = sources)
    }

    fun freeze(item: IntakeItem, committed: Boolean) {
        val old = checkpoint.frozen.find { it.item.id == item.id }
        val shapes = checkpoint.resolution.places.filter {
            it.intent.materialIdentities().intersect(item.intent.materialIdentities()).isNotEmpty()
        }.flatMap { it.mentions }.map { it.entityShape() }.toSet()
        val value = GuideFrozenPlace(old?.anchorId ?: guideIdentity("guide-frozen", checkpoint.binding.batchId, item.id),
            item, old?.materialKeys.orEmpty() + item.intent.materialIdentities(), committed || old?.committed == true,
            old?.shapes?.takeIf { it.isNotEmpty() } ?: shapes)
        checkpoint = checkpoint.copy(frozen = checkpoint.frozen.filterNot { it.item.id == item.id } + value)
    }

    fun apply(resolution: GuideResolution, liveItems: List<IntakeItem>): GuideApplyResult {
        val additions = mutableListOf<PlaceIntent>()
        val absorbed = linkedMapOf<String, Set<String>>()
        val pending = resolution.pending.toMutableList()
        val accepted = mutableListOf<GuideResolvedPlace>()
        for (place in resolution.places) {
            val keys = place.intent.materialIdentities()
            val anchors = checkpoint.frozen.filter {
                it.anchorId in place.frozenAnchorIds || it.materialKeys.intersect(keys).isNotEmpty()
            }
            if (anchors.size > 1 || place.frozenAnchorIds.any { id -> anchors.none { it.anchorId == id } }) {
                pending += GuidePendingEvidence(place.mentions.map { it.evidenceId }.toSet(), "已有地点身份存在冲突，请核对。")
                continue
            }
            val anchor = anchors.singleOrNull()
            if (anchor != null && !anchor.accepts(place.mentions)) {
                pending += GuidePendingEvidence(place.mentions.map { it.evidenceId }.toSet(), "已有地点所属关系存在冲突，请核对。")
                continue
            }
            val existing = anchor?.item ?: liveItems.find { it.intent.materialIdentities().intersect(keys).isNotEmpty() }
            if (existing != null && existing.intent.city.isNotBlank() && place.intent.city.isNotBlank() &&
                !sameCity(existing.intent.city, place.intent.city)) {
                pending += GuidePendingEvidence(place.mentions.map { it.evidenceId }.toSet(), "已有地点城市存在冲突，请核对。")
                continue
            }
            if (existing == null) additions += place.intent
            else absorbed[existing.id] = existing.intent.materialIdentities() + anchor?.materialKeys.orEmpty() +
                absorbed[existing.id].orEmpty() + keys
            accepted += place
        }
        val represented = (liveItems.map { it.id } + checkpoint.frozen.map { it.item.id }).distinct().size
        if (represented + additions.size > AssistantPlacePolicy.MAX_PLACES)
            throw AssistantFailure("本批唯一地点超过100个，请分批；已有结果保留，未截断新增地点。",
                AssistantFailureCode.OVERFLOW)
        checkpoint = checkpoint.copy(
            resolution = GuideResolution(accepted, pending, resolution.excludedEvidenceIds),
            frozen = checkpoint.frozen.map { old ->
                absorbed[old.item.id]?.let { keys -> old.copy(materialKeys = keys) } ?: old
            })
        return GuideApplyResult(additions, absorbed)
    }
}
