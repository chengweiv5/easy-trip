package com.yangchengwei.easytrip.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.GeoPoint
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

enum class WorkspaceTab { SEARCH, PLACES, ITINERARY }
enum class WorkspaceSheetLevel { COLLAPSED, HALF, EXPANDED }

data class SearchResultSelection(
    val poiId: String,
    val point: GeoPoint,
)

data class TripWorkspaceUiState(
    val tripName: String = "",
    val days: List<TripDay> = emptyList(),
    val selectedDayId: String? = null,
    val tab: WorkspaceTab = WorkspaceTab.PLACES,
    val mapScope: MapScope = MapScope.PLACE_POOL,
    val sheetLevel: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF,
    val map: MapUiModel = MapUiModel(),
    val selectedMarker: MapMarkerUi? = null,
    val searchSelection: SearchResultSelection? = null,
    val mapLayer: MapLayer = MapLayer.STANDARD,
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
    private val mapPreferences: MapPreferences = InMemoryMapPreferences(),
) : ViewModel() {
    private val selectedDay = savedState.getStateFlow<String?>(SELECTED_DAY, null)
    private val tab = savedState.getStateFlow(TAB, WorkspaceTab.PLACES.name)
    private val scope = savedState.getStateFlow(SCOPE, MapScope.PLACE_POOL.name)
    private val sheet = savedState.getStateFlow(SHEET, WorkspaceSheetLevel.HALF.name)
    private val focusedPoiId = savedState.getStateFlow<String?>(FOCUSED_POI, null)
    private val selectedMarkerKey = MutableStateFlow<String?>(null)
    private val restoredFocusPoint = savedState.get<Double>(FOCUSED_LATITUDE)?.let { latitude ->
        savedState.get<Double>(FOCUSED_LONGITUDE)?.let { longitude -> GeoPoint(latitude, longitude) }
    }
    private val mapInteraction = MutableStateFlow(
        focusedPoiId.value?.let { poiId ->
            restoredFocusPoint?.let { point -> reduceMapInteraction(MapInteractionState(), MapInteractionAction.FocusSearchResult(poiId, point)) }
        } ?: MapInteractionState(),
    )
    private val viewportController = MapViewportController().apply {
        restoredFocusPoint?.let(::focusSearchResult)
    }
    private var focusedResultObserved = false
    private val mutable = MutableStateFlow(TripWorkspaceUiState())
    val state: StateFlow<TripWorkspaceUiState> = mutable
    val selectedDayId: StateFlow<String?> = selectedDay

    init {
        val trip = trips.observeTrip(tripId).filterNotNull()
        val snapshots = trip.flatMapLatest { value -> observeSnapshots(value.days, itineraries, routes) }
        viewModelScope.launch {
            combine(trip, places.observePlaces(tripId, emptySet()), snapshots, selectedDay, tab, scope, sheet, selectedMarkerKey, searchResults, focusedPoiId, mapInteraction, mapPreferences.layer) { values ->
                @Suppress("UNCHECKED_CAST")
                val currentTrip = values[0] as com.yangchengwei.easytrip.trip.domain.TripWithDays
                @Suppress("UNCHECKED_CAST") val currentPlaces = values[1] as List<SavedPlace>
                @Suppress("UNCHECKED_CAST") val currentSnapshots = values[2] as List<DayMapSnapshot>
                val selected = (values[3] as String?)?.takeIf { id -> currentTrip.days.any { it.id == id } } ?: currentTrip.days.firstOrNull()?.id
                if (selected != values[3]) savedState[SELECTED_DAY] = selected
                @Suppress("UNCHECKED_CAST") val currentSearch = values[8] as List<PlaceCandidate>
                val focusedId = values[9] as String?
                val focusedCandidate = currentSearch.firstOrNull { it.poiId == focusedId }
                val interaction = values[10] as MapInteractionState
                if (focusedCandidate != null) focusedResultObserved = true
                if (focusedId != null && focusedResultObserved && focusedCandidate == null) {
                    clearSearchFocus()
                }
                val activeFocusedId = focusedId.takeUnless { focusedResultObserved && focusedCandidate == null }
                val focusPoint = focusedCandidate?.point ?: restoredFocusPoint.takeIf { activeFocusedId != null }
                val mapped = MapUiModelMapper.map(
                    MapScope.valueOf(values[5] as String),
                    currentPlaces,
                    currentTrip.days,
                    currentSnapshots,
                    selected,
                    currentTrip.startDate,
                    currentSearch,
                    activeFocusedId,
                    focusPoint,
                )
                val baseVisiblePoints = MapUiModelMapper.map(
                    MapScope.valueOf(values[5] as String),
                    currentPlaces,
                    currentTrip.days,
                    currentSnapshots,
                    selected,
                    currentTrip.startDate,
                ).markers.map(MapMarkerUi::point)
                viewportController.update(
                    currentPlaces.map(SavedPlace::point),
                    MapScope.valueOf(values[5] as String),
                    baseVisiblePoints,
                )
                val model = mapped.copy(
                    highlightedMarkerKey = activeFocusedId?.takeIf { focusPoint != null }?.let { "search-$it" },
                    viewportRequest = viewportController.currentRequest,
                )
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
                    searchSelection = activeFocusedId?.let { id -> focusPoint?.let { SearchResultSelection(id, it) } },
                    mapLayer = values[11] as MapLayer,
                )
            }.collect { mutable.value = it }
        }
    }

    fun selectDay(id: String) { savedState[SELECTED_DAY] = id }
    fun selectTab(value: WorkspaceTab) { savedState[TAB] = value.name }
    fun onSearchQueryChanged(query: String) {
        if (query.isNotEmpty()) savedState[TAB] = WorkspaceTab.SEARCH.name
    }
    fun focusSearchResult(candidate: PlaceCandidate) {
        val point = candidate.point ?: return
        savedState[FOCUSED_POI] = candidate.poiId
        savedState[FOCUSED_LATITUDE] = point.latitude
        savedState[FOCUSED_LONGITUDE] = point.longitude
        viewportController.focusSearchResult(point)
        mapInteraction.value = reduceMapInteraction(
            mapInteraction.value,
            MapInteractionAction.FocusSearchResult(candidate.poiId, point),
        )
    }
    fun clearSearchFocus() {
        savedState[FOCUSED_POI] = null
        savedState[FOCUSED_LATITUDE] = null
        savedState[FOCUSED_LONGITUDE] = null
        mapInteraction.value = mapInteraction.value.copy(focusedPoiId = null, highlightedMarkerKey = null)
    }
    fun selectScope(value: MapScope) { savedState[SCOPE] = value.name }
    fun selectMapLayer(value: MapLayer) { mapPreferences.setLayer(value) }
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
        private val mapPreferences: MapPreferences = InMemoryMapPreferences(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            TripWorkspaceViewModel(tripId, trips, places, itineraries, routes, extras.createSavedStateHandle(), searchResults, mapPreferences) as T
    }

    companion object {
        private const val SELECTED_DAY = "workspace.selectedDay"
        private const val TAB = "workspace.tab"
        private const val SCOPE = "workspace.scope"
        private const val SHEET = "workspace.sheet"
        private const val FOCUSED_POI = "workspace.focusedPoi"
        private const val FOCUSED_LATITUDE = "workspace.focusedLatitude"
        private const val FOCUSED_LONGITUDE = "workspace.focusedLongitude"
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
