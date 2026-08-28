package com.yangchengwei.easytrip.trip.domain

import com.yangchengwei.easytrip.trip.ui.DateRangeChangeRequest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

class DateRangeSnapshotChangedException(message: String) : IllegalStateException(message)

data class DateRangeChangeImpact(
    val request: DateRangeChangeRequest,
    val retainedDayIds: List<String>,
    val deletedDayIds: List<String>,
    val deletedItineraryItems: Int,
    val deletedRouteLegs: Int,
    val retainedSavedPlaces: Int,
)

data class DateRangeDeletionCounts(
    val itineraryItems: Int,
    val routeLegs: Int,
    val retainedSavedPlaces: Int,
)

data class DateRangeApply(
    val tripId: String,
    val expectedStartDate: LocalDate,
    val startDate: LocalDate,
    val dayCount: Int,
    val expectedDayIds: List<String>,
    val expectedDeletedDayIds: List<String>,
    val expectedDeletedItineraryItems: Int,
    val expectedDeletedRouteLegs: Int,
)

class TripDateRangeService(private val repository: TripRepository) {
    suspend fun preview(request: DateRangeChangeRequest): DateRangeChangeImpact {
        val trip = requireNotNull(repository.observeTrip(request.tripId).first()) { "Unknown trip: ${request.tripId}" }
        if (trip.startDate != request.baselineStartDate) {
            throw DateRangeSnapshotChangedException("Trip start date changed after edit began")
        }
        if (trip.days.map(TripDay::id) != request.baselineDayIds) {
            throw DateRangeSnapshotChangedException("Trip days changed after edit began")
        }
        val dayCount = validateAndCount(request.baselineStartDate, request.targetEndDate)
        val retained = trip.days.take(dayCount).map(TripDay::id)
        val deleted = trip.days.drop(dayCount).map(TripDay::id)
        val counts = repository.dateRangeDeletionCounts(request.tripId, deleted)
        return DateRangeChangeImpact(
            request,
            retained,
            deleted,
            counts.itineraryItems,
            counts.routeLegs,
            counts.retainedSavedPlaces,
        )
    }

    suspend fun apply(impact: DateRangeChangeImpact) {
        val request = impact.request
        repository.applyDateRange(
            DateRangeApply(
                tripId = request.tripId,
                expectedStartDate = request.baselineStartDate,
                startDate = request.baselineStartDate,
                dayCount = validateAndCount(request.baselineStartDate, request.targetEndDate),
                expectedDayIds = request.baselineDayIds,
                expectedDeletedDayIds = impact.deletedDayIds,
                expectedDeletedItineraryItems = impact.deletedItineraryItems,
                expectedDeletedRouteLegs = impact.deletedRouteLegs,
            ),
        )
    }

    private fun validateAndCount(startDate: LocalDate, endDate: LocalDate): Int {
        require(!endDate.isBefore(startDate)) { "End date cannot be before start date" }
        val dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1L
        require(dayCount in 1L..MAX_TRIP_DAYS.toLong()) { "旅行最多 30 天" }
        return dayCount.toInt()
    }
}
