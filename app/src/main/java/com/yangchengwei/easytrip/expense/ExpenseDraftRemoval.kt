package com.yangchengwei.easytrip.expense

/** Local draft removal only. The persistence boundary is the whole-place save operation. */
data class ExpenseDraftRemoval(
    val remainingRows: List<ExpenseDraftRow>,
    val removed: ExpenseDraftRow,
    val originalIndex: Int,
)

fun removeExpenseDraftRow(rows: List<ExpenseDraftRow>, key: String): ExpenseDraftRemoval {
    require(rows.count { it.key == key } == 1) { "费用草稿已变更" }
    val index = rows.indexOfFirst { it.key == key }
    return ExpenseDraftRemoval(rows.filterIndexed { i, _ -> i != index }, rows[index], index)
}

fun restoreExpenseDraftRow(rows: List<ExpenseDraftRow>, removal: ExpenseDraftRemoval): List<ExpenseDraftRow> {
    require(removal.originalIndex >= 0) { "费用草稿位置无效" }
    require(rows.none { it.key == removal.removed.key ||
        removal.removed.savedId != null && it.savedId == removal.removed.savedId }) { "费用已存在，不能重复恢复" }
    return rows.toMutableList().apply {
        add(removal.originalIndex.coerceAtMost(size), removal.removed)
    }
}
