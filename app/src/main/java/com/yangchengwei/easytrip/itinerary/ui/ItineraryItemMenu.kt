package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal enum class ItineraryItemMenuAction {
    EditTiming,
    ScheduleAgain,
    MoveToOtherDay,
    Delete,
}

@Composable
internal fun ItineraryItemMenu(
    itemId: String,
    itemName: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (ItineraryItemMenuAction) -> Unit,
    modifier: Modifier = Modifier,
    canScheduleAgain: Boolean = true,
) {
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier
                .testTag("more-$itemId")
                .semantics { contentDescription = "$itemName，更多行程项操作" },
        ) {
            MoreVertIcon(Modifier.size(22.dp).testTag("more-icon-$itemId"))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            MenuItem("编辑时间与停留时长", "menu-timing-$itemId") {
                onExpandedChange(false)
                onAction(ItineraryItemMenuAction.EditTiming)
            }
            if (canScheduleAgain) {
                MenuItem("再次安排这个地点", "menu-schedule-again-$itemId") {
                    onExpandedChange(false)
                    onAction(ItineraryItemMenuAction.ScheduleAgain)
                }
            }
            MenuItem("移动到其他日期", "menu-move-$itemId") {
                onExpandedChange(false)
                onAction(ItineraryItemMenuAction.MoveToOtherDay)
            }
            DropdownMenuItem(
                text = { Text("删除行程项", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onExpandedChange(false)
                    onAction(ItineraryItemMenuAction.Delete)
                },
                modifier = Modifier.testTag("menu-delete-$itemId"),
            )
        }
    }
}

@Composable
private fun MoreVertIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val radius = size.minDimension / 11f
        listOf(.25f, .5f, .75f).forEach { fraction ->
            drawCircle(color, radius, Offset(size.width / 2f, size.height * fraction))
        }
    }
}

@Composable
private fun MenuItem(text: String, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}
