package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.place.domain.city
import java.text.Collator
import java.util.Locale

internal const val UNKNOWN_CITY_KEY = "unknown"
internal data class PlaceCityGroup(val key: String, val name: String, val rows: List<SavedPlaceRowUi>)

internal fun placeCityGroups(rows: List<SavedPlaceRowUi>): List<PlaceCityGroup> {
    val collator = Collator.getInstance(Locale.CHINA)
    return rows.groupBy { it.place.city()?.key ?: UNKNOWN_CITY_KEY }.map { (key, places) ->
        PlaceCityGroup(key, places.first().place.city()?.name ?: "未识别城市", places)
    }.sortedWith { a, b ->
        when {
            a.key == UNKNOWN_CITY_KEY -> if (b.key == UNKNOWN_CITY_KEY) 0 else 1
            b.key == UNKNOWN_CITY_KEY -> -1
            else -> collator.compare(a.name, b.name).takeIf { it != 0 } ?: a.key.compareTo(b.key)
        }
    }
}

internal fun filterPlaceCityGroups(rows: List<SavedPlaceRowUi>, cityKey: String?, query: String = ""): List<PlaceCityGroup> {
    val keyword = query.trim()
    return placeCityGroups(rows).filter { cityKey == null || it.key == cityKey }.mapNotNull { group ->
        val matches = group.rows.filter { keyword.isEmpty() || it.name.contains(keyword, true) || it.address.contains(keyword, true) || group.name.contains(keyword, true) }
        if (matches.isEmpty()) null else group.copy(rows = matches)
    }
}
