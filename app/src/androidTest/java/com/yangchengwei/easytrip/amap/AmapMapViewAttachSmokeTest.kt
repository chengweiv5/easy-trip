package com.yangchengwei.easytrip.amap

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AmapMapViewAttachSmokeTest {
    @Test
    fun realMapViewAttachesAndSurvivesUntilLoadedOrStable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assumeTrue(info.metaData?.getString("com.amap.api.v2.apikey").orEmpty().isNotBlank())
        AmapPrivacyGate.create(context).apply {
            reportPrivacyShown()
            assertNotNull(reportUserDecision(true))
        }

        val loaded = CountDownLatch(1)
        lateinit var mapView: MapView
        ActivityScenario.launch(AmapAttachSmokeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                mapView = MapView(activity)
                activity.attach(mapView)
                mapView.onCreate(null)
                mapView.onResume()
                mapView.map.setOnMapLoadedListener(AMap.OnMapLoadedListener { loaded.countDown() })
                assertTrue(mapView.isAttachedToWindow)
            }
            val mapLoaded = loaded.await(20, TimeUnit.SECONDS)
            if (!mapLoaded) SystemClock.sleep(5_000)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val processAlive = instrumentation.uiAutomation.executeShellCommand("pidof ${context.packageName}").use {
                ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().isNotEmpty()
            }
            assertTrue(processAlive)
            scenario.onActivity {
                assertTrue(mapView.isAttachedToWindow)
                mapView.onPause()
                mapView.onDestroy()
                it.detach(mapView)
            }
        }
    }
}

class AmapAttachSmokeActivity : ComponentActivity() {
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        setContentView(container)
    }

    fun attach(view: MapView) = container.addView(
        view,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
    )

    fun detach(view: MapView) = container.removeView(view)
}
