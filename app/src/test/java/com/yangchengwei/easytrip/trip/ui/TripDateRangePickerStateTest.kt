package com.yangchengwei.easytrip.trip.ui

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDateRangePickerStateTest {
    @Test
    fun `first tap starts selection and same day completes one day range`() {
        val date = LocalDate.parse("2026-10-02")

        val started = reduceDateRangeSelection(DateRangeSelection(), date)
        val completed = reduceDateRangeSelection(started, date)

        assertEquals(DateRangeSelection(startDate = date), started)
        assertEquals(DateRangeSelection(startDate = date, endDate = date), completed)
        assertEquals(1, completed.dayCount)
        assertTrue(completed.isConfirmable)
        assertNull(completed.validationError)
    }

    @Test
    fun `earlier second tap starts a new range`() {
        val start = LocalDate.parse("2026-10-10")
        val earlier = LocalDate.parse("2026-10-04")

        val result = reduceDateRangeSelection(DateRangeSelection(start), earlier)

        assertEquals(DateRangeSelection(startDate = earlier), result)
    }

    @Test
    fun `tap after completed range starts a replacement selection`() {
        val result = reduceDateRangeSelection(
            DateRangeSelection(
                startDate = LocalDate.parse("2026-10-02"),
                endDate = LocalDate.parse("2026-10-05"),
            ),
            LocalDate.parse("2026-10-12"),
        )

        assertEquals(DateRangeSelection(startDate = LocalDate.parse("2026-10-12")), result)
    }

    @Test
    fun `calendar month at local date maximum stays in representable range`() {
        val calendar = calendarMonth(YearMonth.from(LocalDate.MAX))

        assertEquals(LocalDate.MAX, calendar.days.last())
        assertTrue(calendar.days.isNotEmpty())
        assertTrue(calendar.days.all { it in LocalDate.MIN..LocalDate.MAX })
    }

    @Test
    fun `calendar month at local date minimum stays in representable range`() {
        val calendar = calendarMonth(YearMonth.from(LocalDate.MIN))

        assertEquals(LocalDate.MIN, calendar.days.first())
        assertTrue(calendar.days.isNotEmpty())
        assertTrue(calendar.days.all { it in LocalDate.MIN..LocalDate.MAX })
    }

    @Test
    fun `calendar month is Monday first and includes complete leading and trailing weeks`() {
        val calendar = calendarMonth(YearMonth.of(2026, 2))

        assertEquals(LocalDate.parse("2026-01-26"), calendar.days.first())
        assertEquals(LocalDate.parse("2026-03-01"), calendar.days.last())
        assertTrue(calendar.days.size % DAYS_IN_WEEK == 0)
        assertTrue(calendar.days.all { it.dayOfWeek.value in 1..7 })
    }

    @Test
    fun `30 day selection is confirmable at maximum boundary`() {
        val selection = DateRangeSelection(
            startDate = LocalDate.parse("2026-10-01"),
            endDate = LocalDate.parse("2026-10-30"),
        )

        assertEquals(MAX_TRIP_DAYS, selection.dayCount)
        assertTrue(selection.isConfirmable)
        assertNull(selection.validationError)
    }

    @Test
    fun `very large date span is rejected without narrowing into a valid int`() {
        val selection = DateRangeSelection(
            startDate = LocalDate.MIN,
            endDate = LocalDate.MAX,
        )

        assertNull(selection.dayCount)
        assertFalse(selection.isConfirmable)
        assertEquals("旅行最多 $MAX_TRIP_DAYS 天", selection.validationError)
    }

    @Test
    fun `31 day selection exposes accessible maximum range error`() {
        val selection = DateRangeSelection(
            startDate = LocalDate.parse("2026-10-01"),
            endDate = LocalDate.parse("2026-10-31"),
        )

        assertFalse(selection.isConfirmable)
        assertEquals("旅行最多 $MAX_TRIP_DAYS 天", selection.validationError)
    }
}
