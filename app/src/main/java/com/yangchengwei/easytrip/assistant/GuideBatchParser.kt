package com.yangchengwei.easytrip.assistant

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** Per-source validation behind one bounded multimodal request; never performs normalization or POI writes. */
class BatchedGuideMaterialParser(
    private val config: ProviderConfig,
    private val files: GuideBatchFiles,
    private val body: GuideBatchBody,
    private val transport: AssistantTransport,
    private val scheduler: GuideBatchScheduler,
    private val diagnostics: AssistantProtocolDiagnostics = AssistantProtocolDiagnostics.NONE,
    private val protocol: GuideWireProtocol = LegacyGuideWireProtocol,
    private val onUnsupportedImage: () -> Unit = {},
) : GuideMaterialParser {
    private sealed interface Event {
        data class Progress(val value: SourceProgress) : Event
        data class Source(val value: ParsedSource, val received: CompletableDeferred<Unit>) : Event
    }

    override suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
        onProgress: (SourceProgress) -> Unit): ParsedInputBatch = coroutineScope {
        val context = GuideContext(request.text, request.city, request.cityScope)
        val targets = (if (request.text.isNotBlank()) listOf("text") else emptyList()) +
            request.images.map { it.id }
        val selected = targets.filter { request.retrySourceIds == null || it in request.retrySourceIds }
        val results = linkedMapOf<String, ParsedSource>()
        val events = Channel<Event>(Channel.UNLIMITED) // Bounded by 21 sources and at most two attempts.
        val stop = AtomicReference<AssistantFailure?>(null)
        suspend fun publish(source: ParsedSource) {
            val received = CompletableDeferred<Unit>()
            events.send(Event.Source(source, received))
            received.await()
        }
        fun progress(source: SourceProgress) { events.trySend(Event.Progress(source)) }
        val producer = launch {
            try {
                if (selected.isEmpty()) return@launch
                val policy = GuideBatchPolicies.forConfig(config)
                val prepared = files.prepare(request, ::progress)
                try {
                    prepared.failures.forEach { publish(it) }
                    val plan = body.groups(config, context, prepared.sources, policy)
                    plan.failures.forEach { publish(it) }
                    coroutineScope {
                        plan.groups.map { group -> launch {
                            scheduler.withSlot {
                                currentCoroutineContext().ensureActive()
                                val blocked = stop.get()
                                if (blocked != null) {
                                    group.sources.forEach { source -> publish(ParsedSource(source.source.localId,
                                        emptyList(), AssistantFailure("前组服务拒绝请求，本份素材尚未发送；请检查设置后重试。",
                                            blocked.code, SendEvidence.NOT_DISPATCHED))) }
                                    return@withSlot
                                }
                                val operation = UUID.randomUUID().toString()
                                suspend fun attempt(target: GuideGroup, repair: Boolean): GuideGroupDecode {
                                  var dispatched = false
                                  return try {
                                    val file = body.write(config, context, target, policy, repair, prepared.workspace)
                                    val response = try {
                                        currentCoroutineContext().ensureActive()
                                        stop.get()?.let { throw AssistantFailure(
                                            "前组服务拒绝请求，本份素材尚未发送；请检查设置后重试。",
                                            it.code, SendEvidence.NOT_DISPATCHED) }
                                        dispatched = true
                                        target.sources.forEach { progress(SourceProgress(it.source.localId,
                                            SourceStage.SENDING, sendEvidence = SendEvidence.POSSIBLY_SENT)) }
                                        transport.postFile(config, file, policy) {
                                            target.sources.forEach { progress(SourceProgress(it.source.localId,
                                                SourceStage.UNDERSTANDING, sendEvidence = SendEvidence.POSSIBLY_SENT)) }
                                        }
                                    } finally { withContext(NonCancellable + Dispatchers.IO) { file.delete() } }
                                    try { protocol.decode(response, context, target.sources, request.guideBinding) }
                                    catch (e: AssistantFailure) {
                                        GuideGroupDecode(target.sources.map { ParsedSource(it.source.localId, emptyList(), e) },
                                            if (e.protocolIssue?.repairable == true)
                                                target.sources.map { it.source.localId }.toSet() else emptySet())
                                    }
                                  } catch (e: CancellationException) { throw e }
                                  catch (e: Exception) {
                                    val failure = e as? AssistantFailure ?: AssistantFailure(
                                        "本组素材未能处理，原素材保留，请手动重试。",
                                        sendEvidence = if (dispatched) SendEvidence.POSSIBLY_SENT else SendEvidence.NOT_DISPATCHED)
                                    GuideGroupDecode(target.sources.map {
                                        ParsedSource(it.source.localId, emptyList(), failure)
                                    }, emptySet())
                                  }
                                }
                                fun stopIfRejected(decoded: GuideGroupDecode) {
                                    val failure = decoded.sources.firstNotNullOfOrNull { it.failure }
                                    if (failure?.code in setOf(AssistantFailureCode.AUTH, AssistantFailureCode.UNSUPPORTED_IMAGE)) {
                                        stop.compareAndSet(null, failure)
                                        if (failure?.code == AssistantFailureCode.UNSUPPORTED_IMAGE) {
                                            try { onUnsupportedImage() }
                                            catch (e: CancellationException) { throw e }
                                            catch (_: Exception) {
                                                // Capability persistence is advisory; keep this rejection and other groups' results.
                                            }
                                        }
                                    }
                                }
                                val decoded = attempt(group, false)
                                stopIfRejected(decoded)
                                val repairIds = decoded.repairableSourceIds
                                record(operation, 1, decoded.sources.firstNotNullOfOrNull { it.failure }, repairIds.isNotEmpty())
                                decoded.sources.filter { it.sourceId !in repairIds }.forEach { publish(it) }
                                if (repairIds.isNotEmpty()) {
                                    val repaired = attempt(group.copy(sources = group.sources.filter {
                                        it.source.localId in repairIds
                                    }), true)
                                    stopIfRejected(repaired)
                                    record(operation, 2, repaired.sources.firstNotNullOfOrNull { it.failure })
                                    repaired.sources.forEach { publish(it) }
                                }
                            }
                        } }.joinAll()
                    }
                } finally { files.release(prepared) }
            } finally { events.close() }
        }
        // Only this coroutine calls controller callbacks. Network threads only enqueue immutable events.
        for (event in events) {
            currentCoroutineContext().ensureActive()
            when (event) {
                is Event.Progress -> onProgress(event.value)
                is Event.Source -> {
                    val source = event.value
                    check(source.sourceId !in results)
                    results[source.sourceId] = source
                    onSource(source)
                    onProgress(SourceProgress(source.sourceId,
                        if (source.failure == null) SourceStage.COMPLETE else SourceStage.FAILED,
                        source.failure?.userMessage, source.failure?.sendEvidence ?: SendEvidence.RESPONSE_RECEIVED))
                    event.received.complete(Unit)
                }
            }
        }
        producer.join()
        val ordered = selected.map { requireNotNull(results[it]) }
        ParsedInputBatch(ordered, ordered.any { it.failure?.code == AssistantFailureCode.OVERFLOW } ||
            ordered.sumOf { it.evidence.size } > AssistantMaterialPolicy.MAX_CANDIDATES ||
            mergeIdenticalPlaceIntents(ordered.flatMap { it.intents }).size > AssistantMaterialPolicy.MAX_CANDIDATES)
    }

    private suspend fun record(operation: String, attempt: Int, failure: AssistantFailure?,
        repairing: Boolean = false) {
        try {
            diagnostics.record(AssistantProtocolEvent(operation, AssistantProtocolStage.GUIDE_BATCH, null,
                attempt, if (repairing) AssistantProtocolOutcome.REPAIRING
                    else if (failure == null) AssistantProtocolOutcome.SUCCESS else AssistantProtocolOutcome.FAILED,
                failure?.protocolIssue, failure?.code))
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* Diagnostic failure must not cause a duplicate model request. */ }
    }
}
