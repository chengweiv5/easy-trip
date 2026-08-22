package com.yangchengwei.easytrip.itinerary.ui

import java.math.BigDecimal
import java.math.RoundingMode

fun formatDistance(meters: Int): String {
    require(meters >= 0) { "Distance cannot be negative" }
    if (meters < 1_000) return "$meters 米"
    val kilometers = BigDecimal.valueOf(meters.toLong())
        .divide(BigDecimal.valueOf(1_000L), 1, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return "$kilometers 公里"
}

fun formatDuration(seconds: Int): String {
    require(seconds >= 0) { "Duration cannot be negative" }
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours == 0 -> "$minutes 分钟"
        remainingMinutes == 0 -> "$hours 小时"
        else -> "$hours 小时 $remainingMinutes 分钟"
    }
}
