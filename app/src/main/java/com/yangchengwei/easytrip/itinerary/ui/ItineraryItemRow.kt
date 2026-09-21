package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.yangchengwei.easytrip.core.ui.theme.EasyTripAddress
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPlaceBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPlaceSurface
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimaryDark
import com.yangchengwei.easytrip.workspace.routeColorForDay
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

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
    isDragging: Boolean = false,
    dragTranslationY: Float = 0f,
    onDragStart: () -> Unit = {},
    onDragDelta: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    sharedDragEnabled: Boolean = false,
    parentHandlesDrag: Boolean = false,
    orderColor: Color = Color(routeColorForDay(0)),
) {
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    var localDragging by remember(item.id) { mutableStateOf(false) }
    var localDragY by remember(item.id) { mutableFloatStateOf(0f) }
    var localStartIndex by remember(item.id) { mutableIntStateOf(index) }
    var localItemHeight by remember(item.id) { mutableFloatStateOf(1f) }
    val effectiveDragging = if (sharedDragEnabled) isDragging else localDragging
    val effectiveTranslationY = if (sharedDragEnabled) dragTranslationY else localDragY
    val currentIndex by rememberUpdatedState(index)
    val currentOnPreview by rememberUpdatedState(onPreview)
    val currentOnCommit by rememberUpdatedState(onCommit)
    val effectiveDragStart: () -> Unit = if (sharedDragEnabled) onDragStart else {
        {
            localDragging = true
            localStartIndex = currentIndex
            localDragY = 0f
        }
    }
    val effectiveDragDelta: (Float) -> Unit = if (sharedDragEnabled) onDragDelta else {
        { delta ->
            localDragY += delta
            currentOnPreview((localStartIndex + (localDragY / localItemHeight).toInt()).coerceIn(0, count - 1))
        }
    }
    val effectiveDragEnd: () -> Unit = if (sharedDragEnabled) onDragEnd else {
        {
            currentOnCommit((localStartIndex + (localDragY / localItemHeight).toInt()).coerceIn(0, count - 1))
            localDragging = false
            localDragY = 0f
        }
    }
    val effectiveDragCancel: () -> Unit = if (sharedDragEnabled) onDragCancel else {
        {
            currentOnPreview(localStartIndex)
            localDragging = false
            localDragY = 0f
        }
    }
    ItineraryPlaceContent(
        item = item,
        displayOrder = index + 1,
        orderColor = orderColor,
        modifier = modifier
            .fillMaxWidth()
            .semanticsActions(item.name, index, count, onCommit)
            .onSizeChanged { localItemHeight = it.height.toFloat().coerceAtLeast(1f) }
            .zIndex(if (effectiveDragging) 1f else 0f)
            .graphicsLayer { translationY = effectiveTranslationY },
        leadingAction = {
            ItineraryDragHandle(
                itemId = item.id,
                itemName = item.name,
                count = count,
                parentHandlesDrag = parentHandlesDrag,
                onDragStart = effectiveDragStart,
                onDragDelta = effectiveDragDelta,
                onDragEnd = effectiveDragEnd,
                onDragCancel = effectiveDragCancel,
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
        compactTimeline = true,
        containerColor = if (effectiveDragging) MaterialTheme.colorScheme.primaryContainer else EasyTripPlaceSurface,
        elevation = if (effectiveDragging) 12.dp else 0.dp,
    )
}

@Composable
private fun ItineraryDragHandle(
    itemId: String,
    itemName: String,
    count: Int,
    parentHandlesDrag: Boolean,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val handleColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .size(28.dp)
            .testTag("drag-handle-$itemId")
            .semantics { contentDescription = "拖动调整 $itemName 的顺序" }
            .pointerInput(itemId, count, parentHandlesDrag) {
                if (parentHandlesDrag) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        currentOnDragDelta(amount.y)
                    },
                    onDragEnd = currentOnDragEnd,
                    onDragCancel = currentOnDragCancel,
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(
            Modifier
                .size(18.dp)
                .testTag("drag-handle-icon-$itemId"),
        ) {
            val strokeWidth = 2.dp.toPx()
            val startX = 4.dp.toPx()
            val endX = size.width - startX
            listOf(-5.dp, 0.dp, 5.dp).forEach { offset ->
                val y = size.height / 2f + offset.toPx()
                drawLine(
                    color = handleColor,
                    start = Offset(startX, y),
                    end = Offset(endX, y),
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
    containerColor: Color = EasyTripPlaceSurface,
    elevation: Dp = 0.dp,
    compactTimeline: Boolean = false,
    orderColor: Color = Color(routeColorForDay(0)),
) {
    if (compactTimeline) {
        CompactItineraryStop(item, displayOrder, modifier, leadingAction, trailingAction, orderColor)
        return
    }
    Surface(
        modifier = modifier.testTag("item-${item.id}"),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, EasyTripPlaceBorder),
        shadowElevation = elevation,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                leadingAction?.invoke()
                ItineraryOrderBadge(displayOrder, item.id, orderColor)
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = EasyTripPrimaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("itinerary-place-name-${item.id}"),
                )
                trailingAction?.invoke()
            }
            val contentInset = if (leadingAction == null) 0.dp else 34.dp
            if (item.address.isNotBlank()) {
                Text(
                    text = item.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = EasyTripAddress,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = contentInset).testTag("itinerary-place-address-${item.id}"),
                )
            }
            if (item.arrivalTime != null || item.stayMinutes != null) {
                val arrivalColor = MaterialTheme.colorScheme.primary
                Text(
                    text = buildAnnotatedString {
                        item.arrivalTime?.let {
                            withStyle(SpanStyle(color = arrivalColor)) { append("$it 到达") }
                        }
                        if (item.arrivalTime != null && item.stayMinutes != null) append(" · ")
                        item.stayMinutes?.let { append("停留 ${formatStayHours(it)}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = contentInset).testTag("itinerary-place-timing-${item.id}"),
                )
            }
        }
    }
}

@Composable
private fun CompactItineraryStop(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier,
    dragHandle: (@Composable () -> Unit)?,
    menu: (@Composable () -> Unit)?,
    orderColor: Color,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("item-${item.id}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.width(42.dp), contentAlignment = Alignment.Center) {
                    ItineraryOrderBadge(displayOrder, item.id, orderColor)
                }
                Text(item.name, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("itinerary-place-name-${item.id}"))
            }
            com.yangchengwei.easytrip.core.ui.component.ExpandableNote(
                note = item.note,
                identity = "itinerary-${item.id}",
                textStartPadding = 49.dp,
            ) {
                Text(item.arrivalTime?.toString() ?: "待定",
                    modifier = Modifier.width(42.dp).alignByBaseline().testTag("itinerary-arrival-${item.id}"),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    item.stayMinutes?.let { "停留 ${formatStayHours(it)}" } ?: "待安排停留时长",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).alignByBaseline().testTag("itinerary-place-timing-${item.id}"),
                )
            }
        }
        Row(Modifier.padding(top = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            dragHandle?.invoke()
            menu?.invoke()
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

@Composable
private fun ItineraryOrderBadge(order: Int, itemId: String, color: Color) {
    Box(Modifier.widthIn(min = 18.dp).heightIn(min = 18.dp)
        .testTag("itinerary-order-badge-$itemId")
        .background(color, RoundedCornerShape(9.dp))
        .padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
        Text(order.toString(), Modifier.testTag("itinerary-order-$itemId"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}
