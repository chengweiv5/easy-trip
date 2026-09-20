package com.yangchengwei.easytrip.itinerary.ui

import java.math.BigDecimal
import java.math.RoundingMode

internal fun formatStayHours(minutes: Int): String =
    BigDecimal(minutes).divide(BigDecimal(60), 2, RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString() + " 小时"

/** Keep an existing fractional/long stay selectable without changing its stored minutes. */
internal fun stayMinuteOptions(existingMinutes: Int?): List<Int?> =
    listOf(null) + ((0..24).map { it * 60 } + listOfNotNull(existingMinutes))
        .filter { it >= 0 }.distinct().sorted()
