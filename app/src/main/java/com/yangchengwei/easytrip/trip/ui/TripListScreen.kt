package com.yangchengwei.easytrip.trip.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog

@Composable
fun TripListScreen(
    viewModel: TripListViewModel,
    onCreateTrip: () -> Unit,
    onWorkspace: (String) -> Unit,
    onSettings: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.navigation.collect {
                when (it) {
                    is TripListNavigation.OpenWorkspace -> onWorkspace(it.tripId)
                    is TripListNavigation.OpenSettings -> onSettings(it.tripId)
                }
            }
        }
    }
    TripListContent(
        state = state,
        onAction = { action ->
            if (action == TripListAction.CreateTrip) onCreateTrip() else viewModel.onAction(action)
        },
    )
    state.deleteConfirmation?.let { confirmation ->
        ConfirmationDialog(
            model = confirmation,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
            confirmEnabled = !state.deleteInProgress,
            errorMessage = state.deleteError,
        )
    }
}
