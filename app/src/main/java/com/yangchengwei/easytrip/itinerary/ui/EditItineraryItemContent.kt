package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
fun EditItineraryItemContent(
    draft: ItineraryEditDraft,
    onArrivalTimeChange: (String) -> Unit,
    onStayMinutesChange: (String) -> Unit,
    onNoteChange: (String) -> Unit = {},
    onScheduleAgain: (() -> Unit)? = null,
    onExpenseChange: (String) -> Unit = {},
    onExpenseAction: (DayItineraryAction) -> Unit = {},
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timingExpanded by rememberSaveable(draft.itemId, draft.generation) { mutableStateOf(false) }
    if (draft.showDiscardConfirmation) androidx.compose.material3.AlertDialog(
        onDismissRequest = { onExpenseAction(DayItineraryAction.KeepEditing) },
        title = { Text("放弃本次修改？") },
        text = { Text("时间、停留、地点备注和所有费用改动都将放弃。") },
        confirmButton = {
            androidx.compose.material3.TextButton({ onExpenseAction(DayItineraryAction.DiscardEdit) }) { Text("放弃修改") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton({ onExpenseAction(DayItineraryAction.KeepEditing) }) { Text("继续编辑") }
        },
    )
    Column(
        modifier.imePadding().testTag("itinerary-item-editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(draft.placeName.ifBlank { "到达与停留" }, style = MaterialTheme.typography.titleMedium)
            Text("到达时间、停留时长、花费与备注", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (draft.sourceMissing) Text("本次安排已被移除，无法保存。草稿仍保留，可查看后放弃。",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { timingExpanded = !timingExpanded }, enabled = !draft.isSaving,
                    modifier = Modifier.weight(1f).testTag("itinerary-timing-toggle"),
                ) { Text("到达 ${draft.arrivalTimeText.ifBlank { "待定" }} ${if (timingExpanded) "⌃" else "⌄"}") }
                androidx.compose.material3.OutlinedButton(
                    onClick = { timingExpanded = !timingExpanded }, enabled = !draft.isSaving,
                    modifier = Modifier.weight(1f),
                ) { Text("停留 ${draft.stayMinutes?.let { "${it}分钟" } ?: "待定"}") }
            }
            if (timingExpanded) ItineraryTimingPickers(draft, onArrivalTimeChange, onStayMinutesChange)
            com.yangchengwei.easytrip.expense.ui.InlineExpenseEditor(draft, onExpenseAction)
            OutlinedTextField(
                value = draft.noteText,
                onValueChange = onNoteChange,
                label = { Text("地点备注（选填）") },
                enabled = !draft.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("itinerary-note-input"),
            )
            onScheduleAgain?.let { scheduleAgain ->
                CompactSecondaryButton(
                    onClick = scheduleAgain,
                    enabled = !draft.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("schedule-again-place"),
                ) { Text("再次安排${draft.placeName.ifBlank { "这个地点" }}") }
            }
        }
        CompactPrimaryButton(onClick = onSave, enabled = draft.isValid && !draft.isSaving, modifier = Modifier.fillMaxWidth().testTag("itinerary-save")) {
            Text(if (draft.isSaving) "保存中…" else "保存")
        }
        CompactSecondaryButton(onClick = onCancel, enabled = !draft.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("itinerary-cancel")) { Text("取消") }
    }
}
