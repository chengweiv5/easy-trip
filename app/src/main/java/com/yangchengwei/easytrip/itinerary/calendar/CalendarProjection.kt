package com.yangchengwei.easytrip.itinerary.calendar

import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi

/** A visit keeps its original day and identity even when displayed as a continuation. */
data class CalendarEvent(
    val sourceDayId: String,
    val sourceDayNumber: Int,
    val order: Int,
    val item: ItineraryItemUi,
    val displayDayId: String,
    val start: Int,
    val end: Int,
    val continuation: Boolean = false,
    val continues: Boolean = false,
    val outsideTrip: Boolean = false,
    val conflicts: Set<String> = emptySet(),
    val trafficConflict: Boolean = false,
    val lane: Int = 0,
    val laneCount: Int = 1,
    val group: Int = 0,
) {
    val key get() = "${sourceDayId}:${item.id}:$displayDayId"
    val placeholder get() = item.stayMinutes == null
    val point get() = item.stayMinutes == 0
    val crossDay get() = continuation || continues
    val canDrag get() = !crossDay && !point && (placeholder || end - start >= 30)
}

data class CalendarTransfer(
    val fromId: String, val toId: String, val minutes: Int?,
    val start: Int?, val end: Int?, val conflict: Boolean,
    val legId: String = "", val sourceDayId: String = "", val displayDayId: String = "",
    val continuation: Boolean = false, val continues: Boolean = false,
    val message: String? = null,
    val mode: TransportMode? = null,
    val fromName: String = "", val toName: String = "",
    // Estimated start/end remain intact for soft conflict warnings. These fields only allocate display time.
    val allocatedMinutes: Int? = null,
    val blockStart: Int? = null, val blockEnd: Int? = null,
)
data class CalendarDay(
    val dayId: String,
    val number: Int,
    val sourceItems: List<ItineraryItemUi>,
    val events: List<CalendarEvent>,
    val pending: List<ItineraryItemUi>,
    val transfers: List<CalendarTransfer>,
)

fun projectCalendarDays(rawDays: List<WholeTripDayUi>): List<CalendarDay> {
    val days = rawDays.sortedBy { it.dayNumber }
    val fragments = days.associate { it.dayId to mutableListOf<CalendarEvent>() }
    days.forEachIndexed { dayIndex, day ->
        day.items.forEachIndexed itemLoop@ { index, item ->
            val time = item.arrivalTime ?: return@itemLoop
            val start = time.hour * 60 + time.minute
            val duration = (item.stayMinutes ?: 60).coerceAtLeast(0).toLong()
            val realEnd = start.toLong() + duration
            // Iterate real travel dates only, including unusually long historical stays safely.
            for (offset in 0 until days.size - dayIndex) {
                val localStart = if (offset == 0) start else 0
                val remaining = realEnd - offset * 1440L
                if (offset > 0 && remaining <= 0) break
                val target = days[dayIndex + offset]
                val displayedEnd = remaining.coerceIn(localStart.toLong(), 1440L).toInt()
                fragments.getValue(target.dayId).add(CalendarEvent(
                    day.dayId, day.dayNumber, index + 1, item, target.dayId,
                    localStart, displayedEnd,
                    continuation = offset > 0,
                    continues = item.stayMinutes != null && remaining > 1440,
                    outsideTrip = item.stayMinutes != null && realEnd > (days.size - dayIndex) * 1440L,
                ))
                if (item.stayMinutes == null || duration == 0L) break
            }
        }
    }
    val transfers = days.associate { it.dayId to mutableListOf<CalendarTransfer>() }
    days.forEachIndexed { index, day ->
        day.items.zipWithNext().forEach pairs@ { (from, to) ->
            val leg = day.legs.firstOrNull { it.fromItemId == from.id && it.toItemId == to.id } ?: return@pairs
            val minutes = leg.effectiveDurationSeconds?.takeIf { it >= 0 }?.let { (it.toLong() + 59L).div(60).toInt() }
            val sourceEnd = from.arrivalTime?.let { time -> from.stayMinutes?.let { time.hour * 60L + time.minute + it } }
            val routeEnd = sourceEnd?.let { minutes?.let { duration -> it + duration } }
            val arrival = to.arrivalTime?.let { it.hour * 60 + it.minute }
            val allocatedEnd = if (sourceEnd == null || routeEnd == null) null else {
                val occupiedAt = days.drop(index).flatMapIndexed { offset, target ->
                    fragments.getValue(target.dayId).filter {
                        it.item.id != from.id && !it.placeholder && !it.point
                    }.mapNotNull { event ->
                        val start = offset * 1440L + event.start
                        val end = offset * 1440L + event.end
                        if (end > sourceEnd && start < routeEnd) maxOf(sourceEnd, start) else null
                    }
                }.minOrNull()
                minOf(routeEnd, arrival?.toLong()?.coerceAtLeast(sourceEnd) ?: routeEnd, occupiedAt ?: routeEnd)
            }
            val reference = CalendarTransfer(from.id, to.id, minutes, null, null,
                routeEnd != null && arrival != null && routeEnd > arrival,
                leg.id, day.dayId, day.dayId,
                message = when (val state = leg.state) {
                    is com.yangchengwei.easytrip.itinerary.ui.RouteLegUiState.Failed -> state.message
                    com.yangchengwei.easytrip.itinerary.ui.RouteLegUiState.WaitingForNetwork -> "交通用时待联网计算"
                    else -> null
                }, mode = leg.mode, fromName = from.name, toName = to.name,
                allocatedMinutes = allocatedEnd?.let { (it - requireNotNull(sourceEnd)).toInt() })
            if (sourceEnd == null || routeEnd == null || sourceEnd >= (days.size-index)*1440L) {
                transfers.getValue(day.dayId).add(reference)
            } else {
                for (offset in 0 until days.size-index) {
                    val lower = offset * 1440L
                    val upper = lower + 1440L
                    val zeroAtStart = routeEnd == sourceEnd && sourceEnd >= lower && sourceEnd < upper
                    if (!zeroAtStart && (sourceEnd >= upper || routeEnd <= lower)) continue
                    val target = days[index+offset]
                    val start = (sourceEnd-lower).coerceAtLeast(0).toInt()
                    val end = (routeEnd-lower).coerceAtMost(1440).toInt()
                    val overlapsEvent = end > start && fragments.getValue(target.dayId).any { !it.placeholder && !it.point && it.item.id != from.id && it.start < end && start < it.end }
                    val blockEnds = requireNotNull(allocatedEnd)
                    val hasBlock = sourceEnd < upper && (blockEnds > lower || blockEnds == sourceEnd && sourceEnd >= lower)
                    transfers.getValue(target.dayId).add(reference.copy(start=start, end=end, displayDayId=target.dayId,
                        continuation=sourceEnd<lower, continues=routeEnd>upper, conflict=reference.conflict || overlapsEvent,
                        blockStart = if (hasBlock) start else null,
                        blockEnd = if (hasBlock) (blockEnds - lower).coerceIn(0, 1440).toInt() else null))
                }
            }
        }
    }
    return days.map { day ->
        val dayTransfers = transfers.getValue(day.dayId)
        val events = layoutCalendarEvents(fragments.getValue(day.dayId)).map { event ->
            event.copy(trafficConflict = !event.placeholder && !event.point && dayTransfers.any { transfer ->
                transfer.fromId != event.item.id && transfer.start != null && transfer.end != null &&
                    transfer.end > transfer.start && transfer.start < event.end && event.start < transfer.end
            })
        }
        CalendarDay(day.dayId, day.dayNumber, day.items, events,
            day.items.filter { it.arrivalTime == null }, dayTransfers)
    }

}

fun layoutCalendarEvents(events: List<CalendarEvent>): List<CalendarEvent> {
    val real = events.filterNot { it.placeholder || it.point }
        .sortedWith(compareBy<CalendarEvent> { it.start }.thenBy { it.order }.thenBy { it.key })
    var groupIndex = 0
    var groupEnd = -1
    val laidOutReal = real.groupBy { event ->
        if (event.start >= groupEnd) groupIndex++
        groupEnd = maxOf(if (event.start >= groupEnd) -1 else groupEnd, event.end)
        groupIndex
    }.flatMap { (group, members) ->
        val ends = mutableListOf<Int>()
        val assigned = members.map { event ->
            val lane = ends.indexOfFirst { it <= event.start }.takeIf { it >= 0 } ?: ends.size
            if (lane == ends.size) ends.add(event.end) else ends[lane] = event.end
            event.copy(
                lane = lane,
                conflicts = members.filter { it.key != event.key && it.start < event.end && event.start < it.end }
                    .mapTo(mutableSetOf()) { it.item.id },
                group = group,
            )
        }
        assigned.map { it.copy(laneCount = ends.size.coerceAtLeast(1)) }
    }
    // Placeholders and zero-duration points are visual aids, not real occupancy. Layout them separately so
    // they cannot force real events into tiny lanes or into a false overlap aggregate.
    val decorations = events.filter { it.placeholder || it.point }
        .groupBy { it.start to it.end }
        .flatMap { (slot, members) ->
            members.sortedBy { it.order }.mapIndexed { index, event ->
                event.copy(lane = index, laneCount = members.size, group = -1 - slot.first * 2 - slot.second)
            }
        }
    return (laidOutReal + decorations).sortedWith(compareBy<CalendarEvent> { it.start }.thenBy { it.order }.thenBy { it.key })
}

fun calendarTime(minutes: Int): String = if (minutes == 1440) "24:00" else
    "%02d:%02d".format((minutes / 60).coerceIn(0, 23), minutes % 60)

fun calendarEndLabel(item: ItineraryItemUi): String? {
    val arrival = item.arrivalTime ?: return null
    val stay = item.stayMinutes ?: return null
    val end = arrival.hour * 60L + arrival.minute + stay
    val offset = end / 1440
    return (if (offset > 0) "${if (offset == 1L) "次日" else "$offset 天后"} " else "") + calendarTime((end % 1440).toInt())
}
