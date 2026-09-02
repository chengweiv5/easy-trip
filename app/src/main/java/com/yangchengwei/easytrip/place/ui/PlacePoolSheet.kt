package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePoolSheet(
    viewModel: PlacePoolViewModel,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
    onSearch: () -> Unit = {},
    onStartAdd: (() -> Unit)? = null,
    onStartAddSingle: ((String) -> Unit)? = null,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    PlacePoolContent(
        state = state,
        modifier = modifier,
        showSearch = showSearch,
        onSearch = onSearch,
        onOpenDetail = viewModel::openDetail,
        onDismissDetail = { viewModel.dispatch(PlacePoolAction.DismissDetail) },
        onSetQuery = viewModel::setQuery,
        onToggleTag = viewModel::toggleTag,
        onEdit = viewModel::edit,
        onDelete = viewModel::requestDelete,
        onToggleCollection = viewModel::toggleCollection,
        onDismissEdit = viewModel::dismissEdit,
        onUpdateDraft = viewModel::updateDetailDraft,
        onUpdateNewTagInput = viewModel::updateNewTagInput,
        onAddTag = viewModel::addNewTag,
        onRemoveTag = viewModel::removeTag,
        onUpdateDetails = viewModel::updateDetails,
        onDismissCollectionRemoval = viewModel::dismissCollectionRemoval,
        onConfirmCollectionRemoval = viewModel::confirmCollectionRemoval,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onStartAdd = onStartAdd,
        onStartAddSingle = onStartAddSingle,
    )
}

sealed interface PlacePoolAction {
    data class SetQuery(val value: String) : PlacePoolAction
    data class ToggleTag(val id: String) : PlacePoolAction
    data class OpenDetail(val placeId: String) : PlacePoolAction
    data object DismissDetail : PlacePoolAction
    data class Edit(val place: com.yangchengwei.easytrip.place.domain.SavedPlace) : PlacePoolAction
    data class Delete(val place: com.yangchengwei.easytrip.place.domain.SavedPlace) : PlacePoolAction
    data class ToggleCollection(val candidate: com.yangchengwei.easytrip.place.amap.PlaceCandidate) : PlacePoolAction
    data class UpdateDraft(val note: String, val tags: Set<String>) : PlacePoolAction
    data class UpdateNewTagInput(val value: String) : PlacePoolAction
    data object AddTag : PlacePoolAction
    data class RemoveTag(val name: String) : PlacePoolAction
    data class UpdateDetails(val note: String, val tags: Set<String>) : PlacePoolAction
    data object StartAddToItinerary : PlacePoolAction
    data class StartAddSingle(val placeId: String) : PlacePoolAction
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
    onSearch: () -> Unit = {},
    showDialogs: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    schedulesByPlaceId: Map<String, PlaceScheduleSummaryUi> = emptyMap(),
) {
    PlacePoolContent(
        state = state,
        modifier = modifier,
        showSearch = showSearch,
        onSearch = onSearch,
        onOpenDetail = { onAction(PlacePoolAction.OpenDetail(it)) },
        onDismissDetail = { onAction(PlacePoolAction.DismissDetail) },
        onSetQuery = { onAction(PlacePoolAction.SetQuery(it)) },
        onToggleTag = { onAction(PlacePoolAction.ToggleTag(it)) },
        onEdit = { onAction(PlacePoolAction.Edit(it)) },
        onDelete = { onAction(PlacePoolAction.Delete(it)) },
        onToggleCollection = { onAction(PlacePoolAction.ToggleCollection(it)) },
        onDismissEdit = { onAction(PlacePoolAction.DismissDialogs) },
        onUpdateDraft = { note, tags -> onAction(PlacePoolAction.UpdateDraft(note, tags)) },
        onUpdateNewTagInput = { onAction(PlacePoolAction.UpdateNewTagInput(it)) },
        onAddTag = { onAction(PlacePoolAction.AddTag) },
        onRemoveTag = { onAction(PlacePoolAction.RemoveTag(it)) },
        onUpdateDetails = { note, tags -> onAction(PlacePoolAction.UpdateDetails(note, tags)) },
        onDismissCollectionRemoval = { onAction(PlacePoolAction.DismissDialogs) },
        onConfirmCollectionRemoval = { onAction(PlacePoolAction.ConfirmCollectionRemoval) },
        onDismissDelete = { onAction(PlacePoolAction.DismissDialogs) },
        onConfirmDelete = { onAction(PlacePoolAction.ConfirmDelete) },
        onStartAdd = { onAction(PlacePoolAction.StartAddToItinerary) },
        onStartAddSingle = { onAction(PlacePoolAction.StartAddSingle(it)) },
        schedulesByPlaceId = schedulesByPlaceId,
        showDialogs = showDialogs,
        contentPadding = contentPadding,
    )
}

@Composable
fun PlacePoolContent(
    state: PlacePoolUiState,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
    onSearch: () -> Unit = {},
    onOpenDetail: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onSetQuery: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onEdit: (com.yangchengwei.easytrip.place.domain.SavedPlace) -> Unit,
    onDelete: (com.yangchengwei.easytrip.place.domain.SavedPlace) -> Unit,
    onToggleCollection: (com.yangchengwei.easytrip.place.amap.PlaceCandidate) -> Unit,
    onDismissEdit: () -> Unit,
    onUpdateDraft: (String, Set<String>) -> Unit,
    onUpdateNewTagInput: (String) -> Unit = {},
    onAddTag: () -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onUpdateDetails: (String, Set<String>) -> Unit,
    onDismissCollectionRemoval: () -> Unit,
    onConfirmCollectionRemoval: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onStartAdd: (() -> Unit)?,
    onStartAddSingle: ((String) -> Unit)?,
    schedulesByPlaceId: Map<String, PlaceScheduleSummaryUi> = emptyMap(),
    showDialogs: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    Column(modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showSearch) PlaceSearchField(state.search.query, onSetQuery)
        if (state.rows.isEmpty() && !showSearch) {
            com.yangchengwei.easytrip.core.ui.component.EmptyState(
                title = "还没有收藏地点",
                message = "先搜索想去的地点，收藏后再安排到每天的行程。",
                emptyIllustration = com.yangchengwei.easytrip.core.ui.component.EmptyIllustration.Places,
                verticalPadding = 16.dp,
                action = {
                    com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton(onSearch) { Text("搜索地点") }
                },
            )
        } else {
            if (!showSearch) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已收藏 ${placePoolCollectionTotal(state)} 个", modifier = Modifier.testTag("place-pool-collection-total"))
                    Text("已排入 · 仅收藏", modifier = Modifier.testTag("place-pool-marker-legend"))
                    onStartAdd?.let { startAdd ->
                        EasyTripSecondaryButton(
                            onClick = startAdd,
                            modifier = Modifier.testTag("start-add-to-itinerary"),
                        ) { Text("添加到行程") }
                    }
                }
            } else if (onStartAdd != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EasyTripSecondaryButton(
                        onClick = onStartAdd,
                        modifier = Modifier.testTag("start-add-to-itinerary"),
                    ) { Text("添加到行程") }
                }
            }
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxWidth().weight(1f).testTag("workspace-place-list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                if (state.tags.isNotEmpty()) item {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("place-pool-tags"),
                    ) {
                        state.tags.forEach { tag ->
                            com.yangchengwei.easytrip.core.ui.component.SelectablePill(
                                selected = tag.id in state.selectedTagIds,
                                onClick = { onToggleTag(tag.id) },
                                label = { Text(tag.name) },
                                modifier = Modifier.testTag("tag-${tag.id}"),
                            )
                        }
                    }
                }
                items(state.rows.size, key = { state.rows[it].place.id }) { index ->
                    val row = state.rows[index]
                    SavedPlaceRow(
                        place = row,
                        onQuickAdd = onStartAddSingle?.let { callback -> { callback(row.place.id) } },
                        onOpenDetail = { onOpenDetail(row.place.id) },
                        onEdit = { onEdit(row.place) },
                        onDelete = { onDelete(row.place) },
                    )
                }
            }
        }
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
    if (showDialogs) state.selectedDetailPlace?.let { place ->
            AlertDialog(
                onDismissRequest = { if (!state.detailSaving) onDismissDetail() },
                confirmButton = {},
                text = {
                    PlaceDetailPanel(
                        candidate = place.toCandidate(),
                        savedPlace = place,
                        editState = null,
                        source = PlaceDetailSource.PlacePool,
                        collectionBusy = false,
                        collectionError = null,
                        schedule = schedulesByPlaceId[place.id]
                            ?: PlaceScheduleSummaryUi(isKnown = false),
                        canStartAddToItinerary = onStartAddSingle != null,
                        onAction = { action ->
                            when (action) {
                                PlaceDetailPanelAction.Dismiss -> onDismissDetail()
                                PlaceDetailPanelAction.StartEdit -> onEdit(place)
                                PlaceDetailPanelAction.StartAddToItinerary -> onStartAddSingle?.invoke(place.id)
                                PlaceDetailPanelAction.Delete -> onDelete(place)
                                else -> Unit
                            }
                        },
                    )
                },
            )
        }
    if (showDialogs) {
        state.editing?.let { place ->
            val draft = state.detailDraft ?: return@let
            AlertDialog(
                onDismissRequest = { if (!state.detailSaving) onDismissEdit() },
                confirmButton = {},
                text = {
                    PlaceDetailPanel(
                        candidate = place.toCandidate(),
                        savedPlace = place,
                        editState = PlaceDetailEditState(
                            placeId = draft.placeId,
                            note = draft.note,
                            selectedTagNames = draft.tags,
                            newTagInput = draft.newTagInput,
                            isSaving = state.detailSaving,
                            errorMessage = state.detailSaveError,
                        ),
                        source = PlaceDetailSource.PlacePool,
                        collectionBusy = false,
                        collectionError = null,
                        availableTagNames = state.tags.map { it.name },
                        onAction = { action ->
                            when (action) {
                                PlaceDetailPanelAction.Dismiss,
                                PlaceDetailPanelAction.CancelEdit -> onDismissEdit()
                                is PlaceDetailPanelAction.NoteChanged -> onUpdateDraft(action.value, draft.tags)
                                is PlaceDetailPanelAction.NewTagInputChanged -> onUpdateNewTagInput(action.value)
                                PlaceDetailPanelAction.AddTag -> onAddTag()
                                is PlaceDetailPanelAction.AddPresetTag -> onUpdateDraft(draft.note, draft.tags + action.name)
                                is PlaceDetailPanelAction.RemoveTag -> onRemoveTag(action.name)
                                PlaceDetailPanelAction.Delete -> onDelete(place)
                                PlaceDetailPanelAction.SaveEdit -> onUpdateDetails(draft.note, draft.tags)
                                PlaceDetailPanelAction.StartEdit,
                                PlaceDetailPanelAction.StartAddToItinerary,
                                PlaceDetailPanelAction.ToggleCollection -> Unit
                            }
                        },
                    )
                },
            )
        }
        state.pendingCollectionRemoval?.let { pending ->
            val busy = pending.candidate.poiId in state.collectionBusyPoiIds
            AlertDialog(
                onDismissRequest = { if (!busy) onDismissCollectionRemoval() },
                title = { Text("取消收藏 ${pending.place.name}？") },
                text = { Text("将同时删除 ${pending.impact.itineraryItemCount} 次行程安排和 ${pending.impact.routeLegCount} 段路线。") },
                confirmButton = { TextButton(onConfirmCollectionRemoval, enabled = !busy) { Text(if (busy) "取消中…" else "确认取消收藏") } },
                dismissButton = { TextButton(onDismissCollectionRemoval, enabled = !busy) { Text("取消") } },
            )
        }
        state.deleting?.let { place ->
            AlertDialog(
                onDismissRequest = { if (!state.deletionBusy) onDismissDelete() },
                title = { Text("删除 ${place.name}？") },
                text = {
                    Column {
                        state.deletionImpact?.let { impact ->
                            Text("将同时删除 ${impact.itineraryItemCount} 次行程安排和 ${impact.routeLegCount} 段路线。")
                        }
                        state.deletionError?.let { Text(it) }
                    }
                },
                confirmButton = { TextButton(onConfirmDelete, enabled = !state.deletionBusy) { Text(if (state.deletionBusy) "删除中…" else "确认删除地点") } },
                dismissButton = { TextButton(onDismissDelete, enabled = !state.deletionBusy) { Text("取消") } },
            )
        }
    }
}
