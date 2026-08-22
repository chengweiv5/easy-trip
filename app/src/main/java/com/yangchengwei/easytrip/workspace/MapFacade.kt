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
enum class MapLayer { STANDARD, SATELLITE_ROAD }

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

data class MapRouteLabelUi(
    val dayId: String,
    val point: GeoPoint,
    val label: String,
    val colorArgb: Long,
)

data class CorruptRoute(val legId: String, val version: Long)

enum class ViewportReason { INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, SEARCH_FOCUS }

data class MapViewportRequest(
    val id: Long,
    val reason: ViewportReason,
    val points: List<GeoPoint>,
    val singlePointZoom: Float? = null,
)

const val SEARCH_FOCUS_ZOOM = 15f

data class MapInteractionState(
    val focusedPoiId: String? = null,
    val highlightedMarkerKey: String? = null,
    val viewportRequest: MapViewportRequest? = null,
)

sealed interface MapInteractionAction {
    data class FocusSearchResult(val poiId: String, val point: GeoPoint) : MapInteractionAction
    data class ReconcileSearchResults(val poiIds: Set<String>) : MapInteractionAction
}

fun reduceMapInteraction(state: MapInteractionState, action: MapInteractionAction): MapInteractionState = when (action) {
    is MapInteractionAction.FocusSearchResult -> state.copy(
        focusedPoiId = action.poiId,
        highlightedMarkerKey = "search-${action.poiId}",
        viewportRequest = MapViewportRequest(
            id = (state.viewportRequest?.id ?: 0L) + 1L,
            reason = ViewportReason.SEARCH_FOCUS,
            points = listOf(action.point),
            singlePointZoom = SEARCH_FOCUS_ZOOM,
        ),
    )
    is MapInteractionAction.ReconcileSearchResults -> if (state.focusedPoiId != null && state.focusedPoiId !in action.poiIds) {
        state.copy(focusedPoiId = null, highlightedMarkerKey = null)
    } else state
}

data class DayMapSnapshot(val itinerary: DayItinerary, val legs: List<RouteLegEntity>)
data class MapUiModel(
    val markers: List<MapMarkerUi> = emptyList(),
    val polylines: List<MapPolylineUi> = emptyList(),
    val routeLabels: List<MapRouteLabelUi> = emptyList(),
    val highlightedMarkerKey: String? = null,
    val viewportRequest: MapViewportRequest? = null,
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
        focusedPoiId: String? = null,
        restoredFocusedPoint: GeoPoint? = null,
    ): MapUiModel {
        val focusedSearch = searchResults.firstOrNull { it.poiId == focusedPoiId }
        val focusedPointCandidate = focusedSearch?.point ?: restoredFocusedPoint
        val savedPoiIds = places.mapTo(mutableSetOf(), SavedPlace::amapPoiId)
        fun withSearchMarkers(baseMarkers: List<MapMarkerUi>): List<MapMarkerUi> {
            val focusedPoint = focusedPointCandidate
            val focusedKey = focusedPoiId?.let { "search-$it" }
            val focusedBaseIndex = focusedPoint?.let { point -> baseMarkers.indexOfFirst { it.point == point } } ?: -1
            val canonicalBase = if (focusedBaseIndex >= 0) {
                baseMarkers.mapIndexed { index, marker ->
                    if (index == focusedBaseIndex) marker.copy(key = requireNotNull(focusedKey)) else marker
                }
            } else baseMarkers
            val occupiedPoints = canonicalBase.mapTo(mutableSetOf(), MapMarkerUi::point)
            val additions = mutableListOf<MapMarkerUi>()
            if (focusedPoint != null && focusedKey != null && focusedBaseIndex < 0) {
                additions += MapMarkerUi(focusedKey, focusedPoint, focusedSearch?.name ?: "搜索地点", emptyList())
                occupiedPoints += focusedPoint
            }
            additions += searchResults.asSequence()
                .filter { it.point != null }
                .filterNot { it.poiId in savedPoiIds || it.poiId == focusedPoiId }
                .distinctBy(PlaceCandidate::point)
                .filterNot { it.point in occupiedPoints }
                .map { MapMarkerUi("search-${it.poiId}", requireNotNull(it.point), it.name, emptyList()) }
            return canonicalBase + additions
        }
        if (scope == MapScope.PLACE_POOL) {
            val savedMarkers = places.map { MapMarkerUi("place-${it.id}", it.point, it.name, emptyList()) }
            return MapUiModel(withSearchMarkers(savedMarkers))
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
        val wholeTripOrder = occurrences.mapIndexed { index, (_, occurrence) -> occurrence.itemId to index + 1 }.toMap()
        val markers = occurrences.groupBy(Pair<GeoPoint, OccurrenceUi>::first).map { (point, entries) ->
            val values = entries.map(Pair<GeoPoint, OccurrenceUi>::second)
            val label = when (scope) {
                MapScope.SINGLE_DAY -> values.joinToString(", ") { it.order.toString() }
                MapScope.WHOLE_TRIP -> values.joinToString(", ") { wholeTripOrder.getValue(it.itemId).toString() }
                MapScope.PLACE_POOL -> error("Place pool returns before itinerary mapping")
            }
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
        val routeLabels = if (scope == MapScope.WHOLE_TRIP) {
            polylines.groupBy(MapPolylineUi::dayId).mapNotNull { (dayId, dayLines) ->
                val points = dayLines.flatMap(MapPolylineUi::points)
                val dayIndex = dayById[dayId]?.index ?: return@mapNotNull null
                points.getOrNull(points.size / 2)?.let { point ->
                    MapRouteLabelUi(dayId, point, dayOrdinalLabel(dayIndex), dayLines.first().colorArgb)
                }
            }
        } else emptyList()
        return MapUiModel(
            markers = withSearchMarkers(markers),
            polylines = polylines,
            routeLabels = routeLabels,
            corruptRoutes = corrupt,
        )
    }

    private fun dayOrdinalLabel(dayIndex: Int): String {
        val number = dayIndex + 1
        val numerals = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val text = when {
            number < 10 -> numerals[number]
            number == 10 -> "十"
            number < 20 -> "十${numerals[number % 10]}"
            number < 100 -> "${numerals[number / 10]}十${if (number % 10 == 0) "" else numerals[number % 10]}"
            else -> number.toString()
        }
        return "第${text}天"
    }
}
