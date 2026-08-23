package com.yangchengwei.easytrip.trip.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun CreateTripRoute(
    onBack: () -> Unit,
    onOpenWorkspace: (String) -> Unit,
    viewModel: CreateTripViewModel,
    initialDateMillis: Long? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    CreateTripEffect.NavigateBack -> onBack()
                    is CreateTripEffect.OpenWorkspace -> onOpenWorkspace(effect.tripId)
                }
            }
        }
    }
    CreateTripContent(state, viewModel::onAction, initialDateMillis = initialDateMillis)
}
