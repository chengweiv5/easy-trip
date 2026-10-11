package com.yangchengwei.easytrip.assistant

/** Qualify the map request only; keep the user's name, source span and editable city unchanged. */
internal fun assistantSearchKeyword(query: String, city: String): String {
    val name = query.trim()
    val prefix = city.trim().removeSuffix("市").ifBlank { city.trim() }
    if (prefix.isBlank() || normalizedPlace(name).contains(normalizedPlace(prefix))) return name
    return prefix + name
}
