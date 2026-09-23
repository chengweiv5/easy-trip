package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.ui.graphics.Color
import com.yangchengwei.easytrip.core.ui.theme.EasyTripDanger
import com.yangchengwei.easytrip.core.ui.theme.EasyTripErrorSurface
import com.yangchengwei.easytrip.core.ui.theme.EasyTripMuted
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSurfaceSoft

internal data class RouteLegColors(
    val foreground: Color,
    val background: Color,
)

internal fun RouteLegUi.routeLegColors(
    muted: Color = EasyTripMuted,
    soft: Color = EasyTripSurfaceSoft,
): RouteLegColors = when (state) {
    is RouteLegUiState.Failed -> RouteLegColors(
        foreground = EasyTripDanger,
        background = EasyTripErrorSurface,
    )
    else -> RouteLegColors(
        foreground = muted,
        background = soft,
    )
}
