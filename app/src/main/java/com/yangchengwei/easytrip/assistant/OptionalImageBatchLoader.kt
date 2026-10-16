package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Two download workers hold at most two raw images; imports share the aggregate byte budget. */
class OptionalImageBatchLoader(
    private val images: AssistantImageStore, private val fetcher: AssistantLinkImageFetcher,
) {
    suspend fun load(session: String, input: AssistantLinkInput, existing: List<AssistantImage>,
        loaded: LoadedAssistantLink, retryIds: Set<String>, progress: (String) -> Unit): LoadedAssistantLink {
        var plan = loaded.imagePlan
        val selected = plan.assets.filter { it.remoteUrl != null && (it.status == OptionalImageStatus.NOT_ATTEMPTED ||
            it.status == OptionalImageStatus.SKIPPED && it.id in retryIds) }
        val created = mutableListOf<AssistantImage>()
        val importing = Mutex()
        try {
            val completed = withTimeoutOrNull(30_000) {
                coroutineScope {
                    val cursor = java.util.concurrent.atomic.AtomicInteger()
                    (0 until minOf(2, selected.size)).map { async {
                        while (true) {
                            val index = cursor.getAndIncrement()
                            if (index >= selected.size) break
                            val asset = selected[index]
                            currentCoroutineContext().ensureActive()
                            var reason = OptionalImageSkip.DOWNLOAD
                            var createdThisAttempt = false
                            try {
                                progress("正在读取可选图片 ${asset.slot}")
                                if (asset.descriptor != null && asset.preparedSha256 != null) {
                                    val cached = try {
                                        val prepared = images.prepare(session, asset.id)
                                        preparedImageDigest(prepared.bytes) == asset.preparedSha256
                                    } catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { false }
                                    if (cached) {
                                        importing.withLock { plan = plan.updated(asset.copy(status = OptionalImageStatus.AVAILABLE, skip = null)) }
                                        continue
                                    }
                                }
                                // Policy checks also protect callers loading persisted plans.
                                val url = AssistantLinkPolicy.image(input.platform, requireNotNull(asset.remoteUrl))
                                val bytes = fetcher.fetch(input.platform, url)
                                importing.withLock {
                                    reason = OptionalImageSkip.LIMIT
                                    val retained = (existing + plan.descriptors).distinctBy { it.id }.filterNot { it.id == asset.id }
                                    AssistantImagePolicy.rejection(retained.size, retained.sumOf { it.byteCount }, bytes.size.toLong())
                                        ?.let { throw AssistantFailure(it) }
                                    reason = OptionalImageSkip.DECODE
                                    val descriptor = images.importSource(session, asset.id, asset.slot, bytes, retained)
                                    created += descriptor
                                    createdThisAttempt = true
                                    val prepared = images.prepare(session, descriptor.id)
                                    currentCoroutineContext().ensureActive()
                                    plan = plan.updated(asset.copy(status = OptionalImageStatus.AVAILABLE,
                                        descriptor = descriptor, preparedSha256 = preparedImageDigest(prepared.bytes), skip = null))
                                }
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) {
                                // Only this attempt's new temporary file may be discarded.
                                withContext(NonCancellable) {
                                    importing.withLock {
                                        if (createdThisAttempt && asset.descriptor == null)
                                            runCatching { images.remove(session, asset.id) }.onSuccess {
                                                created.removeAll { it.id == asset.id }
                                            }
                                    }
                                }
                                importing.withLock { plan = plan.updated(asset.skipped(reason)) }
                            }
                        }
                    } }.awaitAll()
                }
                true
            }
            currentCoroutineContext().ensureActive()
            if (completed == null) plan = plan.copy(assets = plan.assets.map {
                if (selected.any { selected -> selected.id == it.id } && it.status != OptionalImageStatus.AVAILABLE)
                    it.skipped(OptionalImageSkip.TIMEOUT) else it
            })
            withContext(NonCancellable) {
                created.filter { image -> plan.available.none { it.id == image.id } &&
                    loaded.imagePlan.descriptors.none { it.id == image.id } }.forEach { runCatching { images.remove(session, it.id) } }
            }
            return loaded.copy(images = plan.available, imagePlan = plan)
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                created.filter { image -> loaded.imagePlan.descriptors.none { it.id == image.id } }
                    .forEach { runCatching { images.remove(session, it.id) } }
            }
            throw e
        }
    }
}
