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
import com.yangchengwei.easytrip.workspace.MapUiModel
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
        lateinit var lastOwner: TestOwner
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        rule.scenario.onActivity { lastOwner = TestOwner(); ownerState = mutableStateOf(lastOwner) }
        val created = CountDownLatch(1)
        val destroyed = CountDownLatch(1)
        val resumed = CountDownLatch(2)
        val initialViewport = CountDownLatch(1)
        val secondViewport = CountDownLatch(1)
        val state = mutableStateOf(0)
        val model = mutableStateOf(MapUiModel(viewportRequest = request(1)))
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
