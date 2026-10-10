package com.yangchengwei.easytrip.amap

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.district.DistrictItem
import com.amap.api.services.district.DistrictResult
import com.amap.api.services.district.DistrictSearch
import com.amap.api.services.district.DistrictSearchQuery
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.workspace.TripCity
import com.yangchengwei.easytrip.workspace.firstTripCity

/** Uses the consent-gated SDK's administrative catalog, not a hardcoded popular-city list. */
internal suspend fun lookupTripCity(context: Context, consent: AmapConsentToken, title: String): TripCity? {
    if (title.isBlank()) return null
    val country = searchDistrict(context, consent, "中国", children = 3)
    fun flatten(item: DistrictItem): List<DistrictItem> = listOf(item) + item.subDistrict.orEmpty().flatMap(::flatten)
    val cities = country.flatMap(::flatten).filter {
        it.name?.endsWith("市") == true || it.level == "city"
    }.mapNotNull { item ->
        val point = item.center ?: return@mapNotNull null
        val code = item.adcode?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        TripCity(code, item.name ?: return@mapNotNull null, GeoPoint(point.latitude, point.longitude))
    }
    val city = firstTripCity(title, cities) ?: return null
    val details = searchDistrict(context, consent, city.code, children = 0, boundary = true)
        .firstOrNull { it.adcode == city.code } ?: return city
    val points = details.districtBoundary().orEmpty().flatMap { polygon ->
        polygon.split(';').mapNotNull { coordinate ->
            val pair = coordinate.split(',')
            val longitude = pair.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null
            val latitude = pair.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
            GeoPoint(latitude, longitude).takeIf { latitude in -90.0..90.0 && longitude in -180.0..180.0 }
        }
    }
    val bounds = if (points.isEmpty()) emptyList() else listOf(
        GeoPoint(points.minOf { it.latitude }, points.minOf { it.longitude }),
        GeoPoint(points.maxOf { it.latitude }, points.maxOf { it.longitude }),
    )
    return city.copy(bounds = bounds)
}

private suspend fun searchDistrict(
    context: Context, consent: AmapConsentToken, keyword: String, children: Int, boundary: Boolean = false,
): List<DistrictItem> {
    consent.validateActive()
    val search = DistrictSearch(context.applicationContext)
    search.query = DistrictSearchQuery().apply {
        keywords = keyword
        subDistrict = children
        isShowChild = children > 0
        isShowBoundary = boundary
    }
    return awaitSdkCallback("TRIP_CITY", object : CallbackBoundary<DistrictResult?> {
        override fun install(listener: (Result<DistrictResult?>) -> Unit) {
            search.setOnDistrictSearchListener { listener(Result.success(it)) }
        }
        override fun clear() = search.setOnDistrictSearchListener(null)
        override fun start() = search.searchDistrictAsyn()
    }) { result ->
        consent.validateActive()
        val code = result?.aMapException?.errorCode
        if (code != AMapException.CODE_AMAP_SUCCESS) throw AmapServiceException("TRIP_CITY", code ?: 0, "City lookup failed")
        result.district.orEmpty()
    }
}
