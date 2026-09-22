package com.yangchengwei.easytrip.share

import com.yangchengwei.easytrip.core.model.TransportMode
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class ShareCalendarTest {
    private fun stop(id: String, time: String? = "09:00", stay: Int? = 60, seconds: Int? = null) =
        ShareStop(id, 1, "重复酒店", null, time?.let(LocalTime::parse), stay, null,
            seconds?.let { ShareLeg("ignored display label", null, emptyList(), true, TransportMode.WALK, it) })
    private fun trip(vararg stops: List<ShareStop>) = ShareTrip("t", "旅行", stops.mapIndexed { i, values ->
        ShareDay("d$i", i, null, values.mapIndexed { n, s -> s.copy(number = n + 1) })
    })
    @Test fun groupsEveryDateAndPreservesRepeatedVisitsAndNumbers() {
        val trip = trip(*Array(7) { listOf(stop("a$it"), stop("b$it", "12:00")) })
        val model = projectShareCalendar(trip, ShareOptions())
        assertEquals(listOf(3, 3, 1), model.groups.map { it.size })
        assertEquals(14, model.days.sumOf { it.visits.size })
        assertEquals(listOf(1, 2), model.days[0].visits.map { it.stop.number })
        assertEquals(540, model.start)
        assertEquals(780, model.end)
    }
    @Test fun singleDayIncludesEarlierVisitContinuationEvenIfDayHasNoOwnStops() {
        val model = projectShareCalendar(trip(listOf(stop("a", "23:30", 120)), emptyList()), ShareOptions("d1"))
        val v = model.days.single().visits.single()
        assertTrue(model.hasContent)
        assertEquals("d0", v.source.id)
        assertEquals(0, v.start)
        assertEquals(90, v.end)
        assertTrue(v.continuation)
    }
    @Test fun unknownStayNearMidnightNeverCreatesNextDayOrGuessedDeparture() {
        val model = projectShareCalendar(trip(listOf(stop("a", "23:30", null, 1500), stop("b", null)), emptyList()), ShareOptions())
        assertEquals(1440, model.days[0].visits.single().end)
        assertTrue(model.days[1].visits.isEmpty())
        val route = model.days[0].transfers.single()
        assertNull(route.departure)
        assertNull(route.start)
        assertFalse(route.conflict)
        assertEquals("", model.days[0].visits.single().rangeNote)
    }
    @Test fun allUntimedKeepsPendingAndOmitsTimeline() {
        val model = projectShareCalendar(trip(listOf(stop("a", null), stop("b", null))), ShareOptions())
        assertFalse(model.hasTimeline)
        assertEquals(2, model.days.single().pending.size)
    }
    @Test fun trafficUsesActualDurationAndKeepsFreeGap() {
        val model = projectShareCalendar(trip(listOf(stop("a", seconds = 1500), stop("b", "12:00"))), ShareOptions())
        val transfer = model.days.single().transfers.single()
        assertEquals(600, transfer.start)
        assertEquals(625, transfer.end)
        assertEquals(25, transfer.minutes)
        assertEquals("步行", transfer.mode)
        assertFalse(transfer.conflict)
    }
    @Test fun trafficConflictKeepsOriginalArrivalAndFullDuration() {
        val trip = trip(listOf(stop("a", seconds = 1500), stop("b", "10:10")))
        val snapshot = trip.copy()
        val model = projectShareCalendar(trip, ShareOptions())
        assertTrue(model.days.single().transfers.single().conflict)
        assertEquals(625, model.days.single().transfers.single().end)
        assertEquals(610, model.days.single().visits.last().start)
        assertEquals(snapshot, trip)
    }
    @Test fun unknownTravelTimeNeverBorrowsGap() {
        val from = stop("a").copy(leg = ShareLeg("驾车 · 时长待定", null, emptyList(), true, TransportMode.DRIVE))
        val t = projectShareCalendar(trip(listOf(from, stop("b", "12:00"))), ShareOptions()).days.single().transfers.single()
        assertEquals(600L, t.departure)
        assertNull(t.arrival)
        assertNull(t.start)
        assertFalse(t.conflict)
    }
    @Test fun crossMidnightTrafficSurvivesSingleDaySelectionAndRangeBoundaries() {
        val trip = trip(listOf(stop("a", "22:30", 60, 5400), stop("b", null)), emptyList())
        val day = projectShareCalendar(trip, ShareOptions("d1")).days.single()
        val t = day.transfers.single()
        assertEquals(0, t.start)
        assertEquals(60, t.end)
        assertEquals(1410L, t.departure)
        assertEquals(1500L, t.arrival)
        assertTrue(t.continuation)
    }
    @Test fun trafficStartingEntirelyOutsideSelectionRemainsInSourceDayDetails() {
        val trip = trip(listOf(stop("a", "23:30", 90, 1500), stop("b", null)), emptyList())
        val t = projectShareCalendar(trip, ShareOptions("d0")).days.single().transfers.single()
        assertNull(t.start)
        assertEquals(1500L, t.departure)
        assertEquals("出发超出本图所选日", t.rangeNote)
        val last = projectShareCalendar(trip.copy(days = trip.days.take(1)), ShareOptions()).days.single().transfers.single()
        assertEquals("出发超出旅行日期", last.rangeNote)
    }
    @Test fun trafficDepartingOnLaterDayStillIdentifiesOriginalDayAndStopNumbers() {
        val trip = trip(listOf(stop("a", "23:30", 90, 1500), stop("b", null)), emptyList())
        val transfer = projectShareCalendar(trip, ShareOptions("d1")).days.single().transfers.single()
        assertEquals(60, transfer.start)
        assertEquals(85, transfer.end)
        assertTrue(transfer.continuation)
        assertEquals("d0", transfer.source.id)
        assertEquals(1, transfer.from.number)
        assertEquals(2, transfer.to.number)
    }
    @Test fun zeroStayAndZeroTravelRemainVisibleReferences() {
        val model = projectShareCalendar(trip(listOf(stop("a", stay = 0, seconds = 0), stop("b", "12:00", 0))), ShareOptions())
        assertEquals(2, model.days.single().visits.size)
        assertEquals(540, model.days.single().transfers.single().end)
        assertFalse(model.days.single().transfers.single().conflict)
    }
    @Test fun notesDoNotAffectCalendarAndTripDatesAreNotInvented() {
        val trip = trip(listOf(stop("a", seconds = 901), stop("b", "12:00")), emptyList())
        assertEquals(projectShareCalendar(trip, ShareOptions()), projectShareCalendar(trip, ShareOptions(includeNotes = false)))
        assertNull(projectShareCalendar(trip, ShareOptions()).days[0].day.date)
        assertEquals(16, projectShareCalendar(trip, ShareOptions()).days[0].transfers.single().minutes)
    }
    @Test fun veryLongHistoricalDurationUsesWideArithmeticAndOnlyRealDates() {
        val model = projectShareCalendar(trip(listOf(stop("a", "23:30", Int.MAX_VALUE, Int.MAX_VALUE), stop("b", null)), emptyList()), ShareOptions())
        assertEquals(2, model.days.size)
        assertEquals(1440, model.days[1].visits.single().end)
        assertEquals("结束超出旅行日期", model.days[0].visits.single().rangeNote)
        assertEquals("出发超出旅行日期", model.days[0].transfers.single().rangeNote)
    }
}
