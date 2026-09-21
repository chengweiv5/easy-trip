package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.time.LocalDate

@Composable
fun WorkspaceItineraryContent(
    days: List<TripDay>,
    selected: ItineraryScope,
    wholeTripDays: List<WholeTripDayUi>,
    onSelect: (ItineraryScope) -> Unit,
    dayContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onAddDay: () -> Unit = {},
    onAppendDay: () -> Unit = onAddDay,
    contentPadding: PaddingValues = PaddingValues.Zero,
    startDate: LocalDate? = null,
    calendarContent: (@Composable () -> Unit)? = null,
    onToggleCalendar: (() -> Unit)? = null,
) {
    val savedViews = rememberSaveableStateHolder()
    Row(modifier.padding(contentPadding).padding(horizontal = 16.dp)) {
        ItineraryScopeRail(
            days = days,
            selected = selected,
            onSelect = onSelect,
            onAddDay = onAppendDay,
            modifier = Modifier.fillMaxHeight(),
            startDate = startDate,
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
        Box(Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp)) {
            savedViews.SaveableStateProvider(if (calendarContent != null) "calendar" else "list-$selected") {
            if (calendarContent != null) calendarContent() else when (selected) {
                ItineraryScope.WholeTrip -> WholeTripItineraryContent(
                    days = wholeTripDays,
                    onToggleCalendar = onToggleCalendar,
                    startDate = startDate,
                    onAddDay = onAddDay,
                    modifier = Modifier.testTag("whole-trip-content"),
                )
                is ItineraryScope.Day -> dayContent()
            }
            }
        }
    }
}
