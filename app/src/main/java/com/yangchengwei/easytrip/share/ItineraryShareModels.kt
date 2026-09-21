package com.yangchengwei.easytrip.share

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.ui.formatDistance
import com.yangchengwei.easytrip.itinerary.ui.formatDuration
import com.yangchengwei.easytrip.route.domain.PolylineCodec
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import com.yangchengwei.easytrip.workspace.DayMapSnapshot
import com.yangchengwei.easytrip.workspace.routeColorForDay
import java.time.LocalDate
import java.time.LocalTime

data class ShareOptions(val dayId: String? = null, val includeNotes: Boolean = true)
data class ShareTrip(val id: String, val name: String, val days: List<ShareDay>) {
    fun selected(options: ShareOptions): List<ShareDay> = days.filter { options.dayId == null || it.id == options.dayId }
}
data class ShareDay(val id: String, val index: Int, val date: LocalDate?, val stops: List<ShareStop>) {
    val color: Int get() = routeColorForDay(index).toInt()
    val points: List<GeoPoint> get() = stops.mapNotNull { it.point }
    val hasSchematicLegs: Boolean get() = stops.any { it.leg?.schematic == true }
}
data class ShareStop(
    val id: String, val number: Int, val name: String, val point: GeoPoint?,
    val arrival: LocalTime?, val stayMinutes: Int?, val note: String?, val leg: ShareLeg?,
)
data class ShareLeg(val label: String, val note: String?, val points: List<GeoPoint>, val schematic: Boolean)

internal fun GeoPoint.isShareable(): Boolean = latitude.isFinite() && longitude.isFinite() &&
    latitude in -90.0..90.0 && longitude in -180.0..180.0

fun buildShareTrip(trip: TripWithDays, snapshots: List<DayMapSnapshot>): ShareTrip = ShareTrip(
    trip.id, trip.name, trip.days.sortedBy { it.index }.map { day ->
        val snapshot = snapshots.firstOrNull { it.itinerary.dayId == day.id && it.itinerary.tripId == trip.id }
        val items = snapshot?.itinerary?.items.orEmpty()
        ShareDay(day.id, day.index, trip.startDate?.plusDays(day.index.toLong()), items.mapIndexed { index, item ->
            val next = items.getOrNull(index + 1)
            val entity = next?.let { to -> snapshot?.legs?.firstOrNull {
                it.tripDayId == day.id && it.fromItemId == item.id && it.toItemId == to.id
            } }
            val point = item.place.point.takeIf { it.isShareable() }
            val nextPoint = next?.place?.point?.takeIf { it.isShareable() }
            val route = entity?.takeIf { it.status == RouteStatus.SUCCESS }?.polyline
                ?.let { PolylineCodec.decode(it).getOrNull() }
                ?.takeIf { it.size >= 2 && it.all(GeoPoint::isShareable) }
            val leg = next?.let {
                val mode = entity?.let { it.selectedMode ?: it.recommendedMode }
                val duration = entity?.durationOverrideSeconds?.takeIf { it >= 0 }
                    ?: entity?.takeIf { it.status == RouteStatus.SUCCESS }?.durationSeconds?.takeIf { it >= 0 }
                val distance = entity?.takeIf { it.status == RouteStatus.SUCCESS }?.distanceMeters?.takeIf { it >= 0 }
                ShareLeg(
                    listOfNotNull(mode?.shareLabel(), duration?.let(::formatDuration) ?: "时长待定", distance?.let(::formatDistance)).joinToString(" · "),
                    entity?.note?.takeIf(String::isNotBlank),
                    if (point != null && nextPoint != null) route ?: listOf(point, nextPoint) else emptyList(),
                    route == null,
                )
            }
            ShareStop(item.id, index + 1, item.place.name, point, item.arrivalTime,
                item.stayMinutes, item.note?.takeIf(String::isNotBlank), leg)
        })
    },
)
private fun TransportMode.shareLabel() = when (this) {
    TransportMode.WALK -> "步行"
    TransportMode.TAXI -> "打车"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TRANSIT -> "公交"
}
