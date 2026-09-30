package com.yangchengwei.easytrip.itinerary.data

import androidx.room.Query
import androidx.room.Transaction
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.itinerary.domain.*
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import java.time.LocalTime

/**
 * Shared by the two Room DAOs so route completion and its timing correction commit
 * atomically. Persisted opt-in protects manual edits, including edits saved unchanged.
 */
interface AutomaticTimingQueries {
    @Query("SELECT * FROM itinerary_items WHERE tripDayId=:dayId ORDER BY position,id")
    suspend fun automaticTimingItems(dayId: String): List<ItineraryItemEntity>

    @Query("SELECT * FROM route_legs WHERE tripDayId=:dayId")
    suspend fun automaticTimingLegs(dayId: String): List<RouteLegEntity>

    @Query("SELECT * FROM saved_places WHERE tripId=(SELECT tripId FROM trip_days WHERE id=:dayId)")
    suspend fun automaticTimingPlaces(dayId: String): List<SavedPlaceEntity>

    @Query("UPDATE itinerary_items SET autoTimingPending=0 WHERE id=:itemId")
    suspend fun freezeAutomaticTiming(itemId: String): Int

    @Query("""UPDATE itinerary_items SET arrivalTime=:arrival, stayDurationMinutes=:stay,
        autoTimingPending=:pending, timingWarning=:warning WHERE id=:itemId AND autoTimingPending=1""")
    suspend fun writeAutomaticTiming(itemId: String, arrival: LocalTime?, stay: Int, pending: Boolean, warning: String?): Int

    @Transaction
    suspend fun refreshAutomaticTimings(dayId: String) {
        val items = automaticTimingItems(dayId).toMutableList()
        if (items.none { it.autoTimingPending }) return
        val legs = automaticTimingLegs(dayId).associateBy { it.fromItemId to it.toItemId }
        val places = automaticTimingPlaces(dayId).associateBy { it.id }
        items.indices.forEach { index ->
            val item = items[index]
            if (!item.autoTimingPending) return@forEach
            val previous = items.getOrNull(index - 1)
            // Insertion/reordering must not reschedule pre-existing neighbours.
            if (item.autoTimingAnchorId != previous?.id) {
                freezeAutomaticTiming(item.id)
                items[index] = item.copy(autoTimingPending = false)
                return@forEach
            }
            val stay = item.stayDurationMinutes ?: DEFAULT_STAY_MINUTES
            var pending = false
            var warning: String? = null
            val arrival = if (previous == null) {
                DEFAULT_ARRIVAL
            } else if (previous.arrivalTime == null) {
                pending = previous.autoTimingPending
                warning = previous.timingWarning ?: TIMING_MISSING_ANCHOR
                null
            } else {
                val leg = legs[previous.id to item.id]
                val duration = leg?.durationOverrideSeconds?.takeIf { it >= 0 }
                    ?: leg?.durationSeconds?.takeIf { leg.status == RouteStatus.SUCCESS && it >= 0 }
                val from = places[previous.savedPlaceId]
                val to = places[item.savedPlaceId]
                if (leg == null || from == null || to == null) {
                    warning = TIMING_MISSING_ANCHOR
                    null
                } else {
                    pending = duration == null || previous.autoTimingPending
                    val seconds = duration ?: estimatedTravelSeconds(
                        haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude),
                        leg.selectedMode ?: leg.recommendedMode,
                    )
                    arrivalAfter(previous.arrivalTime, previous.stayDurationMinutes ?: DEFAULT_STAY_MINUTES, seconds)
                        .also { if (it == null) warning = TIMING_OUTSIDE_DAY }
                }
            }
            if (arrival != null && arrival.toSecondOfDay().toLong() + stay.toLong() * 60 > 86_400) {
                warning = TIMING_OUTSIDE_DAY
            }
            if (writeAutomaticTiming(item.id, arrival, stay, pending, warning) == 1) {
                items[index] = item.copy(arrivalTime = arrival, stayDurationMinutes = stay,
                    autoTimingPending = pending, timingWarning = warning)
            }
        }
    }
}
