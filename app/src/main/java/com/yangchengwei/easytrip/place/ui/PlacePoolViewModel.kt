package com.yangchengwei.easytrip.place.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
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
    val id: String,
    val name: String,
    val address: String,
    val note: String?,
    val tags: List<String>,
    val itineraryOccurrenceCount: Int,
    val selected: Boolean,
)

data class PlaceDetailDraft(val note: String, val tags: Set<String>)

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
    val deletionUsageCount: Int = 0,
    val pendingCollectionRemoval: PendingCollectionRemoval? = null,
    val collectionBusyPoiIds: Set<String> = emptySet(),
    val collectionError: String? = null,
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
    fun toggleCollection(candidate: PlaceCandidate) {
        if (candidate.point == null || candidate.poiId in mutableState.value.collectionBusyPoiIds) return
        viewModelScope.launch {
            updateCollectionBusy(candidate.poiId, true)
            mutableState.value = mutableState.value.copy(collectionError = null)
            try {
                val saved = savedByPoiId[candidate.poiId]
                val usageCount = saved?.let { service.deletionUsageCount(it.id) }
                when (decideCollectionToggle(candidate, saved, usageCount)) {
                    CollectionDecision.Save -> repository.save(tripId, candidate)
                    is CollectionDecision.RemoveNow -> service.deletePlaceAndReferences(saved!!.id)
                    is CollectionDecision.Confirm -> mutableState.value = mutableState.value.copy(
                        pendingCollectionRemoval = PendingCollectionRemoval(candidate, saved!!, usageCount!!),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = mutableState.value.copy(collectionError = error.message ?: "收藏操作失败，请重试")
            } finally {
                updateCollectionBusy(candidate.poiId, false)
            }
        }
    }
    fun confirmCollectionRemoval() {
        val pending = mutableState.value.pendingCollectionRemoval ?: return
        if (pending.candidate.poiId in mutableState.value.collectionBusyPoiIds) return
        viewModelScope.launch {
            updateCollectionBusy(pending.candidate.poiId, true)
            mutableState.value = mutableState.value.copy(collectionError = null)
            try {
                service.deletePlaceAndReferences(pending.place.id)
                mutableState.value = mutableState.value.copy(pendingCollectionRemoval = null)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = mutableState.value.copy(collectionError = error.message ?: "取消收藏失败，请重试")
            } finally {
                updateCollectionBusy(pending.candidate.poiId, false)
            }
        }
    }
    fun dismissCollectionRemoval() {
        mutableState.value = mutableState.value.copy(pendingCollectionRemoval = null, collectionError = null)
    }
    fun edit(value: SavedPlace) {
        mutableState.value = mutableState.value.copy(
            editing = value,
            detailDraft = PlaceDetailDraft(value.note, value.tags.mapTo(mutableSetOf(), PlaceTag::name)),
            detailSaveError = null,
        )
    }
    fun updateDetailDraft(note: String, tags: Set<String>) {
        if (mutableState.value.editing == null) return
        mutableState.value = mutableState.value.copy(detailDraft = PlaceDetailDraft(note, tags), detailSaveError = null)
    }
    fun dismissEdit() { mutableState.value = mutableState.value.copy(editing = null, detailDraft = null, detailSaving = false, detailSaveError = null) }
    fun updateDetails(note: String, tags: Set<String>) {
        val place = mutableState.value.editing ?: return
        updateDetailDraft(note, tags)
        mutableState.value = mutableState.value.copy(detailSaving = true)
        viewModelScope.launch {
            try {
                repository.updateDetails(place.id, note, tags)
                dismissEdit()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = mutableState.value.copy(
                    detailSaving = false,
                    detailSaveError = error.message ?: "保存失败，请重试",
                )
            }
        }
    }
    private var deletePreparationJob: Job? = null
    private var deletePreparationId = 0L
    fun requestDelete(place: SavedPlace) {
        deletePreparationJob?.cancel()
        val requestId = ++deletePreparationId
        mutableState.value = mutableState.value.copy(deleting = null, deletionUsageCount = 0)
        deletePreparationJob = viewModelScope.launch {
            val usageCount = service.deletionUsageCount(place.id)
            if (requestId == deletePreparationId) {
                mutableState.value = mutableState.value.copy(deleting = place, deletionUsageCount = usageCount)
                deletePreparationJob = null
            }
        }
    }
    fun dismissDelete() {
        deletePreparationJob?.cancel()
        deletePreparationJob = null
        deletePreparationId++
        mutableState.value = mutableState.value.copy(deleting = null, deletionUsageCount = 0)
    }
    fun confirmDelete() { val place = mutableState.value.deleting ?: return; viewModelScope.launch { service.deletePlaceAndReferences(place.id); dismissDelete() } }
    fun dismissDialogs() {
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
            is PlacePoolAction.UpdateDetails -> updateDetails(action.note, action.tags)
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
            repository.observePlaces(tripId, mutableState.value.selectedTagIds).collect { places ->
                reducer.setSavedPlaces(places)
                val previousCounts = mutableState.value.rows.associate { it.id to it.itineraryOccurrenceCount }
                val rows = places.map { place ->
                    val count = previousCounts[place.id] ?: 0
                    SavedPlaceRowUi(
                        id = place.id,
                        name = place.name,
                        address = place.address,
                        note = place.note.ifBlank { null },
                        tags = place.tags.map(PlaceTag::name),
                        itineraryOccurrenceCount = count,
                        selected = count > 0,
                    )
                }
                mutableState.update { it.copy(rows = rows) }
                places.forEach { place ->
                    val count = service.deletionUsageCount(place.id)
                    mutableState.update { current ->
                        current.copy(
                            rows = current.rows.map { row ->
                                if (row.id == place.id) row.copy(itineraryOccurrenceCount = count, selected = count > 0) else row
                            },
                        )
                    }
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
