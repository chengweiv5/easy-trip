package com.yangchengwei.easytrip.trip.domain

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

const val MAX_TRIP_DAYS = 30

fun isTripDateRangeRepresentable(startDate: LocalDate?, dayCount: Int): Boolean =
    startDate == null || dayCount >= 1 && startDate <= LocalDate.MAX.minusDays(dayCount.toLong() - 1L)

fun tripEndDateOrNull(startDate: LocalDate?, dayCount: Int): LocalDate? =
    if (startDate != null && isTripDateRangeRepresentable(startDate, dayCount)) {
        startDate.plusDays(dayCount.toLong() - 1L)
    } else {
        null
    }

data class TripSummary(
    val id: String,
    val name: String,
    val startDate: LocalDate?,
    val travelMode: TravelMode,
    val dayCount: Int,
    val placeCount: Int,
    val scheduledDayCount: Int,
    val updatedAt: Instant = Instant.EPOCH,
)

fun List<TripSummary>.sortedForTripList(today: LocalDate): List<TripSummary> = sortedWith { left, right ->
    val leftBucket = left.tripListDateBucket(today)
    val rightBucket = right.tripListDateBucket(today)
    val bucket = compareValues(leftBucket, rightBucket)
    if (bucket != 0) return@sortedWith bucket
    val date = when (leftBucket) {
        0 -> compareValues(left.endDate(), right.endDate()).takeIf { it != 0 }
            ?: compareValues(right.startDate, left.startDate)
        1 -> compareValues(left.startDate, right.startDate)
        2 -> compareValues(right.endDate(), left.endDate())
        else -> 0
    }
    if (date != 0) date else right.updatedAt.compareTo(left.updatedAt).takeIf { it != 0 } ?: left.id.compareTo(right.id)
}

private fun TripSummary.tripListDateBucket(today: LocalDate): Int {
    val startDate = startDate ?: return 3
    val endDate = tripEndDateOrNull(startDate, dayCount) ?: return 3
    return when {
        !today.isBefore(startDate) && !today.isAfter(endDate) -> 0
        today.isBefore(startDate) -> 1
        else -> 2
    }
}

private fun TripSummary.endDate(): LocalDate? = tripEndDateOrNull(startDate, dayCount)

data class TripDay(val id: String, val index: Int)

data class TripWithDays(
    val id: String,
    val name: String,
    val startDate: LocalDate?,
    val travelMode: TravelMode,
    val days: List<TripDay>,
)

data class CreateTrip(
    val name: String,
    val dayCount: Int,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val startDate: LocalDate? = null,
    val requestId: String? = null,
)

enum class InsertSide { BEFORE, AFTER }

data class DayDeletion(
    val dayId: String,
    val expectedItineraryItems: Int,
    val expectedRouteLegs: Int,
)

interface TripRepository {
    fun observeTrips(): Flow<List<TripSummary>>
    fun observeTrip(tripId: String): Flow<TripWithDays?>
    suspend fun createTrip(command: CreateTrip): String
    suspend fun renameTrip(tripId: String, name: String)
    suspend fun setStartDate(tripId: String, startDate: LocalDate?)
    suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>): DateRangeDeletionCounts
    suspend fun applyDateRange(command: DateRangeApply)
    suspend fun setTravelMode(tripId: String, mode: TravelMode)
    suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String
    suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int)
    suspend fun deleteDay(command: DayDeletion)
    suspend fun deleteTrip(tripId: String)
}
