package com.yangchengwei.easytrip.permission

import android.content.SharedPreferences
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

data class LocationPermissionUiState(
    val explanationVisible: Boolean = false,
    val permanentlyDenied: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
)

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

sealed interface WorkspaceEffect {
    data class RequestLocationPermission(val generation: Long) : WorkspaceEffect
    data class ShowCurrentLocation(val generation: Long) : WorkspaceEffect
    data class OpenApplicationSettings(
        val generation: Long,
        val workspaceId: String,
    ) : WorkspaceEffect
}

class LocationPermissionCoordinator(
    private val requestStore: LocationPermissionRequestStore,
) {
    private val mutableUiState = MutableStateFlow(LocationPermissionUiState())
    private val effectChannel = Channel<WorkspaceEffect>(Channel.BUFFERED)
    private var workspaceId: String? = null
    private var nextGeneration = 0L
    private var activeGeneration: Long? = null
    private var permissionLaunchRecordedGeneration: Long? = null
    private var permissionRequestInFlight = false
    private var locationEmittedGeneration: Long? = null
    private var settingsRecovery: SettingsRecovery? = null

    val uiState: StateFlow<LocationPermissionUiState> = mutableUiState.asStateFlow()
    val effectFlow = effectChannel.receiveAsFlow()

    fun attachWorkspace(workspaceId: String) {
        if (this.workspaceId != workspaceId) settingsRecovery = null
        this.workspaceId = workspaceId
    }

    fun detachWorkspace(workspaceId: String) {
        if (this.workspaceId != workspaceId) return
        this.workspaceId = null
        permissionRequestInFlight = false
        settingsRecovery = null
    }

    fun onLocateClick(snapshot: LocationPermissionSnapshot) {
        settingsRecovery = null
        val generation = newGeneration()
        if (snapshot.granted) {
            mutableUiState.value = LocationPermissionUiState()
            emitLocationOnce(generation)
        } else if (requestStore.hasRequested && !snapshot.shouldShowRationale) {
            mutableUiState.value = LocationPermissionUiState(permanentlyDenied = true)
        } else {
            mutableUiState.value = LocationPermissionUiState(explanationVisible = true)
        }
    }

    fun confirmExplanation() {
        val generation = activeGeneration ?: return
        if (!mutableUiState.value.explanationVisible || permissionRequestInFlight) return
        permissionRequestInFlight = true
        mutableUiState.value = LocationPermissionUiState(busy = true)
        effectChannel.trySend(WorkspaceEffect.RequestLocationPermission(generation))
    }

    fun dismissExplanation() {
        mutableUiState.value = mutableUiState.value.copy(explanationVisible = false)
    }

    fun onPermissionLaunchStarted(generation: Long) {
        if (generation != activeGeneration || permissionLaunchRecordedGeneration == generation) return
        permissionLaunchRecordedGeneration = generation
        requestStore.hasRequested = true
    }

    fun onPermissionLaunchFailed(generation: Long) {
        permissionRequestInFlight = false
        if (generation != activeGeneration) return
        mutableUiState.value = mutableUiState.value.copy(
            explanationVisible = true,
            busy = false,
            error = "无法打开系统权限请求，请重试",
        )
    }

    fun onPermissionResult(generation: Long, snapshot: LocationPermissionSnapshot) {
        permissionRequestInFlight = false
        if (generation != activeGeneration) return
        mutableUiState.value = when {
            snapshot.granted -> LocationPermissionUiState()
            requestStore.hasRequested && !snapshot.shouldShowRationale ->
                LocationPermissionUiState(permanentlyDenied = true)
            else -> LocationPermissionUiState()
        }
        if (snapshot.granted) emitLocationOnce(generation)
    }

    fun requestApplicationSettings() {
        val workspaceId = workspaceId ?: return
        if (!mutableUiState.value.permanentlyDenied) return
        val generation = newGeneration()
        settingsRecovery = SettingsRecovery(generation, workspaceId, launchStarted = false)
        mutableUiState.value = mutableUiState.value.copy(busy = true, error = null)
        effectChannel.trySend(WorkspaceEffect.OpenApplicationSettings(generation, workspaceId))
    }

    fun onSettingsLaunchStarted(generation: Long) {
        val recovery = settingsRecovery ?: return
        if (recovery.generation != generation || recovery.generation != activeGeneration || recovery.workspaceId != workspaceId) return
        settingsRecovery = recovery.copy(launchStarted = true)
        mutableUiState.value = mutableUiState.value.copy(busy = false)
    }

    fun onSettingsLaunchFailed(generation: Long) {
        val recovery = settingsRecovery ?: return
        if (recovery.generation != generation || recovery.generation != activeGeneration || recovery.workspaceId != workspaceId) return
        settingsRecovery = null
        mutableUiState.value = mutableUiState.value.copy(
            permanentlyDenied = true,
            busy = false,
            error = "无法打开应用设置，请重试",
        )
    }

    fun onWorkspaceResumed(workspaceId: String, snapshot: LocationPermissionSnapshot) {
        val recovery = settingsRecovery ?: return
        if (!recovery.launchStarted || recovery.generation != activeGeneration || recovery.workspaceId != workspaceId || this.workspaceId != workspaceId) return
        settingsRecovery = null
        mutableUiState.value = if (snapshot.granted) {
            LocationPermissionUiState()
        } else {
            LocationPermissionUiState(permanentlyDenied = true)
        }
        if (snapshot.granted) emitLocationOnce(recovery.generation)
    }

    private fun newGeneration(): Long {
        nextGeneration++
        activeGeneration = nextGeneration
        return nextGeneration
    }

    private fun emitLocationOnce(generation: Long) {
        if (locationEmittedGeneration == generation) return
        locationEmittedGeneration = generation
        effectChannel.trySend(WorkspaceEffect.ShowCurrentLocation(generation))
    }

    private data class SettingsRecovery(
        val generation: Long,
        val workspaceId: String,
        val launchStarted: Boolean,
    )

    companion object {
        const val HAS_REQUESTED_KEY = "location.permission.hasRequested"
    }
}
