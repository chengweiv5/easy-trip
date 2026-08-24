package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.SheetValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test fun `three levels have distinct constrained heights`() {
        assertEquals(0f, workspaceSheetFraction(WorkspaceSheetLevel.COLLAPSED))
        assertEquals(0.5f, workspaceSheetFraction(WorkspaceSheetLevel.HALF))
        assertEquals(0.9f, workspaceSheetFraction(WorkspaceSheetLevel.EXPANDED))
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
