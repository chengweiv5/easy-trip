package com.yangchengwei.easytrip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn
import com.yangchengwei.easytrip.workspace.WorkspaceSection
import com.yangchengwei.easytrip.workspace.shouldConsumeSearchReturn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceSearchReturnNavigationTest {
    @Test fun entryScopedStateSurvivesOwnerRecreationButNotNewEntry() {
        val entryA = TestOwner()
        val first = ViewModelProvider(entryA)[WorkspaceSearchReturnViewModel::class.java]
        first.show(WorkspaceSearchReturn(setOf("poi-a")))

        val recreated = ViewModelProvider(entryA)[WorkspaceSearchReturnViewModel::class.java]
        val entryB = TestOwner()
        val otherEntry = ViewModelProvider(entryB)[WorkspaceSearchReturnViewModel::class.java]

        assertEquals(setOf("poi-a"), recreated.value?.recentlyCollectedPoiIds)
        assertNull(otherEntry.value)
    }

    @Test fun clearingEntryAOnExitDoesNotAffectEntryB() {
        val entryA = TestOwner()
        val entryB = TestOwner()
        val stateA = ViewModelProvider(entryA)[WorkspaceSearchReturnViewModel::class.java]
        val stateB = ViewModelProvider(entryB)[WorkspaceSearchReturnViewModel::class.java]
        stateA.show(WorkspaceSearchReturn(setOf("poi-a")))
        stateB.show(WorkspaceSearchReturn(setOf("poi-b")))

        stateA.clear()

        assertNull(stateA.value)
        assertEquals(setOf("poi-b"), stateB.value?.recentlyCollectedPoiIds)
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

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
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
