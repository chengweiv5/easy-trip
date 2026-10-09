package com.yangchengwei.easytrip.expense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.expense.*

@Composable
fun ExpenseRecordEditor(draft: ExpenseRecordDraft, model: ExpenseReviewViewModel) {
    BackHandler { model.closeEditor() }
    Scaffold(modifier = Modifier.imePadding(), containerColor = expensePageColor, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(model::closeEditor, enabled = !draft.saving, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(model::saveEditor, enabled = !draft.saving && draft.validation is ExpenseDraftValidation.Valid,
                modifier = Modifier.weight(1f).testTag("expense-record-save")) { Text(if (draft.saving) "保存中…" else "保存") }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("编辑花费", style = MaterialTheme.typography.headlineSmall)
            ExpenseSection("花费来源", ExpenseGlyph.TRIP, "expense-section-editor-source") {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(draft.expected.sourceName, style = MaterialTheme.typography.titleMedium)
                    Text("${draft.expected.tripName} · ${draft.expected.date ?: "未确定日期"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("仅修改这一笔花费，不改变地点时间、停留和地点备注", style = MaterialTheme.typography.bodySmall)
                }
            }
            ExpenseSection("金额与类别", ExpenseGlyph.CATEGORIES, "expense-section-editor-form") {
                Box(Modifier.padding(top = 12.dp)) {
                    InlineExpenseRow(draft.row, !draft.saving, draft.validation as? ExpenseDraftValidation.Invalid, model::updateEditor, null)
                }
            }
            draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("expense-record-error")) }
            TextButton(model::requestDelete, enabled = !draft.saving, modifier = Modifier.testTag("expense-record-delete")) { Text("删除本笔花费", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (draft.confirmDiscard) AlertDialog(onDismissRequest = model::keepEditor, title = { Text("放弃本次修改？") }, text = { Text("尚未保存的修改将丢弃，已记录花费保持不变。") },
        confirmButton = { TextButton(model::discardEditor) { Text("放弃修改") } }, dismissButton = { TextButton(model::keepEditor) { Text("继续编辑") } })
    if (draft.confirmDelete) AlertDialog(onDismissRequest = model::keepEditor, title = { Text("删除这笔花费？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${draft.expected.sourceName} · ${expenseMoney(draft.expected.cents)}", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(color = expenseBorderColor)
                Text("仅删除这1笔，保留地点与其它花费。该笔金额也会从对应年月统计中移除。")
            }
        },
        confirmButton = { TextButton(model::deleteEditor, modifier = Modifier.testTag("expense-confirm-delete")) { Text("确认删除") } }, dismissButton = { TextButton(model::keepEditor) { Text("取消") } })
}
