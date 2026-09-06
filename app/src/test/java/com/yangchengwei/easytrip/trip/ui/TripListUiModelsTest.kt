package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.trip.domain.TripSummary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripListUiModelsTest {
    @Test fun mapsUndatedTripWithZeroReadiness() {
        val result = trip(startDate = null, dayCount = 3, placeCount = 0, scheduledPlaceCount = 0)
            .toTripCardUiModel(today = LocalDate.of(2026, 4, 1))

        assertEquals("3天2晚", result.dayCountLabel)
        assertEquals("日期待定", result.countdownLabel)
        assertEquals(0, result.placeCount)
        assertEquals(0, result.scheduledPlaceCount)
        assertEquals("3 天行程", result.tripDayCountLabel)
        assertEquals(0, result.readinessPercent)
        assertEquals("0%", result.readinessLabel)
        assertNull(result.dateLabel)
    }

    @Test fun mapsSameMonthAndCrossMonthDateRanges() {
        val today = LocalDate.of(2026, 4, 1)

        assertEquals(
            "4月12日 — 4月14日",
            trip(startDate = LocalDate.of(2026, 4, 12), dayCount = 3).toTripCardUiModel(today).dateLabel,
        )
        assertEquals(
            "4月30日 — 5月2日",
            trip(startDate = LocalDate.of(2026, 4, 30), dayCount = 3).toTripCardUiModel(today).dateLabel,
        )
    }

    @Test fun mapsCrossYearRangeAndSingleDayNights() {
        val result = trip(startDate = LocalDate.of(2026, 12, 31), dayCount = 1)
            .toTripCardUiModel(today = LocalDate.of(2026, 12, 1))
        val crossYear = trip(startDate = LocalDate.of(2026, 12, 31), dayCount = 3)
            .toTripCardUiModel(today = LocalDate.of(2026, 12, 1))

        assertEquals("1天0晚", result.dayCountLabel)
        assertEquals("2026年12月31日", result.dateLabel)
        assertEquals("2026年12月31日 — 2027年1月2日", crossYear.dateLabel)
    }

    @Test fun mapsFutureOngoingAndEndedCountdowns() {
        val today = LocalDate.of(2026, 4, 10)

        assertEquals("还有 2 天", trip(startDate = today.plusDays(2), dayCount = 3).toTripCardUiModel(today).countdownLabel)
        assertEquals("旅行中", trip(startDate = today.minusDays(1), dayCount = 3).toTripCardUiModel(today).countdownLabel)
        assertEquals("旅行中", trip(startDate = today.minusDays(2), dayCount = 3).toTripCardUiModel(today).countdownLabel)
        assertEquals("已结束", trip(startDate = today.minusDays(3), dayCount = 3).toTripCardUiModel(today).countdownLabel)
    }

    @Test fun roundsReadinessFromScheduledDistinctPlaces() {
        val result = trip(placeCount = 3, scheduledPlaceCount = 2)
            .toTripCardUiModel(today = LocalDate.of(2026, 4, 1))

        assertEquals(3, result.placeCount)
        assertEquals(2, result.scheduledPlaceCount)
        assertEquals("3 个地点", result.placeCountLabel)
        assertEquals("3 天行程", result.tripDayCountLabel)
        assertEquals(67, result.readinessPercent)
        assertEquals("67%", result.readinessLabel)
    }

    @Test fun mapsDatedSelfDriveTrip() {
        val result = trip(
            startDate = LocalDate.of(2026, 10, 1),
            travelMode = TravelMode.SELF_DRIVE,
            dayCount = 5,
        ).toTripCardUiModel(today = LocalDate.of(2026, 9, 1))

        assertEquals("5天4晚", result.dayCountLabel)
        assertEquals("10月1日 — 10月5日", result.dateLabel)
        assertEquals("自驾", result.travelModeLabel)
    }

    @Test fun deletionStateKeepsTargetIdentityAndExactConfirmation() {
        val confirmation = ConfirmationUiModel(
            title = "删除京都？",
            message = "不可撤销",
            deletedItems = emptyList(),
            retainedItems = emptyList(),
            confirmLabel = "删除",
            dismissLabel = "取消",
            destructive = true,
            reversible = false,
        )

        assertEquals(
            TripDeletionUiState.LoadingImpact("trip-1", "京都"),
            TripDeletionUiState.LoadingImpact("trip-1", "京都"),
        )
        assertEquals(
            "trip-1",
            TripDeletionUiState.ImpactFailure("trip-1", "京都", "失败").tripId,
        )
        assertEquals(
            confirmation,
            TripDeletionUiState.Ready("trip-1", "京都", confirmation).confirmation,
        )
    }

    private fun trip(
        startDate: LocalDate? = LocalDate.of(2026, 4, 12),
        travelMode: TravelMode = TravelMode.FLEXIBLE,
        dayCount: Int = 3,
        placeCount: Int = 0,
        scheduledPlaceCount: Int = 0,
    ) = TripSummary(
        id = "trip",
        name = "京都",
        startDate = startDate,
        travelMode = travelMode,
        dayCount = dayCount,
        placeCount = placeCount,
        scheduledDistinctPlaceCount = scheduledPlaceCount,
    )
}
