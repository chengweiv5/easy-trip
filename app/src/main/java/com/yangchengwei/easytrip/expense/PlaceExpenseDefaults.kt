package com.yangchengwei.easytrip.expense

import com.yangchengwei.easytrip.place.domain.PlaceCategory

fun defaultExpenseCategory(category: PlaceCategory): ExpenseCategory = when (category) {
    PlaceCategory.ATTRACTION -> ExpenseCategory.ATTRACTION
    PlaceCategory.LODGING -> ExpenseCategory.LODGING
    PlaceCategory.FOOD -> ExpenseCategory.FOOD
    PlaceCategory.TRANSPORT -> ExpenseCategory.TRANSPORT
    PlaceCategory.OTHER -> ExpenseCategory.OTHER
}

fun newPlaceExpenseDraft(key: String, category: PlaceCategory): ExpenseDraftRow =
    ExpenseDraftRow(key = key, category = defaultExpenseCategory(category), categoryIsAutomatic = true)

fun ExpenseDraftRow.selectCategory(category: ExpenseCategory): ExpenseDraftRow =
    copy(category = category, categoryIsAutomatic = false)

val ExpenseDraftRow.isEmptyNewExpense: Boolean
    get() = savedId == null && amount.isBlank() && note.isBlank() &&
        (category == null || categoryIsAutomatic)
