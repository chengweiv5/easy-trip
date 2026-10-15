package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.amap.AmapServiceException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** One controller per trip back-stack entry. Only a user save action may write places. */
class PlaceAssistantController(
    private val tripId: String?,
    private val scope: CoroutineScope,
    private val parser: PlaceIntentParser,
    source: PlaceSearchDataSource?,
    private val importer: PlaceImport,
    private var resolveDefaultCity: suspend () -> String? = { null },
    private val processor: (() -> AssistantInputProcessor)? = null,
    private val sessionId: String = "",
    private val imageAllowed: () -> Boolean = { true },
    private val diagnostics: AssistantSearchDiagnostics = AssistantSearchDiagnostics.NONE,
    private val searchPacer: AssistantSearchPacer = AssistantSearchPacer(),
    private val onBatchSaved: (List<String>) -> Unit = {},
    private val linkLoader: AssistantLinkLoader? = null,
    val target: AssistantTarget = tripId?.let(AssistantTarget::ExistingTrip) ?: AssistantTarget.NewTripDraft,
) {
    private val diagnosticSession = UUID.randomUUID().toString()
    private val diagnosticSalt = UUID.randomUUID().toString()
    private val mutable = MutableStateFlow(PlaceAssistantState())
    val state = mutable.asStateFlow()
    private var source = source
    private var run = 0L
    private var work: Job? = null
    private var savedIds: Set<String> = emptySet()
    private var pendingSave: ImportConfirmation? = null
    private var active = true
    private var cityRevision = 0L
    private var cityEdited = false
    private var parsedSources = emptyList<ParsedSource>()
    private var inputRevision = 0L
    private var currentParsed = emptyList<ParsedSource>()
    private var loadedLink: LoadedAssistantLink? = null
    private var semanticBatch = false
    private var parsedCity: String? = null
    private var evidenceSession: GuideEvidenceSession? = null
    private var restoredLegacyGuide = false
    var guideCityScope: AssistantCityScope? = null
        private set
    private var guideOperation: String? = null
    fun ownsGuideBatch(operation: String) = guideOperation == operation
    fun setGuideCityScope(value: AssistantCityScope?) {
        if (value == guideCityScope) return
        cancel()
        inputRevision++
        cityRevision++
        guideCityScope = value
        evidenceSession = evidenceSession?.checkpoint?.let { prior ->
            GuideEvidenceSession(prior.copy(
                binding = prior.binding.copy(revision = prior.binding.revision + 1,
                    cityScopeKey = value?.encode() ?: mutable.value.city),
                sources = emptyList(), resolution = GuideResolution(emptyList(), emptyList(), emptySet())))
        }
        parsedSources = emptyList(); parsedCity = null
        mutable.value = mutable.value.copy(resolvingCity = false, parsedMaterials = emptyList(),
            parsedMaterialCity = null, normalizationPending = false, guideCheckpoint = evidenceSession?.checkpoint)
        if (!cityEdited && !mutable.value.busy && mutable.value.items.isEmpty())
            mutable.value = mutable.value.copy(city = value?.defaultCity.orEmpty())
    }
    fun acceptGuideInput(operation: String, text: String, images: List<AssistantImage>, loaded: LoadedAssistantLink?, cities: AssistantCityScope): Boolean {
        if (guideOperation == operation) return false
        if (mutable.value.busy || mutable.value.saving || mutable.value.hasDraft) return false
        guideOperation = operation
        setGuideCityScope(cities)
        loadedLink = loaded
        mutable.value = mutable.value.copy(input = text, images = images, error = null, receipt = null)
        return true
    }
    fun restoreGuideState(value: PlaceAssistantState, operation: String, cities: AssistantCityScope?,
        loaded: LoadedAssistantLink? = null) {
        if (mutable.value.hasDraft || mutable.value.busy || mutable.value.saving) return
        setGuideCityScope(cities); guideOperation = operation
        loadedLink = loaded
        parsedSources = value.parsedMaterials
        parsedCity = value.parsedMaterialCity
        evidenceSession = value.guideCheckpoint?.let(::GuideEvidenceSession)
        restoredLegacyGuide = value.guideVersion == GuideProtocolVersion.LEGACY_V1
        // A recovered batch keeps its saved city, including a user's override.
        cityEdited = value.city.isNotBlank()
        semanticBatch = loaded != null
        val old = mutable.value
        mutable.value = value.copy(configured = old.configured, consent = old.consent, mapReady = old.mapReady,
            busy = false, saving = false, resolvingCity = false)
        reconcile()
    }
    internal fun loadedGuideMaterial(): LoadedAssistantLink? = loadedLink
    private fun forgetLink(removeImages: Boolean = false) {
        evidenceSession = null
        restoredLegacyGuide = false
        mutable.value = mutable.value.copy(guideCheckpoint = null, guideVersion = GuideProtocolVersion.LEGACY_V1)
        semanticBatch = false
        parsedCity = null
        val old = loadedLink ?: return
        loadedLink = null
        if (removeImages) {
            val ids = old.images.map { it.id }.toSet()
            mutable.value = mutable.value.copy(images = mutable.value.images.filterNot { it.id in ids })
            scope.launch { withContext(NonCancellable) { linkLoader?.discard(sessionId, old.images) } }
        }
    }
    val draftRevision get() = inputRevision
    fun attachmentSelection(busy: Boolean) { mutable.value = mutable.value.copy(attachmentSelectionBusy = busy) }
    fun reportError(message: String) { fail(message) }
    fun addImages(images: List<AssistantImage>) {
        val s = mutable.value
        if (!active || s.busy || s.saving || s.items.isNotEmpty()) return
        if (s.images.size + images.size > AssistantImagePolicy.MAX_IMAGES || (s.images + images).sumOf { it.byteCount } > AssistantImagePolicy.MAX_TOTAL_BYTES) {
            fail("图片超过本批数量或大小上限。"); return
        }
        parsedSources = emptyList()
        currentParsed = emptyList()
        parsedCity = null
        evidenceSession = null
        inputRevision++
        mutable.value = s.copy(images = s.images + images, sourceProgress = emptyList(), error = null,
            parsedMaterials = emptyList(), parsedMaterialCity = null, normalizationPending = false, guideCheckpoint = null)
    }
    fun removeImage(id: String) {
        if (mutable.value.busy || mutable.value.saving || mutable.value.items.isNotEmpty()) return
        parsedSources = emptyList()
        parsedCity = null
        evidenceSession = null
        inputRevision++
        mutable.value = mutable.value.copy(images = mutable.value.images.filterNot { it.id == id }, sourceProgress = emptyList(),
            parsedMaterials = emptyList(), parsedMaterialCity = null, normalizationPending = false, guideCheckpoint = null)
    }

    fun environment(configured: Boolean, consent: Boolean, mapReady: Boolean) {
        val old = mutable.value
        if (old.consent && !consent) {
            cancel()
            mutable.value = mutable.value.copy(items = emptyList(), selected = emptySet(),
                error = "地图授权已撤回，请重新授权后查询。")
        }
        if (old.configured && !configured) cancel()
        mutable.value = mutable.value.copy(configured = configured, consent = consent, mapReady = mapReady)
    }
    fun updateSource(value: PlaceSearchDataSource?) { source = value }
    fun updateCityResolver(value: suspend () -> String?) { resolveDefaultCity = value }
    /** Refresh automatic context on opening/context changes, but never replace a user override or a batch. */
    suspend fun prepareDefaultCity() {
        if (target == AssistantTarget.NewTripDraft) return
        val s = mutable.value
        if (!active || !s.consent || s.busy || s.saving || s.resolvingCity ||
            cityEdited || s.items.isNotEmpty()) return
        val revision = ++cityRevision
        mutable.value = s.copy(resolvingCity = true)
        try {
            val city = lookupDefaultCity()
            currentCoroutineContext().ensureActive()
            if (revision == cityRevision && active && mutable.value.consent && !cityEdited) {
                mutable.value = mutable.value.copy(city = city)
            }
        } finally {
            if (revision == cityRevision) mutable.value = mutable.value.copy(resolvingCity = false)
        }
    }

    private suspend fun lookupDefaultCity(): String = guideCityScope?.let { it.defaultCity.orEmpty() } ?: try {
        withTimeoutOrNull(12_000) { resolveDefaultCity() }?.trim().orEmpty().take(50)
            .takeUnless { it.any(Char::isISOControl) }.orEmpty()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        ""
    }
    fun configurationChanged() { cancel(); inputRevision++ }
    fun invalidateTrip() {
        cancel(); inputRevision++; active = false
        parsedSources = emptyList()
        currentParsed = emptyList()
        forgetLink()
        mutable.value = mutable.value.copy(items = emptyList(), selected = emptySet(),
            parsedMaterials = emptyList(), parsedMaterialCity = null, normalizationPending = false,
            error = "目标旅行已失效，请返回旅行列表。")
    }
    fun updateSaved(ids: Set<String>) { savedIds = ids; reconcile() }
    fun editInput(value: String) {
        if (mutable.value.saving || mutable.value.busy || mutable.value.items.isNotEmpty()) return
        if (value.length > 2000) { fail("文字最多 2000 字，请分批输入；原输入保留。"); return }
        if (value != mutable.value.input) forgetLink(removeImages = true)
        parsedSources = emptyList()
        parsedCity = null
        mutable.value = mutable.value.copy(input = value, sourceProgress = emptyList(), error = null,
            parsedMaterials = emptyList(), parsedMaterialCity = null, normalizationPending = false)
    }
    fun editCity(value: String) {
        if (mutable.value.saving) return
        cityEdited = true
        cityRevision++
        mutable.value = mutable.value.copy(resolvingCity = false)
        val next = value.take(50)
        if (next == mutable.value.city) return
        cancel()
        inputRevision++
        val affected = mutable.value.items.filter { it.intent.usesDefaultCity || it.intent.city.isBlank() }.map { it.id }.toSet()
        mutable.value = mutable.value.copy(city = next,
            selected = mutable.value.selected - affected,
            items = mutable.value.items.map {
                if (it.id in affected) it.copy(intent = it.intent.copy(city = next, usesDefaultCity = true),
                    generation = it.generation + 1, status = IntakeStatus.CANCELLED, candidates = emptyList(), poi = null)
                else it
            })
    }
    fun clear() {
        if (mutable.value.saving) return
        cancel()
        inputRevision++
        parsedSources = emptyList()
        forgetLink()
        pendingSave = null
        guideOperation = null
        val old = mutable.value
        mutable.value = PlaceAssistantState(city = old.city, configured = old.configured,
            consent = old.consent, mapReady = old.mapReady)
    }
    fun supplyMissingCity(value: String) {
        val city = value.trim()
        val s = mutable.value
        if (s.busy || s.saving || city.isBlank() || city.length > 50 || city.any(Char::isISOControl) || !canQuery()) return
        val missing = s.items.filter { it.status == IntakeStatus.NEED_CITY && it.intent.city.isBlank() }.map { it.id }.toSet()
        if (missing.isEmpty()) return
        if (s.city.isBlank()) {
            cityEdited = true
            cityRevision++
        }
        mutable.value = s.copy(city = s.city.ifBlank { city },
            items = s.items.map {
                if (it.id in missing) it.copy(intent = it.intent.copy(city = city, usesDefaultCity = true),
                    generation = it.generation + 1, status = IntakeStatus.CANCELLED, detail = null, cityConflict = false)
                else it
            })
        retry(missing)
    }
    fun dismissError() { mutable.value = mutable.value.copy(error = null) }
    suspend fun recoverReceipt() {
        if (target == AssistantTarget.NewTripDraft) return
        val receipt = importer.latestReceipt(requireNotNull(tripId))
        if (!mutable.value.hasDraft && !mutable.value.saving && receipt != null) {
            mutable.value = mutable.value.copy(receipt = receipt)
        }
    }
    fun dismissReceipt() { mutable.value = mutable.value.copy(receipt = null) }

    fun submit() {
        val s = mutable.value
        if (!canQuery() || s.busy || s.resolvingCity || s.attachmentSelectionBusy || s.items.isNotEmpty()) return
        if (s.input.isBlank() && s.images.isEmpty()) { fail("请先输入地点文字或选择图片。"); return }
        val link = try { AssistantLinkInput.parse(s.input) }
        catch (error: AssistantFailure) { fail(error.userMessage); return }
        if (link != null) { processImages(null, link); return }
        if (s.images.isNotEmpty()) { processImages(null); return }
        val input = s.input
        val generation = ++run
        mutable.value = mutable.value.copy(busy = true, error = null, receipt = null)
        work = scope.launch {
            try {
                withTimeout(AssistantPlacePolicy.BATCH_TIMEOUT_MILLIS) {
                    val city = resolveSearchCity(s.city)
                    if (!isCurrent(generation)) return@withTimeout
                    mutable.value = mutable.value.copy(city = city)
                    val intents = parser.parseInScope(input, city, guideCityScope)
                    if (!isCurrent(generation)) return@withTimeout
                    if (intents.size > AssistantPlacePolicy.MAX_PLACES) throw AssistantFailure(
                        "每批最多 ${AssistantPlacePolicy.MAX_PLACES} 个地点，请分批；原输入保留，没有截断或查询。",
                        AssistantFailureCode.OVERFLOW, SendEvidence.RESPONSE_RECEIVED)
                    require(intents.isNotEmpty())
                    val items = intents.mapIndexed { index, intent ->
                        val default = intent.city.isBlank() || (city.isNotBlank() &&
                            sameCity(intent.city, city) && !input.contains(intent.city.removeSuffix("市")))
                        val conflict = city.isNotBlank() && intent.city.isNotBlank() && !sameCity(intent.city, city) &&
                            !intent.hasExplicitCityIn(input)
                        IntakeItem(UUID.randomUUID().toString(), AssistantPlacePolicy.label(index),
                            intent.copy(city = intent.city.ifBlank { city }, usesDefaultCity = default),
                            cityConflict = conflict)
                    }
                    mutable.value = mutable.value.copy(items = items)
                    queryOrConfirm(items.map { it.id }.toSet(), generation)
                }
            } catch (_: TimeoutCancellationException) { if (isCurrent(generation)) stopPending("整批查询已超时，已完成结果保留。") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (isCurrent(generation)) fail(e.safeMessage()) }
            finally { if (isCurrent(generation)) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun retryImages(ids: Set<String> = mutable.value.sourceProgress.filter {
        it.stage == SourceStage.FAILED || it.stage == SourceStage.CANCELLED
    }.map { it.sourceId }.toSet()) {
        if (!canQuery() || mutable.value.busy || mutable.value.attachmentSelectionBusy || ids.isEmpty()) return
        val retryable = mutable.value.sourceProgress.filter { it.stage in setOf(SourceStage.FAILED, SourceStage.CANCELLED) }
            .map { it.sourceId }.toSet()
        val allowed = ids.intersect(retryable)
        if (allowed.isNotEmpty()) processImages(allowed)
    }
    fun retryNormalization() {
        val s = mutable.value
        if (!s.normalizationPending || !canQuery() || s.busy || s.attachmentSelectionBusy) return
        processImages(emptySet())
    }
    private fun processImages(retryIds: Set<String>?, link: AssistantLinkInput? = null) {
        val s = mutable.value
        if (s.images.isNotEmpty() && !imageAllowed()) { fail("当前模型的图片能力检查未通过，图片尚未发送。请在助手设置重新检查，素材已保留。"); return }
        val runner = processor?.invoke() ?: run { fail("图片处理暂不可用，请重新打开助手。"); return }
        if (s.guideVersion == GuideProtocolVersion.EVIDENCE_V2 && !runner.supportsEvidence) {
            fail("当前模型尚未核验此攻略整理能力，已保留原文证据；请切换回支持的模型后重试。")
            return
        }
        val generation = ++run
        val semantic = (link != null || loadedLink != null) && runner.supportsNormalization
        semanticBatch = semantic
        val useEvidence = semantic && runner.supportsEvidence && !restoredLegacyGuide
        if (useEvidence && evidenceSession == null) {
            evidenceSession = GuideEvidenceSession(GuideEvidenceCheckpoint(
                GuideBatchBinding(UUID.randomUUID().toString(), inputRevision, guideCityScope?.encode() ?: s.city),
                emptyList(), GuideResolution(emptyList(), emptyList(), emptySet()), emptyList()))
            mutable.value = mutable.value.copy(guideVersion = GuideProtocolVersion.EVIDENCE_V2,
                guideCheckpoint = evidenceSession?.checkpoint)
        }
        currentParsed = emptyList()
        mutable.value = mutable.value.copy(busy = true, error = null, receipt = null)
        work = scope.launch {
            try {
                val imageCount = if (link != null) AssistantImagePolicy.MAX_IMAGES else s.images.size
                withTimeout(if (useEvidence) GuideEvidenceBudget.batchTimeoutMillis(imageCount)
                    else AssistantImagePolicy.batchTimeoutMillis(imageCount)) {
                    if (link != null && loadedLink == null) {
                        val loader = linkLoader ?: throw AssistantFailure("链接读取暂不可用，请重新打开助手。")
                        val loaded = loader.load(sessionId, link, s.images) { progress ->
                            if (isCurrent(generation)) mutable.value = mutable.value.copy(linkProgress = progress)
                        }
                        if (!isCurrent(generation)) {
                            withContext(NonCancellable) { loader.discard(sessionId, loaded.images) }
                            return@withTimeout
                        }
                        loadedLink = loaded
                        mutable.value = mutable.value.copy(images = s.images + loaded.images, linkProgress = null)
                    }
                    val material = mutable.value
                    if (material.images.isNotEmpty() && !imageAllowed())
                        throw AssistantFailure("当前模型不支持图片，素材已保留且尚未发送，请在助手设置检查图片能力。")
                    val city = if (retryIds != null && !semantic) s.city.trim() else resolveSearchCity(s.city.trim())
                    if (!isCurrent(generation)) return@withTimeout
                    val cityChanged = semantic && parsedCity != null && parsedCity != city
                    if (useEvidence && cityChanged) {
                        val prior = requireNotNull(evidenceSession).checkpoint
                        evidenceSession = GuideEvidenceSession(prior.copy(
                            binding = prior.binding.copy(revision = prior.binding.revision + 1,
                                cityScopeKey = guideCityScope?.encode() ?: city), sources = emptyList(),
                            resolution = GuideResolution(emptyList(), emptyList(), emptySet())))
                    }
                    // Extraction binds missing cities to its default. Reuse it only in the same city context.
                    val effectiveRetryIds = if (cityChanged) null
                    else if (semantic && loadedLink != null && parsedSources.isNotEmpty() && retryIds == null) {
                        val completed = parsedSources.filter { it.failure == null }.map { it.sourceId }.toSet()
                        ((if (loadedLink!!.modelText.isBlank()) emptyList() else listOf("text")) +
                            material.images.map { it.id }).filterNot { it in completed }.toSet()
                    } else retryIds
                    if (effectiveRetryIds == null) parsedSources = emptyList()
                    if (semantic) parsedCity = city
                    val input = AssistantInputRequest(sessionId, loadedLink?.modelText ?: s.input,
                        city, material.images, effectiveRetryIds, travelMaterials = semantic, cityScope = guideCityScope,
                        guideVersion = if (useEvidence) GuideProtocolVersion.EVIDENCE_V2 else GuideProtocolVersion.LEGACY_V1,
                        guideBinding = evidenceSession?.checkpoint?.binding)
                    val sourceIds = (if (input.text.isBlank()) emptyList() else listOf("text")) + input.images.map { it.id }
                    mutable.value = mutable.value.copy(linkProgress = null, sourceProgress = sourceIds.map { id ->
                        if (effectiveRetryIds == null || id in effectiveRetryIds) SourceProgress(id, SourceStage.SELECTED)
                        else s.sourceProgress.find { it.sourceId == id } ?: SourceProgress(id, SourceStage.COMPLETE)
                    })
                    val request = input
                    if (!isCurrent(generation)) return@withTimeout
                    mutable.value = mutable.value.copy(city = request.city)
                    val batch = runner.parse(request, onSource = { parsed ->
                        if (isCurrent(generation)) {
                            currentParsed = currentParsed + parsed
                            if (useEvidence) {
                                evidenceSession?.accept(parsed)
                                mutable.value = mutable.value.copy(guideCheckpoint = evidenceSession?.checkpoint,
                                    normalizationPending = evidenceSession?.checkpoint?.sources?.any { it.evidence.isNotEmpty() } == true)
                            }
                        }
                    }) { progress ->
                        // OkHttp's body callback is not on the controller dispatcher.
                        scope.launch {
                            if (isCurrent(generation)) {
                                val old = mutable.value.sourceProgress.find { it.sourceId == progress.sourceId }
                                if (old?.stage !in setOf(SourceStage.COMPLETE, SourceStage.FAILED, SourceStage.CANCELLED))
                                    mutable.value = mutable.value.copy(sourceProgress = mutable.value.sourceProgress.map {
                                        if (it.sourceId == progress.sourceId) progress else it
                                    })
                            }
                        }
                    }
                    if (!isCurrent(generation)) return@withTimeout
                    val merged = (parsedSources.filterNot { old -> batch.sources.any { it.sourceId == old.sourceId } } +
                        batch.sources).sortedBy { sourceIds.indexOf(it.sourceId).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
                    val raw = mergeIdenticalPlaceIntents(merged.flatMap { it.intents })
                    if (useEvidence && (batch.overflow || merged.sumOf { it.evidence.size } > AssistantMaterialPolicy.MAX_CANDIDATES))
                        throw AssistantFailure("本批证据超过400份，请分批；已有结果保留。",
                            AssistantFailureCode.OVERFLOW)
                    if (raw.size > if (semantic) AssistantMaterialPolicy.MAX_CANDIDATES else AssistantPlacePolicy.MAX_PLACES) {
                        fail("每批最多 ${AssistantPlacePolicy.MAX_PLACES} 个地点，请保留素材重新分批；没有查询或截断新增内容。")
                        mutable.value = mutable.value.copy(sourceProgress = mutable.value.sourceProgress.map {
                            if (effectiveRetryIds == null || it.sourceId in effectiveRetryIds) it.copy(stage = SourceStage.FAILED, error = "候选数量超限，请分批") else it
                        })
                        return@withTimeout
                    }
                    parsedSources = merged
                    currentParsed = emptyList()
                    mutable.value = mutable.value.copy(parsedMaterials = parsedSources, parsedMaterialCity = parsedCity,
                        sourceProgress = mutable.value.sourceProgress.map { progress ->
                        batch.sources.find { it.sourceId == progress.sourceId }?.let { parsed ->
                            progress.copy(stage = if (parsed.failure == null) SourceStage.COMPLETE else SourceStage.FAILED,
                                error = parsed.failure?.userMessage, sendEvidence = parsed.failure?.sendEvidence ?: SendEvidence.RESPONSE_RECEIVED)
                        } ?: progress
                    })
                    val normalized = if (useEvidence) {
                        val session = requireNotNull(evidenceSession)
                        mutable.value.items.filter { it.poi != null || it.generation > 0 ||
                            it.status == IntakeStatus.ALREADY_SAVED }.forEach {
                            session.freeze(it, it.status == IntakeStatus.ALREADY_SAVED)
                        }
                        mutable.value = mutable.value.copy(normalizationPending = merged.any { it.evidence.isNotEmpty() },
                            linkProgress = "正在整理地点与原文依据…", guideCheckpoint = session.checkpoint)
                        val resolution = runner.resolveEvidence(merged.flatMap { it.evidence },
                            GuideContext(request.text, request.city, guideCityScope), session.checkpoint.frozen)
                        if (!isCurrent(generation)) return@withTimeout
                        val applied = session.apply(resolution, mutable.value.items)
                        mutable.value = mutable.value.copy(items = mutable.value.items.map { item ->
                            applied.absorbedMaterialKeys[item.id]?.let {
                                item.copy(intent = item.intent.copy(materialKeys = it))
                            } ?: item
                        }, guideCheckpoint = session.checkpoint,
                            normalizationPending = session.checkpoint.resolution.pending.isNotEmpty(), linkProgress = null)
                        applied.additions
                    } else if (semantic) {
                        mutable.value = mutable.value.copy(normalizationPending = raw.isNotEmpty(),
                            linkProgress = "正在整理独立景点、合并同一地点…")
                        val result = if (raw.isEmpty()) emptyList() else runner.normalize(raw, request.text, request.city)
                        if (!isCurrent(generation)) return@withTimeout
                        if (result.size > AssistantPlacePolicy.MAX_PLACES) {
                            fail("语义去重后仍有 ${result.size} 个地点，每批最多 ${AssistantPlacePolicy.MAX_PLACES} 个；没有截断或查询，请分批。")
                            return@withTimeout
                        }
                        mutable.value = mutable.value.copy(linkProgress = null)
                        result
                    } else batch.sources.filter { it.failure == null }.flatMap { it.intents }
                    // v2 has already resolved identities, including homonymous entrances with different parents.
                    val fresh = if (useEvidence) normalized else absorbRepeatedMaterials(normalized)
                    if (!useEvidence) mutable.value = mutable.value.copy(normalizationPending = false)
                    val items = fresh.mapIndexed { index, intent ->
                        val city = request.city
                        IntakeItem(UUID.randomUUID().toString(), AssistantPlacePolicy.label(s.items.size + index),
                            intent.copy(city = intent.city.ifBlank { city }, usesDefaultCity = intent.city.isBlank() ||
                                (sameCity(intent.city, city) && !intent.sourceSpan.contains(intent.city.removeSuffix("市")))),
                            cityConflict = city.isNotBlank() && intent.city.isNotBlank() && !sameCity(intent.city, city) &&
                                !intent.hasExplicitCityIn(intent.sourceSpan) && !intent.hasExplicitCityIn(request.text))
                    }
                    mutable.value = mutable.value.copy(items = mutable.value.items + items)
                    if (mutable.value.items.isEmpty()) fail(merged.firstOrNull { it.failure != null }?.failure?.userMessage
                        ?: "未识别到明确地点，请换清晰图片或补充文字；素材已保留。")
                    queryOrConfirm(items.map { it.id }.toSet(), generation)
                }
            } catch (_: TimeoutCancellationException) {
                if (isCurrent(generation)) { cancel(); fail("整批处理超时，已完成结果保留；已发送内容无法撤回。") }
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (isCurrent(generation)) fail(e.safeMessage()) }
            finally {
                if (isCurrent(generation)) {
                    stopPending(null)
                    mutable.value = mutable.value.copy(busy = false, linkProgress = null, parsedMaterials = parsedSources,
                        parsedMaterialCity = parsedCity,
                        sourceProgress = mutable.value.sourceProgress.map {
                        if (it.stage in setOf(SourceStage.SELECTED, SourceStage.PREPARING, SourceStage.SENDING, SourceStage.UNDERSTANDING))
                            it.copy(stage = SourceStage.CANCELLED) else it
                    })
                }
            }
        }
    }
    private suspend fun resolveSearchCity(value: String): String =
        if (target == AssistantTarget.NewTripDraft) value.trim().take(50)
        else (value.trim().takeIf { cityEdited && it.isNotBlank() } ?: lookupDefaultCity()).take(50)

    private suspend fun queryOrConfirm(ids: Set<String>, generation: Long) {
        if (target != AssistantTarget.NewTripDraft) { query(ids, generation); return }
        mutable.value = mutable.value.copy(awaitingCityConfirmation = true, items = mutable.value.items.map {
            if (it.id !in ids) it else it.copy(status = if (it.intent.city.isBlank()) IntakeStatus.NEED_CITY else IntakeStatus.CANCELLED,
                cityConflict = false, detail = if (it.intent.city.isBlank()) "请补充搜索城市。" else "请核对城市后查询地图。")
        })
    }

    fun confirmCities() {
        val s = mutable.value
        if (target != AssistantTarget.NewTripDraft || !s.awaitingCityConfirmation || s.busy || !canQuery()) return
        mutable.value = s.copy(awaitingCityConfirmation = false)
        retry(s.items.filter { it.intent.city.isNotBlank() }.map { it.id }.toSet())
    }

    fun selectedImportItems(requireMapReady: Boolean = true): List<ImportItem> {
        val s = mutable.value
        if (s.busy || s.saving || !active || !s.consent || (requireMapReady && !s.mapReady) || s.selected.isEmpty()) return emptyList()
        val selected = s.eligible.filter { it.id in s.selected }
        if (selected.size != s.selected.size) return emptyList()
        return selected.map { ImportItem(it.id, it.generation, requireNotNull(it.poi), it.category) }
    }

    fun setExternalSaving(saving: Boolean) {
        if (target == AssistantTarget.NewTripDraft) mutable.value = mutable.value.copy(saving = saving)
    }

    /** Only the locally validated, never-submitted journal may restore a confirmed selection. */
    fun restoreCreationSelection(items: List<ImportItem>) {
        val s = mutable.value
        if (target != AssistantTarget.NewTripDraft || s.busy || s.saving || s.items.isNotEmpty() ||
            items.isEmpty() || items.size > AssistantPlacePolicy.MAX_PLACES) return
        require(items.all { it.poi.validPosition() })
        mutable.value = s.copy(items = items.mapIndexed { index, item ->
            IntakeItem(item.itemId, AssistantPlacePolicy.label(index),
                PlaceIntent(item.poi.name, item.poi.cityName.orEmpty(), item.poi.name),
                generation = item.generation, status = IntakeStatus.READY,
                candidates = listOf(item.poi), poi = item.poi, category = item.category)
        }, selected = items.map { it.itemId }.toSet())
    }

    private fun absorbRepeatedMaterials(intents: List<PlaceIntent>): List<PlaceIntent> {
        val unique = mergeIdenticalPlaceIntents(intents)
        val represented = mutable.value.items.flatMap { it.intent.materialIdentities() }.toSet()
        val fresh = unique.filter { it.materialIdentities().intersect(represented).isEmpty() }
        if (mutable.value.items.size + fresh.size > AssistantPlacePolicy.MAX_PLACES)
            throw AssistantFailure("新增结果合并后超过 ${AssistantPlacePolicy.MAX_PLACES} 个地点，请分批；已有结果保留，未截断或查询新增地点。",
                AssistantFailureCode.OVERFLOW, SendEvidence.RESPONSE_RECEIVED)
        mutable.value = mutable.value.copy(items = mutable.value.items.map { item ->
            val extras = unique.filter { it.materialIdentities().intersect(item.intent.materialIdentities()).isNotEmpty() }
            if (extras.isEmpty()) return@map item
            val sources = (item.intent.sources + extras.flatMap { it.sources }).distinct()
            item.copy(intent = item.intent.copy(sources = sources,
                materialKeys = item.intent.materialIdentities() + extras.flatMap { it.materialIdentities() }), sources = sources)
        })
        return fresh
    }

    fun cancel() {
        // A stopped batch must retain completed image extraction without querying an unchecked prefix.
        if (semanticBatch && currentParsed.isNotEmpty()) {
            val sourceIds = (if (loadedLink?.modelText.orEmpty().isNotBlank()) listOf("text") else emptyList()) +
                mutable.value.images.map { it.id }
            parsedSources = (parsedSources.filterNot { old -> currentParsed.any { it.sourceId == old.sourceId } } +
                currentParsed).sortedBy { sourceIds.indexOf(it.sourceId).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
            mutable.value = mutable.value.copy(parsedMaterials = parsedSources, parsedMaterialCity = parsedCity,
                normalizationPending = mutable.value.normalizationPending || currentParsed.any { it.failure == null && it.intents.isNotEmpty() },
                sourceProgress = mutable.value.sourceProgress.map { progress ->
                currentParsed.find { it.sourceId == progress.sourceId }?.let {
                    progress.copy(stage = if (it.failure == null) SourceStage.COMPLETE else SourceStage.FAILED,
                        error = it.failure?.userMessage)
                } ?: progress
            })
            currentParsed = emptyList()
        }
        if (currentParsed.isNotEmpty()) {
            val merged = parsedSources.filterNot { old -> currentParsed.any { it.sourceId == old.sourceId } } + currentParsed
            if (merged.none { it.failure?.code == AssistantFailureCode.OVERFLOW } && mergeIdenticalPlaceIntents(merged.flatMap { it.intents }).size <= AssistantPlacePolicy.MAX_PLACES) {
                val represented = mutable.value.items.flatMap { it.intent.sources }.mapNotNull {
                    when (it) { is IntakeSource.Image -> it.imageId; is IntakeSource.Text -> "text" }
                }.toSet()
                val fresh = absorbRepeatedMaterials(currentParsed.filter { it.failure == null && it.sourceId !in represented }.flatMap { it.intents })
                val s = mutable.value
                mutable.value = s.copy(items = s.items + fresh.mapIndexed { index, intent ->
                    IntakeItem(UUID.randomUUID().toString(), AssistantPlacePolicy.label(s.items.size + index),
                        intent.copy(city = intent.city.ifBlank { s.city }, usesDefaultCity = intent.city.isBlank() ||
                            (sameCity(intent.city, s.city) && !intent.sourceSpan.contains(intent.city.removeSuffix("市")))),
                        status = IntakeStatus.CANCELLED, detail = "已理解，地图查询尚未开始；可继续此项。",
                        cityConflict = s.city.isNotBlank() && intent.city.isNotBlank() && !sameCity(intent.city, s.city) &&
                            !intent.hasExplicitCityIn(intent.sourceSpan) && !intent.hasExplicitCityIn(s.input))
                })
                parsedSources = merged
                mutable.value = mutable.value.copy(sourceProgress = mutable.value.sourceProgress.map { progress ->
                    currentParsed.find { it.sourceId == progress.sourceId }?.let { parsed ->
                        progress.copy(stage = if (parsed.failure == null) SourceStage.COMPLETE else SourceStage.FAILED,
                            error = parsed.failure?.userMessage, sendEvidence = parsed.failure?.sendEvidence ?: SendEvidence.RESPONSE_RECEIVED)
                    } ?: progress
                })
            }
            currentParsed = emptyList()
        }
        run++
        cityRevision++
        work?.cancel(); work = null
        stopPending(null)
        mutable.value = mutable.value.copy(busy = false, resolvingCity = false, linkProgress = null,
            parsedMaterials = parsedSources, parsedMaterialCity = parsedCity, sourceProgress = mutable.value.sourceProgress.map {
            if (it.stage in setOf(SourceStage.SELECTED, SourceStage.PREPARING, SourceStage.SENDING, SourceStage.UNDERSTANDING))
                it.copy(stage = SourceStage.CANCELLED) else it
        })
    }
    fun retry(ids: Set<String> = mutable.value.items.filter {
        it.status in setOf(IntakeStatus.FAILED, IntakeStatus.CANCELLED)
    }.map { it.id }.toSet()) {
        if (!canQuery() || mutable.value.busy || mutable.value.awaitingCityConfirmation || ids.isEmpty()) return
        val allowed = mutable.value.items.filter { it.id in ids && it.status !in setOf(IntakeStatus.READY, IntakeStatus.ALREADY_SAVED, IntakeStatus.DUPLICATE) }.map { it.id }.toSet()
        if (allowed.isEmpty()) return
        val generation = ++run
        mutable.value = mutable.value.copy(busy = true, error = null)
        work = scope.launch {
            try { withTimeout(AssistantPlacePolicy.BATCH_TIMEOUT_MILLIS) { query(allowed, generation) } }
            catch (_: TimeoutCancellationException) { if (isCurrent(generation)) stopPending("查询超时，已完成结果保留。") }
            finally { if (isCurrent(generation)) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun editItem(id: String, query: String, city: String) {
        if (mutable.value.saving || query.isBlank() || query.length > 100 || city.length > 50) return
        mutable.value = mutable.value.copy(selected = mutable.value.selected - id)
        changeItem(id) { it.copy(intent = it.intent.copy(query = query.trim(), city = city.trim(), needsReview = false),
            generation = it.generation + 1, status = IntakeStatus.CANCELLED, candidates = emptyList(), poi = null, detail = null, cityConflict = false) }
        freezeEvidenceItem(id)
    }
    fun confirmCity(id: String) {
        if (mutable.value.busy || mutable.value.saving) return
        if (mutable.value.items.find { it.id == id }?.intent?.needsReview == true) return
        changeItem(id) { it.copy(cityConflict = false) }
        retry(setOf(id))
    }
    fun choosePoi(id: String, poiId: String) {
        if (mutable.value.saving) return
        val item = mutable.value.items.find { it.id == id } ?: return
        val poi = item.candidates.find { it.poiId == poiId && it.validPosition() } ?: return
        mutable.value = mutable.value.copy(selected = mutable.value.selected - id)
        changeItem(id) { it.copy(poi = poi, status = IntakeStatus.READY, generation = it.generation + 1, detail = null) }
        freezeEvidenceItem(id)
        reconcile()
    }
    fun category(id: String, category: PlaceCategory) {
        if (mutable.value.saving) return
        changeItem(id) { it.copy(category = category) }
        freezeEvidenceItem(id)
    }
    fun toggle(id: String) {
        if (mutable.value.saving || mutable.value.eligible.none { it.id == id }) return
        val selected = mutable.value.selected
        mutable.value = mutable.value.copy(selected = if (id in selected) selected - id else selected + id)
    }
    fun selectAll() {
        if (mutable.value.saving) return
        val eligible = mutable.value.eligible.map { it.id }.toSet()
        mutable.value = mutable.value.copy(selected = if (mutable.value.selected == eligible) emptySet() else eligible)
    }
    /** A click captures immutable content; retrying that content reuses its operation ID. */
    fun saveSelected() {
        if (target == AssistantTarget.NewTripDraft) return
        if (!canSave() || mutable.value.saving) return
        val selected = mutable.value.eligible.filter { it.id in mutable.value.selected }
        if (selected.isEmpty() || selected.size != mutable.value.selected.size) return
        val items = selected.map { ImportItem(it.id, it.generation, requireNotNull(it.poi), it.category) }
        val next = ImportConfirmation.create(requireNotNull(tripId), items)
        val guideNext = guideOperation?.let {
            val prefix = if (mutable.value.hasIncompleteMaterials) "guide-partial" else "guide"
            ImportConfirmation.create(requireNotNull(tripId), items, "$prefix:$it:${next.digest.take(24)}")
        } ?: next
        val snapshot = pendingSave?.takeIf { it.digest == guideNext.digest } ?: guideNext
        val generation = run
        pendingSave = snapshot
        mutable.value = mutable.value.copy(saving = true, error = null, receipt = null)
        scope.launch {
            try {
                // Recheck after dispatch: a trip or map authorization can disappear before execution.
                if (!canSave() || generation != run) return@launch
                val receipt = importer.commit(snapshot)
                pendingSave = null
                if (!active) return@launch
                savedIds = savedIds + receipt.results.map { it.poiId }
                selected.forEach { freezeEvidenceItem(it.id, committed = true) }
                cancel()
                if (mutable.value.hasIncompleteMaterials) {
                    // A partial save is not batch completion: keep the failed sources,
                    // original link and successful extraction cache available for retry.
                    reconcile()
                    mutable.value = mutable.value.copy(receipt = receipt, error = null)
                    return@launch
                }
                inputRevision++
                parsedSources = emptyList()
                forgetLink()
                pendingSave = null
                guideOperation = null
                val old = mutable.value
                mutable.value = PlaceAssistantState(city = old.city, configured = old.configured,
                    consent = old.consent, mapReady = old.mapReady, receipt = receipt, saving = true)
                // Cache/UI cleanup must never turn a committed transaction into a save failure.
                runCatching { onBatchSaved(old.images.map { it.id }) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (active) fail("收藏失败，已保留选择，请重试。") }
            finally { mutable.value = mutable.value.copy(saving = false) }
        }
    }
    private fun freezeEvidenceItem(id: String, committed: Boolean = false) {
        val item = mutable.value.items.find { it.id == id } ?: return
        evidenceSession?.freeze(item, committed)
        mutable.value = mutable.value.copy(guideCheckpoint = evidenceSession?.checkpoint)
    }
    private suspend fun query(ids: Set<String>, generation: Long) = coroutineScope {
        val semaphore = Semaphore(2)
        ids.map { id -> launch {
            semaphore.withPermit {
                val item = mutable.value.items.find { it.id == id } ?: return@withPermit
                if (!isCurrent(generation)) return@withPermit
                if (item.intent.needsReview) {
                    changeItem(id) { it.copy(status = IntakeStatus.NEED_CITY, detail = "地点信息尚不明确，请对照来源核对名称和城市。") }
                    return@withPermit
                }
                if (item.cityConflict) {
                    changeItem(id) { it.copy(status = IntakeStatus.NEED_CITY, detail = "原文城市 ${item.intent.city} 与默认城市不同，请确认此项城市。") }
                    return@withPermit
                }
                if (item.intent.city.isBlank()) { changeItem(id) { it.copy(status = IntakeStatus.NEED_CITY, detail = "请补充搜索城市。") }; return@withPermit }
                changeItem(id) { it.copy(status = IntakeStatus.QUERYING, detail = null) }
                val keyword = assistantSearchKeyword(item.intent.query, item.intent.city)
                var event = AssistantSearchEvent(diagnosticSession, generation,
                    mutable.value.items.indexOfFirst { it.id == item.id },
                    searchFingerprint(diagnosticSalt, keyword), searchFingerprint(diagnosticSalt, item.intent.city),
                    keyword != item.intent.query.trim(), SearchOutcome.STARTED)
                var started: Long? = null
                var outcome = SearchOutcome.CANCELLED
                var resultCount = 0
                var sameCityCount = 0
                var errorCode: Int? = null
                val result = try {
                    val results = searchPacer.execute(beforeStart = {
                        if (!isCurrent(generation) ||
                            mutable.value.items.find { it.id == id }?.generation != item.generation)
                            throw CancellationException()
                        event = event.copy(timestampMs = System.currentTimeMillis())
                        started = System.nanoTime()
                        recordSearch(event)
                    }) {
                        currentCoroutineContext().ensureActive()
                        if (!isCurrent(generation) ||
                            mutable.value.items.find { it.id == id }?.generation != item.generation)
                            throw CancellationException()
                        val dataSource = source ?: throw AssistantFailure("地图服务未授权。")
                        // Waiting for a start slot must not consume the SDK's request timeout.
                        withTimeout(15_000) { dataSource.search(keyword, item.intent.city) }
                    }
                    resultCount = results.size
                    sameCityCount = results.filter { it.validPosition() }.distinctBy { it.poiId }.take(20)
                        .count { sameCity(it.cityName, item.intent.city) }
                    matchItem(item, results).also {
                        outcome = when (it.status) {
                            IntakeStatus.READY -> SearchOutcome.READY
                            IntakeStatus.NEED_CITY -> SearchOutcome.CITY_MISMATCH
                            else -> SearchOutcome.NOT_FOUND
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    outcome = SearchOutcome.TIMEOUT
                    item.copy(status = IntakeStatus.FAILED, detail = "查询超时，可仅重试此项。")
                }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    errorCode = (e as? AmapServiceException)?.errorCode
                    outcome = if (errorCode != null) SearchOutcome.SDK_ERROR else SearchOutcome.ERROR
                    item.copy(status = IntakeStatus.FAILED, detail = "地点查询失败，可仅重试此项。")
                } finally {
                    started?.let { requestStarted ->
                        recordSearch(event.copy(outcome = outcome,
                            elapsedMs = ((System.nanoTime() - requestStarted) / 1_000_000).coerceAtLeast(0),
                            resultCount = resultCount, sameCityCount = sameCityCount, errorCode = errorCode,
                            timestampMs = System.currentTimeMillis()))
                    }
                }
                if (isCurrent(generation) && mutable.value.items.find { it.id == id }?.generation == item.generation) {
                    changeItem(id) { result }; reconcile()
                }
            }
        } }.joinAll()
    }
    private suspend fun recordSearch(event: AssistantSearchEvent) = withContext(NonCancellable) {
        // A full/unavailable diagnostics directory must not change search, cancellation or saving.
        try { withTimeoutOrNull(500) { diagnostics.record(event) } } catch (_: Exception) { }
    }
    private fun reconcile() {
        // Prefer the user's selected representative when an earlier query finishes late.
        val preferred = mutable.value.items.filter { it.id in mutable.value.selected && it.poi != null }
            .associate { requireNotNull(it.poi).poiId to it.id }
        val seen = mutableSetOf<String>()
        val allSources = mutable.value.items.filter { it.poi != null }.groupBy { it.poi!!.poiId }
            .mapValues { (_, items) -> items.flatMap { it.intent.sources }.distinct() }
        val items = mutable.value.items.map { item ->
            val poi = item.poi ?: return@map item
            val reconciled = when {
                poi.poiId in savedIds -> item.copy(status = IntakeStatus.ALREADY_SAVED, detail = "已在地点池，保留已有内容。")
                (preferred[poi.poiId]?.let { it != item.id } == true) || !seen.add(poi.poiId) -> item.copy(status = IntakeStatus.DUPLICATE, detail = "与本批另一项为同一地点，不重复收藏。")
                else -> item.copy(status = IntakeStatus.READY, detail = null)
            }
            reconciled.copy(sources = allSources[poi.poiId].orEmpty())
        }
        val selected = mutable.value.selected.intersect(items.filter { it.status == IntakeStatus.READY }.map { it.id }.toSet())
        mutable.value = mutable.value.copy(items = items, selected = selected)
    }
    private fun stopPending(message: String?) {
        mutable.value = mutable.value.copy(items = mutable.value.items.map {
            if (it.status == IntakeStatus.WAITING || it.status == IntakeStatus.QUERYING) it.copy(status = IntakeStatus.CANCELLED, detail = "已停止，可继续此项。") else it
        }, error = message ?: mutable.value.error)
    }
    private fun changeItem(id: String, transform: (IntakeItem) -> IntakeItem) {
        mutable.value = mutable.value.copy(items = mutable.value.items.map { if (it.id == id) transform(it) else it })
    }
    private fun canQuery(): Boolean {
        val s = mutable.value
        val error = when {
            !active -> "目标旅行已失效。"
            s.saving -> return false
            !s.configured -> "请先在助手设置配置模型。"
            !s.consent -> "请先授权地图服务。"
            else -> null
        }
        if (error != null) fail(error)
        return error == null
    }
    private fun canSave(): Boolean = active && mutable.value.mapReady && mutable.value.consent
    private fun isCurrent(value: Long) = active && value == run && mutable.value.consent
    private fun fail(message: String) { mutable.value = mutable.value.copy(error = message) }
    private fun Exception.safeMessage() = (this as? AssistantFailure)?.userMessage ?: "未能解析地点，原输入保留，请重试。"
}
