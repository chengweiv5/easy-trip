package com.yangchengwei.easytrip.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.lifecycle.SavedStateHandle
import android.net.Uri
import com.yangchengwei.easytrip.assistant.cityfirst.*
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaceAssistantViewModel(
    private val tripId: String?,
    val config: AssistantConfigStore,
    importer: PlaceImport,
    places: SavedPlaceRepository,
    parser: PlaceIntentParser? = null,
    val images: AssistantImageStore? = null,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val imageParserOverride: ImageIntentParser? = null,
    private val searchDiagnostics: AssistantSearchDiagnostics = AssistantSearchDiagnostics.NONE,
    searchPacer: AssistantSearchPacer = AssistantSearchPacer(),
    linkLoader: AssistantLinkLoader? = null,
    private val materialTextParserOverride: PlaceIntentParser? = null,
    private val normalizerOverride: PlaceIntentNormalizer? = null,
    val target: AssistantTarget = tripId?.let(AssistantTarget::ExistingTrip) ?: AssistantTarget.NewTripDraft,
    private val guideStore: GuideHandoffStore? = null,
    private val guideMaterials: GuideMaterialStore? = null,
    private val guideResults: GuideImportResultStore? = null,
    private val protocolDiagnostics: AssistantProtocolDiagnostics = AssistantProtocolDiagnostics.NONE,
    private val guideParserFactory: ((ProviderConfig) -> GuideMaterialParser)? = null,
    private val guideEvidenceParserFactory: ((ProviderConfig) -> GuideMaterialParser)? = null,
    private val evidenceResolverOverride: GuideEvidenceResolver? = null,
) : ViewModel() {
    val sessionId = UUID.randomUUID().toString()
    private val imageRecoveryScope = tripId ?: "new-trip-intake"
    private var selectionGeneration = 0L
    private var clearing = false
    val controller: PlaceAssistantController = PlaceAssistantController(tripId, viewModelScope,
        parser ?: DeepSeekPlaceParser(config::read, diagnostics = protocolDiagnostics), null, importer,
        processor = images?.let { store -> {
            val snapshot = config.read()
            AssistantInputProcessor(materialTextParserOverride ?: DeepSeekPlaceParser(config = { snapshot }, maxInputLength = 20_000,
                diagnostics = protocolDiagnostics),
                imageParserOverride ?: ImageIntentParser { image, ordinal, context, city, waiting ->
                    try { ImagePlaceParser({ snapshot }, AssistantTransport(diagnostics = protocolDiagnostics),
                        cityScope = { controller.guideCityScope }).parse(image, ordinal, context, city, waiting) }
                    catch (e: AssistantFailure) {
                        if (e.code == AssistantFailureCode.UNSUPPORTED_IMAGE && snapshot != null)
                            config.saveImageCapability(snapshot, ImageCapabilityResult(ImageCapability.UNSUPPORTED, System.currentTimeMillis()))
                        throw e
                    }
                }, store,
                normalizer = normalizerOverride ?: ModelPlaceNormalizer({ snapshot }, AssistantTransport(diagnostics = protocolDiagnostics),
                    cityScope = { controller.guideCityScope }),
                materialTextParser = materialTextParserOverride ?: DeepSeekPlaceParser({ snapshot },
                    maxInputLength = 20_000, maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true,
                    diagnostics = protocolDiagnostics),
                materialImageParser = imageParserOverride ?: ImageIntentParser { image, ordinal, context, city, waiting ->
                    try {
                        ImagePlaceParser({ snapshot }, AssistantTransport(diagnostics = protocolDiagnostics),
                            maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true, cityScope = { controller.guideCityScope })
                            .parse(image, ordinal, context, city, waiting)
                    } catch (e: AssistantFailure) {
                        if (e.code == AssistantFailureCode.UNSUPPORTED_IMAGE && snapshot != null)
                            config.saveImageCapability(snapshot, ImageCapabilityResult(ImageCapability.UNSUPPORTED, System.currentTimeMillis()))
                        throw e
                    }
                }, guideParser = snapshot?.let { guideParserFactory?.invoke(it) },
                evidenceGuideParser = snapshot?.let { guideEvidenceParserFactory?.invoke(it) },
                evidenceResolver = evidenceResolverOverride ?: if (guideEvidenceParserFactory != null &&
                    snapshot?.let(GuideEvidenceBudget::supports) == true)
                    ModelGuideEvidenceResolver({ snapshot }, AssistantTransport(diagnostics = protocolDiagnostics)) else null)
        } }, sessionId = sessionId,
        imageAllowed = { config.imageCapability().capability != ImageCapability.UNSUPPORTED },
        diagnostics = searchDiagnostics, searchPacer = searchPacer,
        onBatchSaved = ::completeBatch, linkLoader = linkLoader, target = target)
    private val guideLock = Mutex()
    private val mutableGuide = MutableStateFlow<GuideHandoff?>(null)
    val guide = mutableGuide.asStateFlow()
    private var activeGuide: GuideHandoff? = null
    private var guideStarted = false
    private var lastGuideSnapshot: String? = null
    private val guideSnapshotSaveGuard = GuideSnapshotSaveGuard()
    private var localDraft: GuideLocalDraftRef? = null
    private var localReady = guideResults == null || guideMaterials == null || tripId == null
    private var localRestoreFailed = false
    private var lastLocalSnapshot: String? = null
    private val localSnapshotGuard = GuideSnapshotSaveGuard()
    private val mutableOpen = MutableStateFlow(false)
    val open = mutableOpen.asStateFlow()
    private val mutableFocus = MutableStateFlow<String?>(null)
    val focus = mutableFocus.asStateFlow()
    private val mutableDetail = MutableStateFlow<AssistantDetail?>(null)
    val detail = mutableDetail.asStateFlow()
    fun show() { mutableOpen.value = true }
    fun hide() { if (!controller.state.value.saving) mutableOpen.value = false }
    fun focus(id: String?) {
        if (controller.state.value.saving) return
        if (id != mutableFocus.value) mutableDetail.value = null
        mutableFocus.value = id
        if (id != null) show()
    }
    fun showDetail(id: String, detail: AssistantDetail) {
        if (controller.state.value.saving) return
        focus(id)
        mutableDetail.value = detail
    }
    fun consumeDetail() { mutableDetail.value = null }
    private fun completeBatch(imageIds: List<String>) {
        val completed = activeGuide
        activeGuide = null
        guideStarted = false
        completed?.let { handoff ->
            cleanup.launch {
                guideLock.withLock {
                    if (guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.FINISHED, null) == true) {
                        mutableGuide.value = null
                        runCatching { guideMaterials?.release(handoff.material.id) }
                        handoff.resultRef?.let { runCatching { guideResults?.remove(it) } }
                    }
                }
            }
        }
        selectionGeneration++
        savedState["assistant-had-images"] = false
        mutableFocus.value = null
        mutableDetail.value = null
        // Remove only this batch: releasing the reused session could delete new-round images.
        cleanup.launch {
            imageIds.forEach { id -> runCatching { images?.remove(sessionId, id) } }
        }
    }
    fun selectImages(uris: List<Uri>) {
        val store = images ?: return
        val s = controller.state.value
        if (uris.isEmpty() || s.busy || s.saving || s.items.isNotEmpty() || s.attachmentSelectionBusy) return
        val generation = ++selectionGeneration
        val revision = controller.draftRevision
        controller.attachmentSelection(true)
        viewModelScope.launch {
            var result: ImageSelectionResult? = null
            try {
                result = store.select(sessionId, uris, s.images)
                if (generation != selectionGeneration || revision != controller.draftRevision) {
                    result.accepted.forEach { store.remove(sessionId, it.id) }
                } else {
                    controller.addImages(result.accepted)
                    val retainedIds = controller.state.value.images.map { it.id }.toSet()
                    result.accepted.filterNot { it.id in retainedIds }.forEach { store.remove(sessionId, it.id) }
                    savedState["assistant-had-images"] = controller.state.value.images.isNotEmpty()
                    if (result.rejected.isNotEmpty()) controller.reportError(result.rejected.joinToString("\n"))
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable) { result?.accepted?.forEach { store.remove(sessionId, it.id) } }
                throw e
            } catch (_: Exception) {
                if (generation == selectionGeneration && revision == controller.draftRevision)
                    controller.reportError("无法读取所选图片，请重新选择。")
            }
            finally { if (generation == selectionGeneration) controller.attachmentSelection(false) }
        }
    }
    fun removeImage(id: String) {
        val before = controller.state.value.images
        controller.removeImage(id)
        if (before.any { it.id == id } && controller.state.value.images.none { it.id == id }) {
            viewModelScope.launch { images?.remove(sessionId, id) }
            savedState["assistant-had-images"] = controller.state.value.images.isNotEmpty()
        }
    }
    fun clearDraft() {
        if (controller.state.value.saving || clearing) return
        clearing = true
        selectionGeneration++
        controller.clear()
        controller.attachmentSelection(true)
        savedState["assistant-had-images"] = false
        mutableFocus.value = null
        mutableDetail.value = null
        viewModelScope.launch {
            try {
                images?.release(sessionId)
                (images as? AndroidAssistantImageStore)?.startSession(sessionId, imageRecoveryScope)
            } finally { clearing = false; controller.attachmentSelection(false) }
        }
    }
    override fun onCleared() {
        selectionGeneration++
        controller.cancel()
        cleanup.launch { images?.release(sessionId) }
    }
    private suspend fun clearLocalDraft(expected: GuideLocalDraftRef) {
        val results = guideResults ?: return
        if (results.replaceLocalDraft(expected.tripId, expected, null)) {
            if (localDraft == expected) {
                localDraft = null
                lastLocalSnapshot = null
            }
            results.remove(expected.resultRef)
            guideMaterials?.release(expected.material.id)
        }
    }
    private suspend fun saveLocalDraft(value: PlaceAssistantState) {
        if (!localReady || localRestoreFailed || activeGuide != null || tripId == null ||
            guideResults == null || guideMaterials == null || images == null) return
        val checkpoint = value.guideCheckpoint
        if (checkpoint == null || value.guideVersion != GuideProtocolVersion.EVIDENCE_V2) {
            localDraft?.let { clearLocalDraft(it) }
            return
        }
        val loaded = controller.loadedGuideMaterial() ?: return
        localSnapshotGuard.attempt(checkpoint.binding.batchId, value) {
            val encoded = GuideResultCodec.encode(value.copy(error = null))
            if (encoded == lastLocalSnapshot) return@attempt
            val previous = localDraft
            var createdMaterial: GuideMaterialRef? = null
            var createdResult: String? = null
            var published = false
            try {
                val material = guideMaterials.retainLoaded(
                    value.input, loaded.modelText, true, sessionId, value.images, images, value.optionalImages,
                    loaded.bodyAvailable).ref.also {
                    createdMaterial = it
                }
                createdResult = guideResults.write(value.copy(error = null))
                val next = GuideLocalDraftRef(tripId, checkpoint.binding.batchId, checkpoint.binding.revision,
                    material, requireNotNull(createdResult))
                withContext(NonCancellable) {
                    // Never let an old suspended save publish over a cleared or replaced live batch.
                    if (value != controller.state.value) return@withContext
                    if (!guideResults.replaceLocalDraft(tripId, previous, next))
                        throw IllegalStateException("草稿引用已变化，未覆盖其他批次")
                    published = true
                    localDraft = next
                    lastLocalSnapshot = encoded
                    previous?.let {
                        runCatching { guideResults.remove(it.resultRef) }
                        if (it.material != material) runCatching { guideMaterials.release(it.material.id) }
                    }
                }
            } finally {
                if (!published) withContext(NonCancellable) {
                    createdResult?.let { runCatching { guideResults.remove(it) } }
                    createdMaterial?.let { runCatching { guideMaterials.release(it.id) } }
                }
            }
        }
    }
    init {
        val interrupted = (images as? AndroidAssistantImageStore)?.startSession(sessionId, imageRecoveryScope) == true
        if (!localReady) controller.attachmentSelection(true)
        if (tripId != null) viewModelScope.launch {
            guideLock.withLock { try {
                controller.setGuideCityScope(guideStore?.scope(tripId))
                val stored = guideStore?.find(tripId)
                mutableGuide.value = stored?.takeUnless { it.status == GuideHandoffStatus.FINISHED }
                if (stored?.status == GuideHandoffStatus.FINISHED) runCatching { guideMaterials?.release(stored.material.id) }
                if (guideResults != null && guideMaterials != null) {
                    val draft = guideResults.localDraft(tripId)
                    localDraft = draft
                    if (draft != null && importer.guideReceipt(draft.batchId) != null) {
                        // The transaction may have succeeded just before process death and draft cleanup.
                        // Its durable receipt wins over the stale snapshot: never revive a finished round.
                        clearLocalDraft(draft)
                    } else if (draft != null) {
                        val recovered = guideResults.read(draft.resultRef)
                        val binding = requireNotNull(recovered.guideCheckpoint).binding
                        require(binding.batchId == draft.batchId && binding.revision == draft.revision)
                        val material = guideMaterials.read(draft.material)
                        val copied = copyGuideImages(material)
                        val plan = materialCopyPlan(recoveredPlan(recovered, material), copied)
                        controller.attachmentSelection(false)
                        controller.restoreGuideState(recovered.copy(optionalImages = plan), draft.batchId, controller.guideCityScope,
                            LoadedAssistantLink(material.modelText, recovered.images, plan, material.bodyAvailable))
                        lastLocalSnapshot = GuideResultCodec.encode(recovered.copy(error = null))
                    }
                }
                if (localDraft == null && (interrupted || savedState.get<Boolean>("assistant-had-images") == true))
                    controller.reportError("上次图片草稿已失效，请重新选择；已收藏地点不受影响。")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                localRestoreFailed = true
                controller.reportError("攻略草稿暂不可恢复，原文件和旅行仍保留；请重新打开旅行，勿清理数据。")
            } finally {
                localReady = true
                savedState["assistant-had-images"] = false
                controller.attachmentSelection(false)
            }
                // Restore the receipt only after draft recovery/cleanup releases the attachment gate.
                controller.recoverReceipt()
            }
        }
        viewModelScope.launch {
            controller.state.collect { value ->
                if (value.busy || value.saving || value.attachmentSelectionBusy) return@collect
                guideLock.withLock {
                    if (value != controller.state.value) return@withLock
                    val handoff = activeGuide ?: run {
                        try { saveLocalDraft(value) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) {
                            controller.reportError("攻略草稿暂未保存，当前结果仍可使用；请勿清理数据。")
                        }
                        return@withLock
                    }
                    if (!guideStarted || value != controller.state.value) return@withLock
                    try {
                    if (!controller.ownsGuideBatch(handoff.operationId)) {
                        guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.CANCELLED, null)
                        mutableGuide.value = handoff.copy(status = GuideHandoffStatus.CANCELLED, resultRef = null)
                        activeGuide = null
                        guideStarted = false
                        return@withLock
                    }
                    if (value.guideVersion != GuideProtocolVersion.EVIDENCE_V2 && value.items.isEmpty() &&
                        value.parsedMaterials.none { it.failure == null && it.intents.isNotEmpty() } && value.optionalImages.assets.isEmpty()) {
                        if (handoff.status in setOf(GuideHandoffStatus.FAILED, GuideHandoffStatus.CANCELLED)) return@withLock
                        if (guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.FAILED, null) == true) {
                            val failed = handoff.copy(status = GuideHandoffStatus.FAILED, resultRef = null)
                            mutableGuide.value = failed
                            activeGuide = failed
                        }
                        return@withLock
                    }
                    guideSnapshotSaveGuard.attempt("${handoff.operationId}:${handoff.attempt}", value) {
                      val encoded = GuideResultCodec.encode(value)
                      if (encoded != lastGuideSnapshot) {
                        requireNotNull(guideMaterials).cacheWorkspaceImages(handoff.material,
                            value.images, sessionId, requireNotNull(images))
                        val ref = requireNotNull(guideResults).write(value)
                        val status = if (value.items.isEmpty() || value.guideVersion == GuideProtocolVersion.EVIDENCE_V2 &&
                            (value.hasIncompleteMaterials || value.guideCheckpoint?.resolution?.places.isNullOrEmpty()))
                            GuideHandoffStatus.FAILED else GuideHandoffStatus.IMPORTED
                        if (guideStore?.complete(handoff.operationId, handoff.attempt, status, ref) == true) {
                            lastGuideSnapshot = encoded
                            val updated = handoff.copy(status = status, resultRef = ref)
                            activeGuide = updated
                            mutableGuide.value = updated
                            handoff.resultRef?.let { runCatching { guideResults.remove(it) } }
                        } else guideResults.remove(ref)
                      }
                    }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { controller.reportError("导入结果暂未保存，攻略和旅行仍保留；请勿清理数据。") }
                }
            }
        }
        if (tripId == null && (interrupted || savedState.get<Boolean>("assistant-had-images") == true)) {
            savedState["assistant-had-images"] = false
            controller.reportError("上次图片草稿已失效，请重新选择；已收藏地点不受影响。")
        }
        if (target is AssistantTarget.ExistingTrip)
            viewModelScope.launch { places.observeSavedPoiIds(target.tripId).collect(controller::updateSaved) }
        viewModelScope.launch {
            config.revision.drop(1).collect { controller.configurationChanged() }
        }
    }
    fun consumePendingGuide() { viewModelScope.launch { consumeGuide() } }
    suspend fun consumeGuide() = guideLock.withLock {
        val handoff = mutableGuide.value ?: return@withLock
        val store = guideStore ?: return@withLock
        val materialStore = guideMaterials ?: return@withLock
        val current = controller.state.value
        if (activeGuide != null || !current.configured || !current.consent || !current.mapReady) return@withLock
        if (current.hasDraft) {
            if (handoff.status == GuideHandoffStatus.PENDING)
                controller.reportError("当前助手有未完成输入；完成或清空后再导入攻略。")
            return@withLock
        }
        try {
            val receipt = importerForGuide.guideReceipt(handoff.operationId)
            if (receipt?.tripId == tripId) {
                store.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.FINISHED, null)
                mutableGuide.value = null; materialStore.release(handoff.material.id); return@withLock
            }
            if (handoff.status in setOf(GuideHandoffStatus.IMPORTED, GuideHandoffStatus.FAILED) && handoff.resultRef != null) {
                val recovered = requireNotNull(guideResults).read(handoff.resultRef)
                val materials = materialStore.read(handoff.material)
                val cached = materials.copy(images = (materials.images + recovered.images).distinctBy { it.id })
                val copied = copyGuideImages(cached)
                val plan = materialCopyPlan(recoveredPlan(recovered, materials), copied)
                activeGuide = handoff; guideStarted = true
                controller.restoreGuideState(recovered.copy(optionalImages = plan), handoff.operationId, handoff.scope,
                    if (materials.fromLink) LoadedAssistantLink(materials.modelText, recovered.images, plan, materials.bodyAvailable) else null)
                return@withLock
            }
            if (handoff.status != GuideHandoffStatus.PENDING) {
                controller.reportError("上次导入已中断，旅行和原攻略已保留；请点击重试导入。")
                return@withLock
            }
            val claimed = store.claim(handoff.operationId, handoff.digest, handoff.attempt) ?: return@withLock
            activeGuide = claimed; mutableGuide.value = claimed
            val materials = materialStore.read(claimed.material)
            val copies = copyGuideImages(materials)
            val plan = materialCopyPlan(materials.imagePlan, copies)
            val loaded = if (materials.fromLink) LoadedAssistantLink(materials.modelText, copies, plan, materials.bodyAvailable) else null
            check(controller.acceptGuideInput(claimed.operationId, materials.originalText, copies, loaded, claimed.scope, plan))
            guideStarted = true
            controller.submit()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (handoff.resultRef != null) {
                controller.reportError("保存的攻略暂不能恢复，旅行和原记录已保留；不会自动重新发送。")
                return@withLock
            }
            store.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.FAILED, null)
            activeGuide = null
            guideStarted = false
            mutableGuide.value = store.find(requireNotNull(tripId))
            controller.reportError("导入失败，旅行和原攻略已保留；请重试导入。")
        }
    }
    private val importerForGuide = importer
    fun cancelCurrentBatch() {
        controller.cancel()
        val handoff = activeGuide ?: return
        viewModelScope.launch {
            guideLock.withLock {
                val current = controller.state.value
                if (activeGuide?.attempt != handoff.attempt || current.items.isNotEmpty() ||
                    current.parsedMaterials.any { it.failure == null && (it.intents.isNotEmpty() || it.evidence.isNotEmpty()) }) return@withLock
                if (guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.CANCELLED, null) == true) {
                    val cancelled = handoff.copy(status = GuideHandoffStatus.CANCELLED, resultRef = null)
                    activeGuide = cancelled
                    mutableGuide.value = cancelled
                }
            }
        }
    }
    private fun materialCopyPlan(plan: OptionalImagePlan, copies: List<AssistantImage>) = plan.copy(assets = plan.assets.map { asset ->
        if (asset.descriptor != null && copies.none { it.id == asset.id }) asset.skipped(OptionalImageSkip.MISSING) else asset
    })
    private fun recoveredPlan(state: PlaceAssistantState, material: GuideMaterials): OptionalImagePlan {
        val base = state.optionalImages.takeIf { it.assets.isNotEmpty() } ?: material.imagePlan
        return base.copy(assets = base.assets.map { asset ->
            if (material.imagePlan.assets.any { it.id == asset.id && it.skip == OptionalImageSkip.MISSING })
                asset.skipped(OptionalImageSkip.MISSING) else asset
        })
    }
    private suspend fun copyGuideImages(materials: GuideMaterials): List<AssistantImage> {
        val copies = mutableListOf<AssistantImage>()
        for (image in materials.images) {
            try {
                copies += requireNotNull(images).restoreCopy(sessionId, image,
                    requireNotNull(guideMaterials).images.prepare(materials.imageSessionId, image.id))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* The source descriptor and validated results remain recoverable. */ }
        }
        return copies
    }
    fun retryGuide() {
        if (mutableGuide.value == null) return
        if (controller.state.value.busy || controller.state.value.saving) return
        if (controller.state.value.guideVersion == GuideProtocolVersion.EVIDENCE_V2 &&
            controller.state.value.guideCheckpoint != null) {
            if (controller.state.value.normalizationPending) controller.retryNormalization()
            else controller.retryImages()
            return
        }
        viewModelScope.launch {
            guideLock.withLock {
                val handoff = mutableGuide.value ?: return@withLock
                try {
                    val retry = guideStore?.requestRetry(handoff.operationId, handoff.attempt) ?: return@withLock
                    activeGuide = null; guideStarted = false; lastGuideSnapshot = null
                    controller.clear()
                    mutableGuide.value = retry
                    handoff.resultRef?.let { runCatching { guideResults?.remove(it) } }
                }
                catch (_: Exception) { controller.reportError("暂不能重试，原攻略仍保留。") }
            }
            consumeGuide()
        }
    }
    class Factory(
        private val tripId: String?, private val config: AssistantConfigStore,
        private val importer: PlaceImport, private val places: SavedPlaceRepository,
        private val images: AssistantImageStore? = null,
        private val savedState: SavedStateHandle = SavedStateHandle(),
        private val searchDiagnostics: AssistantSearchDiagnostics = AssistantSearchDiagnostics.NONE,
        private val searchPacer: AssistantSearchPacer = AssistantSearchPacer(),
        private val linkLoader: AssistantLinkLoader? = null,
        private val target: AssistantTarget = tripId?.let(AssistantTarget::ExistingTrip) ?: AssistantTarget.NewTripDraft,
        private val guideStore: GuideHandoffStore? = null,
        private val guideMaterials: GuideMaterialStore? = null,
        private val guideResults: GuideImportResultStore? = null,
        private val protocolDiagnostics: AssistantProtocolDiagnostics = AssistantProtocolDiagnostics.NONE,
        private val guideParserFactory: ((ProviderConfig) -> GuideMaterialParser)? = null,
        private val normalizerOverride: PlaceIntentNormalizer? = null,
        private val guideEvidenceParserFactory: ((ProviderConfig) -> GuideMaterialParser)? = null,
        private val evidenceResolverOverride: GuideEvidenceResolver? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaceAssistantViewModel(tripId, config, importer, places, images = images, savedState = savedState,
                searchDiagnostics = searchDiagnostics, searchPacer = searchPacer, linkLoader = linkLoader, target = target,
                guideStore = guideStore, guideMaterials = guideMaterials, guideResults = guideResults,
                protocolDiagnostics = protocolDiagnostics, guideParserFactory = guideParserFactory,
                normalizerOverride = normalizerOverride, guideEvidenceParserFactory = guideEvidenceParserFactory,
                evidenceResolverOverride = evidenceResolverOverride) as T
    }
    companion object { private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}
