package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.SheetValue
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    @Test fun `regular window produces ordered anchors and search return lift`() {
        assertEquals(
            WorkspaceSheetAnchors(34.dp, 396.dp, 712.8.dp),
            workspaceSheetAnchors(792.dp, searchReturn = false),
        )
        assertEquals(412.dp, workspaceSheetAnchors(792.dp, searchReturn = true).half)
    }

    @Test fun `small window produces ordered anchors within available height`() {
        val anchors = workspaceSheetAnchors(280.dp, searchReturn = false)

        assertTrue(anchors.collapsed < anchors.half)
        assertTrue(anchors.half < anchors.expanded)
        assertTrue(anchors.expanded <= 280.dp)
    }

    @Test fun `tiny window degrades within maximum available sheet height`() {
        val anchors = workspaceSheetAnchors(48.dp, searchReturn = true)

        assertTrue(anchors.collapsed < anchors.half)
        assertTrue(anchors.half < anchors.expanded)
        assertTrue(anchors.expanded <= 48.dp)
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

    @Test fun `transitioning sheet does not overwrite requested level`() {
        assertNull(settledWorkspaceSheetLevel(SheetValue.PartiallyExpanded, SheetValue.Hidden, WorkspaceSheetLevel.HALF))
        assertNull(settledWorkspaceSheetLevel(SheetValue.Hidden, SheetValue.PartiallyExpanded, WorkspaceSheetLevel.HALF))
        assertNull(settledWorkspaceSheetLevel(SheetValue.PartiallyExpanded, SheetValue.PartiallyExpanded, WorkspaceSheetLevel.COLLAPSED))
    }

    @Test fun `settled sheet maps to workspace level`() {
        assertEquals(WorkspaceSheetLevel.COLLAPSED, settledWorkspaceSheetLevel(SheetValue.Hidden, SheetValue.Hidden, WorkspaceSheetLevel.HALF))
        assertEquals(WorkspaceSheetLevel.HALF, settledWorkspaceSheetLevel(SheetValue.PartiallyExpanded, SheetValue.PartiallyExpanded, WorkspaceSheetLevel.HALF))
        assertEquals(WorkspaceSheetLevel.EXPANDED, settledWorkspaceSheetLevel(SheetValue.Expanded, SheetValue.Expanded, WorkspaceSheetLevel.HALF))
    }
}
