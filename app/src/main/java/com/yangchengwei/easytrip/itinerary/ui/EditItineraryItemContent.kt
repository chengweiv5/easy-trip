package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.imePadding().testTag("itinerary-item-editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(draft.placeName.ifBlank { "到达与停留" }, style = MaterialTheme.typography.titleMedium)
            Text("到达时间、停留时长与备注", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ItineraryTimingPickers(draft, onArrivalTimeChange, onStayMinutesChange)
            OutlinedTextField(
                value = draft.noteText,
                onValueChange = onNoteChange,
                label = { Text("备注") },
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
        CompactPrimaryButton(onClick = onSave, enabled = draft.isValid && !draft.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (draft.isSaving) "保存中…" else "保存时间")
        }
        CompactSecondaryButton(onClick = onCancel, enabled = !draft.isSaving, modifier = Modifier.fillMaxWidth()) { Text("取消") }
    }
}
