package com.yangchengwei.easytrip.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val EasyTripBackground = Color(0xFFF5F3EE)
val EasyTripSurface = Color(0xFFFFFFFF)
val EasyTripSurfaceSoft = Color(0xFFE7EFE2)
val EasyTripPrimary = Color(0xFF2D5E3A)
val EasyTripPrimaryDark = Color(0xFF1B3A28)
val EasyTripSecondary = Color(0xFF4A6B52)
val EasyTripMuted = Color(0xFF6E7F72)
val EasyTripBorder = Color(0xFFD6DDD0)
val EasyTripAccent = Color(0xFFD96F3B)
val EasyTripDanger = Color(0xFFBA1A1A)

val easyTripLightColorScheme = lightColorScheme(
    primary = EasyTripPrimary,
    onPrimary = EasyTripSurface,
    primaryContainer = EasyTripSurfaceSoft,
    onPrimaryContainer = EasyTripPrimaryDark,
    secondary = EasyTripSecondary,
    onSecondary = EasyTripSurface,
    background = EasyTripBackground,
    onBackground = EasyTripPrimaryDark,
    surface = EasyTripSurface,
    onSurface = EasyTripPrimaryDark,
    surfaceVariant = EasyTripSurfaceSoft,
    onSurfaceVariant = EasyTripSecondary,
    outline = EasyTripBorder,
    error = EasyTripDanger,
    onError = EasyTripSurface,
)
