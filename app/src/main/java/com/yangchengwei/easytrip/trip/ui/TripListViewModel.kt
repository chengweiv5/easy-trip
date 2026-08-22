package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class CreateTimeMode { DRAFT, DATED }
data class TripListUiState(
    val trips: List<TripSummary> = emptyList(),
    val showCreateDialog: Boolean = false,
    val createName: String = "",
    val createDays: String = "",
    val createTimeMode: CreateTimeMode = CreateTimeMode.DRAFT,
    val createStartDate: LocalDate? = null,
    val createTravelMode: TravelMode = TravelMode.FLEXIBLE,
    val pendingDelete: TripSummary? = null,
    val pendingDeleteImpact: TripDeleteImpact? = null,
)
sealed interface TripListNavigation {
    data class OpenWorkspace(val tripId: String) : TripListNavigation
    data class OpenSettings(val tripId: String) : TripListNavigation
}

class TripListViewModel(private val service: TripService, repository: TripRepository, private val impacts: DeleteImpactProvider) : ViewModel() {
    private val mutableState = MutableStateFlow(TripListUiState())
    val state: StateFlow<TripListUiState> = mutableState.asStateFlow()
    private val navigationChannel = Channel<TripListNavigation>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()

    init { viewModelScope.launch { repository.observeTrips().collect { mutableState.value = mutableState.value.copy(trips = it) } } }
    fun showCreate() { mutableState.value = TripListUiState(trips = mutableState.value.trips, showCreateDialog = true) }
    fun dismissCreate() { mutableState.value = mutableState.value.copy(showCreateDialog = false) }
    fun setCreateName(value: String) { mutableState.value = mutableState.value.copy(createName = value) }
    fun setCreateDays(value: String) { mutableState.value = mutableState.value.copy(createDays = value.filter(Char::isDigit)) }
    fun setCreateTimeMode(value: CreateTimeMode) {
        mutableState.value = mutableState.value.copy(
            createTimeMode = value,
            createStartDate = if (value == CreateTimeMode.DRAFT) null else mutableState.value.createStartDate,
        )
    }
    fun setCreateStartDate(value: LocalDate) { mutableState.value = mutableState.value.copy(createStartDate = value) }
    fun setCreateTravelMode(value: TravelMode) { mutableState.value = mutableState.value.copy(createTravelMode = value) }
    fun create() {
        val current = mutableState.value
        val days = current.createDays.toIntOrNull() ?: return
        if (current.createName.isBlank() || days < 1 || current.createTimeMode == CreateTimeMode.DATED && current.createStartDate == null) return
        viewModelScope.launch {
            val id = service.createTrip(CreateTrip(current.createName, days, current.createTravelMode))
            if (current.createTimeMode == CreateTimeMode.DATED) service.setStartDate(id, current.createStartDate)
            dismissCreate()
        }
    }
    fun openWorkspace(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenWorkspace(id)) } }
    fun openSettings(id: String) { viewModelScope.launch { navigationChannel.send(TripListNavigation.OpenSettings(id)) } }
    fun requestDelete(value: TripSummary) { viewModelScope.launch { mutableState.value = mutableState.value.copy(pendingDelete = value, pendingDeleteImpact = impacts.trip(value.id)) } }
    fun cancelDelete() { mutableState.value = mutableState.value.copy(pendingDelete = null, pendingDeleteImpact = null) }
    fun confirmDelete() { val value = mutableState.value.pendingDelete ?: return; viewModelScope.launch { service.deleteTrip(value.id); cancelDelete() } }

    class Factory(private val service: TripService, private val repository: TripRepository, private val impacts: DeleteImpactProvider) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = TripListViewModel(service, repository, impacts) as T
    }
}
