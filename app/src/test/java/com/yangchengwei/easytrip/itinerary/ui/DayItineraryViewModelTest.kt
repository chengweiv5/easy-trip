package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints
import com.yangchengwei.easytrip.route.domain.RoutePlanOutcome
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DayItineraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `editing time and duration keeps real item identity and order`() = runTest(dispatcher) {
        val repository = Itineraries()
        val model = model(repository)
        advanceUntilIdle()

        model.requestTiming("item-beta")
        model.updateArrivalTime("09:45")
        model.updateStayMinutes("75")

        assertEquals("item-beta", model.state.value.editDraft?.itemId)
        assertEquals("09:45", model.state.value.editDraft?.arrivalTimeText)
        assertEquals("75", model.state.value.editDraft?.stayMinutesText)
        assertEquals(listOf("item-alpha", "item-beta"), model.state.value.previewOrder)
    }

    @Test fun `save pending blocks duplicate and failure keeps latest draft open`() = runTest(dispatcher) {
        val repository = Itineraries()
        val gate = CompletableDeferred<Unit>()
        repository.timingGate = gate
        repository.timingFailure = IllegalStateException("保存失败")
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")
        model.updateArrivalTime("08:30")
        model.updateStayMinutes("90")

        model.saveTiming()
        model.saveTiming()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.timingCalls.size)
        assertTrue(model.state.value.editDraft!!.isSaving)

        model.updateArrivalTime("08:45")
        model.updateStayMinutes("105")
        gate.complete(Unit)
        advanceUntilIdle()

        val draft = model.state.value.editDraft!!
        assertEquals("item-alpha", draft.itemId)
        assertEquals("08:45", draft.arrivalTimeText)
        assertEquals("105", draft.stayMinutesText)
        assertEquals("保存失败", draft.saveError)
        assertFalse(draft.isSaving)
    }

    @Test fun `retry saves current draft and clears editor only after success`() = runTest(dispatcher) {
        val repository = Itineraries().apply { timingFailure = IllegalStateException("失败") }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")
        model.updateArrivalTime("08:30")
        model.updateStayMinutes("90")
        model.saveTiming()
        advanceUntilIdle()
        model.updateArrivalTime("10:15")
        model.updateStayMinutes("120")
        repository.timingFailure = null

        model.saveTiming()
        advanceUntilIdle()

        assertEquals(Timing("item-alpha", LocalTime.of(10, 15), 120), repository.timingCalls.last())
        assertNull(model.state.value.editDraft)
    }

    @Test fun `delete pending blocks duplicate and failure keeps named confirmation`() = runTest(dispatcher) {
        val repository = Itineraries()
        val gate = CompletableDeferred<Unit>()
        repository.deleteGate = gate
        repository.deleteFailure = IllegalStateException("删除失败")
        val model = model(repository)
        advanceUntilIdle()
        model.requestDelete("item-beta")

        model.confirmDelete()
        model.confirmDelete()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("item-beta"), repository.deleteCalls)
        assertEquals("博物馆", model.state.value.deleteConfirmation?.placeName)
        assertTrue(model.state.value.deleteConfirmation!!.isDeleting)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals("删除失败", model.state.value.deleteConfirmation?.deleteError)
        assertFalse(model.state.value.deleteConfirmation!!.isDeleting)
    }

    @Test fun `successful delete clears confirmation`() = runTest(dispatcher) {
        val repository = Itineraries()
        val model = model(repository)
        advanceUntilIdle()
        model.requestDelete("item-alpha")

        model.confirmDelete()
        advanceUntilIdle()

        assertNull(model.state.value.deleteConfirmation)
    }

    @Test fun `stale save completion cannot clear newer editor for same item`() = runTest(dispatcher) {
        val repository = Itineraries()
        val gate = CompletableDeferred<Unit>()
        repository.timingGate = gate
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")
        model.updateArrivalTime("08:30")
        model.saveTiming()
        dispatcher.scheduler.runCurrent()

        assertTrue(model.requestTiming("item-alpha"))
        model.updateArrivalTime("10:15")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("item-alpha", model.state.value.editDraft?.itemId)
        assertEquals("10:15", model.state.value.editDraft?.arrivalTimeText)
        assertFalse(model.state.value.editDraft!!.isSaving)
    }

    @Test fun `stale delete failure cannot modify newer confirmation`() = runTest(dispatcher) {
        val repository = Itineraries()
        val gate = CompletableDeferred<Unit>()
        repository.deleteGate = gate
        repository.deleteFailure = IllegalStateException("旧删除失败")
        val model = model(repository)
        advanceUntilIdle()
        model.requestDelete("item-alpha")
        model.confirmDelete()
        dispatcher.scheduler.runCurrent()

        assertTrue(model.requestDelete("item-beta"))
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("item-beta", model.state.value.deleteConfirmation?.itemId)
        assertNull(model.state.value.deleteConfirmation?.deleteError)
        assertFalse(model.state.value.deleteConfirmation!!.isDeleting)
    }

    @Test fun `request edit and delete reject missing item`() = runTest(dispatcher) {
        val model = model(Itineraries())
        advanceUntilIdle()

        assertFalse(model.requestTiming("missing"))
        assertFalse(model.requestDelete("missing"))
        assertNull(model.state.value.editDraft)
        assertNull(model.state.value.deleteConfirmation)
    }

    @Test fun `cancellation is rethrown without becoming save error`() = runTest(dispatcher) {
        val repository = Itineraries().apply { timingFailure = CancellationException("cancelled") }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")

        model.saveTiming()
        advanceUntilIdle()

        assertNull(model.state.value.editDraft?.saveError)
    }

    @Test fun `route legs expose explicit ready waiting and failed states`() = runTest(dispatcher) {
        val model = model(
            Itineraries(),
            legs = Legs(
                listOf(
                    legEntity("ready", RouteStatus.SUCCESS, distance = 1200, duration = 300),
                    legEntity("waiting", RouteStatus.WAITING_NETWORK),
                    legEntity("failed", RouteStatus.FAILED),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(RouteLegUiState.Ready(TransportMode.TAXI, 5, 1200), model.state.value.legs[0].state)
        assertEquals(RouteLegUiState.WaitingForNetwork, model.state.value.legs[1].state)
        assertEquals(RouteLegUiState.Failed("路线规划失败"), model.state.value.legs[2].state)
    }

    @Test fun `failed same day reorder rolls preview back to official order`() = runTest(dispatcher) {
        val repository = Itineraries().apply { moveFailure = IllegalStateException("排序失败") }
        val model = model(repository)
        advanceUntilIdle()

        model.previewMove("item-beta", 0)
        assertEquals(listOf("item-beta", "item-alpha"), model.state.value.previewOrder)
        model.commitMove("item-beta", 0)
        advanceUntilIdle()

        assertEquals(listOf("item-alpha", "item-beta"), model.state.value.previewOrder)
        assertEquals("排序失败", model.state.value.error)
    }

    @Test fun `cross day failure keeps target and error for retry then closes after success`() = runTest(dispatcher) {
        val repository = Itineraries().apply { moveFailure = IllegalStateException("移动失败") }
        val model = model(repository)
        advanceUntilIdle()
        model.requestCrossDay("item-alpha")

        model.moveToDay("day-2")
        advanceUntilIdle()

        assertEquals("item-alpha", model.state.value.crossDayMove?.itemId)
        assertEquals("day-2", model.state.value.crossDayMove?.targetDayId)
        assertEquals("移动失败", model.state.value.crossDayMove?.moveError)
        repository.moveFailure = null
        model.moveToDay("day-2")
        advanceUntilIdle()
        assertNull(model.state.value.crossDayMove)
    }

    @Test fun `transport mode request rejects missing leg`() = runTest(dispatcher) {
        val model = model(Itineraries())
        advanceUntilIdle()

        assertFalse(model.requestMode("missing"))
        assertNull(model.state.value.modeEditor)
    }

    @Test fun `transport mode failure keeps editor draft and retry closes only after success`() = runTest(dispatcher) {
        val coordinator = Coordinator().apply { overrideResult = false }
        val model = model(
            Itineraries(),
            coordinator,
            Legs(listOf(legEntity("leg-alpha", RouteStatus.SUCCESS))),
        )
        advanceUntilIdle()
        model.requestMode("leg-alpha")
        model.selectMode(TransportMode.WALK)

        model.overrideMode()
        advanceUntilIdle()

        assertEquals("leg-alpha", model.state.value.modeEditor?.legId)
        assertEquals(TransportMode.WALK, model.state.value.modeEditor?.selectedMode)
        assertEquals("联网并同意高德隐私政策后才能更新交通方式", model.state.value.modeEditor?.saveError)
        coordinator.overrideResult = true
        model.overrideMode()
        advanceUntilIdle()
        assertNull(model.state.value.modeEditor)
    }

    @Test fun `transport mode pending blocks duplicate and stale completion cannot alter new leg`() = runTest(dispatcher) {
        val coordinator = Coordinator().apply { overrideGate = CompletableDeferred() }
        val model = model(
            Itineraries(),
            coordinator,
            Legs(
                listOf(
                    legEntity("leg-alpha", RouteStatus.SUCCESS),
                    legEntity("leg-beta", RouteStatus.SUCCESS),
                ),
            ),
        )
        advanceUntilIdle()
        model.requestMode("leg-alpha")
        model.selectMode(TransportMode.WALK)
        model.overrideMode()
        model.overrideMode()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("leg-alpha" to TransportMode.WALK), coordinator.overrides)
        assertTrue(model.state.value.modeEditor!!.isSaving)

        model.requestMode("leg-beta")
        model.selectMode(TransportMode.DRIVE)
        coordinator.overrideGate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals("leg-beta", model.state.value.modeEditor?.legId)
        assertEquals(TransportMode.DRIVE, model.state.value.modeEditor?.selectedMode)
        assertFalse(model.state.value.modeEditor!!.isSaving)
    }

    private fun model(
        repository: Itineraries,
        coordinator: RouteRefreshCoordinator? = null,
        legs: RouteLegRepository = Legs(),
    ) = DayItineraryViewModel(
        "trip",
        Trips(),
        repository,
        legs,
        coordinator,
        selectedDays = flowOf("day-1"),
    )

    private class Itineraries : ItineraryRepository {
        var timingGate: CompletableDeferred<Unit>? = null
        var timingFailure: Throwable? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        var deleteFailure: Throwable? = null
        var moveFailure: Throwable? = null
        val timingCalls = mutableListOf<Timing>()
        val deleteCalls = mutableListOf<String>()
        override fun observeDay(dayId: String) = flowOf(
            DayItinerary(
                dayId,
                "trip",
                listOf(
                    ItineraryItem("item-alpha", ItineraryPlace("place-alpha", "酒店", "地址 A", GeoPoint(1.0, 2.0)), LocalTime.of(8, 0), 60),
                    ItineraryItem("item-beta", ItineraryPlace("place-beta", "博物馆", "地址 B", GeoPoint(3.0, 4.0)), null, null),
                ),
            ),
        )
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "new"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) {
            moveFailure?.let { throw it }
        }
        override suspend fun deleteItem(itemId: String) {
            deleteCalls += itemId
            deleteGate?.await()
            deleteFailure?.let { throw it }
        }
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) {
            timingCalls += Timing(itemId, arrivalTime, stayMinutes)
            timingGate?.await()
            timingFailure?.let { throw it }
        }
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Trips : TripRepository {
        override fun observeTrip(tripId: String) = flowOf(TripWithDays("trip", "Trip", LocalDate.of(2026, 8, 25), TravelMode.FLEXIBLE, listOf(TripDay("day-1", 0))))
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

    private class Coordinator : RouteRefreshCoordinator {
        var overrideResult = true
        var overrideGate: CompletableDeferred<Unit>? = null
        val overrides = mutableListOf<Pair<String, TransportMode>>()
        override fun start(scope: kotlinx.coroutines.CoroutineScope) = Unit
        override suspend fun retry(legId: String) = true
        override suspend fun overrideMode(legId: String, mode: TransportMode): Boolean {
            overrides += legId to mode
            overrideGate?.await()
            return overrideResult
        }
    }

    private class Legs(private val values: List<RouteLegEntity> = emptyList()) : RouteLegRepository {
        override fun observeDay(dayId: String) = flowOf(values)
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

    private fun legEntity(
        id: String,
        status: RouteStatus,
        distance: Int? = null,
        duration: Int? = null,
    ) = RouteLegEntity(
        id = id,
        tripDayId = "day-1",
        fromItemId = "item-alpha",
        toItemId = "item-beta",
        recommendedMode = TransportMode.TAXI,
        status = status,
        distanceMeters = distance,
        durationSeconds = duration,
        version = 1,
        updatedAt = Instant.EPOCH,
    )

    private data class Timing(val itemId: String, val time: LocalTime?, val minutes: Int?)
}
