package com.yangchengwei.easytrip.route.domain

import com.yangchengwei.easytrip.core.model.GeoPoint

enum class RouteMode { WALK, DRIVE, TRANSIT }

data class RouteRequest(
    val origin: GeoPoint,
    val destination: GeoPoint,
    val mode: RouteMode,
    val originCity: String? = null,
    val destinationCity: String? = null,
)

data class RouteResult(
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polyline: List<GeoPoint>,
)

interface RouteDataSource {
    suspend fun plan(request: RouteRequest): RouteResult
}
