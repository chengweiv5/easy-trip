package com.yangchengwei.easytrip.expense

import java.time.YearMonth

sealed interface ExpensePeriod {
    data object All : ExpensePeriod
    data object Undated : ExpensePeriod
    data class Year(val value: Int) : ExpensePeriod
    data class Month(val value: YearMonth) : ExpensePeriod
}

data class ExpenseTotal(val cents: Long, val records: Int, val trips: Int)

data class ExpenseBreakdown(
    val total: ExpenseTotal,
    val byCategory: Map<ExpenseCategory, ExpenseTotal>,
    val unclassified: ExpenseTotal,
)

fun reviewExpenses(records: List<ExpenseRecord>, period: ExpensePeriod): ExpenseTotal =
    totalExpenses(selectExpenses(records, period))

fun expenseBreakdown(records: List<ExpenseRecord>, period: ExpensePeriod): ExpenseBreakdown {
    val selected = selectExpenses(records, period)
    return ExpenseBreakdown(
        total = totalExpenses(selected),
        byCategory = ExpenseCategory.entries.associateWith { category ->
            totalExpenses(selected.filter { it.category == category })
        },
        unclassified = totalExpenses(selected.filter { it.category == null }),
    )
}

private fun selectExpenses(records: List<ExpenseRecord>, period: ExpensePeriod): List<ExpenseRecord> {
    val unique = linkedMapOf<ExpenseKey, ExpenseRecord>()
    records.forEach { record ->
        val previous = unique.putIfAbsent(record.key, record)
        require(previous == null || previous == record) { "费用记录不一致，请重新读取" }
    }
    return unique.values.filter { record ->
        when (period) {
            ExpensePeriod.All -> true
            ExpensePeriod.Undated -> record.date == null
            is ExpensePeriod.Year -> record.date?.year == period.value
            is ExpensePeriod.Month -> record.date?.let(YearMonth::from) == period.value
        }
    }
}

private fun totalExpenses(records: List<ExpenseRecord>): ExpenseTotal = ExpenseTotal(
    cents = records.fold(0L) { total, record -> Math.addExact(total, record.cents) },
    records = records.size,
    trips = records.map { it.tripId }.distinct().size,
)
