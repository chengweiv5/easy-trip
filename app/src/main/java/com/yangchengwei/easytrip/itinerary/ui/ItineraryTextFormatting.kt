package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.core.ui.formatCount
internal fun itineraryDaySummary(dayNumber: Int?, stopCount: Int): String =
    if (dayNumber == null) "${formatCount(stopCount)} 站" else "第 $dayNumber 天 · ${formatCount(stopCount)} 站"
