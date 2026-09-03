package com.yangchengwei.easytrip.route.data
import androidx.room.*
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.route.domain.RouteErrorKind
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import java.time.Instant
@Entity(tableName="route_legs",foreignKeys=[ForeignKey(entity=TripDayEntity::class,parentColumns=["id"],childColumns=["tripDayId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=ItineraryItemEntity::class,parentColumns=["id","tripDayId"],childColumns=["fromItemId","tripDayId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=ItineraryItemEntity::class,parentColumns=["id","tripDayId"],childColumns=["toItemId","tripDayId"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["fromItemId","tripDayId"]),Index(value=["toItemId","tripDayId"]),Index(value=["tripDayId","fromItemId","toItemId"],unique=true)]) data class RouteLegEntity(@PrimaryKey val id:String,val tripDayId:String,val fromItemId:String,val toItemId:String,val recommendedMode:TransportMode,val selectedMode:TransportMode?=null,val status:RouteStatus,val distanceMeters:Int?=null,val durationSeconds:Int?=null,val polyline:String?=null,val errorKind:RouteErrorKind?=null,val errorCode:String?=null,val version:Long=0,val updatedAt:Instant,val durationOverrideSeconds:Int?=null,val note:String?=null)
