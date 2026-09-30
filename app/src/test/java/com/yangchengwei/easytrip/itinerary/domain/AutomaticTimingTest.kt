package com.yangchengwei.easytrip.itinerary.domain

import com.yangchengwei.easytrip.core.model.TransportMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class AutomaticTimingTest {
    @Test fun arrivalUsesFullStayAndRoundsTravelUpToTheNextMinute() {
        assertEquals(LocalTime.of(9, 16), arrivalAfter(LocalTime.of(8, 0), 60, 901))
        assertEquals(LocalTime.of(9, 15), arrivalAfter(LocalTime.of(8, 0), 60, 900))
        assertEquals(LocalTime.of(8, 0), arrivalAfter(LocalTime.of(8, 0), 0, 0))
    }

    @Test fun arrivalNeverWrapsPastMidnightOrOverflows() {
        assertEquals(LocalTime.of(23, 59), arrivalAfter(LocalTime.of(22, 59), 60, 0))
        assertNull(arrivalAfter(LocalTime.of(23, 0), 60, 0))
        assertNull(arrivalAfter(LocalTime.of(23, 59), 0, 1))
        assertNull(arrivalAfter(LocalTime.of(8, 0), Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test fun provisionalTravelDependsOnTransportModeAndKeepsSamePlaceAtZero() {
        assertEquals(1_080, estimatedTravelSeconds(1_000.0, TransportMode.WALK))
        assertEquals(480, estimatedTravelSeconds(1_000.0, TransportMode.DRIVE))
        assertEquals(480, estimatedTravelSeconds(1_000.0, TransportMode.TAXI))
        assertEquals(840, estimatedTravelSeconds(1_000.0, TransportMode.TRANSIT))
        TransportMode.entries.forEach { assertEquals(0, estimatedTravelSeconds(0.0, it)) }
    }
}
