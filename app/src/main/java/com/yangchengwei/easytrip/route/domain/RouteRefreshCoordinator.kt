package com.yangchengwei.easytrip.route.domain
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.network.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select

interface RouteRefreshCoordinator { fun start(scope:CoroutineScope); suspend fun retry(legId:String):Boolean; suspend fun updateDetails(legId:String,selectedModeOverride:TransportMode?,durationOverrideSeconds:Int?,note:String?):Boolean }
class DefaultRouteRefreshCoordinator(private val repository:RouteLegRepository,private val planner:RoutePlanner,private val networkMonitor:NetworkMonitor,private val onRequeueError:(Throwable)->Unit={}):RouteRefreshCoordinator {
 private val lock=Any(); private var collector:Job?=null; private lateinit var ownerScope:CoroutineScope; private val active=mutableMapOf<String,Active>()
 override fun start(scope:CoroutineScope)=synchronized(lock){if(collector?.isActive==true)return@synchronized;ownerScope=scope;collector=scope.launch(start=CoroutineStart.UNDISPATCHED){val initialOnline=networkMonitor.isOnline.value;try{repository.recoverInterruptedCalculations(initialOnline);if(initialOnline)repository.requeueTransientFailures()}catch(error:CancellationException){throw error}catch(error:Throwable){onRequeueError(error)};launch{combine(repository.observePending(),networkMonitor.isOnline){legs,_->legs}.collect{it.forEach(::schedule)}};launch{val risingEdges=Channel<Unit>(Channel.UNLIMITED);launch{for(edge in risingEdges){try{repository.requeueTransientFailures()}catch(error:CancellationException){throw error}catch(error:Throwable){onRequeueError(error)}}};var previous=initialOnline;networkMonitor.isOnline.collect{current->val rising=!previous&&current;previous=current;if(rising)risingEdges.send(Unit)}}}}
override suspend fun retry(legId:String)=repository.retry(legId,networkMonitor.isOnline.value)
 override suspend fun updateDetails(legId:String,selectedModeOverride:TransportMode?,durationOverrideSeconds:Int?,note:String?)=repository.updateDetails(legId,selectedModeOverride,durationOverrideSeconds,note,networkMonitor.isOnline.value)
 private fun schedule(leg:RouteLegWithEndpoints){synchronized(lock){val old=active[leg.id];if(old?.job?.isActive==true){if(old.version==leg.version)return;old.job.cancel()};lateinit var job:Job;job=ownerScope.launch(start=CoroutineStart.DEFAULT){var claimed=false;try{try{if(!networkMonitor.isOnline.value){repository.waitForNetworkIfVersionMatches(leg.id,leg.version);return@launch};if(!repository.claimIfVersionMatches(leg.id,leg.version))return@launch;claimed=true;val outcome=coroutineScope{val request=async{planner.plan(leg)};val disconnected=async{networkMonitor.isOnline.filter{!it}.first()};try{select<RoutePlanOutcome>{request.onAwait{it};disconnected.onAwait{throw RouteDisconnectedException()}}}finally{request.cancel();disconnected.cancel()}};when(outcome){is RoutePlanOutcome.Success->repository.completeIfVersionMatches(leg.id,leg.version,outcome.result);is RoutePlanOutcome.Failure->repository.failIfVersionMatches(leg.id,leg.version,outcome)};claimed=false}catch(e:CancellationException){if(claimed)withContext(NonCancellable){repository.releaseClaimIfVersionMatches(leg.id,leg.version,networkMonitor.isOnline.value)};throw e}catch(e:RouteDisconnectedException){if(claimed)repository.releaseClaimIfVersionMatches(leg.id,leg.version,false)}catch(e:Throwable){if(claimed)runCatching{repository.releaseClaimIfVersionMatches(leg.id,leg.version,networkMonitor.isOnline.value)};onRequeueError(e)}}finally{synchronized(lock){if(active[leg.id]?.job===job)active.remove(leg.id)}}};active[leg.id]=Active(leg.version,job)}}
 private class RouteDisconnectedException:RuntimeException()
 private data class Active(val version:Long,val job:Job)
}
