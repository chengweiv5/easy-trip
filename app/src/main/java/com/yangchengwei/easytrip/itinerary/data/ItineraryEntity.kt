package com.yangchengwei.easytrip.itinerary.data
import androidx.room.*
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import java.time.LocalTime
@Entity(tableName="itinerary_items",foreignKeys=[ForeignKey(entity=TripDayEntity::class,parentColumns=["id","tripId"],childColumns=["tripDayId","tripId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=SavedPlaceEntity::class,parentColumns=["id","tripId"],childColumns=["savedPlaceId","tripId"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["savedPlaceId","tripId"]),Index(value=["id","tripDayId"],unique=true),Index(value=["tripDayId","tripId"]),Index(value=["tripDayId","position"],unique=true)]) data class ItineraryItemEntity(@PrimaryKey val id:String,val tripDayId:String,val tripId:String,val savedPlaceId:String,val position:Long,val arrivalTime:LocalTime?=null,val stayDurationMinutes:Int?=null)
