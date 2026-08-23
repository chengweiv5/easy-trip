package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.trip.domain.TripDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceNavigationTest {
    @Test fun `place pool derives place pool map scope`() {
        assertEquals(MapScope.PLACE_POOL, WorkspaceSection.PLACE_POOL.toMapScope(ItineraryScope.WholeTrip))
    }

    @Test fun `itinerary scopes derive whole trip and single day map scopes`() {
        assertEquals(MapScope.WHOLE_TRIP, WorkspaceSection.ITINERARY.toMapScope(ItineraryScope.WholeTrip))
        assertEquals(MapScope.SINGLE_DAY, WorkspaceSection.ITINERARY.toMapScope(ItineraryScope.Day("day-2")))
    }

    @Test fun `itinerary scopes encode and decode exact persisted values`() {
        assertEquals("WHOLE_TRIP", encodeItineraryScope(ItineraryScope.WholeTrip))
        assertEquals("DAY:day-2", encodeItineraryScope(ItineraryScope.Day("day-2")))
        assertEquals(ItineraryScope.WholeTrip, decodeItineraryScope("WHOLE_TRIP"))
        assertEquals(ItineraryScope.Day("day-2"), decodeItineraryScope("DAY:day-2"))
        assertNull(decodeItineraryScope(null))
        assertNull(decodeItineraryScope(""))
        assertNull(decodeItineraryScope("UNKNOWN"))
        assertNull(decodeItineraryScope("DAY:"))
        assertNull(decodeItineraryScope("DAY:   "))
    }

    @Test fun `itinerary scopes expose selected day id`() {
        assertNull(ItineraryScope.WholeTrip.selectedDayId())
        assertEquals("day-2", ItineraryScope.Day("day-2").selectedDayId())
    }

    @Test fun `new state wins over legacy keys`() {
        assertEquals(
            RestoredWorkspaceNavigation(WorkspaceSection.ITINERARY, ItineraryScope.WholeTrip),
            restoreWorkspaceNavigation("ITINERARY", "WHOLE_TRIP", "PLACES", "PLACE_POOL", "day-1"),
        )
    }

    @Test fun `new section and scope restore independently with legacy filling only missing field`() {
        assertEquals(
            RestoredWorkspaceNavigation(WorkspaceSection.PLACE_POOL, ItineraryScope.Day("day-2")),
            restoreWorkspaceNavigation("PLACE_POOL", null, "ITINERARY", "SINGLE_DAY", "day-2"),
        )
        assertEquals(
            RestoredWorkspaceNavigation(WorkspaceSection.ITINERARY, ItineraryScope.WholeTrip),
            restoreWorkspaceNavigation(null, "WHOLE_TRIP", "ITINERARY", "SINGLE_DAY", "day-2"),
        )
    }

    @Test fun `legacy place pool scope wins over itinerary tab`() {
        assertEquals(
            RestoredWorkspaceNavigation(WorkspaceSection.PLACE_POOL, null),
            restoreWorkspaceNavigation(null, null, "ITINERARY", "PLACE_POOL", "day-2"),
        )
    }

    @Test fun `legacy states migrate without allowing invalid combinations`() {
        assertEquals(
            RestoredWorkspaceNavigation(WorkspaceSection.PLACE_POOL, null),
            restoreWorkspaceNavigation(null, null, "SEARCH", "PLACE_POOL", null),
        )
        assertEquals(
            RestoredWorkspaceNavigation(WorkspaceSection.ITINERARY, ItineraryScope.Day("day-2")),
            restoreWorkspaceNavigation(null, null, "ITINERARY", "SINGLE_DAY", "day-2"),
        )
        assertEquals(
            RestoredWorkspaceNavigation(WorkspaceSection.ITINERARY, ItineraryScope.WholeTrip),
            restoreWorkspaceNavigation(null, null, "PLACES", "WHOLE_TRIP", null),
        )
    }

    @Test fun `first itinerary entry defaults to first day and no days defaults whole trip`() {
        assertEquals(ItineraryScope.Day("day-1"), reconcileItineraryScope(null, emptyList(), days("day-1", "day-2")))
        assertEquals(ItineraryScope.WholeTrip, reconcileItineraryScope(null, emptyList(), emptyList()))
    }

    @Test fun `deleted day selects successor then predecessor and keeps valid id across reorder`() {
        val old = days("day-1", "day-2", "day-3")
        assertEquals(ItineraryScope.Day("day-3"), reconcileItineraryScope(ItineraryScope.Day("day-2"), old, days("day-1", "day-3")))
        assertEquals(ItineraryScope.Day("day-2"), reconcileItineraryScope(ItineraryScope.Day("day-3"), old, days("day-1", "day-2")))
        assertEquals(ItineraryScope.Day("day-2"), reconcileItineraryScope(ItineraryScope.Day("day-2"), old, listOf(TripDay("day-2", 0), TripDay("day-1", 1))))
    }

    @Test fun `stale day without prior position defaults to first current day`() {
        assertEquals(
            ItineraryScope.Day("day-1"),
            reconcileItineraryScope(ItineraryScope.Day("stale-day"), emptyList(), days("day-1", "day-2")),
        )
    }

    private fun days(vararg ids: String): List<TripDay> = ids.mapIndexed { index, id -> TripDay(id, index) }
}
