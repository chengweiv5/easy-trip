package com.yangchengwei.easytrip.itinerary.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi

@Composable
internal fun CalendarGrid(
    days: List<CalendarDay>, trafficDays: List<CalendarDay>, single: String?, expandedGroup: Int?, focusId: String?,
    draftItem: ItineraryItemUi?, draftDayId: String?, draftTiming: ItineraryTiming?, draftMode: CalendarDragMode?,
    modifier: Modifier, editable: Boolean,
    onOpen: (String, String) -> Unit, onExpand: (Int) -> Unit, onOpenGroup: (String, String) -> Unit,
    onStart: (String, ItineraryItemUi, CalendarDragMode, Offset, Float) -> Unit,
    onMove: (Offset) -> Unit, onEnd: (Boolean) -> Unit,
    onStep: (String, ItineraryItemUi, CalendarDragMode, Int) -> Unit,
    onKeyboardStart: (String, ItineraryItemUi) -> Unit,
    onKeyboardStep: (Int) -> Unit, onKeyboardEnd: (Boolean) -> Unit,
    onTraffic: (CalendarTransfer) -> Unit,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val footer = (48f * LocalDensity.current.fontScale + 20f).dp.coerceAtLeast(72.dp)
    Row(modifier.height(24.dp * CALENDAR_HOUR_DP + footer).testTag("calendar-grid")) {
        Box(Modifier.width(CALENDAR_TIME_GUTTER_DP.dp).fillMaxHeight()) {
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
                val eventWidth = maxWidth
                val narrowTraffic = single == null
                val currentEvents = trafficDays.firstOrNull { it.dayId == day.dayId }?.events.orEmpty()
                val trafficWidth = (eventWidth - 6.dp).coerceAtLeast(1.dp)
                val density = LocalDensity.current
                val visibleTransfers = day.transfers.filter {
                    it.trafficLayout(trafficWidth, narrowTraffic, density, currentEvents, day.transfers) != null
                }
                CalendarTraffic(day.dayId, day.transfers, currentEvents, narrowTraffic,
                    Modifier.offset(x = 3.dp).width(trafficWidth).fillMaxHeight(),
                    enabled = editable && draftItem == null, onOpen = onTraffic)
                day.events.groupBy { it.group }.forEach { (group, events) ->
                    val hasPlaceholder = events.any { it.placeholder }
                    val intersectsOtherKind = events.any { event -> day.events.any { other ->
                        other.placeholder != event.placeholder && other.start < event.end && event.start < other.end
                    } }
                    // Reserve separate visual columns when an uncertain placeholder shares time with real events.
                    val groupWidth = if (intersectsOtherKind) eventWidth * (if (hasPlaceholder) .4f else .6f) else eventWidth
                    val groupLeft = if (intersectsOtherKind && hasPlaceholder) eventWidth * .6f else 0.dp
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
                        val trafficConflict = (currentEvents.firstOrNull { it.key == event.key } ?: event)
                            .hasTrafficConflict(visibleTransfers)
                        CalendarCard("${event.order}. ${event.item.name}", eventSubtitle(event, trafficConflict),
                            Modifier.offset(x = groupLeft + groupWidth / event.laneCount * event.lane + 3.dp, y = (event.start * CALENDAR_MINUTE_DP).dp)
                                .drawWithContent { if (draftItem?.id != event.item.id) drawContent() }
                                .width(width).height(if (event.placeholder) CALENDAR_HOUR_DP.dp else if (event.point) 2.dp else ((event.end-event.start)*CALENDAR_MINUTE_DP).dp.coerceAtLeast(1.dp)).testTag("calendar-event-${event.key}"),
                            dashed = event.placeholder, conflict = event.conflicts.isNotEmpty() || trafficConflict,
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
                    val startY = (start * CALENDAR_MINUTE_DP).dp
                    val draftHeight = (duration * CALENDAR_MINUTE_DP).dp.coerceAtLeast(2.dp)
                    val visitConflict = day.events.any { it.item.id != draftItem.id && !it.placeholder && !it.point && it.start < start+duration && start < it.end }
                    val trafficConflict = visibleTransfers.any { it.conflict && (it.fromId == draftItem.id || it.toId == draftItem.id) }
                    val conflict = visitConflict || trafficConflict
                    val conflictLabel = if (visitConflict) " · 日程重叠" else if (trafficConflict) " · 交通预留不足" else ""
                    Surface(Modifier.offset(x = 3.dp, y = startY).width((eventWidth-6.dp).coerceAtLeast(1.dp)).height(draftHeight).testTag("calendar-draft"),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .90f),
                        border = BorderStroke(2.dp, if (conflict) Color(0xFFBA5B37) else MaterialTheme.colorScheme.primary),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)) {
                        CalendarCardText(draftItem.name, "${calendarTime(start)}–${calendarTime(start+duration)}$conflictLabel",
                            conflict, duration < 45)
                    }
                    if (resizingStart || resizingEnd) {
                        val edgeY = if (resizingStart) startY else startY + draftHeight
                        val color = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.offset(x = 3.dp, y = edgeY - 1.5.dp)
                            .width((eventWidth - 6.dp).coerceAtLeast(1.dp)).height(3.dp)) {
                            drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}

private fun eventSubtitle(event: CalendarEvent, showTrafficConflict: Boolean): String = buildList {
    if (event.placeholder) add("${event.item.arrivalTime} 到达 · 停留待设")
    else add("${calendarTime(event.start)}–${calendarTime(event.end)}")
    if (event.continuation) add("第 ${event.sourceDayNumber} 天续住")
    if (event.continues) add(if (event.outsideTrip) "延续至旅行外" else "延续至次日")
    if (event.conflicts.isNotEmpty()) add("时间重叠")
    if (showTrafficConflict) add("交通可能来不及")
}.joinToString(" · ")
