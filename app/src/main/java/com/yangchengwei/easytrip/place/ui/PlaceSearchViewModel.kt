package com.yangchengwei.easytrip.place.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceService
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SEARCH_QUERY_KEY = "query"
private const val DISPLAY_MODE_KEY = "displayMode"
private const val SELECTED_POI_ID_KEY = "selectedPoiId"
private const val RESULTS_MODE = "RESULTS"
private const val MAP_DETAIL_MODE = "MAP_DETAIL"

sealed interface SearchDisplayMode {
    data object Results : SearchDisplayMode
    data class MapDetail(val poiId: String) : SearchDisplayMode
}

data class PlaceDetailEditState(
    val placeId: String,
    val note: String,
    val selectedTagNames: Set<String>,
    val newTagInput: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PlaceSearchBackDecision {
    data object Ignore : PlaceSearchBackDecision
    data object DismissRemovalConfirmation : PlaceSearchBackDecision
    data object CancelEdit : PlaceSearchBackDecision
    data object ShowResults : PlaceSearchBackDecision
    data object ExitDestination : PlaceSearchBackDecision
}

sealed interface PlaceSearchAction {
    data object Back : PlaceSearchAction
    data class OpenDetail(val poiId: String) : PlaceSearchAction
    data class QueryChanged(val value: String) : PlaceSearchAction
    data object Submit : PlaceSearchAction
    data object Retry : PlaceSearchAction
    data object RecenterDetail : PlaceSearchAction
    data class ToggleCollection(val poiId: String) : PlaceSearchAction
    data class StartEdit(val placeId: String) : PlaceSearchAction
    data class UpdateEditNote(val value: String) : PlaceSearchAction
    data class UpdateEditTags(val value: Set<String>) : PlaceSearchAction
    data class UpdateNewTagInput(val value: String) : PlaceSearchAction
    data object AddNewTag : PlaceSearchAction
    data class RemoveEditTag(val name: String) : PlaceSearchAction
    data object SaveEdit : PlaceSearchAction
    data object CancelEdit : PlaceSearchAction
    data object DismissRemovalConfirmation : PlaceSearchAction
    data object ConfirmRemoval : PlaceSearchAction
}

data class PlaceSearchUiState(
    val search: PlaceSearchState = PlaceSearchState(),
    val displayMode: SearchDisplayMode = SearchDisplayMode.Results,
    val savedPoiIds: Set<String> = emptySet(),
    val savedPlacesByPoiId: Map<String, SavedPlace> = emptyMap(),
    val collectionBusyPoiIds: Set<String> = emptySet(),
    val pendingCollectionRemoval: PendingCollectionRemoval? = null,
    val collectionError: String? = null,
    val collectionErrorPoiId: String? = null,
    val detailDraft: PlaceDetailEditState? = null,
    val detailMapRequestId: Long = 1L,
    val shouldNavigateBack: Boolean = false,
)

internal fun decidePlaceSearchBack(state: PlaceSearchUiState): PlaceSearchBackDecision {
    val mutationBusy = state.detailDraft?.isSaving == true || state.collectionBusyPoiIds.isNotEmpty()
    return when {
        mutationBusy -> PlaceSearchBackDecision.Ignore
        state.pendingCollectionRemoval != null -> PlaceSearchBackDecision.DismissRemovalConfirmation
        state.detailDraft != null -> PlaceSearchBackDecision.CancelEdit
        state.displayMode is SearchDisplayMode.MapDetail -> PlaceSearchBackDecision.ShowResults
        else -> PlaceSearchBackDecision.ExitDestination
    }
}

internal fun reducePlaceSearchBack(state: PlaceSearchUiState): PlaceSearchUiState =
    when (decidePlaceSearchBack(state)) {
        PlaceSearchBackDecision.Ignore -> state
        PlaceSearchBackDecision.DismissRemovalConfirmation -> state.copy(
            pendingCollectionRemoval = null,
            collectionError = null,
            collectionErrorPoiId = null,
        )
        PlaceSearchBackDecision.CancelEdit -> state.copy(detailDraft = null)
        PlaceSearchBackDecision.ShowResults -> state.copy(displayMode = SearchDisplayMode.Results)
        PlaceSearchBackDecision.ExitDestination -> state.copy(shouldNavigateBack = true)
    }

class PlaceSearchViewModel(
    private val tripId: String,
    private val repository: SavedPlaceRepository,
    source: PlaceSearchDataSource?,
    private val savedStateHandle: SavedStateHandle,
    private val service: PlaceService = PlaceService(repository),
) : ViewModel() {
    private val reducer = PlaceSearchReducer(
        source,
        viewModelScope,
        Dispatchers.Main.immediate,
        savedStateHandle[SEARCH_QUERY_KEY] ?: "",
    )
    private val mutableState = MutableStateFlow(
        PlaceSearchUiState(
            search = reducer.state.value,
            displayMode = restoredDisplayMode(),
        ),
    )
    val state: StateFlow<PlaceSearchUiState> = mutableState.asStateFlow()
    private var savedByPoiId: Map<String, SavedPlace> = emptyMap()
    private val recentlyCollectedPoiIds = mutableSetOf<String>()
    private var detailEditGeneration = 0L

    fun recentlyCollectedPoiIds(): Set<String> = recentlyCollectedPoiIds.toSet()

    init {
        viewModelScope.launch {
            reducer.state.collect { search ->
                savedStateHandle[SEARCH_QUERY_KEY] = search.query
                mutableState.value = mutableState.value.copy(search = search)
                validateRestoredDetail(search)
            }
        }
        viewModelScope.launch {
            repository.observePlaces(tripId, emptySet()).collect { places ->
                savedByPoiId = places.associateBy(SavedPlace::amapPoiId)
                mutableState.value = mutableState.value.copy(savedPlacesByPoiId = savedByPoiId)
                reducer.setSavedPlaces(places)
            }
        }
        viewModelScope.launch {
            repository.observeSavedPoiIds(tripId).collect { ids ->
                mutableState.value = mutableState.value.copy(savedPoiIds = ids)
            }
        }
    }

    fun dispatch(action: PlaceSearchAction) {
        when (action) {
            PlaceSearchAction.Back -> handleBack()
            is PlaceSearchAction.OpenDetail -> openDetail(action.poiId)
            is PlaceSearchAction.QueryChanged -> reducer.setQuery(action.value)
            PlaceSearchAction.Submit -> reducer.submit()
            PlaceSearchAction.Retry -> reducer.retry()
            PlaceSearchAction.RecenterDetail -> recenterDetail()
            is PlaceSearchAction.ToggleCollection -> toggleCollection(action.poiId)
            is PlaceSearchAction.StartEdit -> startEdit(action.placeId)
            is PlaceSearchAction.UpdateEditNote -> updateEdit(note = action.value)
            is PlaceSearchAction.UpdateEditTags -> updateEdit(tags = action.value)
            is PlaceSearchAction.UpdateNewTagInput -> updateNewTagInput(action.value)
            PlaceSearchAction.AddNewTag -> addNewTag()
            is PlaceSearchAction.RemoveEditTag -> removeEditTag(action.name)
            PlaceSearchAction.SaveEdit -> saveEdit()
            PlaceSearchAction.CancelEdit -> cancelEdit()
            PlaceSearchAction.DismissRemovalConfirmation -> dismissRemovalConfirmation()
            PlaceSearchAction.ConfirmRemoval -> confirmRemoval()
        }
    }

    fun consumeBack() {
        mutableState.value = mutableState.value.copy(shouldNavigateBack = false)
    }

    private fun restoredDisplayMode(): SearchDisplayMode {
        val poiId = savedStateHandle.get<String>(SELECTED_POI_ID_KEY)
        return if (savedStateHandle.get<String>(DISPLAY_MODE_KEY) == MAP_DETAIL_MODE && poiId != null) {
            SearchDisplayMode.MapDetail(poiId)
        } else {
            SearchDisplayMode.Results
        }
    }

    private fun openDetail(poiId: String) {
        if (reducer.state.value.results.none { it.poiId == poiId }) return
        setDisplayMode(SearchDisplayMode.MapDetail(poiId))
    }

    private fun validateRestoredDetail(search: PlaceSearchState) {
        val detail = mutableState.value.displayMode as? SearchDisplayMode.MapDetail ?: return
        if (search.phase == PlaceSearchPhase.Initial || search.phase == PlaceSearchPhase.Loading) return
        if (search.results.none { it.poiId == detail.poiId }) setDisplayMode(SearchDisplayMode.Results)
    }

    private fun handleBack() {
        val current = mutableState.value
        val updated = reducePlaceSearchBack(current)
        if (updated === current) return
        if (updated.displayMode != current.displayMode) {
            setDisplayMode(updated.displayMode)
        } else {
            mutableState.value = updated
        }
    }

    private fun setDisplayMode(mode: SearchDisplayMode) {
        mutableState.value = mutableState.value.copy(displayMode = mode)
        when (mode) {
            SearchDisplayMode.Results -> {
                savedStateHandle[DISPLAY_MODE_KEY] = RESULTS_MODE
                savedStateHandle[SELECTED_POI_ID_KEY] = null
            }
            is SearchDisplayMode.MapDetail -> {
                savedStateHandle[DISPLAY_MODE_KEY] = MAP_DETAIL_MODE
                savedStateHandle[SELECTED_POI_ID_KEY] = mode.poiId
            }
        }
    }

    private fun recenterDetail() {
        if (mutableState.value.displayMode !is SearchDisplayMode.MapDetail) return
        mutableState.value = mutableState.value.copy(
            detailMapRequestId = mutableState.value.detailMapRequestId + 1L,
        )
    }

    private fun startEdit(placeId: String) {
        val place = savedByPoiId.values.firstOrNull { it.id == placeId } ?: return
        detailEditGeneration++
        mutableState.value = mutableState.value.copy(
            detailDraft = PlaceDetailEditState(
                placeId = place.id,
                note = place.note,
                selectedTagNames = place.tags.mapTo(mutableSetOf()) { it.name },
            ),
        )
    }

    private fun updateEdit(note: String? = null, tags: Set<String>? = null) {
        val draft = mutableState.value.detailDraft ?: return
        if (draft.isSaving) return
        mutableState.value = mutableState.value.copy(
            detailDraft = draft.copy(
                note = note ?: draft.note,
                selectedTagNames = tags ?: draft.selectedTagNames,
                errorMessage = null,
            ),
        )
    }

    private fun updateNewTagInput(value: String) {
        val draft = mutableState.value.detailDraft ?: return
        if (draft.isSaving) return
        mutableState.value = mutableState.value.copy(
            detailDraft = draft.copy(newTagInput = value, errorMessage = null),
        )
    }

    private fun addNewTag() {
        val draft = mutableState.value.detailDraft ?: return
        if (draft.isSaving) return
        when (val validation = validatePlaceTag(draft.newTagInput, draft.selectedTagNames)) {
            is PlaceTagValidation.Valid -> mutableState.value = mutableState.value.copy(
                detailDraft = draft.copy(
                    selectedTagNames = draft.selectedTagNames + validation.name,
                    newTagInput = "",
                    errorMessage = null,
                ),
            )
            PlaceTagValidation.Empty -> Unit
            PlaceTagValidation.Duplicate -> setTagError("标签已存在")
            PlaceTagValidation.TooLong -> setTagError("标签不能超过 24 个单位")
            PlaceTagValidation.LimitReached -> setTagError("最多选择 8 个标签")
        }
    }

    private fun removeEditTag(name: String) {
        val draft = mutableState.value.detailDraft ?: return
        if (draft.isSaving) return
        mutableState.value = mutableState.value.copy(
            detailDraft = draft.copy(selectedTagNames = draft.selectedTagNames - name, errorMessage = null),
        )
    }

    private fun setTagError(message: String) {
        mutableState.value = mutableState.value.copy(
            detailDraft = mutableState.value.detailDraft?.copy(errorMessage = message),
        )
    }

    private fun cancelEdit() {
        if (mutableState.value.detailDraft?.isSaving == true) return
        detailEditGeneration++
        mutableState.value = mutableState.value.copy(detailDraft = null)
    }

    private fun saveEdit() {
        val draft = mutableState.value.detailDraft ?: return
        if (draft.isSaving) return
        val generation = ++detailEditGeneration
        mutableState.value = mutableState.value.copy(detailDraft = draft.copy(isSaving = true, errorMessage = null))
        viewModelScope.launch {
            try {
                repository.updateDetails(draft.placeId, draft.note, draft.selectedTagNames)
                if (isCurrentDetailEdit(draft.placeId, generation)) {
                    mutableState.value = mutableState.value.copy(detailDraft = null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (isCurrentDetailEdit(draft.placeId, generation)) {
                    mutableState.value = mutableState.value.copy(
                        detailDraft = mutableState.value.detailDraft?.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "保存失败，请重试",
                        ),
                    )
                }
            }
        }
    }

    private fun isCurrentDetailEdit(placeId: String, generation: Long): Boolean =
        generation == detailEditGeneration && mutableState.value.detailDraft?.placeId == placeId

    private fun toggleCollection(poiId: String) {
        val candidate = reducer.state.value.results.firstOrNull { it.poiId == poiId } ?: return
        if (poiId in mutableState.value.collectionBusyPoiIds) return
        viewModelScope.launch {
            updateBusy(poiId, true)
            mutableState.value = mutableState.value.copy(collectionError = null, collectionErrorPoiId = null)
            try {
                val saved = savedByPoiId[poiId]
                val impact = saved?.let { service.deletionImpact(it.id) }
                when (decideCollectionToggle(candidate, saved, impact)) {
                    CollectionDecision.Save -> {
                        repository.save(tripId, candidate)
                        recentlyCollectedPoiIds += poiId
                    }
                    is CollectionDecision.RemoveNow -> {
                        service.deletePlaceAndReferences(saved!!.id)
                        recentlyCollectedPoiIds -= poiId
                    }
                    is CollectionDecision.Confirm -> mutableState.value = mutableState.value.copy(
                        pendingCollectionRemoval = PendingCollectionRemoval(candidate, saved!!, impact!!),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = mutableState.value.copy(
                    collectionError = error.message ?: "收藏操作失败，请重试",
                    collectionErrorPoiId = poiId,
                )
            } finally {
                updateBusy(poiId, false)
            }
        }
    }

    private fun confirmRemoval() {
        val pending = mutableState.value.pendingCollectionRemoval ?: return
        val poiId = pending.candidate.poiId
        if (poiId in mutableState.value.collectionBusyPoiIds) return
        updateBusy(poiId, true)
        mutableState.value = mutableState.value.copy(
            collectionError = null,
            collectionErrorPoiId = null,
        )
        viewModelScope.launch {
            try {
                service.deletePlaceAndReferences(pending.place.id)
                recentlyCollectedPoiIds -= poiId
                mutableState.value = mutableState.value.copy(
                    pendingCollectionRemoval = null,
                    collectionError = null,
                    collectionErrorPoiId = null,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = mutableState.value.copy(
                    collectionError = error.message ?: "取消收藏失败，请重试",
                    collectionErrorPoiId = poiId,
                )
            } finally {
                updateBusy(poiId, false)
            }
        }
    }

    private fun dismissRemovalConfirmation() {
        val pending = mutableState.value.pendingCollectionRemoval ?: return
        if (pending.candidate.poiId in mutableState.value.collectionBusyPoiIds) return
        mutableState.value = mutableState.value.copy(
            pendingCollectionRemoval = null,
            collectionError = null,
            collectionErrorPoiId = null,
        )
    }

    private fun updateBusy(poiId: String, busy: Boolean) {
        val current = mutableState.value.collectionBusyPoiIds
        mutableState.value = mutableState.value.copy(
            collectionBusyPoiIds = if (busy) current + poiId else current - poiId,
        )
    }

    class Factory(
        private val tripId: String,
        private val repository: SavedPlaceRepository,
        private val source: PlaceSearchDataSource?,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaceSearchViewModel(tripId, repository, source, savedStateHandle) as T
    }
}
