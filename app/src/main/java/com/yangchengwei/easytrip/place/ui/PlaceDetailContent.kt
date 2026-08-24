package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.place.domain.SavedPlace

@Composable
fun PlaceDetailContent(
    place: SavedPlace,
    draft: PlaceDetailDraft,
    saving: Boolean,
    error: String?,
    onNoteChange: (String) -> Unit,
    onTagsChange: (Set<String>) -> Unit,
    onToggleCollection: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(place.name, modifier = Modifier.testTag("place-detail-title"), style = MaterialTheme.typography.titleLarge)
        Text(place.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = draft.note,
            onValueChange = onNoteChange,
            label = { Text("备注") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.tags.joinToString(", "),
            onValueChange = { value -> onTagsChange(value.split(',').map(String::trim).filter(String::isNotEmpty).toSet()) },
            label = { Text("标签（逗号分隔）") },
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactSecondaryButton(onToggleCollection, enabled = !saving, modifier = Modifier.weight(1f)) { Text("取消收藏") }
            CompactPrimaryButton(onSave, enabled = !saving, modifier = Modifier.weight(1f)) { Text(if (saving) "保存中" else "保存") }
        }
    }
}
