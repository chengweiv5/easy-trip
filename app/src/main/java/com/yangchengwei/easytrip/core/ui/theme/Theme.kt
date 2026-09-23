package com.yangchengwei.easytrip.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val easyTripSpacing = EasyTripSpacing()
private val easyTripSizes = EasyTripSizes()
private val easyTripElevation = EasyTripElevation()

@Composable
fun EasyTripTheme(palette: ThemePalette = ThemePalette.LAKE, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalEasyTripSpacing provides easyTripSpacing,
        LocalThemePalette provides palette,
        LocalEasyTripSizes provides easyTripSizes,
        LocalEasyTripElevation provides easyTripElevation,
    ) {
        MaterialTheme(
            colorScheme = palette.colors,
            typography = easyTripTypography,
            shapes = easyTripShapes,
            content = content,
        )
    }
}

val LocalThemePalette = androidx.compose.runtime.staticCompositionLocalOf { ThemePalette.LAKE }
