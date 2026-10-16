package com.yangchengwei.easytrip.assistant.cityfirst

import com.yangchengwei.easytrip.assistant.*
import kotlinx.serialization.json.*

/** Private recovery data only; never interpret a saved model-controlled value as a path. */
object GuideEvidenceCheckpointCodec {
    private fun strings(values: Collection<String>) = JsonArray(values.sorted().map(::JsonPrimitive))
    private fun JsonObject.strings(key: String) = getValue(key).jsonArray.map {
        require(it.jsonPrimitive.isString); it.jsonPrimitive.content
    }.toSet()
    private fun JsonObject.number(key: String) = getValue(key).strictGuideInt()
    private fun JsonObject.flag(key: String) = getValue(key).jsonPrimitive.let { require(!it.isString); it.boolean }

    fun encode(v: GuideEvidenceCheckpoint): JsonObject = buildJsonObject {
        put("batch", v.binding.batchId); put("revision", v.binding.revision); put("scope", v.binding.cityScopeKey)
        putJsonArray("sources") { v.sources.forEach { source -> addJsonObject {
            put("id", source.sourceId)
            source.failure?.let { f -> putJsonObject("failure") {
                put("message", f.userMessage); put("code", f.code.name); put("send", f.sendEvidence.name)
            } }
            putJsonArray("evidence") { source.evidence.forEach { e -> addJsonObject {
                put("id", e.id); put("source", e.sourceId); put("ordinal", e.ordinal)
                put("fingerprint", e.sourceFingerprint); put("quote", e.rawQuote); put("kind", e.kind.name)
                put("basis", e.basis.name); put("uncertain", e.uncertain); put("detail", e.detail)
            } } }
        } } }
        putJsonArray("places") { v.resolution.places.forEach { p -> addJsonObject {
            put("intent", GuideResultCodec.intent(p.intent)); put("relation", p.relation.name)
            put("refs", strings(p.relationEvidenceIds)); put("anchors", strings(p.frozenAnchorIds))
            putJsonArray("mentions") { p.mentions.forEach { m -> addJsonObject {
                put("id", m.id); put("evidence", m.evidenceId); put("literal", m.literal)
                put("start", m.start); put("end", m.endExclusive); put("city", m.city); put("default", m.usesDefaultCity)
                put("cityRefs", strings(m.cityEvidenceIds)); put("role", m.role.name); put("parent", m.parentLiteral)
                put("part", m.partKind?.name); put("uncertain", m.uncertain)
            } } }
            putJsonObject("decision") {
                put("literal", p.decision.literal); put("review", p.decision.needsReview); put("reason", p.decision.reason)
                putJsonArray("votes") { p.decision.votes.forEach { vote -> addJsonObject {
                    put("literal", vote.literal); put("target", vote.clearTargetSources); put("context", vote.clearContextSources)
                } } }
            }
        } } }
        putJsonArray("pending") { v.resolution.pending.forEach { p -> addJsonObject {
            put("refs", strings(p.evidenceIds)); put("reason", p.reason)
        } } }
        put("excluded", strings(v.resolution.excludedEvidenceIds))
        putJsonArray("frozen") { v.frozen.forEach { f -> addJsonObject {
            put("anchor", f.anchorId); put("keys", strings(f.materialKeys)); put("committed", f.committed)
            putJsonArray("shapes") { f.shapes.forEach { shape -> addJsonObject {
                put("role", shape.role.name); put("parent", shape.parent); put("part", shape.part?.name)
                put("direction", shape.direction)
            } } }
            // The established v1 item codec validates coordinates, categories and statuses.
            put("item", Json.parseToJsonElement(GuideResultCodec.encode(PlaceAssistantState(
                items = listOf(f.item.copy(label = "A"))))))
            put("label", f.item.label)
        } } }
    }

    fun decode(v: JsonObject): GuideEvidenceCheckpoint {
        val binding = GuideBatchBinding(v.string("batch"), v.getValue("revision").jsonPrimitive.long, v.string("scope"))
        require(binding.batchId.length in 1..128 && binding.revision >= 0 && binding.cityScopeKey.length <= 2000)
        val sources = v.getValue("sources").jsonArray.map { value ->
            val s = value.jsonObject
            val failure = s["failure"]?.jsonObject?.let { f ->
                AssistantFailure(f.string("message"), AssistantFailureCode.valueOf(f.string("code")),
                    SendEvidence.valueOf(f.string("send")))
            }
            val evidence = s.getValue("evidence").jsonArray.map { raw ->
                val e = raw.jsonObject
                GuideEvidence(e.string("id"), e.string("source"), e["ordinal"]?.jsonPrimitive?.intOrNull,
                    e.string("fingerprint"), binding, e.string("quote"), GuideEvidenceKind.valueOf(e.string("kind")),
                    ImageTargetBasis.valueOf(e.string("basis")), e.flag("uncertain"), e.string("detail")).also {
                    require(it.sourceId == s.string("id") && it.id.length in 1..128 && it.rawQuote.length in 1..500)
                    require(it.sourceFingerprint.matches(Regex("[a-f0-9]{64}")) && it.detail.length <= 500)
                    require(it.id == guideEvidenceId(binding, it.sourceId, it.sourceFingerprint, it.kind, it.rawQuote))
                    require(it.ordinal == null && it.sourceId == "text" || it.ordinal in 1..20)
                }
            }
            require(evidence.size <= 100)
            ParsedSource(s.string("id"), emptyList(), failure, evidence)
        }
        require(sources.size <= 21 && sources.map { it.sourceId }.distinct().size == sources.size)
        val evidence = sources.flatMap { it.evidence }
        require(evidence.size <= 400 && evidence.map { it.id }.distinct().size == evidence.size)
        val byId = evidence.associateBy { it.id }
        val frozen = v.getValue("frozen").jsonArray.map { raw ->
            val f = raw.jsonObject
            val itemEnvelope = f.getValue("item").jsonObject
            val itemVersion = itemEnvelope.getValue("version").strictGuideInt()
            require(itemVersion == 1 || itemVersion == 3 && itemEnvelope.getValue("protocolVersion").strictGuideInt() == 1)
            val item = GuideResultCodec.decode(f.getValue("item").toString()).items.single().copy(label = f.string("label"))
            val shapes = f["shapes"]?.jsonArray?.map { rawShape ->
                val shape = rawShape.jsonObject
                GuideEntityShape(ImageEntityRole.valueOf(shape.string("role")), shape.string("parent"),
                    shape["part"]?.jsonPrimitive?.contentOrNull?.let(GuidePartKind::valueOf),
                    shape["direction"]?.jsonPrimitive?.content.orEmpty()).also {
                    require(it.parent.length <= 500)
                    require(it.direction in setOf("", "东", "南", "西", "北", "东北", "东南", "西北", "西南"))
                    require(it.role != ImageEntityRole.PLACE || it.parent.isEmpty() && it.part == null)
                    require(it.role != ImageEntityRole.DISTINCT_PART || it.part != null)
                }
            }?.toSet().orEmpty()
            require(shapes.size <= 100)
            GuideFrozenPlace(f.string("anchor"), item, f.strings("keys"), f.flag("committed"), shapes).also {
                require(it.anchorId.length in 1..128 && it.materialKeys.size <= 400)
            }
        }
        require(frozen.size <= 100 && frozen.map { it.anchorId }.distinct().size == frozen.size)
        val places = v.getValue("places").jsonArray.map { raw ->
            val p = raw.jsonObject
            val mentions = p.getValue("mentions").jsonArray.map { value ->
                val m = value.jsonObject
                GuideMention(m.string("id"), m.string("evidence"), m.string("literal"), m.number("start"),
                    m.number("end"), m.string("city"), m.flag("default"), m.strings("cityRefs"),
                    ImageEntityRole.valueOf(m.string("role")), m.string("parent"),
                    m["part"]?.jsonPrimitive?.contentOrNull?.let(GuidePartKind::valueOf), m.flag("uncertain")).also {
                    val e = byId.getValue(it.evidenceId)
                    require(it.start >= 0 && it.endExclusive <= e.rawQuote.length && it.endExclusive > it.start)
                    require(it.literal.length in 1..100 && e.rawQuote.substring(it.start, it.endExclusive) == it.literal)
                    require(it.city.length <= 50 && it.cityEvidenceIds.all(byId::containsKey))
                    require(it.parentLiteral.isEmpty() || e.rawQuote.contains(it.parentLiteral))
                    require(!e.uncertain || it.uncertain)
                    require(it.id == guideIdentity(e.id, (it.start until it.endExclusive).toString(),
                        it.city, it.role.name, it.parentLiteral))
                }
            }
            require(mentions.isNotEmpty())
            val d = p.getValue("decision").jsonObject
            val decision = GuideNameDecision(d.string("literal"), d.flag("review"), d.string("reason"),
                d.getValue("votes").jsonArray.map { vote ->
                    val x = vote.jsonObject
                    GuideVote(x.string("literal"), x.number("target"), x.number("context")).also {
                        require(it.clearTargetSources in 0..21 && it.clearContextSources in 0..21)
                    }
                })
            val refs = p.strings("refs"); val anchors = p.strings("anchors")
            require(refs.all(byId::containsKey) && anchors.size <= 1 && anchors.all { id -> frozen.any { it.anchorId == id } })
            require(mentions.all { it.evidenceId in refs })
            require(mentions.map { it.city }.distinct().size == 1 && mentions.map { it.role }.distinct().size == 1)
            require(mentions.map { it.parentLiteral }.distinct().size == 1)
            require(mentions.any { it.literal == decision.literal } || anchors.any { id ->
                frozen.any { it.anchorId == id && it.item.intent.query == decision.literal }
            })
            GuideResolvedPlace(GuideResultCodec.intent(p.getValue("intent").jsonObject), mentions, decision,
                GuideAliasKind.valueOf(p.string("relation")), refs, anchors)
        }
        require(places.size <= 100 && places.sumOf { it.mentions.size } <= 400)
        require(places.flatMap { it.mentions }.map { it.id }.distinct().size == places.sumOf { it.mentions.size })
        require(places.flatMap { it.mentions }.groupBy { byId.getValue(it.evidenceId).sourceId }.values.all { it.size <= 100 })
        val pending = v.getValue("pending").jsonArray.map {
            val p = it.jsonObject
            GuidePendingEvidence(p.strings("refs"), p.string("reason")).also { require(it.evidenceIds.all(byId::containsKey)) }
        }
        val excluded = v.strings("excluded")
        require(excluded.all(byId::containsKey))
        return GuideEvidenceCheckpoint(binding, sources, GuideResolution(places, pending, excluded), frozen)
    }
}
