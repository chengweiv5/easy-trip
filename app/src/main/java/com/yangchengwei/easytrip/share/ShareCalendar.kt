package com.yangchengwei.easytrip.share

/** Times are projected from the complete snapshot before selecting export days. */
internal data class ShareCalendar(
    val days: List<ShareCalendarDay>, val start: Int, val end: Int, val singleDay: Boolean,
) {
    val groups get() = days.chunked(3)
    val hasTimeline get() = days.any { it.visits.isNotEmpty() || it.transfers.any { t -> t.start != null } }
    val hasContent get() = days.any { it.day.stops.isNotEmpty() || it.visits.isNotEmpty() || it.transfers.isNotEmpty() }
}
internal data class ShareCalendarDay(
    val day: ShareDay, val visits: List<ShareCalendarVisit>, val transfers: List<ShareCalendarTransfer>,
) {
    val pending get() = day.stops.filter { it.arrival == null }
}
internal data class ShareCalendarVisit(
    val source: ShareDay, val stop: ShareStop, val start: Int, val end: Int,
    val continuation: Boolean, val rangeNote: String,
)
internal data class ShareCalendarTransfer(
    val source: ShareDay, val from: ShareStop, val to: ShareStop, val minutes: Int?,
    val departure: Long?, val arrival: Long?, val start: Int?, val end: Int?,
    val conflict: Boolean, val continuation: Boolean, val rangeNote: String,
) {
    val mode get() = from.leg?.mode?.shareLabel() ?: "交通"
    val key get() = "${source.id}/${from.id}/${to.id}"
}

internal fun shareClock(minute: Long): String {
    val offset = minute / 1440
    val prefix = when { offset == 0L -> ""; offset == 1L -> "次日 "; else -> "$offset 天后 " }
    return prefix + "%02d:%02d".format(java.util.Locale.ROOT, minute % 1440 / 60, minute % 60)
}
internal fun shareVisitTime(stop: ShareStop): String {
    val start = stop.arrival?.let { it.hour * 60L + it.minute } ?: return "到达待设" + (stop.stayMinutes?.let { " · 停留 $it 分钟" } ?: " · 停留待设")
    return shareClock(start) + (stop.stayMinutes?.let { "—${shareClock(start + it.coerceAtLeast(0))}" } ?: " 到达 · 停留待设")
}

internal fun projectShareCalendar(trip: ShareTrip, options: ShareOptions): ShareCalendar {
    val allDays = trip.days.sortedBy { it.index }
    val selected = allDays.filter { options.dayId == null || it.id == options.dayId }
    val tripEnd = (allDays.lastOrNull()?.index?.toLong()?.plus(1) ?: 0) * 1440
    data class Visit(val source: ShareDay, val stop: ShareStop, val start: Long, val end: Long)
    val rawVisits = allDays.flatMap { day -> day.stops.mapNotNull { stop ->
        stop.arrival?.let { time ->
            val start = day.index * 1440L + time.hour * 60 + time.minute
            val end = start + (stop.stayMinutes ?: 60).coerceAtLeast(0).toLong()
            Visit(day, stop, start, if (stop.stayMinutes == null) minOf(end, (day.index + 1L) * 1440) else end)
        }
    } }
    data class Transfer(val source: ShareDay, val from: ShareStop, val to: ShareStop, val minutes: Int?, val start: Long?, val end: Long?, val conflict: Boolean)
    val rawTransfers = allDays.flatMap { day -> day.stops.zipWithNext().mapNotNull { (from, to) ->
        val leg = from.leg ?: return@mapNotNull null
        val minutes = leg.durationSeconds?.takeIf { it >= 0 }?.let { ((it.toLong() + 59) / 60).toInt() }
        val departure = from.arrival?.let { time -> from.stayMinutes?.let { day.index * 1440L + time.hour * 60 + time.minute + it.coerceAtLeast(0) } }
        val arrival = departure?.let { start -> minutes?.let { start + it } }
        val nextArrival = to.arrival?.let { day.index * 1440L + it.hour * 60 + it.minute }
        val conflict = departure != null && arrival != null && (
            (nextArrival != null && arrival > nextArrival) || rawVisits.any {
                it.stop.id != from.id && it.stop.stayMinutes != null && it.end > it.start &&
                    arrival > departure && it.start < arrival && departure < it.end
            })
        Transfer(day, from, to, minutes, departure, arrival, conflict)
    } }
    val projected = selected.map { day ->
        val lower = day.index * 1440L
        val upper = lower + 1440
        val visits = rawVisits.filter { it.start < upper && (it.end > lower || it.start == it.end && it.start >= lower) }.map { v ->
            val note = when {
                v.stop.stayMinutes == null -> ""
                v.end > tripEnd -> "结束超出旅行日期"
                options.dayId != null && v.end > upper -> "结束超出本图所选日"
                v.end > upper -> "接续下一日"
                else -> ""
            }
            ShareCalendarVisit(v.source, v.stop, (v.start - lower).coerceAtLeast(0).toInt(), (v.end - lower).coerceAtMost(1440).toInt(), v.start < lower, note)
        }.sortedWith(compareBy({ it.start }, { it.stop.number }))
        val transfers = rawTransfers.mapNotNull { t ->
            val intersects = t.start != null && t.end != null && t.start < upper && (t.end > lower || t.start == t.end && t.start >= lower)
            val sourceReference = t.source.id == day.id && !intersects
            if (!intersects && !sourceReference) return@mapNotNull null
            val note = when {
                t.start != null && t.start >= tripEnd -> "出发超出旅行日期"
                t.end != null && t.end > tripEnd -> "结束超出旅行日期"
                t.start != null && t.start >= upper -> if (options.dayId != null) "出发超出本图所选日" else "后续日期出发"
                t.end != null && t.end > upper -> if (options.dayId != null) "结束超出本图所选日" else "接续下一日"
                else -> ""
            }
            ShareCalendarTransfer(t.source, t.from, t.to, t.minutes,
                t.start?.minus(t.source.index * 1440L), t.end?.minus(t.source.index * 1440L),
                if (intersects) (requireNotNull(t.start) - lower).coerceAtLeast(0).toInt() else null,
                if (intersects) (requireNotNull(t.end) - lower).coerceAtMost(1440).toInt() else null,
                t.conflict, intersects && t.source.index < day.index, note)
        }
        ShareCalendarDay(day, visits, transfers)
    }
    val starts = projected.flatMap { d -> d.visits.map { it.start } + d.transfers.mapNotNull { it.start } }
    val ends = projected.flatMap { d -> d.visits.map { it.end } + d.transfers.mapNotNull { it.end } }
    val start = (starts.minOrNull() ?: 480) / 60 * 60
    val end = maxOf(start + 180, ((ends.maxOrNull() ?: 1080) + 59) / 60 * 60).coerceAtMost(1440)
    return ShareCalendar(projected, start, end, options.dayId != null)
}
