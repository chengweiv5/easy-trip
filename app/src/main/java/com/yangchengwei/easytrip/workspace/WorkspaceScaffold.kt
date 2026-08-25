package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class WorkspaceLayoutMetrics(
    val availableHeight: Dp,
    val sheetHeight: Dp,
    val sheetTop: Dp,
    val overlayBottomInset: Dp,
)

internal fun workspaceLayoutMetrics(
    availableHeight: Dp,
    anchors: WorkspaceSheetAnchors,
    level: WorkspaceSheetLevel,
    overlayGap: Dp = 20.dp,
): WorkspaceLayoutMetrics = workspaceLayoutMetrics(
    availableHeight = availableHeight,
    visibleSheetHeight = anchors[level],
    overlayGap = overlayGap,
)

internal fun workspaceLayoutMetrics(
    availableHeight: Dp,
    visibleSheetHeight: Dp,
    overlayGap: Dp = 20.dp,
): WorkspaceLayoutMetrics {
    val sheetHeight = visibleSheetHeight.coerceIn(0.dp, availableHeight)
    val sheetTop = (availableHeight - sheetHeight).coerceAtLeast(0.dp)
    return WorkspaceLayoutMetrics(
        availableHeight = availableHeight,
        sheetHeight = sheetHeight,
        sheetTop = sheetTop,
        overlayBottomInset = sheetHeight + overlayGap,
    )
}

@Composable
internal fun WorkspaceScaffold(
    sheetLevel: WorkspaceSheetLevel,
    searchReturn: Boolean,
    onSheetLevelChange: (WorkspaceSheetLevel) -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable BoxScope.(WorkspaceLayoutMetrics) -> Unit,
    topOverlay: @Composable BoxScope.(WorkspaceLayoutMetrics) -> Unit,
    sheetHeader: @Composable () -> Unit,
    sheetContent: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        val anchors = workspaceSheetAnchors(maxHeight, searchReturn)
        val density = LocalDensity.current
        var dragOffsetPx by remember(sheetLevel, anchors, density) { mutableFloatStateOf(0f) }
        val visibleSheetHeight = with(density) { anchors[sheetLevel].toPx() - dragOffsetPx }.let { heightPx ->
            with(density) { heightPx.toDp() }
        }
        val metrics = workspaceLayoutMetrics(maxHeight, visibleSheetHeight)
        Box(Modifier.fillMaxSize()) {
            map(metrics)
            topOverlay(metrics)
        }
        WorkspaceBottomSheet(
            value = sheetLevel,
            anchors = anchors,
            onValueChange = onSheetLevelChange,
            dragOffsetPx = dragOffsetPx,
            onDragOffsetChange = { dragOffsetPx = it },
            header = sheetHeader,
            content = sheetContent,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )
    }
}
