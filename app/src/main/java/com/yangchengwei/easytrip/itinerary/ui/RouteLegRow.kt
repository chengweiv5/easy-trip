package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton as TextButton

@Composable
fun RouteLegRow(
    leg: RouteLegUi,
    fromPlaceName: String? = null,
    toPlaceName: String? = null,
    onMode: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    RouteLegContent(
        leg = leg,
        modifier = Modifier.fillMaxWidth().testTag("leg-${leg.id}"),
        fromPlaceName = fromPlaceName,
        toPlaceName = toPlaceName,
        onMode = onMode,
        onRetry = onRetry,
    )
}

@Composable
internal fun RouteLegContent(
    leg: RouteLegUi,
    modifier: Modifier = Modifier,
    fromPlaceName: String? = null,
    toPlaceName: String? = null,
    onMode: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    val state = leg.state
    val editAction = onMode?.takeIf { state is RouteLegUiState.Ready }
    val retryAction = onRetry?.takeIf { state is RouteLegUiState.Failed }
    val stateModifier = when (state) {
        RouteLegUiState.Calculating,
        RouteLegUiState.WaitingForNetwork,
        is RouteLegUiState.Failed,
        -> Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        else -> Modifier
    }
    Row(
        modifier.then(stateModifier).padding(start = 28.dp).height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        RouteLegConnector(leg.id, Modifier.fillMaxHeight())
        Row(
            Modifier.weight(1f).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (!fromPlaceName.isNullOrBlank() && !toPlaceName.isNullOrBlank()) {
                    Text(
                        "$fromPlaceName → $toPlaceName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                            buildList {
                                add("已规划")
                                state.distanceMeters?.let(::formatDistance)?.let(::add)
                                leg.effectiveDurationSeconds?.let(::formatDuration)?.let { add("预计 $it") }
                            }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RouteLegUiState.Pending -> RouteLegStatus(leg, "等待计算路线")
                    RouteLegUiState.Calculating -> RouteLegStatus(leg, "正在计算路线")
                    RouteLegUiState.WaitingForNetwork -> RouteLegStatus(leg, "等待联网后计算")
                    is RouteLegUiState.Failed -> {
                        Text(
                            "路线计算失败",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        if (state.message != "路线计算失败") {
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        leg.effectiveDurationSeconds?.let { duration ->
                            Text(
                                "预计 ${formatDuration(duration)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                editAction?.let {
                    TextButton(it, Modifier.testTag("edit-route-${leg.id}")) { Text("编辑路段") }
                }
                when (state) {
                    RouteLegUiState.Pending -> PendingRouteIndicator()
                    RouteLegUiState.Calculating -> CircularProgressIndicator(
                        Modifier.size(24.dp).semantics { contentDescription = "路线计算中" },
                    )
                    RouteLegUiState.WaitingForNetwork -> WaitingForNetworkIndicator()
                    is RouteLegUiState.Failed -> retryAction?.let {
                        TextButton(it, Modifier.testTag("retry-${leg.id}")) { Text("重试") }
                    }
                    is RouteLegUiState.Ready -> Unit
                }
            }
        }
    }
}

@Composable
private fun RouteLegStatus(leg: RouteLegUi, text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    leg.effectiveDurationSeconds?.let { duration ->
        Text(
            "预计 ${formatDuration(duration)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PendingRouteIndicator() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        Modifier
            .size(24.dp)
            .semantics { contentDescription = "等待计算路线" },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = color, center = center, radius = 8.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawLine(color, center, Offset(center.x, center.y - 4.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, center, Offset(center.x + 3.dp.toPx(), center.y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun WaitingForNetworkIndicator() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        Modifier
            .size(24.dp)
            .semantics { contentDescription = "离线，等待联网后计算" },
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
private fun RouteLegConnector(legId: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.width(12.dp).testTag("route-connector-$legId").clearAndSetSemantics {}) {
        val centerX = size.width / 2f
        drawLine(color, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 1.dp.toPx())
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
