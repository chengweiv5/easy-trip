package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspaceSearchReturnNavEntryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun acknowledgedPayloadSurvivesActivityRecreationInSameEntry() {
        val published = AtomicBoolean(false)
        val entryRef = AtomicReference<NavBackStackEntry?>()
        val stateRef = AtomicReference<WorkspaceSearchReturnViewModel?>()
        compose.setContent {
            val nav = rememberNavController()
            NavHost(nav, "workspace/A") {
                composable("workspace/{trip}") { entry ->
                    val state: WorkspaceSearchReturnViewModel = viewModel(viewModelStoreOwner = entry)
                    entryRef.set(entry)
                    stateRef.set(state)
                    LaunchedEffect(entry) {
                        if (published.compareAndSet(false, true)) {
                            publishWorkspaceSearchReturn(entry.savedStateHandle, setOf("poi-a"))
                            state.show(consumeWorkspaceSearchReturn(entry.savedStateHandle))
                        }
                    }
                }
            }
        }
        compose.waitUntil { stateRef.get()?.value != null }
        val oldActivity = compose.activity
        val oldEntryId = entryRef.get()!!.id
        val oldRoute = entryRef.get()!!.destination.route
        val oldState = stateRef.get()!!
        entryRef.set(null)
        stateRef.set(null)

        compose.activityRule.scenario.recreate()

        compose.waitUntil { compose.activity !== oldActivity && entryRef.get() != null && stateRef.get() != null }
        compose.runOnIdle {
            val recreatedEntry = entryRef.get()!!
            val recreatedState = stateRef.get()!!
            assertEquals(oldEntryId, recreatedEntry.id)
            assertEquals(oldRoute, recreatedEntry.destination.route)
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
        compose.setContent {
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
        compose.runOnIdle { navRef.get()!!.navigate("workspace/A") }
        compose.waitUntil { enterCount.get() == 1 && stateRef.get() != null }
        val oldEntry = entryRef.get()!!
        val oldState = stateRef.get()!!
        val oldProbe = probeRef.get()!!
        compose.runOnIdle {
            oldState.show(WorkspaceSearchReturn(setOf("poi-a")))
            navRef.get()!!.popBackStack()
        }
        compose.waitUntil {
            oldEntry.lifecycle.currentState == Lifecycle.State.DESTROYED && oldProbe.cleared.get()
        }
        entryRef.set(null)
        stateRef.set(null)
        probeRef.set(null)

        compose.runOnIdle { navRef.get()!!.navigate("workspace/A") }
        compose.waitUntil { enterCount.get() == 2 && entryRef.get() != null && stateRef.get() != null }
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
        compose.setContent {
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
        compose.waitUntil { compose.runOnIdle { states.containsKey("A") } }
        compose.runOnIdle {
            states.getValue("A").show(WorkspaceSearchReturn(setOf("poi-a")))
            navRef.get()!!.navigate("workspace/B")
        }
        compose.waitUntil { compose.runOnIdle { states.containsKey("B") } }
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
