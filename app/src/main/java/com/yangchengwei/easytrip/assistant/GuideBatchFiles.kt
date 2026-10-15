package com.yangchengwei.easytrip.assistant

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GuidePreparedBatch(val runId: String, val workspace: File,
    val sources: List<GuidePreparedSource>, val failures: List<ParsedSource>,
    internal val lease: GuideRunLease)

class GuideRunLease internal constructor(private val file: RandomAccessFile, private val lock: FileLock) {
    fun close() {
        try { if (lock.isValid) lock.release() } finally { file.close() }
    }
}

/** Owns only UUID run directories; original images remain owned by AssistantImageStore. */
class GuideBatchFiles(root: File, private val images: AssistantImageStore,
    private val preparationLock: Mutex) {
    private val root = root.canonicalFile

    suspend fun prepare(request: AssistantInputRequest, onProgress: (SourceProgress) -> Unit): GuidePreparedBatch {
        val runId = UUID.randomUUID().toString()
        val workspace = File(root, runId)
        var lease: GuideRunLease? = null
        try {
            return withContext(Dispatchers.IO) {
                require(request.images.size <= AssistantImagePolicy.MAX_IMAGES)
                require(request.images.map { it.id }.distinct().size == request.images.size)
                require(request.images.map { it.ordinal }.distinct().size == request.images.size)
                check(root.isDirectory || root.mkdirs())
                preparationLock.withLock {
                    reclaimAbandoned()
                    check(workspace.mkdir() && workspace.canonicalFile.parentFile == root)
                    val owner = RandomAccessFile(File(workspace, ".guide-owner"), "rw")
                    try {
                        val lock = owner.channel.lock()
                        lease = GuideRunLease(owner, lock)
                        owner.writeUTF("guide-batch-v1:$runId")
                    } catch (e: Exception) { owner.close(); throw e }
                }
                val sources = mutableListOf<GuidePreparedSource>()
                val failures = mutableListOf<ParsedSource>()
                if (request.text.isNotBlank() && (request.retrySourceIds == null || "text" in request.retrySourceIds))
                    sources += GuidePreparedSource(GuideSource("text", "text", null), null,
                        sourceFingerprint = if (request.guideVersion == GuideProtocolVersion.EVIDENCE_V2)
                            sha256(request.text.toByteArray(Charsets.UTF_8)) else null)
                for (image in request.images.filter { request.retrySourceIds == null || it.id in request.retrySourceIds }) {
                    currentCoroutineContext().ensureActive()
                    val target = File(workspace, "${UUID.randomUUID()}.jpg")
                    try {
                        onProgress(SourceProgress(image.id, SourceStage.PREPARING))
                        preparationLock.withLock {
                            val prepared = images.prepare(request.sessionId, image.id)
                            currentCoroutineContext().ensureActive()
                            require(prepared.id == image.id && prepared.mimeType == "image/jpeg")
                            require(prepared.bytes.size in 1..AssistantImagePolicy.MAX_PREPARED_BYTES)
                            target.outputStream().use { it.write(prepared.bytes) }
                            currentCoroutineContext().ensureActive()
                        }
                        sources += GuidePreparedSource(GuideSource("image_${image.ordinal}", image.id, image.ordinal), target,
                            sourceFingerprint = if (request.guideVersion == GuideProtocolVersion.EVIDENCE_V2)
                                sha256(target.readBytes()) else null)
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        target.delete()
                        failures += ParsedSource(image.id, emptyList(), AssistantFailure(
                            (e as? AssistantFailure)?.userMessage ?: "这张图片准备失败，尚未发送；原素材保留。",
                            sendEvidence = SendEvidence.NOT_DISPATCHED))
                    }
                }
                GuidePreparedBatch(runId, workspace, sources, failures, requireNotNull(lease))
            }
        } catch (e: Throwable) {
            // Includes cancellation while dispatching the completed value back to its caller.
            cleanup(runId, workspace, lease)
            throw e
        }
    }

    suspend fun release(batch: GuidePreparedBatch) = cleanup(batch.runId, batch.workspace, batch.lease)

    private fun sha256(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private suspend fun cleanup(runId: String, workspace: File, lease: GuideRunLease?) = withContext(NonCancellable + Dispatchers.IO) {
        // Cleanup cannot revoke validated model results. A remaining marked directory is
        // reclaimed on the next preparation, including after a process restart.
        try {
            if (UUID.fromString(runId).toString() != runId ||
                workspace.canonicalFile != File(root, runId).canonicalFile ||
                workspace.canonicalFile.parentFile != root) return@withContext
            if (workspace.exists()) deleteContentsPreservingMarker(workspace)
        } catch (_: IllegalArgumentException) { /* Unknown ownership: retain everything. */ }
        catch (_: java.io.IOException) { /* Retry on the next preparation. */ }
        catch (_: SecurityException) { /* Retry when cache access is available again. */ }
        finally { runCatching { lease?.close() } }
    }

    private fun deleteContentsPreservingMarker(directory: File) {
        val contents = directory.listFiles() ?: return
        var complete = true
        contents.filter { it.name != ".guide-owner" }.forEach { file ->
            // Never follow a redirected path outside this run, even for cache cleanup.
            if (!file.isFile || file.canonicalFile.parentFile != directory.canonicalFile || !file.delete()) complete = false
        }
        if (complete) {
            File(directory, ".guide-owner").delete()
            directory.delete()
        }
    }

    private fun reclaimAbandoned() {
        root.listFiles().orEmpty().forEach { directory ->
            if (runCatching { UUID.fromString(directory.name).toString() == directory.name }.getOrDefault(false) &&
                directory.isDirectory && directory.canonicalFile == directory.absoluteFile) {
                val marker = File(directory, ".guide-owner")
                if (!marker.isFile || marker.canonicalFile.parentFile != directory) return@forEach
                try {
                    RandomAccessFile(marker, "rw").use { owner ->
                        val lock = try { owner.channel.tryLock() } catch (_: OverlappingFileLockException) { null }
                        if (lock != null) lock.use {
                            if (owner.readUTF() == "guide-batch-v1:${directory.name}") deleteContentsPreservingMarker(directory)
                        }
                    }
                } catch (_: java.io.IOException) { /* Unknown marker or unavailable cache: retain it. */ }
                catch (_: SecurityException) { /* Retain inaccessible files; preparation can still proceed. */ }
            }
        }
    }
}
