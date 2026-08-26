package com.yangchengwei.easytrip.itinerary.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal enum class ItineraryItemMenuAction {
    EditTiming,
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
) {
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier
                .testTag("more-$itemId")
                .semantics { contentDescription = "$itemName，更多行程项操作" },
        ) {
            Text("⋮", style = MaterialTheme.typography.titleLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            MenuItem("编辑时间与停留时长", "menu-timing-$itemId") {
                onExpandedChange(false)
                onAction(ItineraryItemMenuAction.EditTiming)
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
private fun MenuItem(text: String, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}
