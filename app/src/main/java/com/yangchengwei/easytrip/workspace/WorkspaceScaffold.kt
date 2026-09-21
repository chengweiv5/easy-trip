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
    val availableWidth: Dp,
    val availableHeight: Dp,
    val sheetHeight: Dp,
    val sheetTop: Dp,
    val overlayBottomInset: Dp,
    val stableSheetHeight: Dp = sheetHeight,
)

internal fun workspaceLayoutMetrics(
    availableHeight: Dp,
    anchors: WorkspaceSheetAnchors,
    level: WorkspaceSheetLevel,
    overlayGap: Dp = 20.dp,
    availableWidth: Dp = Dp.Infinity,
): WorkspaceLayoutMetrics = workspaceLayoutMetrics(
    availableWidth = availableWidth,
    availableHeight = availableHeight,
    visibleSheetHeight = anchors[level],
    overlayGap = overlayGap,
)

internal val workspacePlaceDetailSheetTopCornerRadius = 20.dp

internal fun workspacePlaceDetailSheetHeight(availableHeight: Dp): Dp {
    val height = availableHeight.coerceAtLeast(0.dp)
    if (height < 416.dp) return height
    val proportionalHeight = height * 490f / 782f
    return proportionalHeight.coerceIn(320.dp, 560.dp)
}

internal fun workspaceLayoutMetrics(
    availableHeight: Dp,
    visibleSheetHeight: Dp,
    overlayGap: Dp = 20.dp,
    availableWidth: Dp = Dp.Infinity,
): WorkspaceLayoutMetrics {
    val sheetHeight = visibleSheetHeight.coerceIn(0.dp, availableHeight)
    val sheetTop = (availableHeight - sheetHeight).coerceAtLeast(0.dp)
    return WorkspaceLayoutMetrics(
        availableWidth = availableWidth,
        availableHeight = availableHeight,
        sheetHeight = sheetHeight,
        sheetTop = sheetTop,
        overlayBottomInset = sheetHeight + overlayGap,
        stableSheetHeight = sheetHeight,
    )
}

internal fun workspaceLegendTop(metrics: WorkspaceLayoutMetrics): Dp =
    (metrics.sheetTop - 12.dp - 32.dp).coerceAtLeast(0.dp)

data class WorkspaceMapLayout(
    val fitInsets: MapViewportInsets,
    val visibleInsets: MapViewportInsets,
)

internal fun workspaceMapLayout(metrics: WorkspaceLayoutMetrics, density: Float) = WorkspaceMapLayout(
    fitInsets = workspaceViewportInsets(metrics, density),
    // Locating uses the map above the live drawer, without reserving space for floating controls.
    visibleInsets = MapViewportInsets(bottomPx = (metrics.sheetHeight.value * density).toInt()),
)

internal fun workspaceViewportInsets(
    metrics: WorkspaceLayoutMetrics,
    density: Float,
): MapViewportInsets {
    fun Dp.toPixels() = (value * density).toInt()
    fun clampPair(first: Int, second: Int, available: Int): Pair<Int, Int> {
        val maxTotal = (available - 1).coerceAtLeast(0)
        val total = first + second
        if (total <= maxTotal) return first to second
        if (total == 0) return 0 to 0
        val clampedFirst = (first.toLong() * maxTotal / total).toInt()
        return clampedFirst to (maxTotal - clampedFirst)
    }
    val (left, right) = clampPair(
        132.dp.toPixels(),
        40.dp.toPixels(),
        metrics.availableWidth.toPixels(),
    )
    val (top, bottom) = clampPair(
        76.dp.toPixels(),
        (metrics.stableSheetHeight + 13.dp).toPixels(),
        metrics.availableHeight.toPixels(),
    )
    val stableLegendTop = (metrics.availableHeight - metrics.stableSheetHeight - 12.dp - 32.dp).coerceAtLeast(0.dp)
    val legendBottom = (metrics.availableHeight - stableLegendTop).toPixels()
    val requestedBottom = maxOf(bottom, legendBottom)
    val (safeTopPx, safeBottomPx) = clampPair(top, requestedBottom, metrics.availableHeight.toPixels())
    return MapViewportInsets(left, safeTopPx, right, safeBottomPx)
}

@Composable
internal fun WorkspaceScaffold(
    sheetLevel: WorkspaceSheetLevel,
    onSheetLevelChange: (WorkspaceSheetLevel) -> Unit,
    modifier: Modifier = Modifier,
    map: @Composable BoxScope.(WorkspaceLayoutMetrics) -> Unit,
    topOverlay: @Composable BoxScope.(WorkspaceLayoutMetrics) -> Unit,
    modalOverlay: @Composable BoxScope.(WorkspaceLayoutMetrics) -> Unit = {},
    sheetHeader: @Composable (WorkspaceLayoutMetrics) -> Unit,
    sheetContent: @Composable (WorkspaceLayoutMetrics) -> Unit,
    collapsedContent: @Composable () -> Unit = {},
    sheetContentHorizontalPadding: Dp = 16.dp,
    collapsedContentHorizontalPadding: Dp = sheetContentHorizontalPadding,
    sheetGesturesEnabled: Boolean = true,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        val anchors = workspaceSheetAnchors(maxHeight)
        val density = LocalDensity.current
        var dragOffsetPx by remember(sheetLevel, anchors, density) { mutableFloatStateOf(0f) }
        val visibleSheetHeight = with(density) { anchors[sheetLevel].toPx() - dragOffsetPx }.let { heightPx ->
            with(density) { heightPx.toDp() }
        }
        val metrics = workspaceLayoutMetrics(
            availableHeight = maxHeight,
            visibleSheetHeight = visibleSheetHeight,
            availableWidth = maxWidth,
        ).copy(stableSheetHeight = anchors[sheetLevel])
        Box(Modifier.fillMaxSize()) {
            map(metrics)
            topOverlay(metrics)
        }
        WorkspaceBottomSheet(
            value = sheetLevel,
            anchors = anchors,
            onValueChange = onSheetLevelChange,
            gesturesEnabled = sheetGesturesEnabled,
            dragOffsetPx = dragOffsetPx,
            onDragOffsetChange = { dragOffsetPx = it },
            header = { sheetHeader(metrics) },
            content = { sheetContent(metrics) },
            collapsedContent = collapsedContent,
            contentHorizontalPadding = sheetContentHorizontalPadding,
            collapsedContentHorizontalPadding = collapsedContentHorizontalPadding,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )
        Box(Modifier.fillMaxSize()) { modalOverlay(metrics) }
    }
}
