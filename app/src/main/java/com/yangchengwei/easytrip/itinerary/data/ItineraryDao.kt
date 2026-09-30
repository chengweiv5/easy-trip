package com.yangchengwei.easytrip.itinerary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Embedded
import androidx.room.Relation
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.route.data.RouteLegEntity
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
    val note: String?,
    val placeId: String?,
    val placeName: String?,
    val placeAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val expenseCents: Long? = null,
)

data class TripDayReadSnapshot(
    @Embedded val day: TripDayEntity,
    @Relation(parentColumn = "id", entityColumn = "tripDayId") val items: List<ItineraryItemEntity>,
    @Relation(parentColumn = "tripId", entityColumn = "tripId") val places: List<SavedPlaceEntity>,
    @Relation(parentColumn = "id", entityColumn = "tripDayId") val legs: List<RouteLegEntity>,
)

@Dao
interface ItineraryDao {
    @Transaction
    @Query("SELECT * FROM trip_days WHERE tripId=:tripId ORDER BY position,id")
    fun observeTripDays(tripId: String): Flow<List<TripDayReadSnapshot>>

    @Query("SELECT expenseCents FROM itinerary_items WHERE tripId=:tripId AND id!=:excludedId AND expenseCents IS NOT NULL UNION ALL SELECT l.expenseCents FROM route_legs l JOIN trip_days d ON d.id=l.tripDayId WHERE d.tripId=:tripId AND l.expenseCents IS NOT NULL")
    suspend fun otherExpenses(tripId: String, excludedId: String): List<Long>

    @Query("UPDATE itinerary_items SET expenseCents=:cents WHERE id=:id")
    suspend fun expense(id: String, cents: Long?): Int

    @Query("""
        SELECT d.id AS dayId, d.tripId,
               i.id AS itemId, i.position, i.arrivalTime, i.stayDurationMinutes, i.note, i.expenseCents,
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

    @Query("SELECT * FROM itinerary_items WHERE idempotencyKey=:idempotencyKey")
    suspend fun itemByIdempotencyKey(idempotencyKey: String): ItineraryItemEntity?

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

    @Query("UPDATE itinerary_items SET arrivalTime=:arrivalTime, stayDurationMinutes=:stayMinutes, note=:note WHERE id=:itemId")
    suspend fun details(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?): Int

    @Query("""
        UPDATE itinerary_items SET arrivalTime=:arrival, stayDurationMinutes=:stay
        WHERE id=:itemId AND tripId=:tripId AND tripDayId=:dayId
          AND arrivalTime IS :expectedArrival AND stayDurationMinutes IS :expectedStay
    """)
    suspend fun conditionalTiming(tripId: String, dayId: String, itemId: String,
        expectedArrival: LocalTime?, expectedStay: Int?, arrival: LocalTime?, stay: Int?): Int

    @Transaction
    suspend fun dayAndItems(dayId: String): DayItems? {
        val tripId = tripIdForDay(dayId) ?: return null
        return DayItems(tripId, items(dayId))
    }
}

data class DayItems(val tripId: String, val items: List<ItineraryItemEntity>)
