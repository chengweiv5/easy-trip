package com.yangchengwei.easytrip.itinerary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class DayItineraryUiState(
    val days: List<TripDay> = emptyList(),
    val selectedDayId: String? = null,
    val savedPlaces: List<SavedPlace> = emptyList(),
    val items: List<ItineraryItemUi> = emptyList(),
    val legs: List<RouteLegUi> = emptyList(),
    val previewOrder: List<String> = emptyList(),
    val timingItemId: String? = null,
    val moveItemId: String? = null,
    val deleteItemId: String? = null,
    val modeLegId: String? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DayItineraryViewModel(
    private val tripId: String,
    private val trips: TripRepository,
    private val itineraries: ItineraryRepository,
    private val routeLegs: RouteLegRepository,
    private var coordinator: RouteRefreshCoordinator?,
    savedPlaces: Flow<List<SavedPlace>> = emptyFlow(),
    selectedDays: Flow<String?> = emptyFlow(),
) : ViewModel() {
    private val selectedDay = MutableStateFlow<String?>(null)
    private var hasExternalSelection = false
    private var externalSelectedDayId: String? = null
    private val mutable = MutableStateFlow(DayItineraryUiState())
    val state: StateFlow<DayItineraryUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            trips.observeTrip(tripId).filterNotNull().collect { trip ->
                val chosen = if (hasExternalSelection) {
                    externalSelectedDayId?.takeIf { id -> trip.days.any { it.id == id } }
                } else {
                    selectedDay.value?.takeIf { id -> trip.days.any { it.id == id } } ?: trip.days.firstOrNull()?.id
                }
                selectedDay.value = chosen
                mutable.value = mutable.value.copy(days = trip.days, selectedDayId = chosen)
                if (chosen == null) clearDay()
            }
        }
        viewModelScope.launch {
            selectedDay.flatMapLatest { dayId ->
                if (dayId == null) flowOf(null)
                else combine(itineraries.observeDay(dayId), routeLegs.observeDay(dayId)) { day, legs -> day to legs }
            }.collect { dayAndLegs ->
                if (dayAndLegs == null) clearDay()
                else applyDay(dayAndLegs.first, dayAndLegs.second)
            }
        }
        viewModelScope.launch {
            savedPlaces.collect { mutable.value = mutable.value.copy(savedPlaces = it) }
        }
        viewModelScope.launch {
            selectedDays.collect {
                hasExternalSelection = true
                externalSelectedDayId = it
                selectDay(it)
            }
        }
    }

    fun selectDay(id: String?) {
        if (id == selectedDay.value) return
        selectedDay.value = id
        clearDay(id)
    }

    fun addPlace(placeId: String) {
        val day = state.value.selectedDayId ?: return
        val index = state.value.items.size
        viewModelScope.launch { runCatching { itineraries.addItem(day, placeId, index) }.onFailure(::showError) }
    }

    fun previewMove(itemId: String, target: Int) {
        val order = mutable.value.previewOrder.toMutableList()
        val old = order.indexOf(itemId)
        if (old < 0 || target !in order.indices) return
        order.add(target, order.removeAt(old))
        mutable.value = mutable.value.copy(previewOrder = order)
    }

    fun commitMove(itemId: String, target: Int) {
        val day = state.value.selectedDayId ?: return
        viewModelScope.launch {
            runCatching { itineraries.moveItem(itemId, day, target) }
                .onFailure { mutable.value = mutable.value.copy(previewOrder = mutable.value.items.map(ItineraryItemUi::id)); showError(it) }
        }
    }

    fun requestCrossDay(itemId: String) { mutable.value = mutable.value.copy(moveItemId = itemId) }
    fun moveToDay(dayId: String) {
        val item = state.value.moveItemId ?: return
        mutable.value = mutable.value.copy(moveItemId = null)
        viewModelScope.launch { runCatching { itineraries.moveItem(item, dayId, 0) }.onFailure(::showError) }
    }

    fun requestDelete(itemId: String) { mutable.value = mutable.value.copy(deleteItemId = itemId) }
    fun confirmDelete() {
        val item = state.value.deleteItemId ?: return
        mutable.value = mutable.value.copy(deleteItemId = null)
        viewModelScope.launch { runCatching { itineraries.deleteItem(item) }.onFailure(::showError) }
    }

    fun requestTiming(itemId: String) { mutable.value = mutable.value.copy(timingItemId = itemId) }
    fun saveTiming(time: LocalTime?, minutes: Int?) {
        val item = state.value.timingItemId ?: return
        mutable.value = mutable.value.copy(timingItemId = null)
        viewModelScope.launch { runCatching { itineraries.updateTiming(item, time, minutes) }.onFailure(::showError) }
    }

    fun setRouteCoordinator(value: RouteRefreshCoordinator?) { coordinator = value }
    fun requestMode(legId: String) { mutable.value = mutable.value.copy(modeLegId = legId) }
    fun overrideMode(mode: TransportMode) {
        val leg = state.value.modeLegId ?: return
        mutable.value = mutable.value.copy(modeLegId = null)
        viewModelScope.launch {
            runCatching { coordinator?.overrideMode(leg, mode) ?: false }
                .onSuccess { if (!it) showError(IllegalStateException("联网并同意高德隐私政策后才能更新交通方式")) }
                .onFailure(::showError)
        }
    }

    fun retry(legId: String) {
        viewModelScope.launch {
            runCatching { coordinator?.retry(legId) ?: false }
                .onSuccess { if (!it) showError(IllegalStateException("联网并同意高德隐私政策后才能重试")) }
                .onFailure(::showError)
        }
    }

    fun dismissDialogs() {
        mutable.value = mutable.value.copy(timingItemId = null, moveItemId = null, deleteItemId = null, modeLegId = null)
    }

    private fun clearDay(selectedDayId: String? = null) {
        mutable.value = mutable.value.copy(
            selectedDayId = selectedDayId,
            items = emptyList(),
            legs = emptyList(),
            previewOrder = emptyList(),
            timingItemId = null,
            moveItemId = null,
            deleteItemId = null,
            modeLegId = null,
        )
    }

    private fun applyDay(day: DayItinerary, legs: List<RouteLegEntity>) {
        val items = day.items.map { it.toItineraryItemUi() }
        val previous = mutable.value
        val officialOrder = items.map(ItineraryItemUi::id)
        val keepPreview = previous.items.map(ItineraryItemUi::id) == officialOrder && previous.previewOrder.toSet() == officialOrder.toSet()
        mutable.value = previous.copy(
            items = items,
            previewOrder = if (keepPreview) previous.previewOrder else officialOrder,
            legs = legs.map { it.toRouteLegUi() },
        )
    }

    private fun showError(t: Throwable) { mutable.value = mutable.value.copy(error = t.message ?: "操作失败") }

    class Factory(
        private val tripId: String,
        private val trips: TripRepository,
        private val itineraries: ItineraryRepository,
        private val routeLegs: RouteLegRepository,
        private val coordinator: RouteRefreshCoordinator?,
        private val savedPlaces: Flow<List<SavedPlace>>,
        private val selectedDays: Flow<String?> = emptyFlow(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DayItineraryViewModel(tripId, trips, itineraries, routeLegs, coordinator, savedPlaces, selectedDays) as T
    }
}
