package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.SheetValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class WorkspaceSheetSyncTest {
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
