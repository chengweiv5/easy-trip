package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
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
    ItineraryItemCard(
        item = item,
        displayOrder = index + 1,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item-${item.id}")
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
        containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        elevation = if (isDragging) 12.dp else 3.dp,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onTiming, Modifier.testTag("timing-${item.id}")) { Text("时间") }
            TextButton(onCrossDay, Modifier.testTag("move-${item.id}")) { Text("移动到…") }
            TextButton(onDelete, Modifier.testTag("delete-${item.id}")) { Text("删除") }
        }
    }
}

@Composable
fun ItineraryItemCard(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier = Modifier,
) {
    ItineraryItemCard(item, displayOrder, modifier, MaterialTheme.colorScheme.surfaceContainer, 3.dp)
}

@Composable
private fun ItineraryItemCard(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier,
    containerColor: Color,
    elevation: Dp,
    actions: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = displayOrder.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    if (item.address.isNotBlank()) {
                        Text(item.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val timing = listOfNotNull(
                        item.arrivalTime?.let { "到达 $it" },
                        item.stayMinutes?.let { "停留 $it 分钟" },
                    ).joinToString(" · ")
                    if (timing.isNotEmpty()) Text(timing, style = MaterialTheme.typography.bodyMedium)
                }
            }
            actions()
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
