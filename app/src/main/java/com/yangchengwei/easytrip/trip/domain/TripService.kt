package com.yangchengwei.easytrip.trip.domain

import java.time.LocalDate
import kotlinx.coroutines.flow.first

class TripService(private val repository: TripRepository) {
    suspend fun createTrip(command: CreateTrip): String {
        require(command.name.isNotBlank())
        require(command.dayCount in 1..MAX_TRIP_DAYS)
        require(isTripDateRangeRepresentable(command.startDate, command.dayCount)) { "日期范围超出支持范围" }
        return repository.createTrip(command.copy(name = command.name.trim()))
    }

    suspend fun renameTrip(tripId: String, name: String) {
        require(name.isNotBlank())
        repository.renameTrip(tripId, name.trim())
    }

    suspend fun setStartDate(tripId: String, startDate: LocalDate?) {
        val trip = requireNotNull(repository.observeTrip(tripId).first()) { "Unknown trip: $tripId" }
        require(isTripDateRangeRepresentable(startDate, trip.days.size)) { "日期范围超出支持范围" }
        repository.setStartDate(tripId, startDate)
    }
    suspend fun setTravelMode(tripId: String, mode: com.yangchengwei.easytrip.core.model.TravelMode) = repository.setTravelMode(tripId, mode)
    suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide): String {
        requireNotNull(repository.observeTrip(tripId).first()) { "Unknown trip: $tripId" }.let {
            require(it.days.size < MAX_TRIP_DAYS) { "旅行最多 30 天" }
            require(isTripDateRangeRepresentable(it.startDate, it.days.size + 1)) { "日期范围超出支持范围" }
        }
        return repository.insertDay(tripId, anchorDayId, side)
    }

    suspend fun appendTripDay(tripId: String) = insertDay(tripId, null, InsertSide.AFTER)
    suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) {
        val trip = requireNotNull(repository.observeTrip(tripId).first()) { "Unknown trip: $tripId" }
        require(trip.startDate == null) { "已设置日期的旅行日按日期连续排列" }
        repository.moveDay(tripId, dayId, targetIndex)
    }
    suspend fun deleteDay(command: DayDeletion) = repository.deleteDay(command)
    suspend fun deleteTrip(tripId: String) = repository.deleteTrip(tripId)

    fun displayDate(startDate: LocalDate?, position: Int): LocalDate? = startDate?.plusDays(position.toLong())
    fun displayLabel(day: TripDay, startDate: LocalDate? = null): String = displayDate(startDate, day.index)?.toString() ?: "Day ${day.index + 1}"
}
