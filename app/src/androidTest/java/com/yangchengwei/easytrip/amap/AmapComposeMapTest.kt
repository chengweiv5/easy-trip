package com.yangchengwei.easytrip.amap

import android.content.pm.PackageManager
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.view.View
import com.amap.api.maps.MapView
import com.yangchengwei.easytrip.MainActivity
import com.yangchengwei.easytrip.workspace.AmapComposeMap
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.MapUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AmapComposeMapTest {
    @get:Rule val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test fun recompositionKeepsSingleRealMapView() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assumeTrue(info.metaData?.getString("com.amap.api.v2.apikey").orEmpty().isNotBlank())
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        var creations = 0
        var renders = 0
        var destroys = 0
        var creates = 0
        var resumes = 0
        lateinit var lastOwner: TestOwner
        lateinit var ownerState: androidx.compose.runtime.MutableState<TestOwner>
        rule.scenario.onActivity { lastOwner = TestOwner(); ownerState = mutableStateOf(lastOwner) }
        val created = CountDownLatch(1)
        val destroyed = CountDownLatch(1)
        val resumed = CountDownLatch(2)
        val recomposed = CountDownLatch(2)
        val state = mutableStateOf(0)
        rule.scenario.onActivity { activity ->
            activity.setContent {
                Text("${state.value}")
                CompositionLocalProvider(LocalLifecycleOwner provides ownerState.value) {
                AmapComposeMap(MapUiModel(), {}, token, hostFactory = { ctx ->
                    creations++
                    created.countDown()
                    object : AmapMapHost {
                        override val view: View = View(ctx)
                        override fun onCreate() { creates++ }
                        override fun onResume() { resumes++; resumed.countDown() }
                        override fun onPause() = Unit
                        override fun onDestroy() { destroys++; destroyed.countDown() }
                        override fun render(model: MapUiModel, onMarkerClick: (String) -> Unit) { renders++; recomposed.countDown() }
                    }
                })
                }
            }
        }
        assertTrue(created.await(5, TimeUnit.SECONDS))
        rule.scenario.onActivity { state.value++ }
        assertTrue(recomposed.await(5, TimeUnit.SECONDS))
        assertEquals(1, creations)
        assertTrue(renders >= 2)
        rule.scenario.onActivity { lastOwner = TestOwner(); ownerState.value = lastOwner }
        while (creations < 2) Thread.sleep(10)
        assertTrue(destroyed.await(5, TimeUnit.SECONDS))
        assertEquals(2, creations)
        assertEquals(2, creates)
        assertTrue(resumed.await(5, TimeUnit.SECONDS))
        assertTrue(resumes >= 2)
        assertEquals(1, destroys)
        rule.scenario.onActivity { lastOwner.destroy() }
        while (destroys < 2) Thread.sleep(10)
        assertEquals(2, destroys)
    }
    private class TestOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle = registry
        fun destroy() { registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY) }
    }
}
