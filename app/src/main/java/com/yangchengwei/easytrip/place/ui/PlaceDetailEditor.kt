package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.*
import com.yangchengwei.easytrip.place.amap.PlaceCandidate

@Composable
internal fun PlaceDetailEditor(
    candidate: PlaceCandidate,
    state: PlaceDetailEditState,
    availableTagNames: List<String>,
    onAction: (PlaceDetailPanelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val saving = state.isSaving
    if (state.showDiscardConfirmation) AlertDialog(
        onDismissRequest = { onAction(PlaceDetailPanelAction.ContinueEditing) },
        title = { Text("放弃本次修改？") },
        text = { Text("地点分类、收藏备注和标签的修改都将放弃。") },
        confirmButton = {
            TextButton({ onAction(PlaceDetailPanelAction.ConfirmDiscardEdit) },
                enabled = !saving, modifier = Modifier.testTag("place-detail-discard-confirm")) { Text("放弃修改") }
        },
        dismissButton = {
            TextButton({ onAction(PlaceDetailPanelAction.ContinueEditing) },
                modifier = Modifier.testTag("place-detail-continue-editing")) { Text("继续编辑") }
        },
    )
    Column(modifier.fillMaxWidth().background(editorPageColor)) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .testTag("place-detail-scroll-content").padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("编辑收藏地点", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    Text(candidate.name, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("place-detail-title").semantics { heading() })
                }
                TextButton({ onAction(PlaceDetailPanelAction.Dismiss) }, enabled = !saving,
                    modifier = Modifier.testTag("place-detail-dismiss")) { Text("关闭") }
            }
            Text(candidate.address.ifBlank { "地址暂不可用" }, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            EditorSection("地点分类", EditorSectionGlyph.NOTE,
                Modifier.testTag("place-detail-category-section"), summary = "单选") {
                PlaceCategorySelector(state.category, !saving,
                    { onAction(PlaceDetailPanelAction.CategoryChanged(it)) })
            }
            EditorSection("收藏备注", EditorSectionGlyph.NOTE, Modifier.testTag("place-detail-note-section")) {
                Text("仅用于这个收藏地点，不同步到行程备注", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(state.note, { onAction(PlaceDetailPanelAction.NoteChanged(it)) },
                    enabled = !saving, label = { Text("收藏备注（选填）") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).testTag("place-detail-note-input"),
                    shape = RoundedCornerShape(10.dp))
            }
            EditorSection("标签", EditorSectionGlyph.NOTE, Modifier.testTag("place-detail-tags-section"),
                summary = "${state.selectedTagNames.size}/8") {
                Text("可多选，也可以添加自定义标签", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val suggested = (listOf("亲子", "朋友推荐", "备选") + availableTagNames).distinct()
                // Flow layout preserves natural label widths and wraps under large text.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggested.forEach { tag ->
                        val selected = tag in state.selectedTagNames
                        FilterChip(selected, {
                            onAction(if (selected) PlaceDetailPanelAction.RemoveTag(tag)
                            else PlaceDetailPanelAction.AddPresetTag(tag))
                        }, label = { Text(if (selected) "$tag · 移除" else tag) },
                            enabled = !saving && (selected || state.selectedTagNames.size < 8),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("place-detail-preset-tag-$tag"))
                    }
                }
                state.selectedTagNames.filterNot { it in suggested }.forEach { tag ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(tag, Modifier.weight(1f))
                        CompactSecondaryButton({ onAction(PlaceDetailPanelAction.RemoveTag(tag)) },
                            enabled = !saving) { Text("移除") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(state.newTagInput, { onAction(PlaceDetailPanelAction.NewTagInputChanged(it)) },
                        enabled = !saving, label = { Text("新标签") },
                        modifier = Modifier.weight(1f).testTag("place-detail-tags-input"),
                        shape = RoundedCornerShape(10.dp))
                    OutlinedButton({ onAction(PlaceDetailPanelAction.AddTag) }, enabled = !saving,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.widthIn(min = 52.dp).heightIn(min = 48.dp).testTag("place-detail-add-tag")) { Text("添加") }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).testTag("place-detail-actions")) {
            HorizontalDivider(color = editorBorderColor)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("place-detail-save-error")) }
                CompactPrimaryButton({ onAction(PlaceDetailPanelAction.SaveEdit) }, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("place-detail-save")) { Text(if (saving) "保存中" else "保存") }
                CompactSecondaryButton({ onAction(PlaceDetailPanelAction.CancelEdit) }, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("place-detail-cancel")) { Text("取消") }
            }
        }
    }
}
