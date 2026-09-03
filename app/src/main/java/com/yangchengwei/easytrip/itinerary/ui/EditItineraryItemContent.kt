package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
fun EditItineraryItemContent(
    draft: ItineraryEditDraft,
    onArrivalTimeChange: (String) -> Unit,
    onStayMinutesChange: (String) -> Unit,
    onNoteChange: (String) -> Unit = {},
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("到达与停留")
            OutlinedTextField(
                value = draft.arrivalTimeText,
                onValueChange = onArrivalTimeChange,
                label = { Text("到达时间（HH:mm）") },
                isError = draft.arrivalTimeText.isNotBlank() && draft.arrivalTime == null,
                enabled = !draft.isSaving,
                singleLine = true,
                modifier = Modifier.testTag("arrival-time-input"),
            )
            OutlinedTextField(
                value = draft.stayMinutesText,
                onValueChange = onStayMinutesChange,
                label = { Text("停留分钟") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !draft.isSaving,
                singleLine = true,
                modifier = Modifier.testTag("stay-minutes-input"),
            )
            OutlinedTextField(
                value = draft.noteText,
                onValueChange = onNoteChange,
                label = { Text("备注") },
                enabled = !draft.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("itinerary-note-input"),
            )
            draft.saveError?.let { Text(it) }
        }
        CompactPrimaryButton(onClick = onSave, enabled = draft.isValid && !draft.isSaving) {
            Text(if (draft.isSaving) "保存中…" else "保存时间")
        }
        CompactSecondaryButton(onClick = onCancel, enabled = !draft.isSaving) { Text("取消") }
    }
}
