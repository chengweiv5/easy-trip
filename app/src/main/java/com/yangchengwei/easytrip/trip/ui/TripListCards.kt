package com.yangchengwei.easytrip.trip.ui

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val TripMenuItem = SemanticsPropertyKey<Boolean>("TripMenuItem")
internal var SemanticsPropertyReceiver.tripMenuItem by TripMenuItem
internal val TripMenuTone = SemanticsPropertyKey<String>("TripMenuTone")
internal var SemanticsPropertyReceiver.tripMenuTone by TripMenuTone

@Composable
internal fun PrimaryTripCard(
    trip: TripCardUiModel,
    menuExpanded: Boolean,
    statusUpdating: Boolean = false,
    onMenuExpandedChange: (Boolean) -> Unit,
    onAction: (TripListAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .testTag("primary-trip-${trip.id}")
            .clickable(role = Role.Button) { onAction(TripListAction.OpenTrip(trip.id)) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(Modifier.heightIn(min = 208.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (trip.hasTraveled) "旅行回忆" else "下一站"} · ${trip.name}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    modifier = Modifier.widthIn(max = 132.dp).testTag("trip-status-${trip.id}"),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        trip.statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TripMenu(
                    trip = trip,
                    expanded = menuExpanded,
                    statusUpdating = statusUpdating,
                    iconColor = MaterialTheme.colorScheme.onPrimary,
                    onExpandedChange = onMenuExpandedChange,
                    onAction = onAction,
                )
            }
            Text(
                trip.name,
                modifier = Modifier.testTag("primary-trip-name-${trip.id}"),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(trip.dateLabel ?: "待定日期", trip.dayCountLabel).joinToString(" · "),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(trip.placeCountLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(trip.tripDayCountLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "行程准备度",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Text(trip.readinessLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                ContinuousReadinessProgress(
                    progress = trip.readinessPercent / 100f,
                    contentDescription = "已有行程内容 ${trip.scheduledDayCount}/${trip.tripDayCountLabel.substringBefore(' ')} 个旅行日",
                    modifier = Modifier.fillMaxWidth()
                        .height(5.dp)
                        .semantics { }
                        .testTag("trip-readiness-${trip.id}"),
                )
            }
        }
    }
}

@Composable
private fun ContinuousReadinessProgress(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val visualProgress = progress.coerceIn(0f, 1f).coerceAtLeast(0.02f)
    val highlight = com.yangchengwei.easytrip.core.ui.theme.LocalThemePalette.current.highlight
    val trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
    Box(
        modifier.semantics(mergeDescendants = true) {
            this.contentDescription = contentDescription
            progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
        },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.height / 2f
            drawRoundRect(
                color = trackColor,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            val fillWidth = (size.width * visualProgress).coerceIn(size.height, size.width)
            if (fillWidth == size.width) {
                drawRoundRect(
                    color = highlight,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                )
            } else {
                drawCircle(highlight, radius, Offset(radius, radius))
                drawRect(
                    color = highlight,
                    topLeft = Offset(radius, 0f),
                    size = androidx.compose.ui.geometry.Size(fillWidth - radius, size.height),
                )
            }
        }
    }
}

@Composable
internal fun OtherTripRow(
    trip: TripCardUiModel,
    menuExpanded: Boolean,
    statusUpdating: Boolean = false,
    onMenuExpandedChange: (Boolean) -> Unit,
    onAction: (TripListAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("other-trip-${trip.id}")
            .semantics { contentDescription = "打开旅行 ${trip.name}" }
            .clickable(role = Role.Button) { onAction(TripListAction.OpenTrip(trip.id)) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(Modifier.size(48.dp), RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Text("旅", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                trip.name,
                modifier = Modifier.testTag("other-trip-name-${trip.id}"),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${trip.statusLabel} · ${trip.dateLabel ?: "待定日期"} · ${trip.travelModeLabel}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            modifier = Modifier.size(36.dp).testTag("other-trip-arrow-${trip.id}")
                .clearAndSetSemantics { },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center) { ArrowIcon(Modifier.size(20.dp)) }
        }
        TripMenu(
            trip = trip,
            expanded = menuExpanded,
            statusUpdating = statusUpdating,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onExpandedChange = onMenuExpandedChange,
            onAction = onAction,
        )
    }
}

@Composable
private fun TripMenu(
    trip: TripCardUiModel,
    expanded: Boolean,
    statusUpdating: Boolean,
    iconColor: Color,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (TripListAction) -> Unit,
) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.size(28.dp).testTag("trip-menu-${trip.id}")
                .semantics { contentDescription = "${trip.name}，更多旅行操作" },
        ) { MoreIcon(Modifier.size(22.dp), iconColor) }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.testTag("trip-menu-popup-${trip.id}"),
        ) {
            DropdownMenuItem(
                text = { Text(if (statusUpdating) "正在更新…" else if (trip.hasTraveled) "改为待出行" else "标记为已出行") },
                enabled = !statusUpdating,
                onClick = {
                    onExpandedChange(false)
                    onAction(TripListAction.SetHasTraveled(trip.id, !trip.hasTraveled))
                },
                modifier = Modifier.testTag("trip-menu-status-${trip.id}").semantics { tripMenuItem = true },
            )
            DropdownMenuItem(
                text = { Text("设置") },
                onClick = {
                    onExpandedChange(false)
                    onAction(TripListAction.OpenSettings(trip.id))
                },
                modifier = Modifier.testTag("trip-menu-settings-${trip.id}").semantics { tripMenuItem = true },
            )
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onExpandedChange(false)
                    onAction(TripListAction.RequestDelete(trip.id))
                },
                modifier = Modifier.testTag("trip-menu-delete-${trip.id}").semantics {
                    tripMenuItem = true
                    tripMenuTone = "danger"
                },
            )
        }
    }
}

@Composable
private fun ArrowIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val centerY = size.height / 2f
        val startX = size.width * .25f
        val endX = size.width * .75f
        val arrow = size.minDimension * .2f
        drawLine(color, Offset(startX, centerY), Offset(endX, centerY), strokeWidth = size.minDimension / 12f)
        drawLine(color, Offset(endX - arrow, centerY - arrow), Offset(endX, centerY), strokeWidth = size.minDimension / 12f)
        drawLine(color, Offset(endX - arrow, centerY + arrow), Offset(endX, centerY), strokeWidth = size.minDimension / 12f)
    }
}

@Composable
private fun MoreIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val radius = size.minDimension / 11f
        listOf(.25f, .5f, .75f).forEach { fraction ->
            drawCircle(color, radius, Offset(size.width * fraction, size.height / 2f))
        }
    }
}
