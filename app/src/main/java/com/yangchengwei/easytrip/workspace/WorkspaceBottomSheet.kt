package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WorkspaceSheetLevel { COLLAPSED, HALF, EXPANDED }

internal fun restoreWorkspaceSheetLevel(raw: String?): WorkspaceSheetLevel =
    WorkspaceSheetLevel.entries.firstOrNull { it.name == raw } ?: WorkspaceSheetLevel.HALF

internal data class WorkspaceSheetAnchors(
    val collapsed: Dp,
    val half: Dp,
    val expanded: Dp,
) {
    operator fun get(level: WorkspaceSheetLevel): Dp = when (level) {
        WorkspaceSheetLevel.COLLAPSED -> collapsed
        WorkspaceSheetLevel.HALF -> half
        WorkspaceSheetLevel.EXPANDED -> expanded
    }
}

internal fun workspaceSheetAnchors(availableHeight: Dp): WorkspaceSheetAnchors {
    val height = availableHeight.coerceAtLeast(0.dp)
    val scale = (height.value / 782f).coerceIn(0f, 1f)
    val minimumCollapsed = minOf(96.dp, (height - 6.dp).coerceAtLeast(0.dp))
    val collapsed = maxOf(minimumCollapsed, 108.dp * scale).coerceAtMost(height)
    val expanded = maxOf(720.dp * scale, collapsed + 6.dp).coerceAtMost(height)
    val gap = minOf(12.dp, (expanded - collapsed) / 3f)
    val half = (432.dp * scale).coerceIn(collapsed + gap, expanded - gap)
    return WorkspaceSheetAnchors(collapsed, half, expanded)
}

internal fun workspaceSheetDragThreshold(
    current: WorkspaceSheetLevel,
    anchors: WorkspaceSheetAnchors,
): Dp {
    val adjacentGap = when (current) {
        WorkspaceSheetLevel.COLLAPSED -> anchors.half - anchors.collapsed
        WorkspaceSheetLevel.HALF -> minOf(anchors.half - anchors.collapsed, anchors.expanded - anchors.half)
        WorkspaceSheetLevel.EXPANDED -> anchors.expanded - anchors.half
    }
    return minOf(24.dp, adjacentGap / 2f)
}

internal fun resolveWorkspaceSheetDrag(
    current: WorkspaceSheetLevel,
    dragDeltaPx: Float,
    thresholdPx: Float,
): WorkspaceSheetLevel = when {
    dragDeltaPx < -thresholdPx -> when (current) {
        WorkspaceSheetLevel.COLLAPSED -> WorkspaceSheetLevel.HALF
        WorkspaceSheetLevel.HALF -> WorkspaceSheetLevel.EXPANDED
        WorkspaceSheetLevel.EXPANDED -> current
    }
    dragDeltaPx > thresholdPx -> when (current) {
        WorkspaceSheetLevel.EXPANDED -> WorkspaceSheetLevel.HALF
        WorkspaceSheetLevel.HALF -> WorkspaceSheetLevel.COLLAPSED
        WorkspaceSheetLevel.COLLAPSED -> current
    }
    else -> current
}

internal fun clampWorkspaceSheetDragOffsetPx(
    currentHeightPx: Float,
    collapsedHeightPx: Float,
    expandedHeightPx: Float,
    requestedOffsetPx: Float,
): Float = requestedOffsetPx.coerceIn(
    currentHeightPx - expandedHeightPx,
    currentHeightPx - collapsedHeightPx,
)

private val WorkspaceSheetHeaderHeight = 72.dp
private val CompactWorkspaceSheetHeaderHeight = 68.dp
private val MinimumCollapsedSummaryHeight = 18.dp

@Composable
internal fun WorkspaceBottomSheet(
    value: WorkspaceSheetLevel,
    anchors: WorkspaceSheetAnchors,
    onValueChange: (WorkspaceSheetLevel) -> Unit,
    dragOffsetPx: Float,
    onDragOffsetChange: (Float) -> Unit,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    collapsedContent: @Composable () -> Unit = {},
) {
    val targetHeight = anchors[value]
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { workspaceSheetDragThreshold(value, anchors).toPx() }
    val currentHeightPx = with(density) { targetHeight.toPx() }
    val collapsedHeightPx = with(density) { anchors.collapsed.toPx() }
    val expandedHeightPx = with(density) { anchors.expanded.toPx() }
    val visibleHeight = with(density) { (currentHeightPx - dragOffsetPx).toDp() }
        .coerceIn(anchors.collapsed, anchors.expanded)
    val sheetHeaderHeight = if (
        value == WorkspaceSheetLevel.COLLAPSED && visibleHeight < WorkspaceSheetHeaderHeight + MinimumCollapsedSummaryHeight
    ) {
        CompactWorkspaceSheetHeaderHeight
    } else {
        WorkspaceSheetHeaderHeight
    }
    val collapsedBottomPadding = if (visibleHeight >= 98.dp) 8.dp else 0.dp
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnDragOffsetChange by rememberUpdatedState(onDragOffsetChange)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(visibleHeight)
            .testTag("workspace-sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(sheetHeaderHeight)
                    .padding(horizontal = 20.dp)
                    .pointerInput(value, anchors, density) {
                        var gestureDragOffsetPx = 0f
                        detectVerticalDragGestures(
                            onDragStart = { gestureDragOffsetPx = 0f },
                            onVerticalDrag = { _, amount ->
                                gestureDragOffsetPx = clampWorkspaceSheetDragOffsetPx(
                                    currentHeightPx = currentHeightPx,
                                    collapsedHeightPx = collapsedHeightPx,
                                    expandedHeightPx = expandedHeightPx,
                                    requestedOffsetPx = gestureDragOffsetPx + amount,
                                )
                                currentOnDragOffsetChange(gestureDragOffsetPx)
                            },
                            onDragEnd = {
                                val next = resolveWorkspaceSheetDrag(value, gestureDragOffsetPx, dragThresholdPx)
                                gestureDragOffsetPx = 0f
                                currentOnDragOffsetChange(0f)
                                if (next != value) currentOnValueChange(next)
                            },
                            onDragCancel = {
                                gestureDragOffsetPx = 0f
                                currentOnDragOffsetChange(0f)
                            },
                        )
                    }
                    .testTag("workspace-sheet-handle"),
            ) { header() }
            if (value == WorkspaceSheetLevel.COLLAPSED) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 20.dp, end = 20.dp, bottom = collapsedBottomPadding)
                        .testTag("workspace-collapsed-content"),
                    contentAlignment = Alignment.CenterStart,
                ) { collapsedContent() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.fillMaxSize()) { content() }
                }
            }
        }
    }
}
