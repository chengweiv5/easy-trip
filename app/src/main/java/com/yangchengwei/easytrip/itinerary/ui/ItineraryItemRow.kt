package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    var isDragging by remember(item.id) { mutableStateOf(false) }
    val currentIndex by rememberUpdatedState(index)
    Column {
        ItineraryPlaceContent(
            item = item,
            displayOrder = index + 1,
            modifier = Modifier
                .fillMaxWidth()
                .semanticsActions(item.name, index, count, onCommit)
                .pointerInput(item.id, count) {
                    var startIndex = currentIndex
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            drag = 0f
                            startIndex = currentIndex
                            isDragging = true
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            drag += amount.y
                            onPreview((startIndex + (drag / 120f).roundToInt()).coerceIn(0, count - 1))
                        },
                        onDragEnd = {
                            onCommit((startIndex + (drag / 120f).roundToInt()).coerceIn(0, count - 1))
                            drag = 0f
                            isDragging = false
                        },
                        onDragCancel = {
                            drag = 0f
                            isDragging = false
                        },
                    )
                },
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            elevation = if (isDragging) 12.dp else 0.dp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            androidx.compose.material3.TextButton(onClick = onTiming, modifier = Modifier.testTag("timing-${item.id}")) { Text("时间") }
            androidx.compose.material3.TextButton(onClick = onCrossDay, modifier = Modifier.testTag("move-${item.id}")) { Text("移动到…") }
            androidx.compose.material3.TextButton(onClick = onDelete, modifier = Modifier.testTag("delete-${item.id}")) { Text("删除") }
        }
    }
}

@Composable
internal fun ItineraryPlaceContent(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier = Modifier,
    leadingAction: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 0.dp,
) {
    Surface(
        modifier = modifier.testTag("item-${item.id}"),
        color = containerColor,
        shadowElevation = elevation,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leadingAction?.let { action -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { action() } }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item.arrivalTime?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                item.stayMinutes?.let {
                    Text(
                        text = "停留 $it 分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.address.isNotBlank()) {
                    Text(
                        text = item.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailingAction?.let { action -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { action() } }
        }
    }
}

private fun Modifier.semanticsActions(name: String, index: Int, count: Int, onCommit: (Int) -> Unit) = semantics(mergeDescendants = true) {
    contentDescription = "$name，第 ${index + 1} 项，共 $count 项"
    customActions = listOfNotNull(
        (index > 0).takeIf { it }?.let { CustomAccessibilityAction("上移") { onCommit(index - 1); true } },
        (index < count - 1).takeIf { it }?.let { CustomAccessibilityAction("下移") { onCommit(index + 1); true } },
    )
}
