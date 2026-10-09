package com.yangchengwei.easytrip.expense

import org.junit.Assert.*
import org.junit.Test

class ExpenseDraftTest {
    @Test fun blankRowDoesNotCreateAZeroAmountRecordButExplicitZeroDoes() {
        assertEquals(
            ExpenseDraftValidation.Valid(emptyList(), 0),
            validateExpenseDraft(listOf(ExpenseDraftRow("empty", amount = " ", note = " "))),
        )
        assertEquals(
            ExpenseDraftValidation.Valid(listOf(PlaceExpenseInput(null, 0, ExpenseCategory.FOOD, null)), 0),
            validateExpenseDraft(listOf(ExpenseDraftRow("free", amount = "0", category = ExpenseCategory.FOOD))),
        )
    }

    @Test fun partiallyEnteredRowsNeedAnAmountAndCategoryWithoutRounding() {
        fun invalid(row: ExpenseDraftRow, field: ExpenseDraftField) {
            val result = validateExpenseDraft(listOf(row))
            assertTrue(result is ExpenseDraftValidation.Invalid)
            result as ExpenseDraftValidation.Invalid
            assertEquals(row.key, result.rowKey)
            assertEquals(field, result.field)
            assertTrue(result.message.isNotBlank())
        }
        invalid(ExpenseDraftRow("note-only", note = "晚餐"), ExpenseDraftField.AMOUNT)
        invalid(ExpenseDraftRow("class-only", category = ExpenseCategory.FOOD), ExpenseDraftField.AMOUNT)
        invalid(ExpenseDraftRow("amount-only", amount = "12"), ExpenseDraftField.CATEGORY)
        listOf("-1", "1.001", "NaN", "1e3", "999999999999999999999").forEach {
            invalid(ExpenseDraftRow("bad", amount = it, category = ExpenseCategory.FOOD), ExpenseDraftField.AMOUNT)
        }
        assertEquals(
            ExpenseDraftValidation.Valid(
                listOf(PlaceExpenseInput(null, 1234, ExpenseCategory.FOOD, "早餐")), 1234,
            ),
            validateExpenseDraft(listOf(ExpenseDraftRow("breakfast", amount = " 12.34 ", category = ExpenseCategory.FOOD, note = " 早餐 "))),
        )
    }

    @Test fun unchangedLegacyExpenseCanBeKeptButEditingItRequiresARealCategory() {
        val original = PlaceExpenseInput("legacy:hotel", 42800, null, "房费")
        val row = ExpenseDraftRow("row", original.id, "428.00", note = "房费")
        assertEquals(
            ExpenseDraftValidation.Valid(listOf(original), 42800),
            validateExpenseDraft(listOf(row), listOf(original)),
        )
        listOf(row.copy(amount = "400"), row.copy(note = "新备注")).forEach { changed ->
            val result = validateExpenseDraft(listOf(changed), listOf(original)) as ExpenseDraftValidation.Invalid
            assertEquals(ExpenseDraftField.CATEGORY, result.field)
        }
        assertEquals(
            ExpenseDraftValidation.Valid(listOf(original.copy(category = ExpenseCategory.LODGING)), 42800),
            validateExpenseDraft(listOf(row.copy(category = ExpenseCategory.LODGING)), listOf(original)),
        )
        val blankExisting = validateExpenseDraft(
            listOf(row.copy(amount = "", note = "")), listOf(original),
        ) as ExpenseDraftValidation.Invalid
        assertEquals(ExpenseDraftField.AMOUNT, blankExisting.field)
        // Removal is explicit: omit the row rather than silently ignoring cleared fields.
        assertEquals(ExpenseDraftValidation.Valid(emptyList(), 0), validateExpenseDraft(emptyList(), listOf(original)))
        val categorized = original.copy(category = ExpenseCategory.LODGING)
        assertTrue(validateExpenseDraft(listOf(row), listOf(categorized)) is ExpenseDraftValidation.Invalid)
        val spaced = original.copy(note = "  房费  ")
        assertEquals(
            ExpenseDraftValidation.Valid(listOf(original), 42800),
            validateExpenseDraft(listOf(row), listOf(spaced)),
        )
    }

    @Test fun draftRejectsStaleIdentitiesAndDuplicateKeysBeforeSubmittingAnything() {
        val original = PlaceExpenseInput("saved", 100, ExpenseCategory.FOOD, null)
        val row = ExpenseDraftRow("row", "saved", "1", ExpenseCategory.FOOD)
        fun identityError(rows: List<ExpenseDraftRow>, originals: List<PlaceExpenseInput>) {
            val result = validateExpenseDraft(rows, originals) as ExpenseDraftValidation.Invalid
            assertEquals(ExpenseDraftField.IDENTITY, result.field)
        }
        identityError(listOf(row), emptyList())
        identityError(listOf(row, row.copy(key = "another-row")), listOf(original))
        identityError(listOf(row, row.copy(savedId = null)), listOf(original))
        assertThrows(IllegalArgumentException::class.java) { ExpenseDraftRow("") }
        assertThrows(IllegalArgumentException::class.java) { row.copy(savedId = "") }
        assertThrows(IllegalArgumentException::class.java) { original.copy(cents = -1) }
        assertThrows(IllegalArgumentException::class.java) { original.copy(id = "") }
        assertThrows(IllegalArgumentException::class.java) {
            validateExpenseDraft(listOf(row), listOf(original, original.copy(cents = 200)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateExpenseDraft(emptyList(), listOf(original.copy(id = null)))
        }
    }

    @Test fun draftTotalOverflowPointsAtTheRowThatWouldOverflow() {
        val maximum = ExpenseDraftRow("maximum", amount = "92233720368547758.07", category = ExpenseCategory.LODGING)
        assertEquals(Long.MAX_VALUE, (validateExpenseDraft(listOf(maximum)) as ExpenseDraftValidation.Valid).totalCents)
        val result = validateExpenseDraft(listOf(
            maximum, ExpenseDraftRow("extra", amount = "0.01", category = ExpenseCategory.FOOD),
        )) as ExpenseDraftValidation.Invalid
        assertEquals("extra", result.rowKey)
        assertEquals(ExpenseDraftField.AMOUNT, result.field)
    }

    @Test fun removingAndUndoingASavedRowOnlyChangesTheDraftAndPreservesItsIdentity() {
        val originals = listOf(
            PlaceExpenseInput("stay", 60000, ExpenseCategory.LODGING, null),
            PlaceExpenseInput("dinner", 12000, ExpenseCategory.FOOD, "晚餐"),
            PlaceExpenseInput("breakfast", 4000, ExpenseCategory.FOOD, "早餐"),
        )
        val rows = originals.map { ExpenseDraftRow("draft:${it.id}", it.id, expenseInput(it.cents), it.category, it.note.orEmpty()) }
        val removal = removeExpenseDraftRow(rows, "draft:dinner")
        assertEquals(listOf("stay", "breakfast"), removal.remainingRows.map { it.savedId })
        assertEquals(64000, (validateExpenseDraft(removal.remainingRows, originals) as ExpenseDraftValidation.Valid).totalCents)
        val restored = restoreExpenseDraftRow(removal.remainingRows, removal)
        assertEquals(rows, restored)
        assertEquals(76000, (validateExpenseDraft(restored, originals) as ExpenseDraftValidation.Valid).totalCents)
        assertEquals(3, rows.size)
        assertEquals(3, originals.size)
        assertThrows(IllegalArgumentException::class.java) { removeExpenseDraftRow(rows, "missing") }
        assertThrows(IllegalArgumentException::class.java) { restoreExpenseDraftRow(restored, removal) }
        assertThrows(IllegalArgumentException::class.java) {
            restoreExpenseDraftRow(removal.remainingRows + removal.removed.copy(key = "different-key"), removal)
        }
        val unsaved = rows + ExpenseDraftRow("new", amount = "12", category = ExpenseCategory.FOOD)
        assertEquals(rows, removeExpenseDraftRow(unsaved, "new").remainingRows)
    }
}
