package com.yangchengwei.easytrip.route.domain
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import kotlinx.coroutines.flow.Flow

data class RouteLegWithEndpoints(val id:String,val version:Long,val status:RouteStatus,val origin:GeoPoint,val destination:GeoPoint,val originCity:String?,val destinationCity:String?,val recommendedMode:TransportMode,val selectedMode:TransportMode?,val distanceMeters:Int?=null,val durationSeconds:Int?=null,val polyline:List<GeoPoint>?=null,val errorKind:RouteErrorKind?=null,val errorCode:String?=null){val actualMode get()=selectedMode?:recommendedMode}
interface RouteLegRepository {
 fun observeDay(dayId:String):Flow<List<RouteLegEntity>>
 fun observePending():Flow<List<RouteLegWithEndpoints>>
 suspend fun get(legId:String):RouteLegWithEndpoints?
 suspend fun requeueTransientFailures():Int
 suspend fun recoverInterruptedCalculations(online:Boolean):Int
 suspend fun repairCorruptPolyline(legId:String,version:Long):Boolean
 suspend fun claimIfVersionMatches(legId:String,version:Long):Boolean
 suspend fun waitForNetworkIfVersionMatches(legId:String,version:Long):Boolean
 suspend fun releaseClaimIfVersionMatches(legId:String,version:Long,online:Boolean):Boolean
 suspend fun completeIfVersionMatches(legId:String,version:Long,result:RouteResult):Boolean
 suspend fun failIfVersionMatches(legId:String,version:Long,failure:RoutePlanOutcome.Failure):Boolean
 suspend fun overrideMode(legId:String,mode:TransportMode,online:Boolean):Boolean
 suspend fun retry(legId:String,online:Boolean):Boolean
}
