package com.yangchengwei.easytrip

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceSearchReturnNavigationTest {
    @Test fun returnPayloadIsConsumedExactlyOnce() {
        val handle = SavedStateHandle()
        publishWorkspaceSearchReturn(handle, setOf("poi-1"))

        assertEquals(setOf("poi-1"), consumeWorkspaceSearchReturn(handle)?.recentlyCollectedPoiIds)
        assertNull(consumeWorkspaceSearchReturn(handle))
        assertNull(handle.get<Array<String>>(WORKSPACE_SEARCH_RETURN_KEY))
    }

    @Test fun emptyReturnClearsPreviousVisualState() {
        val handle = SavedStateHandle()
        publishWorkspaceSearchReturn(handle, setOf("poi-old"))
        assertEquals(setOf("poi-old"), consumeWorkspaceSearchReturn(handle)?.recentlyCollectedPoiIds)

        publishWorkspaceSearchReturn(handle, emptySet())

        assertNull(consumeWorkspaceSearchReturn(handle))
        assertNull(handle.get<Array<String>>(WORKSPACE_SEARCH_RETURN_KEY))
    }

    @Test fun laterSearchProducesOnlyItsOwnReturnPayload() {
        val handle = SavedStateHandle()
        publishWorkspaceSearchReturn(handle, setOf("poi-1"))
        consumeWorkspaceSearchReturn(handle)

        publishWorkspaceSearchReturn(handle, setOf("poi-2"))

        assertEquals(setOf("poi-2"), consumeWorkspaceSearchReturn(handle)?.recentlyCollectedPoiIds)
        assertNull(consumeWorkspaceSearchReturn(handle))
    }
}
