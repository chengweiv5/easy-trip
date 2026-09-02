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
enum class MapLayer { STANDARD, SATELLITE, SATELLITE_ROAD }

data class OccurrenceUi(
    val itemId: String,
    val dayId: String,
    val dayLabel: String,
    val order: Int,
    val placeName: String,
    val savedPlaceId: String,
)

enum class MapMarkerKind { UNSAVED_SEARCH, SAVED_PLACE_POOL, SAVED_ITINERARY }

data class MapMarkerUi(
    val key: String,
    val point: GeoPoint,
    val label: String,
    val occurrences: List<OccurrenceUi>,
    val kind: MapMarkerKind,
    val badgeText: String? = null,
    val isFocused: Boolean = false,
    val savedPlaceId: String? = null,
    val scheduled: Boolean = false,
)

fun formatOccurrenceBadge(orders: List<Int>): String = when {
    orders.size <= 3 -> orders.joinToString("·")
    else -> "${orders.first()} +${orders.size - 1}"
}

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

enum class ViewportReason { INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, VISIBLE_SET_CHANGED, SEARCH_FOCUS }

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

fun mapViewportPoints(scope: MapScope, model: MapUiModel): List<GeoPoint> =
    if (scope == MapScope.PLACE_POOL) {
        model.markers.map(MapMarkerUi::point).distinct()
    } else {
        (model.markers.map(MapMarkerUi::point) + model.polylines.flatMap(MapPolylineUi::points)).distinct()
    }

fun routePalette() = listOf(
    0xFF1565C0,
    0xFFC2185B,
    0xFF00897B,
    0xFF7B1FA2,
    0xFFC62828,
    0xFF0097A7,
    0xFF303F9F,
    0xFF2E7D32,
)

object MapUiModelMapper {
    private val palette = routePalette()

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
        restoredFocusedCandidate: PlaceCandidate? = null,
    ): MapUiModel {
        val focusedSearch = searchResults.firstOrNull { it.poiId == focusedPoiId }
            ?: restoredFocusedCandidate?.takeIf { it.poiId == focusedPoiId }
        val focusedPoint = focusedSearch?.point ?: restoredFocusedPoint
        val savedByPoiId = places.associateBy(SavedPlace::amapPoiId)
        val savedById = places.associateBy(SavedPlace::id)
        val savedByPoint = places.groupBy(SavedPlace::point).mapValues { (_, values) -> values.singleOrNull() }
        fun withSearchMarkers(baseMarkers: List<MapMarkerUi>): List<MapMarkerUi> {
            val markers = baseMarkers.toMutableList()
            val markerIndexByPoint = markers.indices.associateBy { markers[it].point }.toMutableMap()
            val markerIndexByPoiId = mutableMapOf<String, Int>()
            markers.forEachIndexed { index, marker ->
                savedByPoint[marker.point]?.amapPoiId?.let { markerIndexByPoiId[it] = index }
            }

            if (focusedPoiId != null && focusedPoint != null) {
                val saved = savedByPoiId[focusedPoiId] ?: savedByPoint[focusedPoint]
                val existingIndex = markerIndexByPoiId[focusedPoiId] ?: markerIndexByPoint[focusedPoint]
                if (existingIndex != null) {
                    markers[existingIndex] = markers[existingIndex].copy(isFocused = true)
                } else if (saved != null) {
                    val marker = MapMarkerUi(
                        key = "place-${saved.id}",
                        point = saved.point,
                        label = saved.name,
                        occurrences = emptyList(),
                        kind = if (scope == MapScope.PLACE_POOL) MapMarkerKind.SAVED_PLACE_POOL else MapMarkerKind.SAVED_ITINERARY,
                        isFocused = true,
                        savedPlaceId = saved.id,
                        scheduled = baseMarkers.any { it.savedPlaceId == saved.id && it.scheduled },
                    )
                    markerIndexByPoint[marker.point] = markers.size
                    markerIndexByPoiId[saved.amapPoiId] = markers.size
                    markers += marker
                } else {
                    val marker = MapMarkerUi(
                        key = "search-$focusedPoiId",
                        point = focusedPoint,
                        label = focusedSearch?.name ?: "搜索地点",
                        occurrences = emptyList(),
                        kind = MapMarkerKind.UNSAVED_SEARCH,
                        isFocused = true,
                    )
                    markerIndexByPoint[marker.point] = markers.size
                    markers += marker
                }
            }

            searchResults.asSequence()
                .filter { it.point != null && it.poiId != focusedPoiId }
                .filterNot { it.poiId in savedByPoiId }
                .distinctBy(PlaceCandidate::point)
                .filterNot { it.point in markerIndexByPoint }
                .forEach { result ->
                    val marker = MapMarkerUi(
                        key = "search-${result.poiId}",
                        point = requireNotNull(result.point),
                        label = result.name,
                        occurrences = emptyList(),
                        kind = MapMarkerKind.UNSAVED_SEARCH,
                    )
                    markerIndexByPoint[marker.point] = markers.size
                    markers += marker
                }
            return markers
        }
        if (scope == MapScope.PLACE_POOL) {
            val scheduledPlaceIds = snapshots
                .flatMap { snapshot -> snapshot.itinerary.items }
                .mapTo(mutableSetOf()) { item -> item.place.id }
            val savedMarkers = places.map {
                MapMarkerUi(
                    key = "place-${it.id}",
                    point = it.point,
                    label = it.name,
                    occurrences = emptyList(),
                    kind = MapMarkerKind.SAVED_PLACE_POOL,
                    savedPlaceId = it.id,
                    scheduled = it.id in scheduledPlaceIds,
                )
            }
            return MapUiModel(withSearchMarkers(savedMarkers))
        }
        val dayById = days.associateBy(TripDay::id)
        val visible = if (scope == MapScope.SINGLE_DAY) snapshots.filter { it.itinerary.dayId == selectedDayId } else snapshots
        val occurrences = visible.flatMap { snapshot ->
            val day = dayById[snapshot.itinerary.dayId]
            snapshot.itinerary.items.mapIndexed { index, item ->
                val dayIndex = day?.index ?: 0
                val dayLabel = startDate?.plusDays(dayIndex.toLong())?.toString() ?: "Day ${dayIndex + 1}"
                item.place.point to OccurrenceUi(item.id, snapshot.itinerary.dayId, dayLabel, index + 1, item.place.name, item.place.id)
            }
        }
        val wholeTripOrder = occurrences.mapIndexed { index, (_, occurrence) -> occurrence.itemId to index + 1 }.toMap()
        val markers = occurrences.groupBy(Pair<GeoPoint, OccurrenceUi>::first).map { (point, entries) ->
            val values = entries.map(Pair<GeoPoint, OccurrenceUi>::second)
            val orders = when (scope) {
                MapScope.SINGLE_DAY -> values.map(OccurrenceUi::order)
                MapScope.WHOLE_TRIP -> values.map { wholeTripOrder.getValue(it.itemId) }
                MapScope.PLACE_POOL -> error("Place pool returns before itinerary mapping")
            }.sorted()
            val occurrencePlaceId = values.map(OccurrenceUi::savedPlaceId).distinct().singleOrNull()
            val saved = occurrencePlaceId?.let(savedById::get)
            MapMarkerUi(
                key = saved?.let { "place-${it.id}" } ?: values.joinToString("|") { it.itemId },
                point = point,
                label = saved?.name ?: values.first().placeName,
                occurrences = values,
                kind = MapMarkerKind.SAVED_ITINERARY,
                badgeText = formatOccurrenceBadge(orders),
                savedPlaceId = saved?.id,
                scheduled = true,
            )
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
