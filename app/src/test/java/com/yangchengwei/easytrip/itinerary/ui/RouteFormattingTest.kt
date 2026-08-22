package com.yangchengwei.easytrip.itinerary.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RouteFormattingTest {
    @Test
    fun distanceUsesMetersBelowOneKilometer() {
        assertEquals("0 米", formatDistance(0))
        assertEquals("999 米", formatDistance(999))
    }

    @Test
    fun distanceUsesKilometersWithAtMostOneDecimal() {
        assertEquals("1 公里", formatDistance(1_000))
        assertEquals("1.1 公里", formatDistance(1_050))
        assertEquals("10 公里", formatDistance(10_000))
    }

    @Test
    fun durationUsesReadableMinutesAndHours() {
        assertEquals("0 分钟", formatDuration(0))
        assertEquals("12 分钟", formatDuration(12 * 60))
        assertEquals("1 小时", formatDuration(60 * 60))
        assertEquals("1 小时 15 分钟", formatDuration(75 * 60))
    }

    @Test
    fun negativeValuesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { formatDistance(-1) }
        assertThrows(IllegalArgumentException::class.java) { formatDuration(-1) }
    }
}
