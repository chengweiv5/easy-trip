package com.yangchengwei.easytrip.place.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact
import com.yangchengwei.easytrip.place.domain.PlaceService
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedPlaceRowUi(
    val place: SavedPlace,
    val itineraryOccurrenceCount: Int,
    val scheduled: Boolean,
    val recentlyCollected: Boolean = false,
) {
    val id get() = place.id
    val name get() = place.name
    val address get() = place.address
    val note get() = place.note.ifBlank { null }
    val tags get() = place.tags.map(PlaceTag::name)
}

data class PlaceDetailDraft(
    val note: String,
    val tags: Set<String>,
    val placeId: String,
    val newTagInput: String = "",
)

data class PlacePoolUiState(
    val search: PlaceSearchState = PlaceSearchState(),
    val rows: List<SavedPlaceRowUi> = emptyList(),
    val tags: List<PlaceTag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val savedPoiIds: Set<String> = emptySet(),
    val editing: SavedPlace? = null,
    val detailDraft: PlaceDetailDraft? = null,
    val detailSaving: Boolean = false,
    val detailSaveError: String? = null,
    val deleting: SavedPlace? = null,
    val deletionImpact: PlaceDeletionImpact? = null,
    val deletionBusy: Boolean = false,
    val deletionError: String? = null,
    val pendingCollectionRemoval: PendingCollectionRemoval? = null,
    val collectionBusyPoiIds: Set<String> = emptySet(),
    val collectionError: String? = null,
    val recentlyCollectedPoiIds: Set<String> = emptySet(),
)

class PlacePoolViewModel(private val tripId: String, private val repository: SavedPlaceRepository, searchSource: PlaceSearchDataSource?, private val service: PlaceService = PlaceService(repository)) : ViewModel() {
    private val reducer = PlaceSearchReducer(searchSource, viewModelScope, Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlacePoolUiState())
    val state: StateFlow<PlacePoolUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { reducer.state.collect { search -> mutableState.update { it.copy(search = search) } } }
        viewModelScope.launch {
            combine(repository.observeTags(tripId), repository.observeSavedPoiIds(tripId)) { tags, savedPoiIds -> tags to savedPoiIds }
                .collect { (tags, savedPoiIds) ->
                    val previous = mutableState.value.selectedTagIds
                    val selected = previous.intersect(tags.mapTo(mutableSetOf(), PlaceTag::id))
                    mutableState.update { it.copy(tags = tags, selectedTagIds = selected, savedPoiIds = savedPoiIds) }
                    if (selected != previous) observePlaces()
                }
        }
        observePlaces()
    }
    fun setSearchSource(value: PlaceSearchDataSource?) = reducer.setSource(value)
    fun setQuery(value: String) = reducer.setQuery(value)
    fun clearSearch() = reducer.clear()
    fun toggleTag(id: String) { val selected = mutableState.value.selectedTagIds; mutableState.value = mutableState.value.copy(selectedTagIds = if (id in selected) selected - id else selected + id); observePlaces() }
    fun save(candidate: PlaceCandidate) { viewModelScope.launch { repository.save(tripId, candidate) } }
    private var collectionGeneration = 0L
    fun toggleCollection(candidate: PlaceCandidate) {
        val state = mutableState.value
        val confirmedRemovalBusy = state.pendingCollectionRemoval
            ?.candidate
            ?.poiId
            ?.let { it in state.collectionBusyPoiIds } == true
        if (candidate.point == null || candidate.poiId in state.collectionBusyPoiIds || state.deletionBusy || confirmedRemovalBusy) return
        val generation = ++collectionGeneration
        viewModelScope.launch {
            updateCollectionBusy(candidate.poiId, true)
            mutableState.value = mutableState.value.copy(collectionError = null)
            try {
                val saved = savedByPoiId[candidate.poiId]
                val impact = saved?.let { service.deletionImpact(it.id) }
                if (!isCurrentCollection(candidate.poiId, generation)) return@launch
                when (decideCollectionToggle(candidate, saved, impact)) {
                    CollectionDecision.Save -> repository.save(tripId, candidate)
                    is CollectionDecision.RemoveNow -> service.deletePlaceAndReferences(saved!!.id)
                    is CollectionDecision.Confirm -> mutableState.value = mutableState.value.copy(
                        pendingCollectionRemoval = PendingCollectionRemoval(candidate, saved!!, impact!!),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (isCurrentCollection(candidate.poiId, generation)) {
                    mutableState.value = mutableState.value.copy(collectionError = error.message ?: "收藏操作失败，请重试")
                }
            } finally {
                updateCollectionBusy(candidate.poiId, false)
            }
        }
    }
    fun confirmCollectionRemoval() {
        val pending = mutableState.value.pendingCollectionRemoval ?: return
        if (pending.candidate.poiId in mutableState.value.collectionBusyPoiIds || mutableState.value.deletionBusy) return
        val generation = ++collectionGeneration
        updateCollectionBusy(pending.candidate.poiId, true)
        mutableState.value = mutableState.value.copy(collectionError = null)
        viewModelScope.launch {
            try {
                service.deletePlaceAndReferences(pending.place.id)
                if (isCurrentCollection(pending.candidate.poiId, generation, pending.place.id)) {
                    mutableState.value = mutableState.value.copy(pendingCollectionRemoval = null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (isCurrentCollection(pending.candidate.poiId, generation, pending.place.id)) {
                    mutableState.value = mutableState.value.copy(collectionError = error.message ?: "取消收藏失败，请重试")
                }
            } finally {
                updateCollectionBusy(pending.candidate.poiId, false)
            }
        }
    }
    fun dismissCollectionRemoval() {
        val pending = mutableState.value.pendingCollectionRemoval ?: return
        if (pending.candidate.poiId in mutableState.value.collectionBusyPoiIds) return
        collectionGeneration++
        mutableState.value = mutableState.value.copy(pendingCollectionRemoval = null, collectionError = null)
    }
    private fun isCurrentCollection(poiId: String, generation: Long, placeId: String? = null): Boolean =
        generation == collectionGeneration &&
            (placeId == null || mutableState.value.pendingCollectionRemoval?.place?.id == placeId) &&
            poiId in mutableState.value.collectionBusyPoiIds
    private var detailEditGeneration = 0L
    fun edit(value: SavedPlace) {
        detailEditGeneration++
        mutableState.value = mutableState.value.copy(
            editing = value,
            detailDraft = PlaceDetailDraft(value.note, value.tags.mapTo(mutableSetOf(), PlaceTag::name), value.id),
            detailSaveError = null,
        )
    }
    fun updateDetailDraft(note: String, tags: Set<String>) {
        val draft = mutableState.value.detailDraft ?: return
        if (mutableState.value.detailSaving) return
        mutableState.value = mutableState.value.copy(
            detailDraft = draft.copy(note = note, tags = tags),
            detailSaveError = null,
        )
    }
    fun updateNewTagInput(value: String) {
        val draft = mutableState.value.detailDraft ?: return
        if (mutableState.value.detailSaving) return
        mutableState.value = mutableState.value.copy(
            detailDraft = draft.copy(newTagInput = value),
            detailSaveError = null,
        )
    }
    fun addNewTag() {
        val draft = mutableState.value.detailDraft ?: return
        if (mutableState.value.detailSaving) return
        when (val validation = validatePlaceTag(draft.newTagInput, draft.tags)) {
            is PlaceTagValidation.Valid -> mutableState.value = mutableState.value.copy(
                detailDraft = draft.copy(tags = draft.tags + validation.name, newTagInput = ""),
                detailSaveError = null,
            )
            PlaceTagValidation.Empty -> Unit
            PlaceTagValidation.Duplicate -> mutableState.value = mutableState.value.copy(detailSaveError = "标签已存在")
            PlaceTagValidation.TooLong -> mutableState.value = mutableState.value.copy(detailSaveError = "标签不能超过 24 个单位")
            PlaceTagValidation.LimitReached -> mutableState.value = mutableState.value.copy(detailSaveError = "最多选择 8 个标签")
        }
    }
    fun removeTag(name: String) {
        val draft = mutableState.value.detailDraft ?: return
        if (mutableState.value.detailSaving) return
        mutableState.value = mutableState.value.copy(
            detailDraft = draft.copy(tags = draft.tags - name),
            detailSaveError = null,
        )
    }
    fun dismissEdit() {
        detailEditGeneration++
        mutableState.value = mutableState.value.copy(editing = null, detailDraft = null, detailSaving = false, detailSaveError = null)
    }
    fun updateDetails(note: String, tags: Set<String>) {
        updateDetailDraft(note, tags)
        updateDetails()
    }
    fun updateDetails() {
        val draft = mutableState.value.detailDraft ?: return
        if (mutableState.value.detailSaving) return
        val generation = ++detailEditGeneration
        mutableState.value = mutableState.value.copy(detailSaving = true, detailSaveError = null)
        viewModelScope.launch {
            try {
                repository.updateDetails(draft.placeId, draft.note, draft.tags)
                if (isCurrentDetailEdit(draft.placeId, generation)) dismissEdit()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (isCurrentDetailEdit(draft.placeId, generation)) {
                    mutableState.value = mutableState.value.copy(
                        detailSaving = false,
                        detailSaveError = error.message ?: "保存失败，请重试",
                    )
                }
            }
        }
    }
    private fun isCurrentDetailEdit(placeId: String, generation: Long): Boolean =
        generation == detailEditGeneration && mutableState.value.detailDraft?.placeId == placeId
    private var deletePreparationJob: Job? = null
    private var deleteGeneration = 0L
    fun requestDelete(place: SavedPlace) {
        if (mutableState.value.deletionBusy || mutableState.value.collectionBusyPoiIds.isNotEmpty()) return
        deletePreparationJob?.cancel()
        val retryingImpact = mutableState.value.deleting?.id == place.id &&
            mutableState.value.deletionImpact == null
        val generation = ++deleteGeneration
        mutableState.value = mutableState.value.copy(
            editing = null,
            detailDraft = null,
            detailSaving = false,
            detailSaveError = null,
            deleting = if (retryingImpact) place else null,
            deletionImpact = null,
            deletionBusy = retryingImpact,
            deletionError = null,
        )
        deletePreparationJob = viewModelScope.launch {
            try {
                val impact = service.deletionImpact(place.id)
                if (!isCurrentDelete(place.id, generation, allowPreparing = true)) return@launch
                mutableState.value = mutableState.value.copy(deletionBusy = false)
                if (impact.itineraryItemCount == 0 && impact.routeLegCount == 0) {
                    mutableState.value = mutableState.value.copy(
                        deleting = place,
                        deletionImpact = impact,
                        deletionBusy = true,
                    )
                    service.deletePlaceAndReferences(place.id)
                    if (isCurrentDelete(place.id, generation)) clearDeleteState()
                } else {
                    mutableState.value = mutableState.value.copy(deleting = place, deletionImpact = impact)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation == deleteGeneration) {
                    mutableState.value = mutableState.value.copy(
                        deleting = place,
                        deletionBusy = false,
                        deletionError = error.message ?: "删除失败，请重试",
                    )
                }
            } finally {
                if (generation == deleteGeneration) deletePreparationJob = null
            }
        }
    }
    fun dismissDelete() {
        if (mutableState.value.deletionBusy) return
        deletePreparationJob?.cancel()
        deletePreparationJob = null
        deleteGeneration++
        clearDeleteState()
    }
    fun confirmDelete() {
        val place = mutableState.value.deleting ?: return
        if (mutableState.value.deletionBusy || mutableState.value.deletionImpact == null) {
            if (mutableState.value.deletionImpact == null) requestDelete(place)
            return
        }
        val generation = ++deleteGeneration
        mutableState.value = mutableState.value.copy(deletionBusy = true, deletionError = null)
        viewModelScope.launch {
            try {
                service.deletePlaceAndReferences(place.id)
                if (isCurrentDelete(place.id, generation)) clearDeleteState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (isCurrentDelete(place.id, generation)) {
                    mutableState.value = mutableState.value.copy(
                        deletionBusy = false,
                        deletionError = error.message ?: "删除失败，请重试",
                    )
                }
            }
        }
    }
    private fun isCurrentDelete(placeId: String, generation: Long, allowPreparing: Boolean = false): Boolean =
        generation == deleteGeneration &&
            (mutableState.value.deleting?.id == placeId || allowPreparing && mutableState.value.deleting == null)
    private fun clearDeleteState() {
        mutableState.value = mutableState.value.copy(
            deleting = null,
            deletionImpact = null,
            deletionBusy = false,
            deletionError = null,
        )
    }
    fun dismissDialogs() {
        if (mutableState.value.deletionBusy || mutableState.value.collectionBusyPoiIds.isNotEmpty()) return
        dismissEdit()
        dismissDelete()
        dismissCollectionRemoval()
    }
    fun dispatch(action: PlacePoolAction) {
        when (action) {
            is PlacePoolAction.SetQuery -> setQuery(action.value)
            is PlacePoolAction.ToggleTag -> toggleTag(action.id)
            is PlacePoolAction.Edit -> edit(action.place)
            is PlacePoolAction.Delete -> requestDelete(action.place)
            is PlacePoolAction.ToggleCollection -> toggleCollection(action.candidate)
            is PlacePoolAction.UpdateDraft -> updateDetailDraft(action.note, action.tags)
            is PlacePoolAction.UpdateNewTagInput -> updateNewTagInput(action.value)
            PlacePoolAction.AddTag -> addNewTag()
            is PlacePoolAction.RemoveTag -> removeTag(action.name)
            is PlacePoolAction.UpdateDetails -> updateDetails(action.note, action.tags)
            PlacePoolAction.StartAddToItinerary,
            is PlacePoolAction.StartAddSingle -> Unit
            PlacePoolAction.ConfirmCollectionRemoval -> confirmCollectionRemoval()
            PlacePoolAction.ConfirmDelete -> confirmDelete()
            PlacePoolAction.DismissDialogs -> dismissDialogs()
        }
    }
    private var savedByPoiId: Map<String, SavedPlace> = emptyMap()
    private var placesJob: kotlinx.coroutines.Job? = null
    private var allPlacesJob: kotlinx.coroutines.Job? = null
    private fun observePlaces() {
        placesJob?.cancel()
        placesJob = viewModelScope.launch {
            repository.observePlacesWithUsage(tripId, mutableState.value.selectedTagIds)
                .collect { placesWithUsage ->
                    val places = placesWithUsage.map { it.first }
                    reducer.setSavedPlaces(places)
                    mutableState.update {
                        it.copy(
                            rows = placesWithUsage.map { (place, count) ->
                                SavedPlaceRowUi(place, count, count > 0)
                            },
                        )
                    }
                }
        }
        if (allPlacesJob == null) {
            allPlacesJob = viewModelScope.launch {
                repository.observePlaces(tripId, emptySet()).collect { places ->
                    savedByPoiId = places.associateBy(SavedPlace::amapPoiId)
                }
            }
        }
    }
    private fun updateCollectionBusy(poiId: String, busy: Boolean) {
        val ids = mutableState.value.collectionBusyPoiIds
        mutableState.value = mutableState.value.copy(
            collectionBusyPoiIds = if (busy) ids + poiId else ids - poiId,
        )
    }

    class Factory(private val tripId: String, private val repository: SavedPlaceRepository, private val source: PlaceSearchDataSource?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = PlacePoolViewModel(tripId, repository, source) as T
    }
}
