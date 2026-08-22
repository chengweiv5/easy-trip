package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.PolylineCodec
import com.yangchengwei.easytrip.trip.domain.TripDay
import java.time.LocalDate

enum class MapScope { PLACE_POOL, SINGLE_DAY, WHOLE_TRIP }

data class OccurrenceUi(
    val itemId: String,
    val dayId: String,
    val dayLabel: String,
    val order: Int,
    val placeName: String,
)

data class MapMarkerUi(
    val key: String,
    val point: GeoPoint,
    val label: String,
    val occurrences: List<OccurrenceUi>,
)

data class MapPolylineUi(
    val legId: String,
    val dayId: String,
    val points: List<GeoPoint>,
    val colorArgb: Long,
)

data class CorruptRoute(val legId: String, val version: Long)
data class DayMapSnapshot(val itinerary: DayItinerary, val legs: List<RouteLegEntity>)
data class MapUiModel(
    val markers: List<MapMarkerUi> = emptyList(),
    val polylines: List<MapPolylineUi> = emptyList(),
    val corruptRoutes: List<CorruptRoute> = emptyList(),
)

object MapUiModelMapper {
    private val palette = listOf(
        0xFFE53935,
        0xFF1E88E5,
        0xFF43A047,
        0xFFFB8C00,
        0xFF8E24AA,
        0xFF00ACC1,
        0xFFF4511E,
        0xFF3949AB,
    )

    fun map(
        scope: MapScope,
        places: List<SavedPlace>,
        days: List<TripDay>,
        snapshots: List<DayMapSnapshot>,
        selectedDayId: String? = null,
        startDate: LocalDate? = null,
        searchResults: List<PlaceCandidate> = emptyList(),
    ): MapUiModel {
        val savedPoiIds = places.mapTo(mutableSetOf(), SavedPlace::amapPoiId)
        val searchMarkers = searchResults.filterNot { it.poiId in savedPoiIds }.map { MapMarkerUi("search-${it.poiId}", it.point, it.name, emptyList()) }
        if (scope == MapScope.PLACE_POOL) {
            val savedMarkers = places.map { MapMarkerUi("place-${it.id}", it.point, it.name, emptyList()) }
            return MapUiModel(savedMarkers + searchMarkers)
        }
        val dayById = days.associateBy(TripDay::id)
        val visible = if (scope == MapScope.SINGLE_DAY) snapshots.filter { it.itinerary.dayId == selectedDayId } else snapshots
        val occurrences = visible.flatMap { snapshot ->
            val day = dayById[snapshot.itinerary.dayId]
            snapshot.itinerary.items.mapIndexed { index, item ->
                val dayIndex = day?.index ?: 0
                val dayLabel = startDate?.plusDays(dayIndex.toLong())?.toString() ?: "Day ${dayIndex + 1}"
                item.place.point to OccurrenceUi(item.id, snapshot.itinerary.dayId, dayLabel, index + 1, item.place.name)
            }
        }
        val markers = occurrences.groupBy(Pair<GeoPoint, OccurrenceUi>::first).map { (point, entries) ->
            val values = entries.map(Pair<GeoPoint, OccurrenceUi>::second)
            val label = if (scope == MapScope.SINGLE_DAY) values.joinToString(", ") { it.order.toString() }
            else values.joinToString(", ") { "${it.dayLabel}-${it.order}" }
            MapMarkerUi(values.joinToString("|") { it.itemId }, point, label, values)
        }
        val corrupt = mutableListOf<CorruptRoute>()
        val polylines = visible.flatMap { snapshot ->
            val dayIndex = dayById[snapshot.itinerary.dayId]?.index ?: 0
            snapshot.legs.mapNotNull { leg ->
                if (leg.status != RouteStatus.SUCCESS || leg.polyline == null) return@mapNotNull null
                PolylineCodec.decode(leg.polyline).fold(
                    onSuccess = { points ->
                        if (points.size < 2 || points.any { !it.latitude.isFinite() || !it.longitude.isFinite() }) {
                            corrupt += CorruptRoute(leg.id, leg.version)
                            null
                        } else MapPolylineUi(leg.id, leg.tripDayId, points, palette[dayIndex.mod(palette.size)])
                    },
                    onFailure = { corrupt += CorruptRoute(leg.id, leg.version); null },
                )
            }
        }
        return MapUiModel(markers + searchMarkers, polylines, corrupt)
    }
}
