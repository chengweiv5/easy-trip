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
import com.yangchengwei.easytrip.assistant.cityfirst.*
import java.util.UUID
import kotlinx.serialization.json.*

/** Production NavHost and recovery; only this test's in-memory DB is written. */
class GlobalAssistantNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun cityFirstRecoveredCreationOpensWorkspaceAndBackRemovesGlobalEntry() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val db = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val container = AppContainer(compose.activity, scope, databaseFactory = { db },
            networkFactory = { _, _ -> object : NetworkMonitor { override val isOnline = MutableStateFlow(false) } })
        check(container.tripCreationJournal.read() == null && container.guideJournal.read() == null)
        val session = UUID.randomUUID().toString()
        try {
            compose.setContent {
                EasyTripTheme {
                    AppNavigation(container.tripService, container.tripRepository, container.deleteImpactProvider,
                        dependencies = AppNavigationDependencies(container.savedPlaceRepository, container.itineraryRepository,
                            container.routeLegRepository, container.mapPreferences, container.locationPermissionRequestStore,
                            assistantContainer = container))
                }
            }
            val command = runBlocking {
                val material = container.guideMaterials.prepare(session, "青州公园", emptyList()) {}
                CreateGuideTripCommand.create("城市优先导航", null, 1, TravelMode.FLEXIBLE,
                    AssistantCityScope.parse("青州"), material.ref).also {
                    container.guideMaterials.withRetained(it.material) { container.guideCreation.commit(it) }
                }
            }
            container.guideJournal.write(buildJsonObject {
                put("version", 1); put("session", session); put("reference", "2026-10-14"); put("zone", "Asia/Shanghai")
                put("input", "青州公园"); put("images", JsonArray(emptyList())); put("cities", "青州")
                put("name", "城市优先导航"); put("date", JsonNull); put("days", 1); put("mode", "FLEXIBLE")
                put("confirming", true); put("materialDigest", command.material.digest); put("pending", command.encode())
            }.toString())
            compose.onNodeWithTag("assistant-create-trip").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("workspace-screen-root").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(10_000) { container.guideJournal.read() == null }
            assertEquals(1, runBlocking { container.tripRepository.observeTrips().first().size })
            assertEquals(0, runBlocking { container.tripRepository.observeTrips().first().single().placeCount })
            assertEquals(listOf("青州"), runBlocking { container.guideCreation.scope(command.tripId) }!!.names)
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("assistant-create-trip").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("global-assistant").assertDoesNotExist()
            runBlocking { container.tripRepository.deleteTrip(command.tripId) }
        } finally {
            container.guideJournal.read()?.let { container.guideJournal.clear(session) }
            compose.activityRule.scenario.close()
            scope.cancel()
            db.close()
        }
    }

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
