package com.yangchengwei.easytrip.permission

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.workspace.PermissionKind
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class LocationPermissionSnapshot(
    val granted: Boolean,
    val shouldShowRationale: Boolean,
) {
    companion object {
        fun <T> from(
            permissions: Set<T>,
            isGranted: (T) -> Boolean,
            shouldShowRationale: (T) -> Boolean,
        ) = LocationPermissionSnapshot(
            granted = permissions.any(isGranted),
            shouldShowRationale = permissions.any(shouldShowRationale),
        )
    }
}

interface LocationPermissionRequestStore {
    var hasRequested: Boolean
}

class InMemoryLocationPermissionRequestStore(
    override var hasRequested: Boolean = false,
) : LocationPermissionRequestStore

class SharedPreferencesLocationPermissionRequestStore(
    private val preferences: SharedPreferences,
) : LocationPermissionRequestStore {
    override var hasRequested: Boolean
        get() = preferences.getBoolean(LocationPermissionCoordinator.HAS_REQUESTED_KEY, false)
        set(value) {
            preferences.edit().putBoolean(LocationPermissionCoordinator.HAS_REQUESTED_KEY, value).apply()
        }
}

private class SavedStateLocationPermissionRequestStore(
    private val savedStateHandle: SavedStateHandle,
) : LocationPermissionRequestStore {
    override var hasRequested: Boolean
        get() = savedStateHandle[LocationPermissionCoordinator.HAS_REQUESTED_KEY] ?: false
        set(value) {
            savedStateHandle[LocationPermissionCoordinator.HAS_REQUESTED_KEY] = value
        }
}

sealed interface WorkspaceEffect {
    data object RequestLocationPermission : WorkspaceEffect
    data object OpenApplicationSettings : WorkspaceEffect
    data object ShowCurrentLocation : WorkspaceEffect
}

class LocationPermissionCoordinator(
    private val savedStateHandle: SavedStateHandle,
    private val requestStore: LocationPermissionRequestStore = SavedStateLocationPermissionRequestStore(savedStateHandle),
) {
    private val explanationState = MutableStateFlow<PermissionKind?>(null)
    private val effectChannel = Channel<WorkspaceEffect>(Channel.BUFFERED)

    val explanation: StateFlow<PermissionKind?> = explanationState.asStateFlow()
    val effectFlow = effectChannel.receiveAsFlow()
    val effects: Channel<WorkspaceEffect> get() = effectChannel

    fun onLocateClick(snapshot: LocationPermissionSnapshot) {
        if (snapshot.granted) {
            effectChannel.trySend(WorkspaceEffect.ShowCurrentLocation)
            return
        }
        explanationState.value = if (hasRequested && !snapshot.shouldShowRationale) {
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

    fun onPermissionResult(snapshot: LocationPermissionSnapshot) {
        if (snapshot.granted) {
            effectChannel.trySend(WorkspaceEffect.ShowCurrentLocation)
        } else if (hasRequested && !snapshot.shouldShowRationale) {
            explanationState.value = PermissionKind.DEVICE_LOCATION_SETTINGS
        }
    }

    private var hasRequested: Boolean
        get() = requestStore.hasRequested || savedStateHandle[HAS_REQUESTED_KEY] ?: false
        set(value) {
            requestStore.hasRequested = value
            savedStateHandle[HAS_REQUESTED_KEY] = value
        }

    companion object {
        const val HAS_REQUESTED_KEY = "location.permission.hasRequested"
    }
}
