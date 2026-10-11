package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

/** Preserve the 32dp design at normal text size; both launchers grow together for large text. */
@Composable
internal fun workspaceMapEntryHeight(): Dp = with(LocalDensity.current) { maxOf(32.dp, 20.sp.toDp() + 12.dp) }

@Composable
internal fun MapEntrySurface(
    width: Dp,
    tag: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val visualHeight = workspaceMapEntryHeight()
    Box(
        modifier.width(width).height(maxOf(48.dp, visualHeight)).testTag(tag)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth().height(visualHeight).testTag("$tag-surface"),
            shape = RoundedCornerShape(16.dp),
            color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation = EasyTripTheme.elevation.floating,
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (primary) Arrangement.Center else Arrangement.Start,
                content = content,
            )
        }
    }
}

@Composable
internal fun MapAssistantEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    MapEntrySurface(124.dp, "assistant-entry", "助手", onClick, modifier, primary = true) {
        val color = MaterialTheme.colorScheme.onPrimary
        Canvas(Modifier.size(16.dp)) {
            fun sparkle(x: Float, y: Float, radius: Float) {
                drawPath(Path().apply {
                    moveTo(size.width * x, size.height * (y - radius))
                    lineTo(size.width * (x + radius * .32f), size.height * (y - radius * .32f))
                    lineTo(size.width * (x + radius), size.height * y)
                    lineTo(size.width * (x + radius * .32f), size.height * (y + radius * .32f))
                    lineTo(size.width * x, size.height * (y + radius))
                    lineTo(size.width * (x - radius * .32f), size.height * (y + radius * .32f))
                    lineTo(size.width * (x - radius), size.height * y)
                    lineTo(size.width * (x - radius * .32f), size.height * (y - radius * .32f))
                    close()
                }, color)
            }
            sparkle(.38f, .62f, .36f)
            sparkle(.79f, .22f, .18f)
        }
        Spacer(Modifier.width(6.dp))
        Text("助手", style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
