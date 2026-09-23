package com.yangchengwei.easytrip.itinerary.calendar

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi
import com.yangchengwei.easytrip.itinerary.ui.wholeTripDayDate
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CalendarHeaderAlignmentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var focusedDay: String? = null
    private val startDate = LocalDate.of(2026, 10, 9)

    private fun setup(count: Int = 3, width: Int = 278, fontScale: Float = 1f, dated: Boolean = true) {
        val days = (1..count).map { WholeTripDayUi("day$it", it, emptyList(), emptyList()) }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                EasyTripTheme {
                    CalendarContent(
                        days, ItineraryScope.WholeTrip, CalendarSaveState(), null,
                        onToggle = {}, onFocus = { day, _ -> focusedDay = day }, onEdit = { _, _ -> },
                        onAdd = {}, onSave = {}, onUndo = {}, onRetry = {}, onDismissMessage = {}, onBusy = {},
                        startDate = startDate.takeIf { dated }, modifier = Modifier.width(width.dp).height(660.dp),
                    )
                }
            }
        }
    }

    private fun column(day: Int) = compose.onNodeWithTag("calendar-column-day$day").fetchSemanticsNode().boundsInRoot

    private fun assertEveryLineCentered(text: String, centerX: Float, lines: Int) {
        val node = compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertFalse("$text overflows: size=${layout.size}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}, bottom=${layout.getLineBottom(layout.lineCount-1)}", layout.hasVisualOverflow)
        assertEquals("$text line count", lines, layout.lineCount)
        repeat(layout.lineCount) { line ->
            val inkCenter = bounds.left + (layout.getLineLeft(line) + layout.getLineRight(line)) / 2
            assertEquals("$text line $line must align with its calendar column", centerX, inkCenter, 1f)
        }
    }

    private fun assertPagerAligned(first: Int, last: Int, total: Int) {
        val pager = compose.onNodeWithTag("calendar-date-pager").fetchSemanticsNode().boundsInRoot
        val left = column(first).left
        val right = column(last).right
        assertEquals("Pager must exclude the time labels", left, pager.left, 1f)
        assertEquals("Pager right edge must match the last day", right, pager.right, 1f)
        assertEveryLineCentered("$first–$last / $total 天", (left + right) / 2, 1)
    }

    private fun assertDayAligned(day: Int, dated: Boolean = true) {
        val column = column(day)
        val heading = compose.onNodeWithTag("calendar-date-day$day").fetchSemanticsNode().boundsInRoot
        assertEquals(column.left, heading.left, 1f)
        assertEquals(column.right, heading.right, 1f)
        val date = if (dated) wholeTripDayDate(day, startDate) else "日期待定"
        assertEveryLineCentered("第 $day 天\n$date", column.center.x, 2)
    }

    @Test fun pagerAlignsWithDayColumnsOnFirstAndLastPages() {
        setup()
        assertPagerAligned(1, 2, 3)
        compose.onNodeWithText("‹").assertIsNotEnabled()
        compose.onNodeWithText("›").performClick()
        assertPagerAligned(2, 3, 3)
        compose.onNodeWithText("›").assertIsNotEnabled()
        compose.onNodeWithText("‹").performClick()
        assertPagerAligned(1, 2, 3)
    }

    @Test fun eachHeadingLineCentersWithinItsDayColumn() {
        setup()
        assertDayAligned(1)
        assertDayAligned(2)
        compose.onNodeWithText("›").performClick()
        assertDayAligned(2)
        assertDayAligned(3)
        compose.onNodeWithTag("calendar-date-day3").performClick()
        assertEquals("day3", focusedDay)
    }

    @Test fun largeFontAndTwoDigitDaysUseTheSameColumnBounds() {
        setup(count = 30, fontScale = 1.3f)
        assertPagerAligned(1, 1, 30)
        assertDayAligned(1)
        repeat(29) { compose.onNodeWithText("›").performClick() }
        assertPagerAligned(30, 30, 30)
        assertDayAligned(30)
    }

    @Test fun narrowCalendarKeepsPagerAndDateCentered() {
        setup(width = 218)
        assertPagerAligned(1, 1, 3)
        assertDayAligned(1)
        compose.onNodeWithText("›").performClick()
        assertPagerAligned(2, 2, 3)
        assertDayAligned(2)
    }

    @Test fun singleUndatedDayUsesTheEntireDayColumn() {
        setup(count = 1, dated = false)
        assertPagerAligned(1, 1, 1)
        assertDayAligned(1, dated = false)
        compose.onNodeWithText("‹").assertIsNotEnabled()
        compose.onNodeWithText("›").assertIsNotEnabled()
    }
}
