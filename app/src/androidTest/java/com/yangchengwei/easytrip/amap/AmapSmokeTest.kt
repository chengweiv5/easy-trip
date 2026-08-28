package com.yangchengwei.easytrip.amap

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amap.api.maps.MapView
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.route.amap.AmapRouteDataSource
import com.yangchengwei.easytrip.route.domain.RouteMode
import com.yangchengwei.easytrip.route.domain.RouteRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmapSmokeTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun poiSearchReturnsForbiddenCityCandidate() = runBlocking {
        assumeConfiguredKey()
        val places = AmapPlaceDataSource(context, consent()).search("故宫博物院", "北京")
        assertTrue(places.isNotEmpty())
        assertTrue(places.all { it.poiId.isNotBlank() && it.name.isNotBlank() })
        println("AMAP_RESULT POI count=${places.size} first=${places.first().name}")
    }

    @Test fun walkAndDriveRoutesContainUsableGeometry() = runBlocking {
        assumeConfiguredKey()
        val source = AmapRouteDataSource(context, consent())
        val origin = GeoPoint(39.9087, 116.3975)
        val destination = GeoPoint(39.9163, 116.3972)
        listOf(RouteMode.WALK, RouteMode.DRIVE).forEach { mode ->
            val route = source.plan(RouteRequest(origin, destination, mode))
            assertTrue(route.distanceMeters > 0 && route.durationSeconds > 0 && route.polyline.size >= 2)
            println("AMAP_RESULT $mode distance=${route.distanceMeters} duration=${route.durationSeconds} points=${route.polyline.size}")
        }
    }


    @Test fun concurrentWalkAndDriveRequestsStayIsolated() = runBlocking {
        assumeConfiguredKey()
        val source = AmapRouteDataSource(context, consent())
        val origin = GeoPoint(39.9087, 116.3975)
        val destination = GeoPoint(39.9163, 116.3972)
        val results = listOf(RouteMode.WALK, RouteMode.DRIVE).map { mode ->
            async { mode to source.plan(RouteRequest(origin, destination, mode)) }
        }.awaitAll().toMap()
        assertTrue(results.getValue(RouteMode.WALK).polyline.size >= 2)
        assertTrue(results.getValue(RouteMode.DRIVE).polyline.size >= 2)
    }

    @Test fun transitReturnsUsableRouteOrStructuredFailure() {
        runBlocking {
            assumeConfiguredKey()
            val request = RouteRequest(GeoPoint(39.9087, 116.3975), GeoPoint(39.9163, 116.3972), RouteMode.TRANSIT, "北京", "北京")
            runCatching { AmapRouteDataSource(context, consent()).plan(request) }
                .onSuccess {
                    assertTrue(it.distanceMeters > 0 && it.durationSeconds > 0 && it.polyline.size >= 2)
                    println("AMAP_RESULT TRANSIT distance=${it.distanceMeters} duration=${it.durationSeconds} points=${it.polyline.size}")
                }
                .onFailure {
                    assertTrue(it is AmapServiceException)
                    it as AmapServiceException
                    assertTrue(it.operation == "TRANSIT_ROUTE" && it.errorCode == 1000 && it.message.orEmpty().contains("no usable path"))
                    println("AMAP_RESULT TRANSIT structuredFailure=${it.message}")
                }
        }
    }

    @Test fun mapViewCreatesAfterPrivacyConsent() {
        assumeConfiguredKey()
        consent()
        val mapView = MapView(context)
        mapView.onCreate(null)
        assertTrue(mapView.map != null)
        mapView.onResume()
        mapView.onPause()
        mapView.onDestroy()
        println("AMAP_RESULT MAP created lifecycle=onCreate,onResume,onPause,onDestroy")
    }

    private fun assumeConfiguredKey() {
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assumeTrue(info.metaData?.getString("com.amap.api.v2.apikey").orEmpty().isNotBlank())
    }

    private fun consent(): AmapConsentToken {
        val gate = TestConsentGate()
        gate.show()
        return requireNotNull(gate.decide(true))
    }
}
