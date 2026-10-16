package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*

object GuideEntityProtocol {
    fun request(config: ProviderConfig, evidence: List<GuideEvidence>, context: GuideContext,
        frozen: List<GuideFrozenPlace>): String = guideEntityRequest(config, evidence, context, frozen)

    fun decode(response: String, evidence: List<GuideEvidence>, context: GuideContext,
        frozen: List<GuideFrozenPlace>, requireClassification: Boolean = false): GuideEntityDecode {
        val args = try {
            AssistantToolResponse.arguments(response, "resolve_guide_entities").also {
                it.getValue("evidenceResults").jsonArray
                it.getValue("groups").jsonArray
            }
        } catch (e: Exception) {
            throw protocolFailure(e, AssistantProtocolStage.NORMALIZATION, AssistantProtocolIssue.INVALID_FIELDS)
        }
        val pending = linkedSetOf<String>()
        val excluded = linkedSetOf<String>()
        val rawCounts = args.getValue("evidenceResults").jsonArray.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            val i = (o["e"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: return@mapNotNull null
            val source = evidence.getOrNull(i)?.sourceId ?: return@mapNotNull null
            source to ((o["mentions"] as? JsonArray)?.size ?: 0)
        }
        if (rawCounts.sumOf { it.second } > AssistantMaterialPolicy.MAX_CANDIDATES ||
            rawCounts.groupBy({ it.first }, { it.second }).values.any { it.sum() > AssistantMaterialPolicy.MAX_PER_SOURCE })
            throw AssistantFailure("地点出现项超过本批预算，请分批；原证据保留，没有截断。",
                AssistantFailureCode.OVERFLOW, SendEvidence.RESPONSE_RECEIVED)
        val mentions = linkedMapOf<Pair<Int, Int>, GuideMention>()
        val entries = args.getValue("evidenceResults").jsonArray.mapNotNull {
            val o = it as? JsonObject ?: return@mapNotNull null
            (o["e"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull?.let { index -> index to o }
        }.groupBy({ it.first }, { it.second })
        val invalidEvidence = linkedSetOf<String>()
        val invalidOccurrences = linkedSetOf<Pair<Int, Int>>()
        evidence.forEachIndexed { index, e ->
            try {
                val entry = entries[index].orEmpty().single()
                when (entry.text("status")) {
                    "REVIEW" -> { pending += e.id; return@forEachIndexed }
                    "EXCLUDED" -> {
                        val reason = entry.text("reason")
                        require(reason in setOf("FOOD_OR_DINING", "BACKGROUND", "NOT_PLACE"))
                        if (requireClassification) {
                            require(classify(entry, e.rawQuote) == GuidePlaceClassification.Decision.DROP)
                        } else require(reason == "FOOD_OR_DINING" || e.kind != GuideEvidenceKind.AUTHOR_LABEL &&
                            e.basis == ImageTargetBasis.CONTEXT_REFERENCE)
                        require(entry["mentions"]?.jsonArray.isNullOrEmpty())
                        excluded += e.id
                        return@forEachIndexed
                    }
                    "PARSED" -> Unit
                    else -> error("unknown receipt")
                }
                val list = entry.getValue("mentions").jsonArray
                require(list.isNotEmpty())
                list.forEachIndexed { m, value ->
                    try {
                        val o = value.jsonObject
                        val literal = o.text("literal")
                        require(literal.length in 1..100)
                        val range = requireNotNull(GuideEntityValidation.locate(e.rawQuote, literal,
                            o.getValue("occurrence").strictGuideInt()))
                        val role = ImageEntityRole.valueOf(o.text("role"))
                        val parent = o.text("parentLiteral")
                        require(parent.isEmpty() || e.rawQuote.contains(parent))
                        val requestedCity = o.text("city")
                        require(requestedCity.length <= 50 && requestedCity.none(Char::isISOControl))
                        val cityRefs = o.getValue("cityEvidenceRefs").jsonArray.map {
                            evidence[it.strictGuideInt()]
                        }
                        val defaultCity = if (context.scope != null) context.scope.defaultCity.orEmpty() else context.city
                        require(requestedCity.isEmpty() || requestedCity == defaultCity ||
                            cityRefs.any { it.rawQuote.contains(requestedCity) })
                        require(cityRefs.isEmpty() || requestedCity.isNotEmpty() &&
                            cityRefs.all { it.rawQuote.contains(requestedCity) })
                        val city = requestedCity.ifEmpty { defaultCity }
                        val part = o["partKind"]?.takeUnless { it == JsonNull }?.jsonPrimitive?.content?.let(GuidePartKind::valueOf)
                        require(role != ImageEntityRole.PLACE || parent.isEmpty() && part == null)
                        require(role != ImageEntityRole.DISTINCT_PART || part != null)
                        val uncertainty = o.getValue("uncertain").jsonPrimitive
                        require(!uncertainty.isString && uncertainty.booleanOrNull != null)
                        mentions[index to m] = GuideMention(guideIdentity(e.id, range.toString(), city, role.name, parent),
                            e.id, e.rawQuote.substring(range), range.first, range.last + 1, city,
                            requestedCity.isEmpty() && city.isNotEmpty(), cityRefs.map { it.id }.toSet(), role, parent,
                            part, e.uncertain || uncertainty.boolean)
                    } catch (failure: Exception) {
                        // Keep original [e, mentionIndex] identities. One malformed
                        // occurrence must not prevent later independent destinations
                        // in the same route excerpt from being validated.
                        if (!requireClassification) throw failure
                        pending += e.id
                        invalidOccurrences += index to m
                    }
                }
                if (e.kind == GuideEvidenceKind.AUTHOR_LABEL) {
                    GuideEntityValidation.completeBracketParts(e.rawQuote)?.let { (outer, inner) ->
                        val literals = list.map { it.jsonObject.text("literal") }
                        require(outer in literals && inner in literals && e.rawQuote !in literals)
                    }
                }
            } catch (_: Exception) { pending += e.id; invalidEvidence += e.id }
        }
        // Classification is transient: retain every original quote and index for
        // recovery, but never send an excluded/unknown occurrence to POI search.
        val decisions = mentions.mapValues { (key, mention) ->
            if (!requireClassification) GuidePlaceClassification.Decision.KEEP
            else classify(entries.getValue(key.first).single().getValue("mentions")
                .jsonArray[key.second].jsonObject, evidence[key.first].rawQuote, mention.literal)
        }
        decisions.filterValues { it == GuidePlaceClassification.Decision.REVIEW }.keys.forEach {
            pending += mentions.getValue(it).evidenceId
        }
        val eligibleIds = decisions.filterValues { it == GuidePlaceClassification.Decision.KEEP }
            .keys.map { mentions.getValue(it).id }.toSet()
        val invalidMentions = linkedSetOf<String>()
        val groups = args.getValue("groups").jsonArray.mapNotNull { value ->
            val related = linkedSetOf<String>()
            val relatedMentions = linkedSetOf<String>()
            // Collect the whole group's valid references before validating any one member.
            // An invalid first pair must not hide later evidence from local isolation.
            ((value as? JsonObject)?.get("members") as? JsonArray)?.forEach { raw ->
                (raw as? JsonArray)?.takeIf { it.size == 2 }?.let { pair ->
                    val eIndex = (pair[0] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                    val mIndex = (pair[1] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                    if (eIndex != null && mIndex != null) mentions[eIndex to mIndex]
                        ?.takeIf { it.id in eligibleIds }?.let { relatedMentions += it.id }
                }
                ((raw as? JsonArray)?.firstOrNull() as? JsonPrimitive)
                    ?.takeUnless { it.isString }?.intOrNull?.let { index ->
                        evidence.getOrNull(index)?.let { related += it.id }
                    }
            }
            try {
                val o = value.jsonObject
                val members = o.getValue("members").jsonArray.mapNotNull {
                    val pair = it.jsonArray
                    require(pair.size == 2)
                    val eIndex = pair[0].strictGuideInt()
                    evidence.getOrNull(eIndex)?.let { e -> related += e.id }
                    val key = eIndex to pair[1].strictGuideInt()
                    // A known malformed occurrence is isolated, not an invalid
                    // identity for every independently validated group member.
                    // Unknown indices still invalidate the group below.
                    if (requireClassification && key in invalidOccurrences) return@mapNotNull null
                    requireNotNull(mentions[key])
                        .takeIf { it.id in eligibleIds }
                }
                if (members.isEmpty()) return@mapNotNull null
                require(members.none { it.evidenceId in invalidEvidence })
                require(members.map { it.id }.distinct().size == members.size)
                val relation = GuideAliasKind.valueOf(o.text("relation"))
                val refs = o.getValue("evidenceRefs").jsonArray.map { evidence[it.strictGuideInt()] }
                require(refs.isNotEmpty() && members.all { m -> refs.any { it.id == m.evidenceId } })
                require(members.map { it.city }.distinct().size == 1)
                require(members.map { it.role }.distinct().size == 1)
                if (members.first().role == ImageEntityRole.DISTINCT_PART) {
                    require(members.map { it.parentLiteral }.distinct().size == 1)
                    require(members.size == 1 || members.first().parentLiteral.isNotEmpty())
                    require(members.map { it.partKind }.distinct().size == 1)
                    val directions = members.mapNotNull {
                        guideEntranceDirection(it.literal).takeIf(String::isNotEmpty)
                    }
                    require(directions.distinct().size <= 1)
                }
                if (relation == GuideAliasKind.SAME_LITERAL) require(members.map { it.literal }.distinct().size == 1)
                val anchors = o.getValue("frozenAnchors").jsonArray.map {
                    require(it.jsonPrimitive.isString); it.jsonPrimitive.content
                }.toSet()
                require(anchors.size <= 1 && anchors.all { id -> frozen.any { it.anchorId == id } })
                anchors.forEach { id ->
                    val anchor = frozen.single { it.anchorId == id }
                    require(anchor.accepts(members))
                    val old = anchor.item.intent
                    require(members.all { it.city == old.city })
                    val oldTargets = old.sources.filterIsInstance<IntakeSource.Image>().mapNotNull { it.target }
                    val oldParts = oldTargets.filter { it.role == ImageEntityRole.DISTINCT_PART }
                    if (oldParts.isNotEmpty()) {
                        require(members.all { m -> m.role == ImageEntityRole.DISTINCT_PART &&
                            m.parentLiteral.isNotEmpty() && oldParts.all { it.parentName == m.parentLiteral } })
                    } else if (oldTargets.isNotEmpty()) require(members.all { it.role == ImageEntityRole.PLACE })
                }
                GuideAliasGroup(members, relation, refs.map { it.id }.toSet(), anchors)
            } catch (_: Exception) { pending += related; invalidMentions += relatedMentions; null }
        }
        val counts = groups.flatMap { it.members }.groupingBy { it.id }.eachCount()
        mentions.values.filter { it.id in eligibleIds && counts[it.id] != 1 }.forEach {
            pending += it.evidenceId; invalidMentions += it.id
        }
        if (requireClassification) evidence.forEachIndexed { index, e ->
            val values = decisions.filterKeys { it.first == index }.values
            if (values.isNotEmpty() && values.all { it == GuidePlaceClassification.Decision.DROP })
                excluded += e.id
        }
        // A bad mention/group must not taint independent mentions in the same route excerpt.
        val accepted = groups.filter { g ->
            val valid = g.members.none { it.evidenceId in invalidEvidence || it.id in invalidMentions }
            if (!valid) pending += g.members.map { it.evidenceId }
            valid
        }
        val represented = accepted.flatMap { it.members }.map { it.evidenceId }.toSet()
        pending += evidence.map { it.id }.filterNot { it in represented || it in excluded }
        return GuideEntityDecode(accepted,
            pending.map { GuidePendingEvidence(setOf(it), "部分原文尚未整理完成，请重试整理。") }, excluded - pending)
    }

    private fun classify(item: JsonObject, quote: String, literal: String = ""): GuidePlaceClassification.Decision =
        try {
            val citation = GuidePlaceClassification.quote(item)
            if (citation.isBlank() || GuidePlaceClassification.originalQuote(quote, citation) == null ||
                literal.isNotEmpty() && !citation.contains(literal)) GuidePlaceClassification.Decision.REVIEW
            else GuidePlaceClassification.decide(buildJsonObject {
                item.forEach { (key, value) -> put(key, value) }
                put("query", literal)
            }, listOf(quote))
        } catch (_: Exception) { GuidePlaceClassification.Decision.REVIEW }
}

internal fun JsonElement.strictGuideInt(): Int = jsonPrimitive.let {
    require(!it.isString); requireNotNull(it.intOrNull)
}
