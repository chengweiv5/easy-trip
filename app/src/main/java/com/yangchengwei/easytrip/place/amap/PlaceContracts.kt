package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint

internal data class RawPlace(val id: String, val title: String, val address: String, val point: GeoPoint?, val cityCode: String?)
internal fun parsePlaces(raw: List<RawPlace>, suggestions: List<String>): List<PlaceCandidate> {
    val places = raw.filter { it.id.isNotBlank() && it.title.isNotBlank() }.map { item ->
        PlaceCandidate(item.id, item.title, item.address, item.point, item.cityCode)
    }
    return places
}
