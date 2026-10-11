package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.workspace.TripCity
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import com.yangchengwei.easytrip.place.ui.PlaceSearchReducer
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TripSearchCityResolverTest {
    private val xuzhou = TripCity("320300", "徐州市", GeoPoint(34.26, 117.18))
    private val suzhou = TripCity("320500", "苏州市", GeoPoint(31.30, 120.58))
    private val shanghai = TripCity("310000", "上海市", GeoPoint(31.23, 121.47))
    private val catalog = listOf(shanghai, suzhou, xuzhou)

    @Test fun singleTripCityOverridesDeviceLocation() = runTest {
        val resolver = TripSearchCityResolver(
            tripTitle = { "徐州周末游" },
            cityCatalog = { catalog },
            locatedCity = { PlaceCity("上海市", "310000") },
        )
        assertEquals("徐州市", resolver.resolve())
    }

    @Test fun multipleTripCitiesPreferDeviceCityOnlyWhenItBelongsToTrip() = runTest {
        var location = PlaceCity("苏州市", "320500")
        val resolver = TripSearchCityResolver({ "徐州·苏州" }, { catalog }, { location })
        assertEquals("苏州市", resolver.resolve())
        location = PlaceCity("上海市", "310000")
        assertEquals("徐州市", resolver.resolve())
    }

    @Test fun titleWithoutCityFallsBackToLocation() = runTest {
        val resolver = TripSearchCityResolver({ "周末出游" }, { catalog }, { PlaceCity("上海市", "310000") })
        assertEquals("上海市", resolver.resolve())
    }

    @Test fun failedLocationDoesNotBlockACityNamedInTrip() = runTest {
        val resolver = TripSearchCityResolver({ "苏州·徐州" }, { catalog }, { throw LocationPermissionRequired() })
        assertEquals("苏州市", resolver.resolve())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun searchBoxExplicitCityReachesProviderEvenWithoutLocation() = runTest {
        val requests = mutableListOf<Pair<String, String?>>()
        val resolver = TripSearchCityResolver({ "徐州" }, { catalog }, { throw LocationPermissionRequired() })
        val source = TripCitySearchSource(object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                requests += keyword to city
                return emptyList()
            }
        }, resolver)
        val search = PlaceSearchReducer(source, this, StandardTestDispatcher(testScheduler))
        search.setQuery("苏州 博物馆")
        advanceUntilIdle()
        assertEquals(listOf("苏州 博物馆" to "苏州市"), requests)
        search.setQuery("博物馆")
        advanceUntilIdle()
        assertEquals("博物馆" to "徐州市", requests.last())
    }

    @Test fun manualCityDoesNotDependOnCatalogOrLocation() = runTest {
        val resolver = TripSearchCityResolver({ error("no title needed") }, { error("no catalog needed") },
            { error("no location needed") })
        assertEquals("北京", resolver.resolve(explicitCity = " 北京 "))
    }

    @Test fun repeatedCitiesAndServiceOrderingDoNotChangeFirstCity() = runTest {
        val resolver = TripSearchCityResolver({ "苏州·徐州·苏州市" }, { catalog }, { null })
        assertEquals("苏州市", resolver.resolve())
    }

    @Test fun noCityAndNoLocationDoesNotBecomeNationwideSearch() = runTest {
        val resolver = TripSearchCityResolver({ "周末出游" }, { catalog }, { null })
        val source = TripCitySearchSource(object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> = error("Must not search nationwide")
        }, resolver)
        try { source.search("博物馆", null); fail("Expected missing city") }
        catch (_: CurrentLocationUnavailable) { }
    }

    @Test fun locationPermissionFailureIsPreservedWhenTitleHasNoCity() = runTest {
        val resolver = TripSearchCityResolver({ "周末出游" }, { catalog }, { throw LocationPermissionRequired() })
        try { resolver.resolve(); fail("Expected permission failure") }
        catch (_: LocationPermissionRequired) { }
    }

    @Test fun catalogFailureIsNotTreatedAsACitylessTitle() = runTest {
        val resolver = TripSearchCityResolver({ "徐州" }, { throw IllegalStateException("catalog unavailable") },
            { error("Must not silently use phone city") })
        try { resolver.resolve(); fail("Expected catalog failure") }
        catch (e: IllegalStateException) { assertEquals("catalog unavailable", e.message) }
    }

    @Test fun slowLocationFallsBackToFirstCityWithinBoundedTime() = runTest {
        val resolver = TripSearchCityResolver({ "徐州·苏州" }, { catalog }, { CompletableDeferred<PlaceCity>().await() })
        assertEquals("徐州市", resolver.resolve())
        assertEquals(3_000, testScheduler.currentTime)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun cancellationNeverPublishesAFallbackCity() = runTest {
        val resolver = TripSearchCityResolver({ "徐州·苏州" }, { catalog }, { CompletableDeferred<PlaceCity>().await() })
        var published = false
        val job = launch { resolver.resolve(); published = true }
        runCurrent(); job.cancel(); advanceUntilIdle()
        assertFalse(published)
    }

    @Test fun countyLocationBelongsToItsPrefectureCity() = runTest {
        val resolver = TripSearchCityResolver({ "徐州·苏州" }, { catalog }, { PlaceCity("昆山市", "320583") })
        assertEquals("苏州市", resolver.resolve())
    }

    @Test fun explicitlyNamedCountyWinsOverItsContainingCity() = runTest {
        val kunshan = TripCity("320583", "昆山市", suzhou.center)
        val resolver = TripSearchCityResolver({ "徐州·苏州·昆山" }, { catalog + kunshan }, { PlaceCity("昆山市", "320583") })
        assertEquals("昆山市", resolver.resolve())
    }

    @Test fun roadNamesAreNotExplicitCityOverrides() = runTest {
        val nanjing = TripCity("320100", "南京市", suzhou.center)
        val resolver = TripSearchCityResolver({ "上海旅行" }, { catalog + nanjing }, { null })
        assertEquals("上海市", resolver.resolve("南京路"))
        assertEquals("上海市", resolver.resolve("南京路步行街"))
        assertEquals("苏州市", resolver.resolve("苏州 南京路"))
        assertEquals("苏州市", resolver.resolve("苏州的博物馆"))
    }

    @Test fun catalogTimeoutBecomesRetryableSearchFailure() = runTest {
        var slow = true
        val resolver = TripSearchCityResolver({ "徐州" }, {
            if (slow) CompletableDeferred<List<TripCity>>().await() else catalog
        }, { null })
        val source = TripCitySearchSource(object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        }, resolver)
        val reducer = PlaceSearchReducer(source, this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("博物馆"); advanceUntilIdle()
        assertTrue(reducer.state.value.phase is com.yangchengwei.easytrip.place.ui.PlaceSearchPhase.NetworkFailure)
        slow = false
        reducer.submit(); advanceUntilIdle()
        assertEquals(com.yangchengwei.easytrip.place.ui.PlaceSearchPhase.Empty, reducer.state.value.phase)
    }
}
