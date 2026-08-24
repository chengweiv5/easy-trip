package com.yangchengwei.easytrip.trip.domain

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TripDateRangeServiceTest {
    @Test fun bothNullKeepsDraftAndCurrentDayCount() = runTest {
        val repository = FakeRepository(trip())
        val service = TripDateRangeService(repository)

        val impact = service.preview("trip", null, null)
        service.apply(impact, "trip")

        assertEquals(3, impact.retainedDayIds.size)
        assertEquals(emptyList<String>(), impact.deletedDayIds)
        assertEquals(DateRangeApply("trip", null, 3, listOf("day-1", "day-2", "day-3"), emptyList(), 0, 0), repository.applied)
    }

    @Test fun oneNullIsInvalid() {
        val service = TripDateRangeService(FakeRepository(trip()))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { service.preview("trip", LocalDate.parse("2026-10-01"), null) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { service.preview("trip", null, LocalDate.parse("2026-10-03")) }
        }
    }

    @Test fun endBeforeStartIsInvalid() {
        val service = TripDateRangeService(FakeRepository(trip()))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                service.preview("trip", LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-01"))
            }
        }
    }

    @Test fun sameLengthMoveRetainsIdsAndOnlyUpdatesStart() = runTest {
        val repository = FakeRepository(trip())
        val service = TripDateRangeService(repository)

        val impact = service.preview("trip", LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"))
        service.apply(impact, "trip")

        assertEquals(listOf("day-1", "day-2", "day-3"), impact.retainedDayIds)
        assertEquals(emptyList<String>(), impact.deletedDayIds)
        assertEquals(DateRangeApply("trip", LocalDate.parse("2026-11-01"), 3, listOf("day-1", "day-2", "day-3"), emptyList(), 0, 0), repository.applied)
    }

    @Test fun growthAppendsOnlyAtEnd() = runTest {
        val repository = FakeRepository(trip())
        val service = TripDateRangeService(repository)

        val impact = service.preview("trip", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-05"))
        service.apply(impact, "trip")

        assertEquals(listOf("day-1", "day-2", "day-3"), impact.retainedDayIds)
        assertEquals(DateRangeApply("trip", LocalDate.parse("2026-10-01"), 5, listOf("day-1", "day-2", "day-3"), emptyList(), 0, 0), repository.applied)
    }

    @Test fun shrinkReportsTrailingStringIdsAndContentCounts() = runTest {
        val repository = FakeRepository(trip(), DateRangeDeletionCounts(4, 2, 7))
        val impact = TripDateRangeService(repository).preview(
            "trip",
            LocalDate.parse("2026-10-01"),
            LocalDate.parse("2026-10-01"),
        )

        assertEquals(listOf("day-1"), impact.retainedDayIds)
        assertEquals(listOf("day-2", "day-3"), impact.deletedDayIds)
        assertEquals(4, impact.deletedItineraryItems)
        assertEquals(2, impact.deletedRouteLegs)
        assertEquals(7, impact.retainedSavedPlaces)
    }

    private class FakeRepository(
        initial: TripWithDays,
        private val counts: DateRangeDeletionCounts = DateRangeDeletionCounts(0, 0, 0),
    ) : TripRepository {
        private val trip = MutableStateFlow<TripWithDays?>(initial)
        var applied: DateRangeApply? = null
        override fun observeTrips(): Flow<List<TripSummary>> = MutableStateFlow(emptyList())
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = trip
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = counts
        override suspend fun applyDateRange(command: DateRangeApply) { applied = command }
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    companion object {
        private fun trip() = TripWithDays(
            "trip", "Kyoto", LocalDate.parse("2026-10-01"), TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2)),
        )
    }
}
