package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class SourceStage { SELECTED, PREPARING, SENDING, UNDERSTANDING, COMPLETE, FAILED, CANCELLED }
data class SourceProgress(val sourceId: String, val stage: SourceStage,
    val error: String? = null, val sendEvidence: SendEvidence = SendEvidence.NOT_DISPATCHED)
data class ParsedSource(val sourceId: String, val intents: List<PlaceIntent>, val failure: AssistantFailure? = null)
data class ParsedInputBatch(val sources: List<ParsedSource>, val overflow: Boolean)
data class AssistantInputRequest(val sessionId: String, val text: String, val city: String,
    val images: List<AssistantImage>, val retrySourceIds: Set<String>? = null)

class AssistantInputProcessor(
    private val textParser: PlaceIntentParser,
    private val imageParser: ImageIntentParser,
    private val images: AssistantImageStore,
) {
    suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit = {},
        onProgress: (SourceProgress) -> Unit): ParsedInputBatch {
        val sources = mutableListOf<ParsedSource>()
        suspend fun parseOne(id: String, operation: suspend () -> List<PlaceIntent>) {
            currentCoroutineContext().ensureActive()
            try {
                val result = operation()
                currentCoroutineContext().ensureActive()
                if (result.size > 20) throw AssistantFailure("每批最多 20 个地点，请分批。", AssistantFailureCode.OVERFLOW)
                val parsed = ParsedSource(id, result)
                sources += parsed
                onSource(parsed)
                onProgress(SourceProgress(id, SourceStage.COMPLETE))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val failure = e as? AssistantFailure ?: AssistantFailure("本项未能处理，请重试。")
                val parsed = ParsedSource(id, emptyList(), failure)
                sources += parsed
                onSource(parsed)
                onProgress(SourceProgress(id, SourceStage.FAILED, failure.userMessage, failure.sendEvidence))
            }
        }
        if (request.text.isNotBlank() && (request.retrySourceIds == null || "text" in request.retrySourceIds)) {
            parseOne("text") {
                onProgress(SourceProgress("text", SourceStage.SENDING, sendEvidence = SendEvidence.POSSIBLY_SENT))
                textParser.parse(request.text, request.city).map {
                    it.copy(sources = listOf(IntakeSource.Text(it.sourceSpan)))
                }
            }
        }
        for (image in request.images.filter { request.retrySourceIds == null || it.id in request.retrySourceIds }) {
            if (sources.any { it.failure?.code == AssistantFailureCode.UNSUPPORTED_IMAGE }) {
                val failure = AssistantFailure("前一张已被服务拒绝，此图尚未发送；请检查图片能力。",
                    AssistantFailureCode.UNSUPPORTED_IMAGE, SendEvidence.NOT_DISPATCHED)
                val skipped = ParsedSource(image.id, emptyList(), failure)
                sources += skipped
                onSource(skipped)
                onProgress(SourceProgress(image.id, SourceStage.FAILED, failure.userMessage, failure.sendEvidence))
                continue
            }
            parseOne(image.id) {
                onProgress(SourceProgress(image.id, SourceStage.PREPARING))
                val prepared = images.prepare(request.sessionId, image.id)
                currentCoroutineContext().ensureActive()
                onProgress(SourceProgress(image.id, SourceStage.SENDING, sendEvidence = SendEvidence.POSSIBLY_SENT))
                imageParser.parse(prepared, image.ordinal, request.text, request.city) {
                    onProgress(SourceProgress(image.id, SourceStage.UNDERSTANDING, sendEvidence = SendEvidence.POSSIBLY_SENT))
                }
            }
        }
        return ParsedInputBatch(sources, sources.any { it.failure?.code == AssistantFailureCode.OVERFLOW } ||
            sources.sumOf { it.intents.size } > 20)
    }
}
