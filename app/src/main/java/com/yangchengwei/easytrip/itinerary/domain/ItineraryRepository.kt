package com.yangchengwei.easytrip.itinerary.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

data class ItineraryPlace(val id: String, val name: String, val address: String, val point: GeoPoint)
data class ItineraryItem(
    val id: String,
    val place: ItineraryPlace,
    val arrivalTime: LocalTime?,
    val stayMinutes: Int?,
    val note: String? = null,
    val idempotencyKey: String? = null,
)
data class DayItinerary(val dayId: String, val tripId: String, val items: List<ItineraryItem>)
data class AddItineraryItemResult(val itemId: String, val created: Boolean)

class TargetDayNotFoundException(dayId: String) : IllegalArgumentException("Unknown day: $dayId")
class RecoverablePlaceAddException(placeId: String) : IllegalArgumentException("Cannot add place: $placeId")
class ItineraryItemNotFoundException(itemId: String) : IllegalArgumentException("Unknown item: $itemId")

interface ItineraryRepository {
    fun observeDay(dayId: String): Flow<DayItinerary>
    suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String
    suspend fun addItemIdempotently(
        dayId: String,
        savedPlaceId: String,
        targetIndex: Int,
        idempotencyKey: String,
    ): AddItineraryItemResult = AddItineraryItemResult(
        itemId = addItem(dayId, savedPlaceId, targetIndex),
        created = true,
    )
    suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int)
    suspend fun deleteItem(itemId: String)
    suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?)
    /** Implementations must perform an atomic comparison; unsupported writers reject safely. */
    suspend fun compareAndSetTiming(change: ItineraryTimingChange): Boolean = false
    suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?)
    suspend fun removePlaceOccurrences(placeId: String)
}
