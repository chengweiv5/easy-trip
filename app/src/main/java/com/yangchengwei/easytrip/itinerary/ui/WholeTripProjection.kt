package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.DayMapSnapshot

internal data class WholeTripProjection(
    val snapshots: List<DayMapSnapshot>,
    val collapsedCounts: Map<String, Int>,
    val originalOrders: Map<String, Int>,
    val representativeItemIds: Map<String, String>,
)

internal fun projectWholeTrip(days: List<TripDay>, snapshots: List<DayMapSnapshot>): WholeTripProjection {
    val byDay = snapshots.associateBy { it.itinerary.dayId }
    val ordered = days.sortedBy(TripDay::index).mapNotNull { byDay[it.id] }
    val representatives = mutableMapOf<String, ItineraryItem>()
    val kept = mutableSetOf<String>()
    val originalOrders = mutableMapOf<String, Int>()
    var previous: ItineraryItem? = null
    ordered.forEach { snapshot ->
        snapshot.itinerary.items.forEachIndexed { index, item ->
            val first = previous?.takeIf { item.place.id.isNotBlank() && it.place.id == item.place.id }
            if (first == null) {
                kept += item.id
                previous = item
            }
            representatives[item.id] = first ?: item
            originalOrders[item.id] = index + 1
        }
    }
    val collapsedCounts = mutableMapOf<String, Int>()
    val projected = ordered.map { snapshot ->
        val items = snapshot.itinerary.items.filter { it.id in kept }
        collapsedCounts[snapshot.itinerary.dayId] = snapshot.itinerary.items.size - items.size
        val adjacent = snapshot.itinerary.items.zipWithNext { from, to -> from.id to to.id }.toSet()
        val legs = snapshot.legs.mapNotNull { leg ->
            if (leg.tripDayId != snapshot.itinerary.dayId || leg.fromItemId to leg.toItemId !in adjacent) return@mapNotNull null
            val from = representatives.getValue(leg.fromItemId)
            val to = representatives.getValue(leg.toItemId)
            if (from.id == to.id) null else leg.copy(fromItemId = from.id, toItemId = to.id)
        }
        snapshot.copy(itinerary = snapshot.itinerary.copy(items = items), legs = legs)
    }
    return WholeTripProjection(projected, collapsedCounts, originalOrders, representatives.mapValues { it.value.id })
}
