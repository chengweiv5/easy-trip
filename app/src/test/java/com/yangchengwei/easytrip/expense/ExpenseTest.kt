package com.yangchengwei.easytrip.expense

import org.junit.Assert.*
import org.junit.Test

class ExpenseTest {
    @Test fun displayGroupsThousandsWithoutChangingExactAmountOrEditableInput() {
        assertEquals("¥1,234", formatExpense(123400))
        assertEquals("¥12,345.60", formatExpense(1234560))
        assertEquals("¥92,233,720,368,547,758.07", formatExpense(Long.MAX_VALUE))
        assertEquals("12345.6", expenseInput(1234560))
        assertEquals(1234560L, parseExpense(expenseInput(1234560)))
    }
    @Test fun aggregateValidationRejectsOverflowWithoutRounding() {
        requireSummableExpense(listOf(Long.MAX_VALUE - 1), 1)
        requireSummableExpense(listOf(1), null)
        assertThrows(IllegalArgumentException::class.java) { requireSummableExpense(listOf(Long.MAX_VALUE), 1) }
        assertThrows(IllegalArgumentException::class.java) { requireSummableExpense(emptyList(), -1) }
    }
    @Test fun amountsAreOptionalExactAndNeverRounded() {
        assertNull(parseExpense(""))
        assertEquals(0L, parseExpense("0"))
        assertEquals(3650L, parseExpense("36.50"))
        assertEquals(1L, parseExpense("0.01"))
        listOf("-1", "1.001", "NaN", "1e3", "999999999999999999999").forEach { assertFalse(validExpense(it)) }
        assertEquals("¥36.50", formatExpense(3650))
        assertEquals("¥80", formatExpense(8000))
        val summary = expenseSummary(listOf(8000, null, 3650, 0))
        assertEquals(11650L, summary.cents)
        assertEquals(3, summary.recorded)
        assertEquals(1, summary.missing)
    }
}
