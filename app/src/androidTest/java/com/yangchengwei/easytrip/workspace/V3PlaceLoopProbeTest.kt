package com.yangchengwei.easytrip.workspace

import android.graphics.Bitmap
import android.os.Process
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.amap.api.maps.MapView
import com.yangchengwei.easytrip.*
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.*
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.TripService
import com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Throwaway v3 feasibility probe, NOT the production Agent runtime.
 * Dedicated runner only. Real AMap search/rendering + existing App confirmation/save/Flow.
 * Each run uses a separate file-backed test database; never opens the application's database.
 */
class V3PlaceLoopProbeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun probe() {
        val args = InstrumentationRegistry.getArguments()
        val token = args.getString("amapRunToken").orEmpty()
        val probeId = args.getString("probeId").orEmpty()
        assumeTrue(
            "Opt-in probe: use the dedicated runner, never the ordinary connected suite",
            token.isNotBlank() && probeId.isNotBlank(),
        )
        require(token.matches(Regex("amap-[A-Za-z0-9._-]+"))) { "Dedicated runner required" }
        require(probeId.matches(Regex("v3-[A-Za-z0-9._-]+")))
        val stage = args.getString("probeStage").orEmpty()
        require(stage in setOf("query", "confirm", "restart"))
        val context = compose.activity.applicationContext
        val toolCall = if (probeId.startsWith("v3-agent-")) {
            JSONObject(File(context.filesDir, "$probeId-tool.json").readText()).also {
                check(it.getString("name") == "search_places")
                check(it.getString("tool_call_id").isNotBlank())
            }
        } else null
        val keyword = toolCall?.getJSONObject("arguments")?.getString("query")
            ?: args.getString("probeQuery").orEmpty()
        val city = toolCall?.getJSONObject("arguments")?.getString("city")
            ?: args.getString("probeCity").orEmpty()
        require(keyword == "雷峰塔" && city == "杭州") { "Approved single-place scope only" }
        val directory = File(context.filesDir, probeId).apply { mkdirs() }
        val report = JSONObject().put("stage", stage).put("probe_id", probeId)
            .put("pid", Process.myPid()).put("started_at", System.currentTimeMillis())
            .put("query", keyword).put("city", city).put("passed", false)
        toolCall?.let { report.put("model_tool_call_id", it.getString("tool_call_id")) }
        fun persist() = File(directory, "$stage.json").writeText(report.toString(2))
        fun screenshot(name: String) {
            // Native MapView draws asynchronously after Compose and map-loaded callbacks.
            android.os.SystemClock.sleep(5_000)
            val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            assertNotNull(image)
            File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        persist()
        runBlocking { AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) } }
        val consent = TestConsentGate().run { show(); requireNotNull(decide(true)) }
        val realSource = AmapPlaceDataSource(context, consent)
        if (stage == "query") {
            check(!File(directory, "candidates.json").exists()) { "One-shot query already consumed" }
            File(directory, "QUERY_STARTED").createNewFile().also { check(it) }
            val candidates = runBlocking { withTimeout(30_000) { realSource.search(keyword, city) } }
            assertTrue("Real AMap returned no POIs", candidates.isNotEmpty())
            val output = JSONArray(candidates.map(::candidateJson))
            File(directory, "candidates.json").writeText(output.toString(2))
            report.put("source", "AmapPlaceDataSource / live SDK").put("candidate_count", candidates.size)
                .put("saved_count", 0).put("passed", true)
            persist()
            return
        }
        val candidateArray = JSONArray(File(directory, "candidates.json").readText())
        val candidates = (0 until candidateArray.length()).map { candidateFromJson(candidateArray.getJSONObject(it)) }
        val selectedPoi = args.getString("selectedPoi").orEmpty()
        val selected = candidates.single { it.poiId == selectedPoi }
        // SDK search may omit administrative codes; never invent missing metadata.
        check(selected.cityName == "杭州市")
        check(selected.cityAdCode == null || selected.cityAdCode == "330100")
        check(selected.name.contains("雷峰塔") && selected.point != null)
        val databaseName = "$probeId.db"
        val databaseFile = context.getDatabasePath(databaseName)
        check(if (stage == "confirm") !databaseFile.exists() else databaseFile.exists())
        val database = Room.databaseBuilder(context, EasyTripDatabase::class.java, databaseName).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val trips = RoomTripRepository(database.tripDao())
        val places = RoomSavedPlaceRepository(database)
        val tripId = "$probeId-trip"
        val models = CopyOnWriteArrayList<MapUiModel>()
        val hosts = CopyOnWriteArrayList<RealAmapMapHost>()
        val loaded = AtomicBoolean(false)
        fun saved() = runBlocking { places.observePlaces(tripId, emptySet()).first() }
        try {
            if (stage == "confirm") runBlocking { trips.createTrip(CreateTrip("杭州 · Agent 闭环验证", 1, requestId = tripId)) }
            val before = saved()
            assertEquals(if (stage == "confirm") 0 else 1, before.size)
            val prior = if (stage == "restart") JSONObject(File(directory, "confirm.json").readText()) else null
            if (prior != null) {
                assertTrue(prior.getBoolean("passed"))
                assertNotEquals(prior.getInt("pid"), Process.myPid())
                assertEquals(prior.getString("saved_id"), before.single().id)
                assertEquals(selected.poiId, before.single().amapPoiId)
            }
            // Replay only this run's live SDK response, not hardcoded/fabricated POIs.
            val cachedSource = object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    check(keyword == "雷峰塔" && city in setOf("杭州", "杭州市"))
                    return candidates
                }
                override suspend fun cityAt(point: GeoPoint) = PlaceCity("杭州市", "330100")
            }
            // The city context is explicit test input, not a claim of actual GPS position.
            val location = AppLocationSession(scope, { true }, { requireNotNull(selected.point) }, cachedSource::cityAt)
            val store = AmapConsentStore(object : AmapConsentPersistence {
                override fun readDecision() = true
                override fun writeDecision(accepted: Boolean) = Unit
            }, AmapPrivacyGate.create(context), ConsentRegistry())
            val routes = object : RouteRefreshCoordinator {
                override fun start(scope: CoroutineScope) = Unit
                override suspend fun retry(legId: String) = false
                override suspend fun updateDetails(legId: String, selectedModeOverride: TransportMode?,
                    durationOverrideSeconds: Int?, note: String?) = false
            }
            compose.setContent {
                AppNavigation(
                    service = TripService(trips), repository = trips,
                    impacts = RoomDeleteImpactProvider(database.deleteImpactDao()),
                    dependencies = AppNavigationDependencies(
                        savedPlaceRepository = places,
                        itineraryRepository = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao()),
                        routeLegRepository = RoomRouteLegRepository(database.routeLegDao()),
                        mapPreferences = InMemoryMapPreferences(),
                        locationPermissionRequestStore = InMemoryLocationPermissionRequestStore(),
                        consentStore = store,
                        runtimeSessionFactory = { fact -> AmapRuntimeSession(fact.generation, fact.token, cachedSource, routes, location) },
                        stopRuntimeSession = location::close,
                    ),
                    mapHostFactory = { activityContext ->
                        val real = RealAmapMapHost(activityContext).also(hosts::add)
                        object : AmapMapHost by real {
                            override fun setOnReadyListener(listener: (() -> Unit)?) =
                                real.setOnReadyListener(listener?.let { { loaded.set(true); it() } })
                            override fun render(model: MapUiModel, layer: MapLayer,
                                onMarkerClick: (String) -> Unit, onMapPoiClick: (MapPoiUi) -> Unit,
                                onLayerError: (Throwable, MapLayer) -> Unit) {
                                real.render(model, layer, onMarkerClick, onMapPoiClick, onLayerError)
                                models += model
                            }
                        }
                    },
                )
            }
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("primary-trip-$tripId") or hasTestTag("other-trip-$tripId")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(hasTestTag("primary-trip-$tripId") or hasTestTag("other-trip-$tripId")).performClick()
            compose.onNodeWithTag("section-PLACE_POOL").performClick()
            compose.waitUntil(30_000) { loaded.get() }
            if (stage == "confirm") {
                compose.onNodeWithTag("workspace-search-launcher").performClick()
                compose.onNodeWithTag("place-search-field").performTextInput(keyword)
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag("place-search-result-row-$selectedPoi").fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("place-search-submit").performClick()
                compose.waitUntil(10_000) { models.lastOrNull()?.markers?.any { it.kind == MapMarkerKind.SEARCH_RESULT } == true }
                assertTrue(saved().isEmpty())
                screenshot("01-candidates-not-saved")
                // Cancel the preview before confirming: no write may have happened.
                compose.onNodeWithTag("workspace-search-results-clear").performClick()
                assertTrue(saved().isEmpty())
                report.put("preview_saved_count", 0).put("cancel_saved_count", 0)
                compose.onNodeWithTag("workspace-search-launcher").performClick()
                compose.onNodeWithTag("place-search-field").performTextReplacement(keyword)
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag("place-search-bookmark-touch-$selectedPoi").fetchSemanticsNodes().isNotEmpty()
                }
                // Test-user confirmation through the existing UI; never model-authorized save.
                compose.onNodeWithTag("place-search-bookmark-touch-$selectedPoi").performScrollTo().performClick()
                compose.waitUntil(10_000) { saved().size == 1 }
                val stored = saved().single()
                assertEquals(selected.poiId, stored.amapPoiId)
                val repeated = runBlocking { places.save(tripId, selected) }
                assertEquals(SavePlaceResult.AlreadySaved(stored.id), repeated)
                assertEquals(1, saved().size)
                val invalid = runCatching { runBlocking { places.save(tripId, selected.copy(poiId = "invalid-$probeId", point = null)) } }
                assertTrue(invalid.isFailure)
                assertEquals(1, saved().size)
                report.put("duplicate_count", 1).put("missing_coordinate_rejected", true)
                compose.onNodeWithTag("place-search-submit").performClick()
                compose.onNodeWithTag("workspace-search-results-clear").performClick()
            }
            val stored = saved().single()
            compose.waitUntil(15_000) {
                models.lastOrNull()?.markers?.any { it.savedPlaceId == stored.id && it.kind == MapMarkerKind.SAVED_PLACE_POOL } == true
            }
            assertTrue(models.last().markers.none { it.kind == MapMarkerKind.SEARCH_RESULT })
            val expectedKey = "place-${stored.id}"
            var nativeMarker: JSONObject? = null
            compose.waitUntil(20_000) {
                compose.runOnUiThread {
                    hosts.filter { it.view.isAttachedToWindow }.forEach { host ->
                        val native = (host.view as MapView).map.mapScreenMarkers.orEmpty().firstOrNull { it.`object` == expectedKey }
                        if (native != null) nativeMarker = JSONObject().put("key", native.`object`)
                            .put("title", native.title).put("lat", native.position.latitude).put("lng", native.position.longitude)
                    }
                }
                nativeMarker != null
            }
            assertEquals(selected.point!!.latitude, nativeMarker!!.getDouble("lat"), 0.000001)
            assertEquals(selected.point.longitude, nativeMarker!!.getDouble("lng"), 0.000001)
            assertEquals(selected.name, nativeMarker!!.getString("title"))
            screenshot(if (stage == "confirm") "02-confirmed-saved-marker" else "03-restarted-saved-marker")
            report.put("saved_count", 1).put("saved_id", stored.id).put("poi", candidateJson(selected))
                .put("native_marker", nativeMarker).put("search_overlay_cleared", true)
                .put("database", databaseName).put("category", stored.category.name)
                .put("confirmation", if (stage == "confirm") "existing UI click by authorized test user" else "no new confirmation or write")
                .put("passed", true).put("finished_at", System.currentTimeMillis())
        } catch (failure: Throwable) {
            report.put("error_type", failure.javaClass.name).put("error", failure.message)
            runCatching { screenshot("$stage-failed") }
            throw failure
        } finally {
            persist()
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)) }
            scope.cancel()
            database.close()
        }
    }

    private fun candidateJson(c: PlaceCandidate) = JSONObject()
        .put("poi_id", c.poiId).put("name", c.name).put("address", c.address)
        .put("latitude", c.point?.latitude ?: JSONObject.NULL).put("longitude", c.point?.longitude ?: JSONObject.NULL)
        .put("city_code", c.cityCode ?: JSONObject.NULL).put("city_name", c.cityName ?: JSONObject.NULL)
        .put("city_adcode", c.cityAdCode ?: JSONObject.NULL).put("city_metadata_version", c.cityMetadataVersion)

    private fun candidateFromJson(j: JSONObject) = PlaceCandidate(
        j.getString("poi_id"), j.getString("name"), j.getString("address"),
        if (j.isNull("latitude")) null else GeoPoint(j.getDouble("latitude"), j.getDouble("longitude")),
        j.optString("city_code").takeUnless { it == "null" },
        j.optString("city_name").takeUnless { it == "null" },
        j.optString("city_adcode").takeUnless { it == "null" }, j.getInt("city_metadata_version"),
    )
}
