package com.yangchengwei.easytrip.itinerary.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.model.TransportMode

internal fun CalendarTransfer.modeLabel() = when (mode) {
    TransportMode.WALK -> "步行"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TAXI -> "打车"
    TransportMode.TRANSIT -> "公交"
    null -> "交通"
}

internal fun CalendarTransfer.description(): String = buildString {
    append("$fromName → $toName · ${modeLabel()} · 预计约 $minutes 分钟")
    if (continuation || continues) append("（全程）")
    allocatedMinutes?.let { append(" · 预留 $it 分钟") }
    if (blockStart != null && blockEnd != null) {
        append(" · 本段 ${calendarTime(blockStart)}–${calendarTime(blockEnd)}")
        if (continuation || continues) append(" · 本段预留 ${blockEnd - blockStart} 分钟")
    }
    if (allocatedMinutes != null && minutes != null && allocatedMinutes < minutes) append(" · 可调整交通用时")
}

/** Labels have a separate readable target; the thin interval bar always keeps its truthful height. */
private data class TrafficLabel(val top: Float, val routes: List<CalendarTransfer>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarTraffic(
    day: CalendarDay, modifier: Modifier, enabled: Boolean,
    onOpen: (CalendarTransfer) -> Unit,
) {
    val transfers = day.transfers.filter { it.blockStart != null && it.blockEnd != null }
    val labelHeight = (48f * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val groups = mutableListOf<TrafficLabel>()
    transfers.sortedBy { it.blockStart }.forEach { transfer ->
        val y = requireNotNull(transfer.blockStart) * CALENDAR_MINUTE_DP
        val previous = groups.lastOrNull()
        if (previous != null && y < previous.top + labelHeight.value + 2f) {
            groups[groups.lastIndex] = previous.copy(routes = previous.routes + transfer)
        } else groups += TrafficLabel(y, listOf(transfer))
    }
    var opened by remember(day.dayId) { mutableStateOf<List<CalendarTransfer>?>(null) }
    val color = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.secondaryContainer
    Box(modifier) {
        transfers.forEach { transfer ->
            val start = requireNotNull(transfer.blockStart)
            val end = requireNotNull(transfer.blockEnd)
            Canvas(Modifier.offset(y = (start * CALENDAR_MINUTE_DP).dp)
                .fillMaxWidth().height(((end - start) * CALENDAR_MINUTE_DP).dp.coerceAtLeast(2.dp))
                .testTag("calendar-traffic-interval-${day.dayId}-${transfer.legId}")) {
                drawRoundRect(container.copy(alpha = .55f), cornerRadius = CornerRadius(3.dp.toPx()))
                drawRoundRect(color.copy(alpha = .65f), size = Size(3.dp.toPx(), size.height), cornerRadius = CornerRadius(2.dp.toPx()))
                if (end == start) drawLine(color, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
            }
        }
        groups.forEach { group ->
            val transfer = group.routes.first()
            val multiple = group.routes.size > 1
            Surface(modifier = Modifier.offset(x = 5.dp, y = group.top.dp).fillMaxWidth().height(labelHeight)
                .testTag(if (multiple) "calendar-traffic-group-${day.dayId}-${transfer.legId}" else "calendar-traffic-${day.dayId}-${transfer.legId}")
                .semantics { contentDescription = group.routes.joinToString("\n") { it.description() } }
                .clickable(enabled = enabled) { if (multiple) opened = group.routes else onOpen(transfer) },
                color = Color.Transparent) {
                Column(Modifier.padding(horizontal = 3.dp, vertical = 3.dp), verticalArrangement = Arrangement.Center) {
                    val style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp)
                    if (multiple) {
                        Text("${group.routes.size}段交通", style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("查看耗时", style = style, color = color, maxLines = 1)
                    } else {
                        Text(transfer.modeLabel() + if (transfer.continuation || transfer.continues) "·续" else "", style = style, maxLines = 1)
                        Text("${if (transfer.continuation || transfer.continues) "总" else "约"}${transfer.minutes}分", style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (transfer.allocatedMinutes != null && transfer.allocatedMinutes < (transfer.minutes ?: 0)) {
                            Text("预留${transfer.allocatedMinutes}分", style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    opened?.let { routes ->
        ModalBottomSheet(onDismissRequest = { opened = null }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text("${routes.size} 段交通", style = MaterialTheme.typography.titleMedium)
                routes.forEach { transfer ->
                    TextButton({ opened = null; onOpen(transfer) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("calendar-traffic-choice-${transfer.legId}")) {
                        Text(transfer.description(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
