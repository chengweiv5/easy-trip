package com.yangchengwei.easytrip.trip.data

import androidx.room.*
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class TripEntityWithDays(
    @Embedded val trip: TripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId") val days: List<TripDayEntity>,
)

@Dao
interface TripDao {
    @Transaction @Query("SELECT * FROM trips ORDER BY updatedAt DESC")
    fun observeTrips(): Flow<List<TripEntityWithDays>>
    @Transaction @Query("SELECT * FROM trips WHERE id = :tripId")
    fun observeTrip(tripId: String): Flow<TripEntityWithDays?>
    @Insert fun insertTrip(value: TripEntity)
    @Insert fun insertDay(value: TripDayEntity)
    @Query("SELECT * FROM trip_days WHERE tripId=:tripId ORDER BY position") suspend fun days(tripId:String):List<TripDayEntity>
    @Query("SELECT * FROM trips WHERE id=:tripId") suspend fun trip(tripId:String):TripEntity?
    @Query("SELECT tripId FROM trip_days WHERE id=:dayId") suspend fun tripIdForDay(dayId:String):String?
    @Query("SELECT EXISTS(SELECT 1 FROM trips WHERE id=:tripId)") suspend fun tripExists(tripId:String):Boolean
    @Query("UPDATE trips SET name=:name,updatedAt=:now WHERE id=:tripId") suspend fun renameRow(tripId:String,name:String,now:Instant):Int
    @Query("UPDATE trips SET startDate=:date,timeMode=:mode,updatedAt=:now WHERE id=:tripId") suspend fun dateRow(tripId:String,date:LocalDate?,mode:TimeMode,now:Instant):Int
    @Query("UPDATE trips SET travelMode=:mode,updatedAt=:now WHERE id=:tripId") suspend fun modeRow(tripId:String,mode:TravelMode,now:Instant):Int
    @Query("UPDATE trips SET updatedAt=:now WHERE id=:tripId") suspend fun touch(tripId:String,now:Instant):Int
    @Query("UPDATE trip_days SET position=:position WHERE id=:dayId") suspend fun position(dayId:String,position:Long):Int
    @Query("DELETE FROM trip_days WHERE id=:dayId") suspend fun deleteDayRow(dayId:String):Int
    @Query("DELETE FROM trips WHERE id=:tripId") suspend fun deleteTripRow(tripId:String):Int
    @Query("DELETE FROM trips WHERE id=:id") fun deleteTrip(id:String)
    @Query("SELECT COUNT(*) FROM trip_days WHERE tripId=:tripId") fun countDays(tripId:String):Int
    @Query("SELECT position FROM trip_days WHERE tripId=:tripId ORDER BY position") suspend fun dayPositions(tripId:String):List<Long>
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE tripDayId IN (:dayIds)") suspend fun itemCountForDays(dayIds:List<String>):Int
    @Query("SELECT COUNT(*) FROM route_legs WHERE tripDayId IN (:dayIds)") suspend fun legCountForDays(dayIds:List<String>):Int
    @Query("SELECT COUNT(*) FROM saved_places WHERE tripId=:tripId") suspend fun savedPlaceCount(tripId:String):Int

    @Transaction suspend fun createTripWithDays(trip:TripEntity,days:List<TripDayEntity>){ insertTrip(trip); days.forEach(::insertDay) }
    @Transaction suspend fun createTripWithDaysIdempotent(trip:TripEntity,dayCount:Int,dayIdFactory:()->String){
        val existing=trip(trip.id)
        if(existing==null){
            val newDays=List(dayCount){index->TripDayEntity(dayIdFactory(),trip.id,index*POSITION_STEP)}
            createTripWithDays(trip,newDays)
            return
        }
        val existingDays=days(trip.id)
        require(existing.name==trip.name && existing.timeMode==trip.timeMode && existing.startDate==trip.startDate && existing.travelMode==trip.travelMode && existingDays.size==dayCount){"Conflicting create request: ${trip.id}"}
    }
    @Transaction suspend fun renameTrip(tripId:String,name:String,now:Instant){ require(renameRow(tripId,name,now)==1){"Unknown trip: $tripId"} }
    @Transaction suspend fun setStartDate(tripId:String,date:LocalDate?,mode:TimeMode,now:Instant){ require(dateRow(tripId,date,mode,now)==1){"Unknown trip: $tripId"} }
    @Transaction suspend fun setTravelMode(tripId:String,mode:TravelMode,now:Instant){ require(modeRow(tripId,mode,now)==1){"Unknown trip: $tripId"} }
    @Transaction suspend fun deleteTripChecked(tripId:String){ require(deleteTripRow(tripId)==1){"Unknown trip: $tripId"} }

    @Transaction
    suspend fun insertAndReorderDay(tripId:String,anchorDayId:String?,after:Boolean,newId:String,now:Instant){
        require(tripExists(tripId)){"Unknown trip: $tripId"}
        val ordered=days(tripId).toMutableList()
        val target=if(anchorDayId==null) ordered.size else {
            val index=ordered.indexOfFirst{it.id==anchorDayId}
            require(index>=0){"Unknown anchor for trip: $anchorDayId"}
            index + if(after) 1 else 0
        }
        park(ordered)
        val day=TripDayEntity(newId,tripId,NEW_DAY_POSITION)
        insertDay(day); ordered.add(target,day); reorder(ordered); require(touch(tripId,now)==1)
    }

    @Transaction
    suspend fun moveAndReorderDay(tripId:String,dayId:String,targetIndex:Int,now:Instant){
        require(tripExists(tripId)){"Unknown trip: $tripId"}
        val ordered=days(tripId).toMutableList()
        require(targetIndex in ordered.indices){"Invalid target index: $targetIndex"}
        val current=ordered.indexOfFirst{it.id==dayId}; require(current>=0){"Unknown day for trip: $dayId"}
        park(ordered); val moved=ordered.removeAt(current); ordered.add(targetIndex,moved); reorder(ordered); require(touch(tripId,now)==1)
    }

    @Transaction
    suspend fun deleteAndReorderDay(dayId:String,now:Instant){
        val tripId=requireNotNull(tripIdForDay(dayId)){"Unknown day: $dayId"}
        val ordered=days(tripId); require(ordered.size>1){"Cannot delete the last trip day"}; park(ordered); require(deleteDayRow(dayId)==1); reorder(ordered.filterNot{it.id==dayId}); require(touch(tripId,now)==1)
    }

    private suspend fun park(values:List<TripDayEntity>){ values.forEachIndexed{i,d->require(position(d.id,Long.MIN_VALUE+i)==1)} }
    private suspend fun reorder(values:List<TripDayEntity>){ values.forEachIndexed{i,d->require(position(d.id,i*POSITION_STEP)==1)} }
    companion object { const val POSITION_STEP=1_000L; const val NEW_DAY_POSITION=Long.MAX_VALUE }
}
