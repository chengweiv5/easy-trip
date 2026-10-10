package com.yangchengwei.easytrip.amap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Live SDK boundary: catalog coverage, title order and city-wide fit geometry. */
@RunWith(AndroidJUnit4::class)
class AmapTripCityLocatorSmokeTest {
    @Test fun resolvesFirstCityIncludingCountyLevelCitiesWithoutAddingPlaces() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
        val token = requireNotNull(TestConsentGate().apply { show() }.decide(true))
        withTimeout(40_000) {
            val city = lookupTripCity(context, token, "杭州·苏州五日游")
            assertEquals("杭州市", city?.name)
            assertEquals("330100", city?.code)
            assertEquals(2, city?.bounds?.size)
            assertTrue(city!!.bounds[0].latitude < city.bounds[1].latitude)
            val county = lookupTripCity(context, token, "登封·洛阳周末游")
            assertEquals("登封市", county?.name)
            assertNull(lookupTripCity(context, token, "周末去远一点"))
            println("AMAP_RESULT TRIP_CITY first=${city.name} county=${county?.name} bounds=${city.bounds}")
        }
    }
}
