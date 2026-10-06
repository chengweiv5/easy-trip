package com.yangchengwei.easytrip

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.hasDeviceLocationPermission
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import com.yangchengwei.easytrip.permission.*

/** Reuses the workspace's permission state machine without centering its map or leaving search. */
@Composable
internal fun rememberSearchLocationPermissionRequest(
    requestStore: LocationPermissionRequestStore,
    snapshot: (() -> LocationPermissionSnapshot)?,
    permissionLauncher: ((Array<String>, (LocationPermissionSnapshot) -> Unit) -> Result<Unit>)?,
    settingsLauncher: (() -> Result<Unit>)?,
    onGranted: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val coordinator = remember(requestStore) { LocationPermissionCoordinator(requestStore) }
    val granted by rememberUpdatedState(onGranted)
    val readSnapshot by rememberUpdatedState {
        snapshot?.invoke() ?: LocationPermissionSnapshot(
            hasDeviceLocationPermission(context),
            (context as? Activity)?.let { activity ->
                listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }
            } == true,
        )
    }
    var bridge by remember { mutableStateOf<LocationPermissionLaunchBridge?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        bridge?.onResult(readSnapshot())
    }
    DisposableEffect(coordinator, lifecycle) {
        coordinator.attachWorkspace("search")
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) coordinator.onWorkspaceResumed("search", readSnapshot())
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            bridge?.invalidate()
            coordinator.detachWorkspace("search")
        }
    }
    LaunchedEffect(coordinator) {
        coordinator.effectFlow.collect { effect ->
            when (effect) {
                is WorkspaceEffect.ShowCurrentLocation -> granted()
                is WorkspaceEffect.RequestLocationPermission -> {
                    bridge?.invalidate()
                    val active = LocationPermissionLaunchBridge(effect.generation, coordinator)
                    bridge = active
                    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    active.onLaunchFinished(
                        permissionLauncher?.invoke(permissions, active::onResult)
                            ?: runCatching { launcher.launch(permissions) },
                    )
                }
                is WorkspaceEffect.OpenApplicationSettings -> dispatchApplicationSettingsRequest(
                    effect.generation,
                    launcher = {
                        settingsLauncher?.invoke() ?: runCatching {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        }
                    },
                    coordinator = coordinator,
                )
            }
        }
    }
    val state by coordinator.uiState.collectAsStateWithLifecycle()
    if (state.prompt != LocationPermissionPrompt.NONE) {
        val settings = state.prompt == LocationPermissionPrompt.SETTINGS
        fun dismiss() { if (settings) coordinator.dismissSettings() else coordinator.dismissExplanation() }
        AlertDialog(
            onDismissRequest = ::dismiss,
            title = { Text("需要定位权限") },
            text = { Text(state.error ?: if (settings) "请在系统设置中允许定位，然后返回继续搜索当前城市。" else "定位用于搜索当前城市、显示距我的距离及地图上的我的位置，不会持续后台定位。") },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = { if (settings) coordinator.requestApplicationSettings() else coordinator.confirmExplanation() },
                ) { Text(if (settings) "打开设置" else "允许定位") }
            },
            dismissButton = { TextButton(onClick = ::dismiss) { Text("暂不") } },
        )
    }
    return { coordinator.onLocateClick(readSnapshot()) }
}
