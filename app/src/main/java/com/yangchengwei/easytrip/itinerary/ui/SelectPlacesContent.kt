package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi

@Composable
fun SelectPlacesContent(
    rows: List<SavedPlaceRowUi>,
    state: AddToItineraryUiState,
    onTogglePlace: (String) -> Unit,
    onContinue: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("从地点池添加", style = MaterialTheme.typography.titleLarge)
        Text("可多选，地点将按选择顺序加入行程", style = MaterialTheme.typography.bodyMedium)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.place.id }) { row ->
                val isSelected = row.place.id in state.selectedPlaceIdSet
                Row(
                    Modifier.fillMaxWidth()
                        .testTag("select-place-${row.place.id}")
                        .semantics { selected = isSelected }
                        .clickable(
                            enabled = !state.isSubmitting && !state.isUndoing,
                            role = Role.Checkbox,
                        ) { onTogglePlace(row.place.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        enabled = !state.isSubmitting && !state.isUndoing,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(row.place.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (row.scheduled) "已安排 ${row.itineraryOccurrenceCount} 次，可重复添加" else row.place.address,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        Text("已选 ${state.selectedPlaceIds.size} 个")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactSecondaryButton(
                onClick = onClose,
                enabled = !state.isSubmitting,
                modifier = Modifier.weight(1f),
            ) { Text("取消") }
            CompactPrimaryButton(
                onClick = onContinue,
                enabled = state.canContinue,
                modifier = Modifier.weight(1f).testTag("select-places-continue"),
            ) { Text("继续") }
        }
    }
}
