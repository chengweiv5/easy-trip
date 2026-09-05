package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi
import com.yangchengwei.easytrip.workspace.workspacePlaceDetailSheetHeight

@Composable
fun WorkspacePlaceDetailSheet(
    state: PlacePoolUiState,
    schedule: PlaceScheduleSummaryUi,
    onAction: (PlaceDetailPanelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val place = state.editing ?: state.selectedDetailPlace ?: return
    val editState = state.detailDraft?.takeIf { state.editing?.id == it.placeId }?.let { draft ->
        PlaceDetailEditState(
            placeId = draft.placeId,
            note = draft.note,
            selectedTagNames = draft.tags,
            newTagInput = draft.newTagInput,
            isSaving = state.detailSaving,
            errorMessage = state.detailSaveError,
        )
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("place-detail-overlay-host"),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(workspacePlaceDetailSheetHeight(maxHeight))
                .testTag("place-detail-bottom-sheet"),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            PlaceDetailPanel(
                candidate = place.toCandidate(),
                savedPlace = place,
                editState = editState,
                source = PlaceDetailSource.PlacePool,
                collectionBusy = false,
                collectionError = null,
                availableTagNames = state.tags.map { it.name },
                schedule = schedule,
                onAction = onAction,
            )
        }
    }
}
