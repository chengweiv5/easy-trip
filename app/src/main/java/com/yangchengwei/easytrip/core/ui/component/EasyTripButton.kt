package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripBorder
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme

private enum class EasyTripButtonStyle { PRIMARY, SECONDARY, DANGER }

@Composable
fun EasyTripPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = EasyTripButton(onClick, modifier, enabled, EasyTripButtonStyle.PRIMARY, content)

@Composable
fun EasyTripSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = EasyTripButton(onClick, modifier, enabled, EasyTripButtonStyle.SECONDARY, content)

@Composable
fun EasyTripDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = EasyTripButton(onClick, modifier, enabled, EasyTripButtonStyle.DANGER, content)

@Composable
private fun EasyTripButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    style: EasyTripButtonStyle,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container = when (style) {
        EasyTripButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        EasyTripButtonStyle.SECONDARY, EasyTripButtonStyle.DANGER -> MaterialTheme.colorScheme.surface
    }
    val foreground = when (style) {
        EasyTripButtonStyle.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        EasyTripButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface
        EasyTripButtonStyle.DANGER -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier
            .defaultMinSize(
                minWidth = EasyTripTheme.sizes.buttonHeight,
                minHeight = EasyTripTheme.sizes.buttonHeight,
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) container else container.copy(alpha = 0.6f),
        contentColor = if (enabled) foreground else foreground.copy(alpha = 0.5f),
        border = when (style) {
            EasyTripButtonStyle.PRIMARY -> null
            EasyTripButtonStyle.SECONDARY -> BorderStroke(1.dp, EasyTripBorder)
            EasyTripButtonStyle.DANGER -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        },
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelMedium) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
