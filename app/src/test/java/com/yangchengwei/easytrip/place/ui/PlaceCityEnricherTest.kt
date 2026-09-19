package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceCityEnricherTest {
    private val place = SavedPlace("a", "trip", "poi", "地点", "地址", GeoPoint(30.0, 120.0), "备注", emptyList())

    @Test fun emissionsDoNotRepeatLookupAndFailuresRemainRetryableOnNextVisit() = runTest {
        val repo = Repository()
        val source = Source { throw IllegalStateException("offline") }
        val enricher = PlaceCityEnricher(this, repo, source)
        repeat(3) { enricher.submit(listOf(place)); advanceUntilIdle() }
        assertEquals(1, source.calls)
        assertTrue(repo.writes.isEmpty())
        PlaceCityEnricher(this, repo, source).submit(listOf(place))
        advanceUntilIdle()
        assertEquals(2, source.calls)
    }

    @Test fun consentRevocationCancelsPendingLookupAndDoesNotWriteStaleMetadata() = runTest {
        val pending = CompletableDeferred<PlaceCity?>()
        val repo = Repository()
        val source = Source { pending.await() }
        val enricher = PlaceCityEnricher(this, repo, source)
        enricher.submit(listOf(place))
        testScheduler.runCurrent()
        enricher.setSource(null)
        pending.complete(PlaceCity("杭州市", "330100"))
        advanceUntilIdle()
        assertTrue(repo.writes.isEmpty())
    }

    @Test fun knownCitiesAreSkippedAndNewPlacesAddedDuringLookupAreProcessed() = runTest {
        val pending = CompletableDeferred<PlaceCity?>()
        val source = Source { pending.await() }
        val repo = Repository()
        val enricher = PlaceCityEnricher(this, repo, source)
        enricher.submit(listOf(place))
        testScheduler.runCurrent()
        enricher.submit(listOf(place, place.copy(id = "b", amapPoiId = "poi2"), place.copy(id = "known", cityName = "北京市")))
        pending.complete(PlaceCity("杭州市", "330100"))
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), repo.writes)
        assertEquals(2, source.calls)
    }

    private class Source(val lookup: suspend () -> PlaceCity?) : PlaceSearchDataSource {
        var calls = 0
        override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        override suspend fun cityForPoi(poiId: String): PlaceCity? { calls++; return lookup() }
    }
    private class Repository : SavedPlaceRepository {
        val writes = mutableListOf<String>()
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(emptyList<SavedPlace>())
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(emptySet<String>())
        override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult = error("unused")
        override suspend fun updateCityIfMissing(placeId: String, city: PlaceCity) { writes += placeId }
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = PlaceDeletionImpact(0, 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }
}
