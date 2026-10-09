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

    @Test fun omittedDates_createOneUndatedDayWithoutInventingADepartureDate() {
        val result = validateCreateTrip(CreateTripUiState(name = " 东京 "))

        assertNull(result.dateError)
        assertEquals(CreateTrip("东京", 1, TravelMode.FLEXIBLE, null), result.valid?.command)
        assertNull(result.valid?.startDate)
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

    @Test fun partiallySelectedRangeIsStillRejected() {
        val date = LocalDate.of(2026, 10, 1)
        for (state in listOf(
            CreateTripUiState(name = "东京", startDate = date),
            CreateTripUiState(name = "东京", endDate = date),
        )) {
            assertEquals("请选择开始和结束日期", validateCreateTrip(state).dateError)
            assertNull(createTripCommand(state))
        }
    }

    @Test fun blankNameWithoutDatesOnlyReturnsNameError() {
        val result = validateCreateTrip(CreateTripUiState(name = "  "))
        assertEquals("请输入旅行名称", result.nameError)
        assertNull(result.dateError)
        assertNull(result.valid)
        assertNull(createTripCommand(CreateTripUiState()))
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
