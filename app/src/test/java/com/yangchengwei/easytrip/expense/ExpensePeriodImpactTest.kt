package com.yangchengwei.easytrip.expense

import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.*

class ExpensePeriodImpactTest {
    private val record = ExpenseRecord(ExpenseKey(ExpenseSourceKind.PLACE,"dinner"),"xiamen","day1","item","厦门晚餐",LocalDate.of(2025,12,31),30000,ExpenseCategory.FOOD,null)
    @Test fun `zero and unclassified records still require period confirmation`() {
        val old = record.copy(cents = 0, category = null, date = null)
        val impact = expensePeriodImpact(listOf(old), listOf(old.copy(date = LocalDate.of(2026,1,1))))
        assertEquals(1, impact.size)
        assertEquals(0L, impact.single().cents)
        assertNull(impact.single().beforeDate)
    }
    @Test fun `cross year requires exact old and new dates while same month does not`() {
        val changes = expensePeriodImpact(listOf(record), listOf(record.copy(date = LocalDate.of(2026,1,1))))
        assertEquals(1, changes.size)
        assertEquals(30000L, changes.single().cents)
        assertEquals(LocalDate.of(2025,12,31), changes.single().beforeDate)
        assertEquals(LocalDate.of(2026,1,1), changes.single().afterDate)
        assertTrue(expensePeriodImpact(listOf(record), listOf(record.copy(date = LocalDate.of(2025,12,30)))).isEmpty())
        assertTrue(expensePeriodImpact(listOf(record), emptyList()).isEmpty())
    }
}
