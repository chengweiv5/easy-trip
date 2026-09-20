package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint

internal data class RawPlace(val id: String, val title: String, val address: String, val point: GeoPoint?, val cityCode: String?, val cityName: String? = null, val adCode: String? = null, val adName: String? = null)
internal fun parsePlaces(raw: List<RawPlace>): List<PlaceCandidate> {
    val places = raw.filter { it.id.isNotBlank() && it.title.isNotBlank() }.map { item ->
        val city = com.yangchengwei.easytrip.place.domain.administrativeCity(item.cityName, item.adCode, item.adName)
        PlaceCandidate(item.id, item.title, item.address, item.point, item.cityCode, city?.name, city?.adCode, if (city?.adCode != null) 1 else 0)
    }
    return places
}
