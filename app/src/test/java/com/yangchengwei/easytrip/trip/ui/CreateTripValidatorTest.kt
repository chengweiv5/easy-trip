package com.yangchengwei.easytrip.trip.ui

import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateTripValidatorTest {
    @Test fun blankName_returnsNameError() {
        assertEquals("请输入旅行名称", validateCreateTrip(CreateTripUiState(dayCount = "2")).nameError)
    }

    @Test fun dayCountBelowOne_returnsDayCountError() {
        assertEquals("请输入至少 1 天", validateCreateTrip(CreateTripUiState(name = "东京", dayCount = "0")).dayCountError)
    }

    @Test fun datedTripWithoutStartDate_returnsDateError() {
        assertEquals("请选择开始日期", validateCreateTrip(CreateTripUiState(name = "东京", dayCount = "2", timeMode = CreateTimeMode.DATED)).dateError)
    }

    @Test fun validDraft_returnsCommandWithoutStartDate() {
        val result = validateCreateTrip(CreateTripUiState(name = " 东京 ", dayCount = "3")).valid!!
        assertEquals(CreateTrip("东京", 3, TravelMode.FLEXIBLE, null), result.command)
        assertNull(result.startDate)
    }

    @Test fun draftTrip_ignoresStaleStartDate() {
        val result = validateCreateTrip(
            CreateTripUiState(
                name = "东京",
                dayCount = "3",
                timeMode = CreateTimeMode.DRAFT,
                startDate = LocalDate.of(2026, 10, 1),
            ),
        ).valid!!
        assertNull(result.startDate)
    }

    @Test fun validDatedTrip_returnsCommandAndStartDate() {
        val date = LocalDate.of(2026, 10, 1)
        val result = validateCreateTrip(CreateTripUiState(name = "东京", dayCount = "3", timeMode = CreateTimeMode.DATED, startDate = date)).valid!!
        assertEquals(CreateTrip("东京", 3, TravelMode.FLEXIBLE, date), result.command)
        assertEquals(date, result.startDate)
    }

    @Test fun datedDraftComputesInclusiveEndDateWithoutChangingCommand() {
        val state = CreateTripUiState(
            name = "东京",
            dayCount = "3",
            timeMode = CreateTimeMode.DATED,
            startDate = LocalDate.of(2026, 10, 1),
        )

        val command = validateCreateTrip(state).valid!!.command

        assertEquals(LocalDate.of(2026, 10, 3), state.endDate)
        assertEquals(LocalDate.of(2026, 10, 1), command.startDate)
        assertEquals(3, command.dayCount)
    }
}
