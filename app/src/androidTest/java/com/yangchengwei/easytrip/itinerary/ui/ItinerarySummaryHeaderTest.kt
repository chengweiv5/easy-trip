package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import android.graphics.Bitmap
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.expense.ui.LocalExpenseReviewOpener
import com.yangchengwei.easytrip.trip.domain.TripDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class ItinerarySummaryHeaderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun titleAndSmallDateShareFirstLineAboveExpense() {
        compose.setContent {
            EasyTripTheme {
                ItinerarySummaryHeader(
                    text = "第 1 天 · 3 站",
                    date = "10 月 11 日",
                    expenseLabel = "当日已记 ¥828 · 5笔费用",
                    modifier = Modifier.width(278.dp),
                )
            }
        }
        val title = compose.onNodeWithText("第 1 天 · 3 站").getUnclippedBoundsInRoot()
        val date = compose.onNodeWithText("10 月 11 日").getUnclippedBoundsInRoot()
        val expense = compose.onNodeWithText("当日已记 ¥828 · 5笔费用").getUnclippedBoundsInRoot()
        assertEquals(title.top.value, date.top.value, 0.5f)
        assertEquals(title.bottom.value, date.bottom.value, 0.5f)
        assertEquals(8f, (date.left - title.right).value, 0.5f)
        assertTrue("Expense stays on the second line", expense.top >= title.bottom)
        val titleStyle = layout("第 1 天 · 3 站").layoutInput.style
        val dateStyle = layout("10 月 11 日").layoutInput.style
        assertEquals(16f, titleStyle.fontSize.value)
        assertEquals(12f, dateStyle.fontSize.value)
        assertEquals(titleStyle.fontFamily, dateStyle.fontFamily)
        assertEquals(24f, titleStyle.lineHeight.value)
        assertEquals(titleStyle.lineHeight, dateStyle.lineHeight)
    }

    @Test
    fun absentDateKeepsTwoLinesWithoutPlaceholder() {
        compose.setContent {
            EasyTripTheme {
                ItinerarySummaryHeader(
                    text = "第 1 天 · 0 站",
                    expenseLabel = "当日已记 ¥0",
                    modifier = Modifier.width(278.dp),
                )
            }
        }
        compose.onAllNodes(hasText("日", substring = true)).assertCountEquals(1)
        val title = compose.onNodeWithText("第 1 天 · 0 站").getUnclippedBoundsInRoot()
        val expense = compose.onNodeWithText("当日已记 ¥0").getUnclippedBoundsInRoot()
        assertTrue(expense.top >= title.bottom)
        assertTrue((expense.bottom - title.top).value <= 44.5f)
    }

    @Test
    fun narrowHeaderPreservesSmallDateAndTruncatesOnlyLongTitle() {
        compose.setContent { headerWithActions(width = 240, scale = 1f) }
        assertCompactLayout()
        assertTrue(layout(longTitle).isLineEllipsized(0))
        assertFalse(layout("10 月 11 日").isLineEllipsized(0))
    }

    @Test
    fun largeFontKeepsDateCenteredAndExpenseOnSecondLine() {
        compose.setContent { headerWithActions(width = 278, scale = 2f) }
        assertCompactLayout()
        assertFalse(layout("10 月 11 日").isLineEllipsized(0))
        capture("header-large-font")
    }

    @Test
    fun singleDayUsesTwoLinesAndPreservesExpenseAndDayActions() {
        val events = mutableListOf<String>()
        compose.setContent {
            EasyTripTheme {
                CompositionLocalProvider(LocalExpenseReviewOpener provides { events += "expense:$it" }) {
                    DayItineraryContent(
                        state = DayItineraryUiState(
                            days = listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
                            selectedDayId = "day-1",
                        ),
                        modifier = Modifier.width(278.dp).height(420.dp),
                        startDate = LocalDate.of(2026, 10, 11),
                        onAction = { if (it == DayItineraryAction.AddPlaces) events += "add" },
                        onToggleCalendar = { events += "calendar" },
                        onDeleteDay = { events += "delete" },
                    )
                }
            }
        }
        assertCentered("第 1 天 · 0 站", "10 月 11 日")
        val title = compose.onNodeWithText("第 1 天 · 0 站").getUnclippedBoundsInRoot()
        val expense = compose.onNodeWithText("当日已记 ¥0 ›", useUnmergedTree = true)
        assertTrue(expense.getUnclippedBoundsInRoot().top >= title.bottom)
        expense.performClick()
        compose.onNodeWithTag("calendar-toggle").performClick()
        compose.onNodeWithTag("add-places-to-selected-day").performClick()
        compose.onNodeWithTag("delete-selected-day").performClick()
        assertEquals(listOf("expense:day-1", "calendar", "add", "delete"), events)
        capture("single-day-two-lines")
    }

    @Test
    fun wholeTripEveryDayUsesTwoLinesIncludingEmptyDays() {
        compose.setContent {
            EasyTripTheme {
                WholeTripItineraryContent(
                    days = (1..3).map { WholeTripDayUi("day-$it", it, emptyList(), emptyList()) },
                    startDate = LocalDate.of(2026, 10, 11),
                    modifier = Modifier.width(278.dp).height(480.dp),
                )
            }
        }
        (1..3).forEach { day ->
            val titleText = "第 $day 天 · 0 站"
            val dateText = "10 月 ${day + 10} 日"
            compose.onNodeWithText(titleText).performScrollTo()
            assertCentered(titleText, dateText)
            val parent = hasAnyAncestor(
                hasTestTag("whole-trip-day-day-$day") or hasTestTag("whole-trip-day-summary-day-$day"),
            )
            val expense = compose.onNode(parent and hasText("当日已记 ¥0"), useUnmergedTree = true)
            val title = compose.onNodeWithText(titleText).getUnclippedBoundsInRoot()
            val bounds = expense.getUnclippedBoundsInRoot()
            assertTrue(bounds.top >= title.bottom)
            assertTrue((bounds.bottom - title.top).value <= 44.5f)
        }
        capture("whole-trip-two-lines")
    }

    @Test
    fun calendarOptOutPreservesItsExistingDatePlacement() {
        compose.setContent {
            EasyTripTheme {
                ItinerarySummaryHeader(
                    text = "第 1 天 · 3 站",
                    date = "10 月 11 日",
                    expenseLabel = "当日已记 ¥0",
                    inlineDate = false,
                )
            }
        }
        val expense = compose.onNodeWithText("当日已记 ¥0").getUnclippedBoundsInRoot()
        val date = compose.onNodeWithText("10 月 11 日").getUnclippedBoundsInRoot()
        assertTrue(date.top >= expense.bottom)
    }

    private val longTitle = "第 1,000 天 · 10,000 站"

    @Composable
    private fun headerWithActions(width: Int, scale: Float) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
            EasyTripTheme {
                ItinerarySummaryHeader(
                    text = longTitle,
                    date = "10 月 11 日",
                    expenseLabel = "当日已记 ¥123,456.78 · 999笔费用",
                    modifier = Modifier.width(width.dp),
                    trailingAction = {
                        ItineraryDayActions(false, {}, {}, {})
                    },
                )
            }
        }
    }

    private fun assertCompactLayout() {
        assertCentered(longTitle, "10 月 11 日")
        val title = compose.onNodeWithText(longTitle).getUnclippedBoundsInRoot()
        val date = compose.onNodeWithText("10 月 11 日").getUnclippedBoundsInRoot()
        val expenseText = "当日已记 ¥123,456.78 · 999笔费用"
        val expense = compose.onNodeWithText(expenseText).getUnclippedBoundsInRoot()
        assertTrue("Title retains visible space", (title.right - title.left).value > 0f)
        assertTrue(date.left >= title.right)
        assertTrue(expense.top >= title.bottom)
        assertEquals(1, layout(expenseText).lineCount)
        compose.onNodeWithTag("calendar-toggle").assertIsDisplayed()
        compose.onNodeWithTag("add-places-to-selected-day").assertIsDisplayed()
        compose.onNodeWithTag("delete-selected-day").assertIsDisplayed()
    }

    private fun assertCentered(title: String, date: String) {
        val titleBounds = compose.onNodeWithText(title).getUnclippedBoundsInRoot()
        val dateBounds = compose.onNodeWithText(date).getUnclippedBoundsInRoot()
        assertEquals(
            ((titleBounds.top + titleBounds.bottom) / 2f).value,
            ((dateBounds.top + dateBounds.bottom) / 2f).value,
            0.5f,
        )
        assertEquals(1, layout(title).lineCount)
        assertEquals(1, layout(date).lineCount)
    }

    private fun layout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "v221-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
