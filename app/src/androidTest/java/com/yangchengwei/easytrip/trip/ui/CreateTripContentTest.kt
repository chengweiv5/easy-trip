package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso.pressBack
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

class CreateTripContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun formHasRangeControlWithoutLegacyDayCountOrTimeModeControls() {
        compose.setContent { EasyTripTheme { CreateTripContent(CreateTripUiState(), {}) } }

        compose.onNodeWithTag("create-submit").assertIsDisplayed().assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("create-name").assertHeightIsEqualTo(52.dp)
        compose.onNodeWithTag("create-date-control").assertHeightIsEqualTo(66.dp).assertHasClickAction()
        org.junit.Assert.assertTrue(compose.onAllNodesWithText("旅行天数").fetchSemanticsNodes().isEmpty())
        org.junit.Assert.assertTrue(compose.onAllNodesWithTag("create-time-DRAFT").fetchSemanticsNodes().isEmpty())
        org.junit.Assert.assertTrue(compose.onAllNodesWithTag("create-time-DATED").fetchSemanticsNodes().isEmpty())
    }

    @Test fun rangeSheetConfirmsAtomicallyAndCancelDoesNotEmitAction() {
        val actions = mutableListOf<CreateTripAction>()
        compose.setContent { EasyTripTheme { CreateTripContent(
                    CreateTripUiState(name = "东京"),
                    actions::add,
                    initialDateMillis = LocalDate.parse("2026-09-01").atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                ) } }

        compose.onNodeWithTag("create-date-control").performClick()
        compose.onNodeWithTag("trip-date-2026-09-01").performClick()
        compose.onNodeWithTag("trip-date-2026-09-03").performClick()
        compose.onNodeWithTag("trip-date-range-confirm").performClick()

        assertEquals(
            listOf(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3))),
            actions,
        )
        compose.onNodeWithTag("create-date-control").performClick()
        compose.onNodeWithTag("trip-date-range-cancel").performClick()
        assertEquals(1, actions.size)
    }

    @Test fun rangeSheetIsModalAndBackgroundHasNoSemantics() {
        compose.setContent { EasyTripTheme { CreateTripContent(CreateTripUiState(), {}) } }

        compose.onNodeWithTag("create-date-control").performClick()
        compose.onNodeWithTag("trip-date-range-sheet").assertIsDisplayed()
        compose.onNodeWithTag("create-submit").assertDoesNotExist()
    }

    @Test fun dateRangeDescriptionShowsInclusiveEndAndDerivedDays() {
        compose.setContent {
            EasyTripTheme {
                CreateTripContent(
                    CreateTripUiState(
                        startDate = LocalDate.of(2027, 1, 11),
                        endDate = LocalDate.of(2027, 1, 13),
                        dateError = "请选择开始和结束日期",
                    ),
                    {},
                )
            }
        }

        compose.onNodeWithContentDescription("选择出行日期，当前范围：2027-01-11 至 2027-01-13")
            .assertHasClickAction()
            .assert(androidx.compose.ui.test.SemanticsMatcher.expectValue(SemanticsProperties.Error, "请选择开始和结束日期"))
        compose.onNodeWithText("至 2027-01-13 · 3天2晚").assertIsDisplayed()
    }

    @Test fun validationShowsRangeErrorThenSuccessfulRangeCreatesTrip() {
        val repository = RecordingTripRepository()
        val viewModel = CreateTripViewModel(TripService(repository), SavedStateHandle()) { "trip-1" }
        compose.setContent {
            val state by viewModel.state.collectAsState()
            EasyTripTheme {
                CreateTripContent(
                    state,
                    viewModel::onAction,
                    initialDateMillis = LocalDate.parse("2026-09-01").atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                )
            }
        }

        compose.onNodeWithTag("create-name").performTextInput("东京")
        compose.onNodeWithTag("create-submit").performClick()
        compose.onNodeWithTag("create-date-error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("create-date-control").performClick()
        compose.onNodeWithTag("trip-date-2026-09-01").performClick()
        compose.onNodeWithTag("trip-date-2026-09-03").performClick()
        compose.onNodeWithTag("trip-date-range-confirm").performClick()
        compose.onNodeWithTag("create-submit").performClick()
        compose.waitUntil { repository.commands.size == 1 }
        assertEquals(3, repository.commands.single().dayCount)
        assertEquals(LocalDate.of(2026, 9, 1), repository.commands.single().startDate)
    }

    @Test fun submittingLocksRangeAndSubmit() {
        compose.setContent { EasyTripTheme { CreateTripContent(CreateTripUiState(isSubmitting = true), {}) } }
        listOf("create-back", "create-name", "create-date-control", "create-mode-FLEXIBLE", "create-mode-SELF_DRIVE", "create-submit")
            .forEach { compose.onNodeWithTag(it).assertIsNotEnabled() }
    }

    @Test fun rangeControlAndSubmitRemainReachableAtLargeFont() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 2f)) {
                EasyTripTheme { Box(Modifier.width(280.dp).height(500.dp)) { CreateTripContent(CreateTripUiState(), {}) } }
            }
        }
        compose.onNodeWithTag("create-date-control").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("create-submit").performScrollTo().assertIsDisplayed()
    }

    private class RecordingTripRepository : TripRepository {
        val commands = mutableListOf<CreateTrip>()
        override fun observeTrips(): Flow<List<TripSummary>> = emptyFlow()
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String { commands += command; return "trip-1" }
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: com.yangchengwei.easytrip.core.model.TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day-1"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }
}
