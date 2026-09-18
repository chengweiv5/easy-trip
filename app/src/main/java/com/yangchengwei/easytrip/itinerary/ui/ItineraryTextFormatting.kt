package com.yangchengwei.easytrip.itinerary.ui

internal fun itineraryDaySummary(dayNumber: Int?, stopCount: Int): String =
    if (dayNumber == null) "$stopCount 站" else "第 $dayNumber 天 · $stopCount 站"
