package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ActionStyle { PRIMARY, SECONDARY, DANGER }

@Composable
fun CompactPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = CompactActionButton(onClick, modifier, enabled, ActionStyle.PRIMARY, content)

@Composable
fun CompactSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = CompactActionButton(onClick, modifier, enabled, ActionStyle.SECONDARY, content)

@Composable
fun CompactDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = CompactActionButton(onClick, modifier, enabled, ActionStyle.DANGER, content)

@Composable
fun CompactActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ActionStyle = ActionStyle.SECONDARY,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val container = when (style) {
        ActionStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        ActionStyle.SECONDARY, ActionStyle.DANGER -> Color.Transparent
    }
    val contentColor = when (style) {
        ActionStyle.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        ActionStyle.SECONDARY -> MaterialTheme.colorScheme.primary
        ActionStyle.DANGER -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier.defaultMinSize(minWidth = 48.dp, minHeight = 32.dp).clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = ripple(),
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = container,
            contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
        ) {
            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { content() }
        }
    }
}
