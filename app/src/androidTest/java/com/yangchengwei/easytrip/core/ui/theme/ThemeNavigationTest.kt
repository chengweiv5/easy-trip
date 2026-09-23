package com.yangchengwei.easytrip.core.ui.theme

import android.graphics.Bitmap
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import com.yangchengwei.easytrip.AppNavigation
import com.yangchengwei.easytrip.AppNavigationDependencies
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.AmapRuntimeSession
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import com.yangchengwei.easytrip.workspace.*
import java.io.File
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ThemeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun workspaceSwitchKeepsDateCalendarDrawerScrollAndMapInstance(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val trips = RoomTripRepository(db.tripDao(), database=db, isOnline={false})
        val itinerary = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao(), isOnline={false})
        val id = trips.createTrip(CreateTrip("主题切换验证", 3))
        val day = trips.observeTrip(id).first()!!.days[1]
        db.savedPlaceDao().insertPlace(SavedPlaceEntity("lake",id,"lake","西湖天地","杭州",30.25,120.15))
        itinerary.addItem(day.id,"lake",0)
        val store = ThemePreferenceStore(object : ThemePersistence {
            var id: String? = null
            override fun read() = id
            override fun write(id: String) { this.id = id }
        })
        val consentStore = AmapConsentStore(
            object : AmapConsentPersistence {
                override fun readDecision(): Boolean? = true
                override fun writeDecision(accepted: Boolean) = Unit
            },
            object : AmapPrivacyReporter {
                override suspend fun reportShown() = Unit
                override suspend fun reportDecision(accepted: Boolean) = Unit
            }, ConsentRegistry(),
        )
        var creations = 0
        var destroys = 0
        var appliedPalette = ThemePalette.LAKE
        var cameraCommands = 0
        var consumed: Long? = null
        compose.setContent { EasyTripThemeHost(store) {
            AppNavigation(TripService(trips), trips, RoomDeleteImpactProvider(db.deleteImpactDao()),
                dependencies=AppNavigationDependencies(RoomSavedPlaceRepository(db), itinerary, RoomRouteLegRepository(db.routeLegDao()), InMemoryMapPreferences(), InMemoryLocationPermissionRequestStore(), consentStore=consentStore,
                    runtimeSessionFactory={ fact -> AmapRuntimeSession(fact.generation, fact.token,
                        object : com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource {
                            override suspend fun search(keyword: String, city: String?) = emptyList<com.yangchengwei.easytrip.place.amap.PlaceCandidate>()
                        },
                        object : com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator {
                            override fun start(scope: kotlinx.coroutines.CoroutineScope) = Unit
                            override suspend fun retry(legId: String) = false
                            override suspend fun updateDetails(legId: String, selectedModeOverride: com.yangchengwei.easytrip.core.model.TransportMode?, durationOverrideSeconds: Int?, note: String?) = false
                        }) }),
                mapHostFactory={ context ->
                    creations++
                    object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() { destroys++ }
                        override fun setPalette(palette: ThemePalette) { appliedPalette = palette }
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable,MapLayer) -> Unit) {
                            val rendering = viewportRendering(consumed, model.viewportRequest)
                            consumed = rendering.consumedRequestId
                            if (rendering.command != null) cameraCommands++
                        }
                    }
                })
        } }
        try {
            compose.waitUntil(5_000) { compose.onAllNodesWithText("主题切换验证").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("主题切换验证").performClick()
            compose.onNodeWithTag("section-ITINERARY").performClick()
            compose.onNodeWithTag("itinerary-scope-${day.id}").performClick()
            compose.onNodeWithTag("calendar-toggle").performClick()
            compose.onNodeWithTag("calendar-content").assertIsDisplayed()
            val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
            val viewBefore = compose.onNodeWithTag("calendar-grid").getUnclippedBoundsInRoot()
            compose.waitUntil(5_000) { creations > 0 }
            val createdBefore = creations
            val cameraBefore = cameraCommands
            ThemePalette.entries.filter { it != ThemePalette.LAKE }.forEach { palette ->
                compose.onNodeWithTag("workspace-more").performClick()
                compose.onNodeWithTag("more-menu-theme").performScrollTo().performClick()
                compose.onNodeWithTag("theme-option-${palette.id}").performScrollTo().performClick()
                compose.onNodeWithTag("theme-apply").performClick()
                compose.waitUntil(5_000) { store.theme.value == palette && appliedPalette == palette }
                compose.onNodeWithTag("itinerary-scope-${day.id}").assertIsSelected()
                compose.onNodeWithTag("calendar-content").assertIsDisplayed()
                assertEquals(sheet, compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot())
                assertEquals(viewBefore, compose.onNodeWithTag("calendar-grid").getUnclippedBoundsInRoot())
                assertEquals(createdBefore, creations)
                assertEquals(0, destroys)
                assertEquals(cameraBefore, cameraCommands)
                val dir=File(compose.activity.getExternalFilesDir(null),"themes").apply { mkdirs() }
                File(dir,"workspace-${palette.id}.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
            }
            compose.onNodeWithTag("workspace-more").performClick()
            compose.onNodeWithTag("more-menu-theme").assertIsDisplayed()
            compose.onNodeWithText("玫瑰沙丘 · 所有旅行").assertIsDisplayed()
            compose.onNodeWithTag("more-menu-back-to-trips").performScrollTo().performClick()
            compose.onNodeWithTag("theme-entry").performClick()
            compose.onNodeWithText("已使用玫瑰沙丘").assertIsDisplayed()
            assertEquals(3, trips.observeTrip(id).first()!!.days.size)
            assertEquals(1, db.savedPlaceDao().observePlaces(id).first().size)
        } finally {
            compose.activityRule.scenario.close()
            db.close()
        }
    }
}
