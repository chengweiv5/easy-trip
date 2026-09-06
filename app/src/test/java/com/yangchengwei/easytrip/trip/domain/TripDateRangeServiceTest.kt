package com.yangchengwei.easytrip.trip.domain

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.ui.DateRangeChangeRequest
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class TripDateRangeServiceTest {
    @Test fun previewRejectsChangedBaselineDays() {
        val service = TripDateRangeService(FakeRepository(trip()))
        val request = request(baselineDayIds = listOf("day-1", "day-3", "day-2"))

        val error = assertThrows(DateRangeSnapshotChangedException::class.java) {
            kotlinx.coroutines.runBlocking { service.preview(request) }
        }

        assertEquals("Trip days changed after edit began", error.message)
    }

    @Test fun previewRejectsChangedBaselineStartDate() {
        val service = TripDateRangeService(FakeRepository(trip()))
        val request = request(baselineStartDate = LocalDate.parse("2026-10-02"))

        val error = assertThrows(DateRangeSnapshotChangedException::class.java) {
            kotlinx.coroutines.runBlocking { service.preview(request) }
        }

        assertEquals("Trip start date changed after edit began", error.message)
    }

    @Test fun previewRetainsSnapshotAndUsesTargetDateRange() = runTest {
        val request = request(
            targetStartDate = LocalDate.parse("2026-10-03"),
            targetEndDate = LocalDate.parse("2026-10-07"),
        )

        val impact = TripDateRangeService(FakeRepository(trip())).preview(request)

        assertSame(request, impact.request)
        assertEquals(LocalDate.parse("2026-10-01"), impact.request.baselineStartDate)
        assertEquals(LocalDate.parse("2026-10-03"), impact.request.targetStartDate)
        assertEquals(listOf("day-1", "day-2", "day-3"), impact.retainedDayIds)
        assertEquals(emptyList<String>(), impact.deletedDayIds)
    }

    @Test fun applyCopiesSnapshotAndTargetDateRangeIntoRepositoryCommand() = runTest {
        val repository = FakeRepository(trip(), DateRangeDeletionCounts(4, 2, 7))
        val request = request(
            targetStartDate = LocalDate.parse("2026-10-03"),
            targetEndDate = LocalDate.parse("2026-10-03"),
        )
        val service = TripDateRangeService(repository)

        val impact = service.preview(request)
        service.apply(impact)

        assertEquals(
            DateRangeApply(
                tripId = "trip",
                expectedStartDate = LocalDate.parse("2026-10-01"),
                startDate = LocalDate.parse("2026-10-03"),
                dayCount = 1,
                expectedDayIds = request.baselineDayIds,
                expectedDeletedDayIds = listOf("day-2", "day-3"),
                expectedDeletedItineraryItems = 4,
                expectedDeletedRouteLegs = 2,
                expectedDeletedSnapshot = null,
            ),
            repository.applied,
        )
    }

    @Test fun undatedTripRejectsTargetRangeWithDifferentDayCountBeforeDeletionPreview() = runTest {
        val repository = FakeRepository(trip(startDate = null))
        val request = request(
            baselineStartDate = null,
            targetStartDate = LocalDate.parse("2026-10-03"),
            targetEndDate = LocalDate.parse("2026-10-04"),
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { TripDateRangeService(repository).preview(request) }
        }

        assertEquals("为未定日期旅行设置日期时必须保留现有 3 天行程", error.message)
        assertEquals(0, repository.countCalls)
        assertEquals(null, repository.applied)
    }

    @Test fun undatedTripCanBecomeDatedWithTargetRange() = runTest {
        val repository = FakeRepository(trip(startDate = null))
        val request = request(
            baselineStartDate = null,
            targetStartDate = LocalDate.parse("2026-10-03"),
            targetEndDate = LocalDate.parse("2026-10-05"),
        )

        val impact = TripDateRangeService(repository).preview(request)
        TripDateRangeService(repository).apply(impact)

        assertEquals(3, repository.applied?.dayCount)
        assertEquals(null, repository.applied?.expectedStartDate)
        assertEquals(LocalDate.parse("2026-10-03"), repository.applied?.startDate)
    }

    @Test fun endBeforeStartIsInvalid() {
        val service = TripDateRangeService(FakeRepository(trip()))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                service.preview(
                    request(
                        targetStartDate = LocalDate.parse("2026-10-02"),
                        targetEndDate = LocalDate.parse("2026-10-01"),
                    ),
                )
            }
        }
    }

    @Test fun localDateMaximumIsAcceptedForOneDayAndThirtyDayBoundaryIsEnforced() = runTest {
        val maximum = LocalDate.MAX
        val repository = FakeRepository(trip(startDate = maximum))

        TripDateRangeService(repository).preview(
            request(
                baselineStartDate = maximum,
                targetStartDate = maximum,
                targetEndDate = maximum,
            ),
        )

        assertEquals(1, repository.countCalls)
    }

    @Test fun localDateMinimumThirtyDayRangeIsAccepted() = runTest {
        val minimum = LocalDate.MIN
        val repository = FakeRepository(trip(startDate = minimum))

        TripDateRangeService(repository).preview(
            request(
                baselineStartDate = minimum,
                targetStartDate = minimum,
                targetEndDate = minimum.plusDays(29),
            ),
        )

        assertEquals(1, repository.countCalls)
    }

    @Test fun localDateMinimumThirtyOneDayRangeIsRejectedBeforeDeletionCounts() = runTest {
        val minimum = LocalDate.MIN
        val repository = FakeRepository(trip(startDate = minimum))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                TripDateRangeService(repository).preview(
                    request(
                        baselineStartDate = minimum,
                        targetStartDate = minimum,
                        targetEndDate = minimum.plusDays(30),
                    ),
                )
            }
        }

        assertEquals(0, repository.countCalls)
    }

    @Test fun thirtyDayRangeIsAccepted() = runTest {
        val repository = FakeRepository(trip())

        TripDateRangeService(repository).preview(request(targetEndDate = LocalDate.parse("2026-10-30")))

        assertEquals(1, repository.countCalls)
    }

    @Test fun thirtyOneDayRangeIsRejectedBeforeDeletionCounts() = runTest {
        val repository = FakeRepository(trip())

        val error = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                TripDateRangeService(repository).preview(request(targetEndDate = LocalDate.parse("2026-10-31")))
            }
        }

        assertEquals("旅行最多 30 天", error.message)
        assertEquals(0, repository.countCalls)
    }

    @Test fun extremeEndDateIsRejectedBeforeDeletionCounts() = runTest {
        val repository = FakeRepository(trip())

        val error = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                TripDateRangeService(repository).preview(request(targetEndDate = LocalDate.parse("9999-12-31")))
            }
        }

        assertEquals("旅行最多 30 天", error.message)
        assertEquals(0, repository.countCalls)
        assertEquals(null, repository.applied)
    }

    @Test fun growthRetainsExistingIdsAndRequestsAdditionalDays() = runTest {
        val repository = FakeRepository(trip())
        val service = TripDateRangeService(repository)

        val impact = service.preview(request(targetEndDate = LocalDate.parse("2026-10-05")))
        service.apply(impact)

        assertEquals(listOf("day-1", "day-2", "day-3"), impact.retainedDayIds)
        assertEquals(emptyList<String>(), impact.deletedDayIds)
        assertEquals(5, repository.applied?.dayCount)
        assertEquals(request().targetStartDate, repository.applied?.startDate)
        assertEquals(request().baselineStartDate, repository.applied?.expectedStartDate)
    }

    @Test fun shrinkReportsTrailingStringIdsAndContentCounts() = runTest {
        val repository = FakeRepository(trip(), DateRangeDeletionCounts(4, 2, 7))
        val impact = TripDateRangeService(repository).preview(
            request(targetEndDate = LocalDate.parse("2026-10-01")),
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
        var countCalls = 0
        override fun observeTrips(): Flow<List<TripSummary>> = MutableStateFlow(emptyList())
        override fun observeTrip(tripId: String): Flow<TripWithDays?> = trip
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>): DateRangeDeletionCounts {
            countCalls++
            return counts
        }
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
        private fun request(
            baselineStartDate: LocalDate? = LocalDate.parse("2026-10-01"),
            baselineDayIds: List<String> = listOf("day-1", "day-2", "day-3"),
            targetStartDate: LocalDate = LocalDate.parse("2026-10-01"),
            targetEndDate: LocalDate = LocalDate.parse("2026-10-03"),
        ) = DateRangeChangeRequest(1, "trip", baselineStartDate, baselineDayIds, targetStartDate, targetEndDate)

        private fun trip(startDate: LocalDate? = LocalDate.parse("2026-10-01")) = TripWithDays(
            "trip", "Kyoto", startDate, TravelMode.FLEXIBLE,
            listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2)),
        )
    }
}
