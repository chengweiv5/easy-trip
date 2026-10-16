package com.yangchengwei.easytrip.assistant.cityfirst

import com.yangchengwei.easytrip.assistant.AssistantCityScope
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.isTripDateRangeRepresentable
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.*

internal fun guideDigest(bytes: ByteArray) =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
internal fun validGuideId(id: String) = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
internal fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.let { require(it.isString); it.content }
const val GUIDE_RECORD_LIMIT = 1024 * 1024
data class GuideMaterialRef(val id: String, val digest: String) {
    init { require(validGuideId(id) && digest.matches(Regex("[a-f0-9]{64}"))) }
}
data class GuideLocalDraftRef(val tripId: String, val batchId: String, val revision: Long,
    val material: GuideMaterialRef, val resultRef: String) {
    init { require(validGuideId(tripId) && validGuideId(batchId) && revision >= 0 && validGuideId(resultRef)) }
}
data class GuideCityEvidence(val name: String, val sourceId: String, val sourceSpan: String)
data class GuideDestinationResult(val cities: List<GuideCityEvidence>, val suggestedName: String,
    val material: GuideMaterials? = null)
enum class GuideHandoffStatus { PENDING, CLAIMED, IMPORTED, FAILED, CANCELLED, FINISHED }

@ConsistentCopyVisibility
data class CreateGuideTripCommand private constructor(
    val operationId: String, val tripId: String, val name: String, val startDate: LocalDate?,
    val dayCount: Int, val travelMode: TravelMode, val scope: AssistantCityScope,
    val material: GuideMaterialRef, val digest: String,
) {
    private fun payload() = payload(operationId, tripId, name, startDate, dayCount, travelMode, scope, material)
    fun encode() = buildJsonObject { put("version", 1); put("command", payload()); put("digest", digest) }.toString()
    fun validated(): CreateGuideTripCommand {
        require(this == create(name, startDate, dayCount, travelMode, scope, material, operationId, tripId))
        return this
    }
    companion object {
        fun create(name: String, startDate: LocalDate?, dayCount: Int, travelMode: TravelMode,
            scope: AssistantCityScope, material: GuideMaterialRef,
            operationId: String = UUID.randomUUID().toString(), tripId: String = UUID.randomUUID().toString()): CreateGuideTripCommand {
            val title = name.trim()
            require(title.length in 1..100 && title.none(Char::isISOControl))
            require(validGuideId(operationId) && validGuideId(tripId))
            require(dayCount in 1..30 && isTripDateRangeRepresentable(startDate, dayCount))
            val payload = payload(operationId, tripId, title, startDate, dayCount, travelMode, scope, material)
            return CreateGuideTripCommand(operationId, tripId, title, startDate, dayCount, travelMode, scope, material,
                guideDigest(payload.toString().toByteArray()))
        }
        fun decode(text: String): CreateGuideTripCommand {
            require(text.toByteArray().size <= GUIDE_RECORD_LIMIT)
            val r = Json.parseToJsonElement(text).jsonObject
            require(r.keys == setOf("version", "command", "digest") && r.getValue("version").jsonPrimitive.int == 1)
            val p = r.getValue("command").jsonObject
            require(p.keys == setOf("operation", "trip", "name", "date", "days", "mode", "cities", "material", "materialDigest"))
            val c = create(p.string("name"), p["date"]?.jsonPrimitive?.contentOrNull?.let(LocalDate::parse),
                p.getValue("days").jsonPrimitive.int, TravelMode.valueOf(p.string("mode")),
                AssistantCityScope.decode(p.getValue("cities").toString()), GuideMaterialRef(p.string("material"), p.string("materialDigest")),
                p.string("operation"), p.string("trip"))
            require(c.digest == r.string("digest"))
            return c
        }
        private fun payload(operation: String, trip: String, name: String, date: LocalDate?, days: Int,
            mode: TravelMode, scope: AssistantCityScope, material: GuideMaterialRef) = buildJsonObject {
            put("operation", operation); put("trip", trip); put("name", name); put("date", date?.toString())
            put("days", days); put("mode", mode.name); put("cities", Json.parseToJsonElement(scope.encode()))
            put("material", material.id); put("materialDigest", material.digest)
        }
    }
}
data class GuideCreationReceipt(val operationId: String, val tripId: String, val digest: String)
sealed interface GuideCreationRecovery {
    data object NotSubmitted : GuideCreationRecovery
    data class Committed(val receipt: GuideCreationReceipt) : GuideCreationRecovery
    data object Deleted : GuideCreationRecovery
}
interface GuideTripCreation {
    suspend fun commit(command: CreateGuideTripCommand): GuideCreationReceipt
    suspend fun recover(command: CreateGuideTripCommand): GuideCreationRecovery
}
data class GuideHandoff(
    val operationId: String, val tripId: String, val digest: String, val material: GuideMaterialRef,
    val scope: AssistantCityScope, val status: GuideHandoffStatus, val attempt: Int = 0, val resultRef: String? = null,
)
interface GuideHandoffStore {
    suspend fun find(tripId: String): GuideHandoff?
    suspend fun scope(tripId: String): AssistantCityScope?
    suspend fun claim(id: String, digest: String, attempt: Int): GuideHandoff?
    suspend fun requestRetry(id: String, attempt: Int): GuideHandoff?
    suspend fun complete(id: String, attempt: Int, status: GuideHandoffStatus, resultRef: String?): Boolean
}
