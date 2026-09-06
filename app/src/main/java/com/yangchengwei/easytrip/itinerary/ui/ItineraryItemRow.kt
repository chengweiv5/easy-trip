package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
private val ReorderStep = 120.dp

@Composable
internal fun ItineraryItemRow(
    item: ItineraryItemUi,
    index: Int,
    count: Int,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onMenuAction: (ItineraryItemMenuAction) -> Unit,
    modifier: Modifier = Modifier,
    canScheduleAgain: Boolean = false,
) {
    var isDragging by remember(item.id) { mutableStateOf(false) }
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    ItineraryPlaceContent(
        item = item,
        displayOrder = index + 1,
        modifier = modifier
            .fillMaxWidth()
            .semanticsActions(item.name, index, count, onCommit),
        leadingAction = {
            ItineraryDragHandle(
                itemId = item.id,
                itemName = item.name,
                index = index,
                count = count,
                onPreview = onPreview,
                onCommit = onCommit,
                onDraggingChange = { isDragging = it },
            )
        },
        trailingAction = {
            ItineraryItemMenu(
                itemId = item.id,
                itemName = item.name,
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                onAction = onMenuAction,
                canScheduleAgain = canScheduleAgain && item.placeId != null,
            )
        },
        containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        elevation = if (isDragging) 12.dp else 0.dp,
    )
}

@Composable
private fun ItineraryDragHandle(
    itemId: String,
    itemName: String,
    index: Int,
    count: Int,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var drag by remember(itemId) { mutableFloatStateOf(0f) }
    val currentIndex by rememberUpdatedState(index)
    val currentOnPreview by rememberUpdatedState(onPreview)
    val currentOnCommit by rememberUpdatedState(onCommit)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingChange)
    val reorderStepPx = with(LocalDensity.current) { ReorderStep.toPx() }
    val handleColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .size(40.dp)
            .testTag("drag-handle-$itemId")
            .semantics { contentDescription = "拖动调整 $itemName 的顺序" }
            .pointerInput(itemId, count, reorderStepPx) {
                var startIndex = currentIndex
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        drag = 0f
                        startIndex = currentIndex
                        currentOnDraggingChange(true)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        drag += amount.y
                        currentOnPreview((startIndex + (drag / reorderStepPx).toInt()).coerceIn(0, count - 1))
                    },
                    onDragEnd = {
                        currentOnCommit((startIndex + (drag / reorderStepPx).toInt()).coerceIn(0, count - 1))
                        drag = 0f
                        currentOnDraggingChange(false)
                    },
                    onDragCancel = {
                        drag = 0f
                        currentOnPreview(startIndex)
                        currentOnDraggingChange(false)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val strokeWidth = 2.dp.toPx()
            val startX = 4.dp.toPx()
            val endX = size.width - startX
            listOf(6.dp, 11.dp, 16.dp).forEach { y ->
                drawLine(
                    color = handleColor,
                    start = Offset(startX, y.toPx()),
                    end = Offset(endX, y.toPx()),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
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
        shape = RoundedCornerShape(12.dp),
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
