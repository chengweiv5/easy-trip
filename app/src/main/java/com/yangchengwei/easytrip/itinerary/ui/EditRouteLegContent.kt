package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import com.yangchengwei.easytrip.core.model.TransportMode
import androidx.compose.ui.semantics.Role
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.SelectablePill

@Composable
fun EditRouteLegContent(
    draft: RouteModeEditDraft,
    onSelectMode: (TransportMode) -> Unit,
    onClearSelectedModeOverride: () -> Unit,
    onDurationMinutesChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.imePadding().testTag("route-leg-editor"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("交通路段编辑")
            if (draft.fromPlaceName.isNotBlank() && draft.toPlaceName.isNotBlank()) {
                Text(
                    "${draft.fromPlaceName} → ${draft.toPlaceName}",
                    modifier = Modifier.testTag("route-endpoints"),
                )
            }
            Text(
                buildList {
                    add("当前路线")
                    add("已规划")
                    draft.distanceMeters?.let { add(formatDistance(it)) }
                    draft.plannedDurationSeconds?.let { add("预计 ${formatDuration(it)}") }
                }.joinToString(" · "),
                modifier = Modifier.testTag("route-current-context"),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TransportMode.entries.forEach { mode ->
                    SelectablePill(
                        selected = draft.selectedModeOverride == mode,
                        onClick = { onSelectMode(mode) },
                        enabled = !draft.isSaving,
                        label = { Text(mode.label()) },
                        role = Role.RadioButton,
                        modifier = Modifier.testTag("route-mode-option-${mode.name}"),
                    )
                }
            }
            SelectablePill(
                selected = draft.selectedModeOverride == null,
                onClick = onClearSelectedModeOverride,
                enabled = !draft.isSaving,
                label = { Text("使用推荐方式") },
                role = Role.RadioButton,
                modifier = Modifier.testTag("route-mode-auto"),
            )
            OutlinedTextField(
                value = draft.durationMinutesText,
                onValueChange = onDurationMinutesChange,
                label = { Text("预计耗时（分钟）") },
                supportingText = { Text("留空使用路线预计耗时${draft.plannedDurationSeconds?.let { "（${it / 60} 分钟）" }.orEmpty()}") },
                isError = !draft.isValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !draft.isSaving,
                singleLine = true,
                modifier = Modifier.testTag("route-duration-minutes-input"),
            )
            OutlinedTextField(
                value = draft.noteText,
                onValueChange = onNoteChange,
                label = { Text("补充说明") },
                enabled = !draft.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("route-note-input"),
            )
            draft.saveError?.let { Text(it) }
        }
        CompactPrimaryButton(
            onClick = onSave,
            enabled = draft.isValid && !draft.isSaving,
            modifier = Modifier.testTag("save-route"),
        ) {
            val recalculates = draft.selectedModeOverride != draft.originalSelectedModeOverride
            Text(
                when {
                    draft.isSaving -> "保存中…"
                    recalculates -> "保存并重新计算路线"
                    else -> "保存路段"
                },
            )
        }
        CompactSecondaryButton(onClick = onCancel, enabled = !draft.isSaving) { Text("取消") }
    }
}

private fun TransportMode.label() = when (this) {
    TransportMode.WALK -> "步行"
    TransportMode.TAXI -> "打车"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TRANSIT -> "公交"
}
