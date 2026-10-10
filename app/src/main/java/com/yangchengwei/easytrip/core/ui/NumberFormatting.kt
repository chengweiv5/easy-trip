package com.yangchengwei.easytrip.core.ui

/** Display only: keep exact decimal digits and never feed grouping back into stored/input values. */
fun groupDecimalDigits(value: String): String {
    val parts = value.split('.', limit = 2)
    val integer = parts[0]
    val sign = integer.takeWhile { it == '-' || it == '+' }
    val digits = integer.removePrefix(sign)
    return sign + digits.reversed().chunked(3).joinToString(",").reversed() +
        if (parts.size == 2) ".${parts[1]}" else ""
}

fun formatCount(value: Int): String = groupDecimalDigits(value.toString())
fun formatCount(value: Long): String = groupDecimalDigits(value.toString())
