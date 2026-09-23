package com.yangchengwei.easytrip.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Stable IDs are persisted; display names may change without losing the user's preference. */
enum class ThemePalette(
    val id: String,
    val displayName: String,
    val description: String,
    primary: Long,
    background: Long,
    soft: Long,
    text: Long,
    muted: Long,
    border: Long,
    accent: Long,
    val highlight: Color,
) {
    LAKE("lake", "湖畔晴空", "清爽 · 经典", 0xFF086F76, 0xFFECF3F4, 0xFFD9EBED, 0xFF20343B, 0xFF53676D, 0xFFCCDDE0, 0xFFA3532B, Color(0xFFE5BA80)),
    FOREST("forest", "松林晨光", "自然 · 安静", 0xFF2F6249, 0xFFEFF3EE, 0xFFDDEADF, 0xFF203329, 0xFF56645B, 0xFFCDD9CE, 0xFFA45432, Color(0xFFE5BA80)),
    SUNSET("sunset", "落日陶土", "温暖 · 松弛", 0xFF9E4830, 0xFFF5F0E9, 0xFFEEDFD3, 0xFF392C26, 0xFF6E5D52, 0xFFDFD2C7, 0xFF2F7065, Color(0xFFBCDBCC)),
    VIOLET("violet", "山岚暮紫", "柔和 · 沉静", 0xFF70548E, 0xFFF2EFF6, 0xFFE7DFF0, 0xFF332C3D, 0xFF665B72, 0xFFD9D0E2, 0xFF98602F, Color(0xFFE5BA80)),
    ROSE("rose", "玫瑰沙丘", "轻盈 · 浪漫", 0xFF934D63, 0xFFF7EFF2, 0xFFF0DFE6, 0xFF3D2C34, 0xFF715B65, 0xFFE2CFD7, 0xFF446D64, Color(0xFFC2D9CB));

    val colors: ColorScheme = easyTripLightColorScheme.copy(
        primary = Color(primary), onPrimary = Color.White,
        primaryContainer = Color(soft), onPrimaryContainer = Color(text),
        secondary = Color(muted), onSecondary = Color.White,
        secondaryContainer = Color(soft), onSecondaryContainer = Color(text),
        tertiary = Color(accent), onTertiary = Color.White,
        tertiaryContainer = if (id == "lake") Color(0xFFF5E6D9) else Color(background),
        onTertiaryContainer = Color(accent),
        background = Color(background), onBackground = Color(text),
        surface = Color.White, onSurface = Color(text),
        surfaceVariant = Color(soft), onSurfaceVariant = Color(muted),
        surfaceTint = Color(primary), surfaceDim = Color(border), surfaceBright = Color.White,
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
        surfaceContainer = Color.White, surfaceContainerHigh = Color(background),
        surfaceContainerHighest = Color(soft),
        inverseSurface = Color(text), inverseOnSurface = Color.White, inversePrimary = Color(soft),
        outline = Color(border), outlineVariant = Color(border),
    )

    companion object {
        fun fromId(id: String?): ThemePalette = entries.firstOrNull { it.id == id } ?: LAKE
    }
}
