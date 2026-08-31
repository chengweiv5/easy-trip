package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun `live visible sheet height drives every overlay metric`() {
        val metrics = workspaceLayoutMetrics(
            availableHeight = 792.dp,
            visibleSheetHeight = 476.dp,
            overlayGap = 20.dp,
        )

        assertEquals(476.dp, metrics.sheetHeight)
        assertEquals(316.dp, metrics.sheetTop)
        assertEquals(496.dp, metrics.overlayBottomInset)
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

    @Test fun `map overlays require enough space above sheet and below top bar`() {
        val tooShort = workspaceLayoutMetrics(443.dp, workspaceSheetAnchors(443.dp), WorkspaceSheetLevel.HALF)
        val baseline = workspaceLayoutMetrics(782.dp, workspaceSheetAnchors(782.dp), WorkspaceSheetLevel.HALF)

        assertFalse(workspaceMapOverlaysFit(tooShort))
        assertTrue(workspaceMapOverlaysFit(baseline))
    }

    @Test fun `layer menu requires safe workspace width and height`() {
        val tooShort = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 428.dp,
            sheetHeight = 0.dp,
            sheetTop = 428.dp,
            overlayBottomInset = 20.dp,
        )
        val tooNarrow = tooShort.copy(availableWidth = 255.dp, availableHeight = 782.dp, sheetTop = 782.dp)
        val baseline = tooNarrow.copy(availableWidth = 256.dp)

        assertFalse(workspaceLayerMenuFits(tooShort))
        assertFalse(workspaceLayerMenuFits(tooNarrow))
        assertTrue(workspaceLayerMenuFits(baseline))
    }
}
