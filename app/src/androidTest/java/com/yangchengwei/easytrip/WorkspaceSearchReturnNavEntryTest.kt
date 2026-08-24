package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yangchengwei.easytrip.workspace.WorkspaceSearchReturn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class WorkspaceSearchReturnNavEntryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun payloadIsAckedIntoEntryViewModelAndSurvivesActivityRecreation() {
        var state: WorkspaceSearchReturnViewModel? = null
        compose.setContent {
            val nav = rememberNavController()
            NavHost(nav, "workspace/A") {
                composable("workspace/{trip}") { entry ->
                    state = viewModel(viewModelStoreOwner = entry)
                    LaunchedEffect(Unit) {
                        publishWorkspaceSearchReturn(entry.savedStateHandle, setOf("poi-a"))
                        state!!.show(consumeWorkspaceSearchReturn(entry.savedStateHandle))
                    }
                }
            }
        }
        compose.waitUntil { state?.value != null }
        compose.activityRule.scenario.recreate()
        compose.waitUntil { state != null }
        compose.runOnIdle { assertEquals(setOf("poi-a"), state?.value?.recentlyCollectedPoiIds) }
    }

    @Test fun poppedEntryIsDestroyedAndReenteringStartsEmpty() {
        var phase = 0
        var current: WorkspaceSearchReturn? = null
        compose.setContent {
            val nav = rememberNavController()
            NavHost(nav, "list") {
                composable("list") {
                    LaunchedEffect(phase) {
                        if (phase == 0) { phase = 1; nav.navigate("workspace/A") }
                        else if (phase == 2) nav.navigate("workspace/A")
                    }
                }
                composable("workspace/{trip}") { entry ->
                    val state: WorkspaceSearchReturnViewModel = viewModel(viewModelStoreOwner = entry)
                    current = state.value
                    LaunchedEffect(phase) {
                        if (phase == 1) {
                            state.show(WorkspaceSearchReturn(setOf("poi-a")))
                            phase = 2
                            nav.popBackStack()
                        }
                    }
                }
            }
        }
        compose.waitUntil { phase == 2 && current == null }
        compose.runOnIdle { assertNull(current) }
    }

    @Test fun differentWorkspaceEntriesAreIsolated() {
        val values = mutableMapOf<String, WorkspaceSearchReturnViewModel>()
        compose.setContent {
            val nav = rememberNavController()
            NavHost(nav, "workspace/A") {
                composable("workspace/{trip}") { entry ->
                    val trip = entry.arguments?.getString("trip")!!
                    values[trip] = viewModel(viewModelStoreOwner = entry)
                    LaunchedEffect(trip) {
                        if (trip == "A") {
                            values.getValue("A").show(WorkspaceSearchReturn(setOf("poi-a")))
                            nav.navigate("workspace/B")
                        }
                    }
                }
            }
        }
        compose.waitUntil { values.keys.containsAll(setOf("A", "B")) }
        compose.runOnIdle {
            assertEquals(setOf("poi-a"), values.getValue("A").value?.recentlyCollectedPoiIds)
            assertNull(values.getValue("B").value)
        }
    }
}
