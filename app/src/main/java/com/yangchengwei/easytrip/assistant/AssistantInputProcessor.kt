package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal fun ParsedSource.completionStage(): SourceStage = when {
    failure == null -> SourceStage.COMPLETE
    sourceId != "text" && failure.code != AssistantFailureCode.AUTH -> SourceStage.SKIPPED
    else -> SourceStage.FAILED
}

enum class SourceStage { SELECTED, PREPARING, SENDING, UNDERSTANDING, COMPLETE, SKIPPED, FAILED, CANCELLED }
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
    val guideBinding: GuideBatchBinding? = null,
    val imagePlan: OptionalImagePlan = OptionalImagePlan(),
    val prepareOptionalImages: (suspend () -> OptionalImagePlan)? = null,
    val imageAllowed: Boolean = true,
    val onImagePlan: (OptionalImagePlan) -> Unit = {})

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
        val sources = linkedMapOf<String, ParsedSource>()
        fun publish(value: ParsedSource) { sources[value.sourceId] = value; onSource(value) }
        var overflow = false
        // A text-only request is completed before any optional file is read or downloaded.
        if (request.text.isNotBlank() && (request.retrySourceIds == null || "text" in request.retrySourceIds)) {
            val textBatch = parseCore(request.copy(images = emptyList(), retrySourceIds = setOf("text")), ::publish, onProgress)
            overflow = textBatch.overflow
        }
        var plan = request.imagePlan
        val requestedIds = (plan.assets.map { it.id } + request.images.map { it.id }).distinct()
            .filter { request.retrySourceIds == null || it in request.retrySourceIds }.toSet()
        if (requestedIds.isEmpty()) return ParsedInputBatch(sources.values.toList(), overflow)
        val sends = mutableMapOf<String, SendEvidence>()
        fun progress(value: SourceProgress) {
            sends[value.sourceId] = value.sendEvidence
            onProgress(value)
        }
        fun skipped(id: String, reason: OptionalImageSkip) {
            val failure = AssistantFailure("图片已跳过（可选），可继续使用其他素材。",
                sendEvidence = sends[id] ?: SendEvidence.NOT_DISPATCHED)
            publish(ParsedSource(id, emptyList(), failure))
            onProgress(SourceProgress(id, SourceStage.SKIPPED, failure.userMessage, failure.sendEvidence))
            plan.assets.find { it.id == id }?.let { plan = plan.updated(it.skipped(reason)) }
        }
        val auth = sources.values.firstOrNull { it.failure?.code == AssistantFailureCode.AUTH }?.failure
        if (auth != null) {
            requestedIds.forEach { id ->
                val failure = AssistantFailure("鉴权失败，此图尚未发送。", AssistantFailureCode.AUTH, SendEvidence.NOT_DISPATCHED)
                publish(ParsedSource(id, emptyList(), failure))
                onProgress(SourceProgress(id, SourceStage.FAILED, failure.userMessage))
            }
            return ParsedInputBatch(sources.values.toList(), overflow)
        }
        if (!request.imageAllowed) requestedIds.forEach { skipped(it, OptionalImageSkip.UNSUPPORTED) }
        else {
            val completed = withTimeoutOrNull(180_000) {
                plan = request.prepareOptionalImages?.invoke() ?: plan
                // Local selections also keep explicit prepared/failed state across retries and recovery.
                plan = plan.copy(assets = plan.assets.map { asset ->
                    if (asset.id !in requestedIds || asset.remoteUrl != null ||
                        asset.status == OptionalImageStatus.SKIPPED && asset.id !in request.retrySourceIds.orEmpty()) return@map asset
                    try {
                        val prepared = images.prepare(request.sessionId, asset.id)
                        require(prepared.id == asset.id)
                        asset.copy(status = OptionalImageStatus.AVAILABLE, preparedSha256 = preparedImageDigest(prepared.bytes), skip = null)
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { asset.skipped(OptionalImageSkip.DECODE) }
                })
                request.onImagePlan(plan)
                val plannedIds = plan.assets.map { it.id }.toSet()
                val targets = (plan.available + request.images.filterNot { it.id in plannedIds })
                    .distinctBy { it.id }.filter { it.id in requestedIds }
                (requestedIds - targets.map { it.id }.toSet()).forEach { id ->
                    skipped(id, plan.assets.find { it.id == id }?.skip ?: OptionalImageSkip.MISSING)
                }
                if (targets.isNotEmpty()) {
                    parseCore(request.copy(images = targets, retrySourceIds = targets.map { it.id }.toSet()),
                        onSource = { source ->
                            publish(source)
                            if (source.failure != null && source.failure.code != AssistantFailureCode.AUTH)
                                plan.assets.find { it.id == source.sourceId }?.let {
                                    plan = plan.updated(it.skipped(OptionalImageSkip.MODEL))
                                }
                        }, onProgress = { value ->
                            progress(if (value.stage == SourceStage.FAILED &&
                                sources[value.sourceId]?.failure?.code != AssistantFailureCode.AUTH)
                                value.copy(stage = SourceStage.SKIPPED) else value)
                        })
                    // A rejected optional source is not accepted batch overflow.
                    // The aggregate bound below still counts every successful source.
                }
                true
            }
            currentCoroutineContext().ensureActive()
            if (completed == null) requestedIds.filterNot { it in sources }.forEach { skipped(it, OptionalImageSkip.TIMEOUT) }
        }
        request.onImagePlan(plan)
        return ParsedInputBatch(sources.values.toList(), overflow ||
            sources.values.sumOf { it.evidence.size } > AssistantMaterialPolicy.MAX_CANDIDATES ||
            mergeIdenticalPlaceIntents(sources.values.flatMap { it.intents }).size >
                if (request.travelMaterials && supportsNormalization) AssistantMaterialPolicy.MAX_CANDIDATES else AssistantPlacePolicy.MAX_PLACES)
    }
    private suspend fun parseCore(request: AssistantInputRequest, onSource: (ParsedSource) -> Unit,
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
            val rejection = sources.firstNotNullOfOrNull { it.failure?.takeIf { error ->
                error.code in setOf(AssistantFailureCode.AUTH, AssistantFailureCode.UNSUPPORTED_IMAGE) } }
            if (rejection != null) {
                val failure = AssistantFailure("前一张已被服务拒绝，此图尚未发送；请检查设置。",
                    rejection.code, SendEvidence.NOT_DISPATCHED)
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
