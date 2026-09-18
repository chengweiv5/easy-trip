package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun loadingEmptyContentAndErrorMapWithoutStaleTrips() = runTest(dispatcher) {
        val repository = TestTripRepository()
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())

        assertEquals(TripListPageState.Loading, viewModel.state.value.page)
        advanceUntilIdle()
        assertEquals(TripListPageState.Empty, viewModel.state.value.page)

        repository.trips.value = listOf(trip("trip-1", "京都"), trip("trip-2", "东京"))
        advanceUntilIdle()
        assertEquals(listOf("trip-1", "trip-2"), viewModel.state.value.trips.map(TripSummary::id))

        repository.failure = IllegalStateException("db unavailable")
        viewModel.onAction(TripListAction.Retry)
        assertEquals(TripListPageState.Loading, viewModel.state.value.page)
        assertEquals(emptyList<TripSummary>(), viewModel.state.value.trips)
        advanceUntilIdle()

        assertEquals(TripListPageState.Error("无法加载旅行"), viewModel.state.value.page)
        assertEquals(emptyList<TripSummary>(), viewModel.state.value.trips)
    }

    @Test fun foregroundRefreshReprojectsDatesWithoutRepeatingDelete() = runTest(dispatcher) {
        val clock = MutableClock(Instant.parse("2026-04-10T00:00:00Z"))
        val datedTrip = TripSummary(
            id = "trip-1",
            name = "京都",
            startDate = LocalDate.of(2026, 4, 11),
            travelMode = TravelMode.FLEXIBLE,
            dayCount = 3,
            placeCount = 3,
            scheduledDayCount = 2,
        )
        val repository = TestTripRepository(listOf(datedTrip))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts(), clock)
        val noNavigation = CompletableDeferred<Unit>()
        val navigationObserver = launch {
            viewModel.navigation.first()
            noNavigation.complete(Unit)
        }
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        val deletionBeforeRefresh = viewModel.state.value.deletion

        clock.instant = Instant.parse("2026-04-11T00:00:00Z")
        viewModel.refreshDateDerivedState()

        val card = (viewModel.state.value.page as TripListPageState.Content).primaryTrip
        assertEquals("旅行中", card.countdownLabel)
        assertEquals(deletionBeforeRefresh, viewModel.state.value.deletion)
        assertEquals(emptyList<String>(), repository.deletedTrips)
        assertEquals(false, noNavigation.isCompleted)
        navigationObserver.cancel()
    }

    @Test fun foregroundRefreshReselectsPrimaryTripAfterCrossingIntoANewDayWithoutRoomEmission() = runTest(dispatcher) {
        val clock = MutableClock(Instant.parse("2026-04-10T00:00:00Z"))
        val endingToday = TripSummary(
            id = "ending-today",
            name = "Ending today",
            startDate = LocalDate.of(2026, 4, 9),
            travelMode = TravelMode.FLEXIBLE,
            dayCount = 2,
            placeCount = 0,
            scheduledDayCount = 0,
        )
        val startingTomorrow = TripSummary(
            id = "starting-tomorrow",
            name = "Starting tomorrow",
            startDate = LocalDate.of(2026, 4, 11),
            travelMode = TravelMode.FLEXIBLE,
            dayCount = 2,
            placeCount = 0,
            scheduledDayCount = 0,
        )
        val repository = TestTripRepository(listOf(endingToday, startingTomorrow))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts(), clock)
        advanceUntilIdle()
        assertEquals("ending-today", (viewModel.state.value.page as TripListPageState.Content).primaryTrip.id)

        clock.instant = Instant.parse("2026-04-11T00:00:00Z")
        viewModel.refreshDateDerivedState()

        assertEquals("starting-tomorrow", (viewModel.state.value.page as TripListPageState.Content).primaryTrip.id)
        assertEquals(listOf("ending-today", "starting-tomorrow"), repository.trips.value.map(TripSummary::id))
    }

    @Test fun newDeleteTargetIgnoresOldImpactCompletion() = runTest(dispatcher) {
        val oldImpact = CompletableDeferred<TripDeleteImpact>()
        val newImpact = CompletableDeferred<TripDeleteImpact>()
        val impacts = TestImpacts { tripId ->
            withContext(NonCancellable) {
                if (tripId == "trip-a") oldImpact.await() else newImpact.await()
            }
        }
        val repository = TestTripRepository(listOf(trip("trip-a", "京都"), trip("trip-b", "东京")))
        val viewModel = TripListViewModel(TripService(repository), repository, impacts)
        advanceUntilIdle()

        viewModel.onAction(TripListAction.RequestDelete("trip-a"))
        runCurrent()
        viewModel.onAction(TripListAction.RequestDelete("trip-b"))
        runCurrent()
        newImpact.complete(TripDeleteImpact(2, 0, 0, 0, 0))
        advanceUntilIdle()
        oldImpact.complete(TripDeleteImpact(9, 0, 0, 0, 0))
        advanceUntilIdle()

        val deletion = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals("trip-b", deletion.tripId)
        assertEquals("删除东京？", deletion.confirmation.title)
        assertEquals("2 个旅行日", deletion.confirmation.deletedItems.first())
    }

    @Test fun impactFailureKeepsTargetAndCanRetry() = runTest(dispatcher) {
        var attempts = 0
        val impacts = TestImpacts {
            attempts++
            if (attempts == 1) throw IllegalStateException("impact unavailable")
            TripDeleteImpact(3, 0, 0, 0, 0)
        }
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(TripService(repository), repository, impacts)
        advanceUntilIdle()

        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        assertEquals(
            TripDeletionUiState.ImpactFailure("trip-1", "京都", "无法加载删除影响，请重试"),
            viewModel.state.value.deletion,
        )

        viewModel.onAction(TripListAction.RetryDeleteImpact)
        advanceUntilIdle()

        val ready = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals("trip-1", ready.tripId)
        assertEquals("3 个旅行日", ready.confirmation.deletedItems.first())
        assertEquals(2, attempts)
    }

    @Test fun cancelWhileImpactLoadingInvalidatesCompletion() = runTest(dispatcher) {
        val impact = CompletableDeferred<TripDeleteImpact>()
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(
            TripService(repository),
            repository,
            TestImpacts { withContext(NonCancellable) { impact.await() } },
        )
        advanceUntilIdle()

        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        runCurrent()
        assertEquals(TripDeletionUiState.LoadingImpact("trip-1", "京都"), viewModel.state.value.deletion)
        viewModel.onAction(TripListAction.CancelDelete)
        impact.complete(TripDeleteImpact(1, 0, 0, 0, 0))
        advanceUntilIdle()

        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
    }

    @Test fun repeatedConfirmDeletesExactlyOnce() = runTest(dispatcher) {
        val deleteGate = CompletableDeferred<Unit>()
        val repository = TestTripRepository(listOf(trip("trip-1", "京都"))).apply {
            deleteBehavior = { deleteGate.await() }
        }
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()

        viewModel.onAction(TripListAction.ConfirmDelete)
        viewModel.onAction(TripListAction.ConfirmDelete)
        runCurrent()

        assertEquals(listOf("trip-1"), repository.deletedTrips)
        val deleting = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals(true, deleting.isDeleting)

        deleteGate.complete(Unit)
        advanceUntilIdle()
        val awaitingFlow = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals(true, awaitingFlow.isDeleting)
        assertEquals(listOf("trip-1"), (viewModel.state.value.page as TripListPageState.Content).trips.map(TripCardUiModel::id))

        repository.trips.value = emptyList()
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
        assertEquals(TripListPageState.Empty, viewModel.state.value.page)
    }

    @Test fun targetMissingEmissionDuringServiceCallClosesWhenServiceReturns() = runTest(dispatcher) {
        val deleteGate = CompletableDeferred<Unit>()
        val repository = TestTripRepository(listOf(trip("trip-1", "京都"))).apply {
            deleteBehavior = { deleteGate.await() }
        }
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        runCurrent()

        repository.trips.value = emptyList()
        advanceUntilIdle()
        assertEquals(true, (viewModel.state.value.deletion as TripDeletionUiState.Ready).isDeleting)

        deleteGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
    }

    @Test fun unrelatedAndStaleFlowEmissionsDoNotCloseSuccessfulDeletion() = runTest(dispatcher) {
        val repository = TestTripRepository(listOf(trip("trip-a", "京都"), trip("trip-b", "东京")))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-a"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()

        repository.trips.value = listOf(trip("trip-a", "旧京都"))
        advanceUntilIdle()
        assertEquals("trip-a", (viewModel.state.value.deletion as TripDeletionUiState.Ready).tripId)

        repository.trips.value = listOf(trip("trip-b", "东京"))
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
    }

    @Test fun loadingAndErrorBeforeFirstEmissionDoNotConfirmSuccessfulDeletion() = runTest(dispatcher) {
        val deleteGate = CompletableDeferred<Unit>()
        val firstEmissionGate = CompletableDeferred<Unit>()
        val repository = TestTripRepository(listOf(trip("trip-1", "京都"))).apply {
            deleteBehavior = { deleteGate.await() }
        }
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        runCurrent()

        repository.firstEmissionGate = firstEmissionGate
        repository.failure = IllegalStateException("db unavailable")
        viewModel.onAction(TripListAction.Retry)
        runCurrent()
        assertEquals(TripListPageState.Loading, viewModel.state.value.page)
        assertEquals(emptyList<TripSummary>(), viewModel.state.value.trips)

        deleteGate.complete(Unit)
        runCurrent()
        val awaitingWhileLoading = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals("trip-1", awaitingWhileLoading.tripId)
        assertEquals(true, awaitingWhileLoading.isDeleting)

        firstEmissionGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(TripListPageState.Error("无法加载旅行"), viewModel.state.value.page)
        val failedSync = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals(false, failedSync.isDeleting)
        assertEquals("删除成功，但同步确认失败，请重新同步", failedSync.errorMessage)

        repository.failure = null
        repository.trips.value = emptyList()
        viewModel.onAction(TripListAction.RetryDeletionSync)
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
    }

    @Test fun collectorErrorWhileAwaitingDeletionRetriesOnceAndRecovers() = runTest(dispatcher) {
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()

        repository.failuresRemaining = 1
        repository.trips.value = emptyList()
        viewModel.onAction(TripListAction.Retry)
        advanceUntilIdle()

        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
        assertEquals(TripListPageState.Empty, viewModel.state.value.page)
        assertEquals(1, repository.deletedTrips.size)
        assertEquals(3, repository.collectorStarts)
    }

    @Test fun collectorExhaustedBeforeServiceReturnsBecomesRecoverableAfterServiceSuccess() = runTest(dispatcher) {
        val deleteGate = CompletableDeferred<Unit>()
        val repository = TestTripRepository(listOf(trip("trip-1", "京都"))).apply {
            deleteBehavior = { deleteGate.await() }
        }
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        runCurrent()

        repository.failuresRemaining = 2
        viewModel.onAction(TripListAction.Retry)
        advanceUntilIdle()
        assertEquals(true, (viewModel.state.value.deletion as TripDeletionUiState.Ready).isDeleting)

        deleteGate.complete(Unit)
        advanceUntilIdle()

        val failedSync = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals(false, failedSync.isDeleting)
        assertEquals("删除成功，但同步确认失败，请重新同步", failedSync.errorMessage)
        assertEquals(listOf("trip-1"), repository.deletedTrips)

        repository.trips.value = emptyList()
        viewModel.onAction(TripListAction.RetryDeletionSync)
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
        assertEquals(listOf("trip-1"), repository.deletedTrips)
    }

    @Test fun exhaustedDeletionConfirmationRetryBecomesRecoverableWithoutDeletingAgain() = runTest(dispatcher) {
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()

        repository.failuresRemaining = 2
        viewModel.onAction(TripListAction.Retry)
        advanceUntilIdle()

        val failedSync = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals(false, failedSync.isDeleting)
        assertEquals("删除成功，但同步确认失败，请重新同步", failedSync.errorMessage)
        assertEquals(listOf("trip-1"), repository.deletedTrips)

        repository.trips.value = emptyList()
        viewModel.onAction(TripListAction.RetryDeletionSync)
        advanceUntilIdle()

        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
        assertEquals(listOf("trip-1"), repository.deletedTrips)
    }

    @Test fun staleConfirmActionDuringSyncFailureCannotDeleteAgain() = runTest(dispatcher) {
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()
        repository.failuresRemaining = 2
        viewModel.onAction(TripListAction.Retry)
        advanceUntilIdle()
        val failedSync = viewModel.state.value.deletion

        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(failedSync, viewModel.state.value.deletion)
        assertEquals(listOf("trip-1"), repository.deletedTrips)

        repository.trips.value = emptyList()
        viewModel.onAction(TripListAction.RetryDeletionSync)
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
        assertEquals(listOf("trip-1"), repository.deletedTrips)
    }

    @Test fun repeatedManualResyncWhileBusyStartsOneCollectorAndCanRetryAfterFailure() = runTest(dispatcher) {
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()
        repository.failuresRemaining = 2
        viewModel.onAction(TripListAction.Retry)
        advanceUntilIdle()

        val resyncGate = CompletableDeferred<Unit>()
        repository.firstEmissionGate = resyncGate
        val startsBeforeResync = repository.collectorStarts
        viewModel.onAction(TripListAction.RetryDeletionSync)
        viewModel.onAction(TripListAction.RetryDeletionSync)
        runCurrent()

        assertEquals(startsBeforeResync + 1, repository.collectorStarts)
        assertEquals(true, (viewModel.state.value.deletion as TripDeletionUiState.Ready).isDeleting)
        resyncGate.complete(Unit)
        repository.firstEmissionGate = null
        repository.failuresRemaining = 2
        advanceUntilIdle()
        assertEquals(false, (viewModel.state.value.deletion as TripDeletionUiState.Ready).isDeleting)

        repository.trips.value = emptyList()
        repository.failuresRemaining = 0
        viewModel.onAction(TripListAction.RetryDeletionSync)
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
        assertEquals(listOf("trip-1"), repository.deletedTrips)
    }

    @Test fun successfulEmissionBeforeDeleteDoesNotConfirmLaterDeletion() = runTest(dispatcher) {
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()

        repository.trips.value = emptyList()
        advanceUntilIdle()
        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()

        val awaitingNewEmission = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertEquals(true, awaitingNewEmission.isDeleting)

        repository.trips.value = listOf(trip("trip-2", "东京"))
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
    }

    @Test fun deleteFailureKeepsExactImpactAndRetries() = runTest(dispatcher) {
        val impact = TripDeleteImpact(3, 2, 1, 4, 5)
        var failDelete = true
        val repository = TestTripRepository(listOf(trip("trip-1", "京都"))).apply {
            deleteBehavior = { if (failDelete) throw IllegalStateException("disk unavailable") }
        }
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts { impact })
        advanceUntilIdle()
        viewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        val before = viewModel.state.value.deletion as TripDeletionUiState.Ready

        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()

        val failed = viewModel.state.value.deletion as TripDeletionUiState.Ready
        assertSame(before.confirmation, failed.confirmation)
        assertEquals(false, failed.isDeleting)
        assertEquals("删除失败，请重试", failed.errorMessage)

        failDelete = false
        viewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()
        assertEquals(listOf("trip-1", "trip-1"), repository.deletedTrips)
        assertEquals(true, (viewModel.state.value.deletion as TripDeletionUiState.Ready).isDeleting)
        repository.trips.value = emptyList()
        advanceUntilIdle()
        assertEquals(TripDeletionUiState.Idle, viewModel.state.value.deletion)
    }

    @Test fun cancellationCompletesImpactAndDeleteJobsWithCancellationCause() = runTest(dispatcher) {
        val impactCancellation = CancellationException("impact cancelled")
        val impactCompletion = CompletableDeferred<Throwable?>()
        val impactRepository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val impactViewModel = TripListViewModel(
            TripService(impactRepository),
            impactRepository,
            TestImpacts {
                currentCoroutineContext().job.invokeOnCompletion { impactCompletion.complete(it) }
                throw impactCancellation
            },
        )
        advanceUntilIdle()
        impactViewModel.onAction(TripListAction.RequestDelete("trip-1"))
        advanceUntilIdle()
        assertSame(impactCancellation, impactCompletion.await())

        val deleteCancellation = CancellationException("delete cancelled")
        val deleteCompletion = CompletableDeferred<Throwable?>()
        val deleteRepository = TestTripRepository(listOf(trip("trip-2", "东京"))).apply {
            deleteBehavior = {
                currentCoroutineContext().job.invokeOnCompletion { deleteCompletion.complete(it) }
                throw deleteCancellation
            }
        }
        val deleteViewModel = TripListViewModel(TripService(deleteRepository), deleteRepository, TestImpacts())
        advanceUntilIdle()
        deleteViewModel.onAction(TripListAction.RequestDelete("trip-2"))
        advanceUntilIdle()
        deleteViewModel.onAction(TripListAction.ConfirmDelete)
        advanceUntilIdle()
        assertSame(deleteCancellation, deleteCompletion.await())
    }

    @Test fun staleCollectorFailureCannotOverwriteNewCollectorResult() = runTest(dispatcher) {
        val repository = TestTripRepository(listOf(trip("trip-1", "京都")))
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()

        val staleFailureGate = CompletableDeferred<Unit>()
        repository.nextNonCancellableFailureGate = staleFailureGate
        viewModel.onAction(TripListAction.Retry)
        runCurrent()
        viewModel.onAction(TripListAction.Retry)
        repository.trips.value = listOf(trip("trip-2", "东京"))
        advanceUntilIdle()

        staleFailureGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("trip-2"), (viewModel.state.value.page as TripListPageState.Content).trips.map(TripCardUiModel::id))
        assertEquals(listOf("trip-2"), viewModel.state.value.trips.map(TripSummary::id))
    }

    @Test fun errorRetryRecoversAndKeepsSingleCollector() = runTest(dispatcher) {
        val repository = TestTripRepository()
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        advanceUntilIdle()
        assertEquals(1, repository.activeCollectors)

        repository.failure = RuntimeException("db unavailable")
        viewModel.onAction(TripListAction.Retry)
        advanceUntilIdle()
        assertEquals("无法加载旅行", (viewModel.state.value.page as TripListPageState.Error).message)
        assertEquals(0, repository.activeCollectors)

        repository.failure = null
        viewModel.onAction(TripListAction.Retry)
        viewModel.onAction(TripListAction.Retry)
        repository.trips.value = listOf(trip("trip-2", "东京"))
        advanceUntilIdle()

        assertEquals(listOf("trip-2"), (viewModel.state.value.page as TripListPageState.Content).trips.map(TripCardUiModel::id))
        assertEquals(1, repository.activeCollectors)
        assertEquals(1, repository.maxActiveCollectors)
    }

    private fun trip(id: String, name: String) = TripSummary(id, name, null, TravelMode.FLEXIBLE, 3, 0, 0)

    private class MutableClock(initialInstant: Instant) : Clock() {
        var instant = initialInstant
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = instant
    }

    private class TestImpacts(
        private val behavior: suspend (String) -> TripDeleteImpact = { TripDeleteImpact(0, 0, 0, 0, 0) },
    ) : DeleteImpactProvider {
        override suspend fun trip(tripId: String) = behavior(tripId)
        override suspend fun day(dayId: String) = DayDeleteImpact(0, 0, 0)
    }

    private class TestTripRepository(initialTrips: List<TripSummary> = emptyList()) : TripRepository {
        val trips = MutableStateFlow(initialTrips)
        var failure: Throwable? = null
        var activeCollectors = 0
        var maxActiveCollectors = 0
        var collectorStarts = 0
        var failuresRemaining = 0
        val deletedTrips = mutableListOf<String>()
        var deleteBehavior: suspend () -> Unit = {}
        var firstEmissionGate: CompletableDeferred<Unit>? = null
        var nextNonCancellableFailureGate: CompletableDeferred<Unit>? = null

        override fun observeTrips(): Flow<List<TripSummary>> {
            val nonCancellableFailureGate = nextNonCancellableFailureGate
            nextNonCancellableFailureGate = null
            return flow {
            if (nonCancellableFailureGate != null) {
                withContext(NonCancellable) { nonCancellableFailureGate.await() }
                throw IllegalStateException("stale db unavailable")
            }
            firstEmissionGate?.await()
            if (failuresRemaining > 0) {
                failuresRemaining--
                throw IllegalStateException("db unavailable")
            }
            failure?.let { throw it }
            trips.collect { emit(it) }
        }.onStart {
            collectorStarts++
            activeCollectors++
            maxActiveCollectors = maxOf(maxActiveCollectors, activeCollectors)
        }.onCompletion {
            activeCollectors--
        }
        }
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) {
            deletedTrips += tripId
            deleteBehavior()
        }
    }
}
