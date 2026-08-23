package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacePoolViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun dismissWhileDeleteImpactLoadsPreventsStaleResult() = runTest(dispatcher) {
        val repository = DelayedRepository()
        val model = PlacePoolViewModel("trip", repository, null)

        model.requestDelete(place("a"))
        model.dismissDelete()
        repository.completeUsage("a", 2)
        advanceUntilIdle()

        assertNull(model.state.value.deleting)
        assertEquals(0, model.state.value.deletionUsageCount)
    }

    @Test fun latestDeleteRequestWinsWhenImpactQueriesCompleteOutOfOrder() = runTest(dispatcher) {
        val repository = DelayedRepository()
        val model = PlacePoolViewModel("trip", repository, null)

        model.requestDelete(place("a"))
        model.requestDelete(place("b"))
        repository.completeUsage("b", 3)
        advanceUntilIdle()
        repository.completeUsage("a", 1)
        advanceUntilIdle()

        assertEquals("b", model.state.value.deleting?.id)
        assertEquals(3, model.state.value.deletionUsageCount)
    }

    private fun place(id: String) = SavedPlace(
        id,
        "trip",
        "poi-$id",
        id.uppercase(),
        "address",
        GeoPoint(39.9, 116.4),
        "",
        emptyList(),
    )

    private class DelayedRepository : SavedPlaceRepository {
        private val usage = mutableMapOf<String, CompletableDeferred<Int>>()

        fun completeUsage(id: String, count: Int) {
            usage.getOrPut(id) { CompletableDeferred() }.complete(count)
        }

        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = emptyFlow()
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = emptyFlow()
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = emptyFlow()
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = usage.getOrPut(placeId) { CompletableDeferred() }.await()
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }
}
