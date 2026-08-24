package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.SheetValue
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

    @Test fun `regular window keeps design heights and search return lift`() {
        assertEquals(34f, workspaceSheetHeightDp(792f, WorkspaceSheetLevel.COLLAPSED, false))
        assertEquals(396f, workspaceSheetHeightDp(792f, WorkspaceSheetLevel.HALF, false))
        assertEquals(412f, workspaceSheetHeightDp(792f, WorkspaceSheetLevel.HALF, true))
        assertEquals(712.8f, workspaceSheetHeightDp(792f, WorkspaceSheetLevel.EXPANDED, false))
    }

    @Test fun `small window gives half sheet minimum content height while keeping levels distinct`() {
        assertEquals(34f, workspaceSheetHeightDp(280f, WorkspaceSheetLevel.COLLAPSED, false))
        assertEquals(240f, workspaceSheetHeightDp(280f, WorkspaceSheetLevel.HALF, false))
        assertEquals(252f, workspaceSheetHeightDp(280f, WorkspaceSheetLevel.EXPANDED, false))
    }

    @Test fun `tiny window degrades within maximum available sheet height`() {
        WorkspaceSheetLevel.entries.forEach { level ->
            val height = workspaceSheetHeightDp(48f, level, searchReturn = true)
            assertTrue("level=$level height=$height", height in 0f..43.2f)
        }
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
