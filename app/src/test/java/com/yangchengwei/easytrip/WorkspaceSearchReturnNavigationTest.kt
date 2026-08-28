package com.yangchengwei.easytrip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.yangchengwei.easytrip.amap.AmapConsentFact
import com.yangchengwei.easytrip.amap.AmapConsentPersistence
import com.yangchengwei.easytrip.amap.AmapConsentStore
import com.yangchengwei.easytrip.amap.AmapPrivacyReporter
import com.yangchengwei.easytrip.amap.ConsentRegistry
import com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn
import com.yangchengwei.easytrip.workspace.WorkspaceSection
import com.yangchengwei.easytrip.workspace.shouldConsumeSearchReturn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test fun supersededConsentDecisionDoesNotRunNavigationSuccessEffects() {
        var successRuns = 0
        var failure: String? = null

        applyConsentDecision(
            result = Result.failure(IllegalStateException("superseded")),
            error = null,
            onSuccess = { successRuns++ },
            onFailure = { failure = it },
        )

        assertEquals(0, successRuns)
        assertNull(failure)
    }

    @Test fun failedConsentDecisionKeepsNavigationSuccessEffectsClosed() = runTest {
        val persistence = MemoryConsentPersistence()
        val reporter = FailingDecisionReporter()
        val store = AmapConsentStore(persistence, reporter, ConsentRegistry())
        var sourceEnabled = false
        var consentClosed = false
        var displayedError: String? = null

        applyConsentDecision(
            result = store.decide(true),
            error = store.state.value.error,
            onSuccess = {
                sourceEnabled = true
                consentClosed = true
            },
            onFailure = { displayedError = it },
        )

        assertEquals(false, sourceEnabled)
        assertEquals(false, consentClosed)
        assertTrue(store.state.value.fact is AmapConsentFact.Undecided)
        assertEquals("地图授权更新失败，请重试", displayedError)
    }

    private class MemoryConsentPersistence : AmapConsentPersistence {
        override fun readDecision(): Boolean? = null

        override fun writeDecision(accepted: Boolean) = Unit
    }

    private class FailingDecisionReporter : AmapPrivacyReporter {
        override suspend fun reportShown() = Unit

        override suspend fun reportDecision(accepted: Boolean) {
            throw IllegalStateException("privacy update failed")
        }
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
