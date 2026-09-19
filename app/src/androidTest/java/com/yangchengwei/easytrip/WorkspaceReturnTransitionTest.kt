package com.yangchengwei.easytrip

import android.graphics.Bitmap
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.room.Room
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.InMemoryMapPreferences
import com.yangchengwei.easytrip.workspace.RealAmapMapHost
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

class WorkspaceReturnTransitionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun toolbarReturnDoesNotLeaveFadedWorkspaceControls() = verifyReturn(systemBack = false)
    @Test fun systemReturnDoesNotLeaveFadedWorkspaceControls() = verifyReturn(systemBack = true)
    @Test fun realMapReturnDoesNotLeaveFadedWorkspaceControls() = verifyReturn(systemBack = false, realMap = true)

    private fun verifyReturn(systemBack: Boolean, realMap: Boolean = false) {
        if (realMap) {
            com.amap.api.maps.MapsInitializer.updatePrivacyShow(compose.activity, true, true)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(compose.activity, true)
        }
        val consentStore = AmapConsentStore(
            persistence = object : AmapConsentPersistence {
                override fun readDecision(): Boolean? = null
                override fun writeDecision(accepted: Boolean) = Unit
            },
            reporter = object : AmapPrivacyReporter {
                override suspend fun reportShown() = Unit
                override suspend fun reportDecision(accepted: Boolean) = Unit
            },
            registry = ConsentRegistry(),
        )
        runBlocking {
            consentStore.reportShown().getOrThrow()
            consentStore.decide(true).getOrThrow()
        }
        val consent = (consentStore.state.value.fact as AmapConsentFact.Accepted).token
        val database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val trips = RoomTripRepository(database.tripDao())
        val tripId = runBlocking { trips.createTrip(CreateTrip("返回显示回归", 2)) }
        val places = RoomSavedPlaceRepository(database)
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao())
        val routes = RoomRouteLegRepository(database.routeLegDao())

        compose.setContent {
            EasyTripTheme {
                AppNavigation(
                    service = TripService(trips), repository = trips,
                    impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                    dependencies = AppNavigationDependencies(
                        savedPlaceRepository = places, itineraryRepository = itineraries,
                        routeLegRepository = routes, mapPreferences = InMemoryMapPreferences(),
                        locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                        mapConsentToken = consent,
                        consentStore = consentStore,
                        runtimeSessionFactory = { fact ->
                            AmapRuntimeSession(
                                fact.generation, fact.token,
                                object : com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource {
                                    override suspend fun search(keyword: String, city: String?) = emptyList<com.yangchengwei.easytrip.place.amap.PlaceCandidate>()
                                },
                                object : com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator {
                                    override fun start(scope: kotlinx.coroutines.CoroutineScope) = Unit
                                    override suspend fun retry(legId: String) = false
                                    override suspend fun updateDetails(legId: String, selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?, durationOverrideSeconds: Int?, note: String?) = false
                                },
                            )
                        },
                    ),
                    mapHostFactory = { context -> if (realMap) RealAmapMapHost(context) else object : AmapMapHost {
                        override val view = View(context).apply { setBackgroundColor(0xFFA5B99C.toInt()) }
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                    } },
                )
            }
        }
        try {
            compose.waitUntil(10_000) { compose.onNodeWithTag("primary-trip-$tripId").runCatching { fetchSemanticsNode() }.isSuccess }
            compose.onNodeWithTag("primary-trip-$tripId").performClick()
            compose.waitUntil(10_000) { compose.onNodeWithTag("map-legend").runCatching { fetchSemanticsNode() }.isSuccess }
            compose.waitForIdle()
            compose.onNodeWithTag("workspace-search-launcher").assertIsDisplayed()
            capture("before-$systemBack-$realMap")
            compose.mainClock.autoAdvance = false
            if (systemBack) compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            else compose.onNodeWithContentDescription("返回").performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(150)
            compose.waitForIdle()
            capture("return-150-$systemBack-$realMap")
            compose.onNodeWithTag("trip-list-title").assertIsDisplayed()
            compose.onNodeWithTag("map-legend").assertDoesNotExist()
            compose.onNodeWithTag("workspace-search-launcher").assertDoesNotExist()
        } finally {
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            database.close()
        }
    }

    private fun capture(name: String) {
        val file = File(compose.activity.getExternalFilesDir(null), "return-transition-$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
