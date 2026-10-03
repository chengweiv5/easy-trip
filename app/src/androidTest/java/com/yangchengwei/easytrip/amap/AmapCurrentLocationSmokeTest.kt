package com.yangchengwei.easytrip.amap

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import com.yangchengwei.easytrip.place.amap.AppLocationSession
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import org.junit.Test

/** Requires granted location, a configured key and an active system GPS test provider on an isolated emulator. */
class AmapCurrentLocationSmokeTest {
    @Test fun oneShotLocationResolvesCityUsingRealSdk() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val gate = TestConsentGate().apply { show() }
        val token = requireNotNull(gate.decide(true))
        val session = AppLocationSession(scope, { hasDeviceLocationPermission(context) },
            AmapCurrentLocation(context, token)::locate, AmapPlaceDataSource(context, token)::cityAt)
        ActivityScenario.launch(AmapAttachSmokeActivity::class.java).use {
            try {
                session.warmUp()
                val city = session.currentCity()
                assertTrue(city.name.isNotBlank())
                assertTrue(session.location != null)
                println("AMAP_RESULT CURRENT_LOCATION city=${city.name} location=${session.location}")
            } finally {
                session.close()
                scope.cancel()
            }
        }
    }
}
