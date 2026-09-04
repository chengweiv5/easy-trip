package com.yangchengwei.easytrip.route.data
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.route.domain.*
import kotlinx.coroutines.flow.map
class RoomRouteLegRepository(private val dao:RouteLegDao):RouteLegRepository {
 override fun observeDay(dayId:String)=dao.observeLegs(dayId)
 override fun observePending()=dao.observePending().map{rows->rows.mapNotNull{row->row.modelOrRepair()}}
 override suspend fun get(legId:String)=dao.endpoint(legId)?.modelOrRepair()
 override suspend fun requeueTransientFailures()=dao.requeueTransientFailures()
 override suspend fun recoverInterruptedCalculations(online:Boolean)=dao.recoverCalculating(if(online)RouteStatus.PENDING else RouteStatus.WAITING_NETWORK)
 override suspend fun repairCorruptPolyline(legId:String,version:Long)=dao.repairPolyline(legId,version)==1
 override suspend fun claimIfVersionMatches(legId:String,version:Long)=dao.claim(legId,version)==1
 override suspend fun waitForNetworkIfVersionMatches(legId:String,version:Long)=dao.waitNetwork(legId,version)==1
 override suspend fun releaseClaimIfVersionMatches(legId:String,version:Long,online:Boolean)=dao.releaseClaim(legId,version,if(online)RouteStatus.PENDING else RouteStatus.WAITING_NETWORK)==1
 override suspend fun completeIfVersionMatches(legId:String,version:Long,result:RouteResult)=dao.complete(legId,version,result.distanceMeters,result.durationSeconds,PolylineCodec.encode(result.polyline))==1
 override suspend fun failIfVersionMatches(legId:String,version:Long,failure:RoutePlanOutcome.Failure)=dao.fail(legId,version,failure.kind,failure.code)==1
 override suspend fun updateDetails(legId:String,selectedModeOverride:TransportMode?,durationOverrideSeconds:Int?,note:String?,online:Boolean):Boolean {
  val normalizedDurationOverrideSeconds = durationOverrideSeconds?.takeIf { it > 0 }
  return dao.updateDetails(legId,selectedModeOverride,normalizedDurationOverrideSeconds,note,online)==1
 }
 override suspend fun retry(legId:String,online:Boolean):Boolean {
  val version=dao.leg(legId)?.version?:return false
  return retry(legId,version,online)
 }
 override suspend fun retry(legId:String,expectedVersion:Long,online:Boolean)=dao.retry(legId,expectedVersion,if(online)RouteStatus.PENDING else RouteStatus.WAITING_NETWORK)==1
 private suspend fun RouteLegEndpointRow.modelOrRepair():RouteLegWithEndpoints? { val decoded=polyline?.let(PolylineCodec::decode); if(decoded?.isFailure==true){dao.repairPolyline(id,version);return null};return RouteLegWithEndpoints(id,version,status,GeoPoint(originLatitude,originLongitude),GeoPoint(destinationLatitude,destinationLongitude),originCity,destinationCity,recommendedMode,selectedMode,distanceMeters,durationSeconds,decoded?.getOrNull(),errorKind,errorCode) }
}
