package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.*
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class TripSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun endBeforeStartDoesNotSubmit() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateDraft(LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-01"))
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

        model.updateDateDraft(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01"))
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

    @Test fun confirmationIsSingleFlightAndFailureKeepsRetry() = runTest(dispatcher) {
        val repository = FakeRepository().apply { applyBlock = CompletableDeferred(); applyFailure = IllegalStateException() }
        val model = model(repository)
        advanceUntilIdle()
        model.updateDateDraft(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()

        model.confirmDateRangeChange()
        model.confirmDateRangeChange()
        advanceUntilIdle()
        assertEquals(1, repository.applyCalls)
        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("保存失败，请重新计算影响", model.state.value.dateRange.error)
        assertEquals(true, model.state.value.dateRange.confirmation != null)

        repository.applyFailure = null
        repository.applyBlock = null
        model.confirmDateRangeChange()
        advanceUntilIdle()
        assertEquals(2, repository.applyCalls)
        assertEquals(null, model.state.value.dateRange.confirmation)
    }

    @Test fun nonDestructiveApplyIsSingleFlight() = runTest(dispatcher) {
        val repository = FakeRepository().apply { applyBlock = CompletableDeferred() }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateDraft(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"))
        model.requestDateRangeChange()
        model.requestDateRangeChange()
        advanceUntilIdle()

        assertEquals(1, repository.applyCalls)
        assertEquals(true, model.state.value.dateRange.submitting)
        repository.applyBlock!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, model.state.value.dateRange.submitting)
    }

    @Test fun stalePreviewCannotOverwriteNewerDraft() = runTest(dispatcher) {
        val repository = FakeRepository().apply { countsBlock = CompletableDeferred() }
        val model = model(repository)
        advanceUntilIdle()

        model.updateDateDraft(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01"))
        model.requestDateRangeChange()
        advanceUntilIdle()
        model.updateDateDraft(LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-03"))
        repository.countsBlock!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(LocalDate.parse("2026-12-01"), model.state.value.dateRange.startDate)
        assertEquals(null, model.state.value.dateRange.confirmation)
        assertEquals(0, repository.applyCalls)
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

    @Test fun failedDayDeleteKeepsRedConfirmationForRetry() = runTest(dispatcher) {
        val repository = FakeRepository().apply { deleteFailure = IllegalStateException() }
        val model = model(repository)
        advanceUntilIdle()
        model.requestDelete(model.state.value.days.last())
        advanceUntilIdle()

        model.confirmDelete()
        advanceUntilIdle()
        assertEquals("删除失败，请重试", model.state.value.dayDeleteError)
        assertEquals("day-3", model.state.value.pendingDayDeletion?.day?.id)

        repository.deleteFailure = null
        model.confirmDelete()
        advanceUntilIdle()
        assertEquals(null, model.state.value.pendingDayDeletion)
        assertEquals(2, repository.deleteCalls)
    }

    private fun model(repository: FakeRepository) = TripSettingsViewModel(
        SavedStateHandle(mapOf("tripId" to "trip")),
        TripService(repository),
        repository,
        object : DeleteImpactProvider {
            override suspend fun trip(tripId: String) = TripDeleteImpact(0, 0, 0, 0, 0)
            override suspend fun day(dayId: String) = DayDeleteImpact(1, 1)
        },
        TripDateRangeService(repository),
    )

    private class FakeRepository(
        private val counts: DateRangeDeletionCounts = DateRangeDeletionCounts(0, 0, 0),
        dayCount: Int = 3,
    ) : TripRepository {
        private val trip = MutableStateFlow<TripWithDays?>(TripWithDays(
            "trip", "Trip", LocalDate.parse("2026-10-01"), TravelMode.FLEXIBLE,
            List(dayCount) { TripDay("day-${it + 1}", it) },
        ))
        var applyCalls = 0
        var countsBlock: CompletableDeferred<Unit>? = null
        var applyBlock: CompletableDeferred<Unit>? = null
        var applyFailure: Throwable? = null
        var deleteCalls = 0
        var deleteFailure: Throwable? = null
        override fun observeTrips(): Flow<List<TripSummary>> = MutableStateFlow(emptyList())
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = trip
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>): DateRangeDeletionCounts {
            countsBlock?.await()
            return counts
        }
        override suspend fun applyDateRange(command: DateRangeApply) { applyCalls++; applyBlock?.await(); applyFailure?.let { throw it } }
        override suspend fun deleteDay(dayId: String) { deleteCalls++; deleteFailure?.let { throw it } }
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }
}
