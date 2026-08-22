package com.yangchengwei.easytrip.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

enum class WorkspaceTab { PLACES, ITINERARY }
enum class WorkspaceSheetLevel { COLLAPSED, HALF, EXPANDED }

data class TripWorkspaceUiState(
    val tripName: String = "",
    val days: List<TripDay> = emptyList(),
    val selectedDayId: String? = null,
    val tab: WorkspaceTab = WorkspaceTab.PLACES,
    val mapScope: MapScope = MapScope.PLACE_POOL,
    val sheetLevel: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF,
    val map: MapUiModel = MapUiModel(),
    val selectedMarker: MapMarkerUi? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TripWorkspaceViewModel(
    private val tripId: String,
    private val trips: TripRepository,
    places: SavedPlaceRepository,
    itineraries: ItineraryRepository,
    private val routes: RouteLegRepository,
    private val savedState: SavedStateHandle,
    searchResults: Flow<List<PlaceCandidate>> = flowOf(emptyList()),
) : ViewModel() {
    private val selectedDay = savedState.getStateFlow<String?>(SELECTED_DAY, null)
    private val tab = savedState.getStateFlow(TAB, WorkspaceTab.PLACES.name)
    private val scope = savedState.getStateFlow(SCOPE, MapScope.PLACE_POOL.name)
    private val sheet = savedState.getStateFlow(SHEET, WorkspaceSheetLevel.HALF.name)
    private val selectedMarkerKey = MutableStateFlow<String?>(null)
    private val mutable = MutableStateFlow(TripWorkspaceUiState())
    val state: StateFlow<TripWorkspaceUiState> = mutable
    val selectedDayId: StateFlow<String?> = selectedDay

    init {
        val trip = trips.observeTrip(tripId).filterNotNull()
        val snapshots = trip.flatMapLatest { value -> observeSnapshots(value.days, itineraries, routes) }
        viewModelScope.launch {
            combine(trip, places.observePlaces(tripId, emptySet()), snapshots, selectedDay, tab, scope, sheet, selectedMarkerKey, searchResults) { values ->
                @Suppress("UNCHECKED_CAST")
                val currentTrip = values[0] as com.yangchengwei.easytrip.trip.domain.TripWithDays
                @Suppress("UNCHECKED_CAST") val currentPlaces = values[1] as List<SavedPlace>
                @Suppress("UNCHECKED_CAST") val currentSnapshots = values[2] as List<DayMapSnapshot>
                val selected = (values[3] as String?)?.takeIf { id -> currentTrip.days.any { it.id == id } } ?: currentTrip.days.firstOrNull()?.id
                if (selected != values[3]) savedState[SELECTED_DAY] = selected
                @Suppress("UNCHECKED_CAST") val currentSearch = values[8] as List<PlaceCandidate>
                val model = MapUiModelMapper.map(MapScope.valueOf(values[5] as String), currentPlaces, currentTrip.days, currentSnapshots, selected, currentTrip.startDate, currentSearch)
                model.corruptRoutes.forEach { route -> launch { routes.repairCorruptPolyline(route.legId, route.version) } }
                val markerKey = values[7] as String?
                TripWorkspaceUiState(
                    tripName = currentTrip.name,
                    days = currentTrip.days,
                    selectedDayId = selected,
                    tab = WorkspaceTab.valueOf(values[4] as String),
                    mapScope = MapScope.valueOf(values[5] as String),
                    sheetLevel = WorkspaceSheetLevel.valueOf(values[6] as String),
                    map = model,
                    selectedMarker = model.markers.firstOrNull { it.key == markerKey },
                )
            }.collect { mutable.value = it }
        }
    }

    fun selectDay(id: String) { savedState[SELECTED_DAY] = id }
    fun selectTab(value: WorkspaceTab) { savedState[TAB] = value.name }
    fun selectScope(value: MapScope) { savedState[SCOPE] = value.name }
    fun setSheetLevel(value: WorkspaceSheetLevel) { savedState[SHEET] = value.name }
    fun selectMarker(key: String) { selectedMarkerKey.value = key }
    fun dismissMarker() { selectedMarkerKey.value = null }

    class Factory(
        private val tripId: String,
        private val trips: TripRepository,
        private val places: SavedPlaceRepository,
        private val itineraries: ItineraryRepository,
        private val routes: RouteLegRepository,
        private val searchResults: Flow<List<PlaceCandidate>> = flowOf(emptyList()),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            TripWorkspaceViewModel(tripId, trips, places, itineraries, routes, extras.createSavedStateHandle(), searchResults) as T
    }

    companion object {
        private const val SELECTED_DAY = "workspace.selectedDay"
        private const val TAB = "workspace.tab"
        private const val SCOPE = "workspace.scope"
        private const val SHEET = "workspace.sheet"
    }
}

private fun observeSnapshots(
    days: List<TripDay>,
    itineraries: ItineraryRepository,
    routes: RouteLegRepository,
): Flow<List<DayMapSnapshot>> {
    if (days.isEmpty()) return flowOf(emptyList())
    val flows = days.map { day ->
        combine(itineraries.observeDay(day.id), routes.observeDay(day.id)) { itinerary, legs -> DayMapSnapshot(itinerary, legs) }
    }
    return combine(flows) { it.toList() }
}
