package com.yangchengwei.easytrip.trip.domain

import java.time.LocalDate

class TripService(private val repository: TripRepository) {
    suspend fun createTrip(command: CreateTrip): String {
        require(command.name.isNotBlank())
        require(command.dayCount >= 1)
        return repository.createTrip(command.copy(name = command.name.trim()))
    }

    suspend fun renameTrip(tripId: String, name: String) {
        require(name.isNotBlank())
        repository.renameTrip(tripId, name.trim())
    }

    suspend fun setStartDate(tripId: String, startDate: LocalDate?) = repository.setStartDate(tripId, startDate)
    suspend fun setTravelMode(tripId: String, mode: com.yangchengwei.easytrip.core.model.TravelMode) = repository.setTravelMode(tripId, mode)
    suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = repository.insertDay(tripId, anchorDayId, side)
    suspend fun appendTripDay(tripId: String) = repository.insertDay(tripId, null, InsertSide.AFTER)
    suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = repository.moveDay(tripId, dayId, targetIndex)
    suspend fun deleteDay(dayId: String) = repository.deleteDay(dayId)
    suspend fun deleteTrip(tripId: String) = repository.deleteTrip(tripId)

    fun displayDate(startDate: LocalDate?, position: Int): LocalDate? = startDate?.plusDays(position.toLong())
    fun displayLabel(day: TripDay, startDate: LocalDate? = null): String = displayDate(startDate, day.index)?.toString() ?: "Day ${day.index + 1}"
}
