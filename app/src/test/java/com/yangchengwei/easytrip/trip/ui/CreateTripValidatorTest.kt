package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateTripValidatorTest {
    @Test fun blankName_returnsNameError() {
        assertEquals(
            "请输入旅行名称",
            validateCreateTrip(
                CreateTripUiState(
                    startDate = LocalDate.of(2026, 10, 1),
                    endDate = LocalDate.of(2026, 10, 2),
                ),
            ).nameError,
        )
    }

    @Test fun missingCompleteRange_returnsDateError() {
        assertEquals("请选择开始和结束日期", validateCreateTrip(CreateTripUiState(name = "东京")).dateError)
    }

    @Test fun reversedRange_returnsDateError() {
        assertEquals(
            "结束日期不能早于开始日期",
            validateCreateTrip(
                CreateTripUiState(
                    name = "东京",
                    startDate = LocalDate.of(2026, 10, 3),
                    endDate = LocalDate.of(2026, 10, 1),
                ),
            ).dateError,
        )
    }

    @Test fun rangeLongerThanThirtyDays_returnsMaximumErrorAndNoCommand() {
        val result = validateCreateTrip(
            CreateTripUiState(
                name = "东京",
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2026, 10, 31),
            ),
        )

        assertEquals("旅行最多 30 天", result.dateError)
        assertNull(result.valid)
    }

    @Test fun inclusiveRangeDerivesDayCountAndMapsStartDate() {
        val result = validateCreateTrip(
            CreateTripUiState(
                name = " 东京 ",
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2026, 10, 3),
            ),
        ).valid!!

        assertEquals(CreateTrip("东京", 3, TravelMode.FLEXIBLE, LocalDate.of(2026, 10, 1)), result.command)
        assertEquals(LocalDate.of(2026, 10, 1), result.startDate)
    }

    @Test fun localDateMaxSingleDayRangeIsValid() {
        val result = validateCreateTrip(
            CreateTripUiState(
                name = "边界旅行",
                startDate = LocalDate.MAX,
                endDate = LocalDate.MAX,
            ),
        )

        assertEquals(1, result.valid?.command?.dayCount)
        assertNull(result.dateError)
    }
}
