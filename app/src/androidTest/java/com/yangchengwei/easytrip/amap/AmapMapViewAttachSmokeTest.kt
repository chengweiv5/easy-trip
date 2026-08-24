package com.yangchengwei.easytrip.amap

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AmapMapViewAttachSmokeTest {
    @Test
    fun realMapViewAttachesLoadsAndSurvives() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        check(info.metaData?.getString("com.amap.api.v2.apikey").orEmpty().isNotBlank()) { "AMAP_API_KEY is required" }
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
            assertTrue("AMap did not report map-loaded within 20 seconds", loaded.await(20, TimeUnit.SECONDS))
            println("AMAP_SMOKE map_loaded=true")
            SystemClock.sleep(5_000)
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
                println("AMAP_SMOKE lifecycle_cleanup=true")
            }
        }
    }
}
