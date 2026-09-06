package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTripViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun createRequiresCompleteDateRange() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        viewModel.onAction(CreateTripAction.NameChanged("东京"))

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(0, repository.createCalls)
        assertEquals("请选择开始和结束日期", viewModel.state.value.dateError)
    }

    @Test fun rangeChangeAtomicallyDerivesInclusiveDayCountAndCreates() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        viewModel.onAction(CreateTripAction.NameChanged("东京"))
        viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)))

        assertEquals(3, viewModel.state.value.dayCount)
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(CreateTrip("东京", 3, TravelMode.FLEXIBLE, LocalDate.of(2026, 10, 1), requestId = "trip-1"), repository.commands.single())
    }

    @Test fun retryAfterFailureReusesRequestIdUntilRangeChanges() = runTest(dispatcher) {
        val ids = ArrayDeque(listOf("request-1", "request-2"))
        val repository = FakeRepository().apply { failure = IllegalStateException("failed") }
        val viewModel = CreateTripViewModel(TripService(repository), SavedStateHandle()) { ids.removeFirst() }
        enterValidRange(viewModel)

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()
        viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 4)))
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(listOf("request-1", "request-1", "request-2"), repository.commands.map { it.requestId })
    }

    @Test fun normalizedEquivalentRangeAndNameKeepRequestId() = runTest(dispatcher) {
        val ids = ArrayDeque(listOf("request-1", "request-2"))
        val repository = FakeRepository().apply { failure = IllegalStateException("failed") }
        val viewModel = CreateTripViewModel(TripService(repository), SavedStateHandle()) { ids.removeFirst() }
        enterValidRange(viewModel)
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        viewModel.onAction(CreateTripAction.NameChanged(" 东京 "))
        viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)))
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(listOf("request-1", "request-1"), repository.commands.map { it.requestId })
    }

    @Test fun savedStatePersistsCompleteRangeAndRequestId() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val first = model(FakeRepository(), saved)
        enterValidRange(first)
        first.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertNull(saved.get<String>("trip.create.startDate"))
        assertNull(saved.get<String>("trip.create.endDate"))

        val restored = model(
            FakeRepository(),
            SavedStateHandle(
                mapOf(
                    "trip.create.name" to "京都",
                    "trip.create.startDate" to "2026-11-02",
                    "trip.create.endDate" to "2026-11-05",
                    "trip.create.requestId" to "restored-request",
                ),
            ),
        )
        assertEquals(4, restored.state.value.dayCount)
        assertEquals(LocalDate.of(2026, 11, 5), restored.state.value.endDate)
        assertEquals("restored-request", restored.state.value.requestId)
    }

    @Test fun oldStartAndDayCountSavedStateMigratesToEndDate() = runTest(dispatcher) {
        val viewModel = model(
            FakeRepository(),
            SavedStateHandle(
                mapOf(
                    "trip.create.name" to "京都",
                    "trip.create.startDate" to "2026-11-02",
                    "trip.create.days" to "4",
                    "trip.create.requestId" to "restored-request",
                ),
            ),
        )

        assertEquals(LocalDate.of(2026, 11, 2), viewModel.state.value.startDate)
        assertEquals(LocalDate.of(2026, 11, 5), viewModel.state.value.endDate)
        assertEquals(4, viewModel.state.value.dayCount)
        assertEquals("restored-request", viewModel.state.value.requestId)
    }

    @Test fun incompleteOldDraftRequiresRangeAndCannotReuseRequestIdForNewCommand() = runTest(dispatcher) {
        val viewModel = model(
            FakeRepository(),
            SavedStateHandle(
                mapOf(
                    "trip.create.name" to "京都",
                    "trip.create.startDate" to "2026-11-02",
                    "trip.create.days" to "invalid",
                    "trip.create.requestId" to "old-request",
                ),
            ),
        )

        assertNull(viewModel.state.value.endDate)
        viewModel.onAction(CreateTripAction.Submit)
        assertEquals("请选择开始和结束日期", viewModel.state.value.dateError)
        viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4)))
        assertNull(viewModel.state.value.requestId)
    }

    @Test fun mutatingActionsAndBackAreIgnoredWhileSubmitting() = runTest(dispatcher) {
        val repository = FakeRepository().apply { suspendCreate = true }
        val viewModel = model(repository)
        enterValidRange(viewModel)
        viewModel.onAction(CreateTripAction.Submit)
        dispatcher.scheduler.runCurrent()
        val submitting = viewModel.state.value

        viewModel.onAction(CreateTripAction.NameChanged("大阪"))
        viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3)))
        viewModel.onAction(CreateTripAction.TravelModeChanged(TravelMode.SELF_DRIVE))
        viewModel.onAction(CreateTripAction.Back)

        assertEquals(submitting, viewModel.state.value)
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
    }

    @Test fun cancelledSubmitDoesNotBecomeFailure() = runTest(dispatcher) {
        val repository = FakeRepository().apply { suspendCreate = true }
        val store = ViewModelStore()
        val viewModel = CreateTripViewModel(TripService(repository), SavedStateHandle()) { "request-1" }
        store.put("create", viewModel)
        enterValidRange(viewModel)
        viewModel.onAction(CreateTripAction.Submit)
        dispatcher.scheduler.runCurrent()

        store.clear()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.state.value.submitError)
    }

    @Test fun successfulCreateClearsSavedStateBeforeSingleNavigationEffect() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val viewModel = model(FakeRepository(), saved)
        enterValidRange(viewModel)
        val effect = async {
            viewModel.effects.first().also {
                assertNull(saved.get<String>("trip.create.startDate"))
                assertNull(saved.get<String>("trip.create.endDate"))
            }
        }

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(CreateTripEffect.OpenWorkspace("trip-1"), effect.await())
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test fun backEmitsNavigateBackWhenIdle() = runTest(dispatcher) {
        val viewModel = model(FakeRepository())
        val effect = async { viewModel.effects.first() }
        viewModel.onAction(CreateTripAction.Back)
        advanceUntilIdle()
        assertEquals(CreateTripEffect.NavigateBack, effect.await())
    }

    private fun enterValidRange(viewModel: CreateTripViewModel) {
        viewModel.onAction(CreateTripAction.NameChanged("东京"))
        viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)))
    }

    private fun model(repository: FakeRepository, saved: SavedStateHandle = SavedStateHandle()) =
        CreateTripViewModel(TripService(repository), saved) { "trip-1" }

    private class FakeRepository : TripRepository {
        val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        val commands = mutableListOf<CreateTrip>()
        var failure: Throwable? = null
        var suspendCreate = false
        var createCalls = 0
        override fun observeTrips(): Flow<List<TripSummary>> = trips
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String {
            createCalls++
            commands += command
            if (suspendCreate) awaitCancellation()
            failure?.let { throw it }
            return command.requestId ?: "trip-1"
        }
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }
}
