package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.time.LocalDate

@Composable
fun ItineraryScopeRail(
    days: List<TripDay>,
    selected: ItineraryScope,
    onSelect: (ItineraryScope) -> Unit,
    onAddDay: () -> Unit = {},
    modifier: Modifier = Modifier,
    startDate: LocalDate? = null,
) {
    LazyColumn(
        modifier.width(64.dp).fillMaxHeight().testTag("itinerary-scope-rail").selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "WHOLE_TRIP") {
            ScopeItem("全程", selected == ItineraryScope.WholeTrip, "itinerary-scope-WHOLE_TRIP", { onSelect(ItineraryScope.WholeTrip) })
        }
        items(days.sortedBy(TripDay::index), key = TripDay::id) { day ->
            val date = startDate?.plusDays(day.index.toLong())
            ScopeItem(
                label = "第 ${day.index + 1} 天",
                selected = selected == ItineraryScope.Day(day.id),
                tag = "itinerary-scope-${day.id}",
                onClick = { onSelect(ItineraryScope.Day(day.id)) },
                subtitle = date?.let { "${it.monthValue}/${it.dayOfMonth}" },
                isDay = true,
            )
        }
        item(key = "ADD_DAY") {
            ScopeItem("＋ 添加", false, "itinerary-add-day", onAddDay, role = Role.Button)
        }
    }
}

@Composable
private fun ScopeItem(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    isDay: Boolean = false,
    role: Role = Role.Tab,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(if (isDay) 52.dp else 48.dp)
            .testTag(tag).selectable(selected = selected, role = role, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
    }
}
