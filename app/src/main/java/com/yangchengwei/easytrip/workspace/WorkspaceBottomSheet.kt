package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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

@Composable
fun WorkspaceBottomSheet(
    value: WorkspaceSheetLevel,
    onValueChange: (WorkspaceSheetLevel) -> Unit,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        background()
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val collapsedHeight = 34.dp
            val targetHeight = when (value) {
                WorkspaceSheetLevel.COLLAPSED -> collapsedHeight
                WorkspaceSheetLevel.HALF -> maxHeight * workspaceSheetFraction(value)
                WorkspaceSheetLevel.EXPANDED -> maxHeight * workspaceSheetFraction(value)
            }.coerceAtMost(maxHeight)
            var dragOffset by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(value) { dragOffset = 0f }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(targetHeight)
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                    .testTag("workspace-sheet"),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
            ) {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(value) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, amount -> dragOffset += amount },
                                    onDragEnd = {
                                        val next = when {
                                            dragOffset < -24f -> when (value) {
                                                WorkspaceSheetLevel.COLLAPSED -> WorkspaceSheetLevel.HALF
                                                WorkspaceSheetLevel.HALF -> WorkspaceSheetLevel.EXPANDED
                                                WorkspaceSheetLevel.EXPANDED -> value
                                            }
                                            dragOffset > 24f -> when (value) {
                                                WorkspaceSheetLevel.EXPANDED -> WorkspaceSheetLevel.HALF
                                                WorkspaceSheetLevel.HALF -> WorkspaceSheetLevel.COLLAPSED
                                                WorkspaceSheetLevel.COLLAPSED -> value
                                            }
                                            else -> value
                                        }
                                        dragOffset = 0f
                                        if (next != value) onValueChange(next)
                                    },
                                    onDragCancel = { dragOffset = 0f },
                                )
                            }
                            .testTag("workspace-sheet-handle"),
                    ) { header() }
                    if (value != WorkspaceSheetLevel.COLLAPSED) content()
                }
            }
        }
    }
}
