package com.yangchengwei.easytrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class WorkspaceSearchReturnTestActivity : ComponentActivity() {
    internal var navController: NavHostController? = null
    internal var workspaceEntry: NavBackStackEntry? = null
    internal var workspaceState: WorkspaceSearchReturnViewModel? = null
    internal var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        if (intent.getStringExtra(EXTRA_MODE) == MODE_SEARCH_RETURN) {
            setContent { SearchReturnHarness() }
        }
    }

    internal fun navigateToSearch() {
        navController!!.navigate("workspace/A/search")
    }

    internal fun returnFromSearch(poiIds: Set<String>) {
        val nav = navController!!
        publishWorkspaceSearchReturn(nav.previousBackStackEntry!!.savedStateHandle, poiIds)
        check(nav.popBackStack())
    }

    @androidx.compose.runtime.Composable
    private fun SearchReturnHarness() {
        val nav = rememberNavController()
        navController = nav
        NavHost(nav, "workspace/A") {
            composable("workspace/{trip}") { entry ->
                val state: WorkspaceSearchReturnViewModel = viewModel(viewModelStoreOwner = entry)
                val payload by entry.savedStateHandle
                    .getStateFlow<Array<String>?>(WORKSPACE_SEARCH_RETURN_KEY, null)
                    .collectAsStateWithLifecycle()
                workspaceEntry = entry
                workspaceState = state
                LaunchedEffect(payload) {
                    if (payload != null) state.show(consumeWorkspaceSearchReturn(entry.savedStateHandle))
                }
            }
            composable("workspace/{trip}/search") {}
        }
    }

    companion object {
        const val EXTRA_MODE = "workspaceSearchReturnTestMode"
        const val EXTRA_SESSION_ID = "workspaceSearchReturnTestSessionId"
        const val MODE_SEARCH_RETURN = "searchReturn"
        const val MODE_EMPTY = "empty"
    }
}
