package com.yangchengwei.easytrip.itinerary.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi

@Composable
internal fun CalendarGrid(
    days: List<CalendarDay>, single: String?, expandedGroup: Int?, focusId: String?,
    draftItem: ItineraryItemUi?, draftDayId: String?, draftTiming: ItineraryTiming?, draftMode: CalendarDragMode?,
    modifier: Modifier, editable: Boolean,
    onOpen: (String, String) -> Unit, onExpand: (Int) -> Unit, onOpenGroup: (String, String) -> Unit,
    onStart: (String, ItineraryItemUi, CalendarDragMode, Offset, Float) -> Unit,
    onMove: (Offset) -> Unit, onEnd: (Boolean) -> Unit,
    onStep: (String, ItineraryItemUi, CalendarDragMode, Int) -> Unit,
    onKeyboardStart: (String, ItineraryItemUi) -> Unit,
    onKeyboardStep: (Int) -> Unit, onKeyboardEnd: (Boolean) -> Unit,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.height(24.dp * CALENDAR_HOUR_DP + CALENDAR_HOUR_DP.dp).testTag("calendar-grid")) {
        Box(Modifier.width(36.dp).fillMaxHeight()) {
            (0..24).forEach { hour ->
                Text(calendarTime(hour * 60), Modifier.offset(y = (hour * CALENDAR_HOUR_DP).dp), fontSize = 9.sp, lineHeight = 12.sp, color = muted)
            }
        }
        days.forEach { day ->
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().testTag("calendar-column-${day.dayId}")) {
                Canvas(Modifier.fillMaxSize()) {
                    (0..24).forEach { hour ->
                        val y = (hour * CALENDAR_HOUR_DP).dp.toPx()
                        drawLine(line, Offset.Zero.copy(y = y), Offset(size.width,y), 1.dp.toPx())
                        if (hour < 24) drawLine(line.copy(alpha = .45f), Offset(0f,y+(CALENDAR_HOUR_DP / 2).dp.toPx()), Offset(size.width,y+(CALENDAR_HOUR_DP / 2).dp.toPx()), .5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
                    }
                    drawLine(line, Offset.Zero, Offset(0f, size.height), 1.dp.toPx())
                }
                // Traffic references never infer an end for an unset stay.
                day.transfers.filter { it.start != null }.forEach { transfer ->
                    val start = transfer.start ?: return@forEach
                    val end = transfer.end?.coerceAtMost(1440) ?: return@forEach
                    if (end > start && day.events.none { !it.placeholder && !it.point && it.start < end && start < it.end }) Box(Modifier.offset(y = (start * CALENDAR_MINUTE_DP).dp).fillMaxWidth().height(((end-start) * CALENDAR_MINUTE_DP).dp).padding(horizontal = 3.dp)) {
                        Text("约 ${transfer.minutes} 分钟赶路${if (transfer.conflict) " · 时间不足" else ""}",
                            color = if (transfer.conflict) Color(0xFFBA5B37) else muted, fontSize = 9.sp, lineHeight = 12.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
                day.events.groupBy { it.group }.forEach { (group, events) ->
                    val hasPlaceholder = events.any { it.placeholder }
                    val intersectsOtherKind = events.any { event -> day.events.any { other ->
                        other.placeholder != event.placeholder && other.start < event.end && event.start < other.end
                    } }
                    // Reserve separate visual columns when an uncertain placeholder shares time with real events.
                    val groupWidth = if (intersectsOtherKind) maxWidth * (if (hasPlaceholder) .4f else .6f) else maxWidth
                    val groupLeft = if (intersectsOtherKind && hasPlaceholder) maxWidth * .6f else 0.dp
                    val aggregate = events.none { it.placeholder } && events.maxOf { it.laneCount } > (if (single == null) 1 else 2)
                    if (aggregate) {
                        val top = events.minOf { it.start }
                        val end = events.maxOf { it.end }
                        CalendarCard("${events.size} 项日程", "时间交叠 · 点击展开",
                            Modifier.offset(x = groupLeft + 3.dp, y = (top * CALENDAR_MINUTE_DP).dp).width((groupWidth-6.dp).coerceAtLeast(1.dp)).height(((end-top)*CALENDAR_MINUTE_DP).dp.coerceAtLeast(24.dp)).testTag("calendar-overlap-${day.dayId}-$group"),
                            conflict = events.any { it.conflicts.isNotEmpty() }, onClick = {
                                if (single == null) onOpenGroup(events.first().sourceDayId, events.first().item.id) else onExpand(group)
                            })
                    } else events.forEach { event ->
                        val width = (groupWidth / event.laneCount - 6.dp).coerceAtLeast(1.dp)
                        val dragEnabled = single == event.sourceDayId && !event.crossDay && editable && (event.point || event.canDrag)
                        val compact = !event.placeholder && event.end-event.start < 45
                        CalendarCard("${event.order}. ${event.item.name}", eventSubtitle(event),
                            Modifier.offset(x = groupLeft + groupWidth / event.laneCount * event.lane + 3.dp, y = (event.start * CALENDAR_MINUTE_DP).dp)
                                .width(width).height(if (event.placeholder) CALENDAR_HOUR_DP.dp else if (event.point) 2.dp else ((event.end-event.start)*CALENDAR_MINUTE_DP).dp.coerceAtLeast(1.dp)).testTag("calendar-event-${event.key}"),
                            dashed = event.placeholder, conflict = event.conflicts.isNotEmpty() || event.trafficConflict,
                            accent = Color(com.yangchengwei.easytrip.workspace.routeColorForDay(event.sourceDayNumber - 1)),
                            editable = dragEnabled, edges = event.canDrag && !event.point, compact = compact,
                            selected = focusId == event.item.id,
                            onClick = { onOpen(event.sourceDayId, event.item.id) },
                            onStart = { mode, point, grab -> onStart(event.sourceDayId, event.item, mode, point, grab) },
                            onMove = onMove, onEnd = onEnd,
                            onStep = { mode, delta -> onStep(event.sourceDayId, event.item, mode, delta) },
                            onKeyboardStart = { onKeyboardStart(event.sourceDayId, event.item) }, onKeyboardStep = onKeyboardStep, onKeyboardEnd = onKeyboardEnd,
                        )
                    }
                }
                if (draftItem != null && draftDayId == day.dayId && draftTiming?.arrivalTime != null) {
                    val time = draftTiming.arrivalTime
                    val start = time.hour * 60 + time.minute
                    val duration = draftTiming.stayMinutes ?: 60
                    val resizingStart = draftMode == CalendarDragMode.START
                    val resizingEnd = draftMode == CalendarDragMode.END
                    val markerHeight = 20.dp
                    val startY = (start * CALENDAR_MINUTE_DP).dp
                    val draftHeight = (duration * CALENDAR_MINUTE_DP).dp.coerceAtLeast(2.dp)
                    val conflict = day.events.any { it.item.id != draftItem.id && !it.placeholder && !it.point && it.start < start+duration && start < it.end } || day.transfers.any { it.conflict && (it.fromId == draftItem.id || it.toId == draftItem.id) }
                    Surface(Modifier.offset(x = 3.dp, y = startY).width((maxWidth-6.dp).coerceAtLeast(1.dp)).height(draftHeight).testTag("calendar-draft"),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .90f),
                        border = BorderStroke(2.dp, if (conflict) Color(0xFFBA5B37) else MaterialTheme.colorScheme.primary),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)) {
                        Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp,
                            top = if (resizingStart && startY < markerHeight) markerHeight + 2.dp else 6.dp)) {
                            Text(draftItem.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${calendarTime(start)}–${calendarTime(start+duration)}${if(conflict) " · 重叠" else ""}", fontSize = 10.sp, maxLines = 1)
                        }
                    }
                    if (resizingStart || resizingEnd) {
                        val edgeY = if (resizingStart) startY else startY + draftHeight
                        val color = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.offset(x = 9.dp, y = edgeY - 1.5.dp)
                            .width((maxWidth - 18.dp).coerceAtLeast(1.dp)).height(3.dp)) {
                            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, StrokeCap.Round)
                        }
                        CalendarResizeIndicator(resizingStart,
                            Modifier.offset(x = (maxWidth - 28.dp) / 2,
                                y = if (resizingStart) (edgeY - markerHeight).coerceAtLeast(0.dp) else edgeY)
                                .size(28.dp, markerHeight))
                    }
                }
            }
        }
    }
}

/** A visual cue only: the whole card edge continues to own the pointer gesture. */
@Composable
private fun CalendarResizeIndicator(start: Boolean, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val arrow = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier.testTag(if (start) "calendar-resize-start" else "calendar-resize-end")) {
        drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
        val centerX = size.width / 2
        val tip = if (start) 5.dp.toPx() else size.height - 5.dp.toPx()
        val tail = if (start) size.height - 5.dp.toPx() else 5.dp.toPx()
        val wing = tip + (if (start) 4.dp.toPx() else -4.dp.toPx())
        val line = 1.75.dp.toPx()
        drawLine(arrow, Offset(centerX, tail), Offset(centerX, tip), line, StrokeCap.Round)
        drawLine(arrow, Offset(centerX - 4.dp.toPx(), wing), Offset(centerX, tip), line, StrokeCap.Round)
        drawLine(arrow, Offset(centerX + 4.dp.toPx(), wing), Offset(centerX, tip), line, StrokeCap.Round)
    }
}

private fun eventSubtitle(event: CalendarEvent): String = buildList {
    if (event.placeholder) add("${event.item.arrivalTime} 到达 · 停留待设")
    else add("${calendarTime(event.start)}–${calendarTime(event.end)}")
    if (event.continuation) add("第 ${event.sourceDayNumber} 天续住")
    if (event.continues) add(if (event.outsideTrip) "延续至旅行外" else "延续至次日")
    if (event.conflicts.isNotEmpty()) add("时间重叠")
    if (event.trafficConflict) add("交通可能来不及")
}.joinToString(" · ")
