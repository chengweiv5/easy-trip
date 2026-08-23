package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class TripListUiState(
    val page: TripListPageState = TripListPageState.Loading,
    val trips: List<TripSummary> = emptyList(),
    val pendingDelete: TripSummary? = null,
    val pendingDeleteImpact: TripDeleteImpact? = null,
)

sealed interface TripListNavigation {
    data class OpenWorkspace(val tripId: String) : TripListNavigation
    data class OpenSettings(val tripId: String) : TripListNavigation
}

class TripListViewModel(
    private val service: TripService,
    private val repository: TripRepository,
    private val impacts: DeleteImpactProvider,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TripListUiState())
    val state: StateFlow<TripListUiState> = mutableState.asStateFlow()
    private val navigationChannel = Channel<TripListNavigation>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()
    private var tripsJob: Job? = null

    init { observeTrips() }

    fun observeTrips() {
        tripsJob?.cancel()
        mutableState.value = mutableState.value.copy(page = TripListPageState.Loading)
        tripsJob = viewModelScope.launch {
            repository.observeTrips().catch {
                mutableState.value = mutableState.value.copy(page = TripListPageState.Error("无法加载旅行"))
            }.collect { trips ->
                mutableState.value = mutableState.value.copy(
                    trips = trips,
                    page = if (trips.isEmpty()) TripListPageState.Empty else TripListPageState.Content(trips.map(TripSummary::toTripCardUiModel)),
                )
            }
        }
    }

    fun onAction(action: TripListAction) {
        when (action) {
            TripListAction.CreateTrip -> Unit
            TripListAction.Retry -> observeTrips()
            is TripListAction.OpenTrip -> openWorkspace(action.tripId)
            is TripListAction.OpenSettings -> openSettings(action.tripId)
            is TripListAction.RequestDelete -> mutableState.value.trips.firstOrNull { it.id == action.tripId }?.let(::requestDelete)
        }
    }

    fun openWorkspace(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenWorkspace(id)) } }
    fun openSettings(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenSettings(id)) } }
    fun requestDelete(value: TripSummary) { viewModelScope.launch { mutableState.value = mutableState.value.copy(pendingDelete = value, pendingDeleteImpact = impacts.trip(value.id)) } }
    fun cancelDelete() { mutableState.value = mutableState.value.copy(pendingDelete = null, pendingDeleteImpact = null) }
    fun confirmDelete() { val value = mutableState.value.pendingDelete ?: return; viewModelScope.launch { service.deleteTrip(value.id); cancelDelete() } }

    class Factory(
        private val service: TripService,
        private val repository: TripRepository,
        private val impacts: DeleteImpactProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            TripListViewModel(service, repository, impacts) as T
    }
}
