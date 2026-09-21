package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun WorkspaceMoreMenu(
    onOpenSettings: () -> Unit,
    onOpenConsent: () -> Unit,
    onBackToTrips: () -> Unit,
    modifier: Modifier = Modifier,
    onShareItinerary: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .width(240.dp)
            .heightIn(max = 260.dp)
            .testTag("more-menu-panel"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            MoreMenuItem(
                tag = "more-menu-settings",
                title = "旅行设置",
                description = "修改名称、日期和旅行日",
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                onClick = onOpenSettings,
            )
            MoreMenuItem(
                tag = "more-menu-share",
                title = "分享行程长图",
                description = "每日地图、交通与备注",
                icon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                onClick = onShareItinerary,
            )
            MoreMenuItem(
                tag = "more-menu-consent",
                title = "地图授权",
                description = "管理高德地图权限",
                icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                onClick = onOpenConsent,
            )
            MoreMenuItem(
                tag = "more-menu-back-to-trips",
                title = "返回我的旅行",
                description = "回到旅行列表",
                icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                onClick = onBackToTrips,
            )
        }
    }
}

@Composable
private fun MoreMenuItem(
    tag: String,
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(7.dp),
            ) { icon() }
        }
        Column(Modifier.padding(start = 10.dp).fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
