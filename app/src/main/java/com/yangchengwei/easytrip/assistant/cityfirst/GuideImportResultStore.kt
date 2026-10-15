package com.yangchengwei.easytrip.assistant.cityfirst

import com.yangchengwei.easytrip.assistant.*
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object GuideResultCodec {
    private fun sources(values: List<IntakeSource>) = buildJsonArray { values.forEach { s -> addJsonObject {
        when (s) {
            is IntakeSource.Text -> { put("type", "text"); put("span", s.span) }
            is IntakeSource.Image -> {
                put("type", "image"); put("id", s.imageId); put("ordinal", s.ordinal); put("span", s.excerpt)
                s.target?.let { t -> putJsonObject("target") {
                    put("basis", t.basis.name); put("detail", t.detail); put("role", t.role.name); put("parent", t.parentName)
                } }
            }
        }
    } } }
    private fun sources(value: JsonElement): List<IntakeSource> = value.jsonArray.map {
        val s = it.jsonObject
        when (s.string("type")) {
            "text" -> IntakeSource.Text(s.string("span"))
            "image" -> IntakeSource.Image(s.string("id"), s.getValue("ordinal").jsonPrimitive.int, s.string("span"),
                s["target"]?.jsonObject?.let { t -> ImageTargetEvidence(
                    ImageTargetBasis.valueOf(t.string("basis")), t.string("detail"),
                    ImageEntityRole.valueOf(t.string("role")), t.string("parent")) })
            else -> error("未知来源类型")
        }
    }
    private fun poi(p: PlaceCandidate) = buildJsonObject {
        put("id", p.poiId); put("name", p.name); put("address", p.address)
        put("lat", p.point?.latitude); put("lon", p.point?.longitude)
        put("code", p.cityCode); put("city", p.cityName); put("adcode", p.cityAdCode); put("version", p.cityMetadataVersion)
    }
    private fun poi(j: JsonElement): PlaceCandidate {
        val p = j.jsonObject
        val lat = p["lat"]?.jsonPrimitive?.doubleOrNull
        val lon = p["lon"]?.jsonPrimitive?.doubleOrNull
        return PlaceCandidate(p.string("id"), p.string("name"), p.string("address"),
            if (lat != null && lon != null) GeoPoint(lat, lon) else null,
            p["code"]?.jsonPrimitive?.contentOrNull, p["city"]?.jsonPrimitive?.contentOrNull,
            p["adcode"]?.jsonPrimitive?.contentOrNull, p.getValue("version").jsonPrimitive.int)
    }
    internal fun intent(p: PlaceIntent) = buildJsonObject {
        put("query", p.query); put("city", p.city); put("span", p.sourceSpan)
        put("default", p.usesDefaultCity); put("review", p.needsReview)
        put("sources", sources(p.sources))
        put("materialKeys", JsonArray(p.materialKeys.sorted().map(::JsonPrimitive)))
    }
    internal fun intent(j: JsonObject): PlaceIntent {
        val keys = j["materialKeys"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty()
        require(keys.size <= AssistantMaterialPolicy.MAX_CANDIDATES && keys.all { it.length <= 500 })
        return PlaceIntent(j.string("query"), j.string("city"), j.string("span"),
            j.getValue("default").jsonPrimitive.boolean, sources(j.getValue("sources")),
            j.getValue("review").jsonPrimitive.boolean, keys).also {
            require(it.query.length in 1..100 && it.city.length <= 50 && it.sourceSpan.length <= 20_000)
        }
    }
    fun encode(s: PlaceAssistantState): String = buildJsonObject {
        put("version", if (s.guideVersion == GuideProtocolVersion.EVIDENCE_V2) 2 else 1)
        if (s.guideVersion == GuideProtocolVersion.EVIDENCE_V2)
            put("checkpoint", GuideEvidenceCheckpointCodec.encode(requireNotNull(s.guideCheckpoint)))
        put("input", s.input); put("city", s.city); put("error", s.error)
        put("normalizationPending", s.normalizationPending)
        put("parsedMaterialCity", s.parsedMaterialCity)
        putJsonArray("sourceProgress") { s.sourceProgress.forEach { p -> addJsonObject {
            put("id", p.sourceId); put("stage", p.stage.name); put("error", p.error); put("send", p.sendEvidence.name)
        } } }
        putJsonArray("parsedMaterials") { s.parsedMaterials.forEach { p -> addJsonObject {
            put("id", p.sourceId); put("intents", JsonArray(p.intents.map(::intent)))
            p.failure?.let { f -> putJsonObject("failure") {
                put("message", f.userMessage); put("code", f.code.name); put("send", f.sendEvidence.name)
            } }
        } } }
        put("images", encodeGuideImages(s.images)); put("selected", JsonArray(s.selected.sorted().map(::JsonPrimitive)))
        putJsonArray("items") { s.items.forEach { item -> addJsonObject {
            put("id", item.id); put("label", item.label); put("generation", item.generation); put("status", item.status.name)
            put("category", item.category.name); put("detail", item.detail); put("conflict", item.cityConflict)
            put("intent", intent(item.intent))
            put("sources", sources(item.sources)); put("candidates", JsonArray(item.candidates.map(::poi)))
            put("poi", item.poi?.let(::poi) ?: JsonNull)
        } } }
    }.let { if (s.guideVersion == GuideProtocolVersion.EVIDENCE_V2) GuideSnapshotStrings.pack(it) else it }
        .toString().also { require(it.toByteArray().size <= GUIDE_RECORD_LIMIT) { "恢复记录过大，原记录已保留" } }
    fun decode(text: String): PlaceAssistantState {
        require(text.toByteArray().size <= GUIDE_RECORD_LIMIT)
        val r = GuideSnapshotStrings.unpack(Json.parseToJsonElement(text).jsonObject)
        val version = r.getValue("version").strictGuideInt()
        require(version in 1..2)
        val checkpoint = if (version == 2) GuideEvidenceCheckpointCodec.decode(r.getValue("checkpoint").jsonObject) else null
        val items = r.getValue("items").jsonArray.map { j ->
            val i = j.jsonObject
            IntakeItem(i.string("id"), i.string("label"),
                intent(i.getValue("intent").jsonObject),
                i.getValue("generation").jsonPrimitive.int, IntakeStatus.valueOf(i.string("status")).let {
                    if (it == IntakeStatus.WAITING || it == IntakeStatus.QUERYING) IntakeStatus.CANCELLED else it
                },
                i.getValue("candidates").jsonArray.map(::poi), i["poi"]?.takeUnless { it == JsonNull }?.let(::poi),
                PlaceCategory.valueOf(i.string("category")), i["detail"]?.jsonPrimitive?.contentOrNull,
                i.getValue("conflict").jsonPrimitive.boolean, sources(i.getValue("sources")))
        }
        require(items.size <= AssistantPlacePolicy.MAX_PLACES && items.map { it.id }.distinct().size == items.size)
        require(r.string("input").length <= 20_000 && r.string("city").length <= 50)
        items.forEachIndexed { index, item ->
            require(item.id.isNotBlank() && item.label == AssistantPlacePolicy.label(index) && item.generation >= 0)
            require(item.intent.query.length in 1..100 && item.intent.city.length <= 50)
            require(item.candidates.size <= 100)
            require(item.status != IntakeStatus.READY || item.poi?.validPosition() == true)
        }
        val selected = r.getValue("selected").jsonArray.map { it.jsonPrimitive.content }.toSet()
        require(selected.all { id -> items.any { it.id == id } })
        val images = decodeGuideImages(r.getValue("images"))
        checkpoint?.sources?.flatMap { it.evidence }?.forEach { evidence ->
            require(if (evidence.sourceId == "text") evidence.ordinal == null
                else images.singleOrNull { it.id == evidence.sourceId }?.ordinal == evidence.ordinal &&
                    evidence.ordinal != null)
        }
        val validSources = images.map { it.id }.toSet() + "text"
        val progress = r["sourceProgress"]?.jsonArray?.map { value ->
            val p = value.jsonObject
            SourceProgress(p.string("id"), SourceStage.valueOf(p.string("stage")).let {
                if (it in setOf(SourceStage.SELECTED, SourceStage.PREPARING, SourceStage.SENDING, SourceStage.UNDERSTANDING))
                    SourceStage.CANCELLED else it
            }, p["error"]?.jsonPrimitive?.contentOrNull, SendEvidence.valueOf(p.string("send")))
        }.orEmpty()
        val parsed = checkpoint?.sources ?: r["parsedMaterials"]?.jsonArray?.map { value ->
            val p = value.jsonObject
            val failure = p["failure"]?.jsonObject?.let {
                AssistantFailure(it.string("message"), AssistantFailureCode.valueOf(it.string("code")),
                    SendEvidence.valueOf(it.string("send")))
            }
            ParsedSource(p.string("id"), p.getValue("intents").jsonArray.map { intent(it.jsonObject) }, failure)
        }.orEmpty()
        require(progress.size <= 21 && progress.map { it.sourceId }.distinct().size == progress.size)
        require(parsed.size <= 21 && parsed.map { it.sourceId }.distinct().size == parsed.size)
        require(progress.all { it.sourceId in validSources } && parsed.all { it.sourceId in validSources })
        require(parsed.all { it.intents.size <= AssistantMaterialPolicy.MAX_PER_SOURCE })
        val parsedCity = r["parsedMaterialCity"]?.jsonPrimitive?.contentOrNull
        require(parsedCity == null || parsedCity.length <= 50 && parsedCity.none(Char::isISOControl))
        return PlaceAssistantState(input = r.string("input"), city = r.string("city"), images = images,
            items = items, selected = selected, error = r["error"]?.jsonPrimitive?.contentOrNull,
            sourceProgress = progress, parsedMaterials = parsed,
            normalizationPending = r["normalizationPending"]?.jsonPrimitive?.booleanOrNull ?: false,
            parsedMaterialCity = parsedCity,
            guideVersion = if (version == 2) GuideProtocolVersion.EVIDENCE_V2 else GuideProtocolVersion.LEGACY_V1,
            guideCheckpoint = checkpoint)
    }
}

class GuideImportResultStore(private val root: File) {
    private val localLock = Mutex()
    private fun localFile(tripId: String): File {
        require(validGuideId(tripId)); return File(root, "local-$tripId.json")
    }
    private fun decodeLocal(file: File): GuideLocalDraftRef? {
        if (!file.exists() && !File(file.path + ".bak").exists()) return null
        val r = Json.parseToJsonElement(readGuideFile(file)).jsonObject
        require(r.getValue("version").jsonPrimitive.int == 1)
        val value = GuideLocalDraftRef(r.string("trip"), r.string("batch"), r.getValue("revision").jsonPrimitive.long,
            GuideMaterialRef(r.string("material"), r.string("digest")), r.string("result"))
        require(file.name == "local-${value.tripId}.json")
        return value
    }
    suspend fun localDraft(tripId: String): GuideLocalDraftRef? = withContext(Dispatchers.IO) {
        localLock.withLock { decodeLocal(localFile(tripId)) }
    }
    suspend fun localMaterialIds(): List<String> = withContext(Dispatchers.IO) {
        localLock.withLock {
            root.listFiles().orEmpty().filter { it.name.startsWith("local-") && it.name.endsWith(".json") }
                .mapNotNull { decodeLocal(it)?.material?.id }
        }
    }
    suspend fun replaceLocalDraft(tripId: String, expected: GuideLocalDraftRef?,
        value: GuideLocalDraftRef?): Boolean = withContext(Dispatchers.IO) {
        localLock.withLock {
            val file = localFile(tripId)
            require(value == null || value.tripId == tripId)
            if (decodeLocal(file) != expected) return@withLock false
            if (value == null) android.util.AtomicFile(file).delete()
            else {
                read(value.resultRef) // Never publish a pointer to an unreadable result.
                writeGuideFile(file, buildJsonObject {
                    put("version", 1); put("trip", tripId); put("batch", value.batchId); put("revision", value.revision)
                    put("material", value.material.id); put("digest", value.material.digest); put("result", value.resultRef)
                }.toString())
            }
            true
        }
    }
    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        require(validGuideId(id))
        android.util.AtomicFile(File(root, "$id.json")).delete()
    }
    suspend fun write(state: PlaceAssistantState): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        writeGuideFile(File(root, "$id.json"), GuideResultCodec.encode(state))
        id
    }
    suspend fun read(id: String): PlaceAssistantState = withContext(Dispatchers.IO) {
        require(validGuideId(id))
        GuideResultCodec.decode(readGuideFile(File(root, "$id.json")))
    }
}
