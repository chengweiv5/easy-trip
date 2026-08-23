package com.yangchengwei.easytrip.place.ui

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test fun queryRestoresFromSavedState() {
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            ImmediateSearchSource(emptyList()),
            SavedStateHandle(mapOf("query" to "故宫")),
        )

        assertEquals("故宫", model.state.value.search.query)
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

    private class FakeSavedPlaces : SavedPlaceRepository {
        val saved = mutableListOf<PlaceCandidate>()
        private val places = MutableStateFlow<List<SavedPlace>>(emptyList())
        private val savedIds = MutableStateFlow<Set<String>>(emptySet())

        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = savedIds
        override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult {
            saved += candidate
            savedIds.value += candidate.poiId
            return SavePlaceResult.Saved(candidate.poiId)
        }
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }
}
