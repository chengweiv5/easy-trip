package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripDateRangePickerSheetTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun rangeSelectionConfirmsOnceAndCancelBackAndScrimDoNotSubmit() {
        var confirmations = 0
        var dismissals = 0
        var open by mutableStateOf(true)
        compose.setContent {
            if (open) TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(startDate = LocalDate.parse("2026-10-02")),
                onConfirm = { confirmations++; open = false },
                onDismiss = { dismissals++; open = false },
            )
        }

        compose.onNodeWithTag("trip-date-2026-10-05").performClick()
        compose.onNodeWithTag("trip-date-range-confirm").performClick()
        compose.runOnIdle {
            assertEquals(1, confirmations)
            assertEquals(0, dismissals)
            open = true
        }
        pressBack()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, dismissals)
            open = true
        }
        compose.waitForIdle()
        compose.onNodeWithTag("trip-date-range-scrim").performTouchInput { click(Offset(1f, 1f)) }
        compose.runOnIdle { assertEquals(2, dismissals) }
    }

    @Test fun fixedDayCountChangeResetsSelectionAndCannotSubmitOldLength() {
        var fixedDays by mutableStateOf(3)
        var confirmed: DateRangeSelection? = null
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(startDate = LocalDate.parse("2026-10-02")),
                fixedDayCount = fixedDays,
                onConfirm = { confirmed = it },
                onDismiss = {},
            )
        }

        compose.runOnIdle { fixedDays = 4 }
        compose.onNodeWithText("2026-10-02 至 2026-10-05 · 4天3晚").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("trip-date-range-confirm").performClick()

        compose.runOnIdle { assertEquals(DateRangeSelection(LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-05")), confirmed) }
    }

    @Test fun confirmIsSingleFlightWhileParentKeepsSheetOpen() {
        var confirmations = 0
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(startDate = LocalDate.parse("2026-10-02")),
                onConfirm = { confirmations++ },
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("trip-date-2026-10-05").performClick()
        compose.onNodeWithTag("trip-date-range-confirm").performClick()
        compose.onNodeWithTag("trip-date-range-confirm").performClick()

        compose.runOnIdle { assertEquals(1, confirmations) }
    }

    @Test fun modalHostHidesBackgroundSemanticsAndScrimExposesDismissClickAction() {
        compose.setContent {
            TripDateRangePickerModalHost(
                sheetVisible = true,
                background = { Text("背景设置", Modifier.testTag("trip-date-range-background")) },
            ) {
                TripDateRangePickerSheet(initialSelection = null, onConfirm = {}, onDismiss = {})
            }
        }

        compose.onNodeWithTag("trip-date-range-background").assertDoesNotExist()
        compose.onNodeWithTag("trip-date-range-scrim").assert(
            SemanticsMatcher("has dismiss click action") { it.config.contains(SemanticsActions.OnClick) },
        )
    }

    @Test fun maximumMonthNextNavigationStaysAtMaximumWithoutOverflow() {
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = null,
                initialDisplayedMonth = java.time.YearMonth.from(LocalDate.MAX),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("trip-date-range-next-month").performClick()
        compose.onNodeWithTag("trip-date-${LocalDate.MAX}").performScrollTo().assertIsDisplayed()
    }

    @Test fun minimumMonthPreviousNavigationStaysAtMinimumWithoutOverflow() {
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = null,
                initialDisplayedMonth = java.time.YearMonth.from(LocalDate.MIN),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("trip-date-range-previous-month").performClick()
        compose.onNodeWithTag("trip-date-${LocalDate.MIN}").performScrollTo().assertIsDisplayed()
    }

    @Test fun suppliedInitialMonthRendersWhenThereIsNoInitialRange() {
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = null,
                initialDisplayedMonth = java.time.YearMonth.of(2027, 3),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithText("2027年3月").assertIsDisplayed()
        compose.onNodeWithTag("trip-date-2027-03-15").assertIsDisplayed()
    }

    @Test fun summaryShowsSelectedRangeDuration() {
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-04")),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithText("2026-10-02 至 2026-10-04 · 3天2晚").performScrollTo().assertIsDisplayed()
    }

    @Test fun rangeGridKeepsRangeBackgroundWithoutConnectorAndTodaySemanticsOverlapsSelection() {
        val today = LocalDate.now()
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(today.minusDays(1), today.plusDays(1)),
                initialDisplayedMonth = java.time.YearMonth.from(today),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("trip-date-$today").assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("$today，范围内日期，今天"),
            ),
        )
        compose.onNodeWithTag("trip-date-range-connector-$today", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("trip-date-range-sheet-handle", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun rangeGridUsesSemanticsForStartEndAndInteriorAcrossMonths() {
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(
                    LocalDate.parse("2026-10-30"),
                    LocalDate.parse("2026-11-02"),
                ),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("trip-date-2026-10-30").assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("2026-10-30，开始日期"),
            ),
        )
        compose.onNodeWithTag("trip-date-2026-10-31").assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("2026-10-31，范围内日期"),
            ),
        )
        compose.onNodeWithTag("trip-date-2026-11-01").assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("2026-11-01，范围内日期"),
            ),
        )
        compose.onNodeWithTag("trip-date-2026-11-02").assertDoesNotExist()
        compose.onNodeWithTag("trip-date-range-next-month").performClick()
        compose.onNodeWithTag("trip-date-2026-11-02").assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("2026-11-02，结束日期"),
            ),
        )
    }

    @Test fun todaySemanticsIncludesTodayWhenItIsRangeEndpoint() {
        val today = LocalDate.now()
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(today, today.plusDays(1)),
                initialDisplayedMonth = java.time.YearMonth.from(today),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("trip-date-$today").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("$today，开始日期，今天"),
            ),
        )
    }

    @Test fun tooLongRangeShowsErrorAndDisablesConfirmation() {
        compose.setContent {
            TripDateRangePickerSheet(
                initialSelection = DateRangeSelection(
                    LocalDate.parse("2026-10-01"),
                    LocalDate.parse("2026-10-31"),
                ),
                onConfirm = {},
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("trip-date-range-error").assertIsDisplayed()
        compose.onNodeWithTag("trip-date-range-confirm").assertIsNotEnabled()
    }

    @Test fun tinyHeightAtTwoTimesFontKeepsFixedActionsAboveNavigationInset() {
        val inset = 36.dp
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Box(Modifier.width(280.dp).height(220.dp).testTag("trip-date-range-test-container")) {
                    TripDateRangePickerSheet(
                        initialSelection = null,
                        onConfirm = {},
                        onDismiss = {},
                        bottomInset = WindowInsets(bottom = inset),
                    )
                }
            }
        }

        val root = compose.onNodeWithTag("trip-date-range-test-container").getUnclippedBoundsInRoot()
        val cancel = compose.onNodeWithTag("trip-date-range-cancel").assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithTag("trip-date-range-confirm").assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()
        check(cancel.bottom <= root.bottom - inset)
        check(confirm.bottom <= root.bottom - inset)
    }
}
