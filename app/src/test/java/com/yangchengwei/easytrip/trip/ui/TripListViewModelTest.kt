package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
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

    private class TestImpacts : DeleteImpactProvider {
        override suspend fun trip(tripId: String) = TripDeleteImpact(0, 0, 0, 0, 0)
        override suspend fun day(dayId: String) = DayDeleteImpact(0, 0)
    }

    private class TestTripRepository : TripRepository {
        val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        var failure: Throwable? = null
        var activeCollectors = 0
        var maxActiveCollectors = 0

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
        override suspend fun deleteTrip(tripId: String) = Unit
    }
}
