package com.yangchengwei.easytrip.trip.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TripSettingsRoute(viewModel: TripSettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TripSettingsContent(
        state = state,
        onBack = onBack,
        onRename = viewModel::rename,
        onTravelMode = viewModel::setTravelMode,
        onDateDraft = viewModel::updateDateDraft,
        onSubmitDateRange = viewModel::requestDateRangeChange,
        onCancelDateRange = viewModel::cancelDateRangeChange,
        onConfirmDateRange = viewModel::confirmDateRangeChange,
        onRequestDeleteDay = viewModel::requestDelete,
        onCancelDeleteDay = viewModel::cancelDelete,
        onConfirmDeleteDay = viewModel::confirmDelete,
    )
}
