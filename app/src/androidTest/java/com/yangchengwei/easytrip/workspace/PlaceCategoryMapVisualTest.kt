package com.yangchengwei.easytrip.workspace

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlaceCategoryMapVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realMapDisplaysCategoriesAndChangingCategoryPreservesCamera() {
        val store = AmapConsentStore(object : AmapConsentPersistence {
            override fun readDecision(): Boolean? = null
            override fun writeDecision(accepted: Boolean) = Unit
        }, AmapPrivacyGate.create(compose.activity), ConsentRegistry())
        runBlocking { store.reportShown().getOrThrow(); store.decide(true).getOrThrow() }
        val token = (store.state.value.fact as AmapConsentFact.Accepted).token
        val points = listOf(GeoPoint(30.252, 120.140), GeoPoint(30.258, 120.155),
            GeoPoint(30.245, 120.168), GeoPoint(30.237, 120.154), GeoPoint(30.240, 120.138))
        val names = listOf("景区", "酒店", "餐厅", "车站", "其他地点")
        val pool = PlaceCategory.entries.mapIndexed { i, category ->
            MapMarkerUi("category-$i", points[i], names[i], emptyList(), MapMarkerKind.SAVED_PLACE_POOL,
                scheduled = i % 2 == 0, category = category)
        }
        val inset = (48 * compose.activity.resources.displayMetrics.density).toInt()
        val model = mutableStateOf(MapUiModel(markers = pool,
            viewportRequest = MapViewportRequest(1, ViewportReason.INITIAL, points,
                safeInsets = MapViewportInsets(inset, inset, inset, inset))))
        val ready = AtomicInteger()
        val creations = AtomicInteger()
        lateinit var host: AmapMapHost
        compose.setContent {
            EasyTripTheme {
                AmapComposeMap(model.value, {}, token, modifier = Modifier.fillMaxSize(),
                    hostFactory = { RealAmapMapHost.create(it).also { h -> host = h; creations.incrementAndGet() } },
                    onMapReady = { ready.incrementAndGet() })
            }
        }
        compose.waitUntil(20_000) { ready.get() > 0 }
        compose.waitUntil(10_000) {
            var valid = false
            compose.runOnUiThread { valid = host.cameraSnapshot()?.target?.latitude?.let { it in 30.23..30.27 } == true }
            valid
        }
        capture("map-pool-five-categories")
        var before: MapCameraState? = null
        compose.runOnUiThread { before = host.cameraSnapshot() }
        compose.runOnIdle { model.value = model.value.copy(markers = pool.map {
            if (it.key == "category-0") it.copy(category = PlaceCategory.LODGING) else it
        }) }
        compose.waitForIdle()
        compose.runOnUiThread {
            assertEquals(1, creations.get())
            assertEquals(before, host.cameraSnapshot())
        }
        compose.runOnIdle {
            model.value = model.value.copy(markers = pool.mapIndexed { i, marker ->
                marker.copy(kind = MapMarkerKind.SAVED_ITINERARY, badgeText = "${i + 1}", scheduled = true,
                    badgeSegments = if (i == 1) listOf(MapMarkerBadgeSegment("2", routeColorForDay(0)),
                        MapMarkerBadgeSegment("2", routeColorForDay(1)))
                    else listOf(MapMarkerBadgeSegment("${i + 1}", routeColorForDay(i % 2))))
            })
        }
        capture("map-itinerary-date-colors-and-categories")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(4000)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = File(compose.activity.getExternalFilesDir(null), "v2.2-place-categories").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
