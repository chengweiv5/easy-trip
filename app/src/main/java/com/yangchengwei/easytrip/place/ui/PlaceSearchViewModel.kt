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

sealed interface PlaceSearchAction {
    data object Back : PlaceSearchAction
    data class QueryChanged(val value: String) : PlaceSearchAction
    data object Submit : PlaceSearchAction
    data object Retry : PlaceSearchAction
    data class ToggleCollection(val poiId: String) : PlaceSearchAction
    data object DismissRemovalConfirmation : PlaceSearchAction
    data object ConfirmRemoval : PlaceSearchAction
}

data class PlaceSearchUiState(
    val search: PlaceSearchState = PlaceSearchState(),
    val savedPoiIds: Set<String> = emptySet(),
    val collectionBusyPoiIds: Set<String> = emptySet(),
    val pendingCollectionRemoval: PendingCollectionRemoval? = null,
    val collectionError: String? = null,
    val shouldNavigateBack: Boolean = false,
)

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
    private val mutableState = MutableStateFlow(PlaceSearchUiState(search = reducer.state.value))
    val state: StateFlow<PlaceSearchUiState> = mutableState.asStateFlow()
    private var savedByPoiId: Map<String, SavedPlace> = emptyMap()

    init {
        viewModelScope.launch {
            reducer.state.collect { search ->
                savedStateHandle[SEARCH_QUERY_KEY] = search.query
                mutableState.value = mutableState.value.copy(search = search)
            }
        }
        viewModelScope.launch {
            repository.observePlaces(tripId, emptySet()).collect { places ->
                savedByPoiId = places.associateBy(SavedPlace::amapPoiId)
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
            PlaceSearchAction.Back -> mutableState.value = mutableState.value.copy(shouldNavigateBack = true)
            is PlaceSearchAction.QueryChanged -> reducer.setQuery(action.value)
            PlaceSearchAction.Submit -> reducer.submit()
            PlaceSearchAction.Retry -> reducer.retry()
            is PlaceSearchAction.ToggleCollection -> toggleCollection(action.poiId)
            PlaceSearchAction.DismissRemovalConfirmation -> dismissRemovalConfirmation()
            PlaceSearchAction.ConfirmRemoval -> confirmRemoval()
        }
    }

    fun consumeBack() {
        mutableState.value = mutableState.value.copy(shouldNavigateBack = false)
    }

    private fun toggleCollection(poiId: String) {
        val candidate = reducer.state.value.results.firstOrNull { it.poiId == poiId } ?: return
        if (candidate.point == null || poiId in mutableState.value.collectionBusyPoiIds) return
        viewModelScope.launch {
            updateBusy(poiId, true)
            mutableState.value = mutableState.value.copy(collectionError = null)
            try {
                val saved = savedByPoiId[poiId]
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
                updateBusy(poiId, false)
            }
        }
    }

    private fun confirmRemoval() {
        val pending = mutableState.value.pendingCollectionRemoval ?: return
        val poiId = pending.candidate.poiId
        if (poiId in mutableState.value.collectionBusyPoiIds) return
        updateBusy(poiId, true)
        viewModelScope.launch {
            try {
                service.deletePlaceAndReferences(pending.place.id)
                mutableState.value = mutableState.value.copy(pendingCollectionRemoval = null, collectionError = null)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.value = mutableState.value.copy(collectionError = error.message ?: "取消收藏失败，请重试")
            } finally {
                updateBusy(poiId, false)
            }
        }
    }

    private fun dismissRemovalConfirmation() {
        val pending = mutableState.value.pendingCollectionRemoval ?: return
        if (pending.candidate.poiId in mutableState.value.collectionBusyPoiIds) return
        mutableState.value = mutableState.value.copy(pendingCollectionRemoval = null, collectionError = null)
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
