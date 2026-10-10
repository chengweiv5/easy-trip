package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.EditorSection
import com.yangchengwei.easytrip.core.ui.component.EditorSectionGlyph
import com.yangchengwei.easytrip.core.ui.component.editorBorderColor
import com.yangchengwei.easytrip.core.ui.component.editorPageColor

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
        modifier.imePadding().fillMaxWidth().background(editorPageColor).testTag("itinerary-item-editor"),
    ) {
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp)
                .testTag("itinerary-editor-scroll"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("编辑地点", Modifier.semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(draft.placeName.ifBlank { "到达与停留" }, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (draft.sourceMissing) Text("本次安排已被移除，无法保存。草稿仍保留，可查看后放弃。",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            EditorSection("时间安排", EditorSectionGlyph.TIME, Modifier.testTag("itinerary-section-timing")) {
                TimingSummary(draft, timingExpanded) { timingExpanded = !timingExpanded }
                if (timingExpanded) ItineraryTimingPickers(draft, onArrivalTimeChange, onStayMinutesChange)
            }
            com.yangchengwei.easytrip.expense.ui.InlineExpenseEditor(draft, onExpenseAction)
            EditorSection("地点备注", EditorSectionGlyph.NOTE, Modifier.testTag("itinerary-section-note")) {
                OutlinedTextField(
                    value = draft.noteText,
                    onValueChange = onNoteChange,
                    label = { Text("地点备注（选填）") },
                    enabled = !draft.isSaving,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedBorderColor = editorBorderColor,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("itinerary-note-input"),
                )
            }
            onScheduleAgain?.let { scheduleAgain ->
                CompactSecondaryButton(
                    onClick = scheduleAgain,
                    enabled = !draft.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("schedule-again-place"),
                ) { Text("再次安排${draft.placeName.ifBlank { "这个地点" }}") }
            }
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .testTag("itinerary-editor-actions")) {
            HorizontalDivider(color = editorBorderColor)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("时间、地点备注和费用一起保存", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CompactPrimaryButton(onClick = onSave, enabled = draft.isValid && !draft.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("itinerary-save")) {
                    Text(if (draft.isSaving) "保存中…" else "保存")
                }
                CompactSecondaryButton(onClick = onCancel, enabled = !draft.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("itinerary-cancel")) { Text("取消") }
            }
        }
    }
}

@Composable
private fun TimingSummary(draft: ItineraryEditDraft, expanded: Boolean, onToggle: () -> Unit) {
    val caret = if (expanded) "⌃" else "⌄"
    val arrival = "到达 ${draft.arrivalTimeText.ifBlank { "待定" }} $caret"
    val stay = "停留 ${draft.stayMinutes?.let {
        if (it % 60 == 0) "${it / 60}小时" else "${it}分钟"
    } ?: "待定"} $caret"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.5f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimingSummaryButton(arrival, !draft.isSaving, onToggle,
                    Modifier.fillMaxWidth().testTag("itinerary-timing-toggle"))
                TimingSummaryButton(stay, !draft.isSaving, onToggle, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimingSummaryButton(arrival, !draft.isSaving, onToggle,
                    Modifier.weight(1f).testTag("itinerary-timing-toggle"))
                TimingSummaryButton(stay, !draft.isSaving, onToggle, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TimingSummaryButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick, modifier.heightIn(min = 52.dp), enabled = enabled,
        shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, editorBorderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) { Text(text, style = MaterialTheme.typography.bodyMedium) }
}
