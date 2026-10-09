package com.yangchengwei.easytrip.expense.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.expense.*
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft

@Composable
fun InlineExpenseEditor(draft: ItineraryEditDraft, onAction: (DayItineraryAction) -> Unit) {
    val validation = draft.expenseValidation
    val total = (validation as? ExpenseDraftValidation.Valid)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("本次安排花费", style = MaterialTheme.typography.titleSmall)
            Text(when {
                total == null -> "请完善费用"
                total.expenses.isEmpty() -> "未记录"
                else -> "${total.expenses.size}笔 · ${formatExpense(total.totalCents)}"
            }, style = MaterialTheme.typography.bodySmall)
        }
        val rows = if (draft.showAllExpenses) draft.expenses else draft.expenses.take(3)
        rows.forEach { row ->
            key(row.key) {
                val expanded = draft.expandedExpenseKey == row.key || draft.expenses.size == 1
                if (expanded) {
                    InlineExpenseRow(row, !draft.isSaving, validation as? ExpenseDraftValidation.Invalid,
                        onChange = { onAction(DayItineraryAction.UpdateExpenseRow(row.key, it)) },
                        onRemove = { onAction(DayItineraryAction.RemoveExpense(row.key)) })
                } else {
                    Surface(
                        Modifier.fillMaxWidth().testTag("expense-row-${row.key}")
                            .clickable(enabled = !draft.isSaving) { onAction(DayItineraryAction.ExpandExpense(row.key)) },
                        shape = MaterialTheme.shapes.small, tonalElevation = 1.dp,
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(listOfNotNull(row.category?.label ?: "待分类", row.note.takeIf { it.isNotBlank() }).joinToString(" · "), Modifier.weight(1f))
                                Text(parseExpense(row.amount)?.let(::formatExpense) ?: "待填写")
                            }
                            if (validation is ExpenseDraftValidation.Invalid && validation.rowKey == row.key) {
                                Text(validation.message, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("expense-error-${row.key}")
                                        .clickable(enabled = !draft.isSaving) { onAction(DayItineraryAction.ExpandExpense(row.key)) })
                            }
                        }
                    }
                }
            }
        }
        if (validation is ExpenseDraftValidation.Invalid && draft.expenses.size > 3 &&
            !draft.showAllExpenses && rows.none { it.key == validation.rowKey }) {
            TextButton(onClick = {
                onAction(DayItineraryAction.ShowAllExpenses)
                onAction(DayItineraryAction.ExpandExpense(validation.rowKey))
            }, enabled = !draft.isSaving) { Text("有一笔费用未完善，点击定位", color = MaterialTheme.colorScheme.error) }
        }
        if (draft.expenses.size > 3) TextButton(
            onClick = { onAction(DayItineraryAction.ShowAllExpenses) }, enabled = !draft.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("expense-show-all"),
        ) { Text(if (draft.showAllExpenses) "收起" else "查看全部 ${draft.expenses.size} 笔") }
        draft.expenseRemoval?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已移除本笔，保存后生效", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onAction(DayItineraryAction.UndoExpenseRemoval) }, enabled = !draft.isSaving) { Text("撤销") }
            }
        }
        OutlinedButton(
            onClick = { onAction(DayItineraryAction.AddExpense) }, enabled = !draft.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("expense-add"),
        ) { Text("＋ 再记一笔") }
    }
}

@Composable
internal fun InlineExpenseRow(
    row: ExpenseDraftRow, enabled: Boolean, error: ExpenseDraftValidation.Invalid?,
    onChange: (ExpenseDraftRow) -> Unit, onRemove: (() -> Unit)?,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember(row.key) { FocusRequester() }
    val bringIntoView = remember(row.key) { BringIntoViewRequester() }
    LaunchedEffect(row.key) {
        if (row.key.startsWith("new-")) {
            bringIntoView.bringIntoView()
            focus.requestFocus()
        }
    }
    var showNote by remember(row.key) { mutableStateOf(row.note.isNotEmpty()) }
    val isPartial = row.amount.isNotBlank() || row.category != null || row.note.isNotBlank()
    Surface(shape = MaterialTheme.shapes.small, tonalElevation = 1.dp) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                row.amount, { onChange(row.copy(amount = it)) }, enabled = enabled, singleLine = true,
                prefix = { Text("¥") }, placeholder = { Text("输入金额") },
                isError = isPartial && error?.rowKey == row.key && error.field == ExpenseDraftField.AMOUNT,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView)
                    .focusRequester(focus).testTag("expense-amount-${row.key}"),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (LocalDensity.current.fontScale >= 1.5f || maxWidth < 240.dp) 2 else 3
                Column(Modifier.fillMaxWidth()) {
                    ExpenseCategory.entries.chunked(columns).forEach { categories ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            categories.forEach { category ->
                                FilterChip(
                                    selected = row.category == category, onClick = { onChange(row.copy(category = category)) },
                                    label = { Text(category.label) }, enabled = enabled,
                                    leadingIcon = if (row.category == category) ({ Text("✓") }) else null,
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                        .testTag("expense-category-${row.key}-${category.storageKey}"),
                                )
                            }
                        }
                    }
                }
            }
            if (isPartial && error?.rowKey == row.key) {
                Text(error.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (showNote) OutlinedTextField(
                row.note, { onChange(row.copy(note = it)) }, label = { Text("费用备注（选填）") },
                enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("expense-note-${row.key}"),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (!showNote) TextButton({ showNote = true }, enabled = enabled) { Text("＋ 费用备注（选填）") }
                else Spacer(Modifier.weight(1f))
                if (onRemove != null) TextButton(onRemove, enabled = enabled, modifier = Modifier.testTag("expense-remove-${row.key}")) { Text("移除本笔") }
            }
        }
    }
}
