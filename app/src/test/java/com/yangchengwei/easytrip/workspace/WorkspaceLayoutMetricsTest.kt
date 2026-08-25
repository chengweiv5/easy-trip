package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLayoutMetricsTest {
    @Test fun `sheet top and overlay inset share the selected anchor`() {
        val anchors = WorkspaceSheetAnchors(34.dp, 396.dp, 712.8.dp)
        val metrics = workspaceLayoutMetrics(
            availableHeight = 792.dp,
            anchors = anchors,
            level = WorkspaceSheetLevel.HALF,
            overlayGap = 20.dp,
        )

        assertEquals(396.dp, metrics.sheetHeight)
        assertEquals(396.dp, metrics.sheetTop)
        assertEquals(416.dp, metrics.overlayBottomInset)
    }

    @Test fun `layout metrics never emit negative viewport`() {
        val metrics = workspaceLayoutMetrics(
            availableHeight = 320.dp,
            anchors = WorkspaceSheetAnchors(34.dp, 240.dp, 288.dp),
            level = WorkspaceSheetLevel.EXPANDED,
            overlayGap = 20.dp,
        )

        assertTrue(metrics.sheetTop >= 0.dp)
    }
}
