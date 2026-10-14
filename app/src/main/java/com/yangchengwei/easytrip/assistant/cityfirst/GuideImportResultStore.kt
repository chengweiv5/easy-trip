package com.yangchengwei.easytrip.assistant.cityfirst

import com.yangchengwei.easytrip.assistant.*
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    fun encode(s: PlaceAssistantState): String = buildJsonObject {
        put("version", 1); put("input", s.input); put("city", s.city); put("error", s.error)
        put("images", encodeGuideImages(s.images)); put("selected", JsonArray(s.selected.sorted().map(::JsonPrimitive)))
        putJsonArray("items") { s.items.forEach { item -> addJsonObject {
            put("id", item.id); put("label", item.label); put("generation", item.generation); put("status", item.status.name)
            put("category", item.category.name); put("detail", item.detail); put("conflict", item.cityConflict)
            putJsonObject("intent") {
                put("query", item.intent.query); put("city", item.intent.city); put("span", item.intent.sourceSpan)
                put("default", item.intent.usesDefaultCity); put("review", item.intent.needsReview)
                put("sources", sources(item.intent.sources))
            }
            put("sources", sources(item.sources)); put("candidates", JsonArray(item.candidates.map(::poi)))
            put("poi", item.poi?.let(::poi) ?: JsonNull)
        } } }
    }.toString().also { require(it.toByteArray().size <= GUIDE_RECORD_LIMIT) }
    fun decode(text: String): PlaceAssistantState {
        require(text.toByteArray().size <= GUIDE_RECORD_LIMIT)
        val r = Json.parseToJsonElement(text).jsonObject
        require(r.getValue("version").jsonPrimitive.int == 1)
        val items = r.getValue("items").jsonArray.map { j ->
            val i = j.jsonObject; val intent = i.getValue("intent").jsonObject
            IntakeItem(i.string("id"), i.string("label"),
                PlaceIntent(intent.string("query"), intent.string("city"), intent.string("span"),
                    intent.getValue("default").jsonPrimitive.boolean, sources(intent.getValue("sources")),
                    intent.getValue("review").jsonPrimitive.boolean),
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
        return PlaceAssistantState(input = r.string("input"), city = r.string("city"), images = decodeGuideImages(r.getValue("images")),
            items = items, selected = selected, error = r["error"]?.jsonPrimitive?.contentOrNull)
    }
}

class GuideImportResultStore(private val root: File) {
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
