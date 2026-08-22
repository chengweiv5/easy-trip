package com.yangchengwei.easytrip.amap

import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.MainActivity
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.workspace.AmapComposeMap
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapMarkerKind
import com.yangchengwei.easytrip.workspace.MapMarkerUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import com.yangchengwei.easytrip.workspace.toMapPoiUi
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Poi
import com.yangchengwei.easytrip.workspace.MapViewportRequest
import com.yangchengwei.easytrip.workspace.ViewportReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AmapComposeMapTest {
    @get:Rule val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test fun sdkPoiTranslationKeepsStableIdAndMarksMissingIdUncollectable() {
        val point = LatLng(39.916, 116.397)

        assertEquals(
            MapPoiUi("B0001", "故宫", "北京市东城区景山前街4号", GeoPoint(39.916, 116.397)),
            Poi("故宫", point, "B0001").toMapPoiUi("北京市东城区景山前街4号"),
        )
        assertEquals(null, Poi("无编号地点", point, null).toMapPoiUi()!!.poiId)
    }

    @Test fun fakeHostDeliversMapPoiToComposeCallback() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val expected = MapPoiUi("B0001", "故宫", "北京市东城区景山前街4号", GeoPoint(39.916, 116.397))
        var received: MapPoiUi? = null
        val emitted = CountDownLatch(1)
        rule.scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    onMapPoiClick = { received = it; emitted.countDown() },
                    hostFactory = { ctx ->
                        object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun render(
                                model: MapUiModel,
                                layer: MapLayer,
                                onMarkerClick: (String) -> Unit,
                                onMapPoiClick: (MapPoiUi) -> Unit,
                                onLayerError: (Throwable, MapLayer) -> Unit,
                            ) {
                                if (received == null) onMapPoiClick(expected)
                            }
                        }
                    },
                )
            }
        }

        assertTrue(emitted.await(5, TimeUnit.SECONDS))
        assertEquals(expected, received)
    }

    @Test fun fakeHostKeepsLifecycleAndConsumesEachViewportRequestOnce() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        var creations = 0
        var destroys = 0
        var creates = 0
        var resumes = 0
        val viewportIds = mutableListOf<Long>()
        var renderedMarker: MapMarkerUi? = null
        lateinit var lastOwner: TestOwner
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        rule.scenario.onActivity { lastOwner = TestOwner(); ownerState = mutableStateOf(lastOwner) }
        val created = CountDownLatch(1)
        val destroyed = CountDownLatch(1)
        val resumed = CountDownLatch(2)
        val initialViewport = CountDownLatch(1)
        val secondViewport = CountDownLatch(1)
        val state = mutableStateOf(0)
        val focusedMarker = MapMarkerUi(
            key = "place-hotel",
            point = GeoPoint(39.9, 116.4),
            label = "酒店",
            occurrences = emptyList(),
            kind = MapMarkerKind.SAVED_ITINERARY,
            badgeText = "1·4",
            isFocused = true,
        )
        val model = mutableStateOf(MapUiModel(markers = listOf(focusedMarker), viewportRequest = request(1)))
        rule.scenario.onActivity { activity ->
            activity.setContent {
                Text("${state.value}")
                CompositionLocalProvider(LocalLifecycleOwner provides ownerState.value) {
                    AmapComposeMap(model.value, {}, token, hostFactory = { ctx ->
                        creations++
                        created.countDown()
                        object : AmapMapHost {
                            override val view: View = View(ctx)
                            private var consumedViewportId: Long? = null
                            override fun onCreate() { creates++ }
                            override fun onResume() { resumes++; resumed.countDown() }
                            override fun onPause() = Unit
                            override fun onDestroy() { destroys++; destroyed.countDown() }
                            override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                                renderedMarker = model.markers.singleOrNull()
                                model.viewportRequest?.id?.takeIf { it != consumedViewportId }?.let {
                                    consumedViewportId = it
                                    viewportIds += it
                                    if (it == 1L) initialViewport.countDown() else secondViewport.countDown()
                                }
                            }
                        }
                    })
                }
            }
        }
        assertTrue(created.await(5, TimeUnit.SECONDS))
        assertTrue(initialViewport.await(5, TimeUnit.SECONDS))
        assertEquals(focusedMarker, renderedMarker)

        rule.scenario.onActivity { state.value++ }
        rule.scenario.onActivity { model.value = model.value.copy(highlightedMarkerKey = "model-only") }
        rule.scenario.onActivity { state.value++ }
        rule.scenario.onActivity { }
        assertEquals(1, creations)
        assertEquals(listOf(1L), viewportIds)

        rule.scenario.onActivity { model.value = model.value.copy(viewportRequest = request(2)) }
        assertTrue(secondViewport.await(5, TimeUnit.SECONDS))
        rule.scenario.onActivity { state.value++ }
        rule.scenario.onActivity { }
        assertEquals(listOf(1L, 2L), viewportIds)
        assertEquals(1, creations)

        rule.scenario.onActivity { lastOwner = TestOwner(); ownerState.value = lastOwner }
        rule.scenario.onActivity { }
        assertTrue(destroyed.await(5, TimeUnit.SECONDS))
        assertEquals(2, creations)
        assertEquals(2, creates)
        assertTrue(resumed.await(5, TimeUnit.SECONDS))
        assertEquals(1, destroys)
    }

    private fun request(id: Long) = MapViewportRequest(
        id = id,
        reason = ViewportReason.INITIAL,
        points = listOf(GeoPoint(39.9, 116.4)),
    )

    private class TestOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle = registry
    }
}
