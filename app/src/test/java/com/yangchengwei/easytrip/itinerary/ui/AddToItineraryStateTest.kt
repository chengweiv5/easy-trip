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

    @Test fun `single place start reports only a fresh valid unlocked launch`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel)

        assertTrue(viewModel.startForPlace("hotel"))
        assertEquals(listOf("hotel"), viewModel.state.value.selectedPlaceIds)
        assertEquals(AddToItineraryEditingTarget.FromPlacePool, viewModel.state.value.editingTarget)
        assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, viewModel.state.value.step)

        assertFalse(viewModel.startForPlace("missing"))
        assertFalse(viewModel.startForPlace("museum"))
        assertEquals(listOf("hotel"), viewModel.state.value.selectedPlaceIds)
    }

    @Test fun `failed single place start keeps old bulk draft and locked draft unchanged`() = runTest(dispatcher) {
        val bulk = model(FakeItineraries())
        ready(bulk)
        bulk.startFromPool()
        bulk.togglePlace("museum")
        bulk.continueToTargetDay()

        assertFalse(bulk.startForPlace("missing"))
        assertEquals(listOf("museum"), bulk.state.value.selectedPlaceIds)

        val locked = model(FakeItineraries(suspendAdd = true))
        ready(locked)
        selectForSubmit(locked)
        locked.submit()
        assertTrue(locked.state.value.isSubmitting)
        assertFalse(locked.startForPlace("museum"))
        assertEquals(listOf("hotel"), locked.state.value.selectedPlaceIds)
    }

    @Test fun `pool and day starts report whether the requested flow launched`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel)

        assertTrue(viewModel.startFromPool())
        assertTrue(viewModel.startForDay("day-2"))
        assertEquals(AddToItineraryEditingTarget.ForDay("day-2"), viewModel.state.value.editingTarget)

        assertFalse(viewModel.startForDay("missing"))
        assertEquals(AddToItineraryEditingTarget.ForDay("day-2"), viewModel.state.value.editingTarget)
    }

    @Test fun `selection list is ordered truth and continue advances real view model`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel)
        viewModel.startFromPool()
        assertFalse(viewModel.state.value.canContinue)

        viewModel.togglePlace("hotel")
        viewModel.togglePlace("museum")
        viewModel.togglePlace("hotel")
        viewModel.togglePlace("hotel")

        assertEquals(listOf("museum", "hotel"), viewModel.state.value.selectedPlaceIds)
        assertEquals(setOf("museum", "hotel"), viewModel.state.value.selectedPlaceIdSet)
        assertTrue(viewModel.state.value.canContinue)

        viewModel.continueToTargetDay()
        assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, viewModel.state.value.step)
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

    @Test fun `from pool draft restores target selection step before target is chosen`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val original = model(FakeItineraries(), saved)
        ready(original)
        original.startFromPool()
        original.togglePlace("hotel")
        original.continueToTargetDay()

        val restored = model(FakeItineraries(), saved)

        assertEquals(AddToItineraryEditingTarget.FromPlacePool, restored.state.value.editingTarget)
        assertEquals(listOf("hotel"), restored.state.value.selectedPlaceIds)
        assertNull(restored.state.value.targetDayId)
        assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, restored.state.value.step)
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

    @Test fun `reconcile drops undo batches for days that no longer exist`() = runTest(dispatcher) {
        val repository = FakeItineraries(
            addResults = ArrayDeque(listOf(Result.success("day-1-hotel"), Result.success("day-2-museum"))),
        )
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()
        viewModel.startFromPool()
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-2")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.reconcile(listOf("day-2"), setOf("hotel", "museum", "park"))

        assertEquals(
            listOf(UndoCreatedItemsBatch("day-2", listOf("day-2-museum"))),
            viewModel.state.value.undoBatches,
        )
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

    @Test fun `closing partial result clears failed draft result and undo token`() = runTest(dispatcher) {
        val repository = FakeItineraries(failedPlaceIds = setOf("museum"))
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.togglePlace("museum")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.cancel()

        assertEquals(AddToItineraryStep.IDLE, viewModel.state.value.step)
        assertTrue(viewModel.state.value.selectedPlaceIds.isEmpty())
        assertNull(viewModel.state.value.result)
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())
    }

    @Test fun `retry after partial success retains earlier created ids for undo`() = runTest(dispatcher) {
        val repository = FakeItineraries(failedPlaceIds = setOf("museum"))
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.togglePlace("museum")
        viewModel.submit()
        advanceUntilIdle()

        repository.failedPlaceIds = emptySet()
        viewModel.retryPartial()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("created-hotel", "created-museum"), viewModel.state.value.undoCreatedItemIds)
    }

    @Test fun `success and partial outcomes merge undo ids without duplicates`() = runTest(dispatcher) {
        val repository = FakeItineraries(createdItemId = "same-item")
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("same-item"), viewModel.state.value.undoCreatedItemIds)
        assertEquals(listOf("same-item"), mergeUndoIds(listOf("same-item"), listOf("same-item")))
    }

    @Test fun `target missing removes only its day batch and preserves another day undo`() = runTest(dispatcher) {
        val repository = FakeItineraries(
            addResults = ArrayDeque(
                listOf(
                    Result.success("day-1-hotel"),
                    Result.failure(com.yangchengwei.easytrip.itinerary.domain.RecoverablePlaceAddException("museum")),
                    Result.success("stale-day-2-museum"),
                    Result.failure(com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException("day-2")),
                ),
            ),
        )
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.togglePlace("museum")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(
            listOf(UndoCreatedItemsBatch("day-1", listOf("day-1-hotel"))),
            viewModel.state.value.undoBatches,
        )

        viewModel.togglePlace("park")
        viewModel.selectTargetDay("day-2")
        viewModel.retryPartial()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            listOf(UndoCreatedItemsBatch("day-1", listOf("day-1-hotel"))),
            viewModel.state.value.undoBatches,
        )
        assertEquals(listOf("museum", "park"), viewModel.state.value.selectedPlaceIds)
        assertNull(viewModel.state.value.targetDayId)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(listOf("day-1-hotel"), repository.deletedItemIds)
    }

    @Test fun `undo deletes batches from separate successful days`() = runTest(dispatcher) {
        val repository = FakeItineraries(
            addResults = ArrayDeque(listOf(Result.success("day-1-hotel"), Result.success("day-2-museum"))),
        )
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()

        viewModel.startFromPool()
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-2")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(
            listOf(
                UndoCreatedItemsBatch("day-1", listOf("day-1-hotel")),
                UndoCreatedItemsBatch("day-2", listOf("day-2-museum")),
            ),
            viewModel.state.value.undoBatches,
        )
        viewModel.undo()
        advanceUntilIdle()

        assertEquals(listOf("day-1-hotel", "day-2-museum"), repository.deletedItemIds)
    }

    @Test fun `same day target missing removes that day batch before reselection`() = runTest(dispatcher) {
        val repository = FakeItineraries(
            addResults = ArrayDeque(
                listOf(
                    Result.success("old-hotel"),
                    Result.failure(com.yangchengwei.easytrip.itinerary.domain.RecoverablePlaceAddException("museum")),
                    Result.success("stale-museum"),
                    Result.failure(com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException("day-1")),
                    Result.success("new-museum"),
                    Result.success("new-park"),
                ),
            ),
        )
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.togglePlace("museum")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.togglePlace("park")
        viewModel.retryPartial()
        viewModel.submit()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())

        viewModel.selectTargetDay("day-2")
        viewModel.submit()
        advanceUntilIdle()
        viewModel.undo()
        advanceUntilIdle()

        assertEquals(listOf("new-museum", "new-park"), repository.deletedItemIds)
    }

    @Test fun `target missing keeps selection clears target and excludes stale created ids from undo`() = runTest(dispatcher) {
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
        viewModel.startForPlace("museum")
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

    @Test fun `undo failure across day batches does not replay deleted ids`() = runTest(dispatcher) {
        val repository = FakeItineraries(
            addResults = ArrayDeque(listOf(Result.success("day-1-hotel"), Result.success("day-2-museum"))),
            deleteFailures = mutableMapOf("day-2-museum" to 1),
        )
        val viewModel = model(repository)
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()
        viewModel.startFromPool()
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-2")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()
        assertEquals(
            listOf(UndoCreatedItemsBatch("day-2", listOf("day-2-museum"))),
            viewModel.state.value.undoBatches,
        )

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(listOf("day-1-hotel", "day-2-museum", "day-2-museum"), repository.deleteCalls)
        assertTrue(viewModel.state.value.undoCreatedItemIds.isEmpty())
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

    @Test fun `start for place rejects an active target-day draft`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel)
        viewModel.startFromPool()
        viewModel.togglePlace("museum")
        viewModel.selectTargetDay("day-1")

        assertFalse(viewModel.startForPlace("hotel"))

        assertEquals(listOf("museum"), viewModel.state.value.selectedPlaceIds)
        assertEquals("day-1", viewModel.state.value.targetDayId)
        assertEquals(AddToItineraryStep.SELECT_TARGET_DAY, viewModel.state.value.step)
    }

    @Test fun `start for place rejects completed result and preserves undo`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.startForPlace("museum"))

        assertTrue(viewModel.state.value.selectedPlaceIds.isEmpty())
        assertTrue(viewModel.state.value.result is AddPlacesOutcome.Success)
        assertEquals(listOf("created-hotel"), viewModel.state.value.undoCreatedItemIds)
    }

    @Test fun `start for place rejects failure draft and preserves its error`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries(throwOnAdd = true))
        ready(viewModel)
        selectForSubmit(viewModel)
        viewModel.submit()
        advanceUntilIdle()
        assertEquals("加入行程失败，请重试", viewModel.state.value.errorMessage)

        assertFalse(viewModel.startForPlace("museum"))

        assertEquals("加入行程失败，请重试", viewModel.state.value.errorMessage)
    }

    @Test fun `start for place rejects ids absent from latest validity`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel, places = setOf("hotel"))

        viewModel.startForPlace("museum")

        assertTrue(viewModel.state.value.selectedPlaceIds.isEmpty())
        assertNull(viewModel.state.value.editingTarget)
        assertEquals(AddToItineraryStep.IDLE, viewModel.state.value.step)
    }

    @Test fun `start for place does not open flow without travel dates`() = runTest(dispatcher) {
        val viewModel = model(FakeItineraries())
        ready(viewModel, days = emptyList())

        viewModel.startForPlace("hotel")

        assertTrue(viewModel.state.value.selectedPlaceIds.isEmpty())
        assertNull(viewModel.state.value.editingTarget)
        assertEquals(AddToItineraryStep.IDLE, viewModel.state.value.step)
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
        var failedPlaceIds: Set<String> = emptySet(),
        private val suspendAdd: Boolean = false,
        var outcome: AddPlacesOutcome? = null,
        private val addGate: CompletableDeferred<Unit>? = null,
        private val deleteGate: CompletableDeferred<Unit>? = null,
        private val deleteFailures: MutableMap<String, Int> = mutableMapOf(),
        private val createdItemId: String? = null,
        private val addResults: ArrayDeque<Result<String>> = ArrayDeque(),
        private val throwOnAdd: Boolean = false,
    ) : ItineraryRepository {
        val addCalls = mutableListOf<String>()
        val deletedItemIds = mutableListOf<String>()
        val deleteCalls = mutableListOf<String>()

        override fun observeDay(dayId: String): Flow<DayItinerary> = flowOf(DayItinerary(dayId, "trip", emptyList()))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String {
            addCalls += savedPlaceId
            if (suspendAdd) awaitCancellation()
            addGate?.await()
            if (throwOnAdd) throw IllegalStateException("add failed")
            if (addResults.isNotEmpty()) return addResults.removeFirst().getOrThrow()
            val configuredOutcome = outcome
            if (configuredOutcome is AddPlacesOutcome.TargetDayMissing) {
                if (configuredOutcome.createdItemIds.isNotEmpty()) {
                    val scripted = configuredOutcome.createdItemIds.first()
                    outcome = configuredOutcome.copy(createdItemIds = configuredOutcome.createdItemIds.drop(1))
                    return scripted
                }
                throw com.yangchengwei.easytrip.itinerary.domain.TargetDayNotFoundException(dayId)
            }
            if (savedPlaceId in failedPlaceIds) throw com.yangchengwei.easytrip.itinerary.domain.RecoverablePlaceAddException(savedPlaceId)
            return createdItemId ?: "created-$savedPlaceId"
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
