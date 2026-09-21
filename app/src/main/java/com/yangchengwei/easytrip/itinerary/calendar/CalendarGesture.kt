package com.yangchengwei.easytrip.itinerary.calendar

import com.yangchengwei.easytrip.itinerary.domain.ItineraryTiming
import java.time.LocalTime
import kotlin.math.roundToInt

enum class CalendarDragMode { MOVE, START, END, PLACE }

/** Relative half-hour steps preserve historical minutes and leave untouched nulls intact. */
fun candidateTiming(original: ItineraryTiming, mode: CalendarDragMode, deltaMinutes: Float): ItineraryTiming? {
    val arrival = original.arrivalTime
    val duration = original.stayMinutes ?: 60
    if (mode == CalendarDragMode.PLACE) {
        if (arrival != null || duration <= 0 || duration > 1440) return null
        val start = (deltaMinutes / 30).roundToInt() * 30
        if (start !in 0..1439 || start + duration > 1440) return null
        return ItineraryTiming(LocalTime.of(start / 60, start % 60), duration)
    }
    if (arrival == null || duration < 0 || duration > 1440) return null
    val start = arrival.hour * 60 + arrival.minute
    val end = start + duration
    if (end > 1440 && original.stayMinutes != null) return null
    val steps = (deltaMinutes / 30).roundToInt()
    if (steps == 0) return original
    // Clamp in whole increments, preserving both the original minutes and the fixed edge.
    val bounds = when (mode) {
        CalendarDragMode.MOVE -> -start to minOf(1439 - start, 1440 - end)
        CalendarDragMode.START -> -start to duration - 1
        CalendarDragMode.END -> 1 - duration to 1440 - end
        else -> return null
    }
    val minSteps = kotlin.math.ceil(bounds.first / 30.0).toInt()
    val maxSteps = kotlin.math.floor(bounds.second / 30.0).toInt()
    if (minSteps > maxSteps || original.stayMinutes == null && end > 1440 && mode == CalendarDragMode.START) return original
    // Clamping may shorten an excessive placeholder only in the direction the user dragged; never reverse it.
    if (original.stayMinutes == null && end > 1440 && steps !in minSteps..maxSteps) return original
    if (steps > 0 && maxSteps < 0 || steps < 0 && minSteps > 0) return original
    val delta = steps.coerceIn(minSteps, maxSteps) * 30
    if (delta == 0) return original
    val nextStart = if (mode == CalendarDragMode.END) start else start + delta
    val nextDuration = when (mode) {
        CalendarDragMode.START -> duration - delta
        CalendarDragMode.END -> duration + delta
        else -> duration
    }
    return ItineraryTiming(arrival.plusMinutes((nextStart - start).toLong()), nextDuration)
}
