package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.ui.placeCityKey

data class PlacePoolMapFilter(val cityKey: String? = null, val tagIds: Set<String> = emptySet()) {
    val isActive: Boolean get() = cityKey != null || tagIds.isNotEmpty()

    fun matches(place: SavedPlace): Boolean =
        (cityKey == null || placeCityKey(place) == cityKey) && tagIds.all { id -> place.tags.any { it.id == id } }
}
