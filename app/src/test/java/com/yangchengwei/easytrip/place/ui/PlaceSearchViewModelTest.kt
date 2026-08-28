package com.yangchengwei.easytrip.place.ui

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    @Test fun editingSavedPlaceCreatesDraftBoundToPlaceId() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "备注", emptyList())
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(listOf(saved)),
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))

        assertEquals("saved-1", model.state.value.detailDraft?.placeId)
    }

    @Test fun addingValidTagSelectsItAndClearsInput() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val model = PlaceSearchViewModel("trip", FakeSavedPlaces(listOf(saved)), ImmediateSearchSource(listOf(candidate("poi-1"))), SavedStateHandle())
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))

        model.dispatch(PlaceSearchAction.UpdateNewTagInput("  景点  "))
        model.dispatch(PlaceSearchAction.AddNewTag)

        assertEquals(setOf("景点"), model.state.value.detailDraft?.selectedTagNames)
        assertEquals("", model.state.value.detailDraft?.newTagInput)
    }

    @Test fun fullTagSelectionStillAllowsRemovingTag() = runTest(dispatcher) {
        val tags = (1..8).map { PlaceTag("id-$it", "tag-$it") }
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", tags)
        val model = PlaceSearchViewModel("trip", FakeSavedPlaces(listOf(saved)), ImmediateSearchSource(listOf(candidate("poi-1"))), SavedStateHandle())
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))

        model.dispatch(PlaceSearchAction.RemoveEditTag("tag-8"))

        assertEquals(7, model.state.value.detailDraft?.selectedTagNames?.size)
    }

    @Test fun failedSavePreservesNoteTagsAndNewInput() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = FakeSavedPlaces(listOf(saved), updateFailure = IllegalStateException("保存失败"))
        val model = PlaceSearchViewModel("trip", repository, ImmediateSearchSource(listOf(candidate("poi-1"))), SavedStateHandle())
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))
        model.dispatch(PlaceSearchAction.UpdateEditNote("备注"))
        model.dispatch(PlaceSearchAction.UpdateEditTags(setOf("已有")))
        model.dispatch(PlaceSearchAction.UpdateNewTagInput("未添加"))

        model.dispatch(PlaceSearchAction.SaveEdit)
        advanceUntilIdle()

        assertEquals("备注", model.state.value.detailDraft?.note)
        assertEquals(setOf("已有"), model.state.value.detailDraft?.selectedTagNames)
        assertEquals("未添加", model.state.value.detailDraft?.newTagInput)
        assertEquals("保存失败", model.state.value.detailDraft?.errorMessage)
    }

    @Test fun staleSaveCompletionCannotCloseNewPlaceDraft() = runTest(dispatcher) {
        val first = SavedPlace("saved-1", "trip", "poi-1", "一", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val second = SavedPlace("saved-2", "trip", "poi-2", "二", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = DelayedDetailSavedPlaces(listOf(first, second))
        val model = PlaceSearchViewModel("trip", repository, ImmediateSearchSource(listOf(candidate("poi-1"), candidate("poi-2"))), SavedStateHandle())
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))
        model.dispatch(PlaceSearchAction.SaveEdit)
        dispatcher.scheduler.runCurrent()
        model.dispatch(PlaceSearchAction.StartEdit("saved-2"))

        repository.complete("saved-1")
        advanceUntilIdle()

        assertEquals("saved-2", model.state.value.detailDraft?.placeId)
    }

    @Test fun cancellationIsNotReportedAsSaveFailure() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = FakeSavedPlaces(listOf(saved), updateFailure = kotlinx.coroutines.CancellationException("cancelled"))
        val model = PlaceSearchViewModel("trip", repository, ImmediateSearchSource(listOf(candidate("poi-1"))), SavedStateHandle())
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))

        model.dispatch(PlaceSearchAction.SaveEdit)
        advanceUntilIdle()

        assertNull(model.state.value.detailDraft?.errorMessage)
        assertTrue(model.state.value.detailDraft?.isSaving == true)
    }

    @Test fun unsavedCandidateCannotStartEdit() = runTest(dispatcher) {
        val model = modelWithResult()

        model.dispatch(PlaceSearchAction.StartEdit("poi-1"))

        assertNull(model.state.value.detailDraft)
    }

    @Test fun cancelEditDiscardsDraftWithoutRepositoryCall() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "备注", emptyList())
        val repository = FakeSavedPlaces(listOf(saved))
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))

        model.dispatch(PlaceSearchAction.CancelEdit)

        assertNull(model.state.value.detailDraft)
        assertEquals(0, repository.updateCalls)
    }

    @Test fun backDismissesRemovalBeforeClosingDetail() {
        val pending = PendingCollectionRemoval(
            candidate("poi-1"),
            SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList()),
            PlaceDeletionImpact(1, 1),
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

    @Test fun backFromDetailReturnsToResultsWithoutExitEffect() = runTest(dispatcher) {
        val model = modelWithResult()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))

        model.dispatch(PlaceSearchAction.Back)

        assertEquals(SearchDisplayMode.Results, model.state.value.displayMode)
        assertNull(withTimeoutOrNull(1) { model.effects.first() })
    }

    @Test fun editDetailResultsWorkspaceBackChainEmitsExitOnlyFromResults() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val model = PlaceSearchViewModel(
            "trip",
            FakeSavedPlaces(listOf(saved)),
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.OpenDetail("poi-1"))
        model.dispatch(PlaceSearchAction.StartEdit("saved-1"))
        val exit = async { model.effects.first() }

        model.dispatch(PlaceSearchAction.Back)
        assertNull(model.state.value.detailDraft)
        assertEquals(SearchDisplayMode.MapDetail("poi-1"), model.state.value.displayMode)
        assertFalse(exit.isCompleted)

        model.dispatch(PlaceSearchAction.Back)
        assertEquals(SearchDisplayMode.Results, model.state.value.displayMode)
        assertFalse(exit.isCompleted)

        model.dispatch(PlaceSearchAction.Back)

        assertEquals(PlaceSearchEffect.ExitDestination, exit.await())
    }

    @Test fun repeatedResultsBackEmitsSingleExitEffect() = runTest(dispatcher) {
        val model = modelWithResult()
        val exit = async { model.effects.first() }

        model.dispatch(PlaceSearchAction.Back)
        model.dispatch(PlaceSearchAction.Back)
        advanceUntilIdle()

        assertEquals(PlaceSearchEffect.ExitDestination, exit.await())
        assertNull(withTimeoutOrNull(1) { model.effects.first() })
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
        assertNull(withTimeoutOrNull(1) { model.effects.first() })
        repository.saveGate.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun backDuringAsyncDeletionDoesNotChangeDetailOrRequestExit() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = BlockingSavedPlaces(
            listOf(saved),
            deletionImpact = PlaceDeletionImpact(0, 0),
        )
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
        assertNull(withTimeoutOrNull(1) { model.effects.first() })
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
            PlaceDeletionImpact(1, 1),
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

        assertEquals(PlaceSearchBackDecision.ExitDestination, decidePlaceSearchBack(state))
    }

    @Test fun differentPoiCollectionMutationsCompleteIndependentlyWhileImpactIgnoresCancellation() = runTest(dispatcher) {
        val first = SavedPlace("saved-1", "trip", "poi-1", "地点一", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = OverlappingCollectionSavedPlaces(first)
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(candidate("poi-1"), candidate("poi-2"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        dispatcher.scheduler.runCurrent()
        model.dispatch(PlaceSearchAction.ToggleCollection("poi-2"))
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("poi-2"), repository.saved.map(PlaceCandidate::poiId))
        assertEquals(setOf("poi-1"), model.state.value.collectionBusyPoiIds)
        assertEquals(setOf("poi-2"), model.recentlyCollectedPoiIds())

        repository.completeImpact(PlaceDeletionImpact(1, 2))
        advanceUntilIdle()

        assertEquals("poi-1", model.state.value.pendingCollectionRemoval?.candidate?.poiId)
        assertEquals(PlaceDeletionImpact(1, 2), model.state.value.pendingCollectionRemoval?.impact)
        assertEquals(emptySet<String>(), model.state.value.collectionBusyPoiIds)
        assertNull(model.state.value.collectionError)
    }

    @Test fun nonZeroRemovalImpactShowsExactCounts() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = FakeSavedPlaces(
            listOf(saved),
            deletionImpact = PlaceDeletionImpact(2, 3),
        )
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()

        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        advanceUntilIdle()

        assertEquals(PlaceDeletionImpact(2, 3), model.state.value.pendingCollectionRemoval?.impact)
        assertEquals(emptyList<String>(), repository.deleted)
    }

    @Test fun repeatedConfirmRemovalDeletesOnlyOnce() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = FakeSavedPlaces(
            listOf(saved),
            usageCount = 1,
            deletionImpact = PlaceDeletionImpact(1, 0),
        )
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            ImmediateSearchSource(listOf(candidate("poi-1"))),
            SavedStateHandle(mapOf("query" to "地点")),
        )
        advanceUntilIdle()
        model.dispatch(PlaceSearchAction.ToggleCollection("poi-1"))
        advanceUntilIdle()

        assertEquals(PlaceDeletionImpact(1, 0), model.state.value.pendingCollectionRemoval?.impact)
        model.dispatch(PlaceSearchAction.ConfirmRemoval)
        model.dispatch(PlaceSearchAction.ConfirmRemoval)
        advanceUntilIdle()

        assertEquals(listOf("saved-1"), repository.deleted)
        assertEquals(null, model.state.value.collectionError)
        assertEquals(null, model.state.value.pendingCollectionRemoval)
    }

    @Test fun retryingFailedRemovalImmediatelyClearsItsErrorWhileBusy() = runTest(dispatcher) {
        val saved = SavedPlace("saved-1", "trip", "poi-1", "地点", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val repository = FailingThenBlockingRemovalSavedPlaces(
            saved,
            PlaceDeletionImpact(1, 0),
        )
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
        advanceUntilIdle()

        assertEquals("首次取消失败", model.state.value.collectionError)
        assertEquals("poi-1", model.state.value.collectionErrorPoiId)

        model.dispatch(PlaceSearchAction.ConfirmRemoval)
        dispatcher.scheduler.runCurrent()

        assertNull(model.state.value.collectionError)
        assertNull(model.state.value.collectionErrorPoiId)
        assertTrue("poi-1" in model.state.value.collectionBusyPoiIds)

        repository.deleteGate.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun reauthorizingRetriesCurrentQueryWithoutRecreatingViewModel() = runTest(dispatcher) {
        val repository = FakeSavedPlaces()
        val first = IgnoringCancellationSearchSource()
        val second = RecordingSearchSource(mapOf("西湖" to listOf(candidate("new"))))
        val model = PlaceSearchViewModel("trip", repository, null, SavedStateHandle())

        model.setRemoteSearchSession(1L, first)
        model.dispatch(PlaceSearchAction.QueryChanged("西湖"))
        advanceUntilIdle()
        model.setRemoteSearchSession(2L, null)
        first.complete("西湖", candidate("old"))
        advanceUntilIdle()
        model.setRemoteSearchSession(3L, second)
        advanceUntilIdle()

        assertEquals("西湖", model.state.value.search.query)
        assertEquals(listOf("西湖"), second.keywords)
        assertEquals(listOf("new"), model.state.value.search.results.map { it.poiId })
    }

    @Test fun newAcceptedGenerationCreatesFreshSearchSession() = runTest(dispatcher) {
        val first = RecordingSearchSource(mapOf("西湖" to listOf(candidate("first"))))
        val second = RecordingSearchSource(mapOf("西湖" to listOf(candidate("second"))))
        val model = PlaceSearchViewModel("trip", FakeSavedPlaces(), null, SavedStateHandle())

        model.setRemoteSearchSession(1L, first)
        model.dispatch(PlaceSearchAction.QueryChanged("西湖"))
        advanceUntilIdle()
        model.setRemoteSearchSession(2L, second)
        advanceUntilIdle()

        assertEquals(listOf("西湖"), first.keywords)
        assertEquals(listOf("西湖"), second.keywords)
        assertEquals(listOf("second"), model.state.value.search.results.map { it.poiId })
    }

    @Test fun replacingSearchSourceKeepsViewModelAndUsesLatestRuntime() = runTest(dispatcher) {
        val repository = FakeSavedPlaces()
        val first = ImmediateSearchSource(listOf(candidate("first")))
        val second = ImmediateSearchSource(listOf(candidate("second")))
        val model = PlaceSearchViewModel("trip", repository, first, SavedStateHandle())

        model.dispatch(PlaceSearchAction.QueryChanged("北京"))
        advanceUntilIdle()
        assertEquals(listOf("first"), model.state.value.search.results.map { it.poiId })

        model.setSearchSource(null)
        advanceUntilIdle()
        assertEquals(PlaceSearchPhase.ConsentRequired, model.state.value.search.phase)

        model.setSearchSource(second)
        advanceUntilIdle()
        assertEquals(listOf("second"), model.state.value.search.results.map { it.poiId })
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
        assertNull(withTimeoutOrNull(1) { model.effects.first() })
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
            PlaceSearchAction.StartEdit("saved"),
            PlaceSearchAction.UpdateEditNote("note"),
            PlaceSearchAction.UpdateEditTags(emptySet()),
            PlaceSearchAction.SaveEdit,
            PlaceSearchAction.CancelEdit,
            PlaceSearchAction.DismissRemovalConfirmation,
            PlaceSearchAction.ConfirmRemoval,
        )

        assertFalse(actions.any { it.javaClass.simpleName.contains("Itinerary") || it.javaClass.simpleName.contains("Schedule") })
        assertEquals(14, actions.size)
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

    private class DelayedImpactSavedPlaces(
        initialPlaces: List<SavedPlace>,
    ) : SavedPlaceRepository {
        private val places = MutableStateFlow(initialPlaces)
        private val savedIds = MutableStateFlow(initialPlaces.mapTo(mutableSetOf(), SavedPlace::amapPoiId))
        private val impacts = mutableMapOf<String, CompletableDeferred<PlaceDeletionImpact>>()
        fun completeImpact(placeId: String, impact: PlaceDeletionImpact) {
            impacts.getOrPut(placeId) { CompletableDeferred() }.complete(impact)
        }
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = savedIds
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.AlreadySaved(candidate.poiId)
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = withContext(NonCancellable) {
            impacts.getOrPut(placeId) { CompletableDeferred() }.await()
        }
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class OverlappingCollectionSavedPlaces(saved: SavedPlace) : SavedPlaceRepository {
        private val impact = CompletableDeferred<PlaceDeletionImpact>()
        private val places = MutableStateFlow(listOf(saved))
        private val savedIds = MutableStateFlow(setOf(saved.amapPoiId))
        val saved = mutableListOf<PlaceCandidate>()

        fun completeImpact(value: PlaceDeletionImpact) = impact.complete(value)
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = savedIds
        override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult {
            saved += candidate
            savedIds.value += candidate.poiId
            return SavePlaceResult.Saved(candidate.poiId)
        }
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 1
        override suspend fun deletionImpact(placeId: String) = withContext(NonCancellable) { impact.await() }
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class BlockingSavedPlaces(
        initialPlaces: List<SavedPlace> = emptyList(),
        private val deletionImpact: PlaceDeletionImpact? = null,
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
        override suspend fun deletionImpact(placeId: String) =
            requireNotNull(deletionImpact) { "Test must configure deletion impact for $placeId" }
        override suspend fun deletePlaceAndReferences(placeId: String) {
            deleteGate.await()
        }
    }

    private class FailingThenBlockingRemovalSavedPlaces(
        saved: SavedPlace,
        private val deletionImpact: PlaceDeletionImpact,
    ) : SavedPlaceRepository {
        val deleteGate = CompletableDeferred<Unit>()
        private val places = MutableStateFlow(listOf(saved))
        private val savedIds = MutableStateFlow(setOf(saved.amapPoiId))
        private var deleteAttempts = 0

        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = savedIds
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.AlreadySaved(places.value.single().id)
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 1
        override suspend fun deletionImpact(placeId: String) = deletionImpact
        override suspend fun deletePlaceAndReferences(placeId: String) {
            deleteAttempts += 1
            if (deleteAttempts == 1) error("首次取消失败")
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
        override suspend fun deletionImpact(placeId: String): PlaceDeletionImpact =
            error("Test must configure deletion impact for $placeId")
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class DelayedDetailSavedPlaces(private val initialPlaces: List<SavedPlace>) : SavedPlaceRepository {
        private val places = MutableStateFlow(initialPlaces)
        private val completions = mutableMapOf<String, CompletableDeferred<Unit>>()
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = MutableStateFlow(initialPlaces.mapTo(mutableSetOf(), SavedPlace::amapPoiId))
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved(candidate.poiId)
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) {
            withContext(NonCancellable) { completions.getOrPut(placeId) { CompletableDeferred() }.await() }
        }
        fun complete(placeId: String) = completions.getValue(placeId).complete(Unit)
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String): PlaceDeletionImpact =
            error("Test must configure deletion impact for $placeId")
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class FakeSavedPlaces(
        initialPlaces: List<SavedPlace> = emptyList(),
        private val usageCount: Int = 0,
        private val updateFailure: Throwable? = null,
        private val deletionImpact: PlaceDeletionImpact? = null,
    ) : SavedPlaceRepository {
        val saved = mutableListOf<PlaceCandidate>()
        val deleted = mutableListOf<String>()
        var updateCalls = 0
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
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) {
            updateCalls += 1
            updateFailure?.let { throw it }
        }
        override suspend fun usageCount(placeId: String) = usageCount
        override suspend fun deletionImpact(placeId: String) =
            requireNotNull(deletionImpact) { "Test must configure deletion impact for $placeId" }
        override suspend fun deletePlaceAndReferences(placeId: String) {
            deleted += placeId
            if (deleted.count { it == placeId } > 1) error("Unknown place")
            places.value = places.value.filterNot { it.id == placeId }
            savedIds.value = places.value.mapTo(mutableSetOf(), SavedPlace::amapPoiId)
        }
    }
}
