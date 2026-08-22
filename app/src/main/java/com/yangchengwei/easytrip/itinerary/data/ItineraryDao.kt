package com.yangchengwei.easytrip.itinerary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

data class DayItineraryRow(
    val dayId: String,
    val tripId: String,
    val itemId: String?,
    val position: Long?,
    val arrivalTime: LocalTime?,
    val stayDurationMinutes: Int?,
    val placeId: String?,
    val placeName: String?,
    val placeAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
)

@Dao
interface ItineraryDao {
    @Query("""
        SELECT d.id AS dayId, d.tripId,
               i.id AS itemId, i.position, i.arrivalTime, i.stayDurationMinutes,
               p.id AS placeId, p.name AS placeName, p.address AS placeAddress,
               p.latitude, p.longitude
        FROM trip_days d
        LEFT JOIN itinerary_items i ON i.tripDayId = d.id AND i.tripId = d.tripId
        LEFT JOIN saved_places p ON p.id = i.savedPlaceId AND p.tripId = i.tripId
        WHERE d.id = :dayId
        ORDER BY i.position, i.id
    """)
    fun observeDayRows(dayId: String): Flow<List<DayItineraryRow>>
    @Query("SELECT * FROM itinerary_items WHERE tripDayId=:dayId ORDER BY position, id")
    suspend fun items(dayId: String): List<ItineraryItemEntity>

    @Query("SELECT * FROM itinerary_items WHERE tripDayId=:dayId ORDER BY position, id")
    fun observeItems(dayId: String): Flow<List<ItineraryItemEntity>>

    @Query("SELECT * FROM itinerary_items WHERE id=:itemId")
    suspend fun item(itemId: String): ItineraryItemEntity?

    @Query("SELECT tripId FROM trip_days WHERE id=:dayId")
    suspend fun tripIdForDay(dayId: String): String?

    @Query("SELECT t.travelMode FROM trips t JOIN trip_days d ON d.tripId=t.id WHERE d.id=:dayId")
    suspend fun travelModeForDay(dayId: String): TravelMode?

    @Query("SELECT * FROM saved_places WHERE id=:placeId")
    suspend fun savedPlace(placeId: String): SavedPlaceEntity?

    @Insert
    suspend fun insertItem(item: ItineraryItemEntity)

    @Query("UPDATE itinerary_items SET position=:position WHERE id=:itemId")
    suspend fun position(itemId: String, position: Long): Int

    @Query("UPDATE itinerary_items SET tripDayId=:dayId, tripId=:tripId, position=:position WHERE id=:itemId")
    suspend fun moveRow(itemId: String, dayId: String, tripId: String, position: Long): Int

    @Query("DELETE FROM itinerary_items WHERE id=:itemId")
    suspend fun deleteRow(itemId: String): Int

    @Query("SELECT * FROM itinerary_items WHERE savedPlaceId=:placeId ORDER BY tripDayId, position, id")
    suspend fun itemsForPlace(placeId: String): List<ItineraryItemEntity>

    @Query("DELETE FROM itinerary_items WHERE savedPlaceId=:placeId")
    suspend fun deleteRowsForPlace(placeId: String): Int

    @Query("UPDATE itinerary_items SET arrivalTime=:arrivalTime, stayDurationMinutes=:stayMinutes WHERE id=:itemId")
    suspend fun timing(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?): Int

    @Transaction
    suspend fun dayAndItems(dayId: String): DayItems? {
        val tripId = tripIdForDay(dayId) ?: return null
        return DayItems(tripId, items(dayId))
    }
}

data class DayItems(val tripId: String, val items: List<ItineraryItemEntity>)
