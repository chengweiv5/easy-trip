package com.yangchengwei.easytrip.place.amap

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.domain.PlaceCity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.yangchengwei.easytrip.place.ui.PlaceSearchReducer
import com.yangchengwei.easytrip.place.ui.PlaceSearchPhase
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppLocationSessionTest {
    @Test fun eachExplicitSearchRetriesFailedLocationWithoutRequiringAppRestart() = runTest {
        var fixes = 0
        val session = AppLocationSession(backgroundScope, { true }, {
            if (++fixes <= 2) throw CurrentLocationUnavailable("定位失败（4），请检查系统定位服务后重试")
            GeoPoint(34.454, 113.050)
        }, { PlaceCity("登封市", "410185") })
        val searchedCities = mutableListOf<String?>()
        val delegate = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searchedCities += city
                return listOf(PlaceCandidate("poi", "西施猪蹄", "", null, null))
            }
        }
        session.warmUp()
        runCurrent()
        val reducer = PlaceSearchReducer(LocatedCitySearchSource(delegate, null, session), this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("西施猪蹄")
        advanceTimeBy(300); runCurrent()
        assertTrue(reducer.state.value.phase is PlaceSearchPhase.LocationFailure)
        assertEquals(1, fixes)

        reducer.submit()
        runCurrent()
        assertTrue(reducer.state.value.phase is PlaceSearchPhase.LocationFailure)
        assertEquals("Clicking search must start a new attempt after failure", 2, fixes)
        assertTrue("Never search without a resolved city", searchedCities.isEmpty())

        reducer.submit()
        runCurrent()
        assertEquals(PlaceSearchPhase.Results, reducer.state.value.phase)
        assertEquals(listOf("登封市"), searchedCities)
        assertEquals(3, fixes)
    }

    @Test fun explicitSearchKeepsSuccessfulLocationAndCityUntilTheyExpire() = runTest {
        var fixes = 0
        var cityLookups = 0
        val session = AppLocationSession(backgroundScope, { true }, {
            fixes++
            GeoPoint(34.454, 113.050)
        }, {
            cityLookups++
            PlaceCity("登封市", "410185")
        })
        val searchedCities = mutableListOf<String?>()
        val delegate = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searchedCities += city
                return emptyList()
            }
        }
        val reducer = PlaceSearchReducer(LocatedCitySearchSource(delegate, null, session), this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("西施猪蹄")
        advanceTimeBy(300); runCurrent()
        reducer.submit()
        runCurrent()
        reducer.retry()
        runCurrent()

        assertEquals(listOf("登封市", "登封市", "登封市"), searchedCities)
        assertEquals(1, fixes)
        assertEquals("Explicit search must not discard a usable resolved city", 1, cityLookups)
    }

    @Test fun explicitSearchWhileLocatingWaitsForTheSameRequest() = runTest {
        var fixes = 0
        val fix = CompletableDeferred<GeoPoint>()
        val session = AppLocationSession(backgroundScope, { true }, {
            fixes++
            fix.await()
        }, { PlaceCity("登封市", "410185") })
        val searchedCities = mutableListOf<String?>()
        val delegate = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searchedCities += city
                return emptyList()
            }
        }
        val reducer = PlaceSearchReducer(LocatedCitySearchSource(delegate, null, session), this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("西施猪蹄")
        advanceTimeBy(300); runCurrent()
        reducer.submit()
        runCurrent()
        reducer.submit()
        runCurrent()
        assertEquals(PlaceSearchPhase.Loading, reducer.state.value.phase)
        assertEquals(1, fixes)
        assertTrue(searchedCities.isEmpty())

        fix.complete(GeoPoint(34.454, 113.050))
        runCurrent()
        assertEquals(PlaceSearchPhase.Empty, reducer.state.value.phase)
        assertEquals(listOf("登封市"), searchedCities)
        assertEquals(1, fixes)
    }

    @Test fun explicitSearchRetriesCityLookupWithoutDiscardingTheAvailableCoordinate() = runTest {
        var fixes = 0
        var cityLookups = 0
        val session = AppLocationSession(backgroundScope, { true }, {
            fixes++
            GeoPoint(34.454, 113.050)
        }, {
            if (++cityLookups == 1) error("City service temporarily unavailable")
            PlaceCity("登封市", "410185")
        })
        val delegate = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                assertEquals("登封市", city)
                return emptyList()
            }
        }
        val reducer = PlaceSearchReducer(LocatedCitySearchSource(delegate, null, session), this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("西施猪蹄")
        advanceTimeBy(300); runCurrent()
        assertTrue(reducer.state.value.phase is PlaceSearchPhase.LocationFailure)
        reducer.submit()
        runCurrent()
        assertEquals(PlaceSearchPhase.Empty, reducer.state.value.phase)
        assertEquals(1, fixes)
        assertEquals(2, cityLookups)
    }

    @Test fun publishesPointBeforeCityAndClearsBothAfterPermissionRevocation() = runTest {
        var allowed = true
        val city = CompletableDeferred<PlaceCity>()
        val point = GeoPoint(30.0, 120.0)
        val session = AppLocationSession(backgroundScope, { allowed }, { point }, { city.await() })
        session.warmUp()
        runCurrent()
        assertEquals(point, session.position.value?.point)
        assertNull(session.position.value?.city)
        city.complete(PlaceCity("杭州市", "330100"))
        runCurrent()
        assertEquals("杭州市", session.position.value?.city?.name)
        allowed = false
        session.warmUp()
        assertNull(session.position.value)
        assertNull(session.location)
        session.close()
    }

    @Test fun explicitLocateReplacesCityWithoutPairingNewPointWithOldCity() = runTest {
        val nextCity = CompletableDeferred<PlaceCity>()
        val first = GeoPoint(30.0, 120.0)
        val second = GeoPoint(34.454, 113.05)
        val session = AppLocationSession(backgroundScope, { true }, { first }, {
            if (it == first) PlaceCity("杭州市", "330100") else nextCity.await()
        })
        session.currentCity()
        session.updateLocation(second)
        assertEquals(second, session.position.value?.point)
        assertNull(session.position.value?.city)
        nextCity.complete(PlaceCity("登封市", "410185"))
        runCurrent()
        assertEquals("登封市", session.position.value?.city?.name)
        session.close()
        assertNull(session.position.value)
    }

    @Test fun expiredFixRefreshesOnForegroundEntryWithoutBackgroundPolling() = runTest {
        var now = 0L
        var calls = 0
        val secondFix = CompletableDeferred<GeoPoint>()
        val session = AppLocationSession(backgroundScope, { true }, {
            if (++calls == 1) GeoPoint(30.0, 120.0) else secondFix.await()
        }, { PlaceCity("杭州市", "330100") }, { now })
        session.currentCity()
        now = 120_001
        runCurrent()
        assertEquals(1, calls)
        session.warmUp()
        assertNull(session.position.value)
        runCurrent()
        assertEquals(2, calls)
        secondFix.complete(GeoPoint(30.01, 120.0))
        assertEquals("杭州市", session.currentCity().name)
        assertEquals(GeoPoint(30.01, 120.0), session.position.value?.point)
        session.close()
    }

    @Test fun deniedOrInvalidLateFixNeverPublishesLocation() = runTest {
        var allowed = true
        val fix = CompletableDeferred<GeoPoint>()
        val session = AppLocationSession(backgroundScope, { allowed }, { fix.await() }, { PlaceCity("杭州市", "330100") })
        session.warmUp()
        runCurrent()
        allowed = false
        fix.complete(GeoPoint(30.0, 120.0))
        runCurrent()
        assertNull(session.position.value)
        assertTrue(runCatching { session.currentCity() }.exceptionOrNull() is LocationPermissionRequired)
        session.updateLocation(GeoPoint(30.0, 120.0))
        assertNull(session.position.value)
        session.close()
        val invalid = AppLocationSession(backgroundScope, { true }, { GeoPoint(0.0, 0.0) }, { PlaceCity("杭州市", "330100") })
        assertTrue(runCatching { invalid.currentCity() }.exceptionOrNull() is CurrentLocationUnavailable)
        assertNull(invalid.position.value)
        invalid.close()
    }

    @Test fun coldStartAndSearchesAcrossDestinationsShareOneLocationAndCity() = runTest {
        var fixes = 0
        var geocodes = 0
        val fix = CompletableDeferred<GeoPoint>()
        val session = AppLocationSession(
            scope = backgroundScope,
            hasPermission = { true },
            locate = { fixes++; fix.await() },
            resolveCity = { geocodes++; PlaceCity("登封市", "410185") },
        )
        session.warmUp()
        val first = async { session.currentCity() }
        val second = async { session.currentCity() }
        runCurrent()
        fix.complete(GeoPoint(34.454, 113.050))
        assertEquals("登封市", first.await().name)
        assertEquals(first.await(), second.await())
        session.warmUp() // Activity recreation / returning from background must not request again.
        assertEquals("登封市", session.currentCity().name)
        assertEquals(1, fixes)
        assertEquals(1, geocodes)
    }

    @Test fun startupWithoutPermissionDoesNotLocateAndSearchExplainsPermissionInsteadOfNetworkError() = runTest {
        var allowed = false
        var fixes = 0
        val session = AppLocationSession(backgroundScope, { allowed }, {
            fixes++
            GeoPoint(34.454, 113.050)
        }, { PlaceCity("登封市", "410185") })
        val delegate = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        }
        session.warmUp()
        val reducer = PlaceSearchReducer(LocatedCitySearchSource(delegate, null, session), this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("西施猪蹄")
        advanceUntilIdle()
        assertEquals(0, fixes)
        assertEquals(PlaceSearchPhase.LocationPermissionRequired, reducer.state.value.phase)
        allowed = true
        assertEquals("登封市", session.currentCity().name)
        assertEquals(1, fixes)
    }

    @Test fun failedLocationStaysExplicitUntilRetryAndNeverFallsBackToNationwide() = runTest {
        var fixes = 0
        var searches = 0
        val session = AppLocationSession(backgroundScope, { true }, {
            if (++fixes == 1) error("Location unavailable")
            GeoPoint(34.454, 113.050)
        }, { PlaceCity("登封市", "410185") })
        val delegate = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searches++
                assertEquals("登封市", city)
                return emptyList()
            }
        }
        val reducer = PlaceSearchReducer(LocatedCitySearchSource(delegate, null, session), this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("西施猪蹄")
        advanceTimeBy(300); runCurrent()
        assertTrue(reducer.state.value.phase is PlaceSearchPhase.LocationFailure)
        reducer.setQuery("登封西施猪蹄")
        advanceTimeBy(300); runCurrent()
        assertEquals(0, searches)
        assertEquals(1, fixes)
        session.retry()
        reducer.retry()
        runCurrent()
        assertEquals(PlaceSearchPhase.Empty, reducer.state.value.phase)
        assertEquals(2, fixes)
    }

    @Test fun manualLocateWinsOverLateStartupFixAndEverySearchSeesTheNewCity() = runTest {
        val oldFix = CompletableDeferred<GeoPoint>()
        val session = AppLocationSession(backgroundScope, { true }, {
            withContext(NonCancellable) { oldFix.await() }
        }, { if (it.longitude == 113.05) PlaceCity("登封市", "410185") else PlaceCity("杭州市", "330100") })
        session.warmUp()
        val waitingSearch = async { session.currentCity() }
        runCurrent()
        session.updateLocation(GeoPoint(34.454, 113.05))
        oldFix.complete(GeoPoint(30.2741, 120.1551))
        assertEquals("登封市", waitingSearch.await().name)
        assertEquals(GeoPoint(34.454, 113.05), session.location)
        session.updateLocation(GeoPoint(30.2741, 120.1551))
        assertEquals("杭州市", session.currentCity().name)
    }

    @Test fun cancelledSearchDoesNotCancelTheSharedStartupFixButClosingSessionDoes() = runTest {
        val fix = CompletableDeferred<GeoPoint>()
        var stopped = false
        val session = AppLocationSession(backgroundScope, { true }, {
            try { fix.await() } finally { stopped = true }
        }, { PlaceCity("登封市", "410185") })
        session.warmUp()
        val search = async { session.currentCity() }
        runCurrent()
        search.cancel()
        runCurrent()
        assertFalse(stopped)
        session.close()
        runCurrent()
        assertTrue(stopped)
        assertTrue(runCatching { session.currentCity() }.isFailure)
    }

    @Test fun locationOrCityLookupThatNeverRepliesTimesOutAsLocationFailure() = runTest {
        val session = AppLocationSession(backgroundScope, { true },
            { GeoPoint(34.454, 113.050) }, { awaitCancellation() })
        val result = async { runCatching { session.currentCity() } }
        runCurrent()
        advanceTimeBy(20_001)
        runCurrent()
        assertTrue("Location and reverse geocode must have an overall timeout", result.isCompleted)
        assertTrue(result.await().exceptionOrNull() is CurrentLocationUnavailable)
    }
}
