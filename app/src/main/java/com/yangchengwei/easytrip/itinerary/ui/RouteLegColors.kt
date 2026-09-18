package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.ui.graphics.Color
import com.yangchengwei.easytrip.core.model.TransportMode

internal data class RouteLegColors(
    val foreground: Color,
    val background: Color,
)

internal fun RouteLegUi.routeLegColors(): RouteLegColors = when (state) {
    is RouteLegUiState.Failed -> RouteLegColors(
        foreground = Color(0xFFBA1A1A),
        background = Color(0xFFFCEFED),
    )
    else -> when (mode) {
        TransportMode.WALK -> RouteLegColors(
            foreground = Color(0xFF08766B),
            background = Color(0xFFEAF5F2),
        )
        TransportMode.DRIVE -> RouteLegColors(
            foreground = Color(0xFF2463AF),
            background = Color(0xFFECF2FB),
        )
        TransportMode.TAXI -> RouteLegColors(
            foreground = Color(0xFF975910),
            background = Color(0xFFFBF1E2),
        )
        TransportMode.TRANSIT -> RouteLegColors(
            foreground = Color(0xFF7952AF),
            background = Color(0xFFF3EEFA),
        )
    }
}
