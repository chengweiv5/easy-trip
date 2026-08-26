package com.yangchengwei.easytrip.place.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yangchengwei.easytrip.place.domain.SavedPlace

@Composable
fun PlaceDetailContent(
    place: SavedPlace,
    draft: PlaceDetailDraft?,
    saving: Boolean,
    error: String?,
    onNoteChange: (String) -> Unit,
    onTagsChange: (Set<String>) -> Unit,
    onToggleCollection: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onNewTagInputChange: (String) -> Unit = {},
    onAddTag: () -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    source: PlaceDetailSource = PlaceDetailSource.Search,
    onDismiss: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    PlaceDetailPanel(
        candidate = place.toCandidate(),
        savedPlace = place,
        editState = draft?.let {
            PlaceDetailEditState(place.id, it.note, it.tags, it.newTagInput, isSaving = saving, errorMessage = error)
        },
        source = source,
        collectionBusy = false,
        collectionError = null,
        onAction = { action ->
            when (action) {
                is PlaceDetailPanelAction.NoteChanged -> onNoteChange(action.value)
                is PlaceDetailPanelAction.NewTagInputChanged -> onNewTagInputChange(action.value)
                PlaceDetailPanelAction.AddTag -> onAddTag()
                is PlaceDetailPanelAction.AddPresetTag -> onTagsChange(draft?.tags.orEmpty() + action.name)
                is PlaceDetailPanelAction.RemoveTag -> onRemoveTag(action.name)
                PlaceDetailPanelAction.Dismiss,
                PlaceDetailPanelAction.CancelEdit -> onDismiss()
                PlaceDetailPanelAction.ToggleCollection -> onToggleCollection()
                PlaceDetailPanelAction.Delete -> onDelete()
                PlaceDetailPanelAction.SaveEdit -> onSave()
                PlaceDetailPanelAction.StartEdit -> Unit
            }
        },
        modifier = modifier,
    )
}

@Composable
fun PlaceDetailDialog(
    place: SavedPlace,
    draft: PlaceDetailDraft?,
    saving: Boolean,
    error: String?,
    source: PlaceDetailSource,
    onNoteChange: (String) -> Unit,
    onTagsChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onNewTagInputChange: (String) -> Unit = {},
    onAddTag: () -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        confirmButton = {},
        text = {
            PlaceDetailContent(
                place = place,
                draft = draft,
                saving = saving,
                error = error,
                source = source,
                onNoteChange = onNoteChange,
                onTagsChange = onTagsChange,
                onNewTagInputChange = onNewTagInputChange,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                onToggleCollection = {},
                onDismiss = onDismiss,
                onDelete = onDelete,
                onSave = onSave,
            )
        },
    )
}
