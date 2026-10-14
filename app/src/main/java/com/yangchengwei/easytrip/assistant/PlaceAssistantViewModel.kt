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
    parser: PlaceIntentParser = DeepSeekPlaceParser(config::read),
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
) : ViewModel() {
    val sessionId = UUID.randomUUID().toString()
    private val imageRecoveryScope = tripId ?: "new-trip-intake"
    private var selectionGeneration = 0L
    private var clearing = false
    val controller: PlaceAssistantController = PlaceAssistantController(tripId, viewModelScope, parser, null, importer,
        processor = images?.let { store -> {
            val snapshot = config.read()
            AssistantInputProcessor(materialTextParserOverride ?: DeepSeekPlaceParser(config = { snapshot }, maxInputLength = 20_000),
                imageParserOverride ?: ImageIntentParser { image, ordinal, context, city, waiting ->
                    try { ImagePlaceParser({ snapshot }, cityScope = { controller.guideCityScope }).parse(image, ordinal, context, city, waiting) }
                    catch (e: AssistantFailure) {
                        if (e.code == AssistantFailureCode.UNSUPPORTED_IMAGE && snapshot != null)
                            config.saveImageCapability(snapshot, ImageCapabilityResult(ImageCapability.UNSUPPORTED, System.currentTimeMillis()))
                        throw e
                    }
                }, store,
                normalizer = normalizerOverride ?: ModelPlaceNormalizer({ snapshot }, cityScope = { controller.guideCityScope }),
                materialTextParser = materialTextParserOverride ?: DeepSeekPlaceParser({ snapshot },
                    maxInputLength = 20_000, maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true),
                materialImageParser = imageParserOverride ?: ImageIntentParser { image, ordinal, context, city, waiting ->
                    try {
                        ImagePlaceParser({ snapshot }, maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true, cityScope = { controller.guideCityScope })
                            .parse(image, ordinal, context, city, waiting)
                    } catch (e: AssistantFailure) {
                        if (e.code == AssistantFailureCode.UNSUPPORTED_IMAGE && snapshot != null)
                            config.saveImageCapability(snapshot, ImageCapabilityResult(ImageCapability.UNSUPPORTED, System.currentTimeMillis()))
                        throw e
                    }
                })
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
    init {
        if (tripId != null && guideStore != null) viewModelScope.launch {
            try {
                controller.setGuideCityScope(guideStore.scope(tripId))
                val stored = guideStore.find(tripId)
                mutableGuide.value = stored?.takeUnless { it.status == GuideHandoffStatus.FINISHED }
                if (stored?.status == GuideHandoffStatus.FINISHED) runCatching { guideMaterials?.release(stored.material.id) }
            } catch (_: Exception) { controller.reportError("攻略交接暂不可读取，请重新打开旅行。") }
        }
        viewModelScope.launch {
            controller.state.collect { value ->
                if (value.busy || value.saving) return@collect
                guideLock.withLock {
                    val handoff = activeGuide ?: return@withLock
                    if (!guideStarted || value != controller.state.value) return@withLock
                    try {
                    if (!controller.ownsGuideBatch(handoff.operationId)) {
                        guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.CANCELLED, null)
                        mutableGuide.value = handoff.copy(status = GuideHandoffStatus.CANCELLED, resultRef = null)
                        activeGuide = null
                        guideStarted = false
                        return@withLock
                    }
                    if (value.items.isEmpty()) {
                        if (handoff.status in setOf(GuideHandoffStatus.FAILED, GuideHandoffStatus.CANCELLED)) return@withLock
                        if (guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.FAILED, null) == true) {
                            val failed = handoff.copy(status = GuideHandoffStatus.FAILED, resultRef = null)
                            mutableGuide.value = failed
                            activeGuide = failed
                        }
                        return@withLock
                    }
                    val encoded = GuideResultCodec.encode(value)
                    if (encoded != lastGuideSnapshot) {
                        val ref = requireNotNull(guideResults).write(value)
                        if (guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.IMPORTED, ref) == true) {
                            lastGuideSnapshot = encoded
                            val updated = handoff.copy(status = GuideHandoffStatus.IMPORTED, resultRef = ref)
                            activeGuide = updated
                            mutableGuide.value = updated
                            handoff.resultRef?.let { runCatching { guideResults.remove(it) } }
                        } else guideResults.remove(ref)
                    }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { controller.reportError("导入结果暂未保存，攻略和旅行仍保留；请勿清理数据。") }
                }
            }
        }
        val interrupted = (images as? AndroidAssistantImageStore)?.startSession(sessionId, imageRecoveryScope) == true
        if (interrupted || savedState.get<Boolean>("assistant-had-images") == true) {
            savedState["assistant-had-images"] = false
            controller.reportError("上次图片草稿已失效，请重新选择；已收藏地点不受影响。")
        }
        viewModelScope.launch { controller.recoverReceipt() }
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
            if (handoff.status == GuideHandoffStatus.IMPORTED && handoff.resultRef != null) {
                val recovered = requireNotNull(guideResults).read(handoff.resultRef)
                val materials = materialStore.read(handoff.material)
                copyGuideImages(materials)
                activeGuide = handoff; guideStarted = true
                controller.restoreGuideState(recovered, handoff.operationId, handoff.scope)
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
            val loaded = if (materials.fromLink) LoadedAssistantLink(materials.modelText, copies) else null
            check(controller.acceptGuideInput(claimed.operationId, materials.originalText, copies, loaded, claimed.scope))
            guideStarted = true
            controller.submit()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
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
                if (activeGuide?.attempt != handoff.attempt || controller.state.value.items.isNotEmpty()) return@withLock
                if (guideStore?.complete(handoff.operationId, handoff.attempt, GuideHandoffStatus.CANCELLED, null) == true) {
                    val cancelled = handoff.copy(status = GuideHandoffStatus.CANCELLED, resultRef = null)
                    activeGuide = cancelled
                    mutableGuide.value = cancelled
                }
            }
        }
    }
    private suspend fun copyGuideImages(materials: GuideMaterials): List<AssistantImage> = materials.images.map { image ->
        requireNotNull(images).restoreCopy(sessionId, image,
            requireNotNull(guideMaterials).images.prepare(materials.imageSessionId, image.id))
    }
    fun retryGuide() {
        if (mutableGuide.value == null) return
        if (controller.state.value.busy || controller.state.value.saving) return
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaceAssistantViewModel(tripId, config, importer, places, images = images, savedState = savedState,
                searchDiagnostics = searchDiagnostics, searchPacer = searchPacer, linkLoader = linkLoader, target = target,
                guideStore = guideStore, guideMaterials = guideMaterials, guideResults = guideResults) as T
    }
    companion object { private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}
