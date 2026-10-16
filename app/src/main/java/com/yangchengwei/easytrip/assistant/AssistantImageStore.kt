package com.yangchengwei.easytrip.assistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AssistantImage(val id: String, val ordinal: Int, val mimeType: String,
    val byteCount: Long, val width: Int, val height: Int)
class PreparedImage(val id: String, val mimeType: String, val bytes: ByteArray) {
    override fun toString() = "PreparedImage(id=$id, payload=redacted)"
}
data class ImageSelectionResult(val accepted: List<AssistantImage>, val rejected: List<String>)

interface AssistantImageStore {
    suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>): ImageSelectionResult
    suspend fun prepare(sessionId: String, imageId: String): PreparedImage
    suspend fun preview(sessionId: String, imageId: String): Bitmap
    suspend fun thumbnail(sessionId: String, imageId: String): Bitmap = preview(sessionId, imageId)
    suspend fun remove(sessionId: String, imageId: String)
    suspend fun release(sessionId: String)
    suspend fun importBytes(sessionId: String, bytes: ByteArray, existing: List<AssistantImage>): AssistantImage =
        throw AssistantFailure("当前图片存储不支持链接素材。")
    suspend fun importSource(sessionId: String, sourceId: String, ordinal: Int, bytes: ByteArray,
        existing: List<AssistantImage>): AssistantImage = throw AssistantFailure("当前图片存储不支持稳定素材来源。")
    suspend fun restoreCopy(sessionId: String, descriptor: AssistantImage, prepared: PreparedImage): AssistantImage =
        throw AssistantFailure("当前图片存储不支持恢复素材。")
}

/** App-scoped, private, temporary storage. No URI, image or model payload enters a database. */
class AndroidAssistantImageStore(
    context: Context,
    private val root: File = File(context.noBackupFilesDir, "assistant-input"),
    private val clearOnStart: Boolean = true,
) : AssistantImageStore {
    private val resolver = context.applicationContext.contentResolver
    private val lock = Mutex()
    private val interrupted = mutableSetOf<String>()
    override suspend fun restoreCopy(sessionId: String, descriptor: AssistantImage, prepared: PreparedImage): AssistantImage =
        withContext(Dispatchers.IO) {
            lock.withLock {
                require(descriptor.id == prepared.id && validId(descriptor.id))
                require(prepared.bytes.size in 1..AssistantImagePolicy.MAX_PREPARED_BYTES)
                AssistantImagePolicy.mime(prepared.bytes)
                directory(sessionId)
                val target = android.util.AtomicFile(file(sessionId, descriptor.id))
                val output = target.startWrite()
                try { output.write(prepared.bytes); target.finishWrite(output) }
                catch (e: Exception) { target.failWrite(output); throw e }
                val marker = android.util.AtomicFile(preparedMarker(sessionId, descriptor.id))
                val markerOut = marker.startWrite()
                try { markerOut.write(digest(prepared.bytes).toByteArray()); marker.finishWrite(markerOut) }
                catch (e: Exception) { marker.failWrite(markerOut); throw e }
                descriptor.copy(mimeType = prepared.mimeType, byteCount = prepared.bytes.size.toLong())
            }
        }
    override suspend fun importBytes(sessionId: String, bytes: ByteArray, existing: List<AssistantImage>): AssistantImage =
        importSource(sessionId, UUID.randomUUID().toString(), (1..AssistantImagePolicy.MAX_IMAGES).firstOrNull { n -> existing.none { it.ordinal == n } } ?: (AssistantImagePolicy.MAX_IMAGES + 1), bytes, existing)
    override suspend fun importSource(sessionId: String, sourceId: String, ordinal: Int,
        bytes: ByteArray, existing: List<AssistantImage>): AssistantImage {
        require(validId(sourceId) && ordinal in 1..AssistantImagePolicy.MAX_IMAGES)
        require(existing.none { it.id == sourceId || it.ordinal == ordinal })
        var created: File? = null
        try {
            return withContext(Dispatchers.IO) {
                lock.withLock {
                    currentCoroutineContext().ensureActive()
                    AssistantImagePolicy.rejection(existing.size, existing.sumOf { it.byteCount }, bytes.size.toLong())
                        ?.let { throw AssistantFailure(it) }
                    val mime = AssistantImagePolicy.mime(bytes)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    AssistantImagePolicy.targetSize(bounds.outWidth, bounds.outHeight)
                    val id = sourceId
                    directory(sessionId)
                    val destination = file(sessionId, id)
                    if (!destination.exists()) created = destination
                    val atomic = android.util.AtomicFile(destination)
                    val output = atomic.startWrite()
                    try {
                        output.write(bytes)
                        currentCoroutineContext().ensureActive()
                        atomic.finishWrite(output)
                        AssistantImage(id, ordinal,
                            mime, bytes.size.toLong(), bounds.outWidth, bounds.outHeight)
                    } catch (error: Exception) {
                        atomic.failWrite(output)
                        throw error
                    }
                }
            }
        } catch (error: Throwable) {
            // Also covers cancellation on the return dispatch from IO to the caller.
            created?.delete()
            throw error
        }
    }
    init {
        // Only our previous process's session directories, never other application caches.
        root.listFiles()?.filter { clearOnStart && it.isDirectory && validId(it.name) }?.forEach {
            if (it.listFiles()?.any { child -> child.isFile && validId(child.name) } == true)
                runCatching { File(it, "trip").readText() }.getOrNull()?.let(interrupted::add)
            it.deleteRecursively()
        }
        root.mkdirs()
    }
    @Synchronized fun startSession(session: String, trip: String): Boolean {
        val hash = digest(trip.toByteArray())
        File(directory(session), "trip").writeText(hash)
        return interrupted.remove(hash)
    }
    private fun validId(value: String) = runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
    private fun directory(session: String): File {
        require(validId(session))
        return File(root, session).apply { mkdirs() }
    }
    private fun file(session: String, id: String): File {
        require(validId(session) && validId(id))
        return File(File(root, session), id)
    }
    private fun preparedMarker(session: String, id: String) = File(file(session, id).parentFile, ".prepared-$id")
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }

    override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>) =
        withContext(Dispatchers.IO) {
            lock.withLock {
                val accepted = mutableListOf<AssistantImage>()
                val rejected = mutableListOf<String>()
                var total = existing.sumOf { it.byteCount }
                val hashes = existing.mapNotNull { runCatching { digest(file(sessionId, it.id).readBytes()) }.getOrNull() }.toMutableSet()
                try {
                    uris.distinct().forEachIndexed { index, uri ->
                        currentCoroutineContext().ensureActive()
                        try {
                            if (existing.size + accepted.size >= AssistantImagePolicy.MAX_IMAGES)
                                throw AssistantFailure("每批最多 ${AssistantImagePolicy.MAX_IMAGES} 张图片")
                            val bytes = resolver.openInputStream(uri)?.use { input ->
                                val out = ByteArrayOutputStream()
                                val buffer = ByteArray(16 * 1024)
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val n = input.read(buffer, 0, minOf(buffer.size, (AssistantImagePolicy.MAX_SINGLE_BYTES + 1 - out.size()).toInt()))
                                    if (n < 0) break
                                    if (n == 0) continue
                                    out.write(buffer, 0, n)
                                    if (out.size() > AssistantImagePolicy.MAX_SINGLE_BYTES) throw AssistantFailure("单张图片不能超过 10 MB")
                                }
                                out.toByteArray()
                            } ?: throw AssistantFailure("无法读取，请重新选择图片")
                            val hash = digest(bytes)
                            if (hash in hashes) throw AssistantFailure("此图片已在本批附件中")
                            AssistantImagePolicy.rejection(existing.size + accepted.size, total, bytes.size.toLong())?.let { throw AssistantFailure(it) }
                            val mime = AssistantImagePolicy.mime(bytes)
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            AssistantImagePolicy.targetSize(bounds.outWidth, bounds.outHeight)
                            val id = UUID.randomUUID().toString()
                            directory(sessionId)
                            val destination = file(sessionId, id)
                            try { destination.writeBytes(bytes) } catch (e: Exception) { destination.delete(); throw e }
                            val ordinal = (1..AssistantImagePolicy.MAX_IMAGES).first { n -> (existing + accepted).none { it.ordinal == n } }
                            accepted += AssistantImage(id, ordinal, mime, bytes.size.toLong(), bounds.outWidth, bounds.outHeight)
                            hashes += hash
                            total += bytes.size
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { rejected += "所选第 ${index + 1} 张：${(e as? AssistantFailure)?.userMessage ?: "无法读取，请重新选择图片"}" }
                    }
                    ImageSelectionResult(accepted, rejected)
                } catch (e: CancellationException) {
                    accepted.forEach { file(sessionId, it.id).delete() }
                    throw e
                }
            }
        }

    private fun decode(session: String, id: String, maxEdge: Int): Bitmap {
        val source = file(session, id)
        if (!source.isFile || source.length() > AssistantImagePolicy.MAX_SINGLE_BYTES) throw AssistantFailure("图片已失效，请重新选择。")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        val (w, h) = AssistantImagePolicy.targetSize(bounds.outWidth, bounds.outHeight)
        val scale = minOf(1.0, maxEdge.toDouble() / maxOf(w, h))
        val targetW = maxOf(1, (w * scale).toInt())
        val targetH = maxOf(1, (h * scale).toInt())
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH) sample *= 2
        while ((bounds.outWidth.toLong() / sample) * (bounds.outHeight / sample) > 8_388_608L) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply {
            inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: throw AssistantFailure("图片损坏或无法解码，请重新选择。")
        val sized = Bitmap.createScaledBitmap(decoded, minOf(targetW, decoded.width), minOf(targetH, decoded.height), true)
        if (sized !== decoded) decoded.recycle()
        val orientation = runCatching { ExifInterface(source).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        val matrix = Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(270f); postScale(-1f, 1f) }
                8 -> setRotate(270f)
            }
        }
        val oriented = Bitmap.createBitmap(sized, 0, 0, sized.width, sized.height, matrix, true)
        if (oriented !== sized) sized.recycle()
        return oriented
    }
    override suspend fun preview(sessionId: String, imageId: String) = withContext(Dispatchers.IO) {
        lock.withLock { decode(sessionId, imageId, 4096) }
    }
    override suspend fun thumbnail(sessionId: String, imageId: String) = withContext(Dispatchers.IO) {
        lock.withLock { decode(sessionId, imageId, 256) }
    }
    override suspend fun prepare(sessionId: String, imageId: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            currentCoroutineContext().ensureActive()
            // restoreCopy already contains sanitized model bytes. Re-encoding would change
            // the evidence fingerprint and defeat cached retry/recovery identity.
            val marker = preparedMarker(sessionId, imageId)
            val source = file(sessionId, imageId)
            if (marker.isFile && marker.length() == 64L && source.isFile &&
                source.length() in 1..AssistantImagePolicy.MAX_PREPARED_BYTES.toLong()) {
                val bytes = source.readBytes()
                if (marker.readText() == digest(bytes) && AssistantImagePolicy.mime(bytes) == "image/jpeg")
                    return@withLock PreparedImage(imageId, "image/jpeg", bytes)
            }
            val bitmap = decode(sessionId, imageId, 4096)
            val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
            bitmap.recycle()
            try {
                for (quality in listOf(90, 85, 80, 75, 70)) {
                    currentCoroutineContext().ensureActive()
                    val bytes = ByteArrayOutputStream().use {
                        if (!opaque.compress(Bitmap.CompressFormat.JPEG, quality, it)) throw AssistantFailure("图片编码失败。")
                        it.toByteArray()
                    }
                    if (bytes.size <= AssistantImagePolicy.MAX_PREPARED_BYTES) return@withLock PreparedImage(imageId, "image/jpeg", bytes)
                }
                throw AssistantFailure("图片准备后仍超过 4 MB，请在相册处理后重选；原图已保留。")
            } finally { opaque.recycle() }
        }
    }
    override suspend fun remove(sessionId: String, imageId: String) = withContext(Dispatchers.IO) {
        lock.withLock { file(sessionId, imageId).delete(); preparedMarker(sessionId, imageId).delete(); Unit }
    }
    override suspend fun release(sessionId: String) = withContext(Dispatchers.IO) {
        lock.withLock { require(validId(sessionId)); File(root, sessionId).deleteRecursively(); Unit }
    }
}
