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

    @Test fun locateRequestInvokesMapHostCurrentLocation() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val located = CountDownLatch(1)
        rule.scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    locateRequest = 1,
                    hostFactory = { ctx ->
                        object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun showCurrentLocation() {
                                located.countDown()
                            }
                        }
                    },
                )
            }
        }

        assertTrue(located.await(5, TimeUnit.SECONDS))
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

    @Test fun delayedMapReadyKeepsLoadingUntilHostSignalsReady() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val listenerRegistered = CountDownLatch(1)
        val rendered = CountDownLatch(1)
        val ready = CountDownLatch(1)
        var readyListener: (() -> Unit)? = null
        var renderCount = 0
        var readyCount = 0

        rule.scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun setOnReadyListener(listener: (() -> Unit)?) {
                            readyListener = listener
                            listenerRegistered.countDown()
                        }
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                            renderCount++
                            rendered.countDown()
                        }
                    } },
                    onMapReady = { readyCount++; ready.countDown() },
                )
            }
        }

        assertTrue(listenerRegistered.await(5, TimeUnit.SECONDS))
        assertEquals(0, renderCount)
        assertEquals(0, readyCount)
        rule.scenario.onActivity { requireNotNull(readyListener).invoke() }
        assertTrue(rendered.await(5, TimeUnit.SECONDS))
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        assertEquals(1, readyCount)
    }

    @Test fun disposedHostReadyCallbackIsIgnoredAfterReplacement() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        val readyCallbacks = mutableListOf<() -> Unit>()
        val firstReadyListener = CountDownLatch(1)
        val secondReadyListener = CountDownLatch(1)
        var renderCount = 0
        var readyCount = 0

        rule.scenario.onActivity { activity ->
            ownerState = mutableStateOf(TestOwner())
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides ownerState.value) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        hostFactory = { ctx -> object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                if (listener != null) {
                                    readyCallbacks += listener
                                    if (readyCallbacks.size == 1) firstReadyListener.countDown() else secondReadyListener.countDown()
                                }
                            }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                                renderCount++
                            }
                        } },
                        onMapReady = { readyCount++ },
                    )
                }
            }
        }

        assertTrue(firstReadyListener.await(5, TimeUnit.SECONDS))
        rule.scenario.onActivity { ownerState.value = TestOwner() }
        assertTrue(secondReadyListener.await(5, TimeUnit.SECONDS))
        rule.scenario.onActivity { readyCallbacks.first().invoke() }
        rule.scenario.onActivity { }
        assertEquals(0, renderCount)
        assertEquals(0, readyCount)
        rule.scenario.onActivity { readyCallbacks.last().invoke() }
        rule.scenario.onActivity { }
        assertEquals(1, renderCount)
        assertEquals(1, readyCount)
    }

    @Test fun hostCreationFailureIsReported() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val reported = CountDownLatch(1)
        var destroyed = 0
        rule.scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() = throw IllegalStateException("create")
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() { destroyed++ }
                    } },
                    onMapError = { reported.countDown() },
                )
            }
        }
        assertTrue(reported.await(5, TimeUnit.SECONDS))
        assertEquals(1, destroyed)
    }

    @Test fun renderFailureIsReported() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val reported = CountDownLatch(1)
        rule.scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                            throw IllegalStateException("render")
                        }
                    } },
                    onMapError = { reported.countDown() },
                )
            }
        }
        assertTrue(reported.await(5, TimeUnit.SECONDS))
    }

    @Test fun layerFailureDoesNotReportMapReady() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val layerReported = CountDownLatch(1)
        var readyCount = 0
        rule.scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                            onLayerError(IllegalStateException("layer"), MapLayer.STANDARD)
                        }
                    } },
                    onLayerError = { _, _ -> layerReported.countDown() },
                    onMapReady = { readyCount++ },
                )
            }
        }
        assertTrue(layerReported.await(5, TimeUnit.SECONDS))
        assertEquals(0, readyCount)
    }

    @Test fun successfulRenderReportsMapReadyOnceAcrossUpdates() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val ready = CountDownLatch(1)
        var readyCount = 0
        lateinit var model: androidx.compose.runtime.MutableState<MapUiModel>
        rule.scenario.onActivity { activity ->
            model = mutableStateOf(MapUiModel())
            activity.setContent {
                AmapComposeMap(
                    model = model.value,
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                    } },
                    onMapReady = { readyCount++; ready.countDown() },
                )
            }
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        rule.scenario.onActivity { model.value = model.value.copy(highlightedMarkerKey = "updated") }
        rule.scenario.onActivity { }
        assertEquals(1, readyCount)
    }

    @Test fun disposedHostCallbacksAreIgnoredAfterLifecycleOwnerReplacement() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        val callbacks = mutableListOf<(Throwable, MapLayer) -> Unit>()
        val firstRender = CountDownLatch(1)
        val secondRender = CountDownLatch(1)
        var layerErrors = 0
        var readyCount = 0
        rule.scenario.onActivity { activity ->
            ownerState = mutableStateOf(TestOwner())
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides ownerState.value) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        hostFactory = { ctx -> object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                                callbacks += onLayerError
                                if (callbacks.size == 1) firstRender.countDown() else secondRender.countDown()
                            }
                        } },
                        onLayerError = { _, _ -> layerErrors++ },
                        onMapReady = { readyCount++ },
                    )
                }
            }
        }
        assertTrue(firstRender.await(5, TimeUnit.SECONDS))
        assertEquals(1, readyCount)
        rule.scenario.onActivity { ownerState.value = TestOwner() }
        assertTrue(secondRender.await(5, TimeUnit.SECONDS))
        assertEquals(2, readyCount)
        rule.scenario.onActivity { callbacks.first()(IllegalStateException("late"), MapLayer.STANDARD) }
        assertEquals(0, layerErrors)
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
