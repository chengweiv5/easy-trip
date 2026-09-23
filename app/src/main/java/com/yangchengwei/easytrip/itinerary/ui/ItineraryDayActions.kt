package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.itinerary.calendar.CalendarToggle

/** Shares the place-row action columns so day and place controls line up. */
@Composable
internal fun ItineraryDayActions(
    calendarSelected: Boolean,
    onToggleCalendar: (() -> Unit)?,
    onAdd: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    enabled: Boolean = true,
    canDelete: Boolean = true,
) {
    Row(Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        onToggleCalendar?.let { CalendarToggle(calendarSelected, it, enabled, compact = true) }
        onAdd?.let {
            IconButton(it, enabled = enabled, modifier = Modifier.size(28.dp).testTag("add-places-to-selected-day")) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Icon(Icons.Rounded.Add, "从地点池添加地点", Modifier.size(18.dp).testTag("day-add-icon"))
                }
            }
        }
        onDelete?.let {
            IconButton(it, enabled = enabled && canDelete, modifier = Modifier.size(28.dp).testTag("delete-selected-day")) {
                Icon(Icons.Rounded.Delete, "删除当天", Modifier.size(18.dp).testTag("day-delete-icon"))
            }
        }
    }
}
