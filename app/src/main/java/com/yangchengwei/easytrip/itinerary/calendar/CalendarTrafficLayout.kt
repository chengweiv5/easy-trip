package com.yangchengwei.easytrip.itinerary.calendar

/** Display geometry only; the route estimate and allocated travel interval remain unchanged. */
internal data class CalendarTrafficLayout(
    val start: Int,
    val intervalEnd: Int,
    val labelStart: Float,
    val end: Float,
)

internal fun CalendarTransfer.layoutInGap(
    events: List<CalendarEvent>,
    transfers: List<CalendarTransfer>,
    labelMinutes: Float,
): CalendarTrafficLayout? {
    val start = blockStart ?: return null
    val end = blockEnd ?: return null
    if (end <= start || !labelMinutes.isFinite() || labelMinutes <= 0f) return null
    val destination = events.firstOrNull {
        it.item.id == toId && it.sourceDayId == sourceDayId && !it.continuation
    } ?: return null
    val occupiedAt = events.filter { it.item.id != fromId }.mapNotNull { event ->
        if (event.end > start || event.point && event.start >= start) maxOf(start, event.start) else null
    }.minOrNull()
    val nextTrafficAt = transfers.filter { it != this }.mapNotNull { other ->
        val otherStart = other.blockStart
        val otherEnd = other.blockEnd
        if (otherStart != null && otherEnd != null && otherEnd > otherStart && otherEnd > start) {
            maxOf(start, otherStart)
        } else null
    }.minOrNull()
    val gapEnd = minOf(destination.start, occupiedAt ?: 1440, nextTrafficAt ?: 1440)
    if (gapEnd - start < 30 || end > gapEnd) return null
    val labelStart = if (end - start >= labelMinutes) {
        start + (end - start - labelMinutes) / 2f
    } else {
        end + 1f / CALENDAR_MINUTE_DP
    }
    val displayEnd = maxOf(end.toFloat(), labelStart + labelMinutes)
    return if (displayEnd <= gapEnd) CalendarTrafficLayout(start, end, labelStart, displayEnd) else null
}
