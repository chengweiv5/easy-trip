package com.yangchengwei.easytrip.itinerary.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class ItineraryPlace(val id: String, val name: String, val address: String, val point: GeoPoint)
data class ItineraryItem(
    val id: String,
    val place: ItineraryPlace,
    val arrivalTime: LocalTime?,
    val stayMinutes: Int?,
    val note: String? = null,
    val idempotencyKey: String? = null,
    val expenseCents: Long? = null,
)
data class DayItinerary(val dayId: String, val tripId: String, val items: List<ItineraryItem>)
data class DayItinerarySnapshot(val itinerary: DayItinerary, val legs: List<com.yangchengwei.easytrip.route.data.RouteLegEntity>)
data class AddItineraryItemResult(val itemId: String, val created: Boolean)

class TargetDayNotFoundException(dayId: String) : IllegalArgumentException("Unknown day: $dayId")
class RecoverablePlaceAddException(placeId: String) : IllegalArgumentException("Cannot add place: $placeId")
class ItineraryItemNotFoundException(itemId: String) : IllegalArgumentException("Unknown item: $itemId")

interface ItineraryRepository {
    /** All days and their routes from one consistent read; null allows legacy stores to fall back. */
    fun observeTripDays(tripId: String): Flow<List<DayItinerarySnapshot>>? = null
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
    /** Appends to the current target order; persistent implementations must resolve the end atomically. */
    suspend fun appendItem(itemId: String, targetDayId: String) =
        moveItem(itemId, targetDayId, observeDay(targetDayId).first().items.count { it.id != itemId })
    suspend fun deleteItem(itemId: String)
    suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?)
    /** Atomically saves calendar timing, occurrence order and adjacent routes; null rejects stale edits.
     * Returns the applied order snapshots so undo can restore the exact previous position.
     */
    suspend fun compareAndSetTiming(change: ItineraryTimingChange): ItineraryTimingChange? = null
    suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?)
    suspend fun updateDetailsWithExpense(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?, expenseCents: Long?) {
        require(expenseCents == null) { "此存储暂不支持花费" }
        updateDetails(itemId, arrivalTime, stayMinutes, note)
    }
    suspend fun removePlaceOccurrences(placeId: String)
}
