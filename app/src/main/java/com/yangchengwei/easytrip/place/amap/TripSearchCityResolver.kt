package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.place.domain.explicitlyNamesCity
import com.yangchengwei.easytrip.workspace.TripCity
import com.yangchengwei.easytrip.workspace.citiesInText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Shared search context; map framing and place-pool filters do not decide the search city. */
class TripSearchCityResolver(
    private val tripTitle: suspend () -> String,
    private val cityCatalog: suspend () -> List<TripCity>,
    private val locatedCity: suspend () -> PlaceCity?,
) {
    suspend fun resolve(keyword: String = "", explicitCity: String? = null): String? {
        explicitCity?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        // A failed catalog is not evidence that the title contains no cities.
        val catalog = withTimeoutOrNull(8_000) { cityCatalog() }
            ?: throw IllegalStateException("城市信息查询超时，请重试。")
        citiesInText(keyword, catalog).filter { explicitlyNamesCity(keyword, it.name) }
            .singleOrNull()?.let { return it.name }
        val cities = citiesInText(tripTitle(), catalog)
        if (cities.size == 1) return cities.single().name
        val location = try {
            withTimeoutOrNull(3_000) { locatedCity() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (cities.isEmpty()) throw error
            null
        }
        if (cities.isEmpty()) return location?.name?.takeIf { it.isNotBlank() }
        return cities.firstOrNull { city ->
            location != null && (city.code == location.adCode ||
                city.name.removeSuffix("市") == location.name.removeSuffix("市"))
        }?.name ?: cities.firstOrNull { city ->
            val code = location?.adCode
            code?.matches(Regex("[0-9]{6}")) == true &&
                city.code.matches(Regex("[0-9]{6}")) &&
                (city.code.endsWith("0000") && city.code.take(2) in setOf("11", "12", "31", "50") &&
                    code.startsWith(city.code.take(2)) ||
                    city.code.endsWith("00") && !city.code.endsWith("0000") && code.startsWith(city.code.take(4)))
        }?.name ?: cities.firstOrNull()?.name
    }
}
