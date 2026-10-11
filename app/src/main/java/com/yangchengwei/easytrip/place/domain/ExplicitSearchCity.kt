package com.yangchengwei.easytrip.place.domain

/** Recognize an explicit city token, not a road/POI substring such as 南京路 or 北京路. */
fun explicitlyNamesCity(text: String, cityName: String): Boolean {
    val fullName = cityName.trim()
    val shortName = fullName.removeSuffix("市")
    if (shortName.length < 2) return false
    val aliases = listOf(fullName, shortName).distinct()
    val separator = "，,、；;：:。.!！?？/|·→-()（）[]【】"
    fun boundary(value: Char?): Boolean = value == null || value.isWhitespace() || value in separator
    return aliases.any { alias ->
        Regex(Regex.escape(alias)).findAll(text).any { match ->
            val before = text.getOrNull(match.range.first - 1)
            val after = text.getOrNull(match.range.last + 1)
            // "苏州 博物馆", "苏州的博物馆", "苏州市博物馆"; never "苏州路".
            (boundary(before) || before in listOf('在', '去', '到')) &&
                (boundary(after) || after == '的' || alias.endsWith("市") && after !in listOf('路', '街', '道', '巷', '省'))
        }
    }
}
