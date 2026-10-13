package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.trip.domain.isTripDateRangeRepresentable
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.*

/** Captured only by final confirmation. No model-generated coordinates or database handles. */
data class TripCreationCommand private constructor(
    val operationId: String,
    val tripId: String,
    val name: String,
    val startDate: LocalDate?,
    val dayCount: Int,
    val travelMode: TravelMode,
    val items: List<ImportItem>,
    val sources: List<String>,
    val digest: String,
) {
    fun validated(): TripCreationCommand {
        val canonical = create(name, startDate, dayCount, travelMode, items, sources, operationId, tripId)
        require(this == canonical) { "创建内容已变化，请重新确认" }
        return this
    }
    fun encode(): String = buildJsonObject {
        put("command", payload(operationId, tripId, name, startDate, dayCount, travelMode, items, sources))
        put("digest", digest)
    }.toString()

    override fun toString() = "TripCreationCommand(content=redacted)"

    companion object {
        const val PREFIX = "create-trip:v1:"
        fun create(
            name: String, startDate: LocalDate?, dayCount: Int, travelMode: TravelMode,
            items: List<ImportItem>, sources: List<String>,
            operationId: String = PREFIX + UUID.randomUUID(),
            tripId: String = UUID.randomUUID().toString(),
        ): TripCreationCommand {
            val title = name.trim()
            require(title.length in 1..100 && title.none(Char::isISOControl)) { "旅行名称须为1–100字" }
            require(dayCount in 1..30 && isTripDateRangeRepresentable(startDate, dayCount)) { "旅行须为1–30天" }
            require(operationId.startsWith(PREFIX) && operationId.length in PREFIX.length + 1..100)
            require(tripId.length in 1..100 && tripId.none(Char::isISOControl))
            require(items.size in 1..AssistantPlacePolicy.MAX_PLACES) { "请至少选择一个有效地点" }
            require(items.map { it.itemId }.distinct().size == items.size)
            require(items.map { it.poi.poiId }.distinct().size == items.size) { "地点不能重复" }
            require(items.all { it.poi.validPosition() && it.itemId.length in 1..100 &&
                it.poi.poiId.length in 1..200 && it.poi.name.length in 1..200 &&
                it.poi.address.length <= 1000 && it.generation >= 0 })
            require(sources.size <= 60 && sources.all { it.length <= 500 }) { "来源摘要过长" }
            val sorted = items.sortedBy { it.itemId }.toList()
            val evidence = sources.distinct().sorted().toList()
            val bytes = payload(operationId, tripId, title, startDate, dayCount, travelMode, sorted, evidence)
                .toString().toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            return TripCreationCommand(operationId, tripId, title, startDate, dayCount, travelMode, sorted, evidence, digest)
        }

        fun decode(value: String): TripCreationCommand {
            require(value.toByteArray(Charsets.UTF_8).size <= 256 * 1024)
            val root = Json.parseToJsonElement(value).jsonObject
            val c = root.getValue("command").jsonObject
            val command = create(c.text("name"), c["date"]?.jsonPrimitive?.contentOrNull?.let(LocalDate::parse),
                c.getValue("days").jsonPrimitive.int, TravelMode.valueOf(c.text("mode")),
                c.getValue("items").jsonArray.map { row ->
                    val i = row.jsonObject
                    val category = PlaceCategory.entries.single { it.storageKey == i.text("category") }
                    ImportItem(i.text("id"), i.getValue("generation").jsonPrimitive.int,
                        PlaceCandidate(i.text("poi"), i.text("name"), i.text("address"),
                            GeoPoint(i.getValue("lat").jsonPrimitive.double, i.getValue("lon").jsonPrimitive.double),
                            i["cityCode"]?.jsonPrimitive?.contentOrNull, i["cityName"]?.jsonPrimitive?.contentOrNull,
                            i["cityAdCode"]?.jsonPrimitive?.contentOrNull, i.getValue("cityVersion").jsonPrimitive.int), category)
                }, c.getValue("sources").jsonArray.map { it.jsonPrimitive.content }, c.text("operation"), c.text("trip"))
            require(command.digest == root.text("digest")) { "恢复记录校验失败" }
            return command
        }

        private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
        private fun payload(operation: String, trip: String, name: String, date: LocalDate?, days: Int,
            mode: TravelMode, items: List<ImportItem>, sources: List<String>) = buildJsonObject {
            put("operation", operation); put("trip", trip); put("name", name)
            put("date", date?.toString()); put("days", days); put("mode", mode.name)
            putJsonArray("items") {
                items.forEach { item -> addJsonObject {
                    val poi = item.poi
                    put("id", item.itemId); put("generation", item.generation); put("poi", poi.poiId)
                    put("name", poi.name); put("address", poi.address)
                    put("lat", requireNotNull(poi.point).latitude); put("lon", poi.point.longitude)
                    put("cityCode", poi.cityCode); put("cityName", poi.cityName); put("cityAdCode", poi.cityAdCode)
                    put("cityVersion", poi.cityMetadataVersion); put("category", item.category.storageKey)
                } }
            }
            putJsonArray("sources") { sources.forEach { add(it) } }
        }
    }
}

sealed interface TripCreationRecovery {
    data object NotSubmitted : TripCreationRecovery
    data class Committed(val receipt: ImportReceipt) : TripCreationRecovery
    data object Deleted : TripCreationRecovery
}

interface TripCreation {
    suspend fun commit(command: TripCreationCommand): ImportReceipt
    suspend fun recover(command: TripCreationCommand): TripCreationRecovery
}
