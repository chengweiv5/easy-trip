package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimary
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimaryDark
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSecondary
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurface
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
        compose.onNodeWithTag("create-planning-tip").assertIsDisplayed()
        compose.onNodeWithTag("create-submit").assertIsDisplayed().assertHeightIsEqualTo(48.dp)
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
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day-1"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    @Test fun usesSpecifiedFortyFourAndFortyEightDpActions() {
        compose.setContent { EasyTripTheme { CreateTripContent(CreateTripUiState(), {}) } }

        compose.onNodeWithTag("create-back").assertHeightIsEqualTo(44.dp)
        compose.onNodeWithTag("create-submit").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("create-name").assertHeightIsEqualTo(52.dp)
        compose.onNodeWithTag("create-date-control").assertHeightIsEqualTo(66.dp)
    }

    @Test fun fieldsFollowNameDateDaysAndModeOrder() {
        compose.setContent { EasyTripTheme { CreateTripContent(CreateTripUiState(), {}) } }

        val nameTop = compose.onNodeWithTag("create-name-container").fetchSemanticsNode().boundsInRoot.top
        val dateTop = compose.onNodeWithTag("create-date-control-container").fetchSemanticsNode().boundsInRoot.top
        val daysTop = compose.onNodeWithTag("create-day-count-container").fetchSemanticsNode().boundsInRoot.top
        val modeTop = compose.onNodeWithTag("create-mode-options").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(nameTop < dateTop)
        org.junit.Assert.assertTrue(dateTop < daysTop)
        org.junit.Assert.assertTrue(daysTop < modeTop)
    }

    @Test fun datedFormShowsStartAndInclusiveEndDate() {
        compose.setContent {
            EasyTripTheme {
                CreateTripContent(
                    CreateTripUiState(
                        dayCount = "3",
                        timeMode = CreateTimeMode.DATED,
                        startDate = LocalDate.of(2027, 1, 11),
                    ),
                    {},
                )
            }
        }

        compose.onNodeWithText("2027-01-11").assertIsDisplayed()
        compose.onNodeWithText("2027-01-13").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun easyTripDatePickerRendersForestSageContainerAndSelectedDay() {
        compose.setContent {
            EasyTripTheme {
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Magenta)) {
                    Box(Modifier.width(560.dp).height(560.dp)) {
                        EasyTripDatePicker(
                            state = rememberDatePickerState(initialSelectedDateMillis = 1_799_625_600_000L),
                            modifier = Modifier.testTag("picker-under-test"),
                        )
                    }
                }
            }
        }

        val pickerImage = compose.onNodeWithTag("picker-under-test").captureToImage().toPixelMap()
        assertEquals(EasyTripSurface, pickerImage[4, 4])

        val selectedDayImage = compose.onNode(isSelected(), useUnmergedTree = true)
            .captureToImage()
            .toPixelMap()
        assertEquals(
            EasyTripPrimary,
            selectedDayImage[selectedDayImage.width / 4, selectedDayImage.height / 2],
        )
    }

    @Test fun datePickerUsesForestSageThemeAndKeepsDraftOnDismiss() {
        val actions = mutableListOf<CreateTripAction>()
        compose.setContent {
            EasyTripTheme {
                CreateTripContent(
                    CreateTripUiState(timeMode = CreateTimeMode.DATED, startDate = LocalDate.of(2027, 1, 11)),
                    actions::add,
                )
            }
        }

        compose.onNodeWithTag("create-time-DATED").performClick()
        compose.onNodeWithTag("create-date-picker").assertIsDisplayed()
        assertEquals(
            CreateTripDatePickerPalette(
                container = EasyTripSurface,
                content = EasyTripPrimaryDark,
                secondaryContent = EasyTripSecondary,
                primary = EasyTripPrimary,
                onPrimary = EasyTripSurface,
                outline = EasyTripBorder,
            ),
            createTripDatePickerPalette(),
        )
        pressBack()
        compose.onNodeWithTag("create-date-picker").assertDoesNotExist()
        assertEquals(listOf(CreateTripAction.TimeModeChanged(CreateTimeMode.DATED)), actions)
        compose.onNodeWithText("2027-01-11").assertIsDisplayed()
    }

    @Test fun validationErrorsHaveNearbyTextAndErrorSemantics() {
        compose.setContent {
            EasyTripTheme {
                CreateTripContent(
                    CreateTripUiState(
                        nameError = "请输入旅行名称",
                        dayCountError = "请输入至少 1 天",
                        dateError = "请选择开始日期",
                    ),
                    {},
                )
            }
        }

        listOf(
            "create-name" to "请输入旅行名称",
            "create-day-count" to "请输入至少 1 天",
            "create-date-control" to "请选择开始日期",
        ).forEach { (tag, message) ->
            compose.onNodeWithTag(tag).assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(SemanticsProperties.Error, message),
            )
            compose.onNodeWithText(message).assert(hasAnyAncestor(hasTestTag("$tag-container")))
        }
    }

    @Test fun submittingLocksBackFieldsPickerModesAndSubmit() {
        compose.setContent {
            EasyTripTheme { CreateTripContent(CreateTripUiState(isSubmitting = true), {}) }
        }

        listOf(
            "create-back",
            "create-name",
            "create-day-count",
            "create-time-DRAFT",
            "create-time-DATED",
            "create-mode-FLEXIBLE",
            "create-mode-SELF_DRIVE",
            "create-submit",
        ).forEach { compose.onNodeWithTag(it).assertIsNotEnabled() }
        compose.onNodeWithTag("create-time-DATED").performClick()
        compose.onNodeWithTag("create-date-picker").assertDoesNotExist()
    }

    @Test fun systemBackIsConsumedWhileSubmitting() {
        compose.setContent {
            EasyTripTheme { CreateTripContent(CreateTripUiState(isSubmitting = true), {}) }
        }

        pressBack()
        compose.onNodeWithText("创建旅行", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun narrowLargeFontAndImeKeepFocusedFieldAndSubmitReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 2f)) {
                EasyTripTheme {
                    Box(Modifier.width(280.dp).height(500.dp).testTag("small-window")) {
                        CreateTripContent(CreateTripUiState(), {})
                    }
                }
            }
        }

        compose.onNodeWithTag("create-name").performClick().performTextInput("东京")
        compose.onNodeWithTag("create-name").assertIsFocused()
        compose.onNode(hasScrollAction()).assertExists()
        compose.onNodeWithTag("create-submit").performScrollTo().assertIsDisplayed().assertHasClickAction().performClick()
        compose.onNodeWithTag("create-planning-tip").performScrollTo().assertIsDisplayed()
    }
}
