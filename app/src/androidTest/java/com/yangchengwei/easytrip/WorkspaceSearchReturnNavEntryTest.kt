package com.yangchengwei.easytrip

import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspaceSearchReturnNavEntryTest {
    @get:Rule val compose = createAndroidComposeRule<WorkspaceSearchReturnTestActivity>()

    @After fun clearActivityContent() {
        compose.runOnIdle {
            compose.activity.intent.putExtra(
                WorkspaceSearchReturnTestActivity.EXTRA_MODE,
                WorkspaceSearchReturnTestActivity.MODE_EMPTY,
            )
            compose.activity.setContent {}
        }
        compose.activityRule.scenario.close()
    }

    @Test fun acknowledgedPayloadSurvivesActivityRecreationInSameEntry() {
        val sessionId = UUID.randomUUID().toString()
        compose.activity.intent
            .putExtra(
                WorkspaceSearchReturnTestActivity.EXTRA_MODE,
                WorkspaceSearchReturnTestActivity.MODE_SEARCH_RETURN,
            )
            .putExtra(WorkspaceSearchReturnTestActivity.EXTRA_SESSION_ID, sessionId)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.activity.workspaceEntry != null && compose.activity.workspaceState != null
        }
        compose.runOnIdle { compose.activity.navigateToSearch() }
        compose.runOnIdle { compose.activity.returnFromSearch(setOf("poi-a")) }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.activity.workspaceState?.value?.recentlyCollectedPoiIds == setOf("poi-a")
        }
        val oldActivity = compose.activity
        val oldEntry = oldActivity.workspaceEntry!!
        val oldState = oldActivity.workspaceState!!
        compose.runOnIdle {
            assertEquals(sessionId, oldActivity.sessionId)
            assertNull(oldEntry.savedStateHandle.get<Array<String>>(WORKSPACE_SEARCH_RETURN_KEY))
            assertNull(consumeWorkspaceSearchReturn(oldEntry.savedStateHandle))
        }

        compose.activityRule.scenario.recreate()

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.activity !== oldActivity &&
                compose.activity.workspaceEntry != null &&
                compose.activity.workspaceState != null
        }
        compose.runOnIdle {
            val recreatedActivity = compose.activity
            val recreatedEntry = recreatedActivity.workspaceEntry!!
            val recreatedState = recreatedActivity.workspaceState!!
            assertEquals(sessionId, recreatedActivity.sessionId)
            assertEquals(oldEntry.id, recreatedEntry.id)
            assertEquals(oldEntry.destination.route, recreatedEntry.destination.route)
            assertSame(oldState, recreatedState)
            assertEquals(setOf("poi-a"), recreatedState.value?.recentlyCollectedPoiIds)
            assertNull(recreatedEntry.savedStateHandle.get<Array<String>>(WORKSPACE_SEARCH_RETURN_KEY))
            assertNull(consumeWorkspaceSearchReturn(recreatedEntry.savedStateHandle))
        }
    }

    @Test fun poppedEntryIsDestroyedAndReenteringCreatesEmptyState() {
        val navRef = AtomicReference<NavHostController?>()
        val enterCount = AtomicInteger()
        val entryRef = AtomicReference<NavBackStackEntry?>()
        val stateRef = AtomicReference<WorkspaceSearchReturnViewModel?>()
        val probeRef = AtomicReference<ClearedProbe?>()
        compose.runOnIdle {
            compose.activity.setContent {
                val nav = rememberNavController()
                navRef.set(nav)
                NavHost(nav, "list") {
                    composable("list") {}
                    composable("workspace/{trip}") { entry ->
                        val state: WorkspaceSearchReturnViewModel = viewModel(viewModelStoreOwner = entry)
                        val probe: ClearedProbe = viewModel(
                            viewModelStoreOwner = entry,
                            factory = ClearedProbe.factory,
                        )
                        LaunchedEffect(entry) { enterCount.incrementAndGet() }
                        entryRef.set(entry)
                        stateRef.set(state)
                        probeRef.set(probe)
                    }
                }
            }
        }
        compose.runOnIdle { navRef.get()!!.navigate("workspace/A") }
        compose.waitUntil(timeoutMillis = 5_000) { enterCount.get() == 1 && stateRef.get() != null }
        val oldEntry = entryRef.get()!!
        val oldState = stateRef.get()!!
        val oldProbe = probeRef.get()!!
        compose.runOnIdle {
            oldState.show(WorkspaceSearchReturn(setOf("poi-a")))
            navRef.get()!!.popBackStack()
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            oldEntry.lifecycle.currentState == Lifecycle.State.DESTROYED && oldProbe.cleared.get()
        }
        entryRef.set(null)
        stateRef.set(null)
        probeRef.set(null)

        compose.runOnIdle { navRef.get()!!.navigate("workspace/A") }
        compose.waitUntil(timeoutMillis = 10_000) {
            enterCount.get() == 2 && entryRef.get() != null && stateRef.get() != null
        }
        compose.runOnIdle {
            assertNotEquals(oldEntry.id, entryRef.get()!!.id)
            assertNotSame(oldState, stateRef.get())
            assertNull(stateRef.get()!!.value)
        }
    }

    @Test fun differentWorkspaceEntriesAreIsolated() {
        val navRef = AtomicReference<NavHostController?>()
        val entries = linkedMapOf<String, NavBackStackEntry>()
        val states = linkedMapOf<String, WorkspaceSearchReturnViewModel>()
        compose.runOnIdle {
            compose.activity.setContent {
                val nav = rememberNavController()
                navRef.set(nav)
                NavHost(nav, "workspace/A") {
                    composable("workspace/{trip}") { entry ->
                        val trip = entry.arguments?.getString("trip")!!
                        entries[trip] = entry
                        states[trip] = viewModel(viewModelStoreOwner = entry)
                    }
                }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) { compose.runOnIdle { states.containsKey("A") } }
        compose.runOnIdle {
            states.getValue("A").show(WorkspaceSearchReturn(setOf("poi-a")))
            navRef.get()!!.navigate("workspace/B")
        }
        compose.waitUntil(timeoutMillis = 5_000) { compose.runOnIdle { states.containsKey("B") } }
        compose.runOnIdle {
            assertNotEquals(entries.getValue("A").id, entries.getValue("B").id)
            assertNotSame(states.getValue("A"), states.getValue("B"))
            assertEquals(setOf("poi-a"), states.getValue("A").value?.recentlyCollectedPoiIds)
            assertNull(states.getValue("B").value)
        }
    }

    private class ClearedProbe : ViewModel() {
        val cleared = AtomicBoolean(false)
        override fun onCleared() {
            cleared.set(true)
        }

        companion object {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ClearedProbe() as T
            }
        }
    }
}
