package com.yangchengwei.easytrip.workspace

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.amap.LocatedCitySearchSource
import com.yangchengwei.easytrip.place.domain.PlaceCity
import com.yangchengwei.easytrip.place.ui.PlaceSearchReducer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkspaceMapSessionTest {
    private val hangzhou = GeoPoint(30.2741, 120.1551)

    @Test fun locatedCityReachesActualSearchRequestInsteadOfBeijingDefault() = runTest {
        val session = WorkspaceMapSession(SavedStateHandle())
        session.onLocated(hangzhou)
        val requestedCities = mutableListOf<String?>()
        val source = object : PlaceSearchDataSource {
            override suspend fun cityAt(point: GeoPoint): PlaceCity {
                assertEquals(hangzhou, point)
                return PlaceCity("杭州市", "330100")
            }
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                requestedCities += city
                return listOf(PlaceCandidate(
                    "museum", if (city == "杭州市") "杭州博物馆" else "北京博物馆",
                    "", hangzhou, null,
                ))
            }
        }
        val reducer = PlaceSearchReducer(
            LocatedCitySearchSource(source, session.location),
            this,
            StandardTestDispatcher(testScheduler),
        )
        reducer.setQuery("博物馆")
        advanceUntilIdle()

        assertEquals(listOf("杭州市"), requestedCities)
        assertEquals("杭州博物馆", reducer.state.value.results.single().name)
    }

    @Test fun returningOrRecreatingWorkspaceRetainsLocatedCameraButOtherTripsDoNot() {
        val handle = SavedStateHandle()
        val session = WorkspaceMapSession(handle)
        val camera = MapCameraState(hangzhou, 16f, 30f, 80f, 7L)
        session.onLocated(hangzhou)
        session.onCameraChanged(camera)

        val restored = WorkspaceMapSession(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(camera, restored.camera)
        assertEquals(hangzhou, restored.location)
        assertNull(WorkspaceMapSession(SavedStateHandle()).camera)
        assertNull(WorkspaceMapSession(SavedStateHandle()).location)
    }

    @Test fun failedCityResolutionDoesNotSilentlySearchNationwide() = runTest {
        var searches = 0
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                searches++
                return emptyList()
            }
        }
        val result = runCatching { LocatedCitySearchSource(source, hangzhou).search("博物馆", null) }
        assertTrue(result.isFailure)
        assertEquals(0, searches)
    }

    @Test fun unresolvedCityCanRetryAndResolvedCityIsCachedOnlyForThisLocation() = runTest {
        var lookups = 0
        val cities = mutableListOf<String?>()
        val source = object : PlaceSearchDataSource {
            override suspend fun cityAt(point: GeoPoint): PlaceCity? {
                lookups++
                return if (lookups == 1) null else PlaceCity("杭州市", "330100")
            }
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                cities += city
                return emptyList()
            }
        }
        val scoped = LocatedCitySearchSource(source, hangzhou)
        assertTrue(runCatching { scoped.search("博物馆", null) }.isFailure)
        scoped.search("博物馆", null)
        scoped.search("公园", null)
        assertEquals(2, lookups)
        assertEquals(listOf("杭州市", "杭州市"), cities)
    }

    @Test fun missingLocationDoesNotSilentlySearchNationwide() = runTest {
        val cities = mutableListOf<String?>()
        val source = object : PlaceSearchDataSource {
            override suspend fun cityAt(point: GeoPoint): PlaceCity = error("No location to resolve")
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                cities += city
                return emptyList()
            }
        }
        val result = runCatching { LocatedCitySearchSource(source, null).search("西施猪蹄", null) }
        assertTrue(result.isFailure)
        assertTrue(cities.isEmpty())
    }

    @Test fun restoredCameraDoesNotReplayOldFitButNewExplicitFitStillWorks() {
        val camera = MapCameraState(hangzhou, 16f, 0f, 0f, 7L)
        val oldFit = MapViewportRequest(7L, ViewportReason.INITIAL, listOf(GeoPoint(39.9, 116.4)))
        assertNull(viewportRendering(camera.consumedViewportId, oldFit).command)
        assertTrue(viewportRendering(camera.consumedViewportId, oldFit.copy(id = 8L)).command != null)
    }
}
