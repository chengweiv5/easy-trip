package com.yangchengwei.easytrip.assistant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.workspace.*
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GuideWorkspaceHandoffTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun workspaceOpensAssistantAndWaitsForReadinessThenClaimsOnlyOnce() {
        val context = compose.activity
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val root = File(context.noBackupFilesDir, "guide-workspace-test-${UUID.randomUUID()}")
        val config = AssistantConfigStore(context)
        val original = config.read()
        val stores = ViewModelStore()
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val creation = RoomGuideCreation(db)
        val links = object : AssistantLinkLoader {
            override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink = error("No link")
            override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) = Unit
        }
        val materials = GuideMaterialStore(File(root, "material"), images, links, db.guideCreationDao()::referencedMaterials)
        val parsed = AtomicInteger()
        val response = CompletableDeferred<List<PlaceIntent>>()
        lateinit var assistant: PlaceAssistantViewModel
        lateinit var workspace: TripWorkspaceViewModel
        var mapReadyListener: (() -> Unit)? = null
        try {
            config.save(ProviderConfig("https://example.invalid", "fixture", "not-real"))
            val command = runBlocking {
                val prepared = materials.prepare(UUID.randomUUID().toString(), "公园", emptyList()) {}
                CreateGuideTripCommand.create("青州之旅", null, 1, TravelMode.FLEXIBLE, AssistantCityScope.parse("青州"), prepared.ref)
                    .also { creation.commit(it) }
            }
            compose.runOnUiThread {
                val places = RoomSavedPlaceRepository(db)
                workspace = TripWorkspaceViewModel(command.tripId, RoomTripRepository(db.tripDao(), database = db), places,
                    RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao()),
                    RoomRouteLegRepository(db.routeLegDao()), SavedStateHandle())
                assistant = PlaceAssistantViewModel(command.tripId, config, RoomPlaceImport(db), places,
                    parser = PlaceIntentParser { _, city ->
                        assertEquals("青州", city); parsed.incrementAndGet(); response.await()
                    }, images = AndroidAssistantImageStore(context, File(root, "workspace")),
                    guideStore = creation, guideMaterials = materials, guideResults = GuideImportResultStore(File(root, "results")))
                assistant.controller.environment(true, true, false)
                stores.put("assistant", assistant); stores.put("workspace", workspace)
            }
            val token = TestConsentGate().run { show(); requireNotNull(decide(true)) }
            val permissions = LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore())
            compose.setContent {
                EasyTripTheme {
                    TripWorkspaceRoute(workspace, token, onBack = {}, onSettings = {},
                        assistantViewModel = assistant, locationPermissionCoordinator = permissions,
                        locationPermissionSnapshot = { LocationPermissionSnapshot(false, false) }, onWorkspaceEffect = {},
                        resolveAssistantTripCity = { "北京" },
                        mapHostFactory = { context -> object : AmapMapHost {
                            override val view = android.view.View(context)
                            override fun setOnReadyListener(listener: (() -> Unit)?) { mapReadyListener = listener }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                        } })
                }
            }
            compose.waitUntil(10_000) { assistant.open.value && assistant.guide.value != null }
            assertEquals(GuideHandoffStatus.PENDING, runBlocking { creation.find(command.tripId) }!!.status)
            assertEquals(0, parsed.get())
            compose.onNodeWithTag("assistant-panel").assertIsDisplayed()
            compose.runOnUiThread { requireNotNull(mapReadyListener).invoke() }
            compose.waitUntil(10_000) { parsed.get() == 1 }
            compose.runOnUiThread { repeat(3) { assistant.consumePendingGuide() } }
            compose.waitForIdle()
            assertEquals(1, parsed.get())
            assertEquals("青州", assistant.controller.state.value.city)
            assertEquals(GuideHandoffStatus.CLAIMED, runBlocking { creation.find(command.tripId) }!!.status)
            compose.runOnUiThread { assistant.controller.cancel() }
        } finally {
            response.cancel()
            compose.runOnUiThread { stores.clear() }
            runBlocking { delay(250) }
            if (original == null) config.clear() else config.save(original)
            db.close(); root.deleteRecursively()
        }
    }
}
