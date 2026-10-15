package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class SourceStage { SELECTED, PREPARING, SENDING, UNDERSTANDING, COMPLETE, FAILED, CANCELLED }
data class SourceProgress(val sourceId: String, val stage: SourceStage,
    val error: String? = null, val sendEvidence: SendEvidence = SendEvidence.NOT_DISPATCHED)
data class ParsedSource(val sourceId: String, val intents: List<PlaceIntent>, val failure: AssistantFailure? = null,
    val evidence: List<GuideEvidence> = emptyList()) {
    init {
        require(intents.isEmpty() || evidence.isEmpty())
        require(failure == null || intents.isEmpty() && evidence.isEmpty())
    }
}
data class ParsedInputBatch(val sources: List<ParsedSource>, val overflow: Boolean)
data class AssistantInputRequest(val sessionId: String, val text: String, val city: String,
    val images: List<AssistantImage>, val retrySourceIds: Set<String>? = null,
    val travelMaterials: Boolean = false, val cityScope: AssistantCityScope? = null,
    val guideVersion: GuideProtocolVersion = GuideProtocolVersion.LEGACY_V1,
    val guideBinding: GuideBatchBinding? = null)

class AssistantInputProcessor(
    private val textParser: PlaceIntentParser,
    private val imageParser: ImageIntentParser,
    private val images: AssistantImageStore,
    private val normalizer: PlaceIntentNormalizer? = null,
    private val materialTextParser: PlaceIntentParser? = null,
    private val materialImageParser: ImageIntentParser? = null,
    private val guideParser: GuideMaterialParser? = null,
    private val evidenceResolver: GuideEvidenceResolver? = null,
    private val evidenceGuideParser: GuideMaterialParser? = null,
) {
    val supportsNormalization get() = normalizer != null
    val supportsEvidence get() = evidenceResolver != null
    suspend fun resolveEvidence(evidence: List<GuideEvidence>, context: GuideContext,
        frozen: List<GuideFrozenPlace>) = requireNotNull(evidenceResolver).resolve(evidence, context, frozen)
    suspend fun normalize(candidates: List<PlaceIntent>, context: String, city: String): List<PlaceIntent> {
        require(candidates.size <= AssistantMaterialPolicy.MAX_CANDIDATES)
        return requireNotNull(normalizer).normalize(candidates, context, city)
    }
    suspend fun parse(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit = {},
        onProgress: (SourceProgress) -> Unit): ParsedInputBatch {
        val semantic = request.travelMaterials && supportsNormalization
        if (semantic && request.guideVersion == GuideProtocolVersion.EVIDENCE_V2)
            return requireNotNull(evidenceGuideParser ?: guideParser).parse(request, onSource, onProgress)
        if (semantic && guideParser != null) return guideParser.parse(request, onSource, onProgress)
        val candidateLimit = if (semantic) AssistantMaterialPolicy.MAX_PER_SOURCE else AssistantPlacePolicy.MAX_PLACES
        val sources = mutableListOf<ParsedSource>()
        suspend fun parseOne(id: String, operation: suspend () -> List<PlaceIntent>) {
            currentCoroutineContext().ensureActive()
            try {
                val result = operation()
                currentCoroutineContext().ensureActive()
                if (result.size > candidateLimit) throw AssistantFailure("本份素材超过 $candidateLimit 个候选，请分批。", AssistantFailureCode.OVERFLOW)
                val parsed = ParsedSource(id, result)
                sources += parsed
                onSource(parsed)
                onProgress(SourceProgress(id, SourceStage.COMPLETE))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val failure = e as? AssistantFailure ?: AssistantFailure("本项未能处理，请重试。")
                if (semantic && failure.code == AssistantFailureCode.NO_PLACES) {
                    val parsed = ParsedSource(id, emptyList())
                    sources += parsed
                    onSource(parsed)
                    onProgress(SourceProgress(id, SourceStage.COMPLETE))
                    return
                }
                val parsed = ParsedSource(id, emptyList(), failure)
                sources += parsed
                onSource(parsed)
                onProgress(SourceProgress(id, SourceStage.FAILED, failure.userMessage, failure.sendEvidence))
            }
        }
        if (request.text.isNotBlank() && (request.retrySourceIds == null || "text" in request.retrySourceIds)) {
            parseOne("text") {
                onProgress(SourceProgress("text", SourceStage.SENDING, sendEvidence = SendEvidence.POSSIBLY_SENT))
                (if (semantic) materialTextParser ?: textParser else textParser).parseInScope(request.text, request.city, request.cityScope).map {
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
                (if (semantic) materialImageParser ?: imageParser else imageParser).parse(prepared, image.ordinal, request.text, request.city) {
                    onProgress(SourceProgress(image.id, SourceStage.UNDERSTANDING, sendEvidence = SendEvidence.POSSIBLY_SENT))
                }
            }
        }
        return ParsedInputBatch(sources, sources.any { it.failure?.code == AssistantFailureCode.OVERFLOW } ||
            mergeIdenticalPlaceIntents(sources.flatMap { it.intents }).size >
                if (semantic) AssistantMaterialPolicy.MAX_CANDIDATES else AssistantPlacePolicy.MAX_PLACES)
    }
}

/** Only identical names in the same explicit city are combined; different aliases still need POI identity. */
internal fun PlaceIntent.materialKey() = Triple(normalizedPlace(query), city.trim().removeSuffix("市"), needsReview)
/** Local identity only: never model-supplied, retained when the user edits a result. */
internal fun PlaceIntent.materialIdentities(): Set<String> = materialKeys.ifEmpty {
    val key = materialKey()
    setOf("${key.first.length}:${key.first}${key.second.length}:${key.second}:${key.third}")
}
internal fun mergeIdenticalPlaceIntents(intents: List<PlaceIntent>): List<PlaceIntent> =
    intents.groupBy { it.materialKey() }.values.map { group ->
        group.first().copy(sources = group.flatMap { it.sources }.distinct(),
            materialKeys = group.flatMap { it.materialIdentities() }.toSet())
    }
