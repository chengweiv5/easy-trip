package com.yangchengwei.easytrip

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn
import com.yangchengwei.easytrip.workspace.WorkspaceSection
import com.yangchengwei.easytrip.workspace.shouldConsumeSearchReturn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceSearchReturnNavigationTest {
    @Test fun transientStateSurvivesHostRecreationButNotNewHost() {
        val state = WorkspaceSearchReturnTransientState()
        state.show(WorkspaceSearchReturn(setOf("poi-1")))

        assertEquals(setOf("poi-1"), state.value?.recentlyCollectedPoiIds)
        assertNull(WorkspaceSearchReturnTransientState().value)
    }

    @Test fun selectingCurrentSectionDoesNotConsumeReturnState() {
        assertEquals(false, shouldConsumeSearchReturn(WorkspaceSection.PLACE_POOL, WorkspaceSection.PLACE_POOL))
        assertEquals(true, shouldConsumeSearchReturn(WorkspaceSection.PLACE_POOL, WorkspaceSection.ITINERARY))
    }

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
