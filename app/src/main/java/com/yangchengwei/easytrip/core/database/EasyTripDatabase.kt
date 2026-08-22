package com.yangchengwei.easytrip.core.database
import androidx.room.*
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.place.data.*
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.trip.data.*
import com.yangchengwei.easytrip.trip.ui.DeleteImpactDao
@Dao interface SchemaPlaceDao { @Insert fun insertSavedPlace(v:SavedPlaceEntity); @Insert fun insertTag(v:TagEntity); @Insert fun insertCrossRef(v:SavedPlaceTagCrossRef); @Query("SELECT COUNT(*) FROM saved_place_tags") fun countCrossRefs():Int; @Query("DELETE FROM tags WHERE id=:id") fun deleteTag(id:String); @Query("DELETE FROM saved_places WHERE id=:id") fun deleteSavedPlace(id:String); @Query("SELECT COUNT(*) FROM saved_places WHERE tripId=:tripId") fun countByTrip(tripId:String):Int }
@Dao
interface SchemaItineraryDao {
    @Insert fun insertItem(v: ItineraryItemEntity)
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE savedPlaceId=:savedPlaceId") fun countBySavedPlace(savedPlaceId: String): Int
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE id=:id") fun countById(id: String): Int
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE tripDayId=:dayId") suspend fun countByDay(dayId: String): Int
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE tripId=:tripId") suspend fun countByTrip(tripId: String): Int
    @Query("DELETE FROM itinerary_items WHERE id=:id") fun deleteItem(id: String)
}
@Dao interface SchemaRouteDao { @Insert fun insertLeg(v:RouteLegEntity); @Query("SELECT COUNT(*) FROM route_legs") fun count():Int }
@Dao interface CascadeCountDao {
    @Query("SELECT COUNT(*) FROM trips WHERE id=:tripId") suspend fun trips(tripId:String):Int
    @Query("SELECT COUNT(*) FROM trip_days WHERE tripId=:tripId") suspend fun days(tripId:String):Int
    @Query("SELECT COUNT(*) FROM saved_places WHERE tripId=:tripId") suspend fun places(tripId:String):Int
    @Query("SELECT COUNT(*) FROM tags WHERE tripId=:tripId") suspend fun tags(tripId:String):Int
    @Query("SELECT COUNT(*) FROM saved_place_tags WHERE tripId=:tripId") suspend fun crossRefs(tripId:String):Int
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE tripId=:tripId") suspend fun items(tripId:String):Int
    @Query("SELECT COUNT(*) FROM route_legs WHERE tripDayId IN (SELECT id FROM trip_days WHERE tripId=:tripId)") suspend fun legs(tripId:String):Int
}
@Database(entities=[TripEntity::class,TripDayEntity::class,SavedPlaceEntity::class,TagEntity::class,SavedPlaceTagCrossRef::class,ItineraryItemEntity::class,RouteLegEntity::class],version=1,exportSchema=true)
@TypeConverters(Converters::class) abstract class EasyTripDatabase:RoomDatabase(){ abstract fun tripDao():TripDao; abstract fun placeDao():SchemaPlaceDao; abstract fun savedPlaceDao():com.yangchengwei.easytrip.place.data.PlaceDao; abstract fun itineraryDao():SchemaItineraryDao; abstract fun itineraryEditingDao():com.yangchengwei.easytrip.itinerary.data.ItineraryDao; abstract fun routeDao():SchemaRouteDao; abstract fun routeLegDao():com.yangchengwei.easytrip.route.data.RouteLegDao; abstract fun deleteImpactDao():DeleteImpactDao; abstract fun cascadeCountDao():CascadeCountDao }
