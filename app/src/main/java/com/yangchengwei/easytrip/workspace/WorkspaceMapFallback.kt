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
    onPrivacySettings: () -> Unit,
    onRetry: () -> Unit,
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
                Text("同意高德隐私政策后显示地图")
                TextButton(onPrivacySettings) { Text("地图授权") }
            }
            is WorkspaceMapState.Failed -> {
                Text(state.message)
                TextButton(onRetry, Modifier.testTag("workspace-map-retry")) { Text("重试地图") }
            }
            WorkspaceMapState.Ready -> Unit
        }
    }
}
