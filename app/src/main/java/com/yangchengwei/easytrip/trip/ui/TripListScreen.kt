package com.yangchengwei.easytrip.trip.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton

@Composable
fun TripListScreen(
    viewModel: TripListViewModel,
    onWorkspace: (String) -> Unit,
    onSettings: (String) -> Unit,
    initialDateMillis: Long? = null,
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
    TripListContent(state, viewModel::onAction)
    if (state.showCreateDialog) CreateTripDialog(state.create, viewModel::onCreateAction, initialDateMillis)
    val impact = state.pendingDeleteImpact
    state.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("删除旅行？") },
            text = { if (impact != null) Text("旅行日 ${impact.days}，地点 ${impact.places}，标签 ${impact.tags}，行程项 ${impact.itineraryItems}，路线段 ${impact.routeLegs}") },
            confirmButton = { CompactPrimaryButton(onClick = viewModel::confirmDelete) { Text("确认删除旅行") } },
            dismissButton = { CompactSecondaryButton(onClick = viewModel::cancelDelete) { Text("取消删除旅行") } },
        )
    }
}
