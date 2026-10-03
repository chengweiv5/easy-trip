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
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppLocationSessionTest {
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
