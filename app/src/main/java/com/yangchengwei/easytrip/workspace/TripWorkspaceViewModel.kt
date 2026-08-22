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
import java.io.Serializable
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

enum class WorkspaceTab { PLACES, ITINERARY }

enum class WorkspaceSheetLevel { COLLAPSED, HALF, EXPANDED }

internal fun restoreWorkspaceTab(raw: String?): WorkspaceTab =
    WorkspaceTab.entries.firstOrNull { it.name == raw } ?: WorkspaceTab.PLACES

const val SEARCH_SELECTION_RESULT = "workspace.searchSelection"

data class SearchSelectionPayload(
    val poiId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
) : Serializable

fun PlaceCandidate.toSearchSelectionPayload(): SearchSelectionPayload {
    val point = requireNotNull(point)
    return SearchSelectionPayload(poiId, name, address, point.latitude, point.longitude)
}

fun SearchSelectionPayload.toPlaceCandidate(): PlaceCandidate =
    PlaceCandidate(poiId, name, address, GeoPoint(latitude, longitude), null)

internal fun consumeSearchSelection(
    handle: SavedStateHandle,
    onSelection: (PlaceCandidate) -> Unit,
): Boolean {
    val payload = handle.remove<SearchSelectionPayload>(SEARCH_SELECTION_RESULT) ?: return false
    onSelection(payload.toPlaceCandidate())
    return true
}

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
    val selectedMarkerPoi: MapPoiUi? = null,
    val selectedMapPoi: MapPoiUi? = null,
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
    private val selectedMapPoi = MutableStateFlow(
        savedState.get<String>(MAP_POI_NAME)?.let { name ->
            val latitude = savedState.get<Double>(MAP_POI_LATITUDE) ?: return@let null
            val longitude = savedState.get<Double>(MAP_POI_LONGITUDE) ?: return@let null
            MapPoiUi(
                poiId = savedState[MAP_POI_ID],
                name = name,
                address = savedState[MAP_POI_ADDRESS] ?: "",
                point = GeoPoint(latitude, longitude),
            )
        },
    )
    private val restoredFocusPoint = savedState.get<Double>(FOCUSED_LATITUDE)?.let { latitude ->
        savedState.get<Double>(FOCUSED_LONGITUDE)?.let { longitude -> GeoPoint(latitude, longitude) }
    }
    private var restoredFocusedCandidate = focusedPoiId.value?.let { poiId ->
        val point = restoredFocusPoint ?: return@let null
        val name = savedState.get<String>(FOCUSED_NAME) ?: return@let null
        PlaceCandidate(poiId, name, savedState[FOCUSED_ADDRESS] ?: "", point, null)
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
            combine(trip, places.observePlaces(tripId, emptySet()), snapshots, selectedDay, tab, scope, sheet, selectedMarkerKey, searchResults, focusedPoiId, mapInteraction, mapPreferences.layer, selectedMapPoi) { values ->
                @Suppress("UNCHECKED_CAST")
                val currentTrip = values[0] as com.yangchengwei.easytrip.trip.domain.TripWithDays
                @Suppress("UNCHECKED_CAST") val currentPlaces = values[1] as List<SavedPlace>
                @Suppress("UNCHECKED_CAST") val currentSnapshots = values[2] as List<DayMapSnapshot>
                val selected = (values[3] as String?)?.takeIf { id -> currentTrip.days.any { it.id == id } } ?: currentTrip.days.firstOrNull()?.id
                if (selected != values[3]) savedState[SELECTED_DAY] = selected
                @Suppress("UNCHECKED_CAST") val currentSearch = values[8] as List<PlaceCandidate>
                val focusedId = values[9] as String?
                val liveFocusedCandidate = currentSearch.firstOrNull { it.poiId == focusedId }
                val focusedCandidate = liveFocusedCandidate ?: restoredFocusedCandidate?.takeIf { it.poiId == focusedId }
                val focusedSavedPlace = currentPlaces.firstOrNull { it.amapPoiId == focusedId }
                val interaction = values[10] as MapInteractionState
                if (liveFocusedCandidate != null) focusedResultObserved = true
                if (focusedId != null && focusedResultObserved && liveFocusedCandidate == null && focusedSavedPlace == null && restoredFocusedCandidate == null) {
                    clearSearchFocus()
                }
                val focusMissing = focusedResultObserved && liveFocusedCandidate == null && focusedSavedPlace == null && restoredFocusedCandidate == null
                val activeFocusedId = focusedId.takeUnless { focusMissing }
                val focusPoint = focusedCandidate?.point
                    ?: focusedSavedPlace?.point
                    ?: interaction.viewportRequest?.takeIf { it.reason == ViewportReason.SEARCH_FOCUS }?.points?.singleOrNull()
                    ?: restoredFocusPoint.takeIf { activeFocusedId != null }
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
                    focusedCandidate,
                )
                val currentScope = MapScope.valueOf(values[5] as String)
                val baseMap = MapUiModelMapper.map(
                    currentScope,
                    currentPlaces,
                    currentTrip.days,
                    currentSnapshots,
                    selected,
                    currentTrip.startDate,
                )
                viewportController.update(
                    currentPlaces.map(SavedPlace::point),
                    currentScope,
                    mapViewportPoints(currentScope, baseMap),
                )
                val model = mapped.copy(viewportRequest = viewportController.currentRequest)
                model.corruptRoutes.forEach { route -> launch { routes.repairCorruptPolyline(route.legId, route.version) } }
                val markerKey = values[7] as String?
                val selectedMarker = model.markers.firstOrNull { it.key == markerKey }
                TripWorkspaceUiState(
                    tripName = currentTrip.name,
                    days = currentTrip.days,
                    selectedDayId = selected,
                    tab = restoreWorkspaceTab(values[4] as String?),
                    mapScope = MapScope.valueOf(values[5] as String),
                    sheetLevel = WorkspaceSheetLevel.valueOf(values[6] as String),
                    map = model,
                    selectedMarker = selectedMarker,
                    selectedMarkerPoi = selectedMarker?.savedPlaceId?.let { savedPlaceId ->
                        currentPlaces.firstOrNull { it.id == savedPlaceId }
                            ?.let { MapPoiUi(it.amapPoiId, it.name, it.address, it.point) }
                    },
                    selectedMapPoi = values[12] as MapPoiUi?,
                    searchSelection = activeFocusedId?.let { id -> focusPoint?.let { SearchResultSelection(id, it) } },
                    mapLayer = values[11] as MapLayer,
                )
            }.collect { mutable.value = it }
        }
    }

    fun selectDay(id: String) { savedState[SELECTED_DAY] = id }
    fun selectTab(value: WorkspaceTab) { savedState[TAB] = value.name }
    fun focusSearchResult(candidate: PlaceCandidate) {
        val point = candidate.point ?: return
        savedState[FOCUSED_POI] = candidate.poiId
        savedState[FOCUSED_NAME] = candidate.name
        savedState[FOCUSED_ADDRESS] = candidate.address
        savedState[FOCUSED_LATITUDE] = point.latitude
        savedState[FOCUSED_LONGITUDE] = point.longitude
        restoredFocusedCandidate = candidate
        viewportController.focusSearchResult(point)
        mapInteraction.value = reduceMapInteraction(
            mapInteraction.value,
            MapInteractionAction.FocusSearchResult(candidate.poiId, point),
        )
    }
    fun clearSearchFocus() {
        savedState[FOCUSED_POI] = null
        savedState[FOCUSED_NAME] = null
        savedState[FOCUSED_ADDRESS] = null
        savedState[FOCUSED_LATITUDE] = null
        savedState[FOCUSED_LONGITUDE] = null
        restoredFocusedCandidate = null
        mapInteraction.value = mapInteraction.value.copy(focusedPoiId = null, highlightedMarkerKey = null)
    }
    fun selectScope(value: MapScope) { savedState[SCOPE] = value.name }
    fun selectMapLayer(value: MapLayer) { mapPreferences.setLayer(value) }
    fun setSheetLevel(value: WorkspaceSheetLevel) { savedState[SHEET] = value.name }
    fun selectMarker(key: String) {
        val marker = mutable.value.map.markers.firstOrNull { it.key == key }
        if (marker?.kind == MapMarkerKind.UNSAVED_SEARCH) {
            val candidate = restoredFocusedCandidate?.takeIf { it.poiId == key.removePrefix("search-") }
            if (candidate != null) {
                selectMapPoi(MapPoiUi(candidate.poiId, candidate.name, candidate.address, requireNotNull(candidate.point)))
                return
            }
        }
        selectedMapPoi.value = null
        clearStoredMapPoi()
        selectedMarkerKey.value = key
    }
    fun dismissMarker() { selectedMarkerKey.value = null }
    fun selectMapPoi(poi: MapPoiUi) {
        selectedMarkerKey.value = null
        savedState[MAP_POI_ID] = poi.poiId
        savedState[MAP_POI_NAME] = poi.name
        savedState[MAP_POI_ADDRESS] = poi.address
        savedState[MAP_POI_LATITUDE] = poi.point.latitude
        savedState[MAP_POI_LONGITUDE] = poi.point.longitude
        selectedMapPoi.value = poi
    }
    fun retainViewportForPlaceCardCollection() {
        selectedMapPoi.value?.let { viewportController.retainViewportForPlaceChange(it.point) }
    }
    fun dismissPlaceCard() {
        selectedMapPoi.value = null
        clearStoredMapPoi()
    }
    private fun clearStoredMapPoi() {
        savedState[MAP_POI_ID] = null
        savedState[MAP_POI_NAME] = null
        savedState[MAP_POI_ADDRESS] = null
        savedState[MAP_POI_LATITUDE] = null
        savedState[MAP_POI_LONGITUDE] = null
    }

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
        private const val FOCUSED_NAME = "workspace.focusedName"
        private const val FOCUSED_ADDRESS = "workspace.focusedAddress"
        private const val FOCUSED_LATITUDE = "workspace.focusedLatitude"
        private const val FOCUSED_LONGITUDE = "workspace.focusedLongitude"
        private const val MAP_POI_ID = "workspace.mapPoi.id"
        private const val MAP_POI_NAME = "workspace.mapPoi.name"
        private const val MAP_POI_ADDRESS = "workspace.mapPoi.address"
        private const val MAP_POI_LATITUDE = "workspace.mapPoi.latitude"
        private const val MAP_POI_LONGITUDE = "workspace.mapPoi.longitude"
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
