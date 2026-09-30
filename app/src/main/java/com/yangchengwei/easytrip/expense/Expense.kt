package com.yangchengwei.easytrip.expense

import java.math.BigDecimal

private val amountPattern = Regex("[0-9]+(?:\\.[0-9]{1,2})?")
fun parseExpense(text: String): Long? = text.trim().takeIf { amountPattern.matches(it) }?.let {
    runCatching { BigDecimal(it).movePointRight(2).longValueExact() }.getOrNull()
}
fun validExpense(text: String): Boolean = text.isBlank() || parseExpense(text) != null
fun expenseInput(cents: Long?): String = cents?.let { BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty()
fun formatExpense(cents: Long): String = "¥" + if (cents % 100L == 0L) (cents / 100L).toString() else BigDecimal.valueOf(cents, 2).toPlainString()
data class ExpenseSummary(val cents: Long = 0, val recorded: Int = 0, val missing: Int = 0) {
    fun label(prefix: String): String = "$prefix ${formatExpense(cents)}" + if (missing > 0) " · ${missing}项未填" else ""
}
fun expenseSummary(amounts: List<Long?>): ExpenseSummary = ExpenseSummary(
    cents = amounts.filterNotNull().fold(0L, Math::addExact),
    recorded = amounts.count { it != null },
    missing = amounts.count { it == null },
)

/** Called inside the write transaction before metadata changes: every trip total fits Long. */
fun requireSummableExpense(otherAmounts: List<Long>, amount: Long?) {
    require(amount == null || amount >= 0) { "花费不能为负数" }
    val total = otherAmounts.fold(java.math.BigInteger.ZERO) { sum, value -> sum + java.math.BigInteger.valueOf(value) } + java.math.BigInteger.valueOf(amount ?: 0)
    require(total <= java.math.BigInteger.valueOf(Long.MAX_VALUE)) { "全程花费超出支持范围，请减小金额" }
}
