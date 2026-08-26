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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test fun openDetailKeepsQueryAndResultsAndStoresPoiId() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("query" to "故宫"))
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            ImmediateSearchSource(listOf(candidate("poi-1"), candidate("poi-2"))),
            handle,
        )
        advanceUntilIdle()
        val originalSearch = model.state.value.search

        model.dispatch(PlaceSearchAction.OpenDetail("poi-2"))

        assertEquals(originalSearch, model.state.value.search)
        assertEquals(SearchDisplayMode.MapDetail("poi-2"), model.state.value.displayMode)
        assertEquals("MAP_DETAIL", handle.get<String>("displayMode"))
        assertEquals("poi-2", handle.get<String>("selectedPoiId"))
        assertNull(handle.get<PlaceCandidate>("selectedCandidate"))
    }

    @Test fun ordinaryDetailStateChangesKeepViewportRequestId() = runTest(dispatcher) {
        val model = modelWithResult()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))
        val initialRequestId = model.state.value.detailMapRequestId

        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        advanceUntilIdle()

        assertEquals(initialRequestId, model.state.value.detailMapRequestId)
    }

    @Test fun recenterIncrementsRequestId() = runTest(dispatcher) {
        val model = modelWithResult()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))
        val initialRequestId = model.state.value.detailMapRequestId

        model.dispatch(PlaceSearchAction.RecenterDetail)

        assertEquals(initialRequestId + 1L, model.state.value.detailMapRequestId)
    }

    @Test fun openDetailRejectsPoiOutsideCurrentResults() = runTest(dispatcher) {
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "故宫")),
        )
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.OpenDetail("missing"))

        assertEquals(SearchDisplayMode.Results, model.state.value.displayMode)
    }

    @Test fun restoredDetailWaitsForSearchTerminalStateBeforeValidation() = runTest(dispatcher) {
        val source = ControlledSearchSource()
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            source,
            SavedStateHandle(
                mapOf(
                    "query" to "故宫",
                    "displayMode" to "MAP_DETAIL",
                    "selectedPoiId" to "poi-1",
                ),
            ),
        )
        dispatcher.scheduler.advanceTimeBy(301)
        dispatcher.scheduler.runCurrent()

        assertEquals(PlaceSearchPhase.Loading, model.state.value.search.phase)
        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)

        source.complete(listOf(candidate("poi-1")))
        advanceUntilIdle()

        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)
    }

    @Test fun restoredMissingPoiFallsBackToResults() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                "query" to "故宫",
                "displayMode" to "MAP_DETAIL",
                "selectedPoiId" to "missing",
            ),
        )
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            handle,
        )

        advanceUntilIdle()

        assertEquals(SearchDisplayMode.Results, model.state.value.displayMode)
        assertEquals("RESULTS", handle.get<String>("displayMode"))
        assertNull(handle.get<String>("selectedPoiId"))
    }

    @Test fun backDismissesRemovalBeforeClosingDetail() {
        val pending = PendingCollectionRemoval(
            candidate("poi-1"),
            SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList()),
            1,
        )
        val state = PlaceSearchUiState(
            displayMode = SearchDisplayMode.MapDetail("poi-1"),
            pendingCollectionRemoval = pending,
        )

        assertEquals(PlaceSearchBackDecision.DismissRemovalConfirmation, decidePlaceSearchBack(state))
    }

    @Test fun backCancelsEditBeforeClosingDetail() {
        val state = PlaceSearchUiState(
            displayMode = SearchDisplayMode.MapDetail("poi-1"),
            detailDraft = PlaceDetailEditState("saved-1", "note", emptySet()),
        )

        assertEquals(PlaceSearchBackDecision.CancelEdit, decidePlaceSearchBack(state))
    }

    @Test fun backFromDetailReturnsToResults() = runTest(dispatcher) {
        val model = modelWithResult()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))

        model.dispatch(PlaceSearchAction.Back)

        assertEquals(SearchDisplayMode.Results, model.state.value.displayMode)
        assertFalse(model.state.value.shouldNavigateBack)
    }

    @Test fun backFromResultsRequestsDestinationExit() = runTest(dispatcher) {
        val model = modelWithResult()

        model.dispatch(PlaceSearchAction.Back)

        assertTrue(model.state.value.shouldNavigateBack)
    }

    @Test fun backIsIgnoredWhileDetailMutationIsSubmitting() {
        val state = PlaceSearchUiState(
            displayMode = SearchDisplayMode.MapDetail("poi-1"),
            collectionBusyPoiIds = setOf("poi-2"),
        )

        assertEquals(PlaceSearchBackDecision.Ignore, decidePlaceSearchBack(state))
    }

    @Test fun backDuringAsyncCollectionDoesNotChangeDetailOrRequestExit() = runTest(dispatcher) {
        val repository = BlockingSavedPlaces()
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))
        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        dispatcher.scheduler.runCurrent()

        model.dispatch(PlaceSearchAction.Back)

        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)
        assertFalse(model.state.value.shouldNavigateBack)
        repository.saveGate.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun backDuringAsyncDeletionDoesNotChangeDetailOrRequestExit() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = BlockingSavedPlaces(listOf(saved))
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))
        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        dispatcher.scheduler.runCurrent()

        model.dispatch(PlaceSearchAction.Back)

        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)
        assertFalse(model.state.value.shouldNavigateBack)
        repository.deleteGate.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun savingDetailDraftIgnoresBack() = runTest(dispatcher) {
        val model = modelWithResult()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))
        val state = model.state.value.copy(
            detailDraft = PlaceDetailEditState("saved-1", "note", emptySet(), isSaving = true),
        )

        val afterBack = reducePlaceSearchBack(state)

        assertEquals(state, afterBack)
    }

    @Test fun restoredDetailUsesOnlyCurrentQueryTerminalResultWhenOldResponseArrivesLate() = runTest(dispatcher) {
        val source = IgnoringCancellationSearchSource()
        val handle = SavedStateHandle(
            mapOf(
                "query" to "恢复词",
                "displayMode" to "MAP_DETAIL",
                "selectedPoiId" to "poi-1",
            ),
        )
        val model = PlaceSearchViewModel("trip", FakeSavedPlaces(), source, handle)
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.QueryChanged("新词"))
        advanceUntilIdle()

        source.complete("恢复词", candidate("stale"))
        advanceUntilIdle()

        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)
        assertEquals("MAP_DETAIL", handle.get<String>("displayMode"))
        assertEquals("poi-1", handle.get<String>("selectedPoiId"))

        source.complete("新词", candidate("poi-1"))
        advanceUntilIdle()

        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)
        assertEquals("MAP_DETAIL", handle.get<String>("displayMode"))
        assertEquals("poi-1", handle.get<String>("selectedPoiId"))
    }

    @Test fun restoredDetailFallsBackOnlyWhenCurrentQueryTerminalResultMissesPoi() = runTest(dispatcher) {
        val source = IgnoringCancellationSearchSource()
        val handle = SavedStateHandle(
            mapOf(
                "query" to "恢复词",
                "displayMode" to "MAP_DETAIL",
                "selectedPoiId" to "poi-1",
            ),
        )
        val model = PlaceSearchViewModel("trip", FakeSavedPlaces(), source, handle)
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.QueryChanged("新词"))
        advanceUntilIdle()
        source.complete("恢复词", candidate("poi-1"))
        advanceUntilIdle()

        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)

        source.complete("新词", candidate("other"))
        advanceUntilIdle()

        assertEquals(SearchDisplayMode.Results, model.state.value.displayMode)
        assertEquals("RESULTS", handle.get<String>("displayMode"))
        assertNull(handle.get<String>("selectedPoiId"))
    }

    @Test fun consecutiveBackFollowsConfirmationEditDetailExitPriority() = runTest(dispatcher) {
        val model = modelWithResult()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))
        val pending = PendingCollectionRemoval(
            candidate("poi-1"),
            SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList()),
            1,
        )
        var state = model.state.value.copy(
            pendingCollectionRemoval = pending,
            detailDraft = PlaceDetailEditState("saved-1", "note", emptySet()),
        )

        state = reducePlaceSearchBack(state)
        assertNull(state.pendingCollectionRemoval)
        assertTrue(state.detailDraft != null)
        assertEquals(SearchDisplayMode.MapDetail("poi-1"), state.displayMode)

        state = reducePlaceSearchBack(state)
        assertNull(state.detailDraft)
        assertEquals(SearchDisplayMode.MapDetail("poi-1"), state.displayMode)

        state = reducePlaceSearchBack(state)
        assertEquals(SearchDisplayMode.Results, state.displayMode)
        assertFalse(state.shouldNavigateBack)

        state = reducePlaceSearchBack(state)
        assertTrue(state.shouldNavigateBack)
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

    @Test fun collectingCandidateWithoutCoordinatesEntersRepositoryFlowAndReportsPersistenceError() = runTest(dispatcher) {
        val repository = CoordinateRequiringSavedPlaces()
        val noPoint = PlaceCandidate("poi-no-point", "未知地点", "地址", null, "010")
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(noPoint)),
            SavedStateHandle(mapOf("query" to "未知")),
        )
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.ToggleCollection(noPoint.poiId))
        advanceUntilIdle()

        assertEquals(listOf(noPoint), repository.attempted)
        assertEquals("无法收藏缺少坐标的地点", model.state.value.collectionError)
        assertEquals(noPoint.poiId, model.state.value.collectionErrorPoiId)
        assertFalse(noPoint.poiId in model.recentlyCollectedPoiIds())
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
        assertEquals(setOf("poi-1", "poi-2"), model.recentlyCollectedPoiIds())
    }

    @Test fun preexistingPlacesAreNotReportedAsRecentlyCollected() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-existing", "已有地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(listOf(saved)),
            ImmediateSearchSource(emptyList()),
            SavedStateHandle(),
        )

        advanceUntilIdle()

        assertEquals(emptySet<String>(), model.recentlyCollectedPoiIds())
    }

    @Test fun searchActionsNeverCreateItineraryItems() {
        val actions = listOf(
            PlaceSearchAction.Back,
            PlaceSearchAction.OpenDetail("poi"),
            PlaceSearchAction.QueryChanged("query"),
            PlaceSearchAction.Submit,
            PlaceSearchAction.Retry,
            PlaceSearchAction.RecenterDetail,
            PlaceSearchAction.ToggleCollection("poi"),
            PlaceSearchAction.DismissRemovalConfirmation,
            PlaceSearchAction.ConfirmRemoval,
        )

        assertFalse(actions.any { it.javaClass.simpleName.contains("Itinerary") || it.javaClass.simpleName.contains("Schedule") })
        assertEquals(9, actions.size)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.modelWithResult(): PlaceSearchViewModel {
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(),
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "故宫")),
        )
        advanceUntilIdle()
        return model
    }

    private fun candidate(id: String) = PlaceCandidate(id, id, "address", GeoPoint(39.9, 116.4), null)

    private class ImmediateSearchSource(private val results: List<PlaceCandidate>) : PlaceSearchDataSource {
        override suspend fun search(keyword: String, city: String?) = results
    }

    private class ControlledSearchSource : PlaceSearchDataSource {
        private val result = CompletableDeferred<List<PlaceCandidate>>()
        override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> = result.await()
        fun complete(value: List<PlaceCandidate>) = result.complete(value)
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

    private class BlockingSavedPlaces(
        initialPlaces: List<SavedPlace> = emptyList(),
    ) : SavedPlaceRepository {
        val saveGate = CompletableDeferred<Unit>()
        val deleteGate = CompletableDeferred<Unit>()
        private val places = MutableStateFlow(initialPlaces)
        private val savedIds = MutableStateFlow(initialPlaces.mapTo(mutableSetOf(), SavedPlace::amapPoiId))

        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = savedIds
        override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult {
            saveGate.await()
            return SavePlaceResult.Saved(candidate.poiId)
        }
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletePlaceAndReferences(placeId: String) {
            deleteGate.await()
        }
    }

    private class CoordinateRequiringSavedPlaces : SavedPlaceRepository {
        val attempted = mutableListOf<PlaceCandidate>()
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = MutableStateFlow(emptyList())
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = MutableStateFlow(emptySet())
        override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult {
            attempted += candidate
            requireNotNull(candidate.point) { "无法收藏缺少坐标的地点" }
            error("unreachable")
        }
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
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
