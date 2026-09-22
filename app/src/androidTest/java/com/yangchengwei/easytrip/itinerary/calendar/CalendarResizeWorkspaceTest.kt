package com.yangchengwei.easytrip.itinerary.calendar

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.*
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.*
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CalendarResizeWorkspaceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun setup(width: Int, scale: Float, midnight: Boolean) {
        val items = if (midnight) listOf(ItineraryItemUi("a", "西湖天地", "杭州", LocalTime.MIDNIGHT, 90))
            else listOf(ItineraryItemUi("early", "西湖天地", "杭州", LocalTime.of(9, 0), 60),
                ItineraryItemUi("a", "湖滨步行街", "杭州", LocalTime.of(11, 30), 90))
        val state = TripWorkspaceUiState(tripName = "杭州慢游", startDate = LocalDate.of(2026, 9, 6),
            days = listOf(TripDay("day", 0)), section = WorkspaceSection.ITINERARY,
            itineraryScope = ItineraryScope.Day("day"), sheetLevel = WorkspaceSheetLevel.EXPANDED,
            calendarMode = true, calendarDays = listOf(WholeTripDayUi("day", 1, items, emptyList())))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) { EasyTripTheme {
                Box(Modifier.requiredSize(width.dp, 844.dp)) {
                    TripWorkspaceContent(TripWorkspacePageState.Ready(state.toReadyState()), WorkspaceMapState.Ready,
                        onAction = {}, placeState = PlacePoolUiState(), onPlaceAction = {},
                        itineraryState = DayItineraryUiState(), onItineraryAction = {}, mapContent = {})
                }
            } }
        }
        compose.waitForIdle()
    }

    private fun event() = compose.onNodeWithTag("calendar-event-day:a:day")
    private fun save(name: String) {
        compose.onRoot().captureToImage().asAndroidBitmap().let { image ->
            java.io.File(compose.activity.getExternalFilesDir(null), name).outputStream().use {
                image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)
            }
        }
    }

    @Test fun fullWorkspaceFloatingHintKeepsFingerGapAndSummary() {
        setup(390, 1f, false)
        val bounds = event().fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        event().performTouchInput { down(Offset(width * .5f, height - 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val hint = compose.onNodeWithTag("calendar-resize-end", true)
        hint.assertIsDisplayed().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Floating"))
        assertEquals(bounds.bottom - 2f - 64f, hint.fetchSemanticsNode().boundsInRoot.bottom, 1f)
        compose.onNodeWithTag("calendar-toggle").assertIsDisplayed()
        assertEquals(viewport, compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot)
        save("calendar-hint-workspace-floating.jpg")
        event().performTouchInput { cancel() }
    }

    @Test fun fullWorkspaceTopHintReplacesSummaryOnly() {
        setup(390, 1f, true)
        val viewport = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        val summary = compose.onNodeWithTag("calendar-summary").fetchSemanticsNode().boundsInRoot
        event().performTouchInput { down(Offset(width * .5f, 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val hint = compose.onNodeWithTag("calendar-resize-start", true)
        hint.assertIsDisplayed().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Summary"))
        assertEquals(summary, hint.fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithTag("calendar-toggle").assertDoesNotExist()
        assertEquals(viewport, compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot)
        save("calendar-hint-workspace-top.jpg")
        event().performTouchInput { cancel() }
        compose.onNodeWithTag("calendar-toggle").assertIsDisplayed()
    }

    @Test fun narrowLargeFontUsesSheetWidthWithoutClippingTextOrMovingTimeline() {
        setup(320, 2f, true)
        val viewport = compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot
        event().performTouchInput { down(Offset(width * .5f, 2f)); advanceEventTime(600); moveBy(Offset.Zero) }
        val hint = compose.onNodeWithTag("calendar-resize-start", true)
        hint.assertIsDisplayed().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Wide"))
        val bounds = hint.fetchSemanticsNode().boundsInRoot
        assertEquals(296f, bounds.width, 1f)
        val action = compose.onNodeWithText("调整开始", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val value = compose.onAllNodesWithText("00:00", useUnmergedTree = true).fetchSemanticsNodes()
            .first { it.boundsInRoot.top < viewport.top }.boundsInRoot
        assertTrue("action $action in $bounds", bounds.contains(action.topLeft) && action.right <= bounds.right && action.bottom <= bounds.bottom)
        assertTrue("time $value in $bounds", bounds.contains(value.topLeft) && value.right <= bounds.right && value.bottom <= bounds.bottom)
        compose.onNodeWithTag("workspace-tabs").assertDoesNotExist()
        compose.onNodeWithTag("calendar-toggle").assertDoesNotExist()
        assertEquals(viewport, compose.onNodeWithTag("calendar-viewport").fetchSemanticsNode().boundsInRoot)
        save("calendar-hint-workspace-large.jpg")
        event().performTouchInput { cancel() }
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithTag("calendar-toggle").assertIsDisplayed()
    }
}
