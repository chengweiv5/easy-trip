package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSheetSyncTest {
    @Test fun `sheet accepts only three levels and restores valid values`() {
        assertEquals(WorkspaceSheetLevel.COLLAPSED, restoreWorkspaceSheetLevel("COLLAPSED"))
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel("HALF"))
        assertEquals(WorkspaceSheetLevel.EXPANDED, restoreWorkspaceSheetLevel("EXPANDED"))
    }

    @Test fun `invalid stored sheet level safely falls back to half`() {
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel(null))
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel("hidden"))
        assertEquals(WorkspaceSheetLevel.HALF, restoreWorkspaceSheetLevel("FULL"))
    }

    @Test fun `baseline height uses exact v2 anchors with seventy five percent half sheet`() {
        assertEquals(
            WorkspaceSheetAnchors(108.dp, 324.dp, 720.dp),
            workspaceSheetAnchors(782.dp),
        )
    }

    @Test fun `anchors scale below baseline and saturate above it`() {
        assertEquals(
            WorkspaceSheetAnchors(96.dp, 162.dp, 360.dp),
            workspaceSheetAnchors(391.dp),
        )
        assertEquals(
            WorkspaceSheetAnchors(108.dp, 324.dp, 720.dp),
            workspaceSheetAnchors(1_200.dp),
        )
    }

    @Test fun `small windows keep all anchors strictly ordered and bounded`() {
        listOf(48.dp, 108.dp, 280.dp, 422.dp, 782.dp, 843.dp).forEach { height ->
            val anchors = workspaceSheetAnchors(height)
            assertTrue("height=$height anchors=$anchors", anchors.collapsed < anchors.half)
            assertTrue("height=$height anchors=$anchors", anchors.half < anchors.expanded)
            assertTrue("height=$height anchors=$anchors", anchors.expanded <= height)
        }
    }

    @Test fun `usable small window preserves collapsed chrome and summary`() {
        val anchors = workspaceSheetAnchors(280.dp)
        assertEquals(96.dp, anchors.collapsed)
        assertTrue(anchors.collapsed < anchors.half)
        assertTrue(anchors.half < anchors.expanded)
    }

    @Test fun `minimum supported window preserves summary height and ordered anchors`() {
        val anchors = workspaceSheetAnchors(108.dp)

        assertEquals(96.dp, anchors.collapsed)
        assertTrue(anchors.collapsed < anchors.half)
        assertTrue(anchors.half < anchors.expanded)
        assertTrue(anchors.expanded <= 108.dp)
    }

    @Test fun `drag threshold is interpreted in pixels supplied by density`() {
        assertEquals(
            WorkspaceSheetLevel.HALF,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -49f, 48f),
        )
        assertEquals(
            WorkspaceSheetLevel.COLLAPSED,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -47f, 48f),
        )
    }

    @Test fun `drag threshold adapts when compact anchors are closer than standard threshold`() {
        val anchors = WorkspaceSheetAnchors(86.4.dp, 90.75.dp, 99.44.dp)

        assertTrue(workspaceSheetDragThreshold(WorkspaceSheetLevel.HALF, anchors) < 4.35.dp)
        assertTrue(workspaceSheetDragThreshold(WorkspaceSheetLevel.HALF, anchors) > 0.dp)
    }

    @Test fun `drag moves only one adjacent level`() {
        assertEquals(
            WorkspaceSheetLevel.HALF,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.COLLAPSED, -500f, 24f),
        )
        assertEquals(
            WorkspaceSheetLevel.HALF,
            resolveWorkspaceSheetDrag(WorkspaceSheetLevel.EXPANDED, 500f, 24f),
        )
    }

    @Test fun `drag offset is clamped to legal anchor range`() {
        assertEquals(-300f, clampWorkspaceSheetDragOffsetPx(400f, 40f, 700f, -999f))
        assertEquals(360f, clampWorkspaceSheetDragOffsetPx(400f, 40f, 700f, 999f))
    }
}
