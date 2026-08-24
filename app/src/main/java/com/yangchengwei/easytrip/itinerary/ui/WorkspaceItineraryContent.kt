package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope

@Composable
fun WorkspaceItineraryContent(
    days: List<TripDay>,
    selected: ItineraryScope,
    wholeTripDays: List<WholeTripDayUi>,
    onSelect: (ItineraryScope) -> Unit,
    dayContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onAddDay: () -> Unit = {},
) {
    Row(modifier) {
        ItineraryScopeRail(
            days = days,
            selected = selected,
            onSelect = onSelect,
            onAddDay = onAddDay,
            modifier = Modifier.fillMaxHeight(),
        )
        VerticalDivider()
        Box(Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp)) {
            when (selected) {
                ItineraryScope.WholeTrip -> WholeTripItineraryContent(wholeTripDays)
                is ItineraryScope.Day -> dayContent()
            }
        }
    }
}
