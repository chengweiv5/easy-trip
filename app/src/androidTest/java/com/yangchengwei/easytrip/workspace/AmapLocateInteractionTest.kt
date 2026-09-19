package com.yangchengwei.easytrip.workspace

import android.location.Location
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amap.api.maps.AMap
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.yangchengwei.easytrip.amap.AmapAttachSmokeActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AmapLocateInteractionTest {
    @Test fun draggingAfterLocateKeepsCameraAtDraggedPositionAcrossLocationUpdates() {
        verifyDragWinsOverLocationUpdates(firstFixBeforeDrag = true)
    }

    @Test fun draggingBeforeFirstFixCancelsPendingRecenter() {
        verifyDragWinsOverLocationUpdates(firstFixBeforeDrag = false)
    }

    private fun verifyDragWinsOverLocationUpdates(firstFixBeforeDrag: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        val loaded = CountDownLatch(1)
        val source = TestLocationSource()
        lateinit var host: RealAmapMapHost
        lateinit var map: AMap
        var gestures = 0
        ActivityScenario.launch(AmapAttachSmokeActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    host = RealAmapMapHost(activity)
                    map = (host.view as MapView).map
                    activity.attach(host.view as MapView)
                    host.onCreate()
                    map.setLocationSource(source)
                    host.setOnReadyListener { loaded.countDown() }
                    host.setOnUserGestureListener { gestures++ }
                    host.onResume()
                }
                assertTrue("Real AMap must load", loaded.await(20, TimeUnit.SECONDS))
                scenario.onActivity { host.showCurrentLocation() }
                assertTrue("Location source must activate", source.activated.await(5, TimeUnit.SECONDS))
                if (firstFixBeforeDrag) {
                    scenario.onActivity { source.emit(30.25, 120.15) }
                    SystemClock.sleep(1_500)
                }
                var located = LatLng(0.0, 0.0)
                scenario.onActivity { located = map.cameraPosition.target }
                if (firstFixBeforeDrag) {
                    assertEquals("Locate must reach the supplied fix", 30.25, located.latitude, .001)
                    assertEquals(120.15, located.longitude, .001)
                }

                repeat(2) { locateRound ->
                    if (locateRound > 0) {
                        scenario.onActivity {
                            host.showCurrentLocation()
                            source.emit(30.25, 120.15)
                        }
                        SystemClock.sleep(1_000)
                        scenario.onActivity {
                            assertEquals("Another locate click must recenter", 120.15, map.cameraPosition.target.longitude, .001)
                        }
                    }
                    val downTime = SystemClock.uptimeMillis()
                    for (step in 0..12) {
                        scenario.onActivity {
                            val action = when (step) { 0 -> MotionEvent.ACTION_DOWN; 12 -> MotionEvent.ACTION_UP; else -> MotionEvent.ACTION_MOVE }
                            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                                host.view.width * (.75f - step / 12f * .5f), host.view.height * .4f, 0)
                            host.view.dispatchTouchEvent(event)
                            event.recycle()
                        }
                        SystemClock.sleep(30)
                    }
                    SystemClock.sleep(500)
                    var dragged = LatLng(0.0, 0.0)
                    scenario.onActivity {
                        dragged = map.cameraPosition.target
                        assertTrue("The drag must reach the production gesture listener", gestures > 0)
                    }
                    assertTrue("The map must move away from the located position", kotlin.math.abs(dragged.longitude - located.longitude) > .0001)
                    repeat(3) { index ->
                        scenario.onActivity { source.emit(30.25 + index * .00001, 120.15) }
                        SystemClock.sleep(600)
                    }
                    scenario.onActivity {
                        val afterUpdates = map.cameraPosition.target
                        assertEquals("Location updates must not pull the map back after drag, round=$locateRound", dragged.longitude, afterUpdates.longitude, .0001)
                        assertEquals(dragged.latitude, afterUpdates.latitude, .0001)
                    }
                }
            } finally {
                scenario.onActivity { activity ->
                    host.onPause()
                    host.onDestroy()
                    activity.detach(host.view as MapView)
                }
            }
        }
    }

    private class TestLocationSource : LocationSource {
        val activated = CountDownLatch(1)
        private var listener: LocationSource.OnLocationChangedListener? = null
        override fun activate(listener: LocationSource.OnLocationChangedListener) {
            this.listener = listener
            activated.countDown()
        }
        override fun deactivate() { listener = null }
        fun emit(latitude: Double, longitude: Double) {
            listener?.onLocationChanged(Location("test").apply {
                this.latitude = latitude
                this.longitude = longitude
                accuracy = 5f
                time = System.currentTimeMillis()
            })
        }
    }
}
