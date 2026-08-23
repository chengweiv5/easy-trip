package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
    PlacePoolContent(
        state = state,
        modifier = modifier,
        showSearch = showSearch,
        onSetQuery = viewModel::setQuery,
        onToggleTag = viewModel::toggleTag,
        onEdit = viewModel::edit,
        onDelete = viewModel::requestDelete,
        onToggleCollection = viewModel::toggleCollection,
        onDismissEdit = viewModel::dismissEdit,
        onUpdateDetails = viewModel::updateDetails,
        onDismissCollectionRemoval = viewModel::dismissCollectionRemoval,
        onConfirmCollectionRemoval = viewModel::confirmCollectionRemoval,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
    )
}

sealed interface PlacePoolAction {
    data class SetQuery(val value: String) : PlacePoolAction
    data class ToggleTag(val id: String) : PlacePoolAction
    data class Edit(val place: com.yangchengwei.easytrip.place.domain.SavedPlace) : PlacePoolAction
    data class Delete(val place: com.yangchengwei.easytrip.place.domain.SavedPlace) : PlacePoolAction
    data class ToggleCollection(val candidate: com.yangchengwei.easytrip.place.amap.PlaceCandidate) : PlacePoolAction
    data class UpdateDetails(val note: String, val tags: Set<String>) : PlacePoolAction
    data object ConfirmCollectionRemoval : PlacePoolAction
    data object ConfirmDelete : PlacePoolAction
    data object DismissDialogs : PlacePoolAction
}

@Composable
fun PlacePoolContent(
    state: PlacePoolUiState,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
    onAction: (PlacePoolAction) -> Unit,
    showDialogs: Boolean = true,
) {
    PlacePoolContent(
        state = state,
        modifier = modifier,
        showSearch = showSearch,
        onSetQuery = { onAction(PlacePoolAction.SetQuery(it)) },
        onToggleTag = { onAction(PlacePoolAction.ToggleTag(it)) },
        onEdit = { onAction(PlacePoolAction.Edit(it)) },
        onDelete = { onAction(PlacePoolAction.Delete(it)) },
        onToggleCollection = { onAction(PlacePoolAction.ToggleCollection(it)) },
        onDismissEdit = { onAction(PlacePoolAction.DismissDialogs) },
        onUpdateDetails = { note, tags -> onAction(PlacePoolAction.UpdateDetails(note, tags)) },
        onDismissCollectionRemoval = { onAction(PlacePoolAction.DismissDialogs) },
        onConfirmCollectionRemoval = { onAction(PlacePoolAction.ConfirmCollectionRemoval) },
        onDismissDelete = { onAction(PlacePoolAction.DismissDialogs) },
        onConfirmDelete = { onAction(PlacePoolAction.ConfirmDelete) },
        showDialogs = showDialogs,
    )
}

@Composable
fun PlacePoolContent(
    state: PlacePoolUiState,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
    onSetQuery: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onEdit: (com.yangchengwei.easytrip.place.domain.SavedPlace) -> Unit,
    onDelete: (com.yangchengwei.easytrip.place.domain.SavedPlace) -> Unit,
    onToggleCollection: (com.yangchengwei.easytrip.place.amap.PlaceCandidate) -> Unit,
    onDismissEdit: () -> Unit,
    onUpdateDetails: (String, Set<String>) -> Unit,
    onDismissCollectionRemoval: () -> Unit,
    onConfirmCollectionRemoval: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    showDialogs: Boolean = true,
) {
    Column(modifier.padding(16.dp)) {
        if (showSearch) PlaceSearchField(state.search.query, onSetQuery)
        SavedPlacesContent(
            places = state.search.savedPlaces,
            tags = state.tags,
            selectedTagIds = state.selectedTagIds,
            onToggleTag = onToggleTag,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showSearch) {
            PlaceSearchResults(
                state = state.search,
                savedPoiIds = state.savedPoiIds,
                onSelect = {},
                onToggleCollection = onToggleCollection,
                modifier = Modifier.fillMaxWidth(),
                collectionBusyPoiIds = state.collectionBusyPoiIds,
            )
        }
    }
    state.collectionError?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp)) }
    if (showDialogs) {
        state.editing?.let { EditSavedPlaceDialog(it, onDismissEdit, onUpdateDetails) }
        state.pendingCollectionRemoval?.let { pending ->
            AlertDialog(
                onDismissRequest = onDismissCollectionRemoval,
                title = { Text("取消收藏 ${pending.place.name}？") },
                text = { Text("将同时删除 ${pending.usageCount} 次行程安排及受影响路线。") },
                confirmButton = { TextButton(onConfirmCollectionRemoval) { Text("确认取消收藏") } },
                dismissButton = { TextButton(onDismissCollectionRemoval) { Text("取消") } },
            )
        }
        state.deleting?.let { place ->
            AlertDialog(
                onDismissRequest = onDismissDelete,
                title = { Text("删除 ${place.name}？") },
                text = { Text("将同时删除 ${state.deletionUsageCount} 次行程安排及受影响路线。") },
                confirmButton = { TextButton(onConfirmDelete) { Text("确认删除地点") } },
                dismissButton = { TextButton(onDismissDelete) { Text("取消") } },
            )
        }
    }
}
