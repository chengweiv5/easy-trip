package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint

/** A destination-local adapter; resolving the city is cancellable with the search/consent session. */
class LocatedCitySearchSource(
    private val delegate: PlaceSearchDataSource,
    private val location: GeoPoint?,
) : PlaceSearchDataSource by delegate {
    private var locatedCity: String? = null

    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
        val searchCity = city?.takeIf(String::isNotBlank) ?: location?.let { point ->
            locatedCity ?: delegate.cityAt(point)?.name?.takeIf(String::isNotBlank)
                ?.also { locatedCity = it }
                ?: error("无法识别当前定位城市，请重试或返回工作台重新定位")
        }
        return delegate.search(keyword, searchCity)
    }
}
