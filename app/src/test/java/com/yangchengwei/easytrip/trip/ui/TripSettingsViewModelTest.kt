package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.*
import java.time.LocalDate
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun overThirtyDaysShowsSpecificErrorWithoutPreviewOrApply() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-31"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals("旅行最多 30 天", model.state.value.dateRange.error)
        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(0, repository.countCalls)
        assertEquals(0, repository.applyCalls)
    }

    @Test fun endBeforeStartDoesNotSubmit() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-09-30"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals("结束日期不能早于开始日期", model.state.value.dateRange.error)
        assertEquals(null, model.state.value.dateRange.confirmation)
        assertEquals(0, repository.applyCalls)
    }

    @Test fun shrinkingRangeRequiresImpactConfirmationAndCancelWritesNothing() = runTest(dispatcher) {
        val repository = FakeRepository(counts = DateRangeDeletionCounts(3, 2, 5))
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        val impact = model.state.value.dateRange.confirmation!!
        assertEquals(listOf("day-2", "day-3"), impact.deletedDayIds)
        assertEquals(3, impact.deletedItineraryItems)
        assertEquals(2, impact.deletedRouteLegs)
        assertEquals(5, impact.retainedSavedPlaces)
        model.cancelDateRangeChange()
        advanceUntilIdle()
        assertEquals(0, repository.applyCalls)
    }

    @Test fun confirmationIsSingleFlightAndFreshMismatchRequiresNewPreview() = runTest(dispatcher) {
        val repository = FakeRepository().apply { applyBlock = CompletableDeferred(); applyFailure = IllegalStateException() }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        model.confirmDateRangeChange()
        model.confirmDateRangeChange()
        advanceUntilIdle()
        assertEquals(1, repository.applyCalls)
        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("保存结果未生效，请重新检查影响", model.state.value.dateRange.error)
        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)

        repository.applyFailure = null
        repository.applyBlock = null
        model.confirmDateRangeChange()
        advanceUntilIdle()
        assertEquals(1, repository.applyCalls)
        model.requestDateRangeChange()
        advanceUntilIdle()
        assertEquals(DateRangeChangePhase.AwaitingConfirmation::class, model.state.value.dateRange.phase::class)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun nonDestructiveApplyIsSingleFlight() = runTest(dispatcher) {
        val repository = FakeRepository().apply { applyBlock = CompletableDeferred() }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-30"))
        model.requestDateRangeChange()
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(1, repository.applyCalls)
        assertEquals(true, model.state.value.dateRange.submitting)
        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(true, model.state.value.dateRange.submitting)
        repository.emit(repository.tripWithDayCount(30))
        advanceUntilIdle()
        assertEquals(false, model.state.value.dateRange.submitting)
    }

    @Test fun stalePreviewCannotReplaceNewerDirtyDraft() = runTest(dispatcher) {
        val repository = FakeRepository().apply { countsBlock = CompletableDeferred() }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-05"))
        repository.countsBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(LocalDate.parse("2026-10-01"), model.state.value.dateRange.startDate)
        assertEquals(LocalDate.parse("2026-10-05"), model.state.value.dateRange.endDate)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(0, repository.applyCalls)
    }

    @Test fun inputAndBackActionsAreIgnoredWhileApplying() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitApply = false
            applyBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        val applying = model.state.value.dateRange.phase

        model.updateDateEndDraft(LocalDate.parse("2026-10-06"))
        model.cancelDateRangeChange()
        model.requestDateRangeChange()
        model.confirmDateRangeChange()
        advanceUntilIdle()

        assertEquals(applying, model.state.value.dateRange.phase)
        assertEquals(LocalDate.parse("2026-10-04"), model.state.value.dateRange.endDate)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun previewFailureKeepsDirtyDraftAndReturnsToIdle() = runTest(dispatcher) {
        val repository = FakeRepository().apply { countsFailure = IllegalStateException() }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(LocalDate.parse("2026-10-01"), model.state.value.dateRange.endDate)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals("无法检查日期范围，请重试", model.state.value.dateRange.error)
    }

    @Test fun tripDeleteRequestLoadsCurrentTripImpact() = runTest(dispatcher) {
        val impacts = FakeImpacts().apply { tripImpact = TripDeleteImpact(3, 2, 1, 4, 5) }
        val model = model(FakeRepository(), impacts)
        advanceUntilIdle()

        model.requestTripDeletion()
        advanceUntilIdle()

        val deletion = model.state.value.tripDeletion as TripDeletionUiState.Ready
        assertEquals("Trip", deletion.tripName)
        assertEquals(
            listOf("3 个旅行日", "2 个收藏地点", "1 个标签", "4 个行程项", "5 个路线段"),
            deletion.confirmation.deletedItems,
        )
    }

    @Test fun tripDeletionRequestBeforeFirstAuthoritativeTripEmissionIsIgnored() = runTest(dispatcher) {
        val initialTrip = CompletableDeferred<TripWithDays?>()
        val repository = FakeRepository(initialTrip = initialTrip)
        val impacts = FakeImpacts()
        val model = model(repository, impacts)
        runCurrent()

        model.requestTripDeletion()
        advanceUntilIdle()

        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
        assertEquals(0, impacts.tripCalls)
        initialTrip.complete(repository.currentTrip())
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()
        assertEquals(1, impacts.tripCalls)
    }

    @Test fun cancelTripDeletionInvalidatesLateImpact() = runTest(dispatcher) {
        val impact = CompletableDeferred<TripDeleteImpact>()
        val impacts = FakeImpacts().apply { tripBlock = impact }
        val model = model(FakeRepository(), impacts)
        advanceUntilIdle()

        model.requestTripDeletion()
        runCurrent()
        model.cancelTripDeletion()
        impact.complete(TripDeleteImpact(3, 0, 0, 0, 0))
        advanceUntilIdle()

        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
    }

    @Test fun repeatedTripDeleteConfirmCallsServiceExactlyOnce() = runTest(dispatcher) {
        val repository = FakeRepository().apply { tripDeleteBlock = CompletableDeferred() }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()

        model.confirmTripDeletion()
        model.confirmTripDeletion()
        runCurrent()

        assertEquals(1, repository.tripDeleteCalls)
        repository.tripDeleteBlock!!.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun tripDeleteWaitsForAuthoritativeNullBeforeReturning() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitTripDelete = false }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()

        model.confirmTripDeletion()
        advanceUntilIdle()

        assertEquals(1, repository.tripDeleteCalls)
        assertEquals(true, (model.state.value.tripDeletion as TripDeletionUiState.Ready).isDeleting)
        val effect = async { model.effects.first() }
        runCurrent()
        assertEquals(false, effect.isCompleted)

        repository.emit(null)
        advanceUntilIdle()

        assertEquals(TripSettingsEffect.ReturnToTripList, effect.await())
    }

    @Test fun tripDeleteNullDuringServiceCallReturnsWhenServiceCompletes() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitTripDelete = false
            tripDeleteBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()
        val effect = async { model.effects.first() }
        runCurrent()

        model.confirmTripDeletion()
        runCurrent()
        repository.emit(null)
        advanceUntilIdle()
        assertEquals(true, (model.state.value.tripDeletion as TripDeletionUiState.Ready).isDeleting)
        assertEquals(false, effect.isCompleted)

        repository.tripDeleteBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(TripSettingsEffect.ReturnToTripList, effect.await())
        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
    }

    @Test fun roomConfirmedDeletionCompletesOnceWhenLateServiceFails() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitTripDelete = false
            tripDeleteBlock = CompletableDeferred()
        }
        val model = model(repository)
        val effects = mutableListOf<TripSettingsEffect>()
        val collector = launch { model.effects.collect(effects::add) }
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()

        model.confirmTripDeletion()
        runCurrent()
        repository.emit(null)
        advanceUntilIdle()
        assertEquals(true, (model.state.value.tripDeletion as TripDeletionUiState.Ready).isDeleting)

        repository.tripDeleteBlock!!.completeExceptionally(IllegalStateException("Unknown trip"))
        advanceUntilIdle()
        repository.emit(null)
        advanceUntilIdle()

        assertEquals(listOf(TripSettingsEffect.ReturnToTripList), effects)
        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
        collector.cancel()
    }

    @Test fun serviceFailureBeforeRoomDeletionKeepsConfirmationRetryable() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitTripDelete = false
            tripDeleteFailure = IllegalStateException("write failed")
        }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()
        val confirmation = (model.state.value.tripDeletion as TripDeletionUiState.Ready).confirmation

        model.confirmTripDeletion()
        advanceUntilIdle()

        val failed = model.state.value.tripDeletion as TripDeletionUiState.Ready
        assertEquals(confirmation, failed.confirmation)
        assertEquals("删除失败，请重试", failed.errorMessage)
        assertEquals(true, repository.currentTrip().id == "trip")
    }

    @Test fun tripDeleteFailureKeepsConfirmationAndCanRetry() = runTest(dispatcher) {
        val repository = FakeRepository().apply { tripDeleteFailure = IllegalStateException("write failed") }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()
        val confirmation = (model.state.value.tripDeletion as TripDeletionUiState.Ready).confirmation

        model.confirmTripDeletion()
        advanceUntilIdle()

        val failed = model.state.value.tripDeletion as TripDeletionUiState.Ready
        assertEquals(confirmation, failed.confirmation)
        assertEquals("删除失败，请重试", failed.errorMessage)
        repository.tripDeleteFailure = null
        model.confirmTripDeletion()
        advanceUntilIdle()
        assertEquals(2, repository.tripDeleteCalls)
    }

    @Test fun collectorFailureBeforeTripDeleteServiceReturnsShowsOnlyDeletionSyncFailure() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitTripDelete = false
            tripDeleteBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()

        model.confirmTripDeletion()
        runCurrent()
        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        assertEquals(null, model.state.value.observationError)

        repository.tripDeleteBlock!!.complete(Unit)
        advanceUntilIdle()

        val failed = model.state.value.tripDeletion as TripDeletionUiState.Ready
        assertEquals(true, failed.confirmationSyncFailed)
        assertEquals("删除成功，但同步确认失败，请重新同步", failed.errorMessage)
        assertEquals(null, model.state.value.observationError)
        model.retryTripDeletionSync()
        repository.emit(null)
        advanceUntilIdle()
        assertEquals(1, repository.tripDeleteCalls)
    }

    @Test fun tripDeleteSyncFailureResyncsWithoutRepeatingService() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitTripDelete = false }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()
        model.confirmTripDeletion()
        advanceUntilIdle()
        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()

        val failed = model.state.value.tripDeletion as TripDeletionUiState.Ready
        assertEquals(true, failed.confirmationSyncFailed)
        assertEquals("删除成功，但同步确认失败，请重新同步", failed.errorMessage)
        model.retryTripDeletionSync()
        repository.emit(null)
        advanceUntilIdle()

        assertEquals(1, repository.tripDeleteCalls)
    }

    @Test fun syncFailedTripDeletionCannotBeCancelledAndKeepsAllWritesLocked() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitTripDelete = false }
        val model = model(repository)
        advanceUntilIdle()
        model.requestTripDeletion()
        advanceUntilIdle()
        model.confirmTripDeletion()
        advanceUntilIdle()
        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        val failed = model.state.value.tripDeletion as TripDeletionUiState.Ready

        model.cancelTripDeletion()
        model.rename("Renamed")
        model.setTravelMode(TravelMode.SELF_DRIVE)
        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()

        assertEquals(failed, model.state.value.tripDeletion)
        assertEquals(0, repository.renameCalls)
        assertEquals(0, repository.travelModeCalls)
        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(1, repository.tripDeleteCalls)
    }

    @Test fun syncFailedAndDeletingTripDeletionRejectDateEndDraftMutations() = runTest(dispatcher) {
        val syncRepository = FakeRepository().apply { autoEmitTripDelete = false }
        val syncModel = model(syncRepository)
        advanceUntilIdle()
        syncModel.requestTripDeletion()
        advanceUntilIdle()
        syncModel.confirmTripDeletion()
        advanceUntilIdle()
        syncRepository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        val syncRange = syncModel.state.value.dateRange
        assertEquals(true, (syncModel.state.value.tripDeletion as TripDeletionUiState.Ready).confirmationSyncFailed)

        syncModel.updateDateEndDraft(LocalDate.parse("2026-10-01"))

        assertEquals(syncRange.endDate, syncModel.state.value.dateRange.endDate)
        assertEquals(syncRange.isDirty, syncModel.state.value.dateRange.isDirty)

        val deletingRepository = FakeRepository().apply { tripDeleteBlock = CompletableDeferred() }
        val deletingModel = model(deletingRepository)
        advanceUntilIdle()
        deletingModel.requestTripDeletion()
        advanceUntilIdle()
        deletingModel.confirmTripDeletion()
        runCurrent()
        val deletingRange = deletingModel.state.value.dateRange
        assertEquals(true, (deletingModel.state.value.tripDeletion as TripDeletionUiState.Ready).isDeleting)

        deletingModel.updateDateEndDraft(LocalDate.parse("2026-10-01"))

        assertEquals(deletingRange.endDate, deletingModel.state.value.dateRange.endDate)
        assertEquals(deletingRange.isDirty, deletingModel.state.value.dateRange.isDirty)
    }

    @Test fun cancellingLoadingImpactFailureOrReadyNeverDeletesTheTrip() = runTest(dispatcher) {
        val loadingImpact = CompletableDeferred<TripDeleteImpact>()
        val impacts = FakeImpacts().apply { tripBlock = loadingImpact }
        val repository = FakeRepository()
        val model = model(repository, impacts)
        advanceUntilIdle()

        model.requestTripDeletion()
        runCurrent()
        model.cancelTripDeletion()
        loadingImpact.complete(TripDeleteImpact(3, 0, 0, 0, 0))
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)

        impacts.tripBlock = null
        impacts.tripFailure = IllegalStateException("impact unavailable")
        model.requestTripDeletion()
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.ImpactFailure::class, model.state.value.tripDeletion::class)
        model.cancelTripDeletion()
        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)

        impacts.tripFailure = null
        model.requestTripDeletion()
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Ready::class, model.state.value.tripDeletion::class)
        model.cancelTripDeletion()
        advanceUntilIdle()

        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
        assertEquals(0, repository.tripDeleteCalls)
        assertEquals("Trip", repository.currentTrip().name)
    }

    @Test fun tripDeletionAndDateRangeAndDayDeletionAreMutuallyExclusive() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        model.requestTripDeletion()
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(TripDeletionUiState.Ready::class, model.state.value.tripDeletion::class)
        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
    }

    @Test fun pendingDayDeletionBlocksTripDeletionRequest() = runTest(dispatcher) {
        val model = model(FakeRepository())
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()

        model.requestTripDeletion()
        advanceUntilIdle()

        assertNotNull(model.state.value.pendingDayDeletion)
        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
    }

    @Test fun pendingDateRangeDeletionBlocksTripDeletionRequest() = runTest(dispatcher) {
        val repository = FakeRepository(counts = DateRangeDeletionCounts(1, 1, 0))
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        model.requestTripDeletion()
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.AwaitingConfirmation::class, model.state.value.dateRange.phase::class)
        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
    }

    @Test fun externalTripDeleteEmitsReturnToTripListOnlyOnce() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()
        val effect = async { model.effects.first() }
        runCurrent()

        repository.emit(null)
        repository.emit(null)
        advanceUntilIdle()

        assertEquals(TripSettingsEffect.ReturnToTripList, effect.await())
        assertEquals(TripDeletionUiState.Idle, model.state.value.tripDeletion)
    }

    @Test fun deletedTripPublishesReturnToTripList() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        repository.emit(null)
        advanceUntilIdle()

        assertEquals(TripSettingsEffect.ReturnToTripList, model.effects.first())
    }

    @Test fun roomEmissionDoesNotOverwriteDirtyEndDateDraft() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-05"))
        repository.emit(repository.currentTrip().copy(name = "Renamed"))
        advanceUntilIdle()

        assertEquals(LocalDate.parse("2026-10-05"), model.state.value.dateRange.endDate)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals("Renamed", model.state.value.name)
    }

    @Test fun roomEmissionUpdatesBaselineAndCleanDraftTogether() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        repository.emit(repository.tripWithDayCount(2))
        advanceUntilIdle()

        assertEquals(LocalDate.parse("2026-10-02"), model.state.value.dateRange.baselineEndDate)
        assertEquals(LocalDate.parse("2026-10-02"), model.state.value.dateRange.endDate)
        assertEquals(false, model.state.value.dateRange.isDirty)
    }

    @Test fun applyReturnWaitsForMatchingRoomEmissionAndWritesOnce() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitApply = false }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(1, repository.applyCalls)
        assertEquals(true, model.state.value.dateRange.submitting)

        model.requestDateRangeChange()
        model.confirmDateRangeChange()
        advanceUntilIdle()
        assertEquals(1, repository.applyCalls)

        repository.emit(repository.tripWithDayCount(4))
        advanceUntilIdle()
        assertEquals(false, model.state.value.dateRange.submitting)
        assertEquals(false, model.state.value.dateRange.isDirty)
    }

    @Test fun roomTargetBeforeServiceReturnCompletesWhenServiceReturns() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitApply = false
            applyBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()

        val target = LocalDate.parse("2026-10-04")
        model.updateDateEndDraft(target)
        model.requestDateRangeChange()
        advanceUntilIdle()
        repository.emit(repository.tripWithDayCount(4))
        advanceUntilIdle()

        assertEquals(true, model.state.value.dateRange.submitting)
        assertEquals(1, repository.applyCalls)

        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(false, model.state.value.dateRange.submitting)
        assertEquals(false, model.state.value.dateRange.isDirty)
        assertEquals(target, model.state.value.dateRange.baselineEndDate)
        assertEquals(target, model.state.value.dateRange.endDate)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun freshMismatchAfterMatchingRoomFactPreventsCompletionWhenServiceReturns() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitApply = false
            applyBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        repository.emit(repository.tripWithDayCount(4))
        advanceUntilIdle()
        repository.emit(repository.currentTrip().copy(days = listOf(
            TripDay("day-1", 0), TripDay("wrong", 1), TripDay("day-3", 2), TripDay("added", 3),
        )))
        advanceUntilIdle()

        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.AwaitingRoom::class, model.state.value.dateRange.phase::class)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun serviceReturnBeforeRoomTargetWaitsUntilMatchingEmission() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitApply = false }
        val model = model(repository)
        advanceUntilIdle()

        val target = LocalDate.parse("2026-10-04")
        model.updateDateEndDraft(target)
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(true, model.state.value.dateRange.submitting)
        assertEquals(1, repository.applyCalls)

        repository.emit(repository.tripWithDayCount(3).copy(name = "Old emission"))
        advanceUntilIdle()
        assertEquals(true, model.state.value.dateRange.submitting)

        repository.emit(repository.tripWithDayCount(4))
        advanceUntilIdle()

        assertEquals(false, model.state.value.dateRange.submitting)
        assertEquals(false, model.state.value.dateRange.isDirty)
        assertEquals(target, model.state.value.dateRange.baselineEndDate)
        assertEquals(target, model.state.value.dateRange.endDate)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun exhaustedRoomConfirmationShowsRetryWithoutApplyingAgain() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitApply = false }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.SyncFailed::class, model.state.value.dateRange.phase::class)
        assertEquals("保存结果待同步确认，请重新同步", model.state.value.dateRange.error)
        assertEquals(1, repository.applyCalls)

        model.retryDateRangeSync()
        repository.emit(repository.tripWithDayCount(4))
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(false, model.state.value.dateRange.isDirty)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun repeatedRetryWhileSyncingStartsOneCollector() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitApply = false }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        val startsBeforeRetry = repository.collectorStarts

        model.retryDateRangeSync()
        model.retryDateRangeSync()
        runCurrent()

        assertEquals(startsBeforeRetry + 1, repository.collectorStarts)
        assertEquals(1, repository.activeCollectors)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun staleCollectorCannotCompleteNewGeneration() = runTest(dispatcher) {
        val target = LocalDate.parse("2026-10-04")
        val repository = FakeRepository().apply { autoEmitApply = false }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(target)
        model.requestDateRangeChange()
        advanceUntilIdle()
        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        val staleGeneration = model.currentTripObservationGeneration()
        repository.resetMaxActiveCollectors()

        model.retryDateRangeSync()
        advanceUntilIdle()
        assertEquals(staleGeneration + 1, model.currentTripObservationGeneration())
        assertEquals(1, repository.activeCollectors)
        assertEquals(1, repository.maxActiveCollectors)

        model.acceptTripObservation(repository.tripWithDayCount(4), staleGeneration)
        advanceUntilIdle()
        assertEquals(true, model.state.value.dateRange.submitting)
        assertEquals(1, repository.applyCalls)

        repository.emit(repository.tripWithDayCount(4))
        advanceUntilIdle()
        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun collectorFailureBeforeServiceReturnBecomesSyncFailedWhenServiceReturns() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitApply = false
            applyBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        assertEquals(DateRangeChangePhase.Applying::class, model.state.value.dateRange.phase::class)

        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.SyncFailed::class, model.state.value.dateRange.phase::class)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun previewSnapshotChangeReturnsToIdleWithSpecificError() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            countsFailure = DateRangeSnapshotChangedException("changed")
        }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals("旅行内容已变化，请重新确认", model.state.value.dateRange.error)
        assertEquals(0, repository.applyCalls)
    }

    @Test fun unknownApplyResultWithCommittedRoomFactCompletesWithoutApplyingAgain() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitApply = false
            applyFailure = IllegalStateException("result unknown")
            commitBeforeApplyFailure = true
        }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(false, model.state.value.dateRange.isDirty)
        assertEquals(1, repository.applyCalls)
        model.requestDateRangeChange()
        advanceUntilIdle()
        assertEquals(1, repository.applyCalls)
    }

    @Test fun unknownApplyResultWithoutWriteFreshMismatchAllowsNewPreviewGeneration() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitApply = false
            applyFailure = IllegalStateException("result unknown")
        }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals("保存结果未生效，请重新检查影响", model.state.value.dateRange.error)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals(1, repository.applyCalls)

        repository.applyFailure = null
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(2, repository.applyCalls)
        val secondRequest = (model.state.value.dateRange.phase as DateRangeChangePhase.AwaitingRoom).request
        assertEquals(3L, secondRequest.generation)
    }

    @Test fun growthReorderedBaselinePrefixDoesNotCompleteRoomConfirmation() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitApply = false }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        repository.emit(repository.currentTrip().copy(days = listOf(
            TripDay("day-2", 0), TripDay("day-1", 1), TripDay("day-3", 2), TripDay("added", 3),
        )))
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.AwaitingRoom::class, model.state.value.dateRange.phase::class)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun sameSizeWrongDayIdsDoNotMatchRequest() {
        val request = DateRangeChangeRequest(
            1,
            "trip",
            LocalDate.parse("2026-10-01"),
            listOf("day-1", "day-2", "day-3"),
            LocalDate.parse("2026-10-03"),
        )
        val trip = TripWithDays(
            "trip", "Trip", request.baselineStartDate, TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("wrong", 1), TripDay("day-3", 2)),
        )

        assertEquals(false, tripMatchesDateRangeRequest(trip, request, impact(request)))
    }

    @Test fun sameSizeReorderedDayIdsDoNotMatchRequest() {
        val request = DateRangeChangeRequest(
            1,
            "trip",
            LocalDate.parse("2026-10-01"),
            listOf("day-1", "day-2", "day-3"),
            LocalDate.parse("2026-10-03"),
        )
        val trip = TripWithDays(
            "trip", "Trip", request.baselineStartDate, TravelMode.FLEXIBLE,
            listOf(TripDay("day-2", 0), TripDay("day-1", 1), TripDay("day-3", 2)),
        )

        assertEquals(false, tripMatchesDateRangeRequest(trip, request, impact(request)))
    }

    @Test fun shrinkWrongRetainedIdsDoNotCompleteRoomConfirmation() = runTest(dispatcher) {
        val repository = FakeRepository(counts = DateRangeDeletionCounts(0, 0, 0)).apply {
            autoEmitApply = false
            applyBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-02"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        model.confirmDateRangeChange()
        advanceUntilIdle()

        repository.emit(repository.currentTrip().copy(days = listOf(TripDay("day-1", 0), TripDay("wrong", 1))))
        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.AwaitingRoom::class, model.state.value.dateRange.phase::class)
        assertEquals(true, model.state.value.dateRange.isDirty)
    }

    @Test fun shrinkReorderedRetainedIdsDoNotCompleteRoomConfirmation() = runTest(dispatcher) {
        val repository = FakeRepository(counts = DateRangeDeletionCounts(0, 0, 0)).apply {
            autoEmitApply = false
            applyBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-02"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        model.confirmDateRangeChange()
        advanceUntilIdle()

        repository.emit(repository.currentTrip().copy(days = listOf(TripDay("day-2", 0), TripDay("day-1", 1))))
        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.AwaitingRoom::class, model.state.value.dateRange.phase::class)
        assertEquals(true, model.state.value.dateRange.isDirty)
    }

    private fun impact(request: DateRangeChangeRequest) = DateRangeChangeImpact(
        request,
        retainedDayIds = request.baselineDayIds,
        deletedDayIds = emptyList(),
        deletedItineraryItems = 0,
        deletedRouteLegs = 0,
        retainedSavedPlaces = 0,
    )

    @Test fun activeDatePhaseGuardsAllRepositoryWriteActions() = runTest(dispatcher) {
        suspend fun assertWritesBlocked(model: TripSettingsViewModel, repository: FakeRepository, impacts: FakeImpacts) {
            model.rename("Blocked")
            model.setTravelMode(TravelMode.SELF_DRIVE)
            model.requestDelete(model.state.value.days.last())
            model.confirmDelete()
            advanceUntilIdle()
            assertEquals(0, repository.renameCalls)
            assertEquals(0, repository.travelModeCalls)
            assertEquals(0, repository.deleteCalls)
            assertEquals(0, impacts.dayCalls)
        }

        run {
            val repository = FakeRepository().apply { countsBlock = CompletableDeferred() }
            val impacts = FakeImpacts()
            val model = model(repository, impacts)
            advanceUntilIdle()
            model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
            model.requestDateRangeChange()
            runCurrent()
            assertEquals(DateRangeChangePhase.Previewing::class, model.state.value.dateRange.phase::class)
            assertWritesBlocked(model, repository, impacts)
        }
        run {
            val repository = FakeRepository(counts = DateRangeDeletionCounts(1, 1, 0))
            val impacts = FakeImpacts()
            val model = model(repository, impacts)
            advanceUntilIdle()
            model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
            model.requestDateRangeChange()
            advanceUntilIdle()
            assertEquals(DateRangeChangePhase.AwaitingConfirmation::class, model.state.value.dateRange.phase::class)
            assertWritesBlocked(model, repository, impacts)
        }
        run {
            val repository = FakeRepository(counts = DateRangeDeletionCounts(1, 1, 0)).apply { applyBlock = CompletableDeferred() }
            val impacts = FakeImpacts()
            val model = model(repository, impacts)
            advanceUntilIdle()
            model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
            model.requestDateRangeChange()
            advanceUntilIdle()
            model.confirmDateRangeChange()
            runCurrent()
            assertEquals(DateRangeChangePhase.Applying::class, model.state.value.dateRange.phase::class)
            assertWritesBlocked(model, repository, impacts)
        }
        run {
            val repository = FakeRepository().apply { autoEmitApply = false }
            val impacts = FakeImpacts()
            val model = model(repository, impacts)
            advanceUntilIdle()
            model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
            model.requestDateRangeChange()
            advanceUntilIdle()
            assertEquals(DateRangeChangePhase.AwaitingRoom::class, model.state.value.dateRange.phase::class)
            assertWritesBlocked(model, repository, impacts)
        }
        run {
            val repository = FakeRepository().apply { autoEmitApply = false }
            val impacts = FakeImpacts()
            val model = model(repository, impacts)
            advanceUntilIdle()
            model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
            model.requestDateRangeChange()
            advanceUntilIdle()
            repository.failObservation(IllegalStateException("db unavailable"))
            advanceUntilIdle()
            assertEquals(DateRangeChangePhase.SyncFailed::class, model.state.value.dateRange.phase::class)
            assertWritesBlocked(model, repository, impacts)
        }
    }

    @Test fun previewingDateRangeInvalidatesLateDayDeleteImpact() = runTest(dispatcher) {
        val repository = FakeRepository().apply { countsBlock = CompletableDeferred() }
        val impacts = FakeImpacts().apply { dayBlock = CompletableDeferred() }
        val model = model(repository, impacts)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        runCurrent()

        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        runCurrent()
        assertEquals(DateRangeChangePhase.Previewing::class, model.state.value.dateRange.phase::class)

        impacts.completeDayImpact()
        advanceUntilIdle()

        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(null, model.state.value.dayDeletionRetry)
        assertEquals(null, model.state.value.dayDeleteError)
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun previewingDateRangeClearsExistingDayDeleteConfirmation() = runTest(dispatcher) {
        val repository = FakeRepository().apply { countsBlock = CompletableDeferred() }
        val model = model(repository)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()
        assertNotNull(model.state.value.pendingDayDeletion)

        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        runCurrent()

        assertEquals(DateRangeChangePhase.Previewing::class, model.state.value.dateRange.phase::class)
        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(null, model.state.value.dayDeletionRetry)
        assertEquals(null, model.state.value.dayDeleteError)
    }

    @Test fun syncFailedDateRangeInvalidatesLateDayDeleteImpact() = runTest(dispatcher) {
        val repository = FakeRepository().apply { autoEmitApply = false }
        val impacts = FakeImpacts().apply { dayBlock = CompletableDeferred() }
        val model = model(repository, impacts)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        runCurrent()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        runCurrent()
        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()
        assertEquals(DateRangeChangePhase.SyncFailed::class, model.state.value.dateRange.phase::class)

        impacts.completeDayImpact()
        advanceUntilIdle()

        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(null, model.state.value.dayDeletionRetry)
        assertEquals(null, model.state.value.dayDeleteError)
        model.confirmDelete()
        model.cancelDelete()
        model.retryDelete()
        advanceUntilIdle()
        assertEquals(1, impacts.dayCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun dayDeleteInProgressBlocksDateRangeRequestUntilDeleteCompletes() = runTest(dispatcher) {
        val repository = FakeRepository().apply { deleteBlock = CompletableDeferred() }
        val model = model(repository)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()

        model.confirmDelete()
        runCurrent()
        assertEquals(true, model.state.value.dayDeleteInProgress)

        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        runCurrent()

        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(0, repository.countCalls)
        assertEquals(0, repository.applyCalls)
        assertEquals(true, model.state.value.dayDeleteInProgress)

        repository.deleteBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(false, model.state.value.dayDeleteInProgress)
        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(1, repository.deleteCalls)
    }

    @Test fun idleAllowsRepositoryWriteActions() = runTest(dispatcher) {
        val repository = FakeRepository()
        val impacts = FakeImpacts()
        val model = model(repository, impacts)
        advanceUntilIdle()

        model.rename("Allowed")
        model.setTravelMode(TravelMode.SELF_DRIVE)
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()
        model.confirmDelete()
        advanceUntilIdle()

        assertEquals(1, repository.renameCalls)
        assertEquals(1, repository.travelModeCalls)
        assertEquals(1, repository.deleteCalls)
        assertEquals(1, impacts.dayCalls)
    }

    @Test fun idleObservationFailureShowsPageErrorAndRetryRecovers() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        repository.failObservation(IllegalStateException("db unavailable"))
        advanceUntilIdle()

        assertEquals("无法加载旅行设置，请重试", model.state.value.observationError)
        val startsBeforeRetry = repository.collectorStarts

        model.retryTripObservation()
        runCurrent()
        repository.emit(repository.currentTrip().copy(name = "Recovered"))
        advanceUntilIdle()

        assertEquals(startsBeforeRetry + 1, repository.collectorStarts)
        assertEquals("Recovered", model.state.value.name)
        assertEquals(null, model.state.value.observationError)
    }

    @Test fun requestDeleteWhileDeletionIsInProgressDoesNotStartImpact() = runTest(dispatcher) {
        val repository = FakeRepository().apply { deleteBlock = CompletableDeferred() }
        val impacts = FakeImpacts()
        val model = model(repository, impacts)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()
        model.confirmDelete()
        runCurrent()
        assertEquals(true, model.state.value.dayDeleteInProgress)
        assertEquals(1, impacts.dayCalls)

        model.requestDelete(model.state.value.days.first())
        advanceUntilIdle()

        assertEquals(1, impacts.dayCalls)
        repository.deleteBlock!!.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun observationCancellationIsRethrown() = runTest(dispatcher) {
        val cancellation = CancellationException("observation cancelled")
        val completion = CompletableDeferred<Throwable?>()
        val repository = FakeRepository().apply {
            nextObservationFailure = cancellation
            nextObservationCompletion = completion
        }

        model(repository)
        advanceUntilIdle()

        val cause = completion.await()
        assertEquals(CancellationException::class, cause!!::class)
        assertEquals(cancellation.message, cause.message)
    }

    @Test fun editingWhileApplyingDoesNotReplaceRequestSnapshot() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            autoEmitApply = false
            applyBlock = CompletableDeferred()
        }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        val applying = model.state.value.dateRange.phase as DateRangeChangePhase.Applying

        model.updateDateEndDraft(LocalDate.parse("2026-10-06"))

        assertEquals(applying, model.state.value.dateRange.phase)
        val lastApply = repository.lastApply
        assertNotNull(lastApply)
        assertEquals(LocalDate.parse("2026-10-04"), lastApply!!.startDate?.plusDays((lastApply.dayCount - 1).toLong()))
    }

    @Test fun lastDayCannotBeRequestedForDeletion() = runTest(dispatcher) {
        val repository = FakeRepository(dayCount = 1)
        val model = model(repository)
        advanceUntilIdle()

        model.requestDelete(model.state.value.days.single())
        advanceUntilIdle()

        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(0, repository.deleteCalls)
    }

    @Test fun snapshotChangeReturnsToIdleAndRequiresFreshPreview() = runTest(dispatcher) {
        val repository = FakeRepository(counts = DateRangeDeletionCounts(3, 2, 5)).apply {
            applyFailure = DateRangeSnapshotChangedException("changed")
        }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateEndDraft(LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        model.confirmDateRangeChange()
        advanceUntilIdle()

        assertEquals(DateRangeChangePhase.Idle, model.state.value.dateRange.phase)
        assertEquals(true, model.state.value.dateRange.isDirty)
        assertEquals("旅行内容已变化，请重新确认", model.state.value.dateRange.error)
        assertEquals(1, repository.applyCalls)

        repository.applyFailure = null
        model.requestDateRangeChange()
        advanceUntilIdle()
        assertEquals(DateRangeChangePhase.AwaitingConfirmation::class, model.state.value.dateRange.phase::class)
        assertEquals(1, repository.applyCalls)
    }

    @Test fun applyCancellationIsRethrown() = runTest(dispatcher) {
        val cancellation = CancellationException("apply cancelled")
        val completion = CompletableDeferred<Throwable?>()
        val repository = FakeRepository().apply {
            applyFailure = cancellation
            applyCompletion = completion
        }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateEndDraft(LocalDate.parse("2026-10-04"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(cancellation, completion.await())
    }

    @Test fun failedDayDeleteRequiresFreshPreviewAndConfirmation() = runTest(dispatcher) {
        val repository = FakeRepository().apply { deleteFailure = IllegalStateException() }
        val impacts = FakeImpacts()
        val model = model(repository, impacts)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()

        model.confirmDelete()
        advanceUntilIdle()
        assertEquals("删除失败，请重新检查影响", model.state.value.dayDeleteError)
        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals("day-3", model.state.value.dayDeletionRetry?.id)

        repository.deleteFailure = null
        impacts.impact = DayDeleteImpact(2, 3, 0)
        model.retryDelete()
        advanceUntilIdle()
        assertEquals(1, repository.deleteCalls)
        assertEquals(2, impacts.dayCalls)
        assertEquals(2, model.state.value.pendingDayDeletion?.impact?.itineraryItems)
        assertEquals(null, model.state.value.dayDeleteError)

        model.confirmDelete()
        advanceUntilIdle()
        assertEquals(2, repository.deleteCalls)
        assertEquals(null, model.state.value.pendingDayDeletion)
    }

    @Test fun failedDayPreviewKeepsVisibleRetryTarget() = runTest(dispatcher) {
        val impacts = FakeImpacts().apply { failure = IllegalStateException() }
        val model = model(FakeRepository(), impacts)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()

        assertEquals("无法检查删除影响，请重试", model.state.value.dayDeleteError)
        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals("day-3", model.state.value.dayDeletionRetry?.id)

        impacts.failure = null
        model.retryDelete()
        advanceUntilIdle()
        assertEquals(2, impacts.dayCalls)
        assertEquals("day-3", model.state.value.pendingDayDeletion?.day?.id)
        assertEquals(null, model.state.value.dayDeleteError)
    }

    private fun model(
        repository: FakeRepository,
        impacts: DeleteImpactProvider = FakeImpacts(),
    ) = TripSettingsViewModel(
        SavedStateHandle(mapOf("tripId" to "trip")),
        TripService(repository),
        repository,
        impacts,
        TripDateRangeService(repository),
    )

    private class FakeImpacts : DeleteImpactProvider {
        var tripImpact = TripDeleteImpact(0, 0, 0, 0, 0)
        var tripBlock: CompletableDeferred<TripDeleteImpact>? = null
        var tripFailure: Throwable? = null
        var tripCalls = 0
        var impact = DayDeleteImpact(1, 1, 0)
        var failure: Throwable? = null
        var dayBlock: CompletableDeferred<Unit>? = null
        var dayCalls = 0
        private var dayContinuation: Continuation<Unit>? = null
        override suspend fun trip(tripId: String): TripDeleteImpact {
            tripCalls++
            tripFailure?.let { throw it }
            return tripBlock?.await() ?: tripImpact
        }
        override suspend fun day(dayId: String): DayDeleteImpact {
            dayCalls++
            if (dayBlock != null) suspendCoroutine { dayContinuation = it }
            failure?.let { throw it }
            return impact
        }
        fun completeDayImpact() {
            dayContinuation?.resume(Unit)
        }
    }

    private class FakeRepository(
        var counts: DateRangeDeletionCounts = DateRangeDeletionCounts(0, 0, 0),
        dayCount: Int = 3,
        initialTrip: CompletableDeferred<TripWithDays?>? = null,
    ) : TripRepository {
        private val trip = MutableStateFlow<TripWithDays?>(TripWithDays(
            "trip", "Trip", LocalDate.parse("2026-10-01"), TravelMode.FLEXIBLE,
            List(dayCount) { TripDay("day-${it + 1}", it) },
        ))
        private val initialTrip = initialTrip
        private var initialTripAwaited = false
        private val observationFailures = MutableSharedFlow<Throwable>()
        var applyCalls = 0
        var collectorStarts = 0
        var activeCollectors = 0
        var maxActiveCollectors = 0
        var nextObservationFailure: Throwable? = null
        var nextObservationCompletion: CompletableDeferred<Throwable?>? = null
        var lastApply: DateRangeApply? = null
        var autoEmitApply = true
        var observeFailure: Throwable? = null
        var countCalls = 0
        var countsBlock: CompletableDeferred<Unit>? = null
        var countsFailure: Throwable? = null
        var applyBlock: CompletableDeferred<Unit>? = null
        var applyFailure: Throwable? = null
        var applyCompletion: CompletableDeferred<Throwable?>? = null
        var commitBeforeApplyFailure = false
        var deleteCalls = 0
        var deleteBlock: CompletableDeferred<Unit>? = null
        var deleteFailure: Throwable? = null
        var tripDeleteCalls = 0
        var tripDeleteBlock: CompletableDeferred<Unit>? = null
        var tripDeleteFailure: Throwable? = null
        var autoEmitTripDelete = true
        var renameCalls = 0
        var travelModeCalls = 0

        fun currentTrip(): TripWithDays = requireNotNull(trip.value)

        fun tripWithDayCount(dayCount: Int): TripWithDays = currentTrip().copy(
            days = List(dayCount) { TripDay("day-${it + 1}", it) },
        )

        fun emit(value: TripWithDays?) {
            trip.value = value
        }

        suspend fun failObservation(failure: Throwable) {
            observationFailures.emit(failure)
        }

        fun resetMaxActiveCollectors() {
            maxActiveCollectors = activeCollectors
        }

        override fun observeTrips(): Flow<List<TripSummary>> = MutableStateFlow(emptyList())
        override fun observeTrip(tripId: String): Flow<TripWithDays?> {
            val initialFailure = nextObservationFailure
            nextObservationFailure = null
            val completion = nextObservationCompletion
            nextObservationCompletion = null
            return channelFlow {
                if (initialFailure != null) throw initialFailure
                if (!initialTripAwaited) {
                    initialTripAwaited = true
                    initialTrip?.await()?.let { send(it) }
                }
                merge(
                    trip,
                    observationFailures.map { throw it },
                ).collect { send(it) }
            }.onStart {
                collectorStarts++
                activeCollectors++
                maxActiveCollectors = maxOf(maxActiveCollectors, activeCollectors)
            }.onCompletion { cause ->
                activeCollectors--
                completion?.complete(cause)
            }
        }
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>): DateRangeDeletionCounts {
            countCalls++
            countsBlock?.await()
            countsFailure?.let { throw it }
            return counts
        }
        override suspend fun applyDateRange(command: DateRangeApply) {
            currentCoroutineContext().job.invokeOnCompletion { applyCompletion?.complete(it) }
            applyCalls++
            lastApply = command
            applyBlock?.await()
            if (commitBeforeApplyFailure) {
                val current = requireNotNull(trip.value)
                trip.value = current.copy(
                    startDate = command.startDate,
                    days = if (command.dayCount <= current.days.size) {
                        current.days.take(command.dayCount)
                    } else {
                        current.days + List(command.dayCount - current.days.size) {
                            TripDay("added-${it + 1}", current.days.size + it)
                        }
                    },
                )
            }
            applyFailure?.let { throw it }
            if (autoEmitApply) {
                val current = requireNotNull(trip.value)
                trip.value = current.copy(
                    startDate = command.startDate,
                    days = current.days.take(command.dayCount),
                )
            }
        }
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) {
            deleteCalls++
            deleteBlock?.await()
            deleteFailure?.let { throw it }
        }
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) { renameCalls++ }
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) { travelModeCalls++ }
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteTrip(tripId: String) {
            tripDeleteCalls++
            tripDeleteBlock?.await()
            tripDeleteFailure?.let { throw it }
            if (autoEmitTripDelete) trip.value = null
        }
    }
}
