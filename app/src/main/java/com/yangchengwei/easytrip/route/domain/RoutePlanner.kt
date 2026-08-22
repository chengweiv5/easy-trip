package com.yangchengwei.easytrip.route.domain
import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.TransportMode
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
sealed interface RoutePlanOutcome { data class Success(val result:RouteResult):RoutePlanOutcome; data class Failure(val kind:RouteErrorKind,val code:String?=null):RoutePlanOutcome }
enum class RouteErrorKind { TRANSIENT, NO_ROUTE, UNSUPPORTED_TRANSIT, PERMANENT }
interface RoutePlanner { suspend fun plan(leg:RouteLegWithEndpoints):RoutePlanOutcome }
class DataSourceRoutePlanner(private val source:RouteDataSource):RoutePlanner { override suspend fun plan(leg:RouteLegWithEndpoints):RoutePlanOutcome=try { RoutePlanOutcome.Success(source.plan(RouteRequest(leg.origin,leg.destination,sdkMode(leg.actualMode),leg.originCity,leg.destinationCity))) } catch(e:CancellationException){throw e} catch(e:AmapServiceException){ val op=e.operation.uppercase(); when { op=="TRANSIT_ARGUMENT"||op.contains("UNSUPPORTED_TRANSIT")->RoutePlanOutcome.Failure(RouteErrorKind.UNSUPPORTED_TRANSIT,e.errorCode.toString()); op=="ROUTE_EMPTY"||e.message.orEmpty().contains("no usable path",true)->RoutePlanOutcome.Failure(RouteErrorKind.NO_ROUTE,e.errorCode.toString()); e.errorCode in NETWORK_CODES->RoutePlanOutcome.Failure(RouteErrorKind.TRANSIENT,e.errorCode.toString()); else->RoutePlanOutcome.Failure(RouteErrorKind.PERMANENT,e.errorCode.toString()) } } catch(e:Throwable){ if(e is IOException||e is SocketTimeoutException) RoutePlanOutcome.Failure(RouteErrorKind.TRANSIENT,e.javaClass.simpleName) else RoutePlanOutcome.Failure(RouteErrorKind.PERMANENT,e.javaClass.simpleName) }
 companion object { private val NETWORK_CODES=setOf(1802,1803,1804,3002,3003) } }
fun sdkMode(mode:TransportMode)=when(mode){TransportMode.WALK->RouteMode.WALK;TransportMode.TAXI,TransportMode.DRIVE->RouteMode.DRIVE;TransportMode.TRANSIT->RouteMode.TRANSIT}
