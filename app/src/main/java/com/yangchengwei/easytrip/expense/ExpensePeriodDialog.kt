package com.yangchengwei.easytrip.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.expense.ui.expenseMoney
import java.time.YearMonth

@Composable
fun ExpensePeriodDialog(prompter: ExpensePeriodPrompter) {
    val request by prompter.pending.collectAsStateWithLifecycle()
    request?.let { pending ->
        val preview = pending.preview
        AlertDialog(onDismissRequest = { prompter.answer(pending, false) }, title = { Text("花费年月归属将变化") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(preview.before.firstOrNull { it.tripId == preview.changes.first().tripId }?.tripName.orEmpty())
                Text("以下${preview.changes.size}笔费用将随行程日期调整；不会复制费用或改变其金额。")
                preview.changes.groupBy { it.beforeDate?.let(YearMonth::from) to it.afterDate?.let(YearMonth::from) }.forEach { (periods, group) ->
                    Text("${periods.first ?: "未确定日期"} → ${periods.second ?: "未确定日期"}\n${group.size}笔 · ${expenseMoney(group.fold(0L) { sum, row -> Math.addExact(sum, row.cents) })}")
                }
                val years = preview.changes.flatMap { listOfNotNull(it.beforeDate?.year, it.afterDate?.year) }.distinct().sorted()
                years.forEach { year ->
                    Text("全部旅行 · ${year}年：${periodAmount(preview.before, ExpensePeriod.Year(year))} → ${periodAmount(preview.after, ExpensePeriod.Year(year))}")
                }
                Text("全部总额：${periodAmount(preview.before, ExpensePeriod.All)} → ${periodAmount(preview.after, ExpensePeriod.All)}")
                preview.changes.forEach { change -> Text("${change.sourceName} · ${change.beforeDate ?: "未确定日期"} → ${change.afterDate ?: "未确定日期"}", style = MaterialTheme.typography.bodySmall) }
                Text("取消后日期、行程与费用均保持原样。金额按旅行日统计，不按录入时间。", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton({ prompter.answer(pending, true) }, Modifier.testTag("expense-period-confirm")) { Text("确认调整") } },
            dismissButton = { TextButton({ prompter.answer(pending, false) }, Modifier.testTag("expense-period-cancel")) { Text("保留原安排") } })
    }
}

private fun periodAmount(records: List<ExpenseRecord>, period: ExpensePeriod): String =
    try { expenseMoney(reviewExpenses(records, period).cents) } catch (_: ArithmeticException) { "超出统计范围" }
