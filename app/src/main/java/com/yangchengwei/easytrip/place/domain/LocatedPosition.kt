package com.yangchengwei.easytrip.place.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Foreground location only; never persisted with a search result or saved place. */
data class LocatedPosition(val point: GeoPoint, val city: PlaceCity? = null)

fun GeoPoint.isUsableLocation(): Boolean =
    latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        (latitude != 0.0 || longitude != 0.0)

fun distanceFromMeLabel(location: GeoPoint?, destination: GeoPoint?): String? {
    if (location?.isUsableLocation() != true || destination?.isUsableLocation() != true) return null
    val lat = Math.toRadians(destination.latitude - location.latitude)
    val lon = Math.toRadians(destination.longitude - location.longitude)
    val a = (sin(lat / 2) * sin(lat / 2) + cos(Math.toRadians(location.latitude)) *
        cos(Math.toRadians(destination.latitude)) * sin(lon / 2) * sin(lon / 2)).coerceIn(0.0, 1.0)
    val meters = 6_371_008.8 * 2 * atan2(sqrt(a), sqrt(1 - a))
    val distance = when {
        meters < 100 -> "100 米内"
        meters < 995 -> "${(meters / 10).roundToInt() * 10} 米"
        else -> "${String.format(Locale.ROOT, "%.1f", meters / 1000)} 公里"
    }
    return "距我 $distance · 直线"
}

/** Telephone area codes are not city identifiers (e.g. Zhengzhou and Dengfeng). */
fun PlaceCandidate.isInCity(city: PlaceCity): Boolean {
    val candidateCity = if (cityMetadataVersion > 0 && !cityName.isNullOrBlank()) {
        PlaceCity(cityName, cityAdCode)
    } else administrativeCity(cityName, cityAdCode)
    candidateCity ?: return false
    val left = candidateCity.adCode?.takeIf { it.matches(Regex("[0-9]{6}")) && it != "000000" }
    val right = city.adCode?.takeIf { it.matches(Regex("[0-9]{6}")) && it != "000000" }
    return if (left != null && right != null) left == right
    else candidateCity.name.trim().isNotEmpty() && candidateCity.name.trim() == city.name.trim()
}
