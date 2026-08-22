package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceTabRestorationTest {
    @Test fun `legacy search tab restores as places`() {
        assertEquals(WorkspaceTab.PLACES, restoreWorkspaceTab("SEARCH"))
    }

    @Test fun `known itinerary tab is retained`() {
        assertEquals(WorkspaceTab.ITINERARY, restoreWorkspaceTab("ITINERARY"))
    }
}
