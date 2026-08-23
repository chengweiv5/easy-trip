package com.yangchengwei.easytrip.place.ui

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun restoredQueryIsTrimmedAndAutomaticallySearched() = runTest(dispatcher) {
        val source = RecordingSearchSource(mapOf("故宫" to listOf(candidate("restored"))))
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            source,
            SavedStateHandle(mapOf("query" to "  故宫  ")),
        )

        advanceUntilIdle()

        assertEquals(listOf("故宫"), source.keywords)
        assertEquals(listOf("restored"), model.state.value.search.results.map { it.poiId })
        assertEquals(PlaceSearchPhase.Results, model.state.value.search.phase)
    }

    @Test fun restoredResponseCannotOverwriteNewQueryWhenCancellationIsIgnored() = runTest(dispatcher) {
        val source = IgnoringCancellationSearchSource()
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            source,
            SavedStateHandle(mapOf("query" to "恢复词")),
        )
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.QueryChanged("新词"))
        advanceUntilIdle()
        source.complete("新词", candidate("new"))
        advanceUntilIdle()
        source.complete("恢复词", candidate("stale"))
        advanceUntilIdle()

        assertEquals("新词", model.state.value.search.query)
        assertEquals(listOf("new"), model.state.value.search.results.map { it.poiId })
    }

    @Test fun repeatedConfirmRemovalDeletesOnlyOnce() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = FakeSavedPlaces(listOf(saved), usageCount = 1)
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.ConfirmRemoval)
        model.dispatch(PlaceSearchAction.ConfirmRemoval)
        advanceUntilIdle()

        assertEquals(listOf("saved-1"), repository.deleted)
        assertEquals(null, model.state.value.collectionError)
        assertEquals(null, model.state.value.pendingCollectionRemoval)
    }

    @Test fun collectingKeepsUserOnSearchScreen() = runTest(dispatcher) {
        val repository = FakeSavedPlaces()
        val handle = SavedStateHandle(mapOf("query" to "故宫"))
        val model = PlaceSearchViewModel("trip", repository, ImmediateSearchSource(listOf(candidate("poi-1"))), handle)
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        advanceUntilIdle()

        assertEquals("故宫", model.state.value.search.query)
        assertEquals(1, repository.saved.size)
        assertFalse(model.state.value.shouldNavigateBack)
    }

    @Test fun multipleResultsCanBeCollectedSequentially() = runTest(dispatcher) {
        val repository = FakeSavedPlaces()
        val results = listOf(candidate("poi-1"), candidate("poi-2"))
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(results),
            SavedStateHandle(mapOf("query" to "北京")),
        )
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.ToggleCollection("poi-2"))
        advanceUntilIdle()

        assertEquals(listOf("poi-1", "poi-2"), repository.saved.map { it.poiId })
    }

    @Test fun searchActionsNeverCreateItineraryItems() {
        val actions = listOf(
            PlaceSearchAction.Back,
            PlaceSearchAction.QueryChanged("query"),
            PlaceSearchAction.Submit,
            PlaceSearchAction.Retry,
            PlaceSearchAction.ToggleCollection("poi"),
            PlaceSearchAction.DismissRemovalConfirmation,
            PlaceSearchAction.ConfirmRemoval,
        )

        assertFalse(actions.any { it.javaClass.simpleName.contains("Itinerary") || it.javaClass.simpleName.contains("Schedule") })
        assertEquals(7, actions.size)
    }

    private fun candidate(id: String) = PlaceCandidate(id, id, "address", GeoPoint(39.9, 116.4), null)

    private class ImmediateSearchSource(private val results: List<PlaceCandidate>) : PlaceSearchDataSource {
        override suspend fun search(keyword: String, city: String?) = results
    }

    private class RecordingSearchSource(private val results: Map<String, List<PlaceCandidate>>) : PlaceSearchDataSource {
        val keywords = mutableListOf<String>()
        override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
            keywords += keyword
            return results.getValue(keyword)
        }
    }

    private class IgnoringCancellationSearchSource : PlaceSearchDataSource {
        private val pending = mutableMapOf<String, CompletableDeferred<List<PlaceCandidate>>>()
        override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> =
            withContext(NonCancellable) { pending.getOrPut(keyword) { CompletableDeferred() }.await() }
        fun complete(keyword: String, result: PlaceCandidate) {
            pending.getValue(keyword).complete(listOf(result))
        }
    }

    private class FakeSavedPlaces(
        initialPlaces: List<SavedPlace> = emptyList(),
        private val usageCount: Int = 0,
    ) : SavedPlaceRepository {
        val saved = mutableListOf<PlaceCandidate>()
        val deleted = mutableListOf<String>()
        private val places = MutableStateFlow(initialPlaces)
        private val savedIds = MutableStateFlow(initialPlaces.mapTo(mutableSetOf(), SavedPlace::amapPoiId))

        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = savedIds
        override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult {
            saved += candidate
            savedIds.value += candidate.poiId
            return SavePlaceResult.Saved(candidate.poiId)
        }
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = usageCount
        override suspend fun deletePlaceAndReferences(placeId: String) {
            deleted += placeId
            if (deleted.count { it == placeId } > 1) error("Unknown place")
            places.value = places.value.filterNot { it.id == placeId }
            savedIds.value = places.value.mapTo(mutableSetOf(), SavedPlace::amapPoiId)
        }
    }
}
