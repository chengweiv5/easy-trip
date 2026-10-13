package com.yangchengwei.easytrip.assistant

import androidx.lifecycle.ViewModelStore
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TripDraftViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val stores = ViewModelStore()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() { stores.clear(); Dispatchers.resetMain() }
    private val item = ImportItem("A", 0, PlaceCandidate("p", "博物馆", "杭州",
        GeoPoint(30.2, 120.1), null, "杭州"), PlaceCategory.ATTRACTION)
    private class Journal : PendingTripCreation {
        var command: TripCreationCommand? = null
        var unreadable = false
        override fun read(): TripCreationCommand? { check(!unreadable); return command }
        override fun write(command: TripCreationCommand) { check(this.command == null || this.command == command); this.command = command }
        override fun clear(command: TripCreationCommand) { check(this.command == command); this.command = null }
    }
    private class Creator : TripCreation {
        var commits = 0
        var result: TripCreationRecovery = TripCreationRecovery.NotSubmitted
        override suspend fun recover(command: TripCreationCommand) = result
        override suspend fun commit(command: TripCreationCommand): ImportReceipt {
            commits++
            return ImportReceipt(command.operationId, command.tripId, command.digest, emptyList()).also {
                result = TripCreationRecovery.Committed(it)
            }
        }
    }
    private fun model(creator: Creator = Creator(), journal: Journal = Journal(),
        interpret: suspend (TripDraftRequest) -> TripDraftMetadata = { TripDraftMetadata(TripDateSuggestion(null)) }) =
        TripDraftViewModel(creator, journal, dispatcher, interpret).also { stores.put("draft", it) }

    @Test fun sameEnvironmentOnRecompositionDoesNotCancelMetadataAndRetryUsesSameReference() = runTest(dispatcher) {
        val requests = mutableListOf<TripDraftRequest>()
        val response = CompletableDeferred<TripDraftMetadata>()
        val vm = model { requests += it; response.await() }
        advanceUntilIdle()
        vm.environment(1, null)
        vm.metadata("明天出发", { true })
        runCurrent()
        vm.environment(1, null)
        response.complete(TripDraftMetadata(TripDateSuggestion(LocalDate.of(2026, 10, 14), 3)))
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 10, 14), vm.state.value.startDate)
        vm.retryMetadata { true }
        advanceUntilIdle()
        assertEquals(2, requests.size)
        assertEquals(requests[0], requests[1])
    }

    @Test fun manualDateWinsOverLateModelAndEnvironmentChangesDiscardStaleResponse() = runTest(dispatcher) {
        val response = CompletableDeferred<TripDraftMetadata>()
        val vm = model { response.await() }
        advanceUntilIdle()
        vm.environment(1, null)
        vm.metadata("明天出发", { true })
        runCurrent()
        vm.editDates(null, 5)
        vm.environment(2, null)
        response.complete(TripDraftMetadata(TripDateSuggestion(LocalDate.of(2026, 10, 14), 3)))
        advanceUntilIdle()
        assertNull(vm.state.value.startDate)
        assertEquals(5, vm.state.value.dayCount)
        assertFalse(vm.state.value.metadataBusy)
    }

    @Test fun invalidSessionShowsErrorAndWritesNothing() = runTest(dispatcher) {
        val creator = Creator(); val journal = Journal(); val vm = model(creator, journal)
        advanceUntilIdle()
        vm.confirm(listOf(item), emptyList())
        vm.submit({ false }, {})
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals(0, creator.commits)
        assertNull(journal.command)
        assertFalse(vm.selectionIsCurrent(emptyList()))
        assertTrue(vm.selectionIsCurrent(listOf(item)))
    }

    @Test fun restoreNeverAutomaticallySubmitsAndDeletedOperationNeverReappears() = runTest(dispatcher) {
        val creator = Creator(); val journal = Journal()
        val command = TripCreationCommand.create("旅行", null, 1,
            com.yangchengwei.easytrip.core.model.TravelMode.FLEXIBLE, listOf(item), emptyList())
        journal.command = command
        val vm = model(creator, journal)
        advanceUntilIdle()
        assertTrue(vm.state.value.confirming)
        assertTrue(vm.state.value.submissionLocked)
        assertEquals(0, creator.commits)
        creator.result = TripCreationRecovery.Deleted
        vm.retryRecovery()
        advanceUntilIdle()
        vm.submit({ true }, {})
        assertTrue(vm.state.value.deleted)
        assertEquals(0, creator.commits)
        vm.acknowledge()
        assertNull(journal.command)
    }

    @Test fun unreadableRecoveryCannotBeOverwrittenByNewDraft() = runTest(dispatcher) {
        val journal = Journal().apply { unreadable = true }
        val vm = model(journal = journal)
        advanceUntilIdle()
        assertTrue(vm.state.value.restored)
        assertNotNull(vm.state.value.error)
        vm.confirm(listOf(item), emptyList())
        assertFalse(vm.state.value.confirming)
    }

    @Test fun recoveredUnsubmittedCommandCanBeEditedAndUsesANewOperation() = runTest(dispatcher) {
        val creator = Creator(); val journal = Journal()
        val old = TripCreationCommand.create("原名称", null, 1,
            com.yangchengwei.easytrip.core.model.TravelMode.FLEXIBLE, listOf(item), emptyList())
        journal.command = old
        val vm = model(creator, journal)
        advanceUntilIdle()
        vm.resumeEditing()
        advanceUntilIdle()
        assertNull(journal.command)
        assertEquals(listOf(item), vm.recoveredSelection())
        vm.editName("修改后")
        vm.submit({ vm.selectionIsCurrent(vm.recoveredSelection()) }, {})
        advanceUntilIdle()
        assertEquals(1, creator.commits)
        assertEquals("修改后", journal.command?.name)
        assertNotEquals(old.operationId, journal.command?.operationId)
    }
}
