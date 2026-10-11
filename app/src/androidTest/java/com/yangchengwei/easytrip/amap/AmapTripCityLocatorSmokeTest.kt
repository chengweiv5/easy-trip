package com.yangchengwei.easytrip.amap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Live SDK boundary: catalog coverage, title order and city-wide fit geometry. */
@RunWith(AndroidJUnit4::class)
class AmapTripCityLocatorSmokeTest {
    @Test fun xuzhouAssistantCityFindsBothNamesWithoutAddingPlaces() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
        val token = requireNotNull(TestConsentGate().apply { show() }.decide(true))
        withTimeout(40_000) {
            val city = withTimeout(12_000) {
                lookupTripCity(context, token, "徐州", requireSingleCity = true)
            }
            assertEquals("徐州市", city?.name)
            assertEquals("320300", city?.code)
            val source = AmapPlaceDataSource(context, token)
            for (name in listOf("云龙湖", "云龙山")) {
                val results = source.search(name, requireNotNull(city).name)
                assertTrue("$name should resolve within the trip city", results.isNotEmpty())
                assertTrue(results.all { it.cityName == "徐州市" })
                assertTrue(results.any { it.name.contains(name) })
                println("AMAP_RESULT ASSISTANT_CITY city=${city.name} query=$name count=${results.size}")
            }
            assertNull(lookupTripCity(context, token, "徐州·南京", requireSingleCity = true))
        }
    }

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
