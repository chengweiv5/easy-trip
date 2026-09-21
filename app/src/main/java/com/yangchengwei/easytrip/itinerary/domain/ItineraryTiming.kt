package com.yangchengwei.easytrip.itinerary.domain

import java.time.LocalTime

data class ItineraryTiming(val arrivalTime: LocalTime?, val stayMinutes: Int?)
data class ItineraryTimingChange(
    val tripId: String,
    val dayId: String,
    val itemId: String,
    val before: ItineraryTiming,
    val after: ItineraryTiming,
) {
    fun reversed() = copy(before = after, after = before)
}
