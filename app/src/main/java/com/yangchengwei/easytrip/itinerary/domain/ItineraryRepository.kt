package com.yangchengwei.easytrip.itinerary.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

data class ItineraryPlace(val id: String, val name: String, val address: String, val point: GeoPoint)
data class ItineraryItem(val id: String, val place: ItineraryPlace, val arrivalTime: LocalTime?, val stayMinutes: Int?)
data class DayItinerary(val dayId: String, val tripId: String, val items: List<ItineraryItem>)

class TargetDayNotFoundException(dayId: String) : IllegalArgumentException("Unknown day: $dayId")
class RecoverablePlaceAddException(placeId: String) : IllegalArgumentException("Cannot add place: $placeId")

interface ItineraryRepository {
    fun observeDay(dayId: String): Flow<DayItinerary>
    suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String
    suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int)
    suspend fun deleteItem(itemId: String)
    suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?)
    suspend fun removePlaceOccurrences(placeId: String)
}
