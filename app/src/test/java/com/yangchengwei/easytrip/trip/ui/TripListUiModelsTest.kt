package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.TripSummary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripListUiModelsTest {
    @Test fun mapsDraftFlexibleTrip() {
        val result = TripSummary("trip", "京都", null, TravelMode.FLEXIBLE, 3).toTripCardUiModel()

        assertEquals("3 天", result.dayCountLabel)
        assertEquals("灵活", result.travelModeLabel)
        assertNull(result.dateLabel)
    }

    @Test fun mapsDatedSelfDriveTrip() {
        val result = TripSummary(
            "trip",
            "京都",
            LocalDate.of(2026, 10, 1),
            TravelMode.SELF_DRIVE,
            5,
        ).toTripCardUiModel()

        assertEquals("5 天", result.dayCountLabel)
        assertEquals("2026年10月1日", result.dateLabel)
        assertEquals("自驾", result.travelModeLabel)
    }
}
