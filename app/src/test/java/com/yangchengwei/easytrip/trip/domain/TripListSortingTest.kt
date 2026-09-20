package com.yangchengwei.easytrip.trip.domain

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TripListSortingTest {
    private val today = LocalDate.of(2026, 9, 20)

    @Test fun pendingPrioritizesTodayThenUpcomingThenRecentPastThenUndated() {
        val trips = listOf(
            trip("past-old", today.minusMonths(1)),
            trip("future-far", today.plusMonths(1)),
            trip("undated", null),
            trip("past-recent", today.minusDays(1), dayCount = 30),
            trip("future-near", today.plusDays(1)),
            trip("today", today),
        )

        val sorted = trips.sortedForTripList(today)

        assertEquals(listOf("today", "future-near", "future-far", "past-recent", "past-old", "undated"), sorted.map { it.id })
        assertEquals(trips.associateBy { it.id }, sorted.associateBy { it.id })
        assertEquals(true, sorted.all { !it.hasTraveled })
    }

    @Test fun traveledSortsByLatestDepartureEvenWhenOlderTripEndsLater() {
        val trips = listOf(
            trip("older-long", today.minusDays(10), hasTraveled = true, dayCount = 30),
            trip("recent", today.minusDays(1), hasTraveled = true),
            trip("undated", null, hasTraveled = true),
            trip("oldest", today.minusYears(1), hasTraveled = true),
        )

        assertEquals(listOf("recent", "older-long", "oldest", "undated"), trips.sortedForTripList(today).map { it.id })
    }

    @Test fun manualStatusOverridesDatesAndTraveledOrderDoesNotDependOnToday() {
        val trips = listOf(
            trip("traveled-today", today, hasTraveled = true),
            trip("traveled-future", today.plusDays(5), hasTraveled = true),
            trip("pending-past", today.minusYears(1)),
            trip("pending-undated", null),
            trip("traveled-past", today.minusDays(5), hasTraveled = true),
        )
        val expected = listOf("pending-past", "pending-undated", "traveled-future", "traveled-today", "traveled-past")

        for (currentDate in listOf(today.minusYears(2), today, today.plusYears(2))) {
            val sorted = trips.sortedForTripList(currentDate)
            assertEquals(expected, sorted.map { it.id })
            assertEquals(trips.associateBy { it.id }, sorted.associateBy { it.id })
        }
    }

    @Test fun editingAndTripDurationNeverOverrideDepartureDate() {
        val recentlyEdited = Instant.parse("2026-09-20T12:00:00Z")
        val trips = listOf(
            trip("pending-later", today.plusDays(2), updatedAt = recentlyEdited),
            trip("traveled-older", today.minusDays(2), hasTraveled = true, dayCount = 30, updatedAt = recentlyEdited),
            trip("pending-earlier", today.plusDays(1), dayCount = 30),
            trip("traveled-newer", today.minusDays(1), hasTraveled = true),
        )

        assertEquals(
            listOf("pending-earlier", "pending-later", "traveled-newer", "traveled-older"),
            trips.sortedForTripList(today).map { it.id },
        )
    }

    @Test fun sameDateAndUndatedUseLatestEditThenIdForStableOrdering() {
        for (hasTraveled in listOf(false, true)) {
            for (date in listOf(null, today, today.minusDays(1))) {
                val trips = listOf(
                    trip("b", date, hasTraveled),
                    trip("newer", date, hasTraveled, updatedAt = Instant.EPOCH.plusSeconds(1)),
                    trip("a", date, hasTraveled),
                )
                val expected = listOf("newer", "a", "b")
                assertEquals(expected, trips.sortedForTripList(today).map { it.id })
                assertEquals(expected, trips.reversed().sortedForTripList(today).map { it.id })
            }
        }
    }

    @Test fun dateOrderWorksAcrossYearBoundaryAndLocalDateExtremes() {
        val yearEnd = LocalDate.of(2026, 12, 31)
        val trips = listOf(
            trip("new-year", LocalDate.of(2027, 1, 1)),
            trip("maximum", LocalDate.MAX),
            trip("year-end", yearEnd),
            trip("minimum", LocalDate.MIN),
        )
        assertEquals(listOf("year-end", "new-year", "maximum", "minimum"), trips.sortedForTripList(yearEnd).map { it.id })
        assertEquals(
            listOf("maximum", "new-year", "year-end", "minimum"),
            trips.map { it.copy(hasTraveled = true) }.sortedForTripList(yearEnd).map { it.id },
        )
    }

    private fun trip(
        id: String,
        date: LocalDate?,
        hasTraveled: Boolean = false,
        dayCount: Int = 1,
        updatedAt: Instant = Instant.EPOCH,
    ) = TripSummary(id, id, date, TravelMode.FLEXIBLE, dayCount, 0, 0, updatedAt, hasTraveled)
}
