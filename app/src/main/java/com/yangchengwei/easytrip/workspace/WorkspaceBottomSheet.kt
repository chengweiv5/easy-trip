package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class WorkspaceSheetLevel { COLLAPSED, HALF, EXPANDED }

internal fun restoreWorkspaceSheetLevel(raw: String?): WorkspaceSheetLevel =
    WorkspaceSheetLevel.entries.firstOrNull { it.name == raw } ?: WorkspaceSheetLevel.HALF

internal fun workspaceSheetFraction(level: WorkspaceSheetLevel): Float = when (level) {
    WorkspaceSheetLevel.COLLAPSED -> 0f
    WorkspaceSheetLevel.HALF -> 0.5f
    WorkspaceSheetLevel.EXPANDED -> 0.9f
}

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

internal fun workspaceSheetAnchors(
    availableHeight: Dp,
    searchReturn: Boolean,
): WorkspaceSheetAnchors {
    val expanded = availableHeight * workspaceSheetFraction(WorkspaceSheetLevel.EXPANDED)
    val collapsed = minOf(34.dp, expanded / 3f)
    val levelGap = minOf(12.dp, (expanded - collapsed) / 2f)
    val half = maxOf(
        availableHeight * workspaceSheetFraction(WorkspaceSheetLevel.HALF) + if (searchReturn) 16.dp else 0.dp,
        240.dp,
    ).coerceIn(collapsed + levelGap, expanded - levelGap)
    return WorkspaceSheetAnchors(collapsed, half, expanded)
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
) {
    val targetHeight = anchors[value]
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 24.dp.toPx() }
    val currentHeightPx = with(density) { targetHeight.toPx() }
    val collapsedHeightPx = with(density) { anchors.collapsed.toPx() }
    val expandedHeightPx = with(density) { anchors.expanded.toPx() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(targetHeight)
            .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
            .testTag("workspace-sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
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
                                onDragOffsetChange(gestureDragOffsetPx)
                            },
                            onDragEnd = {
                                val next = resolveWorkspaceSheetDrag(value, gestureDragOffsetPx, dragThresholdPx)
                                gestureDragOffsetPx = 0f
                                onDragOffsetChange(0f)
                                if (next != value) onValueChange(next)
                            },
                            onDragCancel = {
                                gestureDragOffsetPx = 0f
                                onDragOffsetChange(0f)
                            },
                        )
                    }
                    .testTag("workspace-sheet-handle"),
            ) { header() }
            if (value != WorkspaceSheetLevel.COLLAPSED) content()
        }
    }
}
