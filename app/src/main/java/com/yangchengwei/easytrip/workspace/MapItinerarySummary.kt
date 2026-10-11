package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.formatCount
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState

internal fun workspaceItineraryMapSummary(state: TripWorkspaceReadyState, itinerary: DayItineraryUiState): String =
    when (val scope = state.itineraryScope) {
        ItineraryScope.WholeTrip ->
            "全程 · ${formatCount(state.days.size)} 天 · ${formatCount(state.wholeTripDays.sumOf { it.items.size })} 站"
        is ItineraryScope.Day -> {
            val day = state.days.sortedBy { it.index }.indexOfFirst { it.id == scope.dayId }
            val count = if (itinerary.selectedDayId == scope.dayId && itinerary.isDayLoaded) itinerary.items.size else
                state.wholeTripDays.firstOrNull { it.dayId == scope.dayId }?.let { it.items.size + it.collapsedItemCount } ?: 0
            if (day < 0) "行程 · ${formatCount(count)} 站"
            else "第 ${formatCount(day + 1)} 天 · ${formatCount(count)} 站"
        }
    }

@Composable
internal fun workspaceMapSummaryHeight() = with(LocalDensity.current) { maxOf(32.dp, 18.sp.toDp() + 12.dp) }

@Composable
internal fun MapItinerarySummary(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier.height(workspaceMapSummaryHeight()).testTag("map-itinerary-summary"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = EasyTripTheme.elevation.floating,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            WorkspaceSummaryIcon(WorkspaceSection.ITINERARY, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
