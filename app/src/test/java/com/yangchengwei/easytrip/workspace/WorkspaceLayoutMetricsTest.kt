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

    @Test fun `map controls and bottom search hide before they overlap`() {
        val tooShort = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            sheetHeight = 569.dp,
            sheetTop = 213.dp,
            overlayBottomInset = 589.dp,
        )
        val fitting = tooShort.copy(sheetTop = 226.dp)

        assertFalse(workspaceMapOverlaysFit(tooShort))
        assertTrue(workspaceMapOverlaysFit(fitting))
    }

    @Test fun `viewport safe insets avoid compact chrome legend controls and half sheet`() {
        val metrics = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 324.dp,
        )

        assertEquals(
            MapViewportInsets(leftPx = 132, topPx = 76, rightPx = 40, bottomPx = 368),
            workspaceViewportInsets(metrics, density = 1f),
        )
    }

    @Test fun `locate uses live drawer occlusion while fitting retains stable control margins`() {
        val metrics = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 240.dp,
        ).copy(stableSheetHeight = 324.dp)

        val firstFrame = workspaceMapLayout(metrics, density = 2.75f)
        val nextFrame = workspaceMapLayout(metrics.copy(sheetHeight = 280.dp), density = 2.75f)

        assertEquals(MapViewportInsets(bottomPx = 660), firstFrame.visibleInsets)
        assertEquals(MapViewportInsets(bottomPx = 770), nextFrame.visibleInsets)
        assertEquals(firstFrame.fitInsets, nextFrame.fitInsets)
    }

    @Test fun `viewport safe rectangle stays clear of bottom left legend`() {
        val metrics = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 324.dp,
        )

        val insets = workspaceViewportInsets(metrics, density = 1f)
        val safeLeft = insets.leftPx
        val safeBottom = 782 - insets.bottomPx
        val legendTop = 782 - 324 - 12 - 32

        assertTrue("safeLeft=$safeLeft", safeLeft >= 12)
        assertTrue("safeBottom=$safeBottom legendTop=$legendTop", safeBottom <= legendTop)
    }

    @Test fun `viewport safe insets use stable anchor instead of live drag height`() {
        val metrics = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            sheetHeight = 240.dp,
            sheetTop = 542.dp,
            overlayBottomInset = 260.dp,
            stableSheetHeight = 324.dp,
        )

        assertEquals(
            MapViewportInsets(leftPx = 132, topPx = 76, rightPx = 40, bottomPx = 368),
            workspaceViewportInsets(metrics, density = 1f),
        )
    }

    @Test fun `drag frame viewport retains the stable sheet anchor`() {
        val metrics = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            sheetHeight = 240.dp,
            sheetTop = 542.dp,
            overlayBottomInset = 260.dp,
            stableSheetHeight = 324.dp,
        )

        val insets = workspaceViewportInsets(metrics, density = 1f)
        val safeBottom = 782 - insets.bottomPx
        val stableLegendTop = 782 - 324 - 12 - 32

        assertTrue("safeBottom=$safeBottom stableLegendTop=$stableLegendTop", safeBottom <= stableLegendTop)
    }

    @Test fun `drag frame legend and search remain twelve dp above the live sheet`() {
        val metrics = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            sheetHeight = 400.dp,
            sheetTop = 382.dp,
            overlayBottomInset = 420.dp,
            stableSheetHeight = 324.dp,
        )

        assertEquals(338.dp, workspaceLegendTop(metrics))
        assertEquals(metrics.sheetTop, workspaceLegendTop(metrics) + 32.dp + 12.dp)
    }

    @Test fun `viewport safe insets stay identical across drag frames until level settles`() {
        val firstFrame = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 240.dp,
        ).copy(stableSheetHeight = 324.dp)
        val secondFrame = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 280.dp,
        ).copy(stableSheetHeight = 324.dp)
        val settled = workspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            visibleSheetHeight = 324.dp,
        ).copy(stableSheetHeight = 324.dp)

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
            availableHeight = 423.dp,
            sheetHeight = 0.dp,
            sheetTop = 423.dp,
            overlayBottomInset = 20.dp,
        )
        val tooNarrow = tooShort.copy(availableWidth = 255.dp, availableHeight = 782.dp, sheetTop = 782.dp)
        val baseline = tooNarrow.copy(availableWidth = 256.dp)

        assertFalse(workspaceLayerMenuFits(tooShort))
        assertFalse(workspaceLayerMenuFits(tooNarrow))
        assertTrue(workspaceLayerMenuFits(baseline))
    }

    @Test fun `layer menu hides when the live sheet would cover its bottom`() {
        val metrics = WorkspaceLayoutMetrics(
            availableWidth = 390.dp,
            availableHeight = 782.dp,
            sheetHeight = 360.dp,
            sheetTop = 422.dp,
            overlayBottomInset = 380.dp,
        )

        assertFalse(workspaceLayerMenuFits(metrics))
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
