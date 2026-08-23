package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun startsLoading_thenShowsEmpty() = runTest(dispatcher) {
        val repository = TestTripRepository()
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())

        assertEquals(TripListPageState.Loading, viewModel.state.value.page)
        advanceUntilIdle()

        assertEquals(TripListPageState.Empty, viewModel.state.value.page)
    }

    @Test fun tripsMapToContent() = runTest(dispatcher) {
        val repository = TestTripRepository()
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        repository.trips.value = listOf(TripSummary("trip-1", "京都", null, TravelMode.FLEXIBLE, 3))
        advanceUntilIdle()

        val content = viewModel.state.value.page as TripListPageState.Content
        assertEquals(listOf("trip-1"), content.trips.map(TripCardUiModel::id))
    }

    @Test fun contentMapsPrimaryAndOtherTripsWithoutStaleCards() = runTest(dispatcher) {
        val repository = TestTripRepository()
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        repository.trips.value = listOf(
            TripSummary("trip-1", "京都", null, TravelMode.FLEXIBLE, 3),
            TripSummary("trip-2", "东京", null, TravelMode.SELF_DRIVE, 2),
        )
        advanceUntilIdle()

        val first = viewModel.state.value.page as TripListPageState.Content
        assertEquals("trip-1", first.primaryTrip.id)
        assertEquals(listOf("trip-2"), first.otherTrips.map(TripCardUiModel::id))

        repository.trips.value = listOf(
            TripSummary("trip-3", "杭州", null, TravelMode.FLEXIBLE, 1),
        )
        advanceUntilIdle()

        val replaced = viewModel.state.value.page as TripListPageState.Content
        assertEquals("trip-3", replaced.primaryTrip.id)
        assertEquals(emptyList<TripCardUiModel>(), replaced.otherTrips)
    }

    @Test fun deletePreviewListsAffectedAndRetainedData() = runTest(dispatcher) {
        val repository = TestTripRepository()
        val viewModel = TripListViewModel(
            TripService(repository),
            repository,
            TestImpacts(TripDeleteImpact(3, 2, 1, 4, 5)),
        )
        val trip = TripSummary("trip-1", "京都", null, TravelMode.FLEXIBLE, 3)
        repository.trips.value = listOf(trip)
        advanceUntilIdle()

        viewModel.requestDelete(trip)
        advanceUntilIdle()

        val confirmation = viewModel.state.value.deleteConfirmation!!
        assertEquals("删除京都？", confirmation.title)
        assertEquals(
            listOf("3 个旅行日", "2 个收藏地点", "1 个标签", "4 个行程项", "5 个路线段"),
            confirmation.deletedItems,
        )
        assertEquals(listOf("其他旅行及其内容"), confirmation.retainedItems)
        assertEquals(false, confirmation.reversible)
        assertEquals(true, confirmation.destructive)
    }

    @Test fun cancellingDeleteDoesNotCallRepository() = runTest(dispatcher) {
        val repository = TestTripRepository()
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        val trip = TripSummary("trip-1", "京都", null, TravelMode.FLEXIBLE, 3)

        viewModel.requestDelete(trip)
        advanceUntilIdle()
        viewModel.cancelDelete()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.deletedTrips)
        assertEquals(null, viewModel.state.value.deleteConfirmation)
    }

    @Test fun confirmingDeleteCallsRepositoryOnlyOnce() = runTest(dispatcher) {
        val repository = TestTripRepository().apply { blockDelete = CompletableDeferred() }
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        val trip = TripSummary("trip-1", "京都", null, TravelMode.FLEXIBLE, 3)

        viewModel.requestDelete(trip)
        advanceUntilIdle()
        viewModel.confirmDelete()
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals(listOf("trip-1"), repository.deletedTrips)
        repository.blockDelete!!.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun failedDeleteRestoresConfirmationAndAllowsRetry() = runTest(dispatcher) {
        val repository = TestTripRepository().apply {
            deleteFailure = IllegalStateException("disk unavailable")
        }
        val viewModel = TripListViewModel(TripService(repository), repository, TestImpacts())
        val trip = TripSummary("trip-1", "京都", null, TravelMode.FLEXIBLE, 3)

        viewModel.requestDelete(trip)
        advanceUntilIdle()
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.deleteInProgress)
        assertEquals("删除京都？", viewModel.state.value.deleteConfirmation?.title)
        assertEquals("删除失败，请重试", viewModel.state.value.deleteError)
        assertEquals(listOf("trip-1"), repository.deletedTrips)

        repository.deleteFailure = null
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals(listOf("trip-1", "trip-1"), repository.deletedTrips)
        assertEquals(null, viewModel.state.value.deleteConfirmation)
        assertEquals(null, viewModel.state.value.deleteError)
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
        repository.trips.value = listOf(TripSummary("trip-2", "东京", null, TravelMode.SELF_DRIVE, 2))
        advanceUntilIdle()

        assertEquals(listOf("trip-2"), (viewModel.state.value.page as TripListPageState.Content).trips.map(TripCardUiModel::id))
        assertEquals(1, repository.activeCollectors)
        assertEquals(1, repository.maxActiveCollectors)
    }

    private class TestImpacts(
        private val impact: TripDeleteImpact = TripDeleteImpact(0, 0, 0, 0, 0),
    ) : DeleteImpactProvider {
        override suspend fun trip(tripId: String) = impact
        override suspend fun day(dayId: String) = DayDeleteImpact(0, 0)
    }

    private class TestTripRepository : TripRepository {
        val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        var failure: Throwable? = null
        var activeCollectors = 0
        var maxActiveCollectors = 0
        val deletedTrips = mutableListOf<String>()
        var blockDelete: CompletableDeferred<Unit>? = null
        var deleteFailure: Throwable? = null

        override fun observeTrips(): Flow<List<TripSummary>> = flow {
            failure?.let { throw it }
            trips.collect { emit(it) }
        }.onStart {
            activeCollectors++
            maxActiveCollectors = maxOf(maxActiveCollectors, activeCollectors)
        }.onCompletion {
            activeCollectors--
        }
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(dayId: String) = Unit
        override suspend fun deleteTrip(tripId: String) {
            deletedTrips += tripId
            blockDelete?.await()
            deleteFailure?.let { throw it }
        }
    }
}
