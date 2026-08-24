package com.yangchengwei.easytrip.trip.ui

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DeleteImpactDao {
    @Query("SELECT COUNT(*) FROM trip_days WHERE tripId=:tripId") suspend fun days(tripId: String): Int
    @Query("SELECT COUNT(*) FROM saved_places WHERE tripId=:tripId") suspend fun places(tripId: String): Int
    @Query("SELECT COUNT(*) FROM tags WHERE tripId=:tripId") suspend fun tags(tripId: String): Int
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE tripId=:tripId") suspend fun tripItems(tripId: String): Int
    @Query("SELECT COUNT(*) FROM route_legs WHERE tripDayId IN (SELECT id FROM trip_days WHERE tripId=:tripId)") suspend fun tripLegs(tripId: String): Int
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE tripDayId=:dayId") suspend fun dayItems(dayId: String): Int
    @Query("SELECT COUNT(*) FROM route_legs WHERE tripDayId=:dayId") suspend fun dayLegs(dayId: String): Int
    @Query("SELECT COUNT(*) FROM saved_places WHERE tripId=(SELECT tripId FROM trip_days WHERE id=:dayId)") suspend fun placesForDay(dayId: String): Int
}

class RoomDeleteImpactProvider(private val dao: DeleteImpactDao) : DeleteImpactProvider {
    override suspend fun trip(tripId: String) = TripDeleteImpact(dao.days(tripId), dao.places(tripId), dao.tags(tripId), dao.tripItems(tripId), dao.tripLegs(tripId))
    override suspend fun day(dayId: String) = DayDeleteImpact(
        dao.dayItems(dayId),
        dao.dayLegs(dayId),
        dao.placesForDay(dayId),
    )
}
