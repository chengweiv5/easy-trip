package com.yangchengwei.easytrip.amap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.place.amap.AppLocationSession
import com.yangchengwei.easytrip.place.amap.LocatedCitySearchSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Live AMap regression: requires network and a configured key; only the device fix is supplied. */
@RunWith(AndroidJUnit4::class)
class AmapCitySearchRegressionTest {
    @Test
    fun fullCityNameFindsDengfengStoreWhenLocatedInLuoyang() = runBlocking {
        assertNanhuanStore("登封市西施猪蹄", GeoPoint(34.6197, 112.4540), "洛阳市")
    }

    @Test
    fun shortCityNameFindsDengfengStoreWhenLocatedInLuoyang() = runBlocking {
        assertNanhuanStore("登封西施猪蹄", GeoPoint(34.6197, 112.4540), "洛阳市")
    }

    @Test
    fun unqualifiedKeywordStillFindsLocalDengfengStore() = runBlocking {
        assertNanhuanStore("西施猪蹄", GeoPoint(34.454, 113.050), "登封市")
    }

    private suspend fun assertNanhuanStore(keyword: String, fix: GeoPoint, expectedCity: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AmapPrivacyGate.create(context).apply {
            reportShown()
            reportDecision(true)
        }
        val token = requireNotNull(TestConsentGate().apply { show() }.decide(true))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val dataSource = AmapPlaceDataSource(context, token)
        val locationSession = AppLocationSession(
            scope,
            hasPermission = { true },
            locate = { fix },
            resolveCity = dataSource::cityAt,
        )
        try {
            locationSession.warmUp()
            withTimeout(30_000) {
                assertEquals(expectedCity, locationSession.currentCity().name)
                val places = LocatedCitySearchSource(dataSource, null, locationSession).search(keyword, null)
                val target = places.find { it.poiId == "B0G07S31JG" }
                println(
                    "AMAP_RESULT CITY_SEARCH keyword=$keyword currentCity=$expectedCity " +
                        "count=${places.size} target=${target?.poiId} address=${target?.address}",
                )
                assertNotNull("Must find 西施猪蹄 at 南一环108号, not unrelated local stores", target)
                assertEquals("登封市", target!!.cityName)
                assertEquals("西施猪蹄", target.name)
                assertTrue(target.address.contains("南一环108号"))
                assertTrue("Search should return Dengfeng stores", places.all { it.cityName == "登封市" })
            }
        } finally {
            locationSession.close()
            scope.cancel()
        }
    }
}
