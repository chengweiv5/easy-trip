package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt

@Composable
fun ItineraryItemRow(
    item: ItineraryItemUi,
    index: Int,
    count: Int,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onTiming: () -> Unit,
    onCrossDay: () -> Unit,
    onDelete: () -> Unit,
) {
    var drag by remember(item.id) { mutableFloatStateOf(0f) }
    val currentIndex by rememberUpdatedState(index)
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = { Text(listOfNotNull(item.arrivalTime?.toString(), item.stayMinutes?.let { "停留 $it 分钟" }).joinToString(" · ")) },
        trailingContent = {
            Row {
                TextButton(onTiming, Modifier.testTag("timing-${item.id}")) { Text("时间") }
                TextButton(onCrossDay, Modifier.testTag("move-${item.id}")) { Text("移动到…") }
                TextButton(onDelete, Modifier.testTag("delete-${item.id}")) { Text("删除") }
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("item-${item.id}").semanticsActions(index, count, onCommit).pointerInput(item.id, count) {
            var startIndex = currentIndex
            detectDragGesturesAfterLongPress(
                onDragStart = { drag = 0f; startIndex = currentIndex },
                onDrag = { change, amount ->
                    change.consume()
                    drag += amount.y
                    onPreview((startIndex + (drag / 120f).roundToInt()).coerceIn(0, count - 1))
                },
                onDragEnd = {
                    onCommit((startIndex + (drag / 120f).roundToInt()).coerceIn(0, count - 1))
                    drag = 0f
                },
                onDragCancel = { drag = 0f },
            )
        },
    )
}

private fun Modifier.semanticsActions(index: Int, count: Int, onCommit: (Int) -> Unit) = semantics {
    customActions = listOfNotNull(
        (index > 0).takeIf { it }?.let { CustomAccessibilityAction("上移") { onCommit(index - 1); true } },
        (index < count - 1).takeIf { it }?.let { CustomAccessibilityAction("下移") { onCommit(index + 1); true } },
    )
}
