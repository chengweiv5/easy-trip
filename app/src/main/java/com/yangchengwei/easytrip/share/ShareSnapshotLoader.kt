package com.yangchengwei.easytrip.share

import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.workspace.DayMapSnapshot
import kotlinx.coroutines.flow.first

fun interface ShareSnapshotSource {
    suspend fun load(tripId: String): ShareTrip
}

/** Copies a complete, ordered snapshot once. Export never writes or replans routes. */
class ShareSnapshotLoader(
    private val trips: TripRepository,
    private val itineraries: ItineraryRepository,
    private val routes: RouteLegRepository,
) : ShareSnapshotSource {
    override suspend fun load(tripId: String): ShareTrip {
        val trip = requireNotNull(trips.observeTrip(tripId).first()) { "旅行不存在" }
        val days = trip.days.map { day ->
            DayMapSnapshot(itineraries.observeDay(day.id).first(), routes.observeDay(day.id).first())
        }
        // Verify source rows as well as day metadata before retaining this export snapshot.
        // Background route updates are safe to retry; never mix old endpoints with a new leg.
        val verified = trip.days.map { day ->
            DayMapSnapshot(itineraries.observeDay(day.id).first(), routes.observeDay(day.id).first())
        }
        check(trips.observeTrip(tripId).first() == trip && verified == days) { "行程已更新，请重新生成" }
        return buildShareTrip(trip, days)
    }
}
