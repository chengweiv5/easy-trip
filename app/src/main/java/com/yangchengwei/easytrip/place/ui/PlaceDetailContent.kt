package com.yangchengwei.easytrip.place.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    PlaceDetailPanel(
        candidate = place.toCandidate(),
        savedPlace = place,
        editState = PlaceDetailEditState(place.id, draft.note, draft.tags, isSaving = saving, errorMessage = error),
        source = PlaceDetailSource.Search,
        collectionBusy = false,
        collectionError = null,
        onAction = { action ->
            when (action) {
                is PlaceDetailPanelAction.NoteChanged -> onNoteChange(action.value)
                is PlaceDetailPanelAction.TagsChanged -> onTagsChange(action.value)
                PlaceDetailPanelAction.ToggleCollection -> onToggleCollection()
                PlaceDetailPanelAction.SaveEdit -> onSave()
                else -> Unit
            }
        },
        modifier = modifier,
    )
}
