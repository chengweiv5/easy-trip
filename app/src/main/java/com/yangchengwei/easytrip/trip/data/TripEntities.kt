package com.yangchengwei.easytrip.trip.data
import androidx.room.*
import com.yangchengwei.easytrip.core.model.*
import java.time.*
@Entity(tableName="trips") data class TripEntity(@PrimaryKey val id:String,val name:String,val timeMode:TimeMode,val startDate:LocalDate?=null,val travelMode:TravelMode,val createdAt:Instant,val updatedAt:Instant)
@Entity(tableName="trip_days",foreignKeys=[ForeignKey(entity=TripEntity::class,parentColumns=["id"],childColumns=["tripId"],onDelete=ForeignKey.CASCADE)],indices=[Index("tripId"),Index(value=["id","tripId"],unique=true),Index(value=["tripId","position"],unique=true)]) data class TripDayEntity(@PrimaryKey val id:String,val tripId:String,val position:Long)
