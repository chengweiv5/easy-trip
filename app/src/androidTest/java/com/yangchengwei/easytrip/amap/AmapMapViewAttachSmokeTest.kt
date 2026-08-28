package com.yangchengwei.easytrip.amap

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
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
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AmapMapViewAttachSmokeTest {
    @Test
    fun realMapViewAttachesLoadsAndSurvives() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val runToken = InstrumentationRegistry.getArguments().getString("amapRunToken").orEmpty()
        assumeTrue("AmapMapViewAttachSmokeTest requires the dedicated smoke runner", runToken.isNotEmpty())
        check(runToken.matches(Regex("[A-Za-z0-9._-]+"))) { "amapRunToken must contain only letters, digits, dots, underscores, or hyphens" }
        val status = linkedMapOf<String, Any>("run_token" to runToken)
        fun persistStatus() {
            context.openFileOutput("amap-smoke-$runToken.json", android.content.Context.MODE_PRIVATE).use {
                it.write(JSONObject(status).toString().toByteArray())
            }
        }
        persistStatus()
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        check(info.metaData?.getString("com.amap.api.v2.apikey").orEmpty().isNotBlank()) { "AMAP_API_KEY is required" }
        TestConsentGate().apply {
            show()
            assertNotNull(decide(true))
        }

        val loaded = CountDownLatch(1)
        lateinit var mapView: MapView
        ActivityScenario.launch(AmapAttachSmokeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                mapView = MapView(activity)
                activity.attach(mapView)
                mapView.onCreate(null)
                mapView.map.setOnMapLoadedListener(AMap.OnMapLoadedListener { loaded.countDown() })
                mapView.onResume()
                assertTrue(mapView.isAttachedToWindow)
            }
            assertTrue("AMap did not report map-loaded within 20 seconds", loaded.await(20, TimeUnit.SECONDS))
            status["map_loaded_at"] = System.currentTimeMillis()
            persistStatus()
            Log.i("AMAP_SMOKE", "map_loaded=true")
            SystemClock.sleep(5_000)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val processAlive = instrumentation.uiAutomation.executeShellCommand("pidof ${context.packageName}").use {
                ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().isNotEmpty()
            }
            assertTrue(processAlive)
            status["stable_alive_at"] = System.currentTimeMillis()
            persistStatus()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            assertTrue(screenshot.width > 0 && screenshot.height > 0)
            context.openFileOutput("amap-smoke-$runToken.png", android.content.Context.MODE_PRIVATE).use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            status["screenshot_ready"] = true
            persistStatus()
            Log.i("AMAP_SMOKE", "screenshot_ready=true")
            scenario.onActivity {
                assertTrue(mapView.isAttachedToWindow)
                mapView.onPause()
                mapView.onDestroy()
                it.detach(mapView)
                status["lifecycle_cleanup"] = true
                persistStatus()
                Log.i("AMAP_SMOKE", "lifecycle_cleanup=true")
            }
        }
    }
}
