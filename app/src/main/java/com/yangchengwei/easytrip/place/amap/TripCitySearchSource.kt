package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint

fun tripSearchCityResolver(
    source: PlaceSearchDataSource,
    title: suspend () -> String,
    locationSession: AppLocationSession?,
    lastLocatedPoint: GeoPoint? = null,
): TripSearchCityResolver = TripSearchCityResolver(
    title,
    source::cityCatalog,
    {
        if (locationSession != null) locationSession.currentCity()
        else lastLocatedPoint?.let { source.cityAt(it) }
            ?: throw LocationPermissionRequired()
    },
)

/** Keeps the keyword intact while using the same city policy as the assistant. */
class TripCitySearchSource(
    private val delegate: PlaceSearchDataSource,
    private val resolver: TripSearchCityResolver,
    private val retryLocation: () -> Unit = {},
) : PlaceSearchDataSource by delegate {
    override fun retryFailedLocation() = retryLocation()

    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
        val resolved = resolver.resolve(keyword, city)
            ?: throw CurrentLocationUnavailable("请在搜索框中指定城市，或允许定位后重试。")
        return delegate.search(keyword, resolved)
    }
}
