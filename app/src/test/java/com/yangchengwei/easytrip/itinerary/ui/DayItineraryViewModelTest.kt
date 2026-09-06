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
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
        assertEquals(1, repository.detailCalls.size)
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

    @Test fun `save failure keeps the complete draft and clear failure keeps it open without another repository call`() = runTest(dispatcher) {
        val repository = Itineraries().apply { timingFailure = IllegalStateException("保存失败") }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")
        model.updateArrivalTime("08:30")
        model.updateStayMinutes("90")
        model.updateNote("保留\n备注")
        val generation = requireNotNull(model.state.value.editDraft).generation

        model.saveTiming()
        advanceUntilIdle()

        assertEquals(
            ItineraryEditDraft("item-alpha", "08:30", "90", "保留\n备注", placeId = "place-alpha", placeName = "酒店", saveError = "保存失败", generation = generation),
            model.state.value.editDraft,
        )
        val persistedItem = model.state.value.items.first { it.id == "item-alpha" }
        assertEquals(LocalTime.of(8, 0), persistedItem.arrivalTime)
        assertEquals(60, persistedItem.stayMinutes)
        assertEquals("已有\n备注", persistedItem.note)
        val callsBeforeClear = repository.detailCalls.size
        model.dispatch(DayItineraryAction.DismissEditSaveError)

        assertEquals(
            ItineraryEditDraft("item-alpha", "08:30", "90", "保留\n备注", placeId = "place-alpha", placeName = "酒店", generation = generation),
            model.state.value.editDraft,
        )
        assertEquals(callsBeforeClear, repository.detailCalls.size)

        repository.timingFailure = null
        model.dispatch(DayItineraryAction.SaveEdit)
        advanceUntilIdle()

        assertEquals(callsBeforeClear + 1, repository.detailCalls.size)
        assertEquals(Details("item-alpha", LocalTime.of(8, 30), 90, "保留\n备注"), repository.detailCalls.last())
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
        assertTrue(repository.deleteCalls.isEmpty())

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

    @Test fun `append waits for observed appended suffix before emitting one completion token`() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertTrue(model.state.value.isAppendingDay)
        assertNull(model.state.value.appendDayCompletionToken)

        trips.emitAppendedDay()
        advanceUntilIdle()

        assertFalse(model.state.value.isAppendingDay)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
        model.consumeAppendDayCompletion(1L)
        model.consumeAppendDayCompletion(1L)
        assertNull(model.state.value.appendDayCompletionToken)
    }

    @Test fun `append failure keeps retryable overlay and stale completion cannot finish retry`() = runTest(dispatcher) {
        val trips = Trips().apply { insertFailure = IllegalStateException("新增失败") }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()
        assertEquals("新增失败", model.state.value.appendDayError)
        assertFalse(model.state.value.isAppendingDay)

        trips.insertFailure = null
        model.appendTripDay()
        advanceUntilIdle()
        trips.emit(TripWithDays("trip", "Trip", LocalDate.of(2026, 8, 25), TravelMode.FLEXIBLE, listOf(TripDay("wrong", 0), TripDay("new", 1))))
        advanceUntilIdle()

        assertTrue(model.state.value.isAppendingDay)
        assertNull(model.state.value.appendDayCompletionToken)

        trips.emit(TripWithDays("trip", "Trip", LocalDate.of(2026, 8, 25), TravelMode.FLEXIBLE, listOf(TripDay("day-1", 0), TripDay("day-new", 1))))
        advanceUntilIdle()
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `observation failure after append write retries observation without another write`() = runTest(dispatcher) {
        val trips = Trips().apply { stopObservationAfterInsert = true }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(2, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        trips.stopObservationAfterInsert = false

        val activeObserveCalls = trips.observeCalls
        model.appendTripDay()
        advanceUntilIdle()
        trips.emitAppendedDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(activeObserveCalls, trips.observeCalls)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `returned appended day id completes despite concurrent baseline reorder`() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()
        trips.emit(TripWithDays("trip", "Trip", LocalDate.of(2026, 8, 25), TravelMode.FLEXIBLE, listOf(TripDay("day-new", 0))))
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `observation without returned appended day id does not complete`() = runTest(dispatcher) {
        val trips = Trips()
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()
        trips.emit(TripWithDays("trip", "Trip", LocalDate.of(2026, 8, 25), TravelMode.FLEXIBLE, listOf(TripDay("day-1", 0), TripDay("other", 1))))
        advanceUntilIdle()

        assertTrue(model.state.value.isAppendingDay)
        assertNull(model.state.value.appendDayCompletionToken)
    }

    @Test fun `append cancellation clears pending request for a later retry`() = runTest(dispatcher) {
        val trips = Trips().apply { insertFailure = CancellationException("cancelled") }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()
        assertFalse(model.state.value.isAppendingDay)
        assertNull(model.state.value.appendDayError)

        trips.insertFailure = null
        model.appendTripDay()
        advanceUntilIdle()
        trips.emitAppendedDay()
        advanceUntilIdle()

        assertEquals(2, trips.insertCalls)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `observation error before append service failure restores ordinary trip observation`() = runTest(dispatcher) {
        val trips = Trips().apply { insertGate = CompletableDeferred() }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        assertEquals(0, trips.activeObservationCount)

        trips.insertFailure = IllegalStateException("新增失败")
        trips.insertGate!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(model.state.value.isAppendingDay)
        assertEquals("新增失败", model.state.value.appendDayError)

        assertEquals(2, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        trips.emit(
            TripWithDays(
                "trip",
                "Trip",
                LocalDate.of(2026, 8, 25),
                TravelMode.FLEXIBLE,
                listOf(TripDay("day-1", 0), TripDay("day-later", 1)),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf("day-1", "day-later"), model.state.value.days.map(TripDay::id))
    }

    @Test fun `observation error before append service cancellation restores ordinary trip observation`() = runTest(dispatcher) {
        val trips = Trips().apply { insertGate = CompletableDeferred() }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        assertEquals(0, trips.activeObservationCount)

        trips.insertFailure = CancellationException("cancelled")
        trips.insertGate!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(model.state.value.isAppendingDay)
        assertNull(model.state.value.appendDayError)

        assertEquals(2, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        trips.emit(
            TripWithDays(
                "trip",
                "Trip",
                LocalDate.of(2026, 8, 25),
                TravelMode.FLEXIBLE,
                listOf(TripDay("day-1", 0), TripDay("day-later", 1)),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf("day-1", "day-later"), model.state.value.days.map(TripDay::id))
    }

    @Test fun `observation error during append write keeps single flight then automatically resubscribes after service succeeds`() = runTest(dispatcher) {
        val trips = Trips().apply { insertGate = CompletableDeferred() }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()

        assertTrue(model.state.value.isAppendingDay)
        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, trips.insertCalls)

        trips.stopObservationFailures()
        trips.insertGate!!.complete(Unit)
        advanceUntilIdle()
        trips.emitAppendedDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `observation completion during append write keeps single flight then automatically resubscribes after service succeeds`() = runTest(dispatcher) {
        val trips = Trips().apply {
            insertGate = CompletableDeferred()
            completeObservationOnDemand = true
        }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.completeObservation()
        advanceUntilIdle()

        assertTrue(model.state.value.isAppendingDay)
        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, trips.insertCalls)

        trips.completeObservationOnDemand = false
        trips.insertGate!!.complete(Unit)
        advanceUntilIdle()
        trips.emitAppendedDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `termination marker race on normal completion restarts after in flight service success`() = runTest(dispatcher) {
        val trips = Trips().apply { insertGate = CompletableDeferred() }
        trips.prepareCoordinatedTermination(error = false)
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.terminateObservation()
        trips.terminationEntered.await()

        assertTrue(model.state.value.isAppendingDay)
        trips.coordinatedTermination = false
        trips.insertGate!!.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, trips.insertCalls)
        trips.terminationRelease!!.complete(Unit)
        advanceUntilIdle()
        trips.emitAppendedDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `termination marker race on error restarts after in flight service success`() = runTest(dispatcher) {
        val trips = Trips().apply { insertGate = CompletableDeferred() }
        trips.prepareCoordinatedTermination(error = true)
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.terminateObservation()
        trips.terminationEntered.await()

        assertTrue(model.state.value.isAppendingDay)
        trips.coordinatedTermination = false
        trips.insertGate!!.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, trips.insertCalls)
        trips.terminationRelease!!.complete(Unit)
        advanceUntilIdle()
        trips.emitAppendedDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `observation termination after append write retries observation and confirms existing append`() = runTest(dispatcher) {
        val trips = Trips().apply { stopObservationAfterInsert = true }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()
        assertEquals(2, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        trips.stopObservationAfterInsert = false

        val activeObserveCalls = trips.observeCalls
        model.appendTripDay()
        advanceUntilIdle()
        trips.emitAppendedDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(activeObserveCalls, trips.observeCalls)
        assertEquals(1L, model.state.value.appendDayCompletionToken)
    }

    @Test fun `automatic retry stops after a second immediate observation error`() = runTest(dispatcher) {
        val trips = Trips().apply {
            stopObservationAfterInsert = true
            resubscriptionTerminations += ObservationTermination.ERROR
        }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(2, trips.observeCalls)
        assertEquals(0, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        assertFalse(model.state.value.isAppendingDay)
        assertEquals("新增旅行日等待同步失败，请重试", model.state.value.appendDayError)
    }

    @Test fun `automatic retry stops after a second immediate observation completion`() = runTest(dispatcher) {
        val trips = Trips().apply {
            stopObservationAfterInsert = true
            resubscriptionTerminations += ObservationTermination.COMPLETE
        }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(2, trips.observeCalls)
        assertEquals(0, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        assertFalse(model.state.value.isAppendingDay)
        assertEquals("新增旅行日等待同步失败，请重试", model.state.value.appendDayError)
    }

    @Test fun `explicit retries reuse an active automatic observation collector`() = runTest(dispatcher) {
        val trips = Trips().apply { stopObservationAfterInsert = true }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        advanceUntilIdle()
        assertEquals(2, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)

        val activeObserveCalls = trips.observeCalls
        repeat(3) { model.appendTripDay() }
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(activeObserveCalls, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        assertTrue(model.state.value.isAppendingDay)
        assertNull(model.state.value.appendDayError)
    }

    @Test fun `explicit retry clears pre service error and reuses active automatic collector`() = runTest(dispatcher) {
        val trips = Trips().apply { insertGate = CompletableDeferred() }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()

        assertTrue(model.state.value.isAppendingDay)
        assertEquals("无法加载旅行日，正在等待同步", model.state.value.appendDayError)

        trips.insertGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        val activeObserveCalls = trips.observeCalls

        repeat(3) { model.appendTripDay() }
        advanceUntilIdle()

        assertEquals(1, trips.insertCalls)
        assertEquals(activeObserveCalls, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
        assertTrue(model.state.value.isAppendingDay)
        assertNull(model.state.value.appendDayError)
    }

    @Test fun `matched final emission still restores observation after termination handler runs`() = runTest(dispatcher) {
        val trips = Trips().apply { prepareCoordinatedTermination(error = false, emitCurrentBeforeHandler = true) }
        val model = model(Itineraries(), trips = trips)
        dispatcher.scheduler.runCurrent()

        model.appendTripDay()
        dispatcher.scheduler.runCurrent()
        trips.setAppendedDay()
        trips.terminateObservation()
        trips.terminationEntered.await()

        assertEquals(1L, model.state.value.appendDayCompletionToken)
        model.consumeAppendDayCompletion(1L)
        trips.coordinatedTermination = false
        trips.terminationRelease!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)

        val restoredObserveCalls = trips.observeCalls
        trips.emit(
            TripWithDays(
                "trip",
                "Trip",
                LocalDate.of(2026, 8, 25),
                TravelMode.FLEXIBLE,
                listOf(TripDay("day-1", 0), TripDay("day-new", 1), TripDay("day-later", 2)),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf("day-1", "day-new", "day-later"), model.state.value.days.map(TripDay::id))
        assertEquals(restoredObserveCalls, trips.observeCalls)
        assertEquals(1, trips.activeObservationCount)
        assertEquals(1, trips.maxActiveObservationCount)
    }

    @Test fun `pending and calculating route statuses keep distinct ui states`() = runTest(dispatcher) {
        val model = model(
            Itineraries(),
            legs = Legs(
                listOf(
                    legEntity("pending", RouteStatus.PENDING),
                    legEntity("calculating", RouteStatus.CALCULATING),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(RouteLegUiState.Pending, RouteLegUiState.Calculating),
            model.state.value.legs.map(RouteLegUi::state),
        )
    }

    @Test fun `success maps to ready with distance and duration`() = runTest(dispatcher) {
        val model = model(
            Itineraries(),
            legs = Legs(listOf(legEntity("ready", RouteStatus.SUCCESS, distance = 1200, duration = 300))),
        )
        advanceUntilIdle()

        assertEquals(RouteLegUiState.Ready(1200, 300), model.state.value.legs.single().state)
    }

    @Test fun `waiting network maps to waiting state`() = runTest(dispatcher) {
        val model = model(
            Itineraries(),
            legs = Legs(listOf(legEntity("waiting", RouteStatus.WAITING_NETWORK))),
        )
        advanceUntilIdle()

        assertEquals(RouteLegUiState.WaitingForNetwork, model.state.value.legs.single().state)
    }

    @Test fun `failed route uses legacy error code when typed error is unavailable`() {
        val route = legEntity("legacy-failed", RouteStatus.FAILED).copy(errorCode = "no route")

        assertEquals(RouteLegUiState.Failed("no route"), route.toRouteLegUi().state)
    }

    @Test fun `retry ignores a leg that has become ready and preserves ready route data`() = runTest(dispatcher) {
        val coordinator = Coordinator()
        val legs = Legs(listOf(legEntity("retry-target", RouteStatus.FAILED)))
        val model = model(Itineraries(), coordinator, legs)
        advanceUntilIdle()

        legs.emit(
            listOf(
                legEntity("retry-target", RouteStatus.SUCCESS, distance = 1_200, duration = 300).copy(version = 2),
                legEntity("ready-sibling", RouteStatus.SUCCESS, distance = 800, duration = 180).copy(version = 4),
            ),
        )
        advanceUntilIdle()
        model.retry("retry-target", expectedVersion = 1)
        advanceUntilIdle()

        assertTrue(coordinator.retries.isEmpty())
        assertEquals(
            listOf(
                RouteLegUiState.Ready(1_200, 300),
                RouteLegUiState.Ready(800, 180),
            ),
            model.state.value.legs.map(RouteLegUi::state),
        )
    }

    @Test fun `retry ignores a stale failed version`() = runTest(dispatcher) {
        val coordinator = Coordinator()
        val legs = Legs(listOf(legEntity("retry-target", RouteStatus.FAILED).copy(version = 1)))
        val model = model(Itineraries(), coordinator, legs)
        advanceUntilIdle()

        legs.emit(listOf(legEntity("retry-target", RouteStatus.FAILED).copy(version = 2)))
        advanceUntilIdle()
        model.dispatch(DayItineraryAction.Retry("retry-target", expectedVersion = 1))
        advanceUntilIdle()

        assertTrue(coordinator.retries.isEmpty())
        assertEquals(RouteLegUiState.Failed("路线计算失败"), model.state.value.legs.single().state)
    }

    @Test fun `failed retry calls coordinator once and leaves ready sibling intact`() = runTest(dispatcher) {
        val coordinator = Coordinator()
        val model = model(
            Itineraries(),
            coordinator,
            Legs(
                listOf(
                    legEntity("failed", RouteStatus.FAILED),
                    legEntity("ready-sibling", RouteStatus.SUCCESS, distance = 800, duration = 180).copy(version = 4),
                ),
            ),
        )
        advanceUntilIdle()

        model.dispatch(DayItineraryAction.Retry("failed", expectedVersion = 1))
        advanceUntilIdle()

        assertEquals(listOf("failed" to 1L), coordinator.retries)
        assertEquals(
            RouteLegUiState.Ready(800, 180),
            model.state.value.legs.single { it.id == "ready-sibling" }.state,
        )
    }

    @Test fun `duplicate failed retry is single flight`() = runTest(dispatcher) {
        val coordinator = Coordinator().apply { overrideGate = CompletableDeferred() }
        val model = model(
            Itineraries(),
            coordinator,
            Legs(listOf(legEntity("failed", RouteStatus.FAILED))),
        )
        advanceUntilIdle()

        model.dispatch(DayItineraryAction.Retry("failed", expectedVersion = 1))
        dispatcher.scheduler.runCurrent()
        model.dispatch(DayItineraryAction.Retry("failed", expectedVersion = 1))
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("failed" to 1L), coordinator.retries)
        coordinator.overrideGate?.complete(Unit)
        advanceUntilIdle()
        assertNull(model.state.value.error)
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

    @Test fun `stale same day move failure cannot replace newer flow order or error`() = runTest(dispatcher) {
        val repository = Itineraries()
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()
        repository.moveGates += firstGate
        repository.moveGates += secondGate
        val model = model(repository)
        advanceUntilIdle()

        model.previewMove("item-beta", 0)
        model.commitMove("item-beta", 0)
        dispatcher.scheduler.runCurrent()
        model.previewMove("item-beta", 1)
        model.commitMove("item-beta", 0)
        dispatcher.scheduler.runCurrent()

        secondGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("item-beta", "item-alpha"), model.state.value.items.map(ItineraryItemUi::id))
        assertEquals(listOf("item-beta", "item-alpha"), model.state.value.previewOrder)

        repository.moveFailures += IllegalStateException("旧排序失败")
        firstGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("item-beta", "item-alpha"), model.state.value.items.map(ItineraryItemUi::id))
        assertEquals(listOf("item-beta", "item-alpha"), model.state.value.previewOrder)
        assertNull(model.state.value.error)
    }

    @Test fun `new preview invalidates an older pending commit failure`() = runTest(dispatcher) {
        val repository = Itineraries()
        val gate = CompletableDeferred<Unit>()
        repository.moveGates += gate
        val model = model(repository)
        advanceUntilIdle()

        model.previewMove("item-beta", 0)
        model.commitMove("item-beta", 0)
        dispatcher.scheduler.runCurrent()
        model.previewMove("item-beta", 0)
        repository.moveFailures += IllegalStateException("旧排序失败")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("item-beta", "item-alpha"), model.state.value.previewOrder)
        assertNull(model.state.value.error)
    }

    @Test fun `switching away and back invalidates pending same day move`() = runTest(dispatcher) {
        val repository = Itineraries()
        val gate = CompletableDeferred<Unit>()
        repository.moveGates += gate
        val model = model(repository)
        advanceUntilIdle()

        model.previewMove("item-beta", 0)
        model.commitMove("item-beta", 0)
        dispatcher.scheduler.runCurrent()
        model.selectDay("day-2")
        advanceUntilIdle()
        model.selectDay("day-1")
        advanceUntilIdle()
        repository.moveFailures += IllegalStateException("过期排序失败")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("item-alpha", "item-beta"), model.state.value.items.map(ItineraryItemUi::id))
        assertEquals(listOf("item-alpha", "item-beta"), model.state.value.previewOrder)
        assertNull(model.state.value.error)
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

    @Test fun `route editor accepts ready leg and rejects every visible non ready leg`() = runTest(dispatcher) {
        val model = model(
            Itineraries(),
            legs = Legs(
                listOf(
                    legEntity("pending", RouteStatus.PENDING),
                    legEntity("calculating", RouteStatus.CALCULATING),
                    legEntity("waiting", RouteStatus.WAITING_NETWORK),
                    legEntity("failed", RouteStatus.FAILED),
                    legEntity("ready", RouteStatus.SUCCESS),
                ),
            ),
        )
        advanceUntilIdle()

        listOf("pending", "calculating", "waiting", "failed").forEach { id ->
            assertFalse(model.requestMode(id))
            assertNull(model.state.value.modeEditor)
        }
        assertTrue(model.requestMode("ready"))
        assertEquals("ready", model.state.value.modeEditor?.legId)
        assertFalse(model.requestMode("missing"))
        assertEquals("ready", model.state.value.modeEditor?.legId)
    }

    @Test fun `route editor closes when its ready leg becomes non ready`() = runTest(dispatcher) {
        listOf(
            RouteStatus.PENDING,
            RouteStatus.CALCULATING,
            RouteStatus.WAITING_NETWORK,
            RouteStatus.FAILED,
        ).forEach { status ->
            val legs = Legs(listOf(legEntity("route", RouteStatus.SUCCESS)))
            val model = model(Itineraries(), legs = legs)
            advanceUntilIdle()
            assertTrue(model.requestMode("route"))

            legs.emit(listOf(legEntity("route", status)))
            advanceUntilIdle()

            assertNull(model.state.value.modeEditor)
        }
    }

    @Test fun `saving after a route loses ready eligibility clears editor without repository call`() = runTest(dispatcher) {
        val coordinator = Coordinator()
        val legs = Legs(listOf(legEntity("route", RouteStatus.SUCCESS)))
        val model = model(Itineraries(), coordinator, legs)
        advanceUntilIdle()
        assertTrue(model.requestMode("route"))

        legs.emit(listOf(legEntity("route", RouteStatus.WAITING_NETWORK)))
        advanceUntilIdle()
        model.saveRouteEditor()
        advanceUntilIdle()

        assertNull(model.state.value.modeEditor)
        assertTrue(coordinator.details.isEmpty())
        assertTrue(legs.details.isEmpty())
    }

    @Test fun `preview move immediately clears editor for its old adjacent pair and rejects that leg`() = runTest(dispatcher) {
        val model = model(
            Itineraries(),
            legs = Legs(listOf(legEntity("alpha-beta", RouteStatus.SUCCESS))),
        )
        advanceUntilIdle()
        assertTrue(model.requestMode("alpha-beta"))

        model.previewMove("item-beta", 0)

        assertNull(model.state.value.modeEditor)
        assertFalse(model.requestMode("alpha-beta"))
        assertEquals(listOf("item-beta", "item-alpha"), model.state.value.previewOrder)
    }

    @Test fun `preview order exposes an existing matching leg as editable while retaining raw legs`() = runTest(dispatcher) {
        val model = model(
            Itineraries(),
            legs = Legs(
                listOf(
                    legEntity("alpha-beta", RouteStatus.PENDING),
                    legEntity("beta-alpha", RouteStatus.SUCCESS).copy(fromItemId = "item-beta", toItemId = "item-alpha"),
                ),
            ),
        )
        advanceUntilIdle()

        model.previewMove("item-beta", 0)

        assertEquals(listOf("alpha-beta", "beta-alpha"), model.state.value.legs.map(RouteLegUi::id))
        assertTrue(model.requestMode("beta-alpha"))
    }

    @Test fun `route editor rejects non-adjacent leg and clears it when a visible row becomes stale`() = runTest(dispatcher) {
        val legs = Legs(
            listOf(
                legEntity("adjacent", RouteStatus.SUCCESS),
                legEntity("stale", RouteStatus.FAILED).copy(fromItemId = "item-beta", toItemId = "item-alpha"),
            ),
        )
        val model = model(Itineraries(), legs = legs)
        advanceUntilIdle()

        assertTrue(model.requestMode("adjacent"))
        assertFalse(model.requestMode("stale"))
        assertEquals("adjacent", model.state.value.modeEditor?.legId)

        legs.emit(listOf(legEntity("adjacent", RouteStatus.SUCCESS).copy(fromItemId = "item-beta", toItemId = "item-alpha")))
        advanceUntilIdle()

        assertNull(model.state.value.modeEditor)
    }

    @Test fun `route editor preserves a non-minute override for note-only save then applies user duration changes`() = runTest(dispatcher) {
        val coordinator = Coordinator()
        val model = model(
            Itineraries(),
            coordinator,
            Legs(listOf(legEntity("leg-alpha", RouteStatus.SUCCESS).copy(durationOverrideSeconds = 75))),
        )
        advanceUntilIdle()

        assertTrue(model.requestMode("leg-alpha"))
        model.updateRouteNote("说明")
        model.saveRouteEditor()
        advanceUntilIdle()
        assertEquals(RouteDetails("leg-alpha", null, 75, "说明"), coordinator.details.last())

        assertTrue(model.requestMode("leg-alpha"))
        model.updateRouteDurationMinutes("2")
        model.saveRouteEditor()
        advanceUntilIdle()
        assertEquals(RouteDetails("leg-alpha", null, 120, null), coordinator.details.last())

        assertTrue(model.requestMode("leg-alpha"))
        model.updateRouteDurationMinutes("")
        model.saveRouteEditor()
        advanceUntilIdle()
        assertEquals(RouteDetails("leg-alpha", null, null, null), coordinator.details.last())
    }

    @Test fun `route editor validates duration and preserves latest input after save failure`() = runTest(dispatcher) {
        val coordinator = Coordinator().apply { detailsFailure = IllegalStateException("保存失败") }
        val model = model(
            Itineraries(),
            coordinator,
            Legs(listOf(legEntity("leg-alpha", RouteStatus.SUCCESS, duration = 1_800).copy(note = "原说明"))),
        )
        advanceUntilIdle()
        assertTrue(model.requestMode("leg-alpha"))
        assertEquals("", model.state.value.modeEditor?.durationMinutesText)
        assertEquals("原说明", model.state.value.modeEditor?.noteText)

        model.updateRouteDurationMinutes("0")
        model.saveRouteEditor()
        advanceUntilIdle()
        assertTrue(coordinator.details.isEmpty())

        model.updateRouteDurationMinutes("45")
        model.updateRouteNote("新说明")
        model.saveRouteEditor()
        advanceUntilIdle()

        assertEquals("leg-alpha", model.state.value.modeEditor?.legId)
        assertEquals("45", model.state.value.modeEditor?.durationMinutesText)
        assertEquals("新说明", model.state.value.modeEditor?.noteText)
        assertEquals("保存失败", model.state.value.modeEditor?.saveError)
        assertEquals(listOf(RouteDetails("leg-alpha", null, 2_700, "新说明")), coordinator.details)
    }

    @Test fun `metadata only route edit saves without a route coordinator`() = runTest(dispatcher) {
        val legs = Legs(listOf(legEntity("leg-alpha", RouteStatus.SUCCESS).copy(durationOverrideSeconds = 75)))
        val model = model(Itineraries(), coordinator = null, legs = legs)
        advanceUntilIdle()

        assertTrue(model.requestMode("leg-alpha"))
        model.updateRouteNote("无需地图的说明")
        model.saveRouteEditor()
        advanceUntilIdle()

        assertEquals(
            listOf(RouteDetails("leg-alpha", null, 75, "无需地图的说明")),
            legs.details,
        )
        assertNull(model.state.value.modeEditor)
    }

    @Test fun `mode override remains blocked without a route coordinator`() = runTest(dispatcher) {
        val legs = Legs(listOf(legEntity("leg-alpha", RouteStatus.SUCCESS)))
        val model = model(Itineraries(), coordinator = null, legs = legs)
        advanceUntilIdle()

        assertTrue(model.requestMode("leg-alpha"))
        model.selectMode(TransportMode.DRIVE)
        model.saveRouteEditor()
        advanceUntilIdle()

        assertTrue(legs.details.isEmpty())
        assertEquals("联网并同意高德隐私政策后才能保存路段编辑", model.state.value.modeEditor?.saveError)
    }

    @Test fun `persisted zero duration override is normalized as no override in editor`() = runTest(dispatcher) {
        val legs = Legs(listOf(legEntity("leg-alpha", RouteStatus.SUCCESS).copy(durationOverrideSeconds = 0)))
        val model = model(Itineraries(), coordinator = null, legs = legs)
        advanceUntilIdle()

        assertTrue(model.requestMode("leg-alpha"))

        assertEquals("", model.state.value.modeEditor?.durationMinutesText)
        assertNull(model.state.value.modeEditor?.durationOverrideSeconds)
        assertTrue(model.state.value.modeEditor?.isValid == true)
    }

    @Test fun `route editor failure keeps selected mode and retry closes only after success`() = runTest(dispatcher) {
        val coordinator = Coordinator().apply { detailsResult = false }
        val model = model(
            Itineraries(),
            coordinator,
            Legs(listOf(legEntity("leg-alpha", RouteStatus.SUCCESS))),
        )
        advanceUntilIdle()
        model.requestMode("leg-alpha")
        model.selectMode(TransportMode.WALK)

        model.saveRouteEditor()
        advanceUntilIdle()

        assertEquals("leg-alpha", model.state.value.modeEditor?.legId)
        assertEquals(TransportMode.WALK, model.state.value.modeEditor?.selectedMode)
        assertEquals("联网并同意高德隐私政策后才能保存路段编辑", model.state.value.modeEditor?.saveError)
        coordinator.detailsResult = true
        model.saveRouteEditor()
        advanceUntilIdle()
        assertNull(model.state.value.modeEditor)
    }

    @Test fun `route editor pending blocks duplicate and stale completion cannot alter new leg`() = runTest(dispatcher) {
        val coordinator = Coordinator().apply { detailsGate = CompletableDeferred() }
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
        model.saveRouteEditor()
        model.saveRouteEditor()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(RouteDetails("leg-alpha", TransportMode.WALK, null, null)), coordinator.details)
        assertTrue(model.state.value.modeEditor!!.isSaving)

        model.requestMode("leg-beta")
        model.selectMode(TransportMode.DRIVE)
        coordinator.detailsGate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals("leg-beta", model.state.value.modeEditor?.legId)
        assertEquals(TransportMode.DRIVE, model.state.value.modeEditor?.selectedMode)
        assertFalse(model.state.value.modeEditor!!.isSaving)
    }

    @Test fun `opening item edit prepopulates note and preserves multiline input`() = runTest(dispatcher) {
        val repository = Itineraries()
        val model = model(repository)
        advanceUntilIdle()

        assertTrue(model.requestTiming("item-alpha"))
        assertEquals("已有\n备注", model.state.value.editDraft?.noteText)
        model.updateNote("第一行\n第二行")

        assertEquals("第一行\n第二行", model.state.value.editDraft?.noteText)
    }

    @Test fun `save details sends time stay and normalized note in one call`() = runTest(dispatcher) {
        val repository = Itineraries()
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")
        model.updateArrivalTime("08:30")
        model.updateStayMinutes("45")
        model.updateNote("  第一行\n第二行  ")

        model.saveTiming()
        advanceUntilIdle()

        assertEquals(1, repository.detailCalls.size)
        assertEquals(Details("item-alpha", LocalTime.of(8, 30), 45, "第一行\n第二行"), repository.detailCalls.single())
    }

    @Test fun `observed details update refreshes row and next editor draft`() = runTest(dispatcher) {
        val repository = Itineraries()
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")
        model.updateNote("来自观察流")

        model.saveTiming()
        advanceUntilIdle()

        assertEquals("来自观察流", model.state.value.items.first { it.id == "item-alpha" }.note)
        assertTrue(model.requestTiming("item-alpha"))
        assertEquals("来自观察流", model.state.value.editDraft?.noteText)
    }

    @Test fun `external removal clears an open edit draft`() = runTest(dispatcher) {
        val repository = Itineraries()
        val model = model(repository)
        advanceUntilIdle()
        model.requestTiming("item-alpha")

        repository.emitWithout("item-alpha")
        advanceUntilIdle()

        assertNull(model.state.value.editDraft)
    }

    private fun model(
        repository: Itineraries,
        coordinator: RouteRefreshCoordinator? = null,
        legs: RouteLegRepository = Legs(),
        trips: Trips = Trips(),
    ): DayItineraryViewModel {
        val baselineTrips = object : TripRepository by trips {
            override fun observeTrip(tripId: String): Flow<TripWithDays?> = flowOf(trips.currentTrip())
        }
        return DayItineraryViewModel(
            "trip",
            trips,
            repository,
            legs,
            coordinator,
            selectedDays = flowOf("day-1"),
            tripService = TripService(baselineTrips),
        )
    }

    private class Itineraries : ItineraryRepository {
        var timingGate: CompletableDeferred<Unit>? = null
        var timingFailure: Throwable? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        var deleteFailure: Throwable? = null
        var moveFailure: Throwable? = null
        val moveGates = ArrayDeque<CompletableDeferred<Unit>>()
        val moveFailures = ArrayDeque<Throwable>()
        val timingCalls = mutableListOf<Timing>()
        val detailCalls = mutableListOf<Details>()
        val deleteCalls = mutableListOf<String>()
        private val days = mutableMapOf(
            "day-1" to kotlinx.coroutines.flow.MutableStateFlow(day("day-1")),
            "day-2" to kotlinx.coroutines.flow.MutableStateFlow(DayItinerary("day-2", "trip", emptyList())),
        )
        override fun observeDay(dayId: String) = days.getValue(dayId)
        fun emitWithout(itemId: String) {
            val current = days.getValue("day-1")
            current.value = current.value.copy(items = current.value.items.filterNot { it.id == itemId })
        }
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "new"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) {
            moveGates.removeFirstOrNull()?.await()
            moveFailures.removeFirstOrNull()?.let { throw it }
            moveFailure?.let { throw it }
            val source = days.values.firstOrNull { flow -> flow.value.items.any { it.id == itemId } }
            val current = days.getValue(targetDayId)
            val moved = source?.value?.items?.firstOrNull { it.id == itemId } ?: return
            source.value = source.value.copy(items = source.value.items.filterNot { it.id == itemId })
            val reordered = current.value.items.toMutableList().apply { add(targetIndex, moved) }
            current.value = current.value.copy(items = reordered)
        }

        private fun day(dayId: String) = DayItinerary(
            dayId,
            "trip",
            listOf(
                ItineraryItem("item-alpha", ItineraryPlace("place-alpha", "酒店", "地址 A", GeoPoint(1.0, 2.0)), LocalTime.of(8, 0), 60, "已有\n备注"),
                ItineraryItem("item-beta", ItineraryPlace("place-beta", "博物馆", "地址 B", GeoPoint(3.0, 4.0)), null, null),
            ),
        )
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
        override suspend fun updateDetails(itemId: String, arrivalTime: java.time.LocalTime?, stayMinutes: Int?, note: String?) {
            detailCalls += Details(itemId, arrivalTime, stayMinutes, note)
            timingGate?.await()
            timingFailure?.let { throw it }
            val current = days.getValue("day-1")
            current.value = current.value.copy(items = current.value.items.map { item ->
                if (item.id == itemId) item.copy(arrivalTime = arrivalTime, stayMinutes = stayMinutes, note = note) else item
            })
        }
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Trips : TripRepository {
        private val trip = kotlinx.coroutines.flow.MutableStateFlow(
            TripWithDays("trip", "Trip", LocalDate.of(2026, 8, 25), TravelMode.FLEXIBLE, listOf(TripDay("day-1", 0))),
        )
        var insertCalls = 0
        var observeCalls = 0
        var activeObservationCount = 0
        var maxActiveObservationCount = 0
        var insertFailure: Throwable? = null
        var insertGate: CompletableDeferred<Unit>? = null
        var stopObservationAfterInsert = false
        var completeObservationAfterInsert = false
        var completeObservationOnDemand = false
        val resubscriptionTerminations = ArrayDeque<ObservationTermination>()
        var coordinatedTermination = false
        private var coordinatedTerminationIsError = false
        private var emitCurrentBeforeTerminationHandler = false
        var terminationEntered = CompletableDeferred<Unit>()
        var terminationRelease: CompletableDeferred<Unit>? = null
        private val observationFailures = MutableSharedFlow<Throwable>()
        private val observationCompletion = MutableSharedFlow<Unit>()
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = flow {
            val observationId = ++observeCalls
            activeObservationCount++
            maxActiveObservationCount = maxOf(maxActiveObservationCount, activeObservationCount)
            try {
                val immediateTermination = if (observationId > 1) resubscriptionTerminations.removeFirstOrNull() else null
                if (immediateTermination != null) {
                    emit(trip.value)
                    if (immediateTermination == ObservationTermination.ERROR) {
                        throw IllegalStateException("db unavailable")
                    }
                    return@flow
                }
                if (completeObservationAfterInsert && insertCalls > 0) {
                    emit(trip.first())
                    return@flow
                }
                if (coordinatedTermination) {
                    emit(trip.value)
                    observationCompletion.first()
                    if (emitCurrentBeforeTerminationHandler) emit(trip.value)
                    terminationEntered.complete(Unit)
                    terminationRelease!!.await()
                    if (coordinatedTerminationIsError) throw IllegalStateException("db unavailable")
                    return@flow
                }
                if (completeObservationOnDemand) {
                    emit(trip.value)
                    observationCompletion.first()
                    return@flow
                }
                merge(trip, observationFailures.map { throw it }).collect { emit(it) }
            } finally {
                activeObservationCount--
            }
        }
        fun prepareCoordinatedTermination(error: Boolean, emitCurrentBeforeHandler: Boolean = false) {
            coordinatedTermination = true
            coordinatedTerminationIsError = error
            emitCurrentBeforeTerminationHandler = emitCurrentBeforeHandler
            terminationEntered = CompletableDeferred()
            terminationRelease = CompletableDeferred()
        }
        suspend fun terminateObservation() { observationCompletion.emit(Unit) }
        suspend fun failObservation(failure: Throwable) { observationFailures.emit(failure) }
        suspend fun completeObservation() { observationCompletion.emit(Unit) }
        fun stopObservationFailures() { stopObservationAfterInsert = false }
        fun currentTrip(): TripWithDays = trip.value
        fun emit(value: TripWithDays) { trip.value = value }
        fun setAppendedDay() {
            val current = trip.value
            trip.value = current.copy(days = current.days + TripDay("day-new", current.days.size))
        }
        fun emitAppendedDay() = setAppendedDay()
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String {
            insertCalls++
            insertGate?.await()
            insertFailure?.let { throw it }
            if (stopObservationAfterInsert) observationFailures.emit(IllegalStateException("db unavailable"))
            return "day-new"
        }
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class Coordinator : RouteRefreshCoordinator {
        var overrideResult = true
        var overrideGate: CompletableDeferred<Unit>? = null
        var detailsFailure: Throwable? = null
        var detailsResult = true
        var detailsGate: CompletableDeferred<Unit>? = null
        val overrides = mutableListOf<Pair<String, TransportMode>>()
        val details = mutableListOf<RouteDetails>()
        val legacyRetries = mutableListOf<String>()
        val retries = mutableListOf<Pair<String, Long>>()
        override fun start(scope: kotlinx.coroutines.CoroutineScope) = Unit
        override suspend fun retry(legId: String): Boolean {
            legacyRetries += legId
            return true
        }
        override suspend fun retry(legId: String, expectedVersion: Long): Boolean {
            retries += legId to expectedVersion
            overrideGate?.await()
            return overrideResult
        }
        override suspend fun updateDetails(legId: String, selectedModeOverride: TransportMode?, durationOverrideSeconds: Int?, note: String?): Boolean {
            details += RouteDetails(legId, selectedModeOverride, durationOverrideSeconds, note)
            detailsGate?.await()
            detailsFailure?.let { throw it }
            return detailsResult
        }
    }

    private class Legs(values: List<RouteLegEntity> = emptyList()) : RouteLegRepository {
        private val rows = kotlinx.coroutines.flow.MutableStateFlow(values)
        val details = mutableListOf<RouteDetails>()
        override fun observeDay(dayId: String) = rows
        fun emit(values: List<RouteLegEntity>) { rows.value = values }
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
        override suspend fun updateDetails(legId: String, selectedModeOverride: TransportMode?, durationOverrideSeconds: Int?, note: String?, online: Boolean): Boolean {
            details += RouteDetails(legId, selectedModeOverride, durationOverrideSeconds, note)
            return true
        }
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

    private enum class ObservationTermination { ERROR, COMPLETE }

    private data class Timing(val itemId: String, val time: LocalTime?, val minutes: Int?)
    private data class Details(val itemId: String, val time: LocalTime?, val minutes: Int?, val note: String?)
    private data class RouteDetails(val legId: String, val selectedModeOverride: TransportMode?, val durationOverrideSeconds: Int?, val note: String?)
}
