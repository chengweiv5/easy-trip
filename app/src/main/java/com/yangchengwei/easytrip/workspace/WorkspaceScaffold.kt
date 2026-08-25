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
): WorkspaceLayoutMetrics {
    val sheetHeight = anchors[level].coerceAtMost(availableHeight)
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
        val metrics = workspaceLayoutMetrics(maxHeight, anchors, sheetLevel)
        Box(Modifier.fillMaxSize()) {
            map(metrics)
            topOverlay(metrics)
        }
        WorkspaceBottomSheet(
            value = sheetLevel,
            anchors = anchors,
            onValueChange = onSheetLevelChange,
            header = sheetHeader,
            content = sheetContent,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )
    }
}
