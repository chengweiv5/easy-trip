package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

data class TripDeleteImpact(val days: Int, val places: Int, val tags: Int, val itineraryItems: Int, val routeLegs: Int)
data class DayDeleteImpact(val itineraryItems: Int, val routeLegs: Int)
interface DeleteImpactProvider {
    suspend fun trip(tripId: String): TripDeleteImpact
    suspend fun day(dayId: String): DayDeleteImpact
}

data class DayUi(val id: String, val label: String)
data class PendingDayDeletion(val day: DayUi, val impact: DayDeleteImpact)
data class TripSettingsUiState(
    val tripId: String,
    val name: String = "",
    val startDate: LocalDate? = null,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val days: List<DayUi> = emptyList(),
    val pendingDayDeletion: PendingDayDeletion? = null,
)

class TripSettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val service: TripService,
    repository: TripRepository,
    private val impacts: DeleteImpactProvider,
) : ViewModel() {
    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val mutableState = MutableStateFlow(TripSettingsUiState(tripId))
    val state: StateFlow<TripSettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTrip(tripId).filterNotNull().collect { trip ->
                mutableState.value = mutableState.value.copy(
                    name = trip.name,
                    startDate = trip.startDate,
                    travelMode = trip.travelMode,
                    days = trip.days.map { DayUi(it.id, service.displayLabel(it, trip.startDate)) },
                )
            }
        }
    }

    fun rename(name: String) { viewModelScope.launch { service.renameTrip(tripId, name) } }
    fun setStartDate(value: LocalDate?) { viewModelScope.launch { service.setStartDate(tripId, value) } }
    fun setTravelMode(value: TravelMode) { viewModelScope.launch { service.setTravelMode(tripId, value) } }
    fun requestDelete(day: DayUi) {
        viewModelScope.launch { mutableState.value = mutableState.value.copy(pendingDayDeletion = PendingDayDeletion(day, impacts.day(day.id))) }
    }
    fun cancelDelete() { mutableState.value = mutableState.value.copy(pendingDayDeletion = null) }
    fun confirmDelete() {
        val day = mutableState.value.pendingDayDeletion?.day ?: return
        viewModelScope.launch { service.deleteDay(day.id); cancelDelete() }
    }

    class Factory(private val service: TripService, private val repository: TripRepository, private val impacts: DeleteImpactProvider) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T =
            TripSettingsViewModel(extras.createSavedStateHandle(), service, repository, impacts) as T
    }
}
