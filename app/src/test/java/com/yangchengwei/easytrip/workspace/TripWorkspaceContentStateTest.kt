package com.yangchengwei.easytrip.workspace

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TripWorkspaceContentStateTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

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
    private class Places(private val saved: List<SavedPlace> = emptyList()) : SavedPlaceRepository {
        val fail = MutableStateFlow(false)
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = fail.flatMapLatest { broken ->
            if (broken) flow { throw IllegalStateException("places") } else flowOf(saved)
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
