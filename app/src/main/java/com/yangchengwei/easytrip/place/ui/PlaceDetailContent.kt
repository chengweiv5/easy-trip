package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
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
    onCategoryChange: (com.yangchengwei.easytrip.place.domain.PlaceCategory) -> Unit = {},
    onConfirmDiscardEdit: () -> Unit = {},
    onContinueEditing: () -> Unit = {},
) {
    PlaceDetailPanel(
        candidate = place.toCandidate(),
        savedPlace = place,
        editState = draft?.let {
            PlaceDetailEditState(place.id, it.note, it.tags, it.newTagInput, isSaving = saving, errorMessage = error, category = it.category, original = it.original, showDiscardConfirmation = it.showDiscardConfirmation)
        },
        source = source,
        collectionBusy = false,
        collectionError = null,
        onAction = { action ->
            when (action) {
                is PlaceDetailPanelAction.CategoryChanged -> onCategoryChange(action.value)
                PlaceDetailPanelAction.ConfirmDiscardEdit -> onConfirmDiscardEdit()
                PlaceDetailPanelAction.ContinueEditing -> onContinueEditing()
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
                PlaceDetailPanelAction.StartEdit,
                PlaceDetailPanelAction.StartAddToItinerary -> Unit
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
    onCategoryChange: (com.yangchengwei.easytrip.place.domain.PlaceCategory) -> Unit = {},
    onConfirmDiscardEdit: () -> Unit = {},
    onContinueEditing: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        modifier = Modifier.imePadding(),
        properties = DialogProperties(decorFitsSystemWindows = false),
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
                onCategoryChange = onCategoryChange,
                onConfirmDiscardEdit = onConfirmDiscardEdit,
                onContinueEditing = onContinueEditing,
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
