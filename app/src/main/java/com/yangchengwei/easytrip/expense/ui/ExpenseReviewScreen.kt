package com.yangchengwei.easytrip.expense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.expense.*
import java.math.BigDecimal
import java.util.Locale

val LocalTripExpenseOpener = staticCompositionLocalOf<((String) -> Unit)?> { null }
val LocalExpenseReviewOpener = staticCompositionLocalOf<((String?) -> Unit)?> { null }

fun expenseMoney(cents: Long): String = "¥" + String.format(Locale.CHINA, "%,.2f", BigDecimal.valueOf(cents, 2))

@Composable
fun ExpensePrimaryNavigation(expenses: Boolean, onTrips: () -> Unit, onExpenses: () -> Unit) {
    NavigationBar(Modifier.testTag("expense-primary-navigation")) {
        NavigationBarItem(!expenses, onTrips, icon = { ExpenseGlyphIcon(ExpenseGlyph.TRIP) }, label = { Text("旅行") }, modifier = Modifier.testTag("primary-trips"))
        NavigationBarItem(expenses, onExpenses, icon = { ExpenseGlyphIcon(ExpenseGlyph.CATEGORIES) }, label = { Text("花费") }, modifier = Modifier.testTag("primary-expenses"))
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
    var chooser by remember { mutableStateOf(false) }
    val back = { if (!model.back()) onBack() }
    if (state.editor != null) { ExpenseRecordEditor(state.editor!!, model); return }
    BackHandler { if (chooser) chooser = false else back() }
    Scaffold(containerColor = expensePageColor,
        bottomBar = { if (root && onTrips != null) ExpensePrimaryNavigation(true, onTrips, {}) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!root) TextButton(back, Modifier.testTag("expense-back"), contentPadding = PaddingValues(end = 12.dp)) { Text("‹ 返回") }
                Text(when {
                    scope.category != null -> "${scope.category.label}明细"
                    scope.unclassified -> "待分类明细"
                    scope.tripId != null -> "旅行花费"
                    scope.period == ExpensePeriod.All -> "历年花费"
                    scope.period == ExpensePeriod.Undated -> "未确定日期"
                    scope.period is ExpensePeriod.Month -> "月度花费"
                    else -> "花费"
                }, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (scope.tripId == null && scope.category == null && !scope.unclassified && scope.period is ExpensePeriod.Year) {
                    TextButton({ model.selectPeriod(ExpensePeriod.All) }, Modifier.testTag("expense-all-years")) { Text("历年 ›") }
                }
            }
            savedViews.SaveableStateProvider(scope.toString()) {
                val scroll = rememberScrollState(model.scrollOffset(scope))
                DisposableEffect(scope, scroll) { onDispose { model.rememberScroll(scope, scroll.value) } }
                Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp).testTag("expense-review-scroll"),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (scope.category == null && !scope.unclassified &&
                        (scope.period is ExpensePeriod.Year || scope.period is ExpensePeriod.Month)) {
                        ExpensePeriodControl(scope.period, { chooser = true }, { model.shiftPeriod(-1) }, { model.shiftPeriod(1) })
                    }
                    when (val load = state.load) {
                        ExpenseLoadState.Loading -> ExpenseSection("正在读取花费", ExpenseGlyph.RECORDS, "expense-section-loading") {
                            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.testTag("expense-loading"))
                            }
                        }
                        is ExpenseLoadState.Failed -> ExpenseSection("花费暂时无法读取", ExpenseGlyph.INFO, "expense-section-error") {
                            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(load.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("expense-load-error"))
                                Button(model::retry) { Text("重试") }
                            }
                        }
                        is ExpenseLoadState.Ready -> ExpenseReviewContent(scope, load.records, model, onOpenSource, onOpenTrip, onTripSettings)
                    }
                }
            }
        }
    }
    if (chooser) ExpensePeriodPicker(scope, (state.load as? ExpenseLoadState.Ready)?.records,
        onDismiss = { chooser = false }, onSelect = { chooser = false; model.selectPeriod(it) })
}
