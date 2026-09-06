package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreateTripRouteTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun idleSystemBackDispatchesBackAction() {
        val callbacks = mutableListOf<String>()
        val viewModel = viewModel()
        compose.setContent {
            EasyTripTheme { CreateTripRoute({ callbacks += "back" }, {}, viewModel) }
        }

        pressBack()
        compose.waitUntil { callbacks == listOf("back") }
    }

    @Test fun idleSystemBackUsesLatestOnBackAfterRecomposition() {
        val callbacks = mutableListOf<String>()
        val viewModel = viewModel()
        var generation by mutableStateOf(1)
        compose.setContent {
            EasyTripTheme {
                val current = generation
                CreateTripRoute({ callbacks += "back-$current" }, {}, viewModel)
            }
        }
        compose.runOnIdle { generation = 2 }

        pressBack()
        compose.waitUntil { callbacks.size == 1 }
        assertEquals(listOf("back-2"), callbacks)
    }

    @Test fun submittingConsumesSystemBack() {
        val callbacks = mutableListOf<String>()
        val repository = BlockingRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            EasyTripTheme { CreateTripRoute({ callbacks += "back" }, {}, viewModel) }
        }
        compose.runOnIdle {
            viewModel.onAction(CreateTripAction.NameChanged("东京"))
            viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)))
            viewModel.onAction(CreateTripAction.Submit)
        }
        compose.waitUntil { viewModel.state.value.isSubmitting }

        pressBack()
        compose.waitForIdle()
        assertEquals(emptyList<String>(), callbacks)
    }

    @Test fun effectsUseLatestCallbacksAfterRecomposition() {
        val callbacks = mutableListOf<String>()
        val viewModel = viewModel(CompletingRepository())
        var generation by mutableStateOf(1)
        compose.setContent {
            EasyTripTheme {
                val current = generation
                CreateTripRoute(
                    onBack = { callbacks += "back-$current" },
                    onOpenWorkspace = { callbacks += "open-$current-$it" },
                    viewModel = viewModel,
                )
            }
        }
        compose.runOnIdle { generation = 2 }
        compose.runOnIdle {
            viewModel.onAction(CreateTripAction.NameChanged("东京"))
            viewModel.onAction(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)))
            viewModel.onAction(CreateTripAction.Submit)
        }
        compose.waitUntil { callbacks.size == 1 }
        assertEquals(listOf("open-2-trip"), callbacks)
    }

    private fun viewModel(repository: TripRepository = BlockingRepository()) =
        CreateTripViewModel(TripService(repository), SavedStateHandle()) { "request" }

    private class CompletingRepository : TripRepository by BlockingRepository() {
        override suspend fun createTrip(command: CreateTrip) = "trip"
    }

    private open class BlockingRepository : TripRepository {
        override fun observeTrips(): Flow<List<TripSummary>> = emptyFlow()
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String = kotlinx.coroutines.awaitCancellation()
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day-1"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }
}
