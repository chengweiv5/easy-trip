package com.yangchengwei.easytrip.route.data
import androidx.room.*
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.route.domain.RouteErrorKind
import kotlinx.coroutines.flow.Flow

data class RouteLegEndpointRow(val id:String,val version:Long,val status:RouteStatus,val recommendedMode:TransportMode,val selectedMode:TransportMode?,val distanceMeters:Int?,val durationSeconds:Int?,val polyline:String?,val errorKind:RouteErrorKind?,val errorCode:String?,val originLatitude:Double,val originLongitude:Double,val destinationLatitude:Double,val destinationLongitude:Double,val originCity:String?,val destinationCity:String?)
@Dao interface RouteLegDao {
 @Query("SELECT l.* FROM route_legs l JOIN itinerary_items i ON i.id=l.fromItemId AND i.tripDayId=l.tripDayId WHERE l.tripDayId=:dayId ORDER BY i.position,i.id") suspend fun legs(dayId:String):List<RouteLegEntity>
 @Query("SELECT l.* FROM route_legs l JOIN itinerary_items i ON i.id=l.fromItemId AND i.tripDayId=l.tripDayId WHERE l.tripDayId=:dayId ORDER BY i.position,i.id") fun observeLegs(dayId:String):Flow<List<RouteLegEntity>>
 @Insert suspend fun insert(leg:RouteLegEntity)
 @Query("DELETE FROM route_legs WHERE tripDayId=:dayId AND fromItemId=:fromItemId AND toItemId=:toItemId") suspend fun deleteEdge(dayId:String,fromItemId:String,toItemId:String):Int
 @Query("""SELECT l.id,l.version,l.status,l.recommendedMode,l.selectedMode,l.distanceMeters,l.durationSeconds,l.polyline,l.errorKind,l.errorCode,fp.latitude AS originLatitude,fp.longitude AS originLongitude,tp.latitude AS destinationLatitude,tp.longitude AS destinationLongitude,fp.cityCode AS originCity,tp.cityCode AS destinationCity FROM route_legs l JOIN itinerary_items fi ON fi.id=l.fromItemId AND fi.tripDayId=l.tripDayId JOIN saved_places fp ON fp.id=fi.savedPlaceId AND fp.tripId=fi.tripId JOIN itinerary_items ti ON ti.id=l.toItemId AND ti.tripDayId=l.tripDayId JOIN saved_places tp ON tp.id=ti.savedPlaceId AND tp.tripId=ti.tripId WHERE l.id=:legId""") suspend fun endpoint(legId:String):RouteLegEndpointRow?
 @Query("""SELECT l.id,l.version,l.status,l.recommendedMode,l.selectedMode,l.distanceMeters,l.durationSeconds,l.polyline,l.errorKind,l.errorCode,fp.latitude AS originLatitude,fp.longitude AS originLongitude,tp.latitude AS destinationLatitude,tp.longitude AS destinationLongitude,fp.cityCode AS originCity,tp.cityCode AS destinationCity FROM route_legs l JOIN itinerary_items fi ON fi.id=l.fromItemId AND fi.tripDayId=l.tripDayId JOIN saved_places fp ON fp.id=fi.savedPlaceId AND fp.tripId=fi.tripId JOIN itinerary_items ti ON ti.id=l.toItemId AND ti.tripDayId=l.tripDayId JOIN saved_places tp ON tp.id=ti.savedPlaceId AND tp.tripId=ti.tripId WHERE l.status IN ('PENDING','WAITING_NETWORK')""") fun observePending():Flow<List<RouteLegEndpointRow>>
 @Query("UPDATE route_legs SET status='PENDING',version=version+1 WHERE status='FAILED' AND errorKind='TRANSIENT'") suspend fun requeueTransientFailures():Int
 @Query("UPDATE route_legs SET status=:status,version=version+1 WHERE status='CALCULATING'") suspend fun recoverCalculating(status:RouteStatus):Int
 @Query("UPDATE route_legs SET status='PENDING',version=version+1,distanceMeters=NULL,durationSeconds=NULL,polyline=NULL,errorKind=NULL,errorCode=NULL WHERE id=:id AND version=:version") suspend fun repairPolyline(id:String,version:Long):Int
 @Query("UPDATE route_legs SET status='CALCULATING',errorKind=NULL,errorCode=NULL WHERE id=:id AND version=:version AND status IN ('PENDING','WAITING_NETWORK')") suspend fun claim(id:String,version:Long):Int
 @Query("UPDATE route_legs SET status='WAITING_NETWORK' WHERE id=:id AND version=:version AND status='PENDING'") suspend fun waitNetwork(id:String,version:Long):Int
 @Query("UPDATE route_legs SET status=:status WHERE id=:id AND version=:version AND status='CALCULATING'") suspend fun releaseClaim(id:String,version:Long,status:RouteStatus):Int
 @Query("UPDATE route_legs SET status='SUCCESS',distanceMeters=:distance,durationSeconds=:duration,polyline=:polyline,errorKind=NULL,errorCode=NULL WHERE id=:id AND version=:version AND status='CALCULATING'") suspend fun complete(id:String,version:Long,distance:Int,duration:Int,polyline:String):Int
 @Query("UPDATE route_legs SET status='FAILED',errorKind=:kind,errorCode=:code WHERE id=:id AND version=:version AND status='CALCULATING'") suspend fun fail(id:String,version:Long,kind:RouteErrorKind,code:String?):Int
 @Query("UPDATE route_legs SET selectedMode=:mode WHERE id=:legId") suspend fun selectMode(legId:String,mode:TransportMode?):Int
 @Query("SELECT * FROM route_legs WHERE id=:legId") suspend fun leg(legId:String):RouteLegEntity?
 @Query("UPDATE route_legs SET durationOverrideSeconds=:durationOverrideSeconds,note=:note WHERE id=:legId") suspend fun updateMetadata(legId:String,durationOverrideSeconds:Int?,note:String?):Int
 @Query("UPDATE route_legs SET selectedMode=:selectedModeOverride,durationOverrideSeconds=:durationOverrideSeconds,note=:note,version=version+1,status=:status,distanceMeters=NULL,durationSeconds=NULL,polyline=NULL,errorKind=NULL,errorCode=NULL WHERE id=:legId") suspend fun updateModeAndMetadata(legId:String,selectedModeOverride:TransportMode?,durationOverrideSeconds:Int?,note:String?,status:RouteStatus):Int
 @Transaction suspend fun updateDetails(legId:String,selectedModeOverride:TransportMode?,durationOverrideSeconds:Int?,note:String?,online:Boolean):Int {
  val current=leg(legId)?:return 0
  return if(selectedModeOverride == current.selectedMode) updateMetadata(legId,durationOverrideSeconds,note) else updateModeAndMetadata(legId,selectedModeOverride,durationOverrideSeconds,note,if(online)RouteStatus.PENDING else RouteStatus.WAITING_NETWORK)
 }
 @Query("SELECT * FROM route_legs WHERE tripDayId IN (SELECT id FROM trip_days WHERE tripId=:tripId)") suspend fun legsForTrip(tripId:String):List<RouteLegEntity>
 @Query("UPDATE route_legs SET recommendedMode=:mode,version=version+1,status=:status,distanceMeters=NULL,durationSeconds=NULL,polyline=NULL,errorKind=NULL,errorCode=NULL WHERE id=:id") suspend fun resetRecommendation(id:String,mode:TransportMode,status:RouteStatus):Int
 @Query("UPDATE route_legs SET version=version+1,status=:status,distanceMeters=NULL,durationSeconds=NULL,polyline=NULL,errorKind=NULL,errorCode=NULL WHERE id=:id AND status='FAILED' AND version=:version") suspend fun retry(id:String,version:Long,status:RouteStatus):Int
}
