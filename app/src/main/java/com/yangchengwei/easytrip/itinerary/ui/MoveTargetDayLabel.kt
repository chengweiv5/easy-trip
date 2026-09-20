package com.yangchengwei.easytrip.itinerary.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun moveTargetDayLabel(index: Int, startDate: LocalDate?): String {
    val date = startDate?.plusDays(index.toLong())?.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA)) ?: "日期待定"
    return "第 ${index + 1} 天 · $date"
}
