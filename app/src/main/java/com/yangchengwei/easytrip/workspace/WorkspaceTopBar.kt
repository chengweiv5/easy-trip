package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

@Composable
internal fun WorkspaceTopBar(
    title: String,
    dateLabel: String?,
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = EasyTripTheme.sizes
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(sizes.workspaceTopBarHeight)
            .testTag("workspace-top-bar"),
        shape = RoundedCornerShape(EasyTripTheme.spacing.medium),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = EasyTripTheme.elevation.floating,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = EasyTripTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(sizes.workspacePrimaryTouchTarget)
                    .testTag("workspace-back")
                    .semantics { contentDescription = "返回" }
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                WorkspaceBackIcon(Modifier.size(sizes.workspaceIconSize))
            }
            Column(Modifier.weight(1f).padding(horizontal = EasyTripTheme.spacing.xSmall)) {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("workspace-trip-title"),
                )
                dateLabel?.let {
                    Text(
                        it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                Modifier
                    .size(sizes.workspacePrimaryTouchTarget)
                    .testTag("workspace-more")
                    .semantics { contentDescription = "更多" }
                    .clickable(role = Role.Button, onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                WorkspaceMoreIcon(Modifier.size(sizes.workspaceIconSize))
            }
        }
    }
}
