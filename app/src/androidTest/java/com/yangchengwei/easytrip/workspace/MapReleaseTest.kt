package com.yangchengwei.easytrip.workspace

import android.os.Looper
import android.view.Choreographer
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.yangchengwei.easytrip.amap.AmapAttachSmokeActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MapReleaseTest {
    @Test fun releaseRunsAfterNextFrameOnMainThread() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val released = CountDownLatch(1)
        var frameReached = false
        var releaseOnMain = false
        instrumentation.runOnMainSync {
            Choreographer.getInstance().postFrameCallback { frameReached = true }
            AndroidMapReleaseScheduler.afterNextFrame {
                releaseOnMain = Looper.myLooper() == Looper.getMainLooper()
                assertTrue("The next frame must finish before cleanup", frameReached)
                released.countDown()
            }
            assertEquals("Release must not block the current navigation frame", 1L, released.count)
        }
        assertTrue("Queued cleanup must run", released.await(5, TimeUnit.SECONDS))
        assertTrue(releaseOnMain)
    }

    @Test fun realMapStopsLocationImmediatelyAndReleasesExactlyOnceAfterRemoval() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        val loaded = CountDownLatch(1)
        val released = CountDownLatch(1)
        var releaseRequests = 0
        var locationActive = false
        lateinit var host: RealAmapMapHost
        var pendingRelease: (() -> Unit)? = null
        val glThreadsBefore = glThreadIds()
        ActivityScenario.launch(AmapAttachSmokeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                host = RealAmapMapHost(activity, MapReleaseScheduler { release ->
                    releaseRequests++
                    pendingRelease = release
                })
                val view = host.view as MapView
                activity.attach(view)
                host.onCreate()
                view.map.setLocationSource(object : LocationSource {
                    override fun activate(listener: LocationSource.OnLocationChangedListener) { locationActive = true }
                    override fun deactivate() { locationActive = false }
                })
                host.setOnReadyListener { loaded.countDown() }
                host.onResume()
            }
            assertTrue("Real map must load", loaded.await(20, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                host.showCurrentLocation()
                assertTrue(locationActive)
                host.onPause()
                host.onDestroy()
                host.onDestroy()
                assertFalse("Location must stop without awaiting deferred cleanup", locationActive)
                assertEquals("Disposal is idempotent", 1, releaseRequests)
                assertTrue("SDK release must be queued", pendingRelease != null)
                activity.detach(host.view as MapView)
                AndroidMapReleaseScheduler.afterNextFrame {
                    assertFalse("Release follows removal from the window", host.view.isAttachedToWindow)
                    checkNotNull(pendingRelease).invoke()
                    pendingRelease = null
                    released.countDown()
                }
            }
            assertTrue("SDK release must complete", released.await(5, TimeUnit.SECONDS))
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while ((glThreadIds() - glThreadsBefore).isNotEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(20)
            }
            assertTrue("Map GL threads must terminate", (glThreadIds() - glThreadsBefore).isEmpty())
        }
    }

    private fun glThreadIds() = Thread.getAllStackTraces().keys
        .filter { it.isAlive && it.name.startsWith("GLThread") }
        .map { it.id }.toSet()
}
