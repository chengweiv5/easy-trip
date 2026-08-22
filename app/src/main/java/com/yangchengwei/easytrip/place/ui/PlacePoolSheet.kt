package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePoolSheet(
    viewModel: PlacePoolViewModel,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    Column(modifier.padding(16.dp)) {
        Text("地点池", style = MaterialTheme.typography.headlineSmall)
        if (showSearch) PlaceSearchField(state.search.query, viewModel::setQuery)
        SavedPlacesContent(
            places = state.search.savedPlaces,
            tags = state.tags,
            selectedTagIds = state.selectedTagIds,
            onToggleTag = viewModel::toggleTag,
            onEdit = viewModel::edit,
            onDelete = viewModel::requestDelete,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showSearch) {
            PlaceSearchResults(
                state = state.search,
                savedPoiIds = state.savedPoiIds,
                onSelect = {},
                onSave = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    state.editing?.let { EditSavedPlaceDialog(it, viewModel::dismissEdit, viewModel::updateDetails) }
    state.deleting?.let { place ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("删除 ${place.name}？") },
            text = { Text("将同时删除 ${state.deletionUsageCount} 次行程安排及受影响路线。") },
            confirmButton = { TextButton(viewModel::confirmDelete) { Text("确认删除地点") } },
            dismissButton = { TextButton(viewModel::dismissDelete) { Text("取消") } },
        )
    }
}
