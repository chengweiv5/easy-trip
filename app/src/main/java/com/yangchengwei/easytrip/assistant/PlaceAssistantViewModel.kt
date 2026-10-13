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
) : ViewModel() {
    val sessionId = UUID.randomUUID().toString()
    private val imageRecoveryScope = tripId ?: "new-trip-intake"
    private var selectionGeneration = 0L
    private var clearing = false
    val controller = PlaceAssistantController(tripId, viewModelScope, parser, null, importer,
        processor = images?.let { store -> {
            val snapshot = config.read()
            AssistantInputProcessor(materialTextParserOverride ?: DeepSeekPlaceParser(config = { snapshot }, maxInputLength = 20_000),
                imageParserOverride ?: ImageIntentParser { image, ordinal, context, city, waiting ->
                    try { ImagePlaceParser({ snapshot }).parse(image, ordinal, context, city, waiting) }
                    catch (e: AssistantFailure) {
                        if (e.code == AssistantFailureCode.UNSUPPORTED_IMAGE && snapshot != null)
                            config.saveImageCapability(snapshot, ImageCapabilityResult(ImageCapability.UNSUPPORTED, System.currentTimeMillis()))
                        throw e
                    }
                }, store,
                normalizer = normalizerOverride ?: ModelPlaceNormalizer({ snapshot }),
                materialTextParser = materialTextParserOverride ?: DeepSeekPlaceParser({ snapshot },
                    maxInputLength = 20_000, maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true),
                materialImageParser = imageParserOverride ?: ImageIntentParser { image, ordinal, context, city, waiting ->
                    try {
                        ImagePlaceParser({ snapshot }, maxPlaces = AssistantMaterialPolicy.MAX_PER_SOURCE, travelMaterials = true)
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
        val interrupted = (images as? AndroidAssistantImageStore)?.startSession(sessionId, imageRecoveryScope) == true
        if (interrupted || savedState.get<Boolean>("assistant-had-images") == true) {
            savedState["assistant-had-images"] = false
            controller.reportError("上次图片草稿已失效，请重新选择；已收藏地点不受影响。")
        }
        viewModelScope.launch { controller.recoverReceipt() }
        if (target is AssistantTarget.ExistingTrip)
            viewModelScope.launch { places.observeSavedPoiIds(target.tripId).collect(controller::updateSaved) }
        viewModelScope.launch {
            config.revision.collect { controller.configurationChanged() }
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaceAssistantViewModel(tripId, config, importer, places, images = images, savedState = savedState,
                searchDiagnostics = searchDiagnostics, searchPacer = searchPacer, linkLoader = linkLoader, target = target) as T
    }
    companion object { private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}
