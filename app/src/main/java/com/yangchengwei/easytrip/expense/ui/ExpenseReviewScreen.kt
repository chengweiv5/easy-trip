package com.yangchengwei.easytrip.expense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.expense.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

val LocalExpenseReviewOpener = staticCompositionLocalOf<((String?) -> Unit)?> { null }

fun expenseMoney(cents: Long): String = "¥" + String.format(Locale.CHINA, "%,.2f", BigDecimal.valueOf(cents, 2))

@Composable
fun ExpensePrimaryNavigation(expenses: Boolean, onTrips: () -> Unit, onExpenses: () -> Unit) {
    NavigationBar(Modifier.testTag("expense-primary-navigation")) {
        NavigationBarItem(!expenses, onTrips, icon = { Text("♧") }, label = { Text("旅行") }, modifier = Modifier.testTag("primary-trips"))
        NavigationBarItem(expenses, onExpenses, icon = { Text("¥") }, label = { Text("花费") }, modifier = Modifier.testTag("primary-expenses"))
    }
}

@Composable
fun ExpenseReviewScreen(
    model: ExpenseReviewViewModel,
    onBack: () -> Unit,
    onOpenSource: (ExpenseRecord) -> Unit,
    onTrips: (() -> Unit)? = null,
    onOpenTrip: (String) -> Unit = {},
    onTripSettings: (String) -> Unit = {},
) {
    val state by model.state.collectAsStateWithLifecycle()
    val scope = state.scope
    val root = scope.tripId == null && !state.canGoBack
    val savedViews = rememberSaveableStateHolder()
    var chooser by remember { mutableStateOf<String?>(null) }
    val back = { if (!model.back()) onBack() }
    if (state.editor != null) { ExpenseRecordEditor(state.editor!!, model); return }
    BackHandler { if (chooser != null) chooser = null else back() }
    Scaffold(bottomBar = { if (root && onTrips != null) ExpensePrimaryNavigation(true, onTrips, {}) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!root) TextButton(back, Modifier.testTag("expense-back")) { Text("‹ 返回") }
                Text(when { scope.category != null -> "${scope.category.label}明细"; scope.unclassified -> "待分类明细"; scope.tripId != null -> "旅行花费"; scope.period == ExpensePeriod.All -> "历年花费"; scope.period == ExpensePeriod.Undated -> "未确定日期"; else -> "花费" }, style = MaterialTheme.typography.titleLarge)
            }
            when (val load = state.load) {
                ExpenseLoadState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.testTag("expense-loading")) }
                is ExpenseLoadState.Failed -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(load.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("expense-load-error"))
                    Button(model::retry) { Text("重试") }
                }
                is ExpenseLoadState.Ready -> savedViews.SaveableStateProvider(scope.toString()) {
                    val scroll = rememberScrollState(model.scrollOffset(scope))
                    DisposableEffect(scope, scroll) { onDispose { model.rememberScroll(scope, scroll.value) } }
                    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp).testTag("expense-review-scroll"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val records = scope.select(load.records)
                        val total = reviewExpenses(records, ExpensePeriod.All)
                        val year = when (val period = scope.period) { is ExpensePeriod.Year -> period.value; is ExpensePeriod.Month -> period.value.year; else -> null }
                        if (scope.category == null && !scope.unclassified && year != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton({ if (year > LocalDate.MIN.year) model.selectPeriod(ExpensePeriod.Year(year - 1)) }, Modifier.testTag("expense-period-previous")) { Text("‹") }
                            OutlinedButton({ chooser = "year" }, Modifier.weight(1f).testTag("expense-choose-year")) { Text("${year}年 ▾") }
                            TextButton({ if (year < LocalDate.MAX.year) model.selectPeriod(ExpensePeriod.Year(year + 1)) }, Modifier.testTag("expense-period-next")) { Text("›") }
                            TextButton({ model.selectPeriod(ExpensePeriod.All) }) { Text("历年") }
                        }
                        if (scope.tripId != null) {
                            val tripRecords = load.records.filter { it.tripId == scope.tripId }
                            Text(tripRecords.firstOrNull()?.tripName?.ifBlank { "旅行" } ?: "旅行", style = MaterialTheme.typography.titleMedium)
                            if (scope.period != ExpensePeriod.All || scope.dayId != null) TextButton(model::showWholeTrip) { Text("查看全程 · ${expenseMoney(reviewExpenses(tripRecords, ExpensePeriod.All).cents)}") }
                            TextButton({ onOpenTrip(scope.tripId) }) { Text("进入原旅行") }
                            if (tripRecords.any { it.date == null }) TextButton({ onTripSettings(scope.tripId) }) { Text("设置旅行日期") }
                        }
                        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${periodLabel(scope.period)} · 已记录花费", style = MaterialTheme.typography.labelLarge)
                                Text(if (total.records == 0) "—" else expenseMoney(total.cents), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("expense-total"))
                                Text(if (total.records == 0) "尚无费用记录" else "${total.records}笔 · ${total.trips}次有记录旅行")
                            }
                        }
                        if (scope.period == ExpensePeriod.All && scope.category == null && !scope.unclassified) {
                            Text("按年回顾", style = MaterialTheme.typography.titleMedium)
                            load.records.mapNotNull { it.date?.year }.distinct().sortedDescending().forEach { y ->
                                val count = reviewExpenses(scope.copy(period = ExpensePeriod.Year(y)).select(load.records), ExpensePeriod.All)
                                if (count.records > 0) ReviewRow("${y}年", count, "review-year-$y") { model.selectPeriod(ExpensePeriod.Year(y)) }
                            }
                        }
                        if (year != null && scope.category == null && !scope.unclassified) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (scope.period is ExpensePeriod.Month) "${(scope.period as ExpensePeriod.Month).value.monthValue}月" else "按月查看", style = MaterialTheme.typography.titleMedium)
                                TextButton({ chooser = "month" }) { Text("选择月份") }
                            }
                            if (scope.period is ExpensePeriod.Year) MonthGrid(scope, load.records, year) { model.selectPeriod(ExpensePeriod.Month(it)) }
                        }
                        if (scope.category == null && !scope.unclassified && (scope.period != ExpensePeriod.All || scope.tripId != null)) {
                            Text("按类别查看", style = MaterialTheme.typography.titleMedium)
                            ExpenseCategory.entries.forEach { category ->
                                val count = reviewExpenses(records.filter { it.category == category }, ExpensePeriod.All)
                                ReviewRow(category.label, count, "review-category-${category.storageKey}", percent = if (total.cents == 0L) "—" else String.format(Locale.CHINA, "%.1f%%", count.cents.toDouble() / total.cents * 100)) { model.selectCategory(category) }
                            }
                            val unclassified = reviewExpenses(records.filter { it.category == null }, ExpensePeriod.All)
                            if (unclassified.records > 0) ReviewRow("待分类 · 旧费用", unclassified, "review-unclassified") { model.selectCategory(null) }
                        }
                        if (scope.tripId == null && scope.category == null && !scope.unclassified && scope.period != ExpensePeriod.All) {
                            Text("哪些旅行贡献了花费", style = MaterialTheme.typography.titleMedium)
                            records.groupBy { it.tripId }.forEach { (id, group) ->
                                val whole = reviewExpenses(load.records.filter { it.tripId == id }, ExpensePeriod.All)
                                ReviewRow(group.first().tripName.ifBlank { "旅行" }, reviewExpenses(group, ExpensePeriod.All), "review-trip-$id", detail = "${if (scope.period is ExpensePeriod.Year) "本年部分" else if (scope.period is ExpensePeriod.Month) "本月部分" else "当前范围"} ${expenseMoney(reviewExpenses(group, ExpensePeriod.All).cents)} · 全程 ${expenseMoney(whole.cents)} · ${whole.records}笔") { model.selectTrip(id) }
                            }
                        }
                        if (scope.tripId != null || scope.category != null || scope.unclassified || scope.period == ExpensePeriod.Undated) {
                            Text("费用明细", style = MaterialTheme.typography.titleMedium)
                            records.groupBy { it.tripId }.entries.sortedByDescending { (_, group) -> group.maxOfOrNull { it.date ?: LocalDate.MIN } }.forEach { (_, group) ->
                                val groupTotal = reviewExpenses(group, ExpensePeriod.All)
                                Text("${group.first().tripName.ifBlank { "旅行" }} · ${expenseMoney(groupTotal.cents)} · ${group.size}笔", style = MaterialTheme.typography.titleSmall)
                            group.sortedWith(compareByDescending<ExpenseRecord> { it.date }.thenBy { it.key.id }).forEach { record ->
                                Surface(shape = MaterialTheme.shapes.small, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${record.sourceName} · ${expenseMoney(record.cents)}", fontWeight = FontWeight.Medium)
                                        Text("${record.category?.label ?: "待分类"}${record.note?.let { " · $it" }.orEmpty()}")
                                        Text("${record.tripName} · 第${record.dayNumber ?: "—"}天 · ${record.date ?: "未确定日期"}", style = MaterialTheme.typography.bodySmall)
                                        Row {
                                            TextButton({ if (record.key.kind == ExpenseSourceKind.PLACE) model.editRecord(record) else onOpenSource(record) }, Modifier.testTag("expense-record-edit")) { Text(if (record.key.kind == ExpenseSourceKind.ROUTE) "编辑路段" else "编辑本笔") }
                                            TextButton({ onOpenTrip(record.tripId) }) { Text("原旅行") }
                                            if (record.date == null) TextButton({ onTripSettings(record.tripId) }) { Text("设置旅行日期") }
                                        }
                                    }
                                }
                            }
                        }
                        }
                        if (scope.period != ExpensePeriod.Undated && scope.category == null && !scope.unclassified) {
                            val undated = reviewExpenses(scope.copy(period = ExpensePeriod.Undated).select(load.records), ExpensePeriod.All)
                            if (undated.records > 0) ReviewRow("未确定日期", undated, "review-undated", detail = "单列，未计入任何确定年月") { model.selectPeriod(ExpensePeriod.Undated) }
                        }
                        Text("金额按旅行日归属年月。仅统计已记录花费；没有记录不代表没有消费。未确定日期的费用单独列出。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    chooser?.let { kind ->
        val all = (state.load as? ExpenseLoadState.Ready)?.records.orEmpty()
        val selectedYear = when (val p = scope.period) { is ExpensePeriod.Year -> p.value; is ExpensePeriod.Month -> p.value.year; else -> LocalDate.now().year }
        AlertDialog(onDismissRequest = { chooser = null }, title = { Text(if (kind == "year") "选择年份" else "选择月份") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (kind == "year") (all.mapNotNull { it.date?.year } + selectedYear + LocalDate.now().year).distinct().sortedDescending().forEach { year ->
                    TextButton({ chooser = null; model.selectPeriod(ExpensePeriod.Year(year)) }, Modifier.fillMaxWidth()) { Text("${year}年") }
                } else MonthGrid(scope, all, selectedYear) { chooser = null; model.selectPeriod(ExpensePeriod.Month(it)) }
            }
        }, confirmButton = { TextButton({ chooser = null }) { Text("取消") } })
    }
}

private fun periodLabel(period: ExpensePeriod): String = when (period) {
    ExpensePeriod.All -> "全部"
    ExpensePeriod.Undated -> "未确定日期"
    is ExpensePeriod.Year -> "${period.value}年"
    is ExpensePeriod.Month -> "${period.value.year}年${period.value.monthValue}月"
}

@Composable
private fun ReviewRow(title: String, total: ExpenseTotal, tag: String, percent: String? = null, detail: String? = null, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text(if (total.records == 0) "—" else expenseMoney(total.cents))
            }
            Text(if (total.records == 0) "尚无记录" else "${total.records}笔 · ${total.trips}次旅行${percent?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun MonthGrid(scope: ExpenseScope, all: List<ExpenseRecord>, year: Int, onSelect: (YearMonth) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..12).toList().chunked(3).forEach { months ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                months.forEach { month ->
                    val period = YearMonth.of(year, month)
                    val total = reviewExpenses(scope.copy(period = ExpensePeriod.Month(period)).select(all), ExpensePeriod.All)
                    Surface(Modifier.weight(1f).clickable { onSelect(period) }.testTag("review-month-$month"), shape = MaterialTheme.shapes.small, tonalElevation = 2.dp) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${month}月", fontWeight = FontWeight.Medium)
                            Text(if (total.records == 0) "—" else expenseMoney(total.cents), style = MaterialTheme.typography.bodySmall)
                            Text(if (total.records == 0) "尚无记录" else "${total.records}笔", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
