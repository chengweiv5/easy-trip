package com.yangchengwei.easytrip.assistant

import java.util.UUID
import kotlinx.serialization.json.*

enum class OptionalImageStatus { NOT_ATTEMPTED, AVAILABLE, SKIPPED }
enum class OptionalImageSkip { DOWNLOAD, DECODE, LIMIT, TIMEOUT, MODEL, UNSUPPORTED, MISSING }

data class OptionalImageAsset(
    val id: String, val slot: Int, val originalOrdinal: Int?, val remoteUrl: String?,
    val status: OptionalImageStatus, val descriptor: AssistantImage? = null,
    val preparedSha256: String? = null, val skip: OptionalImageSkip? = null,
) {
    init {
        require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false))
        require(slot in 1..AssistantImagePolicy.MAX_IMAGES && (originalOrdinal == null || originalOrdinal in 1..100_000))
        require(remoteUrl == null || remoteUrl.length in 1..8192)
        require(descriptor == null || descriptor.id == id && descriptor.ordinal == slot &&
            descriptor.mimeType in setOf("image/jpeg", "image/png", "image/webp") &&
            descriptor.byteCount in 1..AssistantImagePolicy.MAX_SINGLE_BYTES && descriptor.width > 0 && descriptor.height > 0)
        descriptor?.let { AssistantImagePolicy.targetSize(it.width, it.height) }
        require(preparedSha256 == null || Regex("[a-f0-9]{64}").matches(preparedSha256))
        when (status) {
            OptionalImageStatus.AVAILABLE -> require(descriptor != null && preparedSha256 != null && skip == null)
            OptionalImageStatus.NOT_ATTEMPTED -> require(preparedSha256 == null && skip == null)
            OptionalImageStatus.SKIPPED -> require(skip != null)
        }
    }
    fun skipped(reason: OptionalImageSkip) = copy(status = OptionalImageStatus.SKIPPED, skip = reason)
}

data class OptionalImagePlan(
    val assets: List<OptionalImageAsset> = emptyList(), val expectedCount: Int? = null,
    val coverageKnown: Boolean = false, val omittedKnownCount: Int = 0, val hasUnknownOmissions: Boolean = false,
) {
    init {
        require(assets.size <= AssistantImagePolicy.MAX_IMAGES)
        require(assets.map { it.id }.distinct().size == assets.size && assets.map { it.slot }.distinct().size == assets.size)
        require(expectedCount == null || expectedCount in 0..100_000)
        require(omittedKnownCount in 0..100_000)
        require(!coverageKnown || expectedCount != null && !hasUnknownOmissions)
        require(assets.mapNotNull { it.descriptor }.sumOf { it.byteCount } <= AssistantImagePolicy.MAX_TOTAL_BYTES)
    }
    val available get() = assets.filter { it.status == OptionalImageStatus.AVAILABLE }.mapNotNull { it.descriptor }
    val descriptors get() = assets.mapNotNull { it.descriptor }
    fun updated(asset: OptionalImageAsset) = copy(assets = assets.map { if (it.id == asset.id) asset else it })
    companion object {
        fun local(images: List<AssistantImage>) = OptionalImagePlan(images.map {
            OptionalImageAsset(it.id, it.ordinal, null, null, OptionalImageStatus.NOT_ATTEMPTED, it)
        })
    }
}

object OptionalImagePlanCodec {
    fun encode(plan: OptionalImagePlan): JsonObject = buildJsonObject {
        put("version", 1); put("expected", plan.expectedCount?.let(::JsonPrimitive) ?: JsonNull)
        put("coverageKnown", plan.coverageKnown); put("omitted", plan.omittedKnownCount)
        put("unknownOmissions", plan.hasUnknownOmissions)
        putJsonArray("assets") { plan.assets.forEach { a -> addJsonObject {
            put("id", a.id); put("slot", a.slot); put("originalOrdinal", a.originalOrdinal?.let(::JsonPrimitive) ?: JsonNull)
            put("url", a.remoteUrl?.let(::JsonPrimitive) ?: JsonNull); put("status", a.status.name)
            put("sha256", a.preparedSha256?.let(::JsonPrimitive) ?: JsonNull)
            put("skip", a.skip?.name?.let(::JsonPrimitive) ?: JsonNull)
            put("descriptor", a.descriptor?.let { d -> buildJsonObject {
                put("mime", d.mimeType); put("bytes", d.byteCount); put("width", d.width); put("height", d.height)
            } } ?: JsonNull)
        } } }
    }
    fun decode(value: JsonObject, platform: AssistantLinkPlatform?): OptionalImagePlan {
        require(value.getValue("version").jsonPrimitive.int == 1)
        val assets = value.getValue("assets").jsonArray.map { entry ->
            val a = entry.jsonObject
            fun string(key: String) = a.getValue(key).jsonPrimitive.content
            fun nullable(key: String) = a.getValue(key).takeUnless { it is JsonNull }
            val id = string("id"); val slot = a.getValue("slot").jsonPrimitive.int
            val url = nullable("url")?.jsonPrimitive?.content
            if (url != null) AssistantLinkPolicy.image(requireNotNull(platform), url)
            val descriptor = nullable("descriptor")?.jsonObject?.let { d ->
                AssistantImage(id, slot, d.getValue("mime").jsonPrimitive.content,
                    d.getValue("bytes").jsonPrimitive.long, d.getValue("width").jsonPrimitive.int,
                    d.getValue("height").jsonPrimitive.int)
            }
            OptionalImageAsset(id, slot, nullable("originalOrdinal")?.jsonPrimitive?.int, url,
                OptionalImageStatus.valueOf(string("status")), descriptor, nullable("sha256")?.jsonPrimitive?.content,
                nullable("skip")?.jsonPrimitive?.content?.let(OptionalImageSkip::valueOf))
        }
        return OptionalImagePlan(assets, value.getValue("expected").takeUnless { it is JsonNull }?.jsonPrimitive?.int,
            value.getValue("coverageKnown").jsonPrimitive.boolean, value.getValue("omitted").jsonPrimitive.int,
            value.getValue("unknownOmissions").jsonPrimitive.boolean)
    }
}

internal fun preparedImageDigest(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }
