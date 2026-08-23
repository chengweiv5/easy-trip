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
)

@Immutable
data class EasyTripSizes(
    val buttonHeight: Dp = 48.dp,
    val pillHeight: Dp = 36.dp,
    val iconButtonSize: Dp = 48.dp,
    val iconSize: Dp = 22.dp,
    val topBarHeight: Dp = 62.dp,
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
