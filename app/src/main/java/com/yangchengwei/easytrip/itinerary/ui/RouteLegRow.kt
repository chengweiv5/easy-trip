package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.TransportMode

@Composable
fun RouteLegRow(leg: RouteLegUi, onMode: () -> Unit, onRetry: () -> Unit) {
    RouteLegContent(
        leg = leg,
        modifier = Modifier.fillMaxWidth().testTag("leg-${leg.id}"),
        onMode = onMode,
        onRetry = onRetry,
    )
}

@Composable
internal fun RouteLegContent(
    leg: RouteLegUi,
    modifier: Modifier = Modifier,
    onMode: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    val state = leg.state
    val stateModifier = if (state == RouteLegUiState.Calculating) {
        Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    } else {
        Modifier
    }
    Row(
        modifier.then(stateModifier).padding(start = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RouteLegConnector()
        Row(
            Modifier.weight(1f).padding(start = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                when (state) {
                    is RouteLegUiState.Ready -> {
                        Text(
                            leg.modeLabel(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = if (onMode == null) Modifier else Modifier
                                .clickable(onClick = onMode)
                                .testTag("mode-${leg.id}"),
                        )
                        Text(
                            listOfNotNull(
                                state.distanceMeters?.let(::formatDistance),
                                state.durationSeconds?.let(::formatDuration),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RouteLegUiState.Calculating -> Text(
                        "正在计算路线",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RouteLegUiState.WaitingForNetwork -> Text(
                        "等待联网",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    is RouteLegUiState.Failed -> Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when (state) {
                RouteLegUiState.Calculating -> CircularProgressIndicator(
                    Modifier.size(24.dp).semantics { contentDescription = "路线计算中" },
                )
                RouteLegUiState.WaitingForNetwork -> WaitingForNetworkIndicator()
                is RouteLegUiState.Failed -> onRetry?.let {
                    TextButton(it, Modifier.testTag("retry-${leg.id}")) { Text("重试") }
                }
                is RouteLegUiState.Ready -> Unit
            }
        }
    }
}

@Composable
private fun WaitingForNetworkIndicator() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        Modifier
            .size(24.dp)
            .semantics { contentDescription = "等待网络连接" },
    ) {
        drawCircle(color, radius = 2.dp.toPx(), center = Offset(size.width / 2f, size.height * 0.75f))
        drawArc(
            color = color,
            startAngle = 220f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(size.width * 0.3f, size.height * 0.42f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.4f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = 220f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(size.width * 0.12f, size.height * 0.16f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun RouteLegConnector() {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.width(12.dp).size(width = 12.dp, height = 48.dp).clearAndSetSemantics {}) {
        val centerX = size.width / 2f
        drawLine(color, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 2.dp.toPx())
        drawLine(
            color,
            Offset(centerX, size.height),
            Offset(centerX - 3.dp.toPx(), size.height - 5.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(centerX, size.height),
            Offset(centerX + 3.dp.toPx(), size.height - 5.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun RouteLegUi.modeLabel() = when (mode) {
    TransportMode.WALK -> "步行"
    TransportMode.TAXI -> "打车"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TRANSIT -> "公交"
}
