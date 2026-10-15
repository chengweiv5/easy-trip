package com.yangchengwei.easytrip.assistant.cityfirst

import android.util.AtomicFile
import com.yangchengwei.easytrip.assistant.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun readGuideFile(file: File): String {
    val bytes = AtomicFile(file).openRead().use { input ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() <= GUIDE_RECORD_LIMIT) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
    require(bytes.size <= GUIDE_RECORD_LIMIT) { "恢复记录过大，原记录已保留" }
    return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
}
internal fun writeGuideFile(file: File, text: String) {
    val bytes = text.toByteArray()
    require(bytes.size <= GUIDE_RECORD_LIMIT)
    file.parentFile?.mkdirs()
    val atomic = AtomicFile(file)
    val out = atomic.startWrite()
    try { out.write(bytes); atomic.finishWrite(out) }
    catch (e: Exception) { atomic.failWrite(out); throw e }
}
internal fun encodeGuideImages(images: List<AssistantImage>) = buildJsonArray {
    images.forEach { image -> addJsonObject {
        put("id", image.id); put("ordinal", image.ordinal); put("mime", image.mimeType)
        put("bytes", image.byteCount); put("width", image.width); put("height", image.height)
    } }
}
internal fun decodeGuideImages(value: JsonElement) = value.jsonArray.map {
    val r = it.jsonObject
    val id = r.string("id"); require(validGuideId(id))
    AssistantImage(id, r.getValue("ordinal").jsonPrimitive.int, r.string("mime"),
        r.getValue("bytes").jsonPrimitive.long, r.getValue("width").jsonPrimitive.int, r.getValue("height").jsonPrimitive.int)
}.also { images ->
    require(images.size <= AssistantImagePolicy.MAX_IMAGES && images.map { it.id }.distinct().size == images.size)
    require(images.all { it.byteCount in 1..AssistantImagePolicy.MAX_SINGLE_BYTES && it.width > 0 && it.height > 0 })
    require(images.sumOf { it.byteCount } <= AssistantImagePolicy.MAX_TOTAL_BYTES)
}

data class GuideMaterials(val ref: GuideMaterialRef, val originalText: String, val userIntent: String,
    val modelText: String, val imageSessionId: String, val images: List<AssistantImage>, val fromLink: Boolean)

/** This store is separate from disposable per-workspace images. Database references pin its sessions. */
class GuideMaterialStore(
    private val root: File, val images: AssistantImageStore, private val links: AssistantLinkLoader,
    private val referenced: suspend () -> List<String>,
) {
    private val lock = Mutex()
    /** Retain already-loaded content; never resolve the URL again during checkpointing. */
    suspend fun retainLoaded(original: String, modelText: String, fromLink: Boolean,
        sourceSession: String, selected: List<AssistantImage>, sourceImages: AssistantImageStore): GuideMaterials =
        withContext(Dispatchers.IO) { lock.withLock {
            require(original.length <= 20_000 && modelText.length <= 20_000)
            val id = java.util.UUID.randomUUID().toString()
            try {
                val checksums = linkedMapOf<String, String>()
                val copies = selected.map { image ->
                    val prepared = sourceImages.prepare(sourceSession, image.id)
                    checksums[image.id] = guideDigest(prepared.bytes)
                    images.restoreCopy(id, image, prepared)
                }
                val payload = buildJsonObject {
                    put("version", 1); put("session", id); put("original", original)
                    put("intent", tripUserIntent(original)); put("text", modelText); put("link", fromLink)
                    put("images", encodeGuideImages(copies))
                    put("checksums", JsonObject(checksums.mapValues { JsonPrimitive(it.value) }))
                }.toString()
                writeGuideFile(file(id), payload)
                GuideMaterials(GuideMaterialRef(id, guideDigest(payload.toByteArray())), original,
                    tripUserIntent(original), modelText, id, copies, fromLink)
            } catch (e: Exception) {
                withContext(NonCancellable) { AtomicFile(file(id)).delete(); images.release(id) }
                throw e
            }
        } }
    suspend fun prepare(sessionId: String, original: String, selected: List<AssistantImage>,
        progress: (String) -> Unit): GuideMaterials = withContext(Dispatchers.IO) { lock.withLock {
        require(validGuideId(sessionId) && original.length <= 20_000)
        check(sessionId !in referenced()) { "已交接的攻略不能被覆盖" }
        val previous = runCatching {
            decodeGuideImages(Json.parseToJsonElement(readGuideFile(file(sessionId))).jsonObject.getValue("images"))
        }.getOrDefault(emptyList())
        val link = AssistantLinkInput.parse(original)
        var loaded: LoadedAssistantLink? = null
        try {
        loaded = link?.let { links.load(sessionId, it, selected, progress) }
        val all = selected + loaded?.images.orEmpty()
        decodeGuideImages(encodeGuideImages(all))
        val text = loaded?.modelText ?: original
        val checksums = all.associate { it.id to guideDigest(images.prepare(sessionId, it.id).bytes) }
        val payload = buildJsonObject {
            put("version", 1); put("session", sessionId); put("original", original)
            put("intent", tripUserIntent(original)); put("text", text); put("link", link != null)
            put("images", encodeGuideImages(all))
            put("checksums", JsonObject(checksums.mapValues { JsonPrimitive(it.value) }))
        }.toString()
        val ref = GuideMaterialRef(sessionId, guideDigest(payload.toByteArray()))
        writeGuideFile(file(sessionId), payload)
        withContext(NonCancellable) {
            previous.filterNot { old -> all.any { it.id == old.id } }.forEach {
                runCatching { images.remove(sessionId, it.id) }
            }
        }
        GuideMaterials(ref, original, tripUserIntent(original), text, sessionId, all, link != null)
        } catch (e: Exception) {
            withContext(NonCancellable) { loaded?.images?.forEach { runCatching { images.remove(sessionId, it.id) } } }
            throw e
        }
    } }
    /** Serializes commit with release/overwrite; the transaction installs the durable reference before unlock. */
    suspend fun <T> withRetained(ref: GuideMaterialRef, block: suspend () -> T): T = lock.withLock {
        read(ref)
        block()
    }
    suspend fun read(ref: GuideMaterialRef): GuideMaterials = withContext(Dispatchers.IO) {
        val payload = readGuideFile(file(ref.id))
        require(guideDigest(payload.toByteArray()) == ref.digest) { "攻略素材已变化，请重新选择" }
        val r = Json.parseToJsonElement(payload).jsonObject
        require(r.getValue("version").jsonPrimitive.int == 1 && r.string("session") == ref.id)
        val all = decodeGuideImages(r.getValue("images"))
        for (image in all) require(guideDigest(images.prepare(ref.id, image.id).bytes) ==
            r.getValue("checksums").jsonObject.string(image.id)) { "图片已失效，请重新选择" }
        GuideMaterials(ref, r.string("original"), r.string("intent"), r.string("text"), ref.id, all,
            r.getValue("link").jsonPrimitive.boolean)
    }
    suspend fun release(sessionId: String) = withContext(Dispatchers.IO) { lock.withLock {
        require(validGuideId(sessionId))
        if (sessionId !in referenced()) {
            AtomicFile(file(sessionId)).delete()
            images.release(sessionId)
        }
    } }
    private fun file(id: String): File {
        require(validGuideId(id)); return File(root, "$id.json")
    }
}
