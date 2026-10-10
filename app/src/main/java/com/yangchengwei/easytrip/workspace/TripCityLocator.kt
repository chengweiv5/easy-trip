package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint

data class TripCity(val code: String, val name: String, val center: GeoPoint, val bounds: List<GeoPoint> = emptyList())

/** Match the title's order, not the service's ranking. Ambiguous aliases never choose a random city. */
fun firstTripCity(title: String, cities: List<TripCity>): TripCity? {
    data class Match(val city: TripCity, val index: Int, val length: Int)
    val matches = cities.distinctBy { it.code }.flatMap { city ->
        val aliases = listOf(city.name, city.name.removeSuffix("市")).distinct().filter { it.length >= 2 }
        aliases.mapNotNull { alias ->
            val index = title.indexOf(alias)
            if (index < 0 || title.getOrNull(index + alias.length) == '省') null else Match(city, index, alias.length)
        }
    }
    val first = matches.minOfOrNull { it.index } ?: return null
    val earliest = matches.filter { it.index == first }
    val longest = earliest.maxOf { it.length }
    return earliest.filter { it.length == longest }.map { it.city }.distinctBy { it.code }.singleOrNull()
}
