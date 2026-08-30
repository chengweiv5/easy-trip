package com.yangchengwei.easytrip.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class EasyTripSpacing(
    val xSmall: Dp = 8.dp,
    val small: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 20.dp,
    val xLarge: Dp = 24.dp,
    val dialogContentHorizontal: Dp = 14.dp,
    val dialogContentVertical: Dp = 14.dp,
    val dialogSectionGap: Dp = 10.dp,
    val settingsGrid: Dp = 20.dp,
    val settingsSectionGap: Dp = 18.dp,
    val settingsCardGap: Dp = 10.dp,
    val workspaceOverlayGap: Dp = 20.dp,
    val workspaceControlGap: Dp = 4.dp,
)

@Immutable
data class EasyTripSizes(
    val buttonHeight: Dp = 48.dp,
    val pillHeight: Dp = 36.dp,
    val iconButtonSize: Dp = 48.dp,
    val iconSize: Dp = 22.dp,
    val topBarHeight: Dp = 62.dp,
    val dialogWidth: Dp = 350.dp,
    val dialogCompactWidth: Dp = 240.dp,
    val dialogMaxHeight: Dp = 640.dp,
    val dialogCornerRadius: Dp = 14.dp,
    val dialogFieldHeight: Dp = 54.dp,
    val settingsHeaderHeight: Dp = 62.dp,
    val settingsRowHeight: Dp = 58.dp,
    val settingsDayRowHeight: Dp = 48.dp,
    val settingsModeCardHeight: Dp = 62.dp,
    val settingsCardCornerRadius: Dp = 10.dp,
    val workspaceTopBarHeight: Dp = 52.dp,
    val workspaceSearchHeight: Dp = 46.dp,
    val workspacePrimaryTouchTarget: Dp = 44.dp,
    val workspaceDenseTouchTarget: Dp = 40.dp,
    val workspaceIconSize: Dp = 22.dp,
    val searchHeaderHeight: Dp = 46.dp,
    val searchHeaderCornerRadius: Dp = 23.dp,
    val searchHeaderHorizontalPadding: Dp = 14.dp,
    val searchHeaderBackTouchTarget: Dp = 48.dp,
)

@Immutable
data class EasyTripElevation(
    val card: Dp = 0.dp,
    val floating: Dp = 3.dp,
    val dialog: Dp = 6.dp,
)

internal val LocalEasyTripSpacing = staticCompositionLocalOf { EasyTripSpacing() }
internal val LocalEasyTripSizes = staticCompositionLocalOf { EasyTripSizes() }
internal val LocalEasyTripElevation = staticCompositionLocalOf { EasyTripElevation() }

object EasyTripTheme {
    val spacing: EasyTripSpacing
        @Composable @ReadOnlyComposable get() = LocalEasyTripSpacing.current
    val sizes: EasyTripSizes
        @Composable @ReadOnlyComposable get() = LocalEasyTripSizes.current
    val elevation: EasyTripElevation
        @Composable @ReadOnlyComposable get() = LocalEasyTripElevation.current
}
