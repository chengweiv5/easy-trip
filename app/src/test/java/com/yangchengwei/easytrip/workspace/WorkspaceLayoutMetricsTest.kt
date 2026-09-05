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

    @Test fun `viewport safe insets mirror asymmetric workspace chrome and sheet anchor`() {
        val metrics = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 432.dp,
        )

        assertEquals(
            MapViewportInsets(leftPx = 20, topPx = 72, rightPx = 68, bottomPx = 510),
            workspaceViewportInsets(metrics, density = 1f),
        )
    }

    @Test fun `viewport safe insets use stable anchor instead of live drag height`() {
        val metrics = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            sheetHeight = 320.dp,
            sheetTop = 462.dp,
            overlayBottomInset = 340.dp,
            stableSheetHeight = 432.dp,
        )

        assertEquals(
            MapViewportInsets(leftPx = 20, topPx = 72, rightPx = 68, bottomPx = 510),
            workspaceViewportInsets(metrics, density = 1f),
        )
    }

    @Test fun `viewport safe insets stay identical across drag frames until level settles`() {
        val firstFrame = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 320.dp,
        ).copy(stableSheetHeight = 432.dp)
        val secondFrame = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 380.dp,
        ).copy(stableSheetHeight = 432.dp)
        val settled = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 432.dp,
        ).copy(stableSheetHeight = 432.dp)

        assertEquals(
            workspaceViewportInsets(firstFrame, density = 1f),
            workspaceViewportInsets(secondFrame, density = 1f),
        )
        assertEquals(
            workspaceViewportInsets(secondFrame, density = 1f),
            workspaceViewportInsets(settled, density = 1f),
        )
    }

    @Test fun `viewport safe insets clamp expanded sheet to a positive map rectangle`() {
        val metrics = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            sheetHeight = 720.dp,
            sheetTop = 62.dp,
            overlayBottomInset = 740.dp,
            stableSheetHeight = 720.dp,
        )

        val insets = workspaceViewportInsets(metrics, density = 1f)

        assertTrue(insets.leftPx >= 0 && insets.topPx >= 0 && insets.rightPx >= 0 && insets.bottomPx >= 0)
        assertTrue(insets.leftPx + insets.rightPx <= 389)
        assertTrue(insets.topPx + insets.bottomPx <= 781)
    }

    @Test fun `viewport safe insets clamp tiny windows without inverting map rectangle`() {
        val metrics = WorkspaceLayoutMetrics(
            availableWidth = 100.dp,
            availableHeight = 90.dp,
            sheetHeight = 84.dp,
            sheetTop = 6.dp,
            overlayBottomInset = 104.dp,
            stableSheetHeight = 84.dp,
        )

        val insets = workspaceViewportInsets(metrics, density = 1f)

        assertTrue(insets.leftPx >= 0 && insets.topPx >= 0 && insets.rightPx >= 0 && insets.bottomPx >= 0)
        assertTrue(insets.leftPx + insets.rightPx <= 99)
        assertTrue(insets.topPx + insets.bottomPx <= 89)
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

    @Test fun `place detail sheet follows design ratio with bounded responsive height`() {
        assertEquals(490.dp, workspacePlaceDetailSheetHeight(782.dp))
        assertEquals(320.dp, workspacePlaceDetailSheetHeight(500.dp))
        assertEquals(560.dp, workspacePlaceDetailSheetHeight(1_000.dp))
    }

    @Test fun `place detail sheet uses the design top corner radius`() {
        assertEquals(20.dp, workspacePlaceDetailSheetTopCornerRadius)
    }

    @Test fun `place detail sheet fills extremely short workspace without invalid height`() {
        assertEquals(0.dp, workspacePlaceDetailSheetHeight(0.dp))
        assertEquals(32.dp, workspacePlaceDetailSheetHeight(32.dp))
        assertEquals(48.dp, workspacePlaceDetailSheetHeight(48.dp))
        assertEquals(96.dp, workspacePlaceDetailSheetHeight(96.dp))
        assertEquals(120.dp, workspacePlaceDetailSheetHeight(120.dp))
        assertEquals(400.dp, workspacePlaceDetailSheetHeight(400.dp))
    }

    @Test fun `workspace root layers content before an overlay only when ready`() {
        assertEquals(
            listOf(WorkspaceRootLayer.Content),
            workspaceRootLayers(hasReadyOverlay = false),
        )
        assertEquals(
            listOf(WorkspaceRootLayer.Content, WorkspaceRootLayer.Overlay),
            workspaceRootLayers(hasReadyOverlay = true),
        )
    }
}
