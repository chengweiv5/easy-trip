package com.yangchengwei.easytrip.place.domain

data class PlaceCity(val name: String, val adCode: String?) {
    val key: String get() = adCode ?: "name:$name"
}

/** AMap cityCode is a telephone area code; only adCode identifies an administrative area. */
fun administrativeCity(cityName: String?, adCode: String?, districtName: String? = null): PlaceCity? {
    val code = adCode?.trim()?.takeIf { it.matches(Regex("[0-9]{6}")) && it != "000000" }
    val municipality = mapOf("11" to "北京市", "12" to "天津市", "31" to "上海市", "50" to "重庆市")
    municipality[code?.take(2)]?.let { return PlaceCity(it, code!!.take(2) + "0000") }
    val directCounty = code?.take(4) in setOf("4190", "4290", "4690", "6590")
    val name = (if (directCounty) districtName?.takeIf { it.isNotBlank() } ?: cityName else cityName)
        ?.trim()?.takeUnless { it.isBlank() || it.contains("直辖县级") || it == "市辖区" || it == "县" }
        ?: return null
    val cityCode = when {
        code == null -> null
        directCounty -> code
        code.endsWith("0000") -> null
        else -> code.take(4) + "00"
    }
    return PlaceCity(name, cityCode)
}

fun SavedPlace.city(): PlaceCity? = administrativeCity(cityName, cityAdCode)
