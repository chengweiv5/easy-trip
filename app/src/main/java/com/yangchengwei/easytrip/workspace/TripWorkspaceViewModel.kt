package com.yangchengwei.easytrip.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi
import com.yangchengwei.easytrip.itinerary.ui.mapWholeTripDays
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

enum class WorkspaceTab { PLACES, ITINERARY }

internal fun restoreWorkspaceTab(raw: String?): WorkspaceTab =
    WorkspaceTab.entries.firstOrNull { it.name == raw } ?: WorkspaceTab.PLACES

data class SearchResultSelection(
    val poiId: String,
    val point: GeoPoint,
)

data class TripWorkspaceUiState(
    val tripName: String = "",
    val dateLabel: String? = null,
    val startDate: java.time.LocalDate? = null,
    val days: List<TripDay> = emptyList(),
    val section: WorkspaceSection = WorkspaceSection.PLACE_POOL,
    val itineraryScope: ItineraryScope = ItineraryScope.WholeTrip,
    val mapScope: MapScope = MapScope.PLACE_POOL,
    val selectedDayId: String? = null,
    val wholeTripDays: List<WholeTripDayUi> = emptyList(),
    val sheetLevel: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF,
    val map: MapUiModel = MapUiModel(),
    val selectedMarker: MapMarkerUi? = null,
    val selectedMarkerPoi: MapPoiUi? = null,
    val selectedMapPoi: MapPoiUi? = null,
    val searchSelection: SearchResultSelection? = null,
    val mapLayer: MapLayer = MapLayer.STANDARD,
    val overlay: WorkspaceOverlay = WorkspaceOverlay.None,
    val isItineraryAllEmpty: Boolean = false,
    val isWorkspaceAllEmpty: Boolean = false,
    val schedulesByPlaceId: Map<String, PlaceScheduleSummaryUi> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TripWorkspaceViewModel(
    private val tripId: String,
    private val trips: TripRepository,
    private val places: SavedPlaceRepository,
    private val itineraries: ItineraryRepository,
    private val routes: RouteLegRepository,
    private val savedState: SavedStateHandle,
    private val searchResults: Flow<List<PlaceCandidate>> = flowOf(emptyList()),
    private val mapPreferences: MapPreferences = InMemoryMapPreferences(),
) : ViewModel() {
    private val restoredNavigation = restoreWorkspaceNavigation(
        savedState[SECTION],
        savedState[ITINERARY_SCOPE],
        savedState[LEGACY_TAB],
        savedState[LEGACY_SCOPE],
        savedState[LEGACY_SELECTED_DAY],
    )
    private val section = MutableStateFlow(restoredNavigation.section)
    private val itineraryScope = MutableStateFlow(restoredNavigation.itineraryScope)
    private val sheet = savedState.getStateFlow(SHEET, WorkspaceSheetLevel.HALF.name)
    private val focusedPoiId = savedState.getStateFlow<String?>(FOCUSED_POI, null)
    private val selectedMarkerKey = MutableStateFlow<String?>(null)
    private val overlay = MutableStateFlow<WorkspaceOverlay>(WorkspaceOverlay.None)
    private val selectedMapPoi = MutableStateFlow<MapPoiUi?>(null)
    private val placePoolFilter = MutableStateFlow(PlacePoolMapFilter())
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
    private var previousDays = emptyList<TripDay>()
    private val mutable = MutableStateFlow(TripWorkspaceUiState())
    val state: StateFlow<TripWorkspaceUiState> = mutable
    private val mutablePageState = MutableStateFlow<TripWorkspacePageState>(TripWorkspacePageState.Loading)
    val pageState: StateFlow<TripWorkspacePageState> = mutablePageState
    private val mutableDaySnapshots = MutableStateFlow<List<DayMapSnapshot>>(emptyList())
    val daySnapshots: StateFlow<List<DayMapSnapshot>> = mutableDaySnapshots
    private val mutableSelectedDayId = MutableStateFlow<String?>(null)
    val selectedDayId: StateFlow<String?> = mutableSelectedDayId
    private var observationJob: Job? = null

    init {
        savedState[SECTION] = section.value.name
        restoredNavigation.itineraryScope?.let { savedState[ITINERARY_SCOPE] = encodeItineraryScope(it) }
        clearStoredMapPoi()
        observeWorkspace()
    }

    fun retry() = observeWorkspace()

    private fun observeWorkspace() {
        observationJob?.cancel()
        mutablePageState.value = TripWorkspacePageState.Loading
        observationJob = viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val trip = trips.observeTrip(tripId).shareIn(this, SharingStarted.Eagerly, replay = 1)
                    val snapshots = trip.flatMapLatest { value ->
                        if (value == null) flowOf(emptyList()) else observeSnapshots(value.days, itineraries, routes)
                    }
                    combine(
                        trip,
                        places.observePlaces(tripId, emptySet()),
                        snapshots,
                        section,
                        itineraryScope,
                        sheet,
                        selectedMarkerKey,
                        searchResults,
                        focusedPoiId,
                        mapInteraction,
                        mapPreferences.layer,
                        selectedMapPoi,
                        overlay,
                        placePoolFilter,
                    ) { values -> mapWorkspaceState(values) }
                        .collect { next ->
                            if (next == null) {
                                mutableDaySnapshots.value = emptyList()
                                mutableSelectedDayId.value = null
                                mutablePageState.value = TripWorkspacePageState.NotFound
                            } else {
                                mutable.value = next
                                mutablePageState.value = TripWorkspacePageState.Ready(next.toReadyState())
                            }
                        }
                }
            }.onFailure {
                if (it !is kotlinx.coroutines.CancellationException) {
                    mutablePageState.value = TripWorkspacePageState.Error("无法加载旅行")
                }
            }
        }
    }

    private fun mapWorkspaceState(values: Array<Any?>): TripWorkspaceUiState? {
        val currentTrip = values[0] as com.yangchengwei.easytrip.trip.domain.TripWithDays? ?: return null
        @Suppress("UNCHECKED_CAST") val currentPlaces = values[1] as List<SavedPlace>
        @Suppress("UNCHECKED_CAST") val emittedSnapshots = values[2] as List<DayMapSnapshot>
        val currentDayIds = currentTrip.days.mapTo(mutableSetOf(), TripDay::id)
        val currentSnapshots = emittedSnapshots.filter { it.itinerary.dayId in currentDayIds }
        val hasCompleteSnapshotDays = currentSnapshots.mapTo(mutableSetOf()) { it.itinerary.dayId } == currentDayIds
        val isItineraryAllEmpty = currentDayIds.isNotEmpty() &&
            hasCompleteSnapshotDays &&
            currentSnapshots.all { it.itinerary.items.isEmpty() }
        val isWorkspaceAllEmpty = isItineraryAllEmpty && currentPlaces.isEmpty()
        val schedulePlaceIds = buildSet {
            currentPlaces.mapTo(this, SavedPlace::id)
            currentSnapshots.flatMapTo(this) { snapshot -> snapshot.itinerary.items.map { it.place.id } }
        }
        val schedulesByPlaceId = schedulePlaceIds.associateWith { placeId ->
            if (hasCompleteSnapshotDays) {
                val days = currentTrip.days.mapNotNull { day ->
                    val occurrences = currentSnapshots.first { it.itinerary.dayId == day.id }
                        .itinerary.items
                        .count { it.place.id == placeId }
                    PlaceScheduleDayUi(day.id, day.index, occurrences).takeIf { occurrences > 0 }
                }
                PlaceScheduleSummaryUi(true, days.sumOf(PlaceScheduleDayUi::occurrences), days)
            } else {
                PlaceScheduleSummaryUi(false)
            }
        }
        val currentSection = values[3] as WorkspaceSection
        val requestedItineraryScope = values[4] as ItineraryScope?
        val currentItineraryScope = reconcileItineraryScope(requestedItineraryScope, previousDays, currentTrip.days)
        previousDays = currentTrip.days
        if (currentItineraryScope != requestedItineraryScope) itineraryScope.value = currentItineraryScope
        savedState[ITINERARY_SCOPE] = encodeItineraryScope(currentItineraryScope)
        val currentMapScope = currentSection.toMapScope(currentItineraryScope)
        mutableDaySnapshots.value = currentSnapshots.filter { it.itinerary.tripId == tripId }
        val selected = currentItineraryScope.selectedDayId().takeIf { currentSection == WorkspaceSection.ITINERARY }
        mutableSelectedDayId.value = selected
        @Suppress("UNCHECKED_CAST") val currentSearch = values[7] as List<PlaceCandidate>
        val focusedId = values[8] as String?
        val liveFocusedCandidate = currentSearch.firstOrNull { it.poiId == focusedId }
        val focusedCandidate = liveFocusedCandidate ?: restoredFocusedCandidate?.takeIf { it.poiId == focusedId }
        val focusedSavedPlace = currentPlaces.firstOrNull { it.amapPoiId == focusedId }
        val interaction = values[9] as MapInteractionState
        if (liveFocusedCandidate != null) focusedResultObserved = true
        if (focusedId != null && focusedResultObserved && liveFocusedCandidate == null && focusedSavedPlace == null && restoredFocusedCandidate == null) clearSearchFocus()
        val focusMissing = focusedResultObserved && liveFocusedCandidate == null && focusedSavedPlace == null && restoredFocusedCandidate == null
        val activeFocusedId = focusedId.takeUnless { focusMissing }
        val focusPoint = focusedCandidate?.point
            ?: focusedSavedPlace?.point
            ?: interaction.viewportRequest?.takeIf { it.reason == ViewportReason.SEARCH_FOCUS }?.points?.singleOrNull()
            ?: restoredFocusPoint.takeIf { activeFocusedId != null }
        val poolFilter = values[13] as PlacePoolMapFilter
        val mapped = MapUiModelMapper.map(currentMapScope, currentPlaces, currentTrip.days, currentSnapshots, selected, currentTrip.startDate, currentSearch, activeFocusedId, focusPoint, focusedCandidate, poolFilter)
        viewportController.update(
            placePoints = currentPlaces.map(SavedPlace::point),
            scope = currentMapScope,
            selectedDayId = selected,
            visiblePoints = automaticMapViewportPoints(currentMapScope, mapped),
            placeFilter = if (currentMapScope == MapScope.PLACE_POOL) poolFilter else PlacePoolMapFilter(),
        )
        val model = mapped.copy(viewportRequest = viewportController.currentRequest)
        model.corruptRoutes.forEach { route -> viewModelScope.launch { routes.repairCorruptPolyline(route.legId, route.version) } }
        val selectedMarker = model.markers.firstOrNull { it.key == values[6] as String? }
        return TripWorkspaceUiState(
            tripName = currentTrip.name,
            dateLabel = workspaceDateLabel(currentTrip.startDate, currentTrip.days.size),
            days = currentTrip.days,
            section = currentSection,
            itineraryScope = currentItineraryScope,
            mapScope = currentMapScope,
            selectedDayId = selected,
            wholeTripDays = mapWholeTripDays(currentTrip.days, currentSnapshots),
            sheetLevel = restoreWorkspaceSheetLevel(values[5] as String?),
            map = model,
            selectedMarker = selectedMarker,
            selectedMarkerPoi = selectedMarker?.savedPlaceId?.let { savedPlaceId -> currentPlaces.firstOrNull { it.id == savedPlaceId }?.let { MapPoiUi(it.amapPoiId, it.name, it.address, it.point) } },
            selectedMapPoi = values[11] as MapPoiUi?,
            searchSelection = activeFocusedId?.let { id -> focusPoint?.let { SearchResultSelection(id, it) } },
            mapLayer = values[10] as MapLayer,
            overlay = values[12] as WorkspaceOverlay,
            isItineraryAllEmpty = isItineraryAllEmpty,
            isWorkspaceAllEmpty = isWorkspaceAllEmpty,
            schedulesByPlaceId = schedulesByPlaceId,
            startDate = currentTrip.startDate,
        )
    }

    private fun workspaceDateLabel(startDate: java.time.LocalDate?, dayCount: Int): String? {
        if (startDate == null || dayCount <= 0) return null
        fun java.time.LocalDate.label() = "${monthValue}月${dayOfMonth}日"
        if (dayCount == 1) return startDate.label()
        return "${startDate.label()} — ${startDate.plusDays(dayCount.toLong() - 1).label()}"
    }

    fun onMapGesture() {
        viewportController.onUserGesture()
        mutable.value = mutable.value.copy(
            map = mutable.value.map.copy(viewportRequest = viewportController.currentRequest),
        )
        (mutablePageState.value as? TripWorkspacePageState.Ready)?.let { page ->
            mutablePageState.value = TripWorkspacePageState.Ready(
                page.content.copy(map = mutable.value.map),
            )
        }
    }

    fun setPlacePoolFilter(filter: PlacePoolMapFilter) {
        if (placePoolFilter.value == filter) return
        placePoolFilter.value = filter
        if (section.value == WorkspaceSection.PLACE_POOL) {
            viewportController.onUserGesture()
            clearSearchFocus()
            selectedMarkerKey.value = null
        }
    }

    fun selectSection(value: WorkspaceSection) {
        if (section.value == value) return
        savedState[SECTION] = value.name
        section.value = value
    }
    fun selectItineraryScope(value: ItineraryScope) {
        if (value is ItineraryScope.Day && mutable.value.days.none { it.id == value.dayId }) return
        if (itineraryScope.value == value) return
        savedState[ITINERARY_SCOPE] = encodeItineraryScope(value)
        itineraryScope.value = value
    }
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
    fun selectMapLayer(value: MapLayer) { mapPreferences.setLayer(value) }
    fun setSheetLevel(value: WorkspaceSheetLevel) {
        savedState[SHEET] = value.name
        if (value == WorkspaceSheetLevel.EXPANDED && overlay.value == WorkspaceOverlay.LayerMenu) {
            closeOverlay()
        }
    }
    fun openOverlay(value: WorkspaceOverlay) {
        if (value == WorkspaceOverlay.MoreMenu && overlay.value != WorkspaceOverlay.None) return
        clearMapDetail()
        overlay.value = value
    }
    fun closeOverlay() {
        clearMapDetail()
        overlay.value = WorkspaceOverlay.None
    }
    fun handleBack(): Boolean {
        if (overlay.value == WorkspaceOverlay.None) return false
        closeOverlay()
        return true
    }
    fun selectMarker(key: String) {
        val marker = mutable.value.map.markers.firstOrNull { it.key == key }
        if (marker?.kind == MapMarkerKind.UNSAVED_SEARCH) {
            val candidate = restoredFocusedCandidate?.takeIf { it.poiId == key.removePrefix("search-") }
            if (candidate != null) {
                selectMapPoi(MapPoiUi(candidate.poiId, candidate.name, candidate.address, requireNotNull(candidate.point)))
                return
            }
        }
        openOverlay(WorkspaceOverlay.PlaceDetail(stableOverlayId(key)))
        selectedMarkerKey.value = key
    }
    fun dismissMarker() {
        selectedMarkerKey.value = null
        closeOverlay()
    }
    fun selectMapPoi(poi: MapPoiUi) {
        openOverlay(WorkspaceOverlay.PlaceDetail(stableOverlayId(poi.poiId ?: "${poi.point.latitude},${poi.point.longitude}")))
        selectedMapPoi.value = poi
    }
    fun retainViewportForPlaceCardCollection() {
        selectedMapPoi.value?.let { viewportController.retainViewportForPlaceChange(it.point) }
    }
    fun dismissPlaceCard() {
        selectedMapPoi.value = null
        clearStoredMapPoi()
        closeOverlay()
    }
    private fun stableOverlayId(value: String): Long = value.hashCode().toLong() and 0xffffffffL
    private fun clearMapDetail() {
        selectedMapPoi.value = null
        selectedMarkerKey.value = null
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
        private const val SECTION = "workspace.section"
        private const val ITINERARY_SCOPE = "workspace.itineraryScope"
        private const val LEGACY_SELECTED_DAY = "workspace.selectedDay"
        private const val LEGACY_TAB = "workspace.tab"
        private const val LEGACY_SCOPE = "workspace.scope"
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
        flow {
            var emitted = false
            try {
                combine(itineraries.observeDay(day.id), routes.observeDay(day.id)) { itinerary, legs -> DayMapSnapshot(itinerary, legs) }
                    .collect {
                        emitted = true
                        emit(it)
                    }
            } catch (error: TargetDayNotFoundException) {
                if (!emitted) throw error
                awaitCancellation()
            }
        }
    }
    return combine(flows) { it.toList() }
}
