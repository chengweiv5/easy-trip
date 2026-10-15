package com.yangchengwei.easytrip.assistant

/** Chooses only among validated original spellings; input order is never a vote. */
object GuideNameSelector {
    fun select(group: GuideAliasGroup, evidence: List<GuideEvidence>, confirmed: String? = null): GuideNameDecision {
        require(group.members.isNotEmpty())
        val byId = evidence.associateBy { it.id }
        val votes = group.members.groupBy { it.literal }.map { (literal, mentions) ->
            val supports = mentions.mapNotNull { mention ->
                val e = byId.getValue(mention.evidenceId)
                if (e.uncertain || mention.uncertain) null
                else (if (e.ordinal == null) "TEXT" else e.sourceFingerprint) to
                    (e.basis != ImageTargetBasis.CONTEXT_REFERENCE)
            }.groupBy({ it.first }, { it.second }).values.map { it.any { v -> v } }
            GuideVote(literal, supports.count { it }, supports.count { !it })
        }.sortedWith(compareByDescending<GuideVote> { it.clearTargetSources }
            .thenByDescending { it.clearContextSources }.thenBy { it.literal })
        if (!confirmed.isNullOrBlank()) return GuideNameDecision(confirmed, false, "保留用户确认名称", votes)
        val first = votes.first()
        val tied = votes.filter { it.clearTargetSources == first.clearTargetSources &&
            it.clearContextSources == first.clearContextSources }
        val clear = first.clearTargetSources + first.clearContextSources > 0
        if (clear && tied.size > 1 && group.relation == GuideAliasKind.CONTEXT_ALIAS) {
            val suffixes = listOf("风景区", "景区", "公园")
            fun stem(s: String) = suffixes.firstOrNull { s.endsWith(it) }?.let { s.removeSuffix(it) } ?: s
            val stems = tied.map { stem(it.literal) }.distinct()
            val complete = tied.filter { stem(it.literal) != it.literal }
            if (stems.size == 1 && stems.single().isNotBlank() && complete.size == 1)
                return GuideNameDecision(complete.single().literal, false, "同专名后缀别名保留原文全称", votes)
        }
        return GuideNameDecision(first.literal, !clear || tied.size > 1,
            if (clear && tied.size == 1) "按不同来源清晰原字支持选择" else "原字支持不足或存在冲突，请核对名称", votes)
    }
}
