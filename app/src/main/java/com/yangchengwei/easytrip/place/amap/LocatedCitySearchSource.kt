package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint

/** A destination-local adapter; resolving the city is cancellable with the search/consent session. */
class LocatedCitySearchSource(
    private val delegate: PlaceSearchDataSource,
    private val location: GeoPoint?,
    private val locationSession: AppLocationSession? = null,
) : PlaceSearchDataSource by delegate {
    private var locatedCity: String? = null

    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
        if (city.isNullOrBlank() && locationSession != null) {
            return delegate.search(keyword, locationSession.currentCity().name)
        }
        val searchCity = city?.takeIf(String::isNotBlank) ?: location?.let { point ->
            locatedCity ?: delegate.cityAt(point)?.name?.takeIf(String::isNotBlank)
                ?.also { locatedCity = it }
                ?: throw CurrentLocationUnavailable("无法识别当前定位城市，请重试")
        } ?: throw CurrentLocationUnavailable("无法获取当前位置，请允许定位后重试")
        return delegate.search(keyword, searchCity)
    }
}
