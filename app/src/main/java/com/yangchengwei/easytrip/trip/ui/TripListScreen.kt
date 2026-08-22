package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton as Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

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
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = viewModel::showCreate) { Text("创建旅行") } }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("我的旅行", style = MaterialTheme.typography.headlineMedium)
            if (state.trips.isEmpty()) Text("还没有旅行", Modifier.padding(top = 24.dp))
            LazyColumn {
                items(state.trips, key = { it.id }) { trip ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.openWorkspace(trip.id) }.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.clickable { viewModel.openWorkspace(trip.id) }) { Text(trip.name); Text("${trip.dayCount} 天") }
                        Row {
                            TextButton(
                                onClick = { viewModel.openSettings(trip.id) },
                                modifier = Modifier.testTag("trip-settings-${trip.id}").semantics { contentDescription = "设置 ${trip.name}" },
                            ) { Text("设置") }
                            TextButton(
                                onClick = { viewModel.requestDelete(trip) },
                                modifier = Modifier.testTag("trip-delete-${trip.id}").semantics { contentDescription = "删除 ${trip.name}" },
                            ) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
    if (state.showCreateDialog) CreateTripDialog(state, viewModel, initialDateMillis)
    val impact = state.pendingDeleteImpact
    state.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("删除旅行？") },
            text = { if (impact != null) Text("旅行日 ${impact.days}，地点 ${impact.places}，标签 ${impact.tags}，行程项 ${impact.itineraryItems}，路线段 ${impact.routeLegs}") },
            confirmButton = { Button(onClick = viewModel::confirmDelete) { Text("确认删除旅行") } },
            dismissButton = { Button(onClick = viewModel::cancelDelete) { Text("取消删除旅行") } },
        )
    }
}
