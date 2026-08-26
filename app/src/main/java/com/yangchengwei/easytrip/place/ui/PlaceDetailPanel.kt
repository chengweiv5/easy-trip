package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace


enum class PlaceDetailSource { Search, PlacePool }

sealed interface PlaceDetailPanelAction {
    data object Dismiss : PlaceDetailPanelAction
    data object ToggleCollection : PlaceDetailPanelAction
    data object StartEdit : PlaceDetailPanelAction
    data object Delete : PlaceDetailPanelAction
    data class NoteChanged(val value: String) : PlaceDetailPanelAction
    data class NewTagInputChanged(val value: String) : PlaceDetailPanelAction
    data object AddTag : PlaceDetailPanelAction
    data class RemoveTag(val name: String) : PlaceDetailPanelAction
    data object SaveEdit : PlaceDetailPanelAction
    data object CancelEdit : PlaceDetailPanelAction
}

@Composable
fun PlaceDetailPanel(
    candidate: PlaceCandidate,
    savedPlace: SavedPlace?,
    editState: PlaceDetailEditState?,
    source: PlaceDetailSource,
    collectionBusy: Boolean,
    collectionError: String?,
    onAction: (PlaceDetailPanelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val saving = editState?.isSaving == true
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                candidate.name,
                modifier = Modifier.testTag("place-detail-title").semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(
                onClick = { onAction(PlaceDetailPanelAction.Dismiss) },
                enabled = !saving,
                modifier = Modifier.testTag("place-detail-dismiss"),
            ) {
                Text("关闭", modifier = Modifier.semantics { role = Role.Button })
            }
        }
        Text(
            candidate.address.ifBlank { "地址暂不可用" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (editState == null) {
            Text(savedPlace?.note?.takeIf(String::isNotBlank) ?: "暂无备注")
            Text(savedPlace?.tags?.map { it.name }?.takeIf(List<String>::isNotEmpty)?.joinToString("、") ?: "暂无标签")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (source) {
                    PlaceDetailSource.Search -> {
                        CompactPrimaryButton(
                            onClick = { onAction(PlaceDetailPanelAction.ToggleCollection) },
                            enabled = !collectionBusy,
                            modifier = Modifier.weight(1f).semantics {
                                contentDescription = if (savedPlace == null) {
                                    "收藏${candidate.name}"
                                } else {
                                    "取消收藏${candidate.name}"
                                }
                            },
                        ) { Text(if (savedPlace == null) "收藏" else "取消收藏") }
                        if (savedPlace != null) {
                            CompactSecondaryButton(
                                onClick = { onAction(PlaceDetailPanelAction.StartEdit) },
                                modifier = Modifier.weight(1f),
                            ) { Text("编辑") }
                        }
                    }
                    PlaceDetailSource.PlacePool -> {
                        CompactSecondaryButton(
                            onClick = { onAction(PlaceDetailPanelAction.StartEdit) },
                            modifier = Modifier.weight(1f),
                        ) { Text("编辑") }
                        CompactSecondaryButton(
                            onClick = { onAction(PlaceDetailPanelAction.Delete) },
                            modifier = Modifier.weight(1f),
                        ) { Text("删除") }
                    }
                }
            }
            collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            if (source == PlaceDetailSource.Search) {
                CompactSecondaryButton(
                    onClick = { onAction(PlaceDetailPanelAction.ToggleCollection) },
                    enabled = !saving && !collectionBusy,
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "取消收藏${candidate.name}"
                    },
                ) { Text("取消收藏") }
            }
            OutlinedTextField(
                value = editState.note,
                onValueChange = { onAction(PlaceDetailPanelAction.NoteChanged(it)) },
                enabled = !saving,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth().testTag("place-detail-note-input"),
            )
            editState.selectedTagNames.forEach { tag ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tag)
                    CompactSecondaryButton(
                        onClick = { onAction(PlaceDetailPanelAction.RemoveTag(tag)) },
                        enabled = !saving,
                    ) { Text("移除") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editState.newTagInput,
                    onValueChange = { onAction(PlaceDetailPanelAction.NewTagInputChanged(it)) },
                    enabled = !saving,
                    label = { Text("新标签") },
                    modifier = Modifier.weight(1f).testTag("place-detail-tags-input"),
                )
                CompactSecondaryButton(
                    onClick = { onAction(PlaceDetailPanelAction.AddTag) },
                    enabled = !saving,
                ) { Text("添加") }
            }
            editState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (source == PlaceDetailSource.PlacePool) {
                    CompactSecondaryButton(
                        onClick = { onAction(PlaceDetailPanelAction.Delete) },
                        enabled = !saving,
                        modifier = Modifier.weight(1f).testTag("place-detail-delete"),
                    ) { Text("删除") }
                }
                CompactSecondaryButton(
                    onClick = { onAction(PlaceDetailPanelAction.CancelEdit) },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                ) { Text("取消") }
                CompactPrimaryButton(
                    onClick = { onAction(PlaceDetailPanelAction.SaveEdit) },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                ) { Text(if (saving) "保存中" else "保存") }
            }
        }
    }
}

internal fun SavedPlace.toCandidate() = PlaceCandidate(amapPoiId, name, address, point, null)
