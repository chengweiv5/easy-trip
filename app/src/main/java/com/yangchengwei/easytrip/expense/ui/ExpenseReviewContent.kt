package com.yangchengwei.easytrip.expense.ui

import com.yangchengwei.easytrip.core.ui.formatCount
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.expense.*
import java.time.LocalDate

@Composable
internal fun ExpenseReviewContent(
    scope: ExpenseScope, all: List<ExpenseRecord>, model: ExpenseReviewViewModel,
    onOpenSource: (ExpenseRecord) -> Unit, onOpenTrip: (String) -> Unit, onTripSettings: (String) -> Unit,
) {
    val records = scope.select(all)
    val total = reviewExpenses(records, ExpensePeriod.All)
    val overview = scope.category == null && !scope.unclassified
    if (scope.tripId != null) ExpenseSection("当前旅行", ExpenseGlyph.TRIP, "expense-section-trip-context") {
        val tripRecords = all.filter { it.tripId == scope.tripId }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tripRecords.firstOrNull()?.tripName?.ifBlank { "旅行" } ?: "旅行", fontWeight = FontWeight.SemiBold)
            if (scope.dayId != null) Text("第${records.firstOrNull()?.dayNumber ?: "—"}天", style = MaterialTheme.typography.bodySmall)
            if (scope.period != ExpensePeriod.All || scope.dayId != null) TextButton(model::showWholeTrip) {
                Text("查看全程 · ${expenseMoney(reviewExpenses(tripRecords, ExpensePeriod.All).cents)}")
            }
            TextButton({ onOpenTrip(scope.tripId) }) { Text("进入原旅行") }
            if (tripRecords.any { it.date == null }) TextButton({ onTripSettings(scope.tripId) }) { Text("设置旅行日期") }
        }
    }
    ExpenseSummary(total, scope.period)
    if (scope.period == ExpensePeriod.All && overview && scope.dayId == null) {
        ExpenseSection("按年回顾", ExpenseGlyph.HISTORY, "expense-section-years") {
            val years = records.mapNotNull { it.date?.year }.distinct().sortedDescending()
            if (years.isEmpty()) EmptySectionText("暂无已确定年份的花费")
            years.forEachIndexed { index, year ->
                if (index > 0) HorizontalDivider(color = expenseBorderColor)
                ReviewRow("${year}年", reviewExpenses(scope.copy(period = ExpensePeriod.Year(year)).select(all), ExpensePeriod.All),
                    "review-year-$year") { model.selectPeriod(ExpensePeriod.Year(year)) }
            }
        }
    }
    if (scope.period is ExpensePeriod.Year && overview) ExpenseSection("每个月的花费", ExpenseGlyph.CALENDAR, "expense-section-months") {
        ExpenseMonthChart(scope, all, scope.period.value) { model.selectPeriod(ExpensePeriod.Month(it)) }
    }
    if (overview && (scope.period != ExpensePeriod.All || scope.tripId != null)) {
        ExpenseSection("花在哪里", ExpenseGlyph.CATEGORIES, "expense-section-categories") {
            ExpenseCategory.entries.forEachIndexed { index, category ->
                if (index > 0) HorizontalDivider(color = expenseBorderColor)
                val count = reviewExpenses(records.filter { it.category == category }, ExpensePeriod.All)
                ReviewRow(category.label, count, "review-category-${category.storageKey}",
                    fraction = if (total.cents > 0 && count.records > 0) (count.cents.toDouble() / total.cents).toFloat() else null) {
                    model.selectCategory(category)
                }
            }
            val unclassified = reviewExpenses(records.filter { it.category == null }, ExpensePeriod.All)
            if (unclassified.records > 0) {
                HorizontalDivider(color = expenseBorderColor)
                ReviewRow("待分类 · 旧费用", unclassified, "review-unclassified") { model.selectCategory(null) }
            }
        }
    }
    if (scope.tripId == null && overview && scope.period != ExpensePeriod.All) {
        ExpenseSection("哪些旅行贡献了花费", ExpenseGlyph.TRIP, "expense-section-trips") {
            if (records.isEmpty()) EmptySectionText("当前范围尚无旅行花费")
            records.groupBy { it.tripId }.entries.forEachIndexed { index, (id, group) ->
                if (index > 0) HorizontalDivider(color = expenseBorderColor)
                val whole = reviewExpenses(all.filter { it.tripId == id }, ExpensePeriod.All)
                val part = reviewExpenses(group, ExpensePeriod.All)
                ReviewRow(group.first().tripName.ifBlank { "旅行" }, part, "review-trip-$id",
                    detail = "${when (scope.period) { is ExpensePeriod.Year -> "本年部分"; is ExpensePeriod.Month -> "本月部分"; else -> "当前范围" }} ${expenseMoney(part.cents)} · 全程 ${expenseMoney(whole.cents)} · ${formatCount(whole.records)}笔") { model.selectTrip(id) }
            }
        }
    }
    if (scope.tripId != null || !overview || scope.period == ExpensePeriod.Undated) {
        ExpenseSection("费用明细", ExpenseGlyph.RECORDS, "expense-section-records") {
            if (records.isEmpty()) EmptySectionText("当前范围尚无费用明细")
            records.groupBy { it.tripId }.entries.sortedByDescending { (_, group) -> group.maxOfOrNull { it.date ?: LocalDate.MIN } }
                .forEach { (_, group) ->
                    Text("${group.first().tripName.ifBlank { "旅行" }} · ${expenseMoney(reviewExpenses(group, ExpensePeriod.All).cents)} · ${formatCount(group.size)}笔",
                        Modifier.padding(top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall)
                    group.sortedWith(compareByDescending<ExpenseRecord> { it.date }.thenBy { it.key.id }).forEach { record ->
                        HorizontalDivider(color = expenseBorderColor)
                        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(record.sourceName, fontWeight = FontWeight.SemiBold)
                            Text(expenseMoney(record.cents), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text("${record.category?.label ?: "待分类"}${record.note?.let { " · $it" }.orEmpty()}")
                            Text("${record.tripName} · 第${record.dayNumber ?: "—"}天 · ${record.date ?: "未确定日期"}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton({ if (record.key.kind == ExpenseSourceKind.PLACE) model.editRecord(record) else onOpenSource(record) },
                                    Modifier.testTag("expense-record-edit")) { Text(if (record.key.kind == ExpenseSourceKind.ROUTE) "编辑路段" else "编辑本笔") }
                                TextButton({ onOpenTrip(record.tripId) }) { Text("原旅行") }
                                if (record.date == null) TextButton({ onTripSettings(record.tripId) }) { Text("设置旅行日期") }
                            }
                        }
                    }
                }
        }
    }
    if (scope.period != ExpensePeriod.Undated && overview) {
        val undated = reviewExpenses(scope.copy(period = ExpensePeriod.Undated).select(all), ExpensePeriod.All)
        if (undated.records > 0) ExpenseUndatedNotice(undated, scope.period == ExpensePeriod.All) { model.selectPeriod(ExpensePeriod.Undated) }
    }
    ExpenseSection("统计口径", ExpenseGlyph.INFO, "expense-section-accounting") {
        Text("金额按旅行日归属年月。仅统计已记录花费；没有记录不代表没有消费。未确定日期的费用单独列出。",
            Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptySectionText(text: String) {
    Text(text, Modifier.padding(vertical = 20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
