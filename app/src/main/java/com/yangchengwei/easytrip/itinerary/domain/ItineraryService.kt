package com.yangchengwei.easytrip.itinerary.domain

import java.time.LocalTime

class ItineraryService(private val repository: ItineraryRepository) {
    fun observeDay(dayId: String) = repository.observeDay(dayId)
    suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = repository.addItem(dayId, savedPlaceId, targetIndex)
    suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = repository.moveItem(itemId, targetDayId, targetIndex)
    suspend fun deleteItem(itemId: String) = repository.deleteItem(itemId)
    suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) {
        require(stayMinutes == null || stayMinutes >= 0)
        repository.updateTiming(itemId, arrivalTime, stayMinutes)
    }
}
