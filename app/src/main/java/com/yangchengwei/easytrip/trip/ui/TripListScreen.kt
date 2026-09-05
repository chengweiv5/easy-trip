package com.yangchengwei.easytrip.trip.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton

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
    TripDeletionDialog(
        deletion = state.deletion,
        onCancel = viewModel::cancelDelete,
        onRetryImpact = { viewModel.onAction(TripListAction.RetryDeleteImpact) },
        onConfirm = { viewModel.onAction(TripListAction.ConfirmDelete) },
        onRetrySync = { viewModel.onAction(TripListAction.RetryDeletionSync) },
    )
}
