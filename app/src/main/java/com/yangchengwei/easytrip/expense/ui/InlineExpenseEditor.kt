package com.yangchengwei.easytrip.expense.ui

import com.yangchengwei.easytrip.core.ui.formatCount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.expense.*
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft
import com.yangchengwei.easytrip.core.ui.component.EditorSection
import com.yangchengwei.easytrip.core.ui.component.EditorSectionGlyph
import com.yangchengwei.easytrip.core.ui.component.editorBorderColor

@Composable
fun InlineExpenseEditor(draft: ItineraryEditDraft, onAction: (DayItineraryAction) -> Unit) {
    val validation = draft.expenseValidation
    val total = (validation as? ExpenseDraftValidation.Valid)
    EditorSection(
        "本次安排花费", EditorSectionGlyph.WALLET, Modifier.testTag("itinerary-section-expenses"),
        summary = when {
            total == null -> "请完善费用"
            total.expenses.isEmpty() -> "未记录"
            else -> "${formatCount(total.expenses.size)}笔 · ${formatExpense(total.totalCents)}"
        },
    ) {
        val rows = if (draft.showAllExpenses) draft.expenses else draft.expenses.take(3)
        rows.forEachIndexed { index, row ->
            key(row.key) {
                if (index > 0) HorizontalDivider(color = editorBorderColor)
                val expanded = draft.expandedExpenseKey == row.key || draft.expenses.size == 1
                if (expanded) {
                    InlineExpenseRow(row, !draft.isSaving, validation as? ExpenseDraftValidation.Invalid,
                        onChange = { onAction(DayItineraryAction.UpdateExpenseRow(row.key, it)) },
                        onRemove = { onAction(DayItineraryAction.RemoveExpense(row.key)) }, flat = true)
                } else {
                    Surface(
                        Modifier.fillMaxWidth().testTag("expense-row-${row.key}")
                            .clickable(enabled = !draft.isSaving, role = Role.Button,
                                onClickLabel = "展开本笔费用") { onAction(DayItineraryAction.ExpandExpense(row.key)) },
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(Modifier.padding(vertical = 10.dp)) {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.5f
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ExpenseCategoryIcon(row.category)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(row.category?.label ?: "待分类",
                                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        if (row.note.isNotBlank()) Text(row.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (stacked) ExpenseRowAmount(row)
                                    }
                                    if (!stacked) ExpenseRowAmount(row)
                                    Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
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
        if (draft.expenses.size > 1) Text("点任意一笔，直接在这里展开修改",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        ) { Text(if (draft.showAllExpenses) "收起" else "查看全部 ${formatCount(draft.expenses.size)} 笔") }
        draft.expenseRemoval?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已移除本笔，保存后生效", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onAction(DayItineraryAction.UndoExpenseRemoval) }, enabled = !draft.isSaving) { Text("撤销") }
            }
        }
        HorizontalDivider(color = editorBorderColor)
        OutlinedButton(
            onClick = { onAction(DayItineraryAction.AddExpense) }, enabled = !draft.isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("expense-add"),
            shape = RoundedCornerShape(10.dp),
        ) { Text("＋ 再记一笔") }
    }
}

@Composable
private fun ExpenseRowAmount(row: ExpenseDraftRow) {
    Text(parseExpense(row.amount)?.let(::formatExpense) ?: "待填写",
        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
internal fun InlineExpenseRow(
    row: ExpenseDraftRow, enabled: Boolean, error: ExpenseDraftValidation.Invalid?,
    onChange: (ExpenseDraftRow) -> Unit, onRemove: (() -> Unit)?,
    flat: Boolean = false,
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
    val isPartial = !row.isEmptyNewExpense
    Surface(shape = MaterialTheme.shapes.small, tonalElevation = if (flat) 0.dp else 1.dp) {
        Column(Modifier.padding(if (flat) 0.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (flat) 8.dp else 4.dp)) {
            OutlinedTextField(
                row.amount, { onChange(row.copy(amount = it)) }, enabled = enabled, singleLine = true,
                prefix = { Text("¥") }, placeholder = { Text("输入金额") },
                isError = isPartial && error?.rowKey == row.key && error.field == ExpenseDraftField.AMOUNT,
                shape = if (flat) RoundedCornerShape(10.dp) else MaterialTheme.shapes.extraSmall,
                textStyle = if (flat) MaterialTheme.typography.headlineSmall else LocalTextStyle.current,
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
                                    selected = row.category == category, onClick = { onChange(row.selectCategory(category)) },
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
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackActions = maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.5f
                val actions: @Composable () -> Unit = {
                    if (!showNote) TextButton({ showNote = true }, enabled = enabled) { Text("＋ 费用备注（选填）") }
                    if (onRemove != null) TextButton(onRemove, enabled = enabled,
                        modifier = Modifier.testTag("expense-remove-${row.key}")) { Text("移除本笔") }
                }
                if (stackActions) Column { actions() }
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { actions() }
            }
        }
    }
}
