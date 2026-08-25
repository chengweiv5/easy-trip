package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().testTag("workspace-search-surface")) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(EasyTripTheme.sizes.workspaceSearchHeight).testTag("workspace-search-launcher")
                .semantics { contentDescription = "搜索地点" }.clickable(role = Role.Button, onClick = onClick),
            shape = RoundedCornerShape(23.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = EasyTripTheme.elevation.floating,
        ) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                WorkspaceSearchIcon(Modifier.size(EasyTripTheme.sizes.workspaceIconSize))
                Text("搜索餐厅、景点或地址", Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
