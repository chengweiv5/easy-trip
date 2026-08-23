package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
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

class CreateTripContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun showsFullPageTitleFieldsPlanningTipAndSubmit() {
        compose.setContent {
            EasyTripTheme { CreateTripContent(CreateTripUiState(), {}) }
        }

        compose.onNodeWithText("创建旅行", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("基本信息").assertIsDisplayed()
        compose.onNodeWithText("旅行名称").assertIsDisplayed()
        compose.onNodeWithText("旅行天数").assertIsDisplayed()
        compose.onNodeWithText("出行日期").assertIsDisplayed()
        compose.onNodeWithText("出行方式").assertIsDisplayed()
        compose.onNodeWithText("创建后先收藏感兴趣的地点，再从地点池添加到每天的行程。").assertIsDisplayed()
        compose.onNodeWithTag("create-header").assertHeightIsEqualTo(58.dp)
        compose.onNodeWithTag("create-step-1").assertHeightIsEqualTo(22.dp)
        compose.onNodeWithTag("create-name").assertHeightIsEqualTo(52.dp)
        compose.onNodeWithTag("create-date-control").assertHeightIsEqualTo(66.dp)
        compose.onNodeWithTag("create-mode-options").assertHeightIsEqualTo(82.dp)
        compose.onNodeWithTag("create-planning-tip").assertHeightIsEqualTo(40.dp)
        compose.onNodeWithTag("create-submit").assertIsDisplayed().assertHeightIsEqualTo(52.dp)
    }

    @Test fun dayCountStartsEmptyAndAcceptsNaturalSingleDigitInput() {
        var state = CreateTripUiState()
        compose.setContent {
            EasyTripTheme {
                CreateTripContent(
                    state = state,
                    onAction = { action ->
                        if (action is CreateTripAction.DayCountChanged) state = state.copy(dayCount = action.value)
                    },
                )
            }
        }

        assertEquals("", state.dayCount)
        compose.onNodeWithTag("create-day-count").performTextInput("3")
        assertEquals("3", state.dayCount)
    }

    @Test fun viewModelValidationErrorsClearOnlyWhenTheirFieldIsCorrected() {
        val repository = RecordingTripRepository()
        val viewModel = CreateTripViewModel(TripService(repository), SavedStateHandle()) { "trip-1" }
        compose.setContent {
            val state by viewModel.state.collectAsState()
            EasyTripTheme { CreateTripContent(state, viewModel::onAction, initialDateMillis = 1_799_625_600_000L) }
        }

        compose.onNodeWithTag("create-time-DATED").performClick()
        pressBack()
        compose.onNodeWithTag("create-submit").performClick()
        compose.onNodeWithText("请输入旅行名称").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("请输入至少 1 天").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("create-date-error").performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("create-name").performScrollTo().performTextInput("东京")
        compose.onNodeWithText("请输入旅行名称").assertDoesNotExist()
        compose.onNodeWithText("请输入至少 1 天").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("create-date-error").performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("create-day-count").performScrollTo().performTextInput("3")
        compose.onNodeWithText("请输入至少 1 天").assertDoesNotExist()
        compose.onNodeWithTag("create-date-error").performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("create-time-DRAFT").performClick()
        compose.onNodeWithTag("create-time-DATED").performClick()
        compose.onNodeWithTag("create-date-confirm").performClick()
        compose.onNodeWithText("请选择开始日期").assertDoesNotExist()
        compose.onNodeWithTag("create-submit").performClick()
        compose.waitUntil { repository.commands.size == 1 }
        assertEquals("东京", repository.commands.single().name)
        assertEquals(3, repository.commands.single().dayCount)
        assertEquals(LocalDate.of(2027, 1, 11), repository.commands.single().startDate)
    }

    private class RecordingTripRepository : TripRepository {
        val commands = mutableListOf<CreateTrip>()
        override fun observeTrips(): Flow<List<TripSummary>> = emptyFlow()
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = emptyFlow()
        override suspend fun createTrip(command: CreateTrip): String {
            commands += command
            return command.requestId ?: "trip-1"
        }
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day-1"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(dayId: String) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    @Test fun imeInsetsKeepSubmitActionVisibleAndClickable() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.height(500.dp).testTag("small-window")) {
                    CreateTripContent(CreateTripUiState(), {})
                }
            }
        }

        compose.onNodeWithTag("create-name").performClick().performTextInput("东京")
        compose.onNodeWithTag("create-name").assertIsFocused()
        compose.waitForIdle()
        compose.onNodeWithTag("create-submit").assertIsDisplayed().assertHasClickAction().performClick()
    }
}
