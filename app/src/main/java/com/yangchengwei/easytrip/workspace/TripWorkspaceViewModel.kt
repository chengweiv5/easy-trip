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
import com.yangchengwei.easytrip.itinerary.ui.toRouteLegUi
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

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
    val calendarDays: List<WholeTripDayUi> = emptyList(),
    val calendarMode: Boolean = false,
    val calendarFocus: String? = null,
    val calendarSave: com.yangchengwei.easytrip.itinerary.calendar.CalendarSaveState = com.yangchengwei.easytrip.itinerary.calendar.CalendarSaveState(),
    val sheetLevel: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF,
    val map: MapUiModel = MapUiModel(),
    val selectedMarker: MapMarkerUi? = null,
    val selectedMarkerPoi: MapPoiUi? = null,
    val selectedMapPoi: MapPoiUi? = null,
    val searchSelection: SearchResultSelection? = null,
    val searchResults: WorkspaceSearchResults? = null,
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
    private val clock: java.time.Clock = java.time.Clock.systemDefaultZone(),
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
    private val calendarMode = savedState.getStateFlow("calendar-mode", false)
    private val calendarFocus = MutableStateFlow<String?>(null)
    private val calendarTiming = com.yangchengwei.easytrip.itinerary.calendar.CalendarTimingController(itineraries::compareAndSetTiming)
    private val sheet = savedState.getStateFlow(SHEET, WorkspaceSheetLevel.HALF.name)
    private val focusedPoiId = savedState.getStateFlow<String?>(FOCUSED_POI, null)
    private val selectedMarkerKey = MutableStateFlow<String?>(null)
    private val overlay = MutableStateFlow<WorkspaceOverlay>(WorkspaceOverlay.None)
    private val selectedMapPoi = MutableStateFlow<MapPoiUi?>(null)
    private val placePoolFilter = MutableStateFlow(PlacePoolMapFilter())
    private val currentPosition = MutableStateFlow<com.yangchengwei.easytrip.place.domain.LocatedPosition?>(null)
    private val submittedSearch = MutableStateFlow(
        WorkspaceSearchResults.restore(savedState.get<ArrayList<String>>("submitted-search-results")),
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
        submittedSearch.value?.let { showSearchResults(it.mappedPlaces.map { place -> requireNotNull(place.point) }) }
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
    private var hasSavedPlaces = false
    // Suppression belongs to this live workspace, not a later restored ViewModel.
    private var initialCityAttempted = false
    private var initialCitySuppressed = false

    /** Each workspace entry initializes an empty trip; recomposition must not undo manual browsing. */
    suspend fun locateEmptyTripCity(resolve: suspend (String) -> TripCity?) {
        val ready = pageState.first { it !is TripWorkspacePageState.Loading } as? TripWorkspacePageState.Ready ?: return
        if (initialCityAttempted) return
        initialCityAttempted = true
        if (hasSavedPlaces || initialCitySuppressed) return
        val name = ready.content.tripName
        val city = try {
            kotlinx.coroutines.withTimeoutOrNull(12_000) { resolve(name) }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        } ?: return
        if (hasSavedPlaces || mutable.value.tripName != name ||
            initialCitySuppressed ||
            mutablePageState.value !is TripWorkspacePageState.Ready
        ) return
        val request = viewportController.showInitialCity(city) ?: return
        mutable.value = mutable.value.copy(map = mutable.value.map.copy(viewportRequest = request))
        mutablePageState.value = TripWorkspacePageState.Ready(mutable.value.toReadyState())
    }

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
                        if (value == null) flowOf(emptyList()) else observeSnapshots(value.id, value.days, itineraries, routes)
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
                        calendarMode,
                        calendarFocus,
                        calendarTiming.state,
                        submittedSearch,
                        currentPosition,
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
        hasSavedPlaces = currentPlaces.isNotEmpty()
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
        val currentItineraryScope = reconcileItineraryScope(
            requestedItineraryScope, previousDays, currentTrip.days,
            currentTrip.startDate, java.time.LocalDate.now(clock),
        )
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
        val activeSearch = values[17] as WorkspaceSearchResults?
        if (activeSearch == null) viewportController.update(
            placePoints = currentPlaces.map(SavedPlace::point),
            scope = currentMapScope,
            selectedDayId = selected,
            visiblePoints = automaticMapViewportPoints(currentMapScope, mapped),
            placeFilter = if (currentMapScope == MapScope.PLACE_POOL) poolFilter else PlacePoolMapFilter(),
            retainCamera = restoreWorkspaceSheetLevel(values[5] as String?) == WorkspaceSheetLevel.EXPANDED,
        )
        val model = if (activeSearch == null) mapped.copy(viewportRequest = viewportController.currentRequest)
        else MapUiModel(
            markers = activeSearch.mappedPlaces.map { candidate ->
                MapMarkerUi("result-${candidate.poiId}", requireNotNull(candidate.point), candidate.name,
                    emptyList(), MapMarkerKind.SEARCH_RESULT)
            },
            viewportRequest = viewportController.currentRequest,
            currentLocation = activeSearch.locationInResultsCity(values[18] as com.yangchengwei.easytrip.place.domain.LocatedPosition?),
        )
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
            calendarDays = currentTrip.days.sortedBy { it.index }.map { day ->
                val snapshot = currentSnapshots.firstOrNull { it.itinerary.dayId == day.id }
                WholeTripDayUi(day.id, day.index + 1,
                    snapshot?.itinerary?.items.orEmpty().map { com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi(it.id, it.place.name, it.place.address, it.arrivalTime, it.stayMinutes, it.note, it.place.id, it.expenseCents, it.timingWarning, it.expenses) },
                    snapshot?.legs.orEmpty().map { it.toRouteLegUi() })
            },
            calendarMode = values[14] as Boolean,
            calendarFocus = values[15] as String?,
            calendarSave = values[16] as com.yangchengwei.easytrip.itinerary.calendar.CalendarSaveState,
            sheetLevel = restoreWorkspaceSheetLevel(values[5] as String?),
            map = model,
            selectedMarker = selectedMarker,
            selectedMarkerPoi = selectedMarker?.savedPlaceId?.let { savedPlaceId -> currentPlaces.firstOrNull { it.id == savedPlaceId }?.let { MapPoiUi(it.amapPoiId, it.name, it.address, it.point) } },
            selectedMapPoi = values[11] as MapPoiUi?,
            searchSelection = activeFocusedId?.let { id -> focusPoint?.let { SearchResultSelection(id, it) } },
            searchResults = activeSearch,
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
        initialCitySuppressed = true
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
        initialCitySuppressed = true
        savedState[SECTION] = value.name
        section.value = value
    }
    fun selectItineraryScope(value: ItineraryScope) {
        if (value is ItineraryScope.Day && mutable.value.days.none { it.id == value.dayId }) return
        if (itineraryScope.value == value) return
        initialCitySuppressed = true
        savedState[ITINERARY_SCOPE] = encodeItineraryScope(value)
        itineraryScope.value = value
    }
    fun showSearchResults(results: WorkspaceSearchResults) {
        initialCitySuppressed = true
        clearSearchFocus()
        closeOverlay()
        viewportController.showSearchResults(results.viewportPoints(currentPosition.value))
        savedState["submitted-search-results"] = results.save()
        submittedSearch.value = results
        setSheetLevel(WorkspaceSheetLevel.COLLAPSED)
    }

    fun updateCurrentPosition(position: com.yangchengwei.easytrip.place.domain.LocatedPosition?) {
        if (position == currentPosition.value) return
        val results = submittedSearch.value
        // A late foreground fix may complete the initial fit, but never undo a map gesture.
        if (results != null && results.locationInResultsCity(currentPosition.value) == null &&
            results.locationInResultsCity(position) != null &&
            viewportController.currentRequest?.reason == ViewportReason.SEARCH_RESULTS
        ) {
            viewportController.showSearchResults(results.viewportPoints(position))
        }
        currentPosition.value = position
    }

    fun clearSearchResults() {
        closeOverlay()
        viewportController.retainViewportOnNextUpdate()
        savedState["submitted-search-results"] = null
        submittedSearch.value = null
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
    fun toggleCalendar() {
        val enabled = !calendarMode.value
        if (enabled) {
            savedState["calendar-previous-sheet"] = sheet.value
            setSheetLevel(WorkspaceSheetLevel.EXPANDED)
        } else {
            setSheetLevel(restoreWorkspaceSheetLevel(savedState["calendar-previous-sheet"]))
            calendarFocus.value = null
        }
        savedState["calendar-mode"] = enabled
    }
    fun focusCalendar(dayId: String, itemId: String?) {
        selectItineraryScope(ItineraryScope.Day(dayId))
        calendarFocus.value = itemId
    }
    fun saveCalendar(change: com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange) {
        if (!calendarMode.value || itineraryScope.value != ItineraryScope.Day(change.dayId)) return
        viewModelScope.launch { calendarTiming.commit(change.copy(tripId = tripId)) }
    }
    fun undoCalendar() { viewModelScope.launch { calendarTiming.undo() } }
    fun retryCalendar() { viewModelScope.launch { calendarTiming.retry() } }
    fun dismissCalendarMessage() = calendarTiming.dismiss()

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
        if (overlay.value == WorkspaceOverlay.None && calendarMode.value && section.value == WorkspaceSection.ITINERARY) {
            if (!calendarTiming.state.value.saving) toggleCalendar()
            return true
        }
        if (overlay.value == WorkspaceOverlay.None) return false
        closeOverlay()
        return true
    }
    fun selectMarker(key: String) {
        val marker = mutable.value.map.markers.firstOrNull { it.key == key }
        if (marker?.kind == MapMarkerKind.SEARCH_RESULT) {
            val candidate = submittedSearch.value?.mappedPlaces?.firstOrNull { "result-${it.poiId}" == key } ?: return
            selectMapPoi(MapPoiUi(candidate.poiId, candidate.name, candidate.address, requireNotNull(candidate.point)))
            return
        }
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
    tripId: String,
    days: List<TripDay>,
    itineraries: ItineraryRepository,
    routes: RouteLegRepository,
): Flow<List<DayMapSnapshot>> {
    itineraries.observeTripDays(tripId)?.let { consistent ->
        return consistent.map { snapshots -> snapshots.map { DayMapSnapshot(it.itinerary, it.legs) } }
    }
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
