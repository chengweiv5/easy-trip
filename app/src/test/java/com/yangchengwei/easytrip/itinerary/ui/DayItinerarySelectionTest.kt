package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints
import com.yangchengwei.easytrip.route.domain.RoutePlanOutcome
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DayItinerarySelectionTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `external day recovers after transient trip snapshot omission without re-emission`() = runTest(dispatcher) {
        val trips = Trips(listOf(TripDay("day-1", 0), TripDay("day-2", 1)))
        val selected = MutableStateFlow<String?>("day-2")
        val model = model(trips, selected)
        advanceUntilIdle()
        assertEquals("day-2", model.state.value.selectedDayId)
        assertEquals(listOf("item-2"), model.state.value.items.map(ItineraryItemUi::id))

        trips.value.value = trip(listOf(TripDay("day-1", 0)))
        advanceUntilIdle()
        assertNull(model.state.value.selectedDayId)
        assertEquals(emptyList<ItineraryItemUi>(), model.state.value.items)

        trips.value.value = trip(listOf(TripDay("day-1", 0), TripDay("day-2", 1)))
        advanceUntilIdle()
        assertEquals("day-2", model.state.value.selectedDayId)
        assertEquals(listOf("item-2"), model.state.value.items.map(ItineraryItemUi::id))
    }

    @Test fun `append stays busy blocks duplicate and keeps current selection`() = runTest(dispatcher) {
        val trips = Trips(listOf(TripDay("day-1", 0)))
        val pending = CompletableDeferred<Unit>()
        trips.insertGate = pending
        val model = model(trips, MutableStateFlow("day-1"))
        advanceUntilIdle()

        model.appendTripDay()
        model.appendTripDay()
        dispatcher.scheduler.runCurrent()

        assertEquals(true, model.state.value.isAppendingDay)
        assertEquals(1, trips.insertCalls)
        pending.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, model.state.value.isAppendingDay)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
        assertEquals("day-1", model.state.value.selectedDayId)

        model.appendTripDay()
        advanceUntilIdle()
        assertEquals(1, trips.insertCalls)

        model.consumeAppendDayCompletion(1L)
        assertNull(model.state.value.appendDayCompletionToken)
        model.appendTripDay()
        advanceUntilIdle()
        assertEquals(2L, model.state.value.appendDayCompletionToken)
        assertEquals(2, trips.insertCalls)
    }

    @Test fun `append failure remains visible and can retry`() = runTest(dispatcher) {
        val trips = Trips(emptyList())
        trips.insertFailure = IllegalStateException("新增失败")
        val model = model(trips, MutableStateFlow(null))
        advanceUntilIdle()

        model.appendTripDay()
        advanceUntilIdle()

        assertEquals("新增失败", model.state.value.appendDayError)
        assertEquals(false, model.state.value.isAppendingDay)
        trips.insertFailure = null
        model.appendTripDay()
        advanceUntilIdle()
        assertNull(model.state.value.appendDayError)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
        assertEquals(2, trips.insertCalls)
    }

    @Test fun `hidden external selection clears editing and dialog state`() = runTest(dispatcher) {
        val selected = MutableStateFlow<String?>("day-2")
        val model = model(Trips(listOf(TripDay("day-1", 0), TripDay("day-2", 1))), selected)
        advanceUntilIdle()
        model.previewMove("item-2", 0)
        model.requestTiming("item-2")
        model.requestCrossDay("item-2")
        model.requestDelete("item-2")
        model.requestMode("leg-2")

        selected.value = null
        advanceUntilIdle()

        assertNull(model.state.value.selectedDayId)
        assertEquals(emptyList<ItineraryItemUi>(), model.state.value.items)
        assertEquals(emptyList<RouteLegUi>(), model.state.value.legs)
        assertEquals(emptyList<String>(), model.state.value.previewOrder)
        assertNull(model.state.value.timingItemId)
        assertNull(model.state.value.moveItemId)
        assertNull(model.state.value.deleteItemId)
        assertNull(model.state.value.modeLegId)
    }

    private fun model(trips: Trips, selected: Flow<String?>) = DayItineraryViewModel(
        "trip",
        trips,
        Itineraries(),
        Legs(),
        null,
        selectedDays = selected,
        tripService = TripService(trips),
    )

    private fun trip(days: List<TripDay>) = TripWithDays("trip", "Trip", LocalDate.of(2026, 8, 23), TravelMode.FLEXIBLE, days)

    private inner class Trips(days: List<TripDay>) : TripRepository {
        val value = MutableStateFlow<TripWithDays?>(trip(days))
        var insertGate: CompletableDeferred<Unit>? = null
        var insertFailure: Throwable? = null
        var insertCalls = 0
        override fun observeTrip(tripId: String) = value
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String {
            insertCalls++
            insertGate?.await()
            insertFailure?.let { throw it }
            val nextId = "day-${value.value!!.days.size + 1}"
            val days = value.value!!.days + TripDay(nextId, value.value!!.days.size)
            value.value = trip(days)
            return nextId
        }
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(dayId: String) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class Itineraries : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(
            DayItinerary(
                dayId,
                "trip",
                listOf(ItineraryItem("item-${dayId.removePrefix("day-")}", ItineraryPlace("place", "Place", "", GeoPoint(1.0, 2.0)), null, null)),
            ),
        )
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "item"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Legs : RouteLegRepository {
        override fun observeDay(dayId: String) = flowOf(emptyList<RouteLegEntity>())
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
        override suspend fun overrideMode(legId: String, mode: TransportMode, online: Boolean) = false
        override suspend fun retry(legId: String, online: Boolean) = false
    }
}
