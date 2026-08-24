package com.yangchengwei.easytrip.trip.ui

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTripViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun dayCountStartsEmptyAndUpdatesNaturally() = runTest(dispatcher) {
        val viewModel = model(FakeRepository())

        assertEquals("", viewModel.state.value.dayCount)
        viewModel.onAction(CreateTripAction.DayCountChanged("3"))

        assertEquals("3", viewModel.state.value.dayCount)
    }

    @Test fun savedStateWithoutDayCountRestoresEmptyValue() = runTest(dispatcher) {
        val viewModel = model(FakeRepository(), SavedStateHandle(mapOf("trip.create.name" to "京都")))

        assertEquals("", viewModel.state.value.dayCount)
    }

    @Test fun blankNameDoesNotCreateTrip() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        viewModel.onAction(CreateTripAction.DayCountChanged("3"))

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(0, repository.createCalls)
        assertEquals("请输入旅行名称", viewModel.state.value.nameError)
    }

    @Test fun repeatedSubmitWhileSavingCreatesOnlyOnce() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        enterValidDraft(viewModel)

        viewModel.onAction(CreateTripAction.Submit)
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
    }

    @Test fun failureKeepsDraftAndAllowsRetryWithSameRequestId() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failure = IllegalStateException("failed") }
        val viewModel = model(repository)
        enterValidDraft(viewModel)

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()
        assertEquals("东京", viewModel.state.value.name)
        assertEquals("3", viewModel.state.value.dayCount)
        assertEquals("创建旅行失败，请重试", viewModel.state.value.submitError)
        val failedRequestId = repository.commands.single().requestId

        repository.failure = null
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(2, repository.createCalls)
        assertEquals(failedRequestId, repository.commands.last().requestId)
    }

    @Test fun lostAcknowledgementRetryUsesSameRequestId() = runTest(dispatcher) {
        val repository = FakeRepository().apply { loseFirstAcknowledgement = true }
        val viewModel = model(repository)
        enterValidDraft(viewModel)

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(2, repository.createCalls)
        assertEquals(repository.commands.first().requestId, repository.commands.last().requestId)
        assertEquals(1, repository.persistedRequests.size)
    }

    @Test fun editingCommandFieldAfterFailureClearsRequestId() = runTest(dispatcher) {
        val ids = ArrayDeque(listOf("request-1", "request-2"))
        val repository = FakeRepository().apply { failure = IllegalStateException("failed") }
        val viewModel = CreateTripViewModel(TripService(repository), SavedStateHandle()) { ids.removeFirst() }
        enterValidDraft(viewModel)

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()
        viewModel.onAction(CreateTripAction.NameChanged("大阪"))
        repository.failure = null
        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(listOf("request-1", "request-2"), repository.commands.map { it.requestId })
        assertNotEquals(repository.commands.first().name, repository.commands.last().name)
    }

    @Test fun restoredRequestIdIsReusedForRetry() = runTest(dispatcher) {
        val saved = SavedStateHandle(
            mapOf(
                "trip.create.name" to "京都",
                "trip.create.days" to "4",
                "trip.create.requestId" to "restored-request",
            ),
        )
        val repository = FakeRepository()
        val viewModel = model(repository, saved)

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals("restored-request", repository.commands.single().requestId)
    }

    @Test fun backEmitsNavigateBackWhenIdle() = runTest(dispatcher) {
        val viewModel = model(FakeRepository())
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(CreateTripAction.Back)
        advanceUntilIdle()

        assertEquals(CreateTripEffect.NavigateBack, effect.await())
    }

    @Test fun backIsIgnoredWhileSubmitting() = runTest(dispatcher) {
        val repository = FakeRepository().apply { suspendCreate = true }
        val viewModel = model(repository)
        enterValidDraft(viewModel)

        viewModel.onAction(CreateTripAction.Submit)
        viewModel.onAction(CreateTripAction.Back)
        dispatcher.scheduler.runCurrent()

        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
    }

    @Test fun successEmitsOpenWorkspaceOnlyOnce() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        enterValidDraft(viewModel)
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(CreateTripEffect.OpenWorkspace("trip-1"), effect.await())
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
    }

    @Test fun savedStateRestoresDraftFields() = runTest(dispatcher) {
        val saved = SavedStateHandle(
            mapOf(
                "trip.create.name" to "京都",
                "trip.create.days" to "4",
                "trip.create.timeMode" to CreateTimeMode.DATED.name,
                "trip.create.startDate" to "2026-11-02",
                "trip.create.travelMode" to TravelMode.SELF_DRIVE.name,
            ),
        )

        val viewModel = model(FakeRepository(), saved)

        assertEquals("京都", viewModel.state.value.name)
        assertEquals("4", viewModel.state.value.dayCount)
        assertEquals(CreateTimeMode.DATED, viewModel.state.value.timeMode)
        assertEquals(LocalDate.of(2026, 11, 2), viewModel.state.value.startDate)
        assertEquals(TravelMode.SELF_DRIVE, viewModel.state.value.travelMode)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    private fun enterValidDraft(viewModel: CreateTripViewModel) {
        viewModel.onAction(CreateTripAction.NameChanged("东京"))
        viewModel.onAction(CreateTripAction.DayCountChanged("3"))
    }

    private fun model(repository: FakeRepository, saved: SavedStateHandle = SavedStateHandle()) =
        CreateTripViewModel(TripService(repository), saved) { "trip-1" }

    private class FakeRepository : TripRepository {
        val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        val commands = mutableListOf<CreateTrip>()
        val persistedRequests = mutableSetOf<String>()
        var failure: Throwable? = null
        var loseFirstAcknowledgement = false
        var suspendCreate = false
        var createCalls = 0
        override fun observeTrips(): Flow<List<TripSummary>> = trips
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String {
            createCalls++
            commands += command
            if (suspendCreate) awaitCancellation()
            failure?.let { throw it }
            val id = command.requestId ?: "trip-1"
            val isNew = persistedRequests.add(id)
            if (loseFirstAcknowledgement && isNew) throw IllegalStateException("lost acknowledgement")
            return id
        }
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(dayId: String) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }
}
