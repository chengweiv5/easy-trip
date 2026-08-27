package com.yangchengwei.easytrip.trip.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnOpenWorkspace by rememberUpdatedState(onOpenWorkspace)
    BackHandler {
        if (!state.isSubmitting) viewModel.onAction(CreateTripAction.Back)
    }
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    CreateTripEffect.NavigateBack -> currentOnBack()
                    is CreateTripEffect.OpenWorkspace -> currentOnOpenWorkspace(effect.tripId)
                }
            }
        }
    }
    CreateTripContent(state, viewModel::onAction, initialDateMillis = initialDateMillis)
}
