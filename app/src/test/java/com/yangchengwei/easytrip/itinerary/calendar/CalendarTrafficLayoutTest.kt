package com.yangchengwei.easytrip.itinerary.calendar

import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.RouteLegUi
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class CalendarTrafficLayoutTest {
    private fun item(id: String, time: String?, stay: Int? = 60) =
        ItineraryItemUi(id, id, "", time?.let(LocalTime::parse), stay)

    private fun day(arrival: String? = "16:30", seconds: Int? = 120, extra: List<ItineraryItemUi> = emptyList()): CalendarDay {
        val route = RouteLegUi("walk", "a", "b", TransportMode.WALK, RouteStatus.SUCCESS, 100, seconds, null)
        return projectCalendarDays(listOf(WholeTripDayUi("day", 1,
            listOf(item("a", "15:00"), item("b", arrival)) + extra, listOf(route)))).single()
    }

    private fun layout(day: CalendarDay, labelMinutes: Float = 13f) =
        day.transfers.single().layoutInGap(day.events, day.transfers, labelMinutes)

    @Test fun thirtyMinuteGapShowsTwoMinuteIntervalAndSeparateLabel() {
        val day = day()
        val layout = requireNotNull(layout(day))
        assertEquals(960, layout.start)
        assertEquals(962, layout.intervalEnd)
        assertTrue(layout.labelStart > layout.intervalEnd)
        assertTrue(layout.end <= 990)
        assertEquals(2, day.transfers.single().minutes)
        assertEquals(2, day.transfers.single().allocatedMinutes)
        assertFalse(day.transfers.single().conflict)
    }

    @Test fun twentyNineMinuteGapIsHiddenRegardlessOfTravelDuration() {
        listOf(120, 1800, 3600).forEach { assertNull(layout(day("16:29", it))) }
        assertNotNull(layout(day("16:30", 1800)))
    }

    @Test fun enoughIntervalHeightKeepsTextInsideActualTraffic() {
        val layout = requireNotNull(layout(day("17:30", 3600)))
        assertEquals(1020, layout.intervalEnd)
        assertTrue(layout.labelStart >= layout.start)
        assertEquals(layout.intervalEnd.toFloat(), layout.end, 0f)
    }

    @Test fun realVisitPlaceholderAndPointBoundTheUsableGap() {
        listOf(60, null, 0).forEach { stay ->
            assertNull(layout(day(extra = listOf(item("c", "16:20", stay)))))
        }
        assertNull(layout(day(extra = listOf(item("c", "15:30", null)))))
        assertNotNull(layout(day(extra = listOf(item("c", "16:30", 0)))))
    }

    @Test fun labelsCannotCoverAnotherTrafficInterval() {
        val day = day()
        val route = day.transfers.single()
        val other = route.copy(legId = "other", blockStart = 980, blockEnd = 982)
        assertNull(route.layoutInGap(day.events, listOf(route, other), 13f))
    }

    @Test fun unknownArrivalUnknownEstimateAndZeroAllocationStayInDetailsOnly() {
        assertNull(layout(day(arrival = null)))
        assertNull(layout(day(seconds = null)))
        assertNull(layout(day(seconds = 0)))
        assertNull(layout(day(arrival = "15:30")))
    }

    @Test fun largerTextMustFitWithinFreeGap() {
        assertNotNull(layout(day(), 26f))
        assertNull(layout(day(), 28f))
        assertNull(layout(day(), Float.NaN))
    }

    @Test fun continuationCannotBorrowUnrelatedOrNextDaySpace() {
        val day = day()
        val transfer = day.transfers.single().copy(sourceDayId = "previous")
        assertNull(transfer.layoutInGap(day.events, listOf(transfer), 13f))
        assertNull(day.transfers.single().layoutInGap(day.events.map {
            if (it.item.id == "b") it.copy(continuation = true) else it
        }, day.transfers, 13f))
    }
}
