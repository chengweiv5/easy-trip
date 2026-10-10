package com.yangchengwei.easytrip.expense

data class PlaceExpenseInput(
    val id: String?,
    val cents: Long,
    val category: ExpenseCategory?,
    val note: String?,
) {
    init {
        require(id == null || id.isNotBlank()) { "费用ID不能为空" }
        require(cents >= 0) { "花费不能为负数" }
    }
}

data class ExpenseDraftRow(
    val key: String,
    val savedId: String? = null,
    val amount: String = "",
    val category: ExpenseCategory? = null,
    val note: String = "",
    val categoryIsAutomatic: Boolean = false,
) {
    init {
        require(key.isNotBlank()) { "草稿标识不能为空" }
        require(savedId == null || savedId.isNotBlank()) { "费用ID不能为空" }
    }
}

sealed interface ExpenseDraftValidation {
    data class Valid(val expenses: List<PlaceExpenseInput>, val totalCents: Long) : ExpenseDraftValidation
    data class Invalid(val rowKey: String, val field: ExpenseDraftField, val message: String) : ExpenseDraftValidation
}

enum class ExpenseDraftField { AMOUNT, CATEGORY, IDENTITY }

fun validateExpenseDraft(
    rows: List<ExpenseDraftRow>,
    originalExpenses: List<PlaceExpenseInput> = emptyList(),
): ExpenseDraftValidation {
    require(originalExpenses.all { it.id != null } &&
        originalExpenses.map { it.id }.distinct().size == originalExpenses.size) {
        "原始费用快照无效"
    }
    val originals = originalExpenses.associateBy { it.id }
    val keys = mutableSetOf<String>()
    val savedIds = mutableSetOf<String>()
    val expenses = mutableListOf<PlaceExpenseInput>()
    var total = 0L
    for (row in rows) {
        if (!keys.add(row.key) || row.savedId?.let { !savedIds.add(it) || it !in originals } == true) {
            return ExpenseDraftValidation.Invalid(row.key, ExpenseDraftField.IDENTITY, "费用记录已变更，请重新读取")
        }
        if (row.isEmptyNewExpense) continue
        val amount = parseExpense(row.amount)
            ?: return ExpenseDraftValidation.Invalid(row.key, ExpenseDraftField.AMOUNT, "请输入有效金额，最多两位小数")
        val note = row.note.trim().ifEmpty { null }
        val original = row.savedId?.let(originals::get)
        val unchangedLegacy = original != null && original.category == null &&
            amount == original.cents && note == original.note?.trim()?.ifEmpty { null }
        if (row.category == null && !unchangedLegacy) {
            return ExpenseDraftValidation.Invalid(row.key, ExpenseDraftField.CATEGORY, "请选择费用类别")
        }
        if (amount > Long.MAX_VALUE - total) {
            return ExpenseDraftValidation.Invalid(row.key, ExpenseDraftField.AMOUNT, "费用合计超出支持范围")
        }
        total += amount
        expenses += PlaceExpenseInput(row.savedId, amount, row.category, note)
    }
    return ExpenseDraftValidation.Valid(expenses, total)
}
