package com.yangchengwei.easytrip.expense.ui

import com.yangchengwei.easytrip.core.ui.formatCount
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.expense.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun ExpensePeriodControl(period: ExpensePeriod, onChoose: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    val month = period is ExpensePeriod.Month
    Surface(Modifier.fillMaxWidth().testTag("expense-period-control"), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, expenseBorderColor)) {
        Row(Modifier.heightIn(min = 60.dp).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onChoose)
                .testTag(if (month) "expense-choose-month" else "expense-choose-year")
                .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseGlyphIcon(ExpenseGlyph.CALENDAR)
                Text("${periodLabel(period)} ▾", fontSize = if (month) 18.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            VerticalDivider(Modifier.height(32.dp), color = expenseBorderColor)
            PeriodArrow("‹", if (month) "上个月" else "上一年", "expense-period-previous", period.adjacent(-1) != null, onPrevious)
            PeriodArrow("›", if (month) "下个月" else "下一年", "expense-period-next", period.adjacent(1) != null, onNext)
        }
    }
}

@Composable
private fun PeriodArrow(text: String, description: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick, enabled = enabled, modifier = Modifier.width(44.dp).heightIn(min = 44.dp)
        .testTag(tag).semantics { contentDescription = description }, contentPadding = PaddingValues(0.dp)) {
        Text(text, fontSize = 24.sp)
    }
}

@Composable
internal fun ExpensePeriodPicker(scope: ExpenseScope, all: List<ExpenseRecord>?, onDismiss: () -> Unit, onSelect: (ExpensePeriod) -> Unit) {
    val monthMode = scope.period is ExpensePeriod.Month
    val selectedYear = when (val p = scope.period) {
        is ExpensePeriod.Year -> p.value
        is ExpensePeriod.Month -> p.value.year
        else -> LocalDate.now().year
    }
    var shownYear by remember(scope.period) { mutableIntStateOf(selectedYear) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (monthMode) "选择月份" else "选择年份") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (monthMode) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${shownYear}年", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        PeriodArrow("‹", "上一年", "picker-year-previous", shownYear > LocalDate.MIN.year) { shownYear-- }
                        PeriodArrow("›", "下一年", "picker-year-next", shownYear < LocalDate.MAX.year) { shownYear++ }
                    }
                    val columns = if (LocalDensity.current.fontScale >= 1.5f) 2 else 3
                    (1..12).toList().chunked(columns).forEach { months ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            months.forEach { month ->
                                val period = ExpensePeriod.Month(YearMonth.of(shownYear, month))
                                val total = all?.let { reviewExpenses(scope.copy(period = period).select(it), ExpensePeriod.All) }
                                val selected = scope.period == period
                                Surface(Modifier.weight(1f).selectable(selected, role = Role.RadioButton) { onSelect(period) }
                                    .testTag("picker-month-$month"), shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else expenseBorderColor)) {
                                    Column(Modifier.padding(8.dp).heightIn(min = 56.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${month}月${if (selected) " ✓" else ""}", fontWeight = FontWeight.SemiBold)
                                        Text(when { total == null -> "统计待加载"; total.records == 0 -> "尚无记录"; else -> expenseMoney(total.cents) }, style = MaterialTheme.typography.labelSmall)
                                        if (total != null && total.records > 0) Text("${formatCount(total.records)}笔", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    (all.orEmpty().mapNotNull { it.date?.year } + selectedYear + LocalDate.now().year).distinct().sortedDescending().forEach { year ->
                        val selected = year == selectedYear
                        val total = all?.let { reviewExpenses(scope.copy(period = ExpensePeriod.Year(year)).select(it), ExpensePeriod.All) }
                        Column(Modifier.fillMaxWidth().background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .selectable(selected, role = Role.RadioButton) { onSelect(ExpensePeriod.Year(year)) }
                            .testTag("picker-year-$year").padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${year}年${if (selected) " ✓ 已选择" else ""}", fontWeight = FontWeight.SemiBold)
                            Text(when { total == null -> "统计待加载"; total.records == 0 -> "尚无记录"; else -> "${expenseMoney(total.cents)} · ${formatCount(total.records)}笔" },
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss, Modifier.testTag("expense-picker-cancel")) { Text("取消") } })
}
