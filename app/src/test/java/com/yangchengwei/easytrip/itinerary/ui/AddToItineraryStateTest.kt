package com.yangchengwei.easytrip.itinerary.ui

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesRequest
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCase
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCase
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddToItineraryStateTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `selection list is ordered truth and set is derived`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel)
        viewModel.togglePlace("hotel")
        viewModel.togglePlace("museum")
        viewModel.togglePlace("hotel")
        viewModel.togglePlace("hotel")

        assertEquals(listOf("museum", "hotel"), viewModel.state.value.selectedPlaceIds)
        assertEquals(setOf("museum", "hotel"), viewModel.state.value.selectedPlaceIdSet)
    }

    @Test fun `saved draft restores order target and editing target but not transient state`() = runTest(dispatcher) {
        val saved = SavedStateHandle(
            mapOf(
                "workspace.addToItinerary.selectedPlaceIds" to arrayListOf("museum", "hotel"),
                "workspace.addToItinerary.targetDayId" to "day-2",
                "workspace.addToItinerary.editingTarget" to "for-day:day-1",
                "workspace.addToItinerary.isSubmitting" to true,
                "workspace.addToItinerary.undoCreatedItemIds" to arrayListOf("old-item"),
            ),
        )

        val viewModel = model(FakeItineraries(), saved)

        assertEquals(listOf("museum", "hotel"), viewModel.state.value.selectedPlaceIds)
        assertEquals("day-2", viewModel.state.value.targetDayId)
        assertEquals(AddToItineraryEditingTarget.ForDay("day-1"), viewModel.state.value.editingTarget)
        assertFalse(viewModel.state.value.isSubmitting)
        assertNull(viewModel.state.value.result)
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())
    }

    @Test fun `restored draft cannot submit before first validity snapshot`() = runTest(dispatcher) {
        val saved = SavedStateHandle(
            mapOf(
                "workspace.addToItinerary.selectedPlaceIds" to arrayListOf("hotel"),
                "workspace.addToItinerary.targetDayId" to "day-1",
                "workspace.addToItinerary.editingTarget" to "from-place-pool",
            ),
        )
        val repository = FakeItineraries()
        val viewModel = model(repository, saved)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(repository.addCalls.isEmpty())
        assertFalse(viewModel.state.value.validityInitialized)
    }

    @Test fun `invalid target day clears target and keeps valid selections`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel)
        viewModel.startFromPool()
        viewModel.togglePlace("hotel")
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-1")

        viewModel.reconcile(listOf("day-2"), setOf("hotel"))

        assertNull(viewModel.state.value.targetDayId)
        assertEquals(listOf("hotel"), viewModel.state.value.selectedPlaceIds)
        assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, viewModel.state.value.step)
    }

    @Test fun `duplicate submit invokes add once`() = runTest(dispatcher) {
        val repository = FakeItineraries(suspendAdd = true)
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)

        viewModel.submit()
        dispatcher.scheduler.runCurrent()
        viewModel.submit()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.addCalls.size)
        assertTrue(viewModel.state.value.isSubmitting)
    }

    @Test fun `partial success keeps only failed places and exact created ids for undo`() = runTest(dispatcher) {
        val repository = FakeItineraries(failedPlaceIds = setOf("museum"))
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.togglePlace("museum")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("museum"), viewModel.state.value.selectedPlaceIds)
        assertEquals(listOf("created-hotel"), viewModel.state.value.undoCreatedItemIds)
        assertEquals(AddPlacesOutcome.PartialSuccess("day-1", listOf("created-hotel"), listOf("museum")), viewModel.state.value.result)
    }

    @Test fun `target missing keeps selection clears target and does not expose stale undo ids`() = runTest(dispatcher) {
        val repository = FakeItineraries(outcome = AddPlacesOutcome.TargetDayMissing(listOf("hotel", "museum"), listOf("created-before-delete")))
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.togglePlace("museum")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("hotel", "museum"), viewModel.state.value.selectedPlaceIds)
        assertNull(viewModel.state.value.targetDayId)
        assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, viewModel.state.value.step)
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())
    }

    @Test fun `undo deletes only created item ids and cannot be submitted twice`() = runTest(dispatcher) {
        val repository = FakeItineraries()
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()

        viewModel.undo()
        viewModel.undo()
        advanceUntilIdle()

        assertEquals(listOf("created-hotel"), repository.deletedItemIds)
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())
    }

    @Test fun `draft actions and reconcile are frozen while submitting`() = runTest(dispatcher) {
        val repository = FakeItineraries(suspendAdd = true)
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        dispatcher.scheduler.runCurrent()
        val frozen = viewModel.state.value

        viewModel.startFromPool()
        viewModel.startForDay("day-2")
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-2")
        viewModel.reconcile(listOf("day-2"), setOf("museum"))

        assertEquals(frozen, viewModel.state.value)
    }

    @Test fun `in flight validity update applies after completion and blocks invalid retry`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeItineraries(addGate = gate, failedPlaceIds = setOf("museum"))
        val viewModel = model(repository)
        ready(viewModel)
        viewModel.startFromPool()
        viewModel.togglePlace("hotel")
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-1")
        viewModel.submit()
        dispatcher.scheduler.runCurrent()

        viewModel.reconcile(listOf("day-2"), setOf("hotel"))
        assertEquals(listOf("hotel", "museum"), viewModel.state.value.selectedPlaceIds)
        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.selectedPlaceIds.isEmpty())
        assertNull(viewModel.state.value.targetDayId)
        val calls = repository.addCalls.size
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(calls, repository.addCalls.size)
    }

    @Test fun `cancel during submit prevents old completion from overwriting a new flow`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeItineraries(addGate = gate)
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        dispatcher.scheduler.runCurrent()

        viewModel.cancel()
        viewModel.startFromPool()
        viewModel.togglePlace("museum")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("museum"), viewModel.state.value.selectedPlaceIds)
        assertEquals(AddToItineraryStep.SELECT_PLACES, viewModel.state.value.step)
        assertNull(viewModel.state.value.result)
    }

    @Test fun `undo failure retains only undeleted ids and retry resumes from them`() = runTest(dispatcher) {
        val repository = FakeItineraries(deleteFailures = mutableMapOf("created-museum" to 1))
        val viewModel = model(repository)
        ready(viewModel)
        viewModel.startFromPool()
        viewModel.togglePlace("hotel")
        viewModel.togglePlace("museum")
        viewModel.togglePlace("park")
        viewModel.selectTargetDay("day-1")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()
        assertEquals(listOf("created-museum", "created-park"), viewModel.state.value.undoCreatedItemIds)

        viewModel.undo()
        advanceUntilIdle()
        assertEquals(
            listOf("created-hotel", "created-museum", "created-museum", "created-park"),
            repository.deleteCalls,
        )
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())
    }

    @Test fun `submit and undo are mutually exclusive and success replaces old token`() = runTest(dispatcher) {
        val deleteGate = CompletableDeferred<Unit>()
        val repository = FakeItineraries(deleteGate = deleteGate)
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(listOf("created-hotel"), viewModel.state.value.undoCreatedItemIds)

        viewModel.undo()
        dispatcher.scheduler.runCurrent()
        viewModel.submit()
        assertEquals(1, repository.addCalls.size)
        deleteGate.complete(Unit)
        advanceUntilIdle()

        ready(viewModel)
        viewModel.startFromPool()
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-1")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("created-museum"), viewModel.state.value.undoCreatedItemIds)
    }

    @Test fun `invalid ids are rejected and all failures produce no undo token`() = runTest(dispatcher) {
        val repository = FakeItineraries(failedPlaceIds = setOf("museum"))
        val viewModel = model(repository)
        ready(viewModel, places = setOf("hotel", "museum"))

        viewModel.togglePlace("invalid")
        viewModel.selectTargetDay("invalid-day")
        viewModel.startForDay("invalid-day")
        assertTrue(viewModel.state.value.selectedPlaceIds.isEmpty())
        assertNull(viewModel.state.value.targetDayId)

        viewModel.startFromPool()
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-1")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(listOf("museum"), viewModel.state.value.selectedPlaceIds)
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())
    }

    private fun ready(
        viewModel: AddToItineraryViewModel,
        days: List<String> = listOf("day-1", "day-2"),
        places: Set<String> = setOf("hotel", "museum", "park"),
    ) = viewModel.reconcile(days, places)

    private fun selectForSubmit(viewModel: AddToItineraryViewModel) {
        viewModel.startFromPool()
        viewModel.togglePlace("hotel")
        viewModel.selectTargetDay("day-1")
    }

    private fun model(repository: FakeItineraries, savedState: SavedStateHandle = SavedStateHandle()) =
        AddToItineraryViewModel(
            tripId = "trip",
            addPlaces = AddPlacesToDayUseCase(repository),
            undoAddedItems = UndoAddedItemsUseCase(repository),
            savedState = savedState,
        )

    private class FakeItineraries(
        private val failedPlaceIds: Set<String> = emptySet(),
        private val suspendAdd: Boolean = false,
        private val outcome: AddPlacesOutcome? = null,
        private val addGate: CompletableDeferred<Unit>? = null,
        private val deleteGate: CompletableDeferred<Unit>? = null,
        private val deleteFailures: MutableMap<String, Int> = mutableMapOf(),
    ) : ItineraryRepository {
        val addCalls = mutableListOf<String>()
        val deletedItemIds = mutableListOf<String>()
        val deleteCalls = mutableListOf<String>()

        override fun observeDay(dayId: String): Flow<DayItinerary> = flowOf(DayItinerary(dayId, "trip", emptyList()))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String {
            addCalls += savedPlaceId
            if (suspendAdd) awaitCancellation()
            addGate?.await()
            if (outcome is AddPlacesOutcome.TargetDayMissing) {
                if (addCalls.size > outcome.createdItemIds.size) {
                    throw com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException(dayId)
                }
                return outcome.createdItemIds[addCalls.lastIndex]
            }
            if (savedPlaceId in failedPlaceIds) throw com.yangchengwei.easytrip.itinerary.domain.RecoverablePlaceAddException(savedPlaceId)
            return "created-$savedPlaceId"
        }
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) {
            deleteCalls += itemId
            deleteGate?.await()
            val failures = deleteFailures[itemId] ?: 0
            if (failures > 0) {
                deleteFailures[itemId] = failures - 1
                throw IllegalStateException("delete failed")
            }
            deletedItemIds += itemId
        }
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun removePlaceOccurrences(placeId: String) = error("undo must not remove occurrences by place")
    }
}
