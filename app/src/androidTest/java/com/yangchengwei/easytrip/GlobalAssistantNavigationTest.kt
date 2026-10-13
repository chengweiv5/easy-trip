package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import com.yangchengwei.easytrip.assistant.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.core.network.NetworkMonitor
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

/** Production NavHost and recovery; only this test's in-memory DB is written. */
class GlobalAssistantNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun restoredCommitOpensNewWorkspaceAndBackDoesNotReturnToDraft() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val db = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val container = AppContainer(compose.activity, scope, databaseFactory = { db },
            networkFactory = { _, _ -> object : NetworkMonitor { override val isOnline = MutableStateFlow(false) } })
        check(container.tripCreationJournal.read() == null) { "Test must not overwrite an existing pending operation" }
        val command = TripCreationCommand.create("导航恢复测试", null, 1, TravelMode.FLEXIBLE,
            listOf(ImportItem("A", 0, PlaceCandidate("test-navigation", "测试地点", "测试地址",
                GeoPoint(30.2, 120.1), null, "杭州"), PlaceCategory.ATTRACTION)), emptyList())
        try {
            compose.setContent {
                EasyTripTheme {
                    AppNavigation(container.tripService, container.tripRepository, container.deleteImpactProvider,
                        dependencies = AppNavigationDependencies(container.savedPlaceRepository, container.itineraryRepository,
                            container.routeLegRepository, container.mapPreferences, container.locationPermissionRequestStore,
                            assistantContainer = container))
                }
            }
            compose.onNodeWithTag("assistant-create-trip").assertIsDisplayed()
            runBlocking { container.tripCreation.commit(command) }
            container.tripCreationJournal.write(command)
            compose.onNodeWithTag("assistant-create-trip").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("workspace-screen-root").fetchSemanticsNodes().isNotEmpty() }
            assertNull(container.tripCreationJournal.read())
            assertEquals(1, runBlocking { container.tripRepository.observeTrips().first().size })
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("assistant-create-trip").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("global-assistant").assertDoesNotExist()
        } catch (failure: Throwable) {
            java.io.File(compose.activity.filesDir, "v34-navigation-failure.txt").writeText(
                compose.onRoot(useUnmergedTree = true).printToString() +
                    "\npending=" + (container.tripCreationJournal.read() != null))
            throw failure
        } finally {
            container.tripCreationJournal.read()?.takeIf { it == command }?.let(container.tripCreationJournal::clear)
            compose.activityRule.scenario.close()
            scope.cancel()
            db.close()
        }
    }
}
