package com.yangchengwei.easytrip.trip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TripSettingsRoute(
    viewModel: TripSettingsViewModel,
    onBack: () -> Unit,
    onTripDeleted: () -> Unit = onBack,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnTripDeleted by rememberUpdatedState(onTripDeleted)
    val tripDeletion = state.tripDeletion
    val tripDeleteBusy = tripDeletion is TripDeletionUiState.Ready && tripDeletion.isDeleting
    val tripDeleteSyncFailed = tripDeletion is TripDeletionUiState.Ready && tripDeletion.confirmationSyncFailed
    val busy = state.dateRange.submitting || state.dayDeleteInProgress || tripDeleteBusy
    BackHandler(enabled = busy || tripDeleteSyncFailed) {}
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TripSettingsEffect.ReturnToTripList -> currentOnTripDeleted()
            }
        }
    }
    TripSettingsContent(
        state = state,
        onBack = { if (!busy) currentOnBack() },
        onRename = viewModel::rename,
        onTravelMode = viewModel::setTravelMode,
        onDateEndDraft = viewModel::updateDateEndDraft,
        onSubmitDateRange = viewModel::requestDateRangeChange,
        onCancelDateRange = viewModel::cancelDateRangeChange,
        onConfirmDateRange = viewModel::confirmDateRangeChange,
        onRetryDateRangeSync = viewModel::retryDateRangeSync,
        onRetryTripObservation = viewModel::retryTripObservation,
        onRequestDeleteDay = viewModel::requestDelete,
        onRetryDeleteDay = viewModel::retryDelete,
        onCancelDeleteDay = viewModel::cancelDelete,
        onConfirmDeleteDay = viewModel::confirmDelete,
        onRequestTripDeletion = viewModel::requestTripDeletion,
        onRetryTripDeletionImpact = viewModel::retryTripDeletionImpact,
        onCancelTripDeletion = viewModel::cancelTripDeletion,
        onConfirmTripDeletion = viewModel::confirmTripDeletion,
        onRetryTripDeletionSync = viewModel::retryTripDeletionSync,
    )
}
