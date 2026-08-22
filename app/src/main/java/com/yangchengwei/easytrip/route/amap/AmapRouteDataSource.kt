package com.yangchengwei.easytrip.route.amap

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.Path
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.WalkRouteResult
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.amap.CallbackBoundary
import com.yangchengwei.easytrip.amap.awaitSdkCallback
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.route.domain.RouteDataSource
import com.yangchengwei.easytrip.route.domain.RouteMode
import com.yangchengwei.easytrip.route.domain.RouteRequest
import com.yangchengwei.easytrip.route.domain.RouteResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private data class RouteCallback(val operation: String, val code: Int, val paths: List<Path>?)

class AmapRouteDataSource(
    context: Context,
    private val consent: AmapConsentToken,
    private val routeSearchFactory: (Context) -> RouteSearch = ::RouteSearch,
) : RouteDataSource {
    private val context = context.applicationContext
    init { consent.validateActive() }

    override suspend fun plan(request: RouteRequest): RouteResult {
        consent.validateActive()
        validateRouteRequest(request)
        val routeSearch = try { routeSearchFactory(context) } catch (error: AMapException) {
            throw AmapServiceException("ROUTE_CREATE", error.errorCode, error.errorMessage.orEmpty())
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw AmapServiceException("ROUTE_CREATE", 0, error.message.orEmpty())
        }
        val query = try { buildQuery(request) } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw AmapServiceException("ROUTE_QUERY", 0, error.message.orEmpty())
        }
        return awaitSdkCallback("ROUTE", object : CallbackBoundary<RouteCallback> {
            override fun install(listener: (Result<RouteCallback>) -> Unit) {
                routeSearch.setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
                    override fun onWalkRouteSearched(result: WalkRouteResult?, code: Int) = listener(Result.success(RouteCallback("WALK_ROUTE", code, result?.paths)))
                    override fun onDriveRouteSearched(result: DriveRouteResult?, code: Int) = listener(Result.success(RouteCallback("DRIVE_ROUTE", code, result?.paths)))
                    override fun onBusRouteSearched(result: BusRouteResult?, code: Int) = listener(Result.success(RouteCallback("TRANSIT_ROUTE", code, result?.paths)))
                    override fun onRideRouteSearched(result: RideRouteResult?, code: Int) = Unit
                })
            }
            override fun clear() = routeSearch.setRouteSearchListener(null)
            override fun start() {
                when (query) {
                    is RouteSearch.WalkRouteQuery -> routeSearch.calculateWalkRouteAsyn(query)
                    is RouteSearch.DriveRouteQuery -> routeSearch.calculateDriveRouteAsyn(query)
                    is RouteSearch.BusRouteQuery -> routeSearch.calculateBusRouteAsyn(query)
                    else -> error("Unsupported route query")
                }
            }
        }) { callback ->
            consent.validateActive()
            if (callback.code != AMapException.CODE_AMAP_SUCCESS) throw AmapServiceException(callback.operation, callback.code, "AMap route search failed")
            parseRouteResult(callback.operation, callback.code) {
                callback.paths.orEmpty().map { path -> RoutePathData(path.distance.toInt(), path.duration.toInt(), path.polyline.orEmpty().map { GeoPoint(it.latitude, it.longitude) }) }
            }
        }
    }

    private fun buildQuery(request: RouteRequest): Any {
        val fromAndTo = RouteSearch.FromAndTo(request.origin.toAmap(), request.destination.toAmap())
        return when (request.mode) {
            RouteMode.WALK -> RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WALK_DEFAULT)
            RouteMode.DRIVE -> RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.DRIVING_SINGLE_DEFAULT, null, null, null)
            RouteMode.TRANSIT -> RouteSearch.BusRouteQuery(fromAndTo, RouteSearch.BUS_DEFAULT, requireNotNull(request.originCity), 0).apply {
                request.destinationCity?.takeIf(String::isNotBlank)?.let(::setCityd)
            }
        }
    }
    private fun GeoPoint.toAmap() = LatLonPoint(latitude, longitude)
}

internal fun parseRouteResult(operation: String, code: Int, paths: () -> List<RoutePathData>): RouteResult {
    val selected = try {
        selectUsablePath(paths())
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        if (error is AmapServiceException) throw AmapServiceException(operation, code, error.message.orEmpty())
        throw AmapServiceException(operation, code, error.message.orEmpty())
    }
    return RouteResult(selected.distanceMeters, selected.durationSeconds, selected.polyline)
}
