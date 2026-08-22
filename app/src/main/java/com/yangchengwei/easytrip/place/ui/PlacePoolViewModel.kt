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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlacePoolUiState(
    val search: PlaceSearchState = PlaceSearchState(),
    val tags: List<PlaceTag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val savedPoiIds: Set<String> = emptySet(),
    val editing: SavedPlace? = null,
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
        viewModelScope.launch { reducer.state.collect { mutableState.value = mutableState.value.copy(search = it) } }
        viewModelScope.launch {
            repository.observeTags(tripId).collect { tags ->
                val validIds = tags.mapTo(mutableSetOf(), PlaceTag::id)
                val previous = mutableState.value.selectedTagIds
                val selected = previous.intersect(validIds)
                mutableState.value = mutableState.value.copy(tags = tags, selectedTagIds = selected)
                if (selected != previous) observePlaces()
            }
        }
        viewModelScope.launch {
            repository.observeSavedPoiIds(tripId).collect { mutableState.value = mutableState.value.copy(savedPoiIds = it) }
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
    fun edit(value: SavedPlace) { mutableState.value = mutableState.value.copy(editing = value) }
    fun dismissEdit() { mutableState.value = mutableState.value.copy(editing = null) }
    fun updateDetails(note: String, tags: Set<String>) { val place = mutableState.value.editing ?: return; viewModelScope.launch { repository.updateDetails(place.id, note, tags); dismissEdit() } }
    fun requestDelete(place: SavedPlace) { viewModelScope.launch { mutableState.value = mutableState.value.copy(deleting = place, deletionUsageCount = service.deletionUsageCount(place.id)) } }
    fun dismissDelete() { mutableState.value = mutableState.value.copy(deleting = null, deletionUsageCount = 0) }
    fun confirmDelete() { val place = mutableState.value.deleting ?: return; viewModelScope.launch { service.deletePlaceAndReferences(place.id); dismissDelete() } }
    private var savedByPoiId: Map<String, SavedPlace> = emptyMap()
    private var placesJob: kotlinx.coroutines.Job? = null
    private var allPlacesJob: kotlinx.coroutines.Job? = null
    private fun observePlaces() {
        placesJob?.cancel()
        placesJob = viewModelScope.launch {
            repository.observePlaces(tripId, mutableState.value.selectedTagIds).collect(reducer::setSavedPlaces)
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
