package com.yangchengwei.easytrip.itinerary.calendar

import com.yangchengwei.easytrip.itinerary.ui.*
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class CalendarProjectionTest {
    private fun item(id: String, time: String? = "09:00", stay: Int? = 60) =
        ItineraryItemUi(id, "Hotel", "", time?.let(LocalTime::parse), stay, placeId = "same-hotel")
    private fun day(id: String, number: Int, vararg items: ItineraryItemUi) = WholeTripDayUi(id, number, items.toList(), emptyList())

    @Test fun preservesEveryVisitAndOnlyActualTravelDays() {
        val result = projectCalendarDays(listOf(day("a", 1, item("a")), day("b", 2, item("b"))))
        assertEquals(listOf("a", "b"), result.flatMap { it.events }.map { it.item.id })
        assertEquals(30, projectCalendarDays((1..30).map { day("$it", it) }).size)
    }
    @Test fun unsetAndZeroAreDifferentAndNeverCreateConflicts() {
        val result = projectCalendarDays(listOf(day("a", 1, item("unset", stay = null), item("zero", stay = 0), item("real"), item("pending", null, 90)))).single()
        assertEquals(60, result.events.first { it.item.id == "unset" }.let { it.end - it.start })
        assertTrue(result.events.all { it.conflicts.isEmpty() })
        assertEquals(listOf("pending"), result.pending.map { it.id })
    }
    @Test fun placeholdersNeverAffectRealOverlapLanesOrAggregationGroups() {
        val result=projectCalendarDays(listOf(day("a",1,item("r1"),item("r2","09:10"),item("r3","09:20"),item("unset","09:00",null)))).single()
        val real=result.events.filterNot { it.placeholder }
        assertEquals(setOf(3),real.map { it.laneCount }.toSet())
        assertFalse(result.events.first { it.placeholder }.conflicts.isNotEmpty())
        assertTrue(result.events.first { it.placeholder }.group < 0)
    }
    @Test fun touchingBoundariesDoNotConflictButOverlapsHaveIndependentLanes() {
        val result = projectCalendarDays(listOf(day("a", 1, item("a"), item("b", "10:00"), item("c", "10:30")))).single()
        assertTrue(result.events.first { it.item.id == "a" }.conflicts.isEmpty())
        assertEquals(setOf("c"), result.events.first { it.item.id == "b" }.conflicts)
        assertEquals(2, result.events.first { it.item.id == "b" }.laneCount)
    }
    @Test fun crossMidnightLinksSourceAndDoesNotInventExtraDay() {
        val result = projectCalendarDays(listOf(day("a", 1, item("overnight", "23:30", 120)), day("b", 2, item("outside", "23:00", Int.MAX_VALUE))))
        val fragments = result.flatMap { it.events }.filter { it.item.id == "overnight" }
        assertEquals(listOf(30, 90), fragments.map { it.end - it.start })
        assertTrue(fragments.all { it.sourceDayId == "a" && it.crossDay })
        assertTrue(result.last().events.first { it.item.id == "outside" }.outsideTrip)
        assertEquals(2, result.size)
    }
    @Test fun unknownStayAtMidnightDoesNotGenerateNextDayPlaceholder() {
        val result = projectCalendarDays(listOf(day("a", 1, item("unset", "23:30", null)), day("b", 2)))
        assertEquals(1440, result.first().events.single().end)
        assertTrue(result.last().events.isEmpty())
    }
    @Test fun trafficCrossingMidnightKeepsIdentityAndOnlyRealDayFragments() {
        val leg = RouteLegUi("leg", "a", "b", com.yangchengwei.easytrip.core.model.TransportMode.WALK,
            com.yangchengwei.easytrip.core.model.RouteStatus.SUCCESS, 100, 3600, null)
        val raw = day("one", 1, item("a", "23:00",30),item("b",null)).copy(legs=listOf(leg))
        val result = projectCalendarDays(listOf(raw,day("two",2)))
        val transfers = result.flatMap { it.transfers }
        assertEquals(listOf(30,30),transfers.map { it.end!!-it.start!! })
        assertTrue(transfers.all { it.legId=="leg" && it.sourceDayId=="one" })
        assertTrue(transfers.last().continuation)
        assertEquals(1,projectCalendarDays(listOf(raw)).single().transfers.size)
    }
    @Test fun trafficRequiresAdjacentSourceItemsAndUsesOverride() {
        val leg = RouteLegUi("leg", "a", "b", com.yangchengwei.easytrip.core.model.TransportMode.WALK,
            com.yangchengwei.easytrip.core.model.RouteStatus.SUCCESS, 100, 600, null, durationOverrideSeconds = 3600)
        val raw = day("a", 1, item("a"), item("b", "10:30")).copy(legs = listOf(leg))
        val route = projectCalendarDays(listOf(raw)).single().transfers.single()
        assertEquals(60, route.minutes); assertTrue(route.conflict)
        val unknown = projectCalendarDays(listOf(raw.copy(items = listOf(item("a", stay = null), item("b"))))).single().transfers.single()
        assertNull(unknown.start); assertNull(unknown.end); assertFalse(unknown.conflict)
        assertTrue(projectCalendarDays(listOf(raw.copy(items = raw.items.reversed()))).single().transfers.isEmpty())
    }
    private fun trafficDay(vararg items: ItineraryItemUi, seconds: Int? = 3600) =
        day("one", 1, *items).copy(legs = listOf(RouteLegUi("leg", "a", "b",
            com.yangchengwei.easytrip.core.model.TransportMode.WALK,
            com.yangchengwei.easytrip.core.model.RouteStatus.SUCCESS, 100, seconds, null)))

    @Test fun squeezedTrafficRetainsEstimateAndDoesNotMoveVisits() {
        val raw = trafficDay(item("a"), item("b", "10:15"))
        val result = projectCalendarDays(listOf(raw)).single()
        val transfer = result.transfers.single()
        assertEquals(60, transfer.minutes)
        assertEquals(600, transfer.start); assertEquals(660, transfer.end)
        assertEquals(600, transfer.blockStart); assertEquals(615, transfer.blockEnd)
        assertEquals(15, transfer.allocatedMinutes)
        assertEquals(raw.items, result.sourceItems)
        assertTrue(result.events.first { it.item.id == "b" }.trafficConflict)
    }
    @Test fun ongoingVisitOrEarlierDestinationCanOccupyAllTraffic() {
        val ongoing = projectCalendarDays(listOf(trafficDay(item("a"), item("b", "12:00"), item("c", "09:45")))).single().transfers.single()
        assertEquals(0, ongoing.allocatedMinutes); assertEquals(ongoing.blockStart, ongoing.blockEnd)
        val earlier = projectCalendarDays(listOf(trafficDay(item("a"), item("b", "09:00")))).single().transfers.single()
        assertEquals(0, earlier.allocatedMinutes); assertTrue(earlier.conflict)
    }
    @Test fun placeholderAndZeroPointDoNotOccupyTrafficButDestinationArrivalBoundsIt() {
        val result = projectCalendarDays(listOf(trafficDay(item("a"), item("b", "10:30", null),
            item("c", "10:00", null), item("d", "10:00", 0)))).single().transfers.single()
        assertEquals(30, result.allocatedMinutes)
        val zeroTarget = projectCalendarDays(listOf(trafficDay(item("a"), item("b", "10:00", 0)))).single().transfers.single()
        assertEquals(0, zeroTarget.allocatedMinutes)
    }
    @Test fun zeroDurationStillHasTrafficMarkerAndUnknownValuesHaveNoInventedBlock() {
        val zero = projectCalendarDays(listOf(trafficDay(item("a"), item("b", "11:00"), seconds = 0))).single().transfers.single()
        assertEquals(0, zero.allocatedMinutes); assertEquals(600, zero.blockStart); assertEquals(600, zero.blockEnd)
        listOf(trafficDay(item("a", stay = null), item("b")), trafficDay(item("a"), item("b"), seconds = null))
            .forEach { raw -> val unknown = projectCalendarDays(listOf(raw)).single().transfers.single()
                assertNull(unknown.blockStart); assertNull(unknown.blockEnd); assertNull(unknown.allocatedMinutes) }
    }
    @Test fun midnightTrafficKeepsEstimateButDropsFullySqueezedContinuationBlock() {
        val source = trafficDay(item("a", "23:00", 30), item("b", null))
        val result = projectCalendarDays(listOf(source, day("two", 2, item("c", "00:00"))))
        val parts = result.flatMap { it.transfers }
        assertEquals(2, parts.size)
        assertEquals(30, parts.first().allocatedMinutes)
        assertEquals(1410, parts.first().blockStart); assertEquals(1440, parts.first().blockEnd)
        assertNull(parts.last().blockStart); assertNull(parts.last().blockEnd)
        assertTrue(result.last().events.single().trafficConflict)
    }
    @Test fun midnightAllocationCanEndInsideContinuationAndKeepsSourceIdentity() {
        val source = trafficDay(item("a", "23:00", 30), item("b", null))
        val result = projectCalendarDays(listOf(source, day("two", 2, item("c", "00:15"))))
        val parts = result.flatMap { it.transfers }
        assertEquals(listOf(30, 15), parts.map { it.blockEnd!! - it.blockStart!! })
        assertTrue(parts.all { it.allocatedMinutes == 45 && it.minutes == 60 && it.sourceDayId == "one" })
    }

    @Test fun zeroMinuteTrafficCannotCreateFalseIntervalConflict() {
        val result = projectCalendarDays(listOf(trafficDay(item("a"), item("b", "11:00"), item("c", "09:45"), seconds = 0))).single()
        assertFalse(result.transfers.single().conflict)
        assertTrue(result.events.none { it.trafficConflict })
    }

}
