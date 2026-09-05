package com.yangchengwei.easytrip.amap

import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.MainActivity
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.workspace.AmapComposeMap
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapMarkerKind
import com.yangchengwei.easytrip.workspace.MapMarkerUi
import com.yangchengwei.easytrip.workspace.MapReadyTimeoutException
import com.yangchengwei.easytrip.workspace.MapUiModel
import com.yangchengwei.easytrip.workspace.toMapPoiUi
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Poi
import com.yangchengwei.easytrip.workspace.MapViewportRequest
import com.yangchengwei.easytrip.workspace.ViewportReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AmapComposeMapTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val scenario get() = rule.activityRule.scenario

    @Test fun sdkPoiTranslationKeepsStableIdAndMarksMissingIdUncollectable() {
        val point = LatLng(39.916, 116.397)

        assertEquals(
            MapPoiUi("B0001", "故宫", "北京市东城区景山前街4号", GeoPoint(39.916, 116.397)),
            Poi("故宫", point, "B0001").toMapPoiUi("北京市东城区景山前街4号"),
        )
        assertEquals(null, Poi("无编号地点", point, null).toMapPoiUi()!!.poiId)
    }

    @Test fun newLocateRequestInvokesReadyMapHostCurrentLocation() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var locateRequest: androidx.compose.runtime.MutableState<Int>
        val ready = CountDownLatch(1)
        val located = CountDownLatch(1)
        scenario.onActivity { activity ->
            locateRequest = mutableStateOf(0)
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    locateRequest = locateRequest.value,
                    hostFactory = { ctx ->
                        object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun canRenderBeforeReady() = true
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun showCurrentLocation() {
                                located.countDown()
                            }
                        }
                    },
                    onMapReady = { ready.countDown() },
                )
            }
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> locateRequest.value = 1 }
        assertTrue(located.await(5, TimeUnit.SECONDS))
    }

    @Test fun locateRequestDuringLoadingWaitsForReadyThenRunsOnce() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var owner: MutableLifecycleOwner
        lateinit var locateRequest: androidx.compose.runtime.MutableState<Int>
        lateinit var readyListener: () -> Unit
        val listenerRegistered = CountDownLatch(1)
        val located = CountDownLatch(1)
        var locationCalls = 0

        scenario.onActivity { activity ->
            owner = MutableLifecycleOwner(Lifecycle.State.RESUMED)
            locateRequest = mutableStateOf(0)
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        locateRequest = locateRequest.value,
                        hostFactory = { context -> object : AmapMapHost {
                            override val view = View(context)
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                if (listener != null) {
                                    readyListener = listener
                                    listenerRegistered.countDown()
                                }
                            }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun showCurrentLocation() {
                                locationCalls++
                                located.countDown()
                            }
                        } },
                    )
                }
            }
        }

        assertTrue(listenerRegistered.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> locateRequest.value = 1 }
        Thread.sleep(300)
        assertEquals(0, locationCalls)

        scenario.onActivity { activity -> readyListener() }
        assertTrue(located.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.STARTED) }
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.RESUMED) }
        scenario.onActivity { activity -> }
        assertEquals(1, locationCalls)

        scenario.onActivity { activity -> locateRequest.value = 2 }
        assertTrue(waitUntil { locationCalls == 2 })
        scenario.onActivity { activity -> }
        assertEquals(2, locationCalls)
    }

    @Test fun locateRequestReceivedWhilePausedWaitsForResume() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var owner: MutableLifecycleOwner
        lateinit var locateRequest: androidx.compose.runtime.MutableState<Int>
        val ready = CountDownLatch(1)
        val located = CountDownLatch(1)
        var locationCalls = 0

        scenario.onActivity { activity ->
            owner = MutableLifecycleOwner(Lifecycle.State.RESUMED)
            locateRequest = mutableStateOf(0)
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        locateRequest = locateRequest.value,
                        hostFactory = { context -> object : AmapMapHost {
                            override val view = View(context)
                            override fun canRenderBeforeReady() = true
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun showCurrentLocation() {
                                locationCalls++
                                located.countDown()
                            }
                        } },
                        onMapReady = { ready.countDown() },
                    )
                }
            }
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.STARTED) }
        scenario.onActivity { activity -> locateRequest.value = 1 }
        scenario.onActivity { activity -> }
        assertEquals(0, locationCalls)

        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.RESUMED) }
        assertTrue(located.await(5, TimeUnit.SECONDS))
        assertEquals(1, locationCalls)
    }

    @Test fun explicitlyForwardedPendingLocateIntentRunsOnceOnReplacementHost() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var retryKey: androidx.compose.runtime.MutableState<Int>
        lateinit var locateRequest: androidx.compose.runtime.MutableState<Int>
        lateinit var initialLocateRequest: androidx.compose.runtime.MutableState<Int>
        val readyListeners = mutableMapOf<Int, () -> Unit>()
        val listenerRegistered = listOf(CountDownLatch(1), CountDownLatch(1))
        val located = CountDownLatch(1)
        val locatedAttempts = mutableListOf<Int>()

        scenario.onActivity { activity ->
            retryKey = mutableStateOf(0)
            locateRequest = mutableStateOf(0)
            initialLocateRequest = mutableStateOf(0)
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    retryKey = retryKey.value,
                    locateRequest = locateRequest.value,
                    initialLocateRequest = initialLocateRequest.value,
                    hostFactory = { context ->
                        val attempt = retryKey.value
                        object : AmapMapHost {
                            override val view = View(context)
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                if (listener != null) {
                                    readyListeners[attempt] = listener
                                    listenerRegistered[attempt].countDown()
                                }
                            }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun showCurrentLocation() {
                                locatedAttempts += attempt
                                located.countDown()
                            }
                        }
                    },
                )
            }
        }

        assertTrue(listenerRegistered[0].await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> locateRequest.value = 1 }
        Thread.sleep(300)
        assertTrue(locatedAttempts.isEmpty())

        scenario.onActivity { activity ->
            initialLocateRequest.value = 0
            retryKey.value = 1
        }
        assertTrue(listenerRegistered[1].await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> requireNotNull(readyListeners[1]).invoke() }
        assertTrue(located.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }
        assertEquals(listOf(1), locatedAttempts)
    }

    @Test fun unforwardedPendingLocateRequestDoesNotLeakToReplacementHost() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var retryKey: androidx.compose.runtime.MutableState<Int>
        lateinit var locateRequest: androidx.compose.runtime.MutableState<Int>
        val readyListeners = mutableMapOf<Int, () -> Unit>()
        val listenerRegistered = listOf(CountDownLatch(1), CountDownLatch(1))
        val newRequestLocated = CountDownLatch(1)
        val locatedAttempts = mutableListOf<Int>()

        scenario.onActivity { activity ->
            retryKey = mutableStateOf(0)
            locateRequest = mutableStateOf(0)
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    retryKey = retryKey.value,
                    locateRequest = locateRequest.value,
                    hostFactory = { context ->
                        val attempt = retryKey.value
                        object : AmapMapHost {
                            override val view = View(context)
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                if (listener != null) {
                                    readyListeners[attempt] = listener
                                    listenerRegistered[attempt].countDown()
                                }
                            }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun showCurrentLocation() {
                                locatedAttempts += attempt
                                if (locateRequest.value == 2) newRequestLocated.countDown()
                            }
                        }
                    },
                )
            }
        }

        assertTrue(listenerRegistered[0].await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> locateRequest.value = 1 }
        Thread.sleep(300)
        assertTrue(locatedAttempts.isEmpty())

        scenario.onActivity { activity -> retryKey.value = 1 }
        assertTrue(listenerRegistered[1].await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> requireNotNull(readyListeners[1]).invoke() }
        scenario.onActivity { activity -> }
        assertTrue(locatedAttempts.isEmpty())

        scenario.onActivity { activity -> locateRequest.value = 2 }
        assertTrue(newRequestLocated.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }
        assertEquals(listOf(1), locatedAttempts)
    }

    @Test fun failedLocateAfterReadyIsGuardedAndNotRetried() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var owner: MutableLifecycleOwner
        lateinit var locateRequest: androidx.compose.runtime.MutableState<Int>
        val ready = CountDownLatch(1)
        val attempted = CountDownLatch(1)
        val reported = CountDownLatch(1)
        var locationCalls = 0
        var errors = 0

        scenario.onActivity { activity ->
            owner = MutableLifecycleOwner(Lifecycle.State.RESUMED)
            locateRequest = mutableStateOf(0)
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        locateRequest = locateRequest.value,
                        hostFactory = { context -> object : AmapMapHost {
                            override val view = View(context)
                            override fun canRenderBeforeReady() = true
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun showCurrentLocation() {
                                locationCalls++
                                attempted.countDown()
                                error("locate")
                            }
                        } },
                        onMapError = { errors++; reported.countDown() },
                        onMapReady = { ready.countDown() },
                    )
                }
            }
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> locateRequest.value = 1 }
        assertTrue(attempted.await(5, TimeUnit.SECONDS))
        assertTrue(reported.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.STARTED) }
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.RESUMED) }
        scenario.onActivity { activity -> }
        assertEquals(1, locationCalls)
        assertEquals(1, errors)
    }

    @Test fun fakeHostDeliversMapPoiToComposeCallback() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        val expected = MapPoiUi("B0001", "故宫", "北京市东城区景山前街4号", GeoPoint(39.916, 116.397))
        var received: MapPoiUi? = null
        val emitted = CountDownLatch(1)
        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    onMapPoiClick = { received = it; emitted.countDown() },
                    hostFactory = { ctx ->
                        object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun canRenderBeforeReady() = true
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
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        val listenerRegistered = CountDownLatch(1)
        val rendered = CountDownLatch(1)
        val ready = CountDownLatch(1)
        var readyListener: (() -> Unit)? = null
        var renderCount = 0
        var readyCount = 0

        scenario.onActivity { activity ->
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
        scenario.onActivity { activity -> requireNotNull(readyListener).invoke() }
        assertTrue(rendered.await(5, TimeUnit.SECONDS))
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        assertEquals(1, readyCount)
    }

    @Test fun zoomButtonsReportViewportOperationExactlyOnceBeforeDelegatingToHost() {
        val registry = ConsentRegistry()
        val active = registry.decide(true)
        val token = AmapConsentToken.issue(registry, active.generation)
        val events = mutableListOf<String>()
        val rendered = CountDownLatch(1)

        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { context -> object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun zoomIn() { events += "zoom-in" }
                        override fun zoomOut() { events += "zoom-out" }
                        override fun render(
                            model: MapUiModel,
                            layer: MapLayer,
                            onMarkerClick: (String) -> Unit,
                            onLayerError: (Throwable, MapLayer) -> Unit,
                        ) { rendered.countDown() }
                    } },
                    onUserGesture = { events += "viewport" },
                )
            }
        }

        assertTrue(rendered.await(5, TimeUnit.SECONDS))
        rule.onNodeWithTag("zoom-in").performClick()
        rule.onNodeWithTag("zoom-out").performClick()
        rule.waitForIdle()

        assertEquals(listOf("viewport", "zoom-in", "viewport", "zoom-out"), events)
    }

    @Test fun gestureListenerUsesLatestComposeCallbackAndIsClearedOnDispose() {
        val registry = ConsentRegistry()
        val active = registry.decide(true)
        val token = AmapConsentToken.issue(registry, active.generation)
        lateinit var callbackVersion: androidx.compose.runtime.MutableState<Int>
        var gestureListener: (() -> Unit)? = null
        var firstCallbackCount = 0
        var latestCallbackCount = 0
        var listenerRegisteredCount = 0
        var listenerRemovedCount = 0
        val listenerRegistered = CountDownLatch(1)
        val latestCallbackComposed = CountDownLatch(1)
        val listenerRemoved = CountDownLatch(1)

        scenario.onActivity { activity ->
            callbackVersion = mutableStateOf(0)
            activity.setContent {
                val currentVersion = callbackVersion.value
                SideEffect {
                    if (currentVersion == 1) latestCallbackComposed.countDown()
                }
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { context -> object : AmapMapHost {
                        override val view: View = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun setOnUserGestureListener(listener: (() -> Unit)?) {
                            gestureListener = listener
                            if (listener == null) {
                                listenerRemovedCount++
                                listenerRemoved.countDown()
                            } else {
                                listenerRegisteredCount++
                                listenerRegistered.countDown()
                            }
                        }
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                    } },
                    onUserGesture = {
                        if (currentVersion == 0) firstCallbackCount++ else latestCallbackCount++
                    },
                )
            }
        }

        assertTrue(listenerRegistered.await(5, TimeUnit.SECONDS))
        val registeredListener = requireNotNull(gestureListener)
        scenario.onActivity { activity -> callbackVersion.value = 1 }
        assertTrue(latestCallbackComposed.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> registeredListener() }
        scenario.onActivity { activity -> }

        assertEquals(0, firstCallbackCount)
        assertEquals(1, latestCallbackCount)
        assertEquals(1, listenerRegisteredCount)

        registry.decide(false)
        assertTrue(listenerRemoved.await(5, TimeUnit.SECONDS))
        registeredListener()
        scenario.onActivity { activity -> }

        assertEquals(1, latestCallbackCount)
        assertEquals(1, listenerRemovedCount)
    }

    @Test fun withdrawalDisposesActiveMapHostAndIgnoresOldCallbacks() {
        val registry = ConsentRegistry()
        val active = registry.decide(true)
        val token = AmapConsentToken.issue(registry, active.generation)
        var readyListener: (() -> Unit)? = null
        val listenerRegistered = CountDownLatch(1)
        val listenerRemoved = CountDownLatch(1)
        val hostDestroyed = CountDownLatch(1)
        var listenerRemovedCount = 0
        var destroyedCount = 0
        var readyCount = 0

        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { context -> object : AmapMapHost {
                        override val view: View = View(context)
                        override fun setOnReadyListener(listener: (() -> Unit)?) {
                            readyListener = listener
                            if (listener == null) {
                                listenerRemovedCount++
                                listenerRemoved.countDown()
                            } else {
                                listenerRegistered.countDown()
                            }
                        }
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() {
                            destroyedCount++
                            hostDestroyed.countDown()
                        }
                    } },
                    onMapReady = { readyCount++ },
                )
            }
        }

        assertTrue(listenerRegistered.await(5, TimeUnit.SECONDS))
        val oldCallback = requireNotNull(readyListener)
        registry.decide(false)
        assertTrue(listenerRemoved.await(5, TimeUnit.SECONDS))
        assertTrue(hostDestroyed.await(5, TimeUnit.SECONDS))
        oldCallback.invoke()
        scenario.onActivity { activity -> }

        assertEquals(1, listenerRemovedCount)
        assertEquals(1, destroyedCount)
        assertEquals(0, readyCount)
    }

    @Test fun disposedHostReadyCallbackIsIgnoredAfterReplacement() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        val readyCallbacks = mutableListOf<() -> Unit>()
        val firstReadyListener = CountDownLatch(1)
        val secondReadyListener = CountDownLatch(1)
        var renderCount = 0
        var readyCount = 0

        scenario.onActivity { activity ->
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
        scenario.onActivity { activity -> ownerState.value = TestOwner() }
        assertTrue(secondReadyListener.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> readyCallbacks.first().invoke() }
        scenario.onActivity { activity -> }
        assertEquals(0, renderCount)
        assertEquals(0, readyCount)
        scenario.onActivity { activity -> readyCallbacks.last().invoke() }
        scenario.onActivity { activity -> }
        assertEquals(1, renderCount)
        assertEquals(1, readyCount)
    }

    @Test fun hostCreationFailureIsReportedExactlyOnce() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        val reported = CountDownLatch(1)
        val destroyedSignal = CountDownLatch(1)
        var errors = 0
        var destroyed = 0
        scenario.onActivity { activity ->
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
                        override fun onDestroy() { destroyed++; destroyedSignal.countDown() }
                    } },
                    onMapError = { errors++; reported.countDown() },
                )
            }
        }
        assertTrue(reported.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }
        assertTrue(destroyedSignal.await(5, TimeUnit.SECONDS))
        assertEquals(1, errors)
        assertEquals(1, destroyed)
    }

    @Test fun renderCapableHostWaitsForSuccessfulResumeBeforeRenderingOrTimingOut() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var owner: MutableLifecycleOwner
        val resumedReady = CountDownLatch(1)
        val events = mutableListOf<String>()
        var errors = 0

        scenario.onActivity { activity ->
            owner = MutableLifecycleOwner(Lifecycle.State.CREATED)
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        readyTimeoutMillis = 400,
                        hostFactory = { context -> object : AmapMapHost {
                            override val view = View(context)
                            override fun canRenderBeforeReady() = true
                            override fun onCreate() { events += "create" }
                            override fun onResume() { events += "resume" }
                            override fun onPause() { events += "pause" }
                            override fun onDestroy() = Unit
                            override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) { events += "render" }
                        } },
                        onMapError = { errors++ },
                        onMapReady = { resumedReady.countDown() },
                    )
                }
            }
        }

        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.STARTED) }
        Thread.sleep(500)
        assertEquals(listOf("create"), events)
        assertEquals(0, errors)
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.RESUMED) }

        assertTrue(resumedReady.await(2, TimeUnit.SECONDS))
        assertEquals("create", events.first())
        assertEquals("resume", events[1])
        assertEquals("render", events[2])
        assertEquals(0, errors)
    }

    @Test fun failedResumeReportsOnlyErrorAndNeverRendersOrReadies() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var owner: MutableLifecycleOwner
        val error = CountDownLatch(1)
        var renders = 0
        var readies = 0
        var errors = 0

        scenario.onActivity { activity ->
            owner = MutableLifecycleOwner(Lifecycle.State.CREATED)
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        hostFactory = { context -> object : AmapMapHost {
                            override val view = View(context)
                            override fun canRenderBeforeReady() = true
                            override fun onCreate() = Unit
                            override fun onResume() = throw IllegalStateException("resume")
                            override fun onPause() = Unit
                            override fun onDestroy() = Unit
                            override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) { renders++ }
                        } },
                        onMapError = { errors++; error.countDown() },
                        onMapReady = { readies++ },
                    )
                }
            }
        }

        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.RESUMED) }
        assertTrue(error.await(2, TimeUnit.SECONDS))
        assertEquals(0, renders)
        assertEquals(0, readies)
        assertEquals(1, errors)
    }

    @Test fun lifecycleFailureIsReportedExactlyOnce() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        val reported = CountDownLatch(1)
        var errors = 0
        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() = Unit
                        override fun onResume() = throw IllegalStateException("resume")
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                    } },
                    onMapError = { errors++; reported.countDown() },
                )
            }
        }
        assertTrue(reported.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }
        assertEquals(1, errors)
    }

    @Test fun renderFailureIsReportedExactlyOnce() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        val reported = CountDownLatch(1)
        var errors = 0
        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                            throw IllegalStateException("render")
                        }
                    } },
                    onMapError = { errors++; reported.countDown() },
                )
            }
        }
        assertTrue(reported.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }
        assertEquals(1, errors)
    }

    @Test fun layerFailureRetainsMapAndReportsLocalFailureWithoutTerminalMapError() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        val layerReported = CountDownLatch(1)
        val ready = CountDownLatch(1)
        var mapErrors = 0
        var readyCount = 0
        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                            onLayerError(IllegalStateException("layer"), MapLayer.STANDARD)
                        }
                    } },
                    onLayerError = { _, retained ->
                        assertEquals(MapLayer.STANDARD, retained)
                        layerReported.countDown()
                    },
                    onMapError = { mapErrors++ },
                    onMapReady = { readyCount++; ready.countDown() },
                )
            }
        }
        assertTrue(layerReported.await(5, TimeUnit.SECONDS))
        assertTrue(ready.await(5, TimeUnit.SECONDS))
        assertEquals(0, mapErrors)
        assertEquals(1, readyCount)
    }

    @Test fun successfulRenderReportsMapReadyOnceAcrossUpdates() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        val ready = CountDownLatch(1)
        var readyCount = 0
        lateinit var model: androidx.compose.runtime.MutableState<MapUiModel>
        scenario.onActivity { activity ->
            model = mutableStateOf(MapUiModel())
            activity.setContent {
                AmapComposeMap(
                    model = model.value,
                    onMarkerClick = {},
                    consent = token,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun canRenderBeforeReady() = true
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
        scenario.onActivity { activity -> model.value = model.value.copy(highlightedMarkerKey = "updated") }
        scenario.onActivity { activity -> }
        assertEquals(1, readyCount)
    }

    @Test fun disposedHostCallbacksAreIgnoredAfterLifecycleOwnerReplacement() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        val callbacks = mutableListOf<(Throwable, MapLayer) -> Unit>()
        val firstRender = CountDownLatch(1)
        val firstReady = CountDownLatch(1)
        val secondRender = CountDownLatch(1)
        val secondReady = CountDownLatch(1)
        var layerErrors = 0
        var readyCount = 0
        scenario.onActivity { activity ->
            ownerState = mutableStateOf(TestOwner())
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides ownerState.value) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        hostFactory = { ctx -> object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun canRenderBeforeReady() = true
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
                        onMapReady = {
                            readyCount++
                            if (readyCount == 1) firstReady.countDown()
                            if (readyCount == 2) secondReady.countDown()
                        },
                    )
                }
            }
        }
        assertTrue(firstRender.await(5, TimeUnit.SECONDS))
        assertTrue(firstReady.await(5, TimeUnit.SECONDS))
        assertEquals(1, readyCount)
        scenario.onActivity { activity -> ownerState.value = TestOwner() }
        assertTrue(secondRender.await(5, TimeUnit.SECONDS))
        assertTrue(secondReady.await(5, TimeUnit.SECONDS))
        assertEquals(2, readyCount)
        scenario.onActivity { activity -> callbacks.first()(IllegalStateException("late"), MapLayer.STANDARD) }
        assertEquals(0, layerErrors)
    }

    @Test fun lifecycleReplacementIgnoresOldDestroyFailure() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        val firstCreated = CountDownLatch(1)
        val secondReady = CountDownLatch(1)
        var hostCount = 0
        var errors = 0

        scenario.onActivity { activity ->
            ownerState = mutableStateOf(TestOwner())
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides ownerState.value) {
                    AmapComposeMap(
                        model = MapUiModel(),
                        onMarkerClick = {},
                        consent = token,
                        hostFactory = { context ->
                            val attempt = hostCount++
                            if (attempt == 0) firstCreated.countDown()
                            object : AmapMapHost {
                                override val view = View(context)
                                override fun canRenderBeforeReady() = true
                                override fun onCreate() = Unit
                                override fun onResume() = Unit
                                override fun onPause() = Unit
                                override fun onDestroy() {
                                    if (attempt == 0) error("old destroy")
                                }
                                override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) = Unit
                            }
                        },
                        onMapError = { errors++ },
                        onMapReady = { if (hostCount > 1) secondReady.countDown() },
                    )
                }
            }
        }

        assertTrue(firstCreated.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> ownerState.value = TestOwner() }
        assertTrue(secondReady.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }

        assertEquals(0, errors)
    }

    @Test fun defaultHostContractWithoutReadyOrRenderCapabilityTimesOut() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        val timeout = CountDownLatch(1)
        var renders = 0
        var errors = 0

        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    readyTimeoutMillis = 400,
                    hostFactory = { context -> object : AmapMapHost {
                        override val view = View(context)
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) { renders++ }
                    } },
                    onMapError = { errors++; timeout.countDown() },
                )
            }
        }

        assertTrue(timeout.await(2, TimeUnit.SECONDS))
        assertEquals(0, renders)
        assertEquals(1, errors)
    }

    @Test fun renderCapableHostBecomesReadyWithoutSdkCallback() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        val ready = CountDownLatch(1)
        var renders = 0
        var errors = 0

        scenario.onActivity { activity ->
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    readyTimeoutMillis = 400,
                    hostFactory = { context -> object : AmapMapHost {
                        override val view = View(context)
                        override fun canRenderBeforeReady() = true
                        override fun onCreate() = Unit
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                        override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) { renders++ }
                    } },
                    onMapError = { errors++ },
                    onMapReady = { ready.countDown() },
                )
            }
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS))
        assertTrue(renders > 0)
        assertEquals(0, errors)
    }

    @Test fun neverReadyHostTimesOutOnceAndLateReadyAfterReplacementIsIgnored() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var retryKey: androidx.compose.runtime.MutableState<Int>
        val firstReady = CountDownLatch(1)
        val secondReady = CountDownLatch(1)
        val errors = CountDownLatch(1)
        val secondError = CountDownLatch(1)
        val readyCallbacks = mutableListOf<() -> Unit>()
        val destroyed = mutableListOf<Int>()
        var errorCount = 0
        var timeout: Throwable? = null
        var readyCount = 0

        scenario.onActivity { activity ->
            retryKey = mutableStateOf(0)
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    retryKey = retryKey.value,
                    readyTimeoutMillis = 400,
                    hostFactory = { context ->
                        val attempt = retryKey.value
                        object : AmapMapHost {
                            override val view = View(context)
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                listener?.let {
                                    readyCallbacks += it
                                    if (attempt == 0) firstReady.countDown() else secondReady.countDown()
                                    if (attempt == 1) it.invoke()
                                }
                            }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() { destroyed += attempt }
                        }
                    },
                    onMapError = { error -> timeout = error; errorCount++; errors.countDown(); if (errorCount > 1) secondError.countDown() },
                    onMapReady = { readyCount++ },
                )
            }
        }

        assertTrue(firstReady.await(2, TimeUnit.SECONDS))
        assertTrue(errors.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> retryKey.value++ }
        assertTrue(secondReady.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> readyCallbacks.first().invoke() }
        scenario.onActivity { activity -> }

        assertEquals(1, errorCount)
        assertTrue(timeout is MapReadyTimeoutException)
        assertEquals(1, readyCount)
        assertTrue(destroyed.contains(0))
        assertFalse(secondError.await(500, TimeUnit.MILLISECONDS))
        assertEquals(1, errorCount)
    }

    @Test fun changingReadyTimeoutRecreatesHostWithTheSameAttemptBoundary() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var timeoutMillis: androidx.compose.runtime.MutableState<Long>
        val creations = mutableListOf<Long>()
        val destroyed = mutableListOf<Long>()
        val firstCreated = CountDownLatch(1)
        val secondReady = CountDownLatch(1)

        scenario.onActivity { activity ->
            timeoutMillis = mutableStateOf(400L)
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    readyTimeoutMillis = timeoutMillis.value,
                    hostFactory = { context ->
                        val timeout = timeoutMillis.value
                        creations += timeout
                        firstCreated.countDown()
                        object : AmapMapHost {
                            override val view = View(context)
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                if (timeout == 800L) {
                                    listener?.invoke()
                                    secondReady.countDown()
                                }
                            }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() { destroyed += timeout }
                        }
                    },
                )
            }
        }

        assertTrue(firstCreated.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> timeoutMillis.value = 800L }
        assertTrue(secondReady.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }

        assertEquals(listOf(400L, 800L), creations)
        assertEquals(listOf(400L), destroyed)
    }

    @Test fun readyHostIgnoresLaterInitialFailuresButKeepsLayerDispatch() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var owner: MutableLifecycleOwner
        lateinit var model: androidx.compose.runtime.MutableState<MapUiModel>
        val ready = CountDownLatch(1)
        val secondRender = CountDownLatch(1)
        val layerReported = CountDownLatch(1)
        var renderCount = 0
        var readyCount = 0
        var mapErrors = 0
        var layerErrors = 0

        scenario.onActivity { activity ->
            owner = MutableLifecycleOwner(Lifecycle.State.RESUMED)
            model = mutableStateOf(MapUiModel())
            activity.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    AmapComposeMap(
                        model = model.value,
                        onMarkerClick = {},
                        consent = token,
                        hostFactory = { context -> object : AmapMapHost {
                            override val view = View(context)
                            override fun canRenderBeforeReady() = true
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = error("pause after ready")
                            override fun onDestroy() = error("destroy after ready")
                            override fun render(model: MapUiModel, layer: MapLayer, onMarkerClick: (String) -> Unit, onLayerError: (Throwable, MapLayer) -> Unit) {
                                renderCount++
                                if (renderCount == 1) {
                                    onLayerError(IllegalStateException("business layer"), MapLayer.STANDARD)
                                    layerReported.countDown()
                                } else {
                                    secondRender.countDown()
                                    error("render after ready")
                                }
                            }
                        } },
                        onLayerError = { _, _ -> layerErrors++ },
                        onMapError = { mapErrors++ },
                        onMapReady = { readyCount++; ready.countDown() },
                    )
                }
            }
        }

        assertTrue(ready.await(2, TimeUnit.SECONDS))
        assertTrue(layerReported.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> model.value = model.value.copy(highlightedMarkerKey = "second") }
        assertTrue(secondRender.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.CREATED) }
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.RESUMED) }
        scenario.onActivity { activity -> owner.moveTo(Lifecycle.State.DESTROYED) }
        scenario.onActivity { activity -> }

        assertEquals(1, readyCount)
        assertEquals(1, layerErrors)
        assertEquals(0, mapErrors)
    }

    @Test fun oldDisposalFailureDoesNotReportAfterReplacement() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var retryKey: androidx.compose.runtime.MutableState<Int>
        val firstCreated = CountDownLatch(1)
        val firstDestroyed = CountDownLatch(1)
        val secondReady = CountDownLatch(1)
        var errors = 0

        scenario.onActivity { activity ->
            retryKey = mutableStateOf(0)
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    retryKey = retryKey.value,
                    hostFactory = { context ->
                        val attempt = retryKey.value
                        if (attempt == 0) firstCreated.countDown()
                        object : AmapMapHost {
                            override val view = View(context)
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                if (attempt == 1) {
                                    listener?.invoke()
                                    secondReady.countDown()
                                }
                            }
                            override fun onCreate() = Unit
                            override fun onResume() = Unit
                            override fun onPause() = Unit
                            override fun onDestroy() {
                                if (attempt == 0) {
                                    firstDestroyed.countDown()
                                    error("old destroy")
                                }
                            }
                        }
                    },
                    onMapError = { errors++ },
                )
            }
        }

        assertTrue(firstCreated.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> retryKey.value++ }
        assertTrue(firstDestroyed.await(2, TimeUnit.SECONDS))
        assertTrue(secondReady.await(2, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }

        assertEquals(0, errors)
    }

    @Test fun retryKeyStartsNewAttemptAndReportsItsOwnFailureOnce() {
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        lateinit var retryKey: androidx.compose.runtime.MutableState<Int>
        val firstError = CountDownLatch(1)
        val secondError = CountDownLatch(1)
        var errors = 0
        scenario.onActivity { activity ->
            retryKey = mutableStateOf(0)
            activity.setContent {
                AmapComposeMap(
                    model = MapUiModel(),
                    onMarkerClick = {},
                    consent = token,
                    retryKey = retryKey.value,
                    hostFactory = { ctx -> object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() = throw IllegalStateException("create-${retryKey.value}")
                        override fun onResume() = Unit
                        override fun onPause() = Unit
                        override fun onDestroy() = Unit
                    } },
                    onMapError = {
                        errors++
                        if (errors == 1) firstError.countDown() else secondError.countDown()
                    },
                )
            }
        }
        assertTrue(firstError.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> retryKey.value++ }
        assertTrue(secondError.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> }
        assertEquals(2, errors)
    }

    @Test fun fakeHostKeepsLifecycleAndConsumesEachViewportRequestOnce() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = TestConsentGate()
        gate.show()
        val token = requireNotNull(gate.decide(true))
        var creations = 0
        var destroys = 0
        var creates = 0
        var resumes = 0
        val viewportIds = mutableListOf<Long>()
        var renderedMarker: MapMarkerUi? = null
        lateinit var lastOwner: TestOwner
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        scenario.onActivity { activity -> lastOwner = TestOwner(); ownerState = mutableStateOf(lastOwner) }
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
        scenario.onActivity { activity ->
            activity.setContent {
                Text("${state.value}")
                CompositionLocalProvider(LocalLifecycleOwner provides ownerState.value) {
                    AmapComposeMap(model.value, {}, token, hostFactory = { ctx ->
                        creations++
                        created.countDown()
                        object : AmapMapHost {
                            override val view: View = View(ctx)
                            override fun canRenderBeforeReady() = true
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

        scenario.onActivity { activity -> state.value++ }
        scenario.onActivity { activity -> model.value = model.value.copy(highlightedMarkerKey = "model-only") }
        scenario.onActivity { activity -> state.value++ }
        scenario.onActivity { activity -> }
        assertEquals(1, creations)
        assertEquals(listOf(1L), viewportIds)

        scenario.onActivity { activity -> model.value = model.value.copy(viewportRequest = request(2)) }
        assertTrue(secondViewport.await(5, TimeUnit.SECONDS))
        scenario.onActivity { activity -> state.value++ }
        scenario.onActivity { activity -> }
        assertEquals(listOf(1L, 2L), viewportIds)
        assertEquals(1, creations)

        scenario.onActivity { activity -> lastOwner = TestOwner(); ownerState.value = lastOwner }
        scenario.onActivity { activity -> }
        assertTrue(destroyed.await(5, TimeUnit.SECONDS))
        assertEquals(2, creations)
        assertEquals(2, creates)
        assertTrue(resumed.await(5, TimeUnit.SECONDS))
        assertEquals(1, destroys)
    }

    private fun waitUntil(timeoutMillis: Long = 5_000, predicate: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            if (predicate()) return true
            Thread.sleep(10)
        }
        return predicate()
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

    private class MutableLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = initialState }
        override val lifecycle: Lifecycle = registry
        fun moveTo(state: Lifecycle.State) {
            registry.currentState = state
        }
    }
}
