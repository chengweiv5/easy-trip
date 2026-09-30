package com.yangchengwei.easytrip.itinerary.domain

import com.yangchengwei.easytrip.core.model.TransportMode
import java.time.LocalTime
import kotlin.math.ceil

internal const val DEFAULT_STAY_MINUTES = 60
internal val DEFAULT_ARRIVAL: LocalTime = LocalTime.of(8, 0)
internal const val TIMING_OUTSIDE_DAY = "预计时间超出当天，请调整"
internal const val TIMING_MISSING_ANCHOR = "前站时间待定，请调整"

/** A provisional estimate only, never a replacement for a route service result. */
internal fun estimatedTravelSeconds(distanceMeters: Double, mode: TransportMode): Int {
    if (distanceMeters < 1.0) return 0
    val (metersPerMinute, overheadMinutes) = when (mode) {
        TransportMode.WALK -> 75.0 to 0
        TransportMode.TAXI, TransportMode.DRIVE -> 500.0 to 5
        TransportMode.TRANSIT -> 330.0 to 10
    }
    return (ceil(distanceMeters * 1.3 / metersPerMinute).toLong() + overheadMinutes)
        .coerceAtMost(Int.MAX_VALUE.toLong() / 60).toInt() * 60
}

internal fun arrivalAfter(arrival: LocalTime, stayMinutes: Int, travelSeconds: Int): LocalTime? {
    val seconds = arrival.toSecondOfDay().toLong() + stayMinutes.toLong() * 60 + travelSeconds
    val rounded = (seconds + 59) / 60 * 60
    return rounded.takeIf { it in 0 until 86_400 }?.let(LocalTime::ofSecondOfDay)
}
