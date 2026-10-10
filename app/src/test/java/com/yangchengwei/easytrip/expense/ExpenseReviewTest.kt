package com.yangchengwei.easytrip.expense

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class ExpenseReviewTest {
    @Test fun expenseBelongsToItsTravelDayIncludingAcrossYears() {
        assertEquals(LocalDate.of(2026, 1, 1), expenseDate(LocalDate.of(2025, 12, 31), 1))
        assertEquals(LocalDate.of(2024, 2, 29), expenseDate(LocalDate.of(2024, 2, 28), 1))
        assertNull(expenseDate(null, 0))
        assertThrows(IllegalArgumentException::class.java) { expenseDate(null, -1) }
    }

    @Test fun samePlaceAndSameCategoryStillCountAsSeparateRecords() {
        val records = listOf(
            record("stay", 60000, ExpenseCategory.LODGING),
            record("dinner", 12000, ExpenseCategory.FOOD),
            record("breakfast", 4000, ExpenseCategory.FOOD),
        )
        assertEquals(ExpenseTotal(76000, 3, 1), reviewExpenses(records, ExpensePeriod.All))
        assertEquals(ExpenseTotal(0, 0, 0), reviewExpenses(emptyList(), ExpensePeriod.All))
    }

    @Test fun undatedAndCrossYearRecordsStayInTheirOwnPeriodsIncludingZero() {
        val records = listOf(
            record("dec", 30000, date = LocalDate.of(2025, 12, 31), trip = "xiamen"),
            record("jan", 70000, date = LocalDate.of(2026, 1, 1), trip = "xiamen"),
            record("zero", 0, date = LocalDate.of(2026, 2, 1), trip = "free-trip"),
            record("undated", 48650, category = null, date = null, trip = "undated-trip"),
        )
        assertEquals(ExpenseTotal(148650, 4, 3), reviewExpenses(records, ExpensePeriod.All))
        assertEquals(ExpenseTotal(48650, 1, 1), reviewExpenses(records, ExpensePeriod.Undated))
        assertEquals(ExpenseTotal(30000, 1, 1), reviewExpenses(records, ExpensePeriod.Year(2025)))
        assertEquals(ExpenseTotal(70000, 2, 2), reviewExpenses(records, ExpensePeriod.Year(2026)))
        assertEquals(ExpenseTotal(70000, 1, 1), reviewExpenses(records, ExpensePeriod.Month(YearMonth.of(2026, 1))))
        assertEquals(ExpenseTotal(0, 1, 1), reviewExpenses(records, ExpensePeriod.Month(YearMonth.of(2026, 2))))
        assertEquals(ExpenseTotal(0, 0, 0), reviewExpenses(records, ExpensePeriod.Month(YearMonth.of(2026, 3))))
    }

    @Test fun duplicateRecordsDoNotDoubleCountButSourceKindsKeepSeparateIdentities() {
        val place = record("same-id", 60000, ExpenseCategory.LODGING)
        val route = record("same-id", 5850, ExpenseCategory.TRANSPORT, kind = ExpenseSourceKind.ROUTE)
        assertEquals(ExpenseTotal(65850, 2, 1), reviewExpenses(listOf(place, place.copy(), route), ExpensePeriod.All))
        assertThrows(IllegalArgumentException::class.java) {
            reviewExpenses(listOf(place, place.copy(cents = 1)), ExpensePeriod.All)
        }
        // A conflicting duplicate cannot be hidden by choosing a narrower period.
        assertThrows(IllegalArgumentException::class.java) {
            reviewExpenses(listOf(place, place.copy(date = LocalDate.of(2026, 1, 1))), ExpensePeriod.Year(2025))
        }
    }

    @Test fun totalsNeverWrapAndInvalidRecordsFailInsteadOfBecomingRealExpenses() {
        assertEquals(ExpenseTotal(Long.MAX_VALUE, 2, 1), reviewExpenses(
            listOf(record("a", Long.MAX_VALUE - 1), record("b", 1)), ExpensePeriod.All,
        ))
        assertThrows(ArithmeticException::class.java) {
            reviewExpenses(listOf(record("a", Long.MAX_VALUE), record("b", 1)), ExpensePeriod.All)
        }
        assertThrows(IllegalArgumentException::class.java) { record("negative", -1) }
        assertThrows(IllegalArgumentException::class.java) { record("", 1) }
        assertThrows(IllegalArgumentException::class.java) { record("a", 1, trip = "") }
        assertThrows(IllegalArgumentException::class.java) { record("a", 1).copy(dayId = "") }
        assertThrows(IllegalArgumentException::class.java) { record("a", 1).copy(sourceId = "") }
        assertThrows(IllegalArgumentException::class.java) { record("a", 1).copy(sourceName = " ") }
        assertThrows(IllegalArgumentException::class.java) {
            record("route", 1, ExpenseCategory.FOOD, kind = ExpenseSourceKind.ROUTE)
        }
    }

    @Test fun sixCategoriesAndLegacyUnclassifiedAlwaysReconcileToTheSameScope() {
        assertEquals(
            listOf("lodging", "transport", "attraction", "food", "shopping", "other"),
            ExpenseCategory.entries.map { it.storageKey },
        )
        assertEquals(listOf("住宿", "交通", "景点", "餐饮", "购物", "其他"), ExpenseCategory.entries.map { it.label })
        ExpenseCategory.entries.forEach { assertEquals(it, ExpenseCategory.fromStorageKey(it.storageKey)) }
        assertNull(ExpenseCategory.fromStorageKey(null))
        assertThrows(IllegalArgumentException::class.java) { ExpenseCategory.fromStorageKey("unknown") }
        val rows = listOf(
            record("stay", 60000, ExpenseCategory.LODGING),
            record("dinner", 12000, ExpenseCategory.FOOD),
            record("breakfast", 4000, ExpenseCategory.FOOD),
            record("legacy", 42800, null),
            record("next-year", 70000, date = LocalDate.of(2026, 1, 1)),
        )
        val result = expenseBreakdown(rows, ExpensePeriod.Year(2025))
        assertEquals(ExpenseTotal(118800, 4, 1), result.total)
        assertEquals(ExpenseTotal(60000, 1, 1), result.byCategory[ExpenseCategory.LODGING])
        assertEquals(ExpenseTotal(16000, 2, 1), result.byCategory[ExpenseCategory.FOOD])
        assertEquals(ExpenseTotal(0, 0, 0), result.byCategory[ExpenseCategory.SHOPPING])
        assertEquals(ExpenseTotal(42800, 1, 1), result.unclassified)
        assertEquals(6, result.byCategory.size)
    }

    @Test fun approvedDesignFixtureReconcilesYearsMonthsCategoriesAndDateChanges() {
        val records = requireNotNull(javaClass.getResourceAsStream("/expense/approved-v2-review.tsv"))
            .bufferedReader().useLines { lines ->
                lines.filterNot { it.startsWith("#") || it.isBlank() }.map { line ->
                    val columns = line.split('\t')
                    record(
                        id = columns[0], trip = columns[1],
                        date = columns[2].takeUnless { it == "-" }?.let(LocalDate::parse),
                        category = ExpenseCategory.fromStorageKey(columns[3].takeUnless { it == "-" }),
                        cents = columns[4].toLong(),
                    )
                }.toList()
            }
        assertEquals(ExpenseTotal(2323300, 65, 7), reviewExpenses(records, ExpensePeriod.All))
        assertEquals(ExpenseTotal(1224650, 35, 4), reviewExpenses(records, ExpensePeriod.Year(2025)))
        assertEquals(ExpenseTotal(980000, 20, 2), reviewExpenses(records, ExpensePeriod.Year(2024)))
        assertEquals(ExpenseTotal(70000, 2, 1), reviewExpenses(records, ExpensePeriod.Year(2026)))
        assertEquals(ExpenseTotal(48650, 8, 1), reviewExpenses(records, ExpensePeriod.Undated))
        val annual = expenseBreakdown(records, ExpensePeriod.Year(2025))
        assertEquals(listOf(600000L, 225850L, 109000L, 213800L, 52000L, 24000L),
            ExpenseCategory.entries.map { annual.byCategory.getValue(it).cents })
        assertEquals(listOf(4, 7, 5, 13, 3, 3),
            ExpenseCategory.entries.map { annual.byCategory.getValue(it).records })
        assertEquals(124650L, reviewExpenses(records, ExpensePeriod.Month(YearMonth.of(2025, 4))).cents)
        assertEquals(650000L, reviewExpenses(records, ExpensePeriod.Month(YearMonth.of(2025, 7))).cents)
        assertEquals(420000L, reviewExpenses(records, ExpensePeriod.Month(YearMonth.of(2025, 10))).cents)
        assertEquals(30000L, reviewExpenses(records, ExpensePeriod.Month(YearMonth.of(2025, 12))).cents)
        val changed = records.map { if (it.tripId == "xiamen") it.copy(date = it.date?.plusDays(1)) else it }
        assertEquals(1194650L, reviewExpenses(changed, ExpensePeriod.Year(2025)).cents)
        assertEquals(100000L, reviewExpenses(changed, ExpensePeriod.Year(2026)).cents)
        assertEquals(ExpenseTotal(2323300, 65, 7), reviewExpenses(changed, ExpensePeriod.All))
        assertEquals(records.map { it.key }, changed.map { it.key })
    }

    private fun record(
        id: String,
        cents: Long,
        category: ExpenseCategory? = ExpenseCategory.FOOD,
        trip: String = "hangzhou",
        date: LocalDate? = LocalDate.of(2025, 4, 12),
        kind: ExpenseSourceKind = ExpenseSourceKind.PLACE,
    ) = ExpenseRecord(
        key = ExpenseKey(kind, id),
        tripId = trip,
        dayId = "$trip-day",
        sourceId = "$trip-hotel",
        sourceName = "湖畔酒店",
        date = date,
        cents = cents,
        category = category,
        note = null,
    )
}
