package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.PolylineCodec
import com.yangchengwei.easytrip.trip.domain.TripDay
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapUiModelMapperTest {
    private val shared = GeoPoint(39.9, 116.4)
    private val other = GeoPoint(39.91, 116.41)
    private val hotel = ItineraryPlace("hotel", "酒店", "", shared)
    private val museum = ItineraryPlace("museum", "博物馆", "", other)
    private val days = listOf(TripDay("day-1", 0), TripDay("day-2", 1))

    @Test fun `place pool markers retain saved identity without badges`() {
        val places = listOf(saved("hotel", "酒店", shared), saved("museum", "博物馆", other))
        val model = MapUiModelMapper.map(MapScope.PLACE_POOL, places, days, emptyList())

        assertEquals(setOf("place-hotel", "place-museum"), model.markers.map { it.key }.toSet())
        assertTrue(model.markers.all { it.kind == MapMarkerKind.SAVED_PLACE_POOL })
        assertTrue(model.markers.all { it.badgeText == null })
        assertTrue(model.markers.none { it.scheduled })
    }

    @Test fun `place pool markers distinguish scheduled from saved only`() {
        val places = listOf(saved("hotel", "酒店", shared), saved("museum", "博物馆", other))
        val itinerary = snapshot("day-1", listOf(item("i1", hotel)))

        val model = MapUiModelMapper.map(MapScope.PLACE_POOL, places, days, listOf(itinerary))

        assertEquals(true, model.markers.single { it.key == "place-hotel" }.scheduled)
        assertEquals(false, model.markers.single { it.key == "place-museum" }.scheduled)
    }

    @Test fun `focused place pool marker retains scheduled state without duplication`() {
        val place = saved("hotel", "酒店", shared)
        val result = com.yangchengwei.easytrip.place.amap.PlaceCandidate("poi-hotel", "酒店搜索结果", "", shared, null)
        val itinerary = snapshot("day-1", listOf(item("i1", hotel)))

        val model = MapUiModelMapper.map(
            MapScope.PLACE_POOL,
            listOf(place),
            days,
            listOf(itinerary),
            searchResults = listOf(result),
            focusedPoiId = result.poiId,
        )

        val marker = model.markers.single()
        assertTrue(marker.scheduled)
        assertTrue(marker.isFocused)
        assertEquals("place-hotel", marker.key)
    }

    @Test fun `single day numbers items and merges equal coordinates without losing occurrences`() {
        val day = snapshot("day-1", listOf(item("i1", hotel), item("i2", hotel), item("i3", hotel), item("i4", museum)))
        val model = MapUiModelMapper.map(MapScope.SINGLE_DAY, emptyList(), days, listOf(day), "day-1")
        assertEquals(2, model.markers.size)
        val merged = model.markers.single { it.point == shared }
        assertEquals(MapMarkerKind.SAVED_ITINERARY, merged.kind)
        assertEquals("1·2·3", merged.badgeText)
        assertEquals(listOf("i1", "i2", "i3"), merged.occurrences.map { it.itemId })
        assertEquals(listOf(1, 2, 3), merged.occurrences.map { it.order })
    }

    @Test fun `place pool adds unsaved search results to map`() {
        val places = listOf(saved("hotel", "酒店", shared))
        val result = com.yangchengwei.easytrip.place.amap.PlaceCandidate("search", "餐厅", "", other, null)
        val model = MapUiModelMapper.map(MapScope.PLACE_POOL, places, days, emptyList(), searchResults = listOf(result))
        assertEquals(setOf("place-hotel", "search-search"), model.markers.map { it.key }.toSet())
        assertEquals(MapMarkerKind.UNSAVED_SEARCH, model.markers.single { it.key == "search-search" }.kind)
    }

    @Test fun `focused saved search result retains saved identity and adds focus`() {
        val places = listOf(saved("hotel", "酒店", shared))
        val result = com.yangchengwei.easytrip.place.amap.PlaceCandidate("poi-hotel", "酒店搜索结果", "", shared, null)

        val model = MapUiModelMapper.map(
            MapScope.PLACE_POOL,
            places,
            days,
            emptyList(),
            searchResults = listOf(result),
            focusedPoiId = result.poiId,
        )

        val marker = model.markers.single { it.point == shared }
        assertEquals("place-hotel", marker.key)
        assertEquals(MapMarkerKind.SAVED_PLACE_POOL, marker.kind)
        assertEquals(null, marker.badgeText)
        assertTrue(marker.isFocused)
    }

    @Test fun `focused saved result absent from selected day retains saved identity`() {
        val result = com.yangchengwei.easytrip.place.amap.PlaceCandidate("poi-hotel", "酒店搜索结果", "", shared, null)
        val selectedDay = snapshot("day-1", listOf(item("i4", museum)))

        val model = MapUiModelMapper.map(
            MapScope.SINGLE_DAY,
            listOf(saved("hotel", "酒店", shared)),
            days,
            listOf(selectedDay),
            selectedDayId = "day-1",
            searchResults = listOf(result),
            focusedPoiId = result.poiId,
        )

        val marker = model.markers.single { it.point == shared }
        assertEquals("place-hotel", marker.key)
        assertEquals(MapMarkerKind.SAVED_ITINERARY, marker.kind)
        assertEquals(null, marker.badgeText)
        assertTrue(marker.isFocused)
    }

    @Test fun `focused result in selected day rekeys occurrence marker without losing occurrences`() {
        val result = com.yangchengwei.easytrip.place.amap.PlaceCandidate("poi-hotel", "酒店搜索结果", "", shared, null)
        val selectedDay = snapshot("day-1", listOf(item("i1", hotel), item("i2", hotel)))

        val model = MapUiModelMapper.map(
            MapScope.SINGLE_DAY,
            listOf(saved("hotel", "酒店", shared)),
            days,
            listOf(selectedDay),
            selectedDayId = "day-1",
            searchResults = listOf(result),
            focusedPoiId = result.poiId,
        )

        val marker = model.markers.single { it.point == shared }
        assertEquals("place-hotel", marker.key)
        assertEquals(MapMarkerKind.SAVED_ITINERARY, marker.kind)
        assertEquals("1·2", marker.badgeText)
        assertTrue(marker.isFocused)
        assertEquals(listOf("i1", "i2"), marker.occurrences.map { it.itemId })
    }

    @Test fun `focused saved result in whole trip rekeys occurrence marker without duplicate coordinate`() {
        val result = com.yangchengwei.easytrip.place.amap.PlaceCandidate("poi-hotel", "酒店搜索结果", "", shared, null)
        val snapshots = listOf(snapshot("day-1", listOf(item("i1", hotel))), snapshot("day-2", listOf(item("i2", hotel))))

        val model = MapUiModelMapper.map(
            MapScope.WHOLE_TRIP,
            listOf(saved("hotel", "酒店", shared)),
            days,
            snapshots,
            searchResults = listOf(result),
            focusedPoiId = result.poiId,
        )

        val marker = model.markers.single { it.point == shared }
        assertEquals("place-hotel", marker.key)
        assertEquals(MapMarkerKind.SAVED_ITINERARY, marker.kind)
        assertEquals("1·2", marker.badgeText)
        assertTrue(marker.isFocused)
        assertEquals(listOf("i1", "i2"), marker.occurrences.map { it.itemId })
    }

    @Test fun `same poi and coordinate collision retains every occurrence on one saved marker`() {
        val result = com.yangchengwei.easytrip.place.amap.PlaceCandidate("poi-hotel", "酒店搜索结果", "", shared, null)
        val snapshot = snapshot("day-1", listOf(item("i1", hotel), item("i2", hotel)))

        val model = MapUiModelMapper.map(
            MapScope.SINGLE_DAY,
            listOf(saved("hotel", "酒店", shared)),
            days,
            listOf(snapshot),
            selectedDayId = "day-1",
            searchResults = listOf(result),
        )

        val marker = model.markers.single { it.point == shared }
        assertEquals("place-hotel", marker.key)
        assertEquals(listOf("i1", "i2"), marker.occurrences.map { it.itemId })
        assertEquals("1·2", marker.badgeText)
    }

    @Test fun `focused search result wins when another result shares its unoccupied coordinate`() {
        val focused = com.yangchengwei.easytrip.place.amap.PlaceCandidate("focused", "焦点", "", shared, null)
        val otherResult = com.yangchengwei.easytrip.place.amap.PlaceCandidate("other", "其他", "", shared, null)

        val model = MapUiModelMapper.map(
            MapScope.SINGLE_DAY,
            emptyList(),
            days,
            emptyList(),
            selectedDayId = "day-1",
            searchResults = listOf(otherResult, focused),
            focusedPoiId = focused.poiId,
        )

        assertEquals(1, model.markers.count { it.point == shared })
        assertEquals("search-focused", model.markers.single { it.point == shared }.key)
    }

    @Test fun `itinerary marker uses occurrence saved place identity instead of coordinate`() {
        val occurrence = ItineraryPlace("hotel", "酒店", "", shared)
        val sameCoordinate = saved("other", "同坐标地点", shared)

        val model = MapUiModelMapper.map(
            MapScope.SINGLE_DAY,
            listOf(saved("hotel", "酒店", shared), sameCoordinate),
            days,
            listOf(snapshot("day-1", listOf(item("i1", occurrence)))),
            selectedDayId = "day-1",
        )

        val marker = model.markers.single()
        assertEquals("place-hotel", marker.key)
        assertEquals("hotel", marker.savedPlaceId)
        assertEquals("酒店", marker.label)
    }

    @Test fun `itinerary marker with different saved places at one coordinate has no saved identity`() {
        val first = ItineraryPlace("hotel", "酒店", "", shared)
        val second = ItineraryPlace("other", "同坐标地点", "", shared)

        val model = MapUiModelMapper.map(
            MapScope.SINGLE_DAY,
            listOf(saved("hotel", "酒店", shared), saved("other", "同坐标地点", shared)),
            days,
            listOf(snapshot("day-1", listOf(item("i1", first), item("i2", second)))),
            selectedDayId = "day-1",
        )

        val marker = model.markers.single()
        assertEquals(null, marker.savedPlaceId)
        assertTrue(!marker.key.startsWith("place-"))
        assertEquals(listOf("i1", "i2"), marker.occurrences.map { it.itemId })
    }

    @Test fun `restored focused candidate supplies marker label without live search results`() {
        val focused = com.yangchengwei.easytrip.place.amap.PlaceCandidate("focused", "故宫", "", shared, null)

        val model = MapUiModelMapper.map(
            MapScope.PLACE_POOL,
            emptyList(),
            days,
            emptyList(),
            focusedPoiId = focused.poiId,
            restoredFocusedCandidate = focused,
        )

        val marker = model.markers.single()
        assertEquals("search-focused", marker.key)
        assertEquals("故宫", marker.label)
        assertTrue(marker.isFocused)
    }

    @Test fun `whole trip numbers places continuously across days`() {
        val first = snapshot("day-1", listOf(item("i1", hotel), item("i2", museum)))
        val second = snapshot("day-2", listOf(item("i3", hotel), item("i4", museum)))

        val model = MapUiModelMapper.map(MapScope.WHOLE_TRIP, emptyList(), days, listOf(first, second))

        assertEquals("1·3", model.markers.single { it.point == shared }.badgeText)
        assertEquals("2·4", model.markers.single { it.point == other }.badgeText)
        assertTrue(model.markers.all { it.kind == MapMarkerKind.SAVED_ITINERARY })
        assertTrue(model.markers.all { it.scheduled })
    }

    @Test fun `whole trip adds one midpoint label for each day with valid routes`() {
        val first = snapshot("day-1", listOf(item("i1", hotel), item("i2", museum)), listOf(leg("l1", "day-1", RouteStatus.SUCCESS, validPolyline())))
        val second = snapshot("day-2", listOf(item("i3", hotel), item("i4", museum)), listOf(leg("l2", "day-2", RouteStatus.SUCCESS, validPolyline()), leg("l3", "day-2", RouteStatus.FAILED, validPolyline())))

        val model = MapUiModelMapper.map(MapScope.WHOLE_TRIP, emptyList(), days, listOf(first, second))

        assertEquals(setOf("l1", "l2"), model.polylines.map { it.legId }.toSet())
        assertEquals(2, model.polylines.map { it.colorArgb }.distinct().size)
        assertEquals(listOf("第一天", "第二天"), model.routeLabels.map { it.label })
        assertEquals(model.polylines.map { it.colorArgb }, model.routeLabels.map { it.colorArgb })
    }

    @Test fun `single day does not add route labels`() {
        val first = snapshot("day-1", listOf(item("i1", hotel), item("i2", museum)), listOf(leg("l1", "day-1", RouteStatus.SUCCESS, validPolyline())))

        val model = MapUiModelMapper.map(MapScope.SINGLE_DAY, emptyList(), days, listOf(first), "day-1")

        assertTrue(model.routeLabels.isEmpty())
    }

    @Test fun `single day viewport points include route geometry beyond markers`() {
        val routePoint = GeoPoint(40.5, 117.2)
        val day = snapshot(
            "day-1",
            listOf(item("i1", hotel), item("i2", museum)),
            listOf(leg("l1", "day-1", RouteStatus.SUCCESS, PolylineCodec.encode(listOf(shared, routePoint, other)))),
        )
        val model = MapUiModelMapper.map(MapScope.SINGLE_DAY, emptyList(), days, listOf(day), "day-1")

        assertEquals(setOf(shared, other, routePoint), mapViewportPoints(MapScope.SINGLE_DAY, model).toSet())
    }

    @Test fun `whole trip viewport points include every route geometry point`() {
        val firstRoutePoint = GeoPoint(40.5, 117.2)
        val secondRoutePoint = GeoPoint(30.8, 120.9)
        val first = snapshot(
            "day-1",
            listOf(item("i1", hotel), item("i2", museum)),
            listOf(leg("l1", "day-1", RouteStatus.SUCCESS, PolylineCodec.encode(listOf(shared, firstRoutePoint, other)))),
        )
        val second = snapshot(
            "day-2",
            listOf(item("i3", museum), item("i4", hotel)),
            listOf(leg("l2", "day-2", RouteStatus.SUCCESS, PolylineCodec.encode(listOf(other, secondRoutePoint, shared)))),
        )
        val model = MapUiModelMapper.map(MapScope.WHOLE_TRIP, emptyList(), days, listOf(first, second))

        assertEquals(
            setOf(shared, other, firstRoutePoint, secondRoutePoint),
            mapViewportPoints(MapScope.WHOLE_TRIP, model).toSet(),
        )
    }

    @Test fun `place pool viewport points ignore route geometry`() {
        val model = MapUiModel(
            markers = listOf(MapMarkerUi("place-hotel", shared, "酒店", emptyList(), MapMarkerKind.SAVED_PLACE_POOL)),
            polylines = listOf(MapPolylineUi("leg", "day-1", listOf(shared, other), 0L)),
        )

        assertEquals(listOf(shared), mapViewportPoints(MapScope.PLACE_POOL, model))
    }

    @Test fun `route palette excludes map road colors and day four is purple`() {
        val colors = routePalette()
        val forbidden = setOf(0xFFFB8C00, 0xFFF4511E, 0xFFFFFFFF, 0xFF9E9E9E)

        assertTrue(colors.none { it in forbidden })
        assertEquals(0xFF7B1FA2, colors[3])
    }

    @Test fun `whole trip uses distinct day colors and excludes failed routes`() {
        val first = snapshot("day-1", listOf(item("i1", hotel), item("i2", museum)), listOf(leg("l1", "day-1", RouteStatus.SUCCESS, validPolyline())))
        val second = snapshot("day-2", listOf(item("i3", hotel), item("i4", museum)), listOf(leg("l2", "day-2", RouteStatus.SUCCESS, validPolyline()), leg("l3", "day-2", RouteStatus.FAILED, validPolyline())))
        val model = MapUiModelMapper.map(MapScope.WHOLE_TRIP, emptyList(), days, listOf(first, second))
        assertEquals(setOf("l1", "l2"), model.polylines.map { it.legId }.toSet())
        assertEquals(2, model.polylines.map { it.colorArgb }.distinct().size)
        assertEquals(2, model.markers.single { it.point == shared }.occurrences.size)
    }

    @Test fun `dated trip occurrences use actual dates`() {
        val day = snapshot("day-2", listOf(item("i", hotel)))
        val model = MapUiModelMapper.map(MapScope.WHOLE_TRIP, emptyList(), days, listOf(day), startDate = java.time.LocalDate.of(2026, 8, 22))
        assertEquals("2026-08-23", model.markers.single().occurrences.single().dayLabel)
    }

    @Test fun `empty successful polyline is reported for repair`() {
        val day = snapshot("day-1", listOf(item("i1", hotel), item("i2", museum)), listOf(leg("empty", "day-1", RouteStatus.SUCCESS, PolylineCodec.encode(emptyList()))))
        val model = MapUiModelMapper.map(MapScope.SINGLE_DAY, emptyList(), days, listOf(day), "day-1")
        assertEquals(listOf(CorruptRoute("empty", 3)), model.corruptRoutes)
    }

    @Test fun `corrupt successful polyline is reported for repair instead of crashing`() {
        val day = snapshot("day-1", listOf(item("i1", hotel), item("i2", museum)), listOf(leg("bad", "day-1", RouteStatus.SUCCESS, "broken")))
        val model = MapUiModelMapper.map(MapScope.SINGLE_DAY, emptyList(), days, listOf(day), "day-1")
        assertTrue(model.polylines.isEmpty())
        assertEquals(listOf(CorruptRoute("bad", 3)), model.corruptRoutes)
    }

    private fun saved(id: String, name: String, point: GeoPoint) = SavedPlace(id, "trip", "poi-$id", name, "", point, "", emptyList())
    private fun item(id: String, place: ItineraryPlace) = ItineraryItem(id, place, null, null)
    private fun snapshot(dayId: String, items: List<ItineraryItem>, legs: List<RouteLegEntity> = emptyList()) = DayMapSnapshot(DayItinerary(dayId, "trip", items), legs)
    private fun validPolyline() = PolylineCodec.encode(listOf(shared, other))
    private fun leg(id: String, dayId: String, status: RouteStatus, polyline: String?) = RouteLegEntity(id, dayId, "from-$id", "to-$id", TransportMode.WALK, status = status, polyline = polyline, version = 3, updatedAt = Instant.EPOCH)
}
