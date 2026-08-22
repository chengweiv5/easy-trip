package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.GeoPoint

internal data class RawPlace(val id: String, val title: String, val address: String, val point: GeoPoint?, val cityCode: String?)
internal fun parsePlaces(raw: List<RawPlace>, suggestions: List<String>): List<PlaceCandidate> {
    val places = raw.mapNotNull { item ->
        item.point?.takeIf { item.id.isNotBlank() && item.title.isNotBlank() }?.let { PlaceCandidate(item.id, item.title, item.address, it, item.cityCode) }
    }
    if (places.isEmpty()) throw AmapServiceException("POI_EMPTY", 1000, "No candidates; suggestions=$suggestions")
    return places
}
