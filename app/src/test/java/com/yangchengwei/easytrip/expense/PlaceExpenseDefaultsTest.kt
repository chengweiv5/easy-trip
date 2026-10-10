package com.yangchengwei.easytrip.expense

import com.yangchengwei.easytrip.place.domain.PlaceCategory
import org.junit.Assert.*
import org.junit.Test

class PlaceExpenseDefaultsTest {
    @Test fun fivePlaceCategoriesUseSameNamedExpenseCategoryWithStableKeys() {
        assertEquals(listOf(ExpenseCategory.ATTRACTION, ExpenseCategory.LODGING, ExpenseCategory.FOOD,
            ExpenseCategory.TRANSPORT, ExpenseCategory.OTHER), PlaceCategory.entries.map(::defaultExpenseCategory))
        assertEquals("餐饮", ExpenseCategory.FOOD.label)
        assertEquals("其他", ExpenseCategory.OTHER.label)
        assertEquals("food", ExpenseCategory.FOOD.storageKey)
        assertEquals("other", ExpenseCategory.OTHER.storageKey)
        assertEquals("购物", ExpenseCategory.SHOPPING.label)
    }
    @Test fun automaticCategoryAloneIsEmptyButZeroAndManualSameCategoryAreNot() {
        val row = newPlaceExpenseDraft("first", PlaceCategory.LODGING)
        assertEquals(ExpenseCategory.LODGING, row.category)
        assertEquals(ExpenseDraftValidation.Valid(emptyList(), 0L), validateExpenseDraft(listOf(row)))
        assertEquals(ExpenseDraftValidation.Valid(
            listOf(PlaceExpenseInput(null, 0, ExpenseCategory.LODGING, null)), 0L),
            validateExpenseDraft(listOf(row.copy(amount = "0"))))
        val manual = row.selectCategory(ExpenseCategory.LODGING)
        assertFalse(manual.categoryIsAutomatic)
        assertTrue(validateExpenseDraft(listOf(manual)) is ExpenseDraftValidation.Invalid)
        assertTrue(validateExpenseDraft(listOf(row.copy(note = "房费"))) is ExpenseDraftValidation.Invalid)
    }
    @Test fun clearingInputAndUndoKeepAutomaticOriginWhileLegacyNullIsUntouched() {
        val row = newPlaceExpenseDraft("first", PlaceCategory.OTHER).copy(amount = "1").copy(amount = "")
        assertTrue(row.isEmptyNewExpense)
        val removal = removeExpenseDraftRow(listOf(row), row.key)
        assertEquals(row, restoreExpenseDraftRow(removal.remainingRows, removal).single())
        val original = PlaceExpenseInput("legacy", 500, null, "原费用")
        val legacy = ExpenseDraftRow("legacy", "legacy", "5", null, "原费用")
        assertEquals(ExpenseDraftValidation.Valid(listOf(original), 500),
            validateExpenseDraft(listOf(legacy), listOf(original)))
    }
}
