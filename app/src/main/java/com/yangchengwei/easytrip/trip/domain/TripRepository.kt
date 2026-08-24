package com.yangchengwei.easytrip.trip.domain

import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class TripSummary(
    val id: String,
    val name: String,
    val startDate: LocalDate?,
    val travelMode: TravelMode,
    val dayCount: Int,
)

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
    suspend fun deleteDay(dayId: String)
    suspend fun deleteTrip(tripId: String)
}
