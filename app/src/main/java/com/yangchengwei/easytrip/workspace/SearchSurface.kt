package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.testTag("workspace-search-surface")) {
        MapEntrySurface(160.dp, "workspace-search-launcher", "搜索地点", onClick) {
            WorkspaceSearchIcon(Modifier.size(EasyTripTheme.sizes.workspaceIconSize))
            Text("搜索地点", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
