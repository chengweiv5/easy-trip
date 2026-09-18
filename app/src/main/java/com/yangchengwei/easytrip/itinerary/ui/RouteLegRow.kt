package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    showEndpointText: Boolean = true,
    connectorInset: Dp = 9.dp,
) {
    RouteLegContent(
        leg = leg,
        modifier = Modifier.fillMaxWidth().testTag("leg-${leg.id}"),
        fromPlaceName = fromPlaceName,
        toPlaceName = toPlaceName,
        onMode = onMode,
        onRetry = onRetry,
        showEndpointText = showEndpointText,
        connectorInset = connectorInset,
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
    showEndpointText: Boolean = true,
    connectorInset: Dp = 9.dp,
) {
    val state = leg.state
    val colors = leg.routeLegColors()
    val editAction = onMode?.takeIf { state is RouteLegUiState.Ready }
    val retryAction = onRetry?.takeIf { state is RouteLegUiState.Failed }
    val stateModifier = when (state) {
        RouteLegUiState.Calculating,
        RouteLegUiState.WaitingForNetwork,
        is RouteLegUiState.Failed,
        -> Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        else -> Modifier
    }
    val hasEndpoints = !fromPlaceName.isNullOrBlank() && !toPlaceName.isNullOrBlank()
    val endpointLabel = if (hasEndpoints) "从${fromPlaceName}到${toPlaceName}的路段" else "路段"
    val surfaceModifier = Modifier
        .then(if (editAction == null) Modifier else Modifier.clickable(onClick = editAction))
        .then(
            if (hasEndpoints) {
                Modifier.semantics {
                    contentDescription = if (editAction == null) endpointLabel else "编辑$endpointLabel"
                }
            } else if (editAction != null) {
                Modifier.semantics { contentDescription = "编辑路段" }
            } else {
                Modifier
            },
        )
    Box(modifier.fillMaxWidth().then(stateModifier)) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(surfaceModifier)
                .testTag("route-leg-action-${leg.id}")
                .background(colors.background, RoundedCornerShape(8.dp))
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 10.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.width(12.dp).fillMaxHeight()) {
                RouteLegConnector(
                    legId = leg.id,
                    color = colors.foreground,
                    modifier = Modifier
                        .offset(x = connectorInset - 6.dp)
                        .fillMaxHeight(),
                )
            }
            Spacer(Modifier.width(13.dp + connectorInset))
            Row(
                Modifier.weight(1f).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    if (showEndpointText && hasEndpoints) {
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
                                color = colors.foreground,
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
                                color = colors.foreground,
                            )
                            if (state.message != "路线计算失败") {
                                Text(
                                    state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foreground,
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
                    when (state) {
                        RouteLegUiState.Pending -> PendingRouteIndicator()
                        RouteLegUiState.Calculating -> CircularProgressIndicator(
                            Modifier.size(24.dp).semantics { contentDescription = "路线计算中" },
                        )
                        RouteLegUiState.WaitingForNetwork -> WaitingForNetworkIndicator()
                        is RouteLegUiState.Failed -> retryAction?.let {
                            TextButton(it, Modifier.widthIn(max = 96.dp).testTag("retry-${leg.id}")) { Text("重试") }
                        }
                        is RouteLegUiState.Ready -> Unit
                    }
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
private fun RouteLegConnector(
    legId: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.width(12.dp).testTag("route-connector-$legId").clearAndSetSemantics {}) {
        val centerX = size.width / 2f
        val inset = 6.dp.toPx()
        val lineTop = inset
        val lineBottom = (size.height - inset).coerceAtLeast(lineTop)
        drawLine(
            color = color,
            start = Offset(centerX, lineTop),
            end = Offset(centerX, lineBottom),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
        )
        drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(centerX, size.height / 2f))
    }
}

private fun RouteLegUi.modeLabel() = when (mode) {
    TransportMode.WALK -> "步行"
    TransportMode.TAXI -> "打车"
    TransportMode.DRIVE -> "驾车"
    TransportMode.TRANSIT -> "公交"
}
