package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceMapFallback(
    state: WorkspaceMapState,
    onOpenConsent: () -> Unit,
    onRetryMap: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp).testTag("workspace-map-fallback"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            WorkspaceMapState.Loading -> {
                CircularProgressIndicator(Modifier.testTag("workspace-map-loading"))
                Text("地图加载中")
            }
            WorkspaceMapState.ConsentRequired -> {
                Text("地图服务未启用", Modifier.testTag("map-consent-required"))
                Text("本地地点与行程仍可使用")
                TextButton(onOpenConsent, Modifier.testTag("map-consent-open")) { Text("查看并授权") }
            }
            is WorkspaceMapState.Failed -> {
                Text("地图加载失败", Modifier.testTag("map-load-failed"))
                Text(state.message)
                TextButton(onRetryMap, Modifier.testTag("map-retry")) { Text("重试地图") }
            }
            WorkspaceMapState.LocationPermanentlyDenied -> {
                Text("定位权限未开启", Modifier.testTag("location-permission-denied"))
                TextButton(onOpenLocationSettings, Modifier.testTag("location-open-settings")) { Text("前往系统设置") }
            }
            WorkspaceMapState.Ready -> Unit
        }
    }
}
