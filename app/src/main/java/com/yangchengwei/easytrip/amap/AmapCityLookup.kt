package com.yangchengwei.easytrip.amap

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.place.domain.administrativeCity

internal suspend fun lookupCity(context: Context, consent: AmapConsentToken, point: GeoPoint): PlaceCity? {
    consent.validateActive()
    val search = GeocodeSearch(context)
    return awaitSdkCallback("CITY_REVERSE", object : CallbackBoundary<Pair<RegeocodeResult?, Int>> {
        override fun install(listener: (Result<Pair<RegeocodeResult?, Int>>) -> Unit) {
            search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onGeocodeSearched(result: GeocodeResult?, code: Int) = Unit
                override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) = listener(Result.success(result to code))
            })
        }
        override fun clear() = search.setOnGeocodeSearchListener(null)
        override fun start() = search.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(point.latitude, point.longitude), 200f, GeocodeSearch.AMAP))
    }) { (result, code) ->
        consent.validateActive()
        if (code != AMapException.CODE_AMAP_SUCCESS) throw AmapServiceException("CITY_REVERSE", code, "AMap city lookup failed")
        result?.regeocodeAddress?.let { address ->
            administrativeCity(address.city, address.adCode, address.district)?.copy(
                routeCityCode = address.cityCode?.takeIf(String::isNotBlank) ?: address.city?.takeIf(String::isNotBlank),
            )
        }
    }
}
