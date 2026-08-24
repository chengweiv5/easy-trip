package com.yangchengwei.easytrip.permission

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.workspace.PermissionKind
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface WorkspaceEffect {
    data object RequestLocationPermission : WorkspaceEffect
    data object OpenApplicationSettings : WorkspaceEffect
}

class LocationPermissionCoordinator(
    private val savedStateHandle: SavedStateHandle,
) {
    private val explanationState = MutableStateFlow<PermissionKind?>(null)
    private val effectChannel = Channel<WorkspaceEffect>(Channel.BUFFERED)

    val explanation: StateFlow<PermissionKind?> = explanationState.asStateFlow()
    val effectFlow = effectChannel.receiveAsFlow()
    val effects: Channel<WorkspaceEffect> get() = effectChannel

    fun onLocateClick(isGranted: Boolean, shouldShowRationale: Boolean) {
        if (isGranted) return
        explanationState.value = if (hasRequested && !shouldShowRationale) {
            PermissionKind.DEVICE_LOCATION_SETTINGS
        } else {
            PermissionKind.DEVICE_LOCATION
        }
    }

    fun confirmExplanation() {
        val kind = explanationState.value ?: return
        explanationState.value = null
        when (kind) {
            PermissionKind.DEVICE_LOCATION -> {
                hasRequested = true
                effectChannel.trySend(WorkspaceEffect.RequestLocationPermission)
            }
            PermissionKind.DEVICE_LOCATION_SETTINGS -> effectChannel.trySend(WorkspaceEffect.OpenApplicationSettings)
            PermissionKind.MAP_SERVICE_CONSENT -> Unit
        }
    }

    fun dismissExplanation() {
        explanationState.value = null
    }

    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        if (!isGranted && hasRequested && !shouldShowRationale) {
            explanationState.value = PermissionKind.DEVICE_LOCATION_SETTINGS
        }
    }

    private var hasRequested: Boolean
        get() = savedStateHandle[HAS_REQUESTED_KEY] ?: false
        set(value) {
            savedStateHandle[HAS_REQUESTED_KEY] = value
        }

    companion object {
        const val HAS_REQUESTED_KEY = "location.permission.hasRequested"
    }
}
