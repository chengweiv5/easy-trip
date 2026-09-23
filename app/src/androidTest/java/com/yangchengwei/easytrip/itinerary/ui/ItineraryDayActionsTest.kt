package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.calendar.CalendarContent
import com.yangchengwei.easytrip.itinerary.calendar.CalendarSaveState
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ItineraryDayActionsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val days = listOf(TripDay("one", 0), TripDay("two", 1))
    private val stop = ItineraryItemUi("a", "地点 A", "地址", null, 60)

    @Test fun dayActionsAlignWithPlaceActionsAndDispatchSeparately() {
        val events = mutableListOf<String>()
        compose.setContent { EasyTripTheme {
            DayItineraryContent(
                DayItineraryUiState(days = days, selectedDayId = "one", items = listOf(stop)),
                Modifier.width(278.dp).height(500.dp),
                onAction = { if (it == DayItineraryAction.AddPlaces) events += "add" },
                onToggleCalendar = { events += "calendar" }, onDeleteDay = { events += "delete" },
            )
        } }
        fun center(tag: String) = compose.onNodeWithTag(tag, true).fetchSemanticsNode().boundsInRoot.center.x
        assertEquals(center("drag-handle-icon-a"), center("day-add-icon"), 1f)
        assertEquals(center("more-icon-a"), center("day-delete-icon"), 1f)
        assertEquals(
            center("day-add-icon") - center("calendar-toggle"),
            center("day-delete-icon") - center("day-add-icon"),
            1f,
        )
        compose.onNodeWithTag("calendar-toggle").performTouchInput { click() }
        compose.onNodeWithTag("add-places-to-selected-day").performTouchInput { click() }
        compose.onNodeWithTag("delete-selected-day").performTouchInput { click() }
        assertEquals(listOf("calendar", "add", "delete"), events)
    }

    @Test fun lastDayCannotBeDeleted() {
        compose.setContent { EasyTripTheme {
            DayItineraryContent(
                DayItineraryUiState(days = days.take(1), selectedDayId = "one"),
                onAction = {}, onDeleteDay = { fail("Final day cannot be deleted") },
            )
        } }
        compose.onNodeWithTag("delete-selected-day").assertIsNotEnabled()
    }

    @Test fun calendarShowsTheSameActionsAndDisablesThemDuringSave() {
        var saving by mutableStateOf(false)
        var deletes = 0
        compose.setContent { EasyTripTheme {
            CalendarContent(
                days.map { WholeTripDayUi(it.id, it.index + 1, listOf(stop.copy(id = it.id)), emptyList()) },
                ItineraryScope.Day("one"), CalendarSaveState(saving = saving), null,
                onToggle = {}, onFocus = { _, _ -> }, onEdit = { _, _ -> }, onAdd = {}, onSave = {},
                onUndo = {}, onRetry = {}, onDismissMessage = {}, onBusy = {},
                modifier = Modifier.width(278.dp).height(500.dp), onDeleteDay = { deletes++ },
            )
        } }
        compose.onNodeWithTag("delete-selected-day").assertIsDisplayed().performClick()
        assertEquals(1, deletes)
        compose.runOnIdle { saving = true }
        compose.onNodeWithTag("delete-selected-day").assertIsNotEnabled()
        compose.onNodeWithTag("add-places-to-selected-day").assertIsNotEnabled()
    }
}
