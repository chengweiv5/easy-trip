package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope

@Composable
fun ItineraryScopeRail(
    days: List<TripDay>,
    selected: ItineraryScope,
    onSelect: (ItineraryScope) -> Unit,
    onAddDay: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight()
            .testTag("itinerary-scope-rail")
            .selectableGroup()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "WHOLE_TRIP") {
            ScopeItem(
                label = "全程",
                selected = selected == ItineraryScope.WholeTrip,
                tag = "itinerary-scope-WHOLE_TRIP",
                onClick = { onSelect(ItineraryScope.WholeTrip) },
            )
        }
        items(days.sortedBy(TripDay::index), key = TripDay::id) { day ->
            ScopeItem(
                label = dayHeading(day.index + 1),
                selected = selected == ItineraryScope.Day(day.id),
                tag = "itinerary-scope-${day.id}",
                onClick = { onSelect(ItineraryScope.Day(day.id)) },
            )
        }
        item(key = "ADD_DAY") {
            CompactSecondaryButton(
                onClick = onAddDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("itinerary-add-day"),
            ) {
                Text("添加")
            }
        }
    }
}

@Composable
private fun ScopeItem(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    SelectablePill(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().testTag(tag),
        role = Role.Tab,
    )
}
