package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.amap.AmapConsentFact
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.ConsentRegistry
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints
import com.yangchengwei.easytrip.route.domain.RoutePlanOutcome
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import java.time.LocalTime
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TripWorkspaceContentStateTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun emptyTripUsesCityOncePerEntryAndLateResultCannotOverrideGesture() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        val city = TripCity("330100", "杭州市", GeoPoint(30.27, 120.15))
        model.locateEmptyTripCity { city }
        advanceUntilIdle()
        assertEquals(listOf(city.center), model.state.value.map.viewportRequest?.points)
        assertEquals(11f, model.state.value.map.viewportRequest?.singlePointZoom)
        assertTrue(model.state.value.map.markers.isEmpty())
        model.onMapGesture()
        model.updateCurrentPosition(null)
        model.locateEmptyTripCity { error("must not query twice") }
        advanceUntilIdle()
        assertNull(model.state.value.map.viewportRequest)

        val other = model(trips)
        advanceUntilIdle()
        val delayed = kotlinx.coroutines.CompletableDeferred<TripCity?>()
        val job = launch { other.locateEmptyTripCity { delayed.await() } }
        runCurrent()
        other.onMapGesture()
        delayed.complete(city)
        job.join()
        advanceUntilIdle()
        assertNull(other.state.value.map.viewportRequest)
    }

    @Test fun savedPlacesAndFailedCityLookupRetainExistingViewport() = runTest(dispatcher) {
        val trips = Trips()
        val withPlaces = model(trips, Places(listOf(savedPlace())))
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        val before = withPlaces.state.value.map.viewportRequest
        var queriedWithSavedPlaces = false
        withPlaces.locateEmptyTripCity {
            queriedWithSavedPlaces = true
            TripCity("330100", "杭州市", GeoPoint(30.27, 120.15))
        }
        assertFalse(queriedWithSavedPlaces)
        assertEquals(before, withPlaces.state.value.map.viewportRequest)
        val empty = model(trips)
        advanceUntilIdle()
        empty.locateEmptyTripCity { throw IllegalStateException("offline") }
        assertNull(empty.state.value.map.viewportRequest)
        assertTrue(empty.pageState.value is TripWorkspacePageState.Ready)
    }

    @Test fun restoredEmptyWorkspaceDoesNotInheritACompletedEntriesCitySuppression() = runTest(dispatcher) {
        val trips = Trips()
        trips.value.value = tripWithTwoDays()
        val handle = SavedStateHandle(mapOf(
            "initial-city-attempted" to true,
            "initial-city-suppressed" to true,
        ))
        val restored = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), handle)
        advanceUntilIdle()
        val city = TripCity("370300", "淄博市", GeoPoint(36.81, 118.05))
        restored.locateEmptyTripCity { city }
        advanceUntilIdle()
        assertEquals(ViewportReason.INITIAL_CITY, restored.state.value.map.viewportRequest?.reason)
        assertEquals(listOf(city.center), restored.state.value.map.viewportRequest?.points)
        restored.onMapGesture()
        var queriedTwice = false
        restored.locateEmptyTripCity { queriedTwice = true; city }
        assertFalse(queriedTwice)
        assertNull(restored.state.value.map.viewportRequest)
    }

    @Test fun savedPlaceArrivingDuringCityLookupWinsOverLateCity() = runTest(dispatcher) {
        val trips = Trips()
        val places = Places()
        val model = model(trips, places)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        val delayed = kotlinx.coroutines.CompletableDeferred<TripCity?>()
        val job = launch { model.locateEmptyTripCity { delayed.await() } }
        runCurrent()
        places.value.value = listOf(savedPlace())
        runCurrent()
        val savedViewport = model.state.value.map.viewportRequest
        assertEquals(ViewportReason.INITIAL, savedViewport?.reason)
        delayed.complete(TripCity("330100", "杭州市", GeoPoint(30.27, 120.15)))
        job.join()
        assertEquals(savedViewport, model.state.value.map.viewportRequest)
    }

    @Test fun searchDuringCityLookupKeepsSearchViewport() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        val delayed = kotlinx.coroutines.CompletableDeferred<TripCity?>()
        val job = launch { model.locateEmptyTripCity { delayed.await() } }
        runCurrent()
        model.showSearchResults(WorkspaceSearchResults("景点", listOf(
            PlaceCandidate("poi-a", "断桥", "", GeoPoint(30.258, 120.149), "0571", "杭州市", "330100", 1),
        )))
        runCurrent()
        val searchViewport = model.state.value.map.viewportRequest
        assertEquals(ViewportReason.SEARCH_RESULTS, searchViewport?.reason)
        delayed.complete(TripCity("330100", "杭州市", GeoPoint(30.27, 120.15)))
        job.join()
        assertEquals(searchViewport, model.state.value.map.viewportRequest)
    }

    @Test fun cityLookupTimeoutLeavesDefaultMapAndDoesNotRetry() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        val job = launch { model.locateEmptyTripCity { kotlinx.coroutines.awaitCancellation() } }
        advanceUntilIdle()
        job.join()
        assertNull(model.state.value.map.viewportRequest)
        assertTrue(model.pageState.value is TripWorkspacePageState.Ready)
        model.locateEmptyTripCity { error("timed-out lookup must not retry") }
    }
    @Test fun sameCityLocationIsSeparateFromSearchPinsAndDrawerDoesNotRefitIt() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        val here = com.yangchengwei.easytrip.place.domain.LocatedPosition(
            GeoPoint(30.25, 120.16), com.yangchengwei.easytrip.place.domain.PlaceCity("杭州市", "330100"),
        )
        model.updateCurrentPosition(here)
        val results = WorkspaceSearchResults("景点", listOf(
            PlaceCandidate("poi-a", "断桥", "", GeoPoint(30.258, 120.149), "0571", "杭州市", "330100", 1),
        ))
        model.showSearchResults(results)
        advanceUntilIdle()
        assertEquals(here.point, model.state.value.map.currentLocation)
        assertEquals(1, model.state.value.map.markers.size)
        val camera = model.state.value.map.viewportRequest
        assertTrue(camera!!.points.contains(here.point))
        model.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
        advanceUntilIdle()
        assertEquals(camera, model.state.value.map.viewportRequest)
        model.updateCurrentPosition(null)
        advanceUntilIdle()
        assertNull(model.state.value.map.currentLocation)
        model.updateCurrentPosition(here)
        model.clearSearchResults()
        advanceUntilIdle()
        assertNull(model.state.value.map.currentLocation)
    }

    @Test fun lateSameCityFixCompletesInitialFitButDoesNotUndoUserMapGesture() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        val here = com.yangchengwei.easytrip.place.domain.LocatedPosition(
            GeoPoint(30.25, 120.16), com.yangchengwei.easytrip.place.domain.PlaceCity("杭州市", "330100"),
        )
        val results = WorkspaceSearchResults("景点", listOf(
            PlaceCandidate("poi-a", "断桥", "", GeoPoint(30.258, 120.149), "0571", "杭州市", "330100", 1),
        ))
        model.showSearchResults(results)
        advanceUntilIdle()
        model.updateCurrentPosition(here)
        advanceUntilIdle()
        assertTrue(model.state.value.map.viewportRequest!!.points.contains(here.point))
        model.updateCurrentPosition(null)
        advanceUntilIdle()
        model.onMapGesture()
        model.updateCurrentPosition(here)
        advanceUntilIdle()
        assertNull(model.state.value.map.viewportRequest)
        assertEquals(here.point, model.state.value.map.currentLocation)
        model.updateCurrentPosition(here.copy(city = com.yangchengwei.easytrip.place.domain.PlaceCity("宁波市", "330200")))
        advanceUntilIdle()
        assertNull(model.state.value.map.currentLocation)
    }

    @Test fun enteringOngoingTripSelectsTodayInsteadOfFirstDay() = runTest(dispatcher) {
        val trips = Trips()
        val clock = Clock.fixed(Instant.parse("2026-10-04T16:30:00Z"), ZoneId.of("Asia/Shanghai"))
        val model = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), SavedStateHandle(), clock = clock)
        trips.value.value = TripWithDays(
            "trip", "进行中的旅行", LocalDate.of(2026, 10, 4), TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2)),
        )
        advanceUntilIdle()
        model.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()

        assertEquals(ItineraryScope.Day("day-2"), model.state.value.itineraryScope)
        assertEquals("day-2", model.selectedDayId.value)
    }

    @Test fun manualDayAndWholeTripSurviveTabSwitchSearchAndStateRestoration() = runTest(dispatcher) {
        val trips = Trips()
        val clock = Clock.fixed(Instant.parse("2026-10-04T16:30:00Z"), ZoneId.of("Asia/Shanghai"))
        trips.value.value = TripWithDays(
            "trip", "进行中的旅行", LocalDate.of(2026, 10, 4), TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2)),
        )
        val handle = SavedStateHandle()
        val model = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), handle, clock = clock)
        advanceUntilIdle()
        model.selectSection(WorkspaceSection.ITINERARY)
        model.selectItineraryScope(ItineraryScope.Day("day-3"))
        advanceUntilIdle()
        model.selectSection(WorkspaceSection.PLACE_POOL)
        model.selectSection(WorkspaceSection.ITINERARY)
        model.showSearchResults(WorkspaceSearchResults("地点", listOf(
            PlaceCandidate("poi", "地点", "", GeoPoint(30.0, 120.0), null),
        )))
        advanceUntilIdle()
        model.clearSearchResults()
        advanceUntilIdle()
        assertEquals("day-3", model.selectedDayId.value)
        val restored = TripWorkspaceViewModel(
            "trip", trips, Places(), Itineraries(), Legs(),
            SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }), clock = clock,
        )
        advanceUntilIdle()
        assertEquals("day-3", restored.selectedDayId.value)
        model.selectItineraryScope(ItineraryScope.WholeTrip)
        model.selectSection(WorkspaceSection.PLACE_POOL)
        model.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()
        assertEquals(ItineraryScope.WholeTrip, model.state.value.itineraryScope)
        val reopened = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), SavedStateHandle(), clock = clock)
        advanceUntilIdle()
        reopened.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()
        assertEquals("day-2", reopened.selectedDayId.value)
    }

    @Test fun emptyPoolFilterClearsOldSearchViewportAndMarkers() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips, Places(listOf(savedPlace())))
        trips.value.value = trip()
        advanceUntilIdle()
        model.focusSearchResult(PlaceCandidate("search", "搜索地点", "", com.yangchengwei.easytrip.core.model.GeoPoint(31.2, 121.5), null))
        advanceUntilIdle()
        assertEquals(ViewportReason.SEARCH_FOCUS, model.state.value.map.viewportRequest?.reason)
        model.setPlacePoolFilter(PlacePoolMapFilter(cityKey = "no-match"))
        advanceUntilIdle()
        assertEquals(emptyList<MapMarkerUi>(), model.state.value.map.markers)
        assertEquals(null, model.state.value.map.viewportRequest)
        model.setPlacePoolFilter(PlacePoolMapFilter())
        advanceUntilIdle()
        assertEquals(listOf(savedPlace().point), model.state.value.map.viewportRequest?.points)
    }

    @Test fun consentRequiredTakesPriorityOverMapFailureIndependentlyOfLocationPrompt() {
        val state = resolveWorkspaceMapState(
            consentFact = declinedFact(),
            mapHostState = MapHostState.Failed("offline"),
        )

        assertEquals(WorkspaceMapState.ConsentRequired, state)
    }

    @Test fun mapFailureIsIndependentOfLocationPrompt() {
        val state = resolveWorkspaceMapState(
            consentFact = acceptedFact(),
            mapHostState = MapHostState.Failed("offline"),
        )

        assertEquals(WorkspaceMapState.Failed("offline"), state)
    }

    @Test fun readyMapStateDoesNotDependOnPermanentLocationDenial() {
        val state = resolveWorkspaceMapState(
            consentFact = acceptedFact(),
            mapHostState = MapHostState.Ready,
        )

        assertEquals(WorkspaceMapState.Ready, state)
    }

    @Test fun initialStateIsLoading() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        assertEquals(TripWorkspacePageState.Loading, model.pageState.value)
    }

    @Test fun nullTripBecomesNotFound() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = null
        advanceUntilIdle()
        assertEquals(TripWorkspacePageState.NotFound, model.pageState.value)
    }

    @Test fun repositoryFailureBecomesPageError() = runTest(dispatcher) {
        val model = model(Trips(failure = IllegalStateException("db")))
        advanceUntilIdle()
        assertEquals(TripWorkspacePageState.Error("无法加载旅行"), model.pageState.value)
    }

    @Test fun readyTripProducesReadyState() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = TripWithDays("trip", "北京", LocalDate.of(2026, 8, 23), TravelMode.FLEXIBLE, listOf(TripDay("day", 0)))
        advanceUntilIdle()
        assertEquals("北京", (model.pageState.value as TripWorkspacePageState.Ready).content.tripName)
    }

    @Test fun searchResultsCollapseDrawerAndDrawerChangesDoNotMoveMap() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = TripWithDays("trip", "北京", LocalDate.of(2026, 8, 23), TravelMode.FLEXIBLE, listOf(TripDay("day", 0)))
        advanceUntilIdle()
        model.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()
        model.showSearchResults(WorkspaceSearchResults("景点", listOf(
            PlaceCandidate("poi-a", "景点甲", "", GeoPoint(30.1, 120.1), null),
            PlaceCandidate("poi-b", "景点乙", "", GeoPoint(30.2, 120.2), null),
        )))
        advanceUntilIdle()
        val camera = model.state.value.map.viewportRequest
        assertEquals(WorkspaceSheetLevel.COLLAPSED, model.state.value.sheetLevel)
        assertEquals(WorkspaceSection.ITINERARY, model.state.value.section)
        assertEquals(2, model.state.value.map.markers.size)
        assertEquals(ViewportReason.SEARCH_RESULTS, camera?.reason)

        for (level in listOf(WorkspaceSheetLevel.HALF, WorkspaceSheetLevel.EXPANDED, WorkspaceSheetLevel.COLLAPSED)) {
            model.setSheetLevel(level)
            advanceUntilIdle()
            assertEquals(camera, model.state.value.map.viewportRequest)
        }
        model.selectMarker("result-poi-a")
        advanceUntilIdle()
        assertEquals("景点甲", model.state.value.selectedMapPoi?.name)
        model.clearSearchResults()
        advanceUntilIdle()
        assertNull(model.state.value.searchResults)
        assertEquals(WorkspaceSection.ITINERARY, model.state.value.section)
        assertTrue(model.state.value.map.markers.isEmpty())
    }

    @Test fun clearingSearchAfterSwitchingDayRestoresLayerWithoutRefittingMap() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = ControlledItineraries(mapOf(
            "day-1" to DayItinerary("day-1", "trip", emptyList()),
            "day-2" to DayItinerary("day-2", "trip", listOf(itineraryItem("item-1"))),
        ))
        val model = TripWorkspaceViewModel("trip", trips, Places(listOf(savedPlace())), itineraries, Legs(), SavedStateHandle())
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()
        model.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
        model.showSearchResults(WorkspaceSearchResults("景点", listOf(
            PlaceCandidate("poi-a", "景点甲", "", GeoPoint(30.1, 120.1), null),
        )))
        model.selectSection(WorkspaceSection.ITINERARY)
        model.selectItineraryScope(ItineraryScope.Day("day-2"))
        advanceUntilIdle()
        model.clearSearchResults()
        advanceUntilIdle()
        assertEquals(ItineraryScope.Day("day-2"), model.state.value.itineraryScope)
        assertNull(model.state.value.searchResults)
        assertNull(model.state.value.map.viewportRequest)
    }

    @Test fun savedSearchSnapshotRestoresOnlyInItsWorkspaceAndKeepsSelectedDay() = runTest(dispatcher) {
        val trips = Trips()
        trips.value.value = tripWithTwoDays()
        val handle = SavedStateHandle()
        val original = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), handle)
        advanceUntilIdle()
        original.selectSection(WorkspaceSection.ITINERARY)
        original.selectItineraryScope(ItineraryScope.Day("day-2"))
        val results = WorkspaceSearchResults("景点", listOf(
            PlaceCandidate("poi-a", "景点甲", "", GeoPoint(30.1, 120.1), null),
            PlaceCandidate("poi-b", "无坐标", "", null, null),
        ))
        original.showSearchResults(results)
        advanceUntilIdle()
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        val restored = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(), restoredHandle)
        val separate = model(trips)
        advanceUntilIdle()
        assertEquals(results, restored.state.value.searchResults)
        assertEquals(ItineraryScope.Day("day-2"), restored.state.value.itineraryScope)
        assertEquals(WorkspaceSection.ITINERARY, restored.state.value.section)
        assertEquals(WorkspaceSheetLevel.COLLAPSED, restored.state.value.sheetLevel)
        assertEquals(listOf("景点甲"), restored.state.value.map.markers.map { it.label })
        assertNull(separate.state.value.searchResults)
    }

    @Test fun fullPlaceIdsMissingPreventsReconciliationDespiteReadyFilteredRows() {
        assertEquals(
            AddToItineraryReconciliation.WaitForFullSnapshot,
            addToItineraryReconciliation(
                days = listOf(TripDay("day-1", 0)),
                placesReady = true,
                savedPlaceIds = null,
            ),
        )
    }

    @Test fun fullPlaceIdsTriggerReconciliationAfterReadyFilteredRows() {
        assertEquals(
            AddToItineraryReconciliation.Reconcile(listOf("day-1"), setOf("saved-place")),
            addToItineraryReconciliation(
                days = listOf(TripDay("day-1", 0)),
                placesReady = true,
                savedPlaceIds = setOf("saved-place"),
            ),
        )
    }

    @Test fun nonEmptyDaysWithEmptySnapshotsAndNoSavedPlacesExposeJointFullEmptyState() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = TripWithDays(
            "trip",
            "北京",
            LocalDate.of(2026, 8, 23),
            TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
        )
        advanceUntilIdle()

        val ready = model.pageState.value as TripWorkspacePageState.Ready
        assertEquals(true, ready.content.isItineraryAllEmpty)
        assertEquals(true, ready.content.isWorkspaceAllEmpty)
    }

    @Test fun incompleteOrMismatchedSnapshotIdsDoNotExposeFullEmptyState() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = ControlledItineraries(
            mapOf(
                "day-1" to DayItinerary("day-1", "trip", emptyList()),
                "day-2" to DayItinerary("stale-day", "trip", emptyList()),
            ),
        )
        val model = TripWorkspaceViewModel("trip", trips, Places(), itineraries, Legs(), SavedStateHandle())
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()

        val ready = model.pageState.value as TripWorkspacePageState.Ready
        assertEquals(false, ready.content.isItineraryAllEmpty)
        assertEquals(false, ready.content.isWorkspaceAllEmpty)
    }

    @Test fun itineraryItemInAnyCurrentDayDoesNotExposeFullEmptyState() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = ControlledItineraries(
            mapOf(
                "day-1" to DayItinerary("day-1", "trip", listOf(itineraryItem("item-1"))),
                "day-2" to DayItinerary("day-2", "trip", emptyList()),
            ),
        )
        val model = TripWorkspaceViewModel("trip", trips, Places(), itineraries, Legs(), SavedStateHandle())
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()

        val ready = model.pageState.value as TripWorkspacePageState.Ready
        assertEquals(false, ready.content.isItineraryAllEmpty)
        assertEquals(false, ready.content.isWorkspaceAllEmpty)
    }

    @Test fun savedPlaceKeepsWorkspaceFullEmptyFalseWhileAllItinerariesAreEmpty() = runTest(dispatcher) {
        val trips = Trips()
        val places = Places(listOf(savedPlace()))
        val model = model(trips, places)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()

        val ready = model.pageState.value as TripWorkspacePageState.Ready
        assertEquals(true, ready.content.isItineraryAllEmpty)
        assertEquals(false, ready.content.isWorkspaceAllEmpty)
    }

    @Test fun tripWithoutDaysDoesNotExposeItineraryFullEmptyState() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = TripWithDays("trip", "北京", LocalDate.of(2026, 8, 23), TravelMode.FLEXIBLE, emptyList())
        advanceUntilIdle()

        val ready = model.pageState.value as TripWorkspacePageState.Ready
        assertEquals(false, ready.content.isItineraryAllEmpty)
        assertEquals(false, ready.content.isWorkspaceAllEmpty)
    }

    @Test fun scheduleSummaryCountsRepeatedPlaceWithinOneDay() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = ControlledItineraries(
            mapOf(
                "day-1" to DayItinerary("day-1", "trip", listOf(
                    itineraryItem("item-1", "place-1"),
                    itineraryItem("item-2", "place-1"),
                )),
                "day-2" to DayItinerary("day-2", "trip", emptyList()),
            ),
        )
        val model = TripWorkspaceViewModel("trip", trips, Places(), itineraries, Legs(), SavedStateHandle())
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()

        val summary = (model.pageState.value as TripWorkspacePageState.Ready).content.schedulesByPlaceId.getValue("place-1")
        assertEquals(true, summary.isKnown)
        assertEquals(2, summary.totalOccurrences)
        assertEquals(listOf(PlaceScheduleDayUi("day-1", 0, 2)), summary.days)
    }

    @Test fun scheduleSummaryAggregatesRepeatedPlaceAcrossDaysInDayOrder() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = ControlledItineraries(
            mapOf(
                "day-1" to DayItinerary("day-1", "trip", listOf(itineraryItem("item-1", "place-1"))),
                "day-2" to DayItinerary("day-2", "trip", listOf(
                    itineraryItem("item-2", "place-1"),
                    itineraryItem("item-3", "place-2"),
                    itineraryItem("item-4", "place-1"),
                )),
            ),
        )
        val model = TripWorkspaceViewModel("trip", trips, Places(), itineraries, Legs(), SavedStateHandle())
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()

        val summary = (model.pageState.value as TripWorkspacePageState.Ready).content.schedulesByPlaceId.getValue("place-1")
        assertEquals(3, summary.totalOccurrences)
        assertEquals(
            listOf(PlaceScheduleDayUi("day-1", 0, 1), PlaceScheduleDayUi("day-2", 1, 2)),
            summary.days,
        )
    }

    @Test fun scheduleSummaryIsUnknownWhenSnapshotDayIdsAreIncomplete() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = ControlledItineraries(
            mapOf(
                "day-1" to DayItinerary("day-1", "trip", emptyList()),
                "day-2" to DayItinerary("stale-day", "trip", emptyList()),
            ),
        )
        val model = TripWorkspaceViewModel("trip", trips, Places(listOf(savedPlace())), itineraries, Legs(), SavedStateHandle())
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()

        val summary = (model.pageState.value as TripWorkspacePageState.Ready).content.schedulesByPlaceId.getValue("place-1")
        assertEquals(false, summary.isKnown)
    }

    @Test fun scheduleSummaryReportsZeroForOnlySavedPlaceAfterCompleteSnapshots() = runTest(dispatcher) {
        val trips = Trips()
        val places = Places(listOf(savedPlace()))
        val model = model(trips, places)
        trips.value.value = tripWithTwoDays()
        advanceUntilIdle()

        val summary = (model.pageState.value as TripWorkspacePageState.Ready).content.schedulesByPlaceId.getValue("place-1")
        assertEquals(true, summary.isKnown)
        assertEquals(0, summary.totalOccurrences)
        assertEquals(emptyList<PlaceScheduleDayUi>(), summary.days)
    }

    @Test fun readyTripExposesWorkspaceDateLabel() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(trips)
        trips.value.value = TripWithDays(
            "trip",
            "北京",
            LocalDate.of(2026, 8, 23),
            TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2)),
        )
        advanceUntilIdle()
        assertEquals("8月23日 — 8月25日", (model.pageState.value as TripWorkspacePageState.Ready).content.dateLabel)
    }

    @Test fun placesFailureAfterReadyBecomesError() = runTest(dispatcher) {
        val trips = Trips()
        val places = Places()
        val model = model(trips, places)
        trips.value.value = trip()
        advanceUntilIdle()
        assertEquals(true, model.pageState.value is TripWorkspacePageState.Ready)
        places.fail.value = true
        advanceUntilIdle()
        assertEquals(TripWorkspacePageState.Error("无法加载旅行"), model.pageState.value)
    }

    @Test fun itineraryFailureBecomesError() = runTest(dispatcher) {
        val trips = Trips()
        val model = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(failure = true), Legs(), SavedStateHandle())
        trips.value.value = trip()
        advanceUntilIdle()
        assertEquals(TripWorkspacePageState.Error("无法加载旅行"), model.pageState.value)
    }

    @Test fun routeFailureBecomesError() = runTest(dispatcher) {
        val trips = Trips()
        val model = TripWorkspaceViewModel("trip", trips, Places(), Itineraries(), Legs(failure = true), SavedStateHandle())
        trips.value.value = trip()
        advanceUntilIdle()
        assertEquals(TripWorkspacePageState.Error("无法加载旅行"), model.pageState.value)
    }

    @Test fun deletedDayInvalidationBeforeTripEmissionDoesNotFailWorkspace() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = DeletingDayItineraries()
        val model = TripWorkspaceViewModel("trip", trips, Places(), itineraries, Legs(), SavedStateHandle())
        trips.value.value = TripWithDays(
            "trip",
            "北京",
            LocalDate.of(2026, 8, 23),
            TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
        )
        advanceUntilIdle()

        itineraries.delete("day-2")
        advanceUntilIdle()
        trips.value.value = TripWithDays(
            "trip",
            "北京",
            LocalDate.of(2026, 8, 23),
            TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0)),
        )
        advanceUntilIdle()

        assertEquals(true, model.pageState.value is TripWorkspacePageState.Ready)
        assertEquals(listOf("day-1"), (model.pageState.value as TripWorkspacePageState.Ready).content.days.map(TripDay::id))
    }

    @Test fun missingDayStillPresentInLatestTripFailsWorkspace() = runTest(dispatcher) {
        val trips = Trips()
        val itineraries = DeletingDayItineraries()
        val model = TripWorkspaceViewModel("trip", trips, Places(), itineraries, Legs(), SavedStateHandle())
        val current = TripWithDays(
            "trip",
            "北京",
            LocalDate.of(2026, 8, 23),
            TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0)),
        )
        trips.value.value = current
        advanceUntilIdle()

        itineraries.delete("day-1")
        advanceUntilIdle()
        trips.value.value = current.copy(name = "北京更新")
        advanceUntilIdle()

        assertEquals(TripWorkspacePageState.Error("无法加载旅行"), model.pageState.value)
    }

    @Test fun searchFailureBecomesError() = runTest(dispatcher) {
        val trips = Trips()
        val model = TripWorkspaceViewModel(
            "trip",
            trips,
            Places(),
            Itineraries(),
            Legs(),
            SavedStateHandle(),
            searchResults = flow { throw IllegalStateException("search") },
        )
        trips.value.value = trip()
        advanceUntilIdle()
        assertEquals(TripWorkspacePageState.Error("无法加载旅行"), model.pageState.value)
    }

    @Test fun mapPreferencesFailureBecomesError() = runTest(dispatcher) {
        val trips = Trips()
        val model = TripWorkspaceViewModel(
            "trip",
            trips,
            Places(),
            Itineraries(),
            Legs(),
            SavedStateHandle(),
            mapPreferences = FailingMapPreferences(),
        )
        trips.value.value = trip()
        advanceUntilIdle()
        assertEquals(TripWorkspacePageState.Error("无法加载旅行"), model.pageState.value)
    }

    @Test fun retryCancelsOldAggregationAndRecoversWithOneTripSubscription() = runTest(dispatcher) {
        val trips = Trips()
        val places = Places()
        val model = model(trips, places)
        trips.value.value = trip()
        advanceUntilIdle()
        places.fail.value = true
        advanceUntilIdle()
        places.fail.value = false
        model.retry()
        advanceUntilIdle()
        assertEquals(true, model.pageState.value is TripWorkspacePageState.Ready)
        assertEquals(1, trips.activeSubscriptions)
        assertEquals(1, trips.maxSubscriptions)
    }

    private fun declinedFact() = AmapConsentFact.Declined(1)

    private fun acceptedFact(): AmapConsentFact.Accepted {
        val registry = ConsentRegistry()
        val snapshot = registry.decide(true)
        return AmapConsentFact.Accepted(1, AmapConsentToken.issue(registry, snapshot.generation))
    }

    private fun trip() = TripWithDays("trip", "北京", LocalDate.of(2026, 8, 23), TravelMode.FLEXIBLE, listOf(TripDay("day", 0)))
    private fun tripWithTwoDays() = TripWithDays(
        "trip",
        "北京",
        LocalDate.of(2026, 8, 23),
        TravelMode.FLEXIBLE,
        listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
    )
    private fun itineraryItem(id: String, placeId: String = "place-$id") = ItineraryItem(
        id,
        ItineraryPlace(placeId, "地点", "地址", com.yangchengwei.easytrip.core.model.GeoPoint(39.9, 116.4)),
        null,
        null,
    )
    private fun savedPlace() = SavedPlace(
        "place-1",
        "trip",
        "poi-1",
        "地点",
        "地址",
        com.yangchengwei.easytrip.core.model.GeoPoint(39.9, 116.4),
        "",
        emptyList(),
    )
    private fun model(trips: Trips, places: Places = Places()) = TripWorkspaceViewModel("trip", trips, places, Itineraries(), Legs(), SavedStateHandle())

    private class Trips(private val failure: Throwable? = null) : TripRepository {
        val value = MutableStateFlow<TripWithDays?>(null)
        var activeSubscriptions = 0
        var maxSubscriptions = 0
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = failure?.let { flow { throw it } } ?: callbackFlow {
            activeSubscriptions++
            maxSubscriptions = maxOf(maxSubscriptions, activeSubscriptions)
            val job = launch { value.collect { trySend(it) } }
            awaitClose { job.cancel(); activeSubscriptions-- }
        }
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun setHasTraveled(tripId: String, hasTraveled: Boolean) = Unit
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }
    private class Places(saved: List<SavedPlace> = emptyList()) : SavedPlaceRepository {
        val value = MutableStateFlow(saved)
        val fail = MutableStateFlow(false)
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = fail.flatMapLatest { broken ->
            if (broken) flow { throw IllegalStateException("places") } else value
        }
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(emptySet<String>())
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("p")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }
    private class Itineraries(private val failure: Boolean = false) : ItineraryRepository {
        override fun observeDay(dayId: String): Flow<DayItinerary> = if (failure) flow { throw IllegalStateException("itinerary") } else flowOf(DayItinerary(dayId, "trip", emptyList()))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "item"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun updateDetails(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?, note: String?) = error("Fake itinerary details are not modeled")
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class ControlledItineraries(private val days: Map<String, DayItinerary>) : ItineraryRepository {
        override fun observeDay(dayId: String): Flow<DayItinerary> = flowOf(requireNotNull(days[dayId]))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "item"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun updateDetails(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?, note: String?) = error("Fake itinerary details are not modeled")
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class DeletingDayItineraries : ItineraryRepository {
        private val days = mutableMapOf<String, MutableStateFlow<Boolean>>()
        override fun observeDay(dayId: String): Flow<DayItinerary> = days.getOrPut(dayId) { MutableStateFlow(true) }.flatMapLatest { exists ->
            if (exists) flowOf(DayItinerary(dayId, "trip", emptyList())) else flow { throw TargetDayNotFoundException(dayId) }
        }
        fun delete(dayId: String) { days.getValue(dayId).value = false }
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "item"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun updateDetails(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?, note: String?) = error("Fake itinerary details are not modeled")
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }
    @OptIn(kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi::class)
    private class FailingMapPreferences : MapPreferences {
        override val layer: StateFlow<MapLayer> = object : StateFlow<MapLayer> {
            override val replayCache: List<MapLayer> = emptyList()
            override val value: MapLayer = MapLayer.STANDARD
            override suspend fun collect(collector: FlowCollector<MapLayer>): Nothing = throw IllegalStateException("preferences")
        }
        override fun setLayer(layer: MapLayer) = Unit
    }

    private class Legs(private val failure: Boolean = false) : RouteLegRepository {
        override fun observeDay(dayId: String): Flow<List<com.yangchengwei.easytrip.route.data.RouteLegEntity>> = if (failure) flow { throw IllegalStateException("routes") } else flowOf(emptyList())
        override fun observePending(): Flow<List<RouteLegWithEndpoints>> = flowOf(emptyList())
        override suspend fun get(legId: String) = null
        override suspend fun requeueTransientFailures() = 0
        override suspend fun recoverInterruptedCalculations(online: Boolean) = 0
        override suspend fun repairCorruptPolyline(legId: String, version: Long) = false
        override suspend fun claimIfVersionMatches(legId: String, version: Long) = false
        override suspend fun waitForNetworkIfVersionMatches(legId: String, version: Long) = false
        override suspend fun releaseClaimIfVersionMatches(legId: String, version: Long, online: Boolean) = false
        override suspend fun completeIfVersionMatches(legId: String, version: Long, result: RouteResult) = false
        override suspend fun failIfVersionMatches(legId: String, version: Long, failure: RoutePlanOutcome.Failure) = false
        override suspend fun updateDetails(legId: String, selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?, durationOverrideSeconds: Int?, note: String?, online: Boolean): Boolean = error("Fake route details are not modeled")
        override suspend fun retry(legId: String, online: Boolean) = false
    }
}
