package com.yangchengwei.easytrip.expense

import java.time.LocalDate

enum class ExpenseCategory(val storageKey: String, val label: String) {
    LODGING("lodging", "住宿"),
    TRANSPORT("transport", "交通"),
    ATTRACTION("attraction", "景点"),
    FOOD("food", "餐饮"),
    SHOPPING("shopping", "购物"),
    OTHER("other", "其他"),
    ;

    companion object {
        /** Null means a migrated unclassified amount, never an unknown stored category. */
        fun fromStorageKey(key: String?): ExpenseCategory? =
            key?.let { value -> requireNotNull(entries.find { it.storageKey == value }) { "未知费用类别" } }
    }
}

enum class ExpenseSourceKind { PLACE, ROUTE }

data class ExpenseKey(val kind: ExpenseSourceKind, val id: String) {
    init {
        require(id.isNotBlank()) { "费用ID不能为空" }
    }
}

data class ExpenseRecord(
    val key: ExpenseKey,
    val tripId: String,
    val dayId: String,
    val sourceId: String,
    val sourceName: String,
    val date: LocalDate?,
    val cents: Long,
    val category: ExpenseCategory?,
    val note: String?,
    val tripName: String = "",
    val dayNumber: Int? = null,
) {
    init {
        require(tripId.isNotBlank() && dayId.isNotBlank() && sourceId.isNotBlank()) { "费用来源已失效" }
        require(sourceName.isNotBlank()) { "费用来源名称不能为空" }
        require(cents >= 0) { "花费不能为负数" }
        require(key.kind != ExpenseSourceKind.ROUTE || category == ExpenseCategory.TRANSPORT) {
            "路段费用只能归为交通"
        }
    }
}

/** Travel-day index is zero-based; recording time never determines the expense period. */
fun expenseDate(startDate: LocalDate?, dayIndex: Int): LocalDate? {
    require(dayIndex >= 0) { "旅行日序号不能为负数" }
    return startDate?.plusDays(dayIndex.toLong())
}
