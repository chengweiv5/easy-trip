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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceMapFallback(
    state: WorkspaceMapState,
    onOpenConsent: () -> Unit,
    onRetryMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateModifier = when (state) {
        WorkspaceMapState.Loading,
        is WorkspaceMapState.Failed,
        -> Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        else -> Modifier
    }
    Column(
        modifier.then(stateModifier).fillMaxSize().padding(24.dp).testTag("workspace-map-fallback"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            WorkspaceMapState.Loading -> {
                CircularProgressIndicator(
                    Modifier
                        .testTag("workspace-map-loading")
                        .semantics { contentDescription = "地图正在加载" },
                )
                Text("正在加载地图")
                Text("地点和行程仍可继续查看")
            }
            WorkspaceMapState.ConsentRequired -> {
                Text("地图服务未启用", Modifier.testTag("map-consent-required"))
                Text("本地地点与行程仍可使用")
                TextButton(onOpenConsent, Modifier.testTag("map-consent-open")) { Text("查看并授权") }
            }
            is WorkspaceMapState.Failed -> {
                Text("地图暂时无法加载", Modifier.testTag("map-load-failed"))
                Text("地点和行程仍可查看，请稍后重试")
                TextButton(onRetryMap, Modifier.testTag("map-retry")) { Text("重试") }
            }
            WorkspaceMapState.Ready -> Unit
        }
    }
}
