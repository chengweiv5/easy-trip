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
    fun toggleTag(id: String) { val selected = mutableState.value.selectedTagIds; mutableState.value = mutableState.value.copy(selectedTagIds = if (id in selected) selected - id else selected + id); observePlaces() }
    fun save(candidate: PlaceCandidate) { viewModelScope.launch { repository.save(tripId, candidate) } }
    fun edit(value: SavedPlace) { mutableState.value = mutableState.value.copy(editing = value) }
    fun dismissEdit() { mutableState.value = mutableState.value.copy(editing = null) }
    fun updateDetails(note: String, tags: Set<String>) { val place = mutableState.value.editing ?: return; viewModelScope.launch { repository.updateDetails(place.id, note, tags); dismissEdit() } }
    fun requestDelete(place: SavedPlace) { viewModelScope.launch { mutableState.value = mutableState.value.copy(deleting = place, deletionUsageCount = service.deletionUsageCount(place.id)) } }
    fun dismissDelete() { mutableState.value = mutableState.value.copy(deleting = null, deletionUsageCount = 0) }
    fun confirmDelete() { val place = mutableState.value.deleting ?: return; viewModelScope.launch { service.deletePlaceAndReferences(place.id); dismissDelete() } }
    private var placesJob: kotlinx.coroutines.Job? = null
    private fun observePlaces() { placesJob?.cancel(); placesJob = viewModelScope.launch { repository.observePlaces(tripId, mutableState.value.selectedTagIds).collect(reducer::setSavedPlaces) } }

    class Factory(private val tripId: String, private val repository: SavedPlaceRepository, private val source: PlaceSearchDataSource?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = PlacePoolViewModel(tripId, repository, source) as T
    }
}
