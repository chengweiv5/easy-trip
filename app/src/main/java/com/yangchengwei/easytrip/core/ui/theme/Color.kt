package com.yangchengwei.easytrip.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val EasyTripBackground = Color(0xFFECF3F4)
val EasyTripSurface = Color(0xFFFFFFFF)
val EasyTripSurfaceSoft = Color(0xFFD9EBED)
val EasyTripPrimary = Color(0xFF086F76)
val EasyTripPrimaryDark = Color(0xFF20343B)
val EasyTripSecondary = Color(0xFF53676D)
val EasyTripMuted = Color(0xFF53676D)
val EasyTripBorder = Color(0xFFCCDDE0)
val EasyTripAccent = Color(0xFFA3532B)
val EasyTripHighlight = Color(0xFFE5BA80)
val EasyTripDanger = Color(0xFFB3261E)
val EasyTripErrorSurface = Color(0xFFFCEFED)
val EasyTripPlaceSurface = EasyTripBackground
val EasyTripPlaceBorder = EasyTripBorder
val EasyTripAddress = EasyTripMuted
val EasyTripNote = Color(0xFF8A5B3D)
val EasyTripScheduled = EasyTripPrimary

val easyTripLightColorScheme = lightColorScheme(
    primary = EasyTripPrimary,
    onPrimary = EasyTripSurface,
    primaryContainer = EasyTripSurfaceSoft,
    onPrimaryContainer = EasyTripPrimaryDark,
    secondary = EasyTripSecondary,
    secondaryContainer = EasyTripSurfaceSoft,
    onSecondaryContainer = EasyTripPrimaryDark,
    onSecondary = EasyTripSurface,
    tertiary = EasyTripAccent,
    onTertiary = EasyTripSurface,
    tertiaryContainer = Color(0xFFF5E6D9),
    onTertiaryContainer = EasyTripAccent,
    background = EasyTripBackground,
    onBackground = EasyTripPrimaryDark,
    surface = EasyTripSurface,
    onSurface = EasyTripPrimaryDark,
    surfaceVariant = EasyTripSurfaceSoft,
    onSurfaceVariant = EasyTripMuted,
    surfaceTint = EasyTripPrimary,
    surfaceDim = EasyTripBorder,
    surfaceBright = EasyTripSurface,
    surfaceContainerLowest = EasyTripSurface,
    surfaceContainerLow = EasyTripSurface,
    surfaceContainer = EasyTripSurface,
    surfaceContainerHigh = EasyTripBackground,
    surfaceContainerHighest = EasyTripSurfaceSoft,
    inverseSurface = EasyTripPrimaryDark,
    inverseOnSurface = EasyTripSurface,
    inversePrimary = EasyTripSurfaceSoft,
    scrim = Color.Black,
    outline = EasyTripBorder,
    outlineVariant = EasyTripBorder,
    error = EasyTripDanger,
    onError = EasyTripSurface,
    errorContainer = EasyTripErrorSurface,
    onErrorContainer = EasyTripDanger,
)
