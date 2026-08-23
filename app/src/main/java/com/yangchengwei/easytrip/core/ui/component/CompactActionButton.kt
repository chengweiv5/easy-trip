package com.yangchengwei.easytrip.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ActionStyle { PRIMARY, SECONDARY, DANGER }

@Composable
fun CompactPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = EasyTripPrimaryButton(onClick, modifier, enabled) { content() }

@Composable
fun CompactSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = EasyTripSecondaryButton(onClick, modifier, enabled) { content() }

@Composable
fun CompactDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = EasyTripDangerButton(onClick, modifier, enabled) { content() }

@Composable
fun CompactActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ActionStyle = ActionStyle.SECONDARY,
    content: @Composable () -> Unit,
) = when (style) {
    ActionStyle.PRIMARY -> EasyTripPrimaryButton(onClick, modifier, enabled) { content() }
    ActionStyle.SECONDARY -> EasyTripSecondaryButton(onClick, modifier, enabled) { content() }
    ActionStyle.DANGER -> EasyTripDangerButton(onClick, modifier, enabled) { content() }
}
