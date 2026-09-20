package com.yangchengwei.easytrip.itinerary.ui

import org.junit.Assert.*
import org.junit.Test

class StayDurationHoursTest {
    @Test fun displaysHoursIncludingExistingFractionalValues() {
        assertEquals("2 小时", formatStayHours(120))
        assertEquals("1.5 小时", formatStayHours(90))
        assertEquals("1.25 小时", formatStayHours(75))
        assertEquals("0 小时", formatStayHours(0))
        assertEquals("0.02 小时", formatStayHours(1))
    }
    @Test fun offersWholeHoursAndPreservesHistoricalValuesWithoutDuplicates() {
        assertEquals(listOf<Int?>(null) + (0..24).map { it * 60 }, stayMinuteOptions(null))
        assertTrue(stayMinuteOptions(75).contains(75))
        assertTrue(stayMinuteOptions(1800).contains(1800))
        assertEquals(1, stayMinuteOptions(120).count { it == 120 })
    }
}
