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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTripViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun duplicateSubmit_callsServiceOnce() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        enterValidDraft(viewModel)

        viewModel.onCreateAction(CreateTripAction.Submit)
        viewModel.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
    }

    @Test fun createFailure_preservesInputAndShowsError() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failure = IllegalStateException("failed") }
        val viewModel = model(repository)
        enterValidDraft(viewModel)
        viewModel.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals("东京", viewModel.state.value.create.name)
        assertEquals("3", viewModel.state.value.create.dayCount)
        assertEquals("创建旅行失败，请重试", viewModel.state.value.create.submitError)
        assertFalse(viewModel.state.value.create.isSubmitting)
    }

    @Test fun createSuccess_setsStartDateAndEmitsWorkspaceOnce() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        val date = LocalDate.of(2026, 10, 1)
        viewModel.showCreate()
        viewModel.onCreateAction(CreateTripAction.NameChanged("东京"))
        viewModel.onCreateAction(CreateTripAction.DayCountChanged("3"))
        viewModel.onCreateAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED))
        viewModel.onCreateAction(CreateTripAction.StartDateChanged(date))
        val event = async { viewModel.navigation.receiveOne() }

        viewModel.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(date, repository.startDate)
        assertEquals(TripListNavigation.OpenWorkspace("trip-1"), event.await())
        assertFalse(viewModel.state.value.create.visible)
        assertNull(withTimeoutOrNull(1) { viewModel.navigation.receiveOne() })
    }

    @Test fun datedCreate_sendsStartDateInSingleCreateCommandAndEmitsWorkspaceOnce() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = model(repository)
        val date = LocalDate.of(2026, 10, 1)
        viewModel.showCreate()
        viewModel.onCreateAction(CreateTripAction.NameChanged("东京"))
        viewModel.onCreateAction(CreateTripAction.DayCountChanged("3"))
        viewModel.onCreateAction(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED))
        viewModel.onCreateAction(CreateTripAction.StartDateChanged(date))
        val event = async { viewModel.navigation.receiveOne() }

        viewModel.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(CreateTrip("东京", 3, TravelMode.FLEXIBLE, date, "trip-1"), repository.createdCommand)
        assertEquals(1, repository.createCalls)
        assertEquals(0, repository.dateSaveCalls)
        assertEquals(TripListNavigation.OpenWorkspace("trip-1"), event.await())
        assertNull(withTimeoutOrNull(1) { viewModel.navigation.receiveOne() })
    }

    @Test fun createFailure_doesNotEmitNavigationOrLeaveTrip() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failure = IllegalStateException("failed") }
        val viewModel = model(repository)
        enterValidDraft(viewModel)

        viewModel.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
        assertNull(withTimeoutOrNull(1) { viewModel.navigation.receiveOne() })
        assertEquals("创建旅行失败，请重试", viewModel.state.value.create.submitError)
    }

    @Test fun lostAcknowledgement_recoveryReusesRequestIdAndCreatesOneTrip() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val repository = FakeRepository().apply { throwAfterCommitOnce = true }
        val first = model(repository, saved)
        enterValidDraft(first)
        first.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()
        assertEquals(1, repository.committedTrips.size)
        assertEquals("创建旅行失败，请重试", first.state.value.create.submitError)

        val restored = model(repository, saved)
        val event = async { restored.navigation.receiveOne() }
        restored.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.committedTrips.size)
        assertEquals(TripListNavigation.OpenWorkspace("trip-1"), event.await())
    }

    @Test fun dismissingFailedRequestClearsRequestAndAllowsFreshSubmission() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val repository = FakeRepository().apply { failure = IllegalStateException("failed") }
        val requestIds = ArrayDeque(listOf("trip-1", "trip-2"))
        val viewModel = TripListViewModel(TripService(repository), repository, NoImpacts, saved) { requestIds.removeFirst() }
        enterValidDraft(viewModel)
        viewModel.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals("trip-1", viewModel.state.value.create.requestId)
        viewModel.onCreateAction(CreateTripAction.Dismiss)
        viewModel.showCreate()
        assertNull(viewModel.state.value.create.requestId)
        assertEquals("", viewModel.state.value.create.name)
        viewModel.onCreateAction(CreateTripAction.NameChanged("京都"))
        viewModel.onCreateAction(CreateTripAction.DayCountChanged("2"))
        repository.failure = null
        viewModel.onCreateAction(CreateTripAction.Submit)
        advanceUntilIdle()

        assertEquals("trip-2", repository.createdCommand?.requestId)
        assertEquals("京都", repository.createdCommand?.name)
    }

    @Test fun savedStateHandle_restoresDraftFieldsButNotSubmitting() = runTest(dispatcher) {
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

        assertEquals("京都", viewModel.state.value.create.name)
        assertEquals("4", viewModel.state.value.create.dayCount)
        assertEquals(CreateTimeMode.DATED, viewModel.state.value.create.timeMode)
        assertEquals(LocalDate.of(2026, 11, 2), viewModel.state.value.create.startDate)
        assertEquals(TravelMode.SELF_DRIVE, viewModel.state.value.create.travelMode)
        assertFalse(viewModel.state.value.create.isSubmitting)
    }

    private fun enterValidDraft(viewModel: TripListViewModel) {
        viewModel.showCreate()
        viewModel.onCreateAction(CreateTripAction.NameChanged("东京"))
        viewModel.onCreateAction(CreateTripAction.DayCountChanged("3"))
    }

    private fun model(repository: FakeRepository, saved: SavedStateHandle = SavedStateHandle()) =
        TripListViewModel(TripService(repository), repository, NoImpacts, saved) { "trip-1" }

    private object NoImpacts : DeleteImpactProvider {
        override suspend fun trip(tripId: String) = TripDeleteImpact(0, 0, 0, 0, 0)
        override suspend fun day(dayId: String) = DayDeleteImpact(0, 0)
    }

    private class FakeRepository : TripRepository {
        val trips = MutableStateFlow<List<TripSummary>>(emptyList())
        var failure: Throwable? = null
        var createCalls = 0
        var dateSaveCalls = 0
        var startDate: LocalDate? = null
        var createdCommand: CreateTrip? = null
        var throwAfterCommitOnce = false
        val committedTrips = linkedMapOf<String, CreateTrip>()
        override fun observeTrips(): Flow<List<TripSummary>> = trips
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String {
            createCalls++
            failure?.let { throw it }
            createdCommand = command
            startDate = command.startDate
            val id = command.requestId ?: "trip-1"
            val existing = committedTrips[id]
            if (existing == null) committedTrips[id] = command else require(existing == command)
            if (throwAfterCommitOnce) {
                throwAfterCommitOnce = false
                throw IllegalStateException("ack lost")
            }
            return id
        }
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) {
            dateSaveCalls++
            this.startDate = startDate
        }
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(dayId: String) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }
}

private suspend fun Flow<TripListNavigation>.receiveOne(): TripListNavigation = first()
