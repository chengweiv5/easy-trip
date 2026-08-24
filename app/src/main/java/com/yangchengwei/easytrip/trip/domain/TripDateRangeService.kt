package com.yangchengwei.easytrip.trip.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

data class DateRangeChangeImpact(
    val newStartDate: LocalDate?,
    val newEndDate: LocalDate?,
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
    val startDate: LocalDate?,
    val dayCount: Int,
    val expectedDayIds: List<String> = emptyList(),
    val expectedDeletedDayIds: List<String> = emptyList(),
    val expectedDeletedItineraryItems: Int = 0,
    val expectedDeletedRouteLegs: Int = 0,
)

class TripDateRangeService(private val repository: TripRepository) {
    suspend fun preview(tripId: String, startDate: LocalDate?, endDate: LocalDate?): DateRangeChangeImpact {
        val dayCount = validateAndCount(startDate, endDate)
        val trip = requireNotNull(repository.observeTrip(tripId).first()) { "Unknown trip: $tripId" }
        val targetCount = dayCount ?: trip.days.size
        val retained = trip.days.take(targetCount).map(TripDay::id)
        val deleted = trip.days.drop(targetCount).map(TripDay::id)
        val counts = repository.dateRangeDeletionCounts(tripId, deleted)
        return DateRangeChangeImpact(
            startDate,
            endDate,
            retained,
            deleted,
            counts.itineraryItems,
            counts.routeLegs,
            counts.retainedSavedPlaces,
        )
    }

    suspend fun apply(impact: DateRangeChangeImpact, tripId: String) {
        val dayCount = validateAndCount(impact.newStartDate, impact.newEndDate)
            ?: impact.retainedDayIds.size + impact.deletedDayIds.size
        repository.applyDateRange(
            DateRangeApply(
                tripId = tripId,
                startDate = impact.newStartDate,
                dayCount = dayCount,
                expectedDayIds = impact.retainedDayIds + impact.deletedDayIds,
                expectedDeletedDayIds = impact.deletedDayIds,
                expectedDeletedItineraryItems = impact.deletedItineraryItems,
                expectedDeletedRouteLegs = impact.deletedRouteLegs,
            ),
        )
    }

    private fun validateAndCount(startDate: LocalDate?, endDate: LocalDate?): Int? {
        require((startDate == null) == (endDate == null)) { "Start and end date must both be set or both be null" }
        if (startDate == null) return null
        require(!endDate!!.isBefore(startDate)) { "End date cannot be before start date" }
        return ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    }
}
