package com.yangchengwei.easytrip.route.amap

import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.route.domain.RouteMode
import com.yangchengwei.easytrip.route.domain.RouteRequest

internal data class RoutePathData(val distanceMeters: Int, val durationSeconds: Int, val polyline: List<GeoPoint>)
internal fun validateRouteRequest(request: RouteRequest): RouteRequest {
    if (request.mode == RouteMode.TRANSIT && request.originCity.isNullOrBlank()) throw AmapServiceException("TRANSIT_ARGUMENT", 0, "originCity is required")
    return request
}
internal fun selectUsablePath(paths: List<RoutePathData>): RoutePathData = paths.firstOrNull { it.distanceMeters > 0 && it.durationSeconds > 0 && it.polyline.size >= 2 }
    ?: throw AmapServiceException("ROUTE_EMPTY", 1000, "AMap route search returned no usable path")
