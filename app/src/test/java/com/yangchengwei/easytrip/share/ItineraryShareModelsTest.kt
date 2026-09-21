package com.yangchengwei.easytrip.share

import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.itinerary.domain.*
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.trip.domain.*
import com.yangchengwei.easytrip.workspace.DayMapSnapshot
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ItineraryShareModelsTest {
    private val point = GeoPoint(30.25, 120.15)
    private fun item(id: String, location: GeoPoint = point) = ItineraryItem(id, ItineraryPlace("same", "西湖", "", location), null, null, "完整备注\n第二行")
    private fun trip() = TripWithDays("t", "杭州", LocalDate.of(2026,4,12), TravelMode.FLEXIBLE, listOf(TripDay("d2",1),TripDay("d1",0)))
    private fun leg(from: String, to: String, day: String = "d1") = RouteLegEntity("$from-$to", day,from,to,TransportMode.WALK,status=RouteStatus.PENDING,updatedAt=Instant.EPOCH)
    @Test fun preservesRepeatedVisitsEmptyDaysAndDailyNumbers() {
        val model=buildShareTrip(trip(),listOf(DayMapSnapshot(DayItinerary("d1","t",listOf(item("a"),item("b"))),emptyList())))
        assertEquals(listOf("d1","d2"),model.days.map { it.id })
        assertEquals(listOf("a","b"),model.days[0].stops.map { it.id })
        assertEquals(listOf(1,2),model.days[0].stops.map { it.number })
        assertTrue(model.days[1].stops.isEmpty())
        assertEquals("完整备注\n第二行",model.days[0].stops[0].note)
        assertNull(model.days[0].stops.last().leg)
        assertEquals(1,model.selected(ShareOptions("d2")).size)
    }
    @Test fun ignoresStaleAndCrossDayLegsAndUsesExplicitUnknowns() {
        val model=buildShareTrip(trip(),listOf(DayMapSnapshot(DayItinerary("d1","t",listOf(item("a"),item("b"))),listOf(leg("a","c"),leg("a","b","d2")))))
        val result=model.days[0].stops[0].leg!!
        assertEquals("时长待定",result.label)
        assertTrue(result.schematic)
    }
    @Test fun malformedRouteFallsBackButRetainsTransportOverrideAndNotes() {
        val entity=leg("a","b").copy(status=RouteStatus.SUCCESS,polyline="v1|NaN,120;30,120",durationSeconds=200,durationOverrideSeconds=900,note="带行李",distanceMeters=1000)
        val result=buildShareTrip(trip(),listOf(DayMapSnapshot(DayItinerary("d1","t",listOf(item("a"),item("b"))),listOf(entity)))).days[0].stops[0].leg!!
        assertEquals("步行 · 15 分钟 · 1 公里",result.label)
        assertEquals("带行李",result.note)
        assertTrue(result.schematic)
    }
    @Test fun successfulRouteRetainsPolylineAndDoesNotBecomeSchematic() {
        val entity=leg("a","b").copy(status=RouteStatus.SUCCESS,polyline="v1|30.25,120.15;30.26,120.16",durationSeconds=120,distanceMeters=100)
        val result=buildShareTrip(trip(),listOf(DayMapSnapshot(DayItinerary("d1","t",listOf(item("a"),item("b"))),listOf(entity)))).days[0].stops[0].leg!!
        assertFalse(result.schematic)
        assertEquals(listOf(GeoPoint(30.25,120.15),GeoPoint(30.26,120.16)),result.points)
        assertEquals("步行 · 2 分钟 · 100 米",result.label)
    }
    @Test fun missingCoordinateDoesNotJoinAcrossTheMissingStop() {
        val stops=buildShareTrip(trip(),listOf(DayMapSnapshot(DayItinerary("d1","t",listOf(item("a"),item("b",GeoPoint(Double.NaN,0.0)),item("c"))),emptyList()))).days[0].stops
        assertNull(stops[1].point)
        assertTrue(stops[0].leg!!.points.isEmpty())
        assertTrue(stops[1].leg!!.points.isEmpty())
    }
}
