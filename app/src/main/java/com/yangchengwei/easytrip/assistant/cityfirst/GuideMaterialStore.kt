package com.yangchengwei.easytrip.assistant.cityfirst

import android.util.AtomicFile
import com.yangchengwei.easytrip.assistant.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    require(images.map { it.ordinal }.distinct().size == images.size && images.all { it.ordinal in 1..20 })
    require(images.all { it.mimeType in setOf("image/jpeg", "image/png", "image/webp") })
    require(images.all { it.byteCount in 1..AssistantImagePolicy.MAX_SINGLE_BYTES && it.width > 0 && it.height > 0 })
    require(images.sumOf { it.byteCount } <= AssistantImagePolicy.MAX_TOTAL_BYTES)
}

data class GuideMaterials(val ref: GuideMaterialRef, val originalText: String, val userIntent: String,
    val modelText: String, val imageSessionId: String, val images: List<AssistantImage>, val fromLink: Boolean,
    val imagePlan: OptionalImagePlan = OptionalImagePlan(), val linkInput: AssistantLinkInput? = null,
    val bodyAvailable: Boolean = true)

/** Durable source identity is independent of whether an optional image file survives. */
class GuideMaterialStore(
    private val root: File, val images: AssistantImageStore, private val links: AssistantLinkLoader,
    private val referenced: suspend () -> List<String>,
) {
    private val lock = Mutex()
    suspend fun retainLoaded(original: String, modelText: String, fromLink: Boolean,
        sourceSession: String, selected: List<AssistantImage>, sourceImages: AssistantImageStore,
        imagePlan: OptionalImagePlan = OptionalImagePlan(), bodyAvailable: Boolean = true): GuideMaterials = withContext(Dispatchers.IO) { lock.withLock {
        require(original.length <= 20_000 && modelText.length <= 20_000)
        val id = java.util.UUID.randomUUID().toString()
        val plan = if (imagePlan.assets.isEmpty()) OptionalImagePlan.local(selected) else imagePlan
        try {
            val retained = plan.copy(assets = plan.assets.map { asset ->
                val descriptor = asset.descriptor ?: return@map asset
                try {
                    val prepared = sourceImages.prepare(sourceSession, asset.id)
                    val copy = images.restoreCopy(id, descriptor, prepared)
                    asset.copy(descriptor = copy, preparedSha256 = guideDigest(prepared.bytes),
                        status = if (asset.status == OptionalImageStatus.SKIPPED) asset.status else OptionalImageStatus.AVAILABLE)
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { asset.skipped(OptionalImageSkip.MISSING) }
            })
            persistUnlocked(GuideMaterials(GuideMaterialRef(id, "0".repeat(64)), original, tripUserIntent(original),
                modelText, id, retained.descriptors, fromLink, retained, AssistantLinkInput.parse(original), bodyAvailable))
        } catch (e: Exception) {
            withContext(NonCancellable) { AtomicFile(file(id)).delete(); images.release(id) }
            throw e
        }
    } }

    suspend fun prepare(sessionId: String, original: String, selected: List<AssistantImage>,
        progress: (String) -> Unit): GuideMaterials = withContext(Dispatchers.IO) { lock.withLock {
        require(validGuideId(sessionId) && original.length <= 20_000)
        check(sessionId !in referenced()) { "已交接的攻略不能被覆盖" }
        decodeGuideImages(encodeGuideImages(selected))
        val link = AssistantLinkInput.parse(original)
        val loaded = link?.let { links.read(it, selected, progress) }
        val local = OptionalImagePlan.local(selected)
        val remote = loaded?.let { source ->
            if (source.imagePlan.assets.isNotEmpty() || source.images.isEmpty()) source.imagePlan
            else OptionalImagePlan.local(source.images)
        }
        val plan = remote?.let { it.copy(assets = local.assets + it.assets) } ?: local
        persistUnlocked(GuideMaterials(GuideMaterialRef(sessionId, "0".repeat(64)), original, tripUserIntent(original),
            loaded?.modelText ?: original, sessionId, selected + loaded?.images.orEmpty(), link != null,
            plan, link, loaded?.bodyAvailable ?: true))
    } }

    suspend fun prepareOptional(material: GuideMaterials, retryIds: Set<String> = emptySet()): GuideMaterials {
        // Fork a pinned source before preparing; never replace a database-bound digest.
        val current = if (material.ref.id in referenced()) retainLoaded(material.originalText, material.modelText,
            material.fromLink, material.imageSessionId, material.images, images, material.imagePlan,
            material.bodyAvailable) else material
        return withContext(Dispatchers.IO) { lock.withLock {
            var plan = current.imagePlan
            current.linkInput?.let { input ->
                val remote = plan.copy(assets = plan.assets.filter { it.remoteUrl != null })
                val loaded = links.prepare(current.imageSessionId, input,
                    plan.assets.filter { it.remoteUrl == null }.mapNotNull { it.descriptor },
                    LoadedAssistantLink(current.modelText, remote.available, remote, current.bodyAvailable), retryIds) {}
                plan = plan.copy(assets = plan.assets.map { a -> loaded.imagePlan.assets.find { it.id == a.id } ?: a })
            }
            plan = plan.copy(assets = plan.assets.map { asset ->
                if (asset.remoteUrl != null || asset.status == OptionalImageStatus.AVAILABLE ||
                    asset.status == OptionalImageStatus.SKIPPED && asset.id !in retryIds) return@map asset
                try {
                    val prepared = images.prepare(current.imageSessionId, asset.id)
                    asset.copy(status = OptionalImageStatus.AVAILABLE, preparedSha256 = guideDigest(prepared.bytes), skip = null)
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { asset.skipped(OptionalImageSkip.DECODE) }
            })
            persistUnlocked(current.copy(imagePlan = plan, images = plan.descriptors))
        } }
    }
    suspend fun persistOptional(material: GuideMaterials): GuideMaterials = withContext(Dispatchers.IO) { lock.withLock {
        check(material.ref.id !in referenced()) { "已交接的攻略不能被覆盖" }
        persistUnlocked(material)
    } }
    private fun persistUnlocked(material: GuideMaterials): GuideMaterials {
        val payload = buildJsonObject {
            put("version", 2); put("session", material.imageSessionId); put("original", material.originalText)
            put("intent", material.userIntent); put("text", material.modelText); put("link", material.fromLink)
            put("bodyAvailable", material.bodyAvailable); put("imagePlan", OptionalImagePlanCodec.encode(material.imagePlan))
            put("images", encodeGuideImages(material.imagePlan.descriptors))
        }.toString()
        val digest = guideDigest(payload.toByteArray())
        // Content-addressed snapshots keep the previous journal ref valid until its CAS.
        writeGuideFile(snapshotFile(material.imageSessionId, digest), payload)
        return material.copy(ref = GuideMaterialRef(material.imageSessionId, guideDigest(payload.toByteArray())),
            images = material.imagePlan.descriptors)
    }
    /** Derived cache only: never changes the immutable, database-bound source metadata. */
    suspend fun cacheWorkspaceImages(ref: GuideMaterialRef, descriptors: List<AssistantImage>,
        sourceSession: String, sourceImages: AssistantImageStore) = withContext(Dispatchers.IO) { lock.withLock {
        read(ref)
        for (descriptor in descriptors) {
            try { images.restoreCopy(ref.id, descriptor, sourceImages.prepare(sourceSession, descriptor.id)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Optional cache failure does not revoke validated results. */ }
        }
    } }
    suspend fun <T> withRetained(ref: GuideMaterialRef, block: suspend () -> T): T = lock.withLock {
        read(ref); block()
    }
    suspend fun read(ref: GuideMaterialRef): GuideMaterials = withContext(Dispatchers.IO) {
        val versioned = snapshotFile(ref.id, ref.digest)
        val payload = readGuideFile(if (versioned.isFile) versioned else file(ref.id))
        require(guideDigest(payload.toByteArray()) == ref.digest) { "攻略素材已变化，请重新选择" }
        val r = Json.parseToJsonElement(payload).jsonObject
        val version = r.getValue("version").jsonPrimitive.int
        require(version in 1..2 && r.string("session") == ref.id)
        val original = r.string("original"); val text = r.string("text")
        require(original.length <= 20_000 && text.length <= 20_000)
        val link = AssistantLinkInput.parse(original)
        val all = decodeGuideImages(r.getValue("images"))
        val initial = if (version == 2) OptionalImagePlanCodec.decode(r.getValue("imagePlan").jsonObject, link?.platform)
            else OptionalImagePlan(all.map { image -> OptionalImageAsset(image.id, image.ordinal, null, null,
                OptionalImageStatus.AVAILABLE, image, r.getValue("checksums").jsonObject.string(image.id)) })
        require(initial.descriptors == all)
        val plan = initial.copy(assets = initial.assets.map { a ->
            if (a.status != OptionalImageStatus.AVAILABLE) return@map a
            try {
                val prepared = images.prepare(ref.id, a.id)
                require(guideDigest(prepared.bytes) == a.preparedSha256)
                a
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { a.skipped(OptionalImageSkip.MISSING) }
        })
        GuideMaterials(ref, original, r.string("intent"), text, ref.id, all,
            r.getValue("link").jsonPrimitive.boolean, plan, link,
            if (version == 2) r.getValue("bodyAvailable").jsonPrimitive.boolean else true)
    }
    suspend fun release(sessionId: String) = withContext(Dispatchers.IO) { lock.withLock {
        require(validGuideId(sessionId))
        if (sessionId !in referenced()) {
            AtomicFile(file(sessionId)).delete()
            root.listFiles()?.filter { it.name.startsWith("$sessionId-") && it.name.endsWith(".json") }
                ?.forEach { AtomicFile(it).delete() }
            images.release(sessionId)
        }
    } }
    private fun snapshotFile(id: String, digest: String): File {
        require(validGuideId(id) && Regex("[a-f0-9]{64}").matches(digest))
        return File(root, "$id-$digest.json")
    }
    private fun file(id: String): File { require(validGuideId(id)); return File(root, "$id.json") }
}
