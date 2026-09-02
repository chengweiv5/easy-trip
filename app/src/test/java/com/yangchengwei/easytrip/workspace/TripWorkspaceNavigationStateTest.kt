package com.yangchengwei.easytrip.workspace

import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.itinerary.ui.CrossDayMoveDraft
import com.yangchengwei.easytrip.itinerary.ui.ItineraryDeleteConfirmation
import com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft
import com.yangchengwei.easytrip.itinerary.ui.RouteModeEditDraft
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints
import com.yangchengwei.easytrip.route.domain.RoutePlanOutcome
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.domain.InsertSide
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.domain.TripRepository
import com.yangchengwei.easytrip.trip.domain.TripSummary
import com.yangchengwei.easytrip.trip.domain.TripWithDays
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TripWorkspaceNavigationStateTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun mapPlaceDetailParticipatesInExclusiveOverlayAndBack() = runTest(dispatcher) {
        val model = model(Trips(days("one", "two")))
        advanceUntilIdle()

        model.selectMapPoi(MapPoiUi("poi", "故宫", "北京", GeoPoint(39.9, 116.4)))
        advanceUntilIdle()

        assertEquals(true, model.state.value.overlay is WorkspaceOverlay.PlaceDetail)
        assertEquals(true, model.handleBack())
        advanceUntilIdle()
        assertEquals(WorkspaceOverlay.None, model.state.value.overlay)
        assertNull(model.state.value.selectedMapPoi)
    }

    @Test fun restoredWorkspaceDropsTemporaryMapDetailPayload() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                "workspace.mapPoi.id" to "stale-poi",
                "workspace.mapPoi.name" to "旧地图地点",
                "workspace.mapPoi.address" to "旧地址",
                "workspace.mapPoi.latitude" to 39.9,
                "workspace.mapPoi.longitude" to 116.4,
            ),
        )

        val model = model(Trips(days("one")), handle)
        advanceUntilIdle()

        assertEquals(WorkspaceOverlay.None, model.state.value.overlay)
        assertNull(model.state.value.selectedMapPoi)
        assertNull(handle.get<String>("workspace.mapPoi.name"))
    }

    @Test fun openingOverlayReplacesCurrentOverlay() = runTest(dispatcher) {
        val model = model(Trips(days("one", "two")))
        advanceUntilIdle()

        model.openOverlay(WorkspaceOverlay.PlaceDetail(11L))
        model.openOverlay(WorkspaceOverlay.EditItineraryItem("item-22"))
        advanceUntilIdle()

        assertEquals(WorkspaceOverlay.EditItineraryItem("item-22"), model.state.value.overlay)
    }

    @Test fun expandingSheetClosesLayerMenuBeforeItBecomesInvisible() = runTest(dispatcher) {
        val model = model(Trips(days("one", "two")))
        advanceUntilIdle()

        model.openOverlay(WorkspaceOverlay.LayerMenu)
        model.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
        advanceUntilIdle()

        assertEquals(WorkspaceOverlay.None, model.state.value.overlay)
        assertEquals(WorkspaceSheetLevel.EXPANDED, model.state.value.sheetLevel)
    }

    @Test fun addFlowBackPolicyLocksSubmitAndUndoWithoutClosingOtherIdleOverlays() {
        assertEquals(WorkspaceBackDecision.Ignore, workspaceBackDecision(WorkspaceOverlay.SelectAddTargetDay, AddToItineraryUiState(isSubmitting = true)))
        assertEquals(WorkspaceBackDecision.Ignore, workspaceBackDecision(WorkspaceOverlay.AddToItineraryResult, AddToItineraryUiState(isUndoing = true)))
        assertEquals(WorkspaceBackDecision.CloseOverlay, workspaceBackDecision(WorkspaceOverlay.LayerMenu, AddToItineraryUiState(isSubmitting = true)))
        assertEquals(WorkspaceBackDecision.CloseOverlay, workspaceBackDecision(WorkspaceOverlay.Feedback(FeedbackUiModel("message")), AddToItineraryUiState(isUndoing = true)))
        assertEquals(WorkspaceBackDecision.CloseOverlay, workspaceBackDecision(WorkspaceOverlay.LayerMenu, AddToItineraryUiState()))
        assertEquals(WorkspaceBackDecision.LeaveWorkspace, workspaceBackDecision(WorkspaceOverlay.None, AddToItineraryUiState()))
    }

    @Test fun appendDayBackPolicyLocksOnlyItsBusyOverlay() {
        assertEquals(
            WorkspaceBackDecision.Ignore,
            workspaceBackDecision(WorkspaceOverlay.AddTripDay, AddToItineraryUiState(), isAppendingDay = true),
        )
        assertEquals(
            WorkspaceBackDecision.CloseOverlay,
            workspaceBackDecision(WorkspaceOverlay.AddTripDay, AddToItineraryUiState(), isAppendingDay = false),
        )
        assertEquals(
            WorkspaceBackDecision.CloseOverlay,
            workspaceBackDecision(WorkspaceOverlay.LayerMenu, AddToItineraryUiState(), isAppendingDay = true),
        )
    }

    private fun confirmationModel() = ConfirmationUiModel(
        title = "确认",
        message = "确认操作",
        deletedItems = emptyList(),
        retainedItems = emptyList(),
        confirmLabel = "确认",
        dismissLabel = "取消",
        destructive = true,
        reversible = false,
    )

    @Test fun itineraryMutationBackPolicyLocksOnlyItsMatchingBusyOverlay() {
        val saving = DayItineraryUiState(
            editDraft = ItineraryEditDraft("item", "09:00", "30", isSaving = true),
        )
        val deleting = DayItineraryUiState(
            deleteConfirmation = ItineraryDeleteConfirmation("item", "故宫", isDeleting = true),
        )

        assertEquals(
            WorkspaceBackDecision.Ignore,
            workspaceBackDecision(WorkspaceOverlay.EditItineraryItem("item"), AddToItineraryUiState(), itinerary = saving),
        )
        assertEquals(
            WorkspaceBackDecision.Ignore,
            workspaceBackDecision(
                WorkspaceOverlay.Confirmation(confirmationModel()),
                AddToItineraryUiState(),
                itinerary = deleting,
            ),
        )
        assertEquals(
            WorkspaceBackDecision.CloseOverlay,
            workspaceBackDecision(WorkspaceOverlay.LayerMenu, AddToItineraryUiState(), itinerary = saving),
        )
        assertEquals(
            WorkspaceBackDecision.CloseOverlay,
            workspaceBackDecision(
                WorkspaceOverlay.Confirmation(confirmationModel()),
                AddToItineraryUiState(),
                itinerary = deleting,
                hasPlaceDeleteConfirmation = true,
            ),
        )
    }

    @Test fun itineraryMutationClosePolicyMatchesBackPolicy() {
        val saving = DayItineraryUiState(
            editDraft = ItineraryEditDraft("item", "09:00", "30", isSaving = true),
        )
        val moving = DayItineraryUiState(
            crossDayMove = CrossDayMoveDraft("item", isMoving = true),
        )
        val savingMode = DayItineraryUiState(
            modeEditor = RouteModeEditDraft("leg", TransportMode.WALK, isSaving = true),
        )
        val deleting = DayItineraryUiState(
            deleteConfirmation = ItineraryDeleteConfirmation("item", "故宫", isDeleting = true),
        )

        assertEquals(false, canDismissWorkspaceOverlay(WorkspaceOverlay.EditItineraryItem("item"), AddToItineraryUiState(), saving))
        assertEquals(false, canDismissWorkspaceOverlay(WorkspaceOverlay.SelectMoveTargetDay("item"), AddToItineraryUiState(), moving))
        assertEquals(false, canDismissWorkspaceOverlay(WorkspaceOverlay.EditRouteLeg(1L), AddToItineraryUiState(), savingMode))
        assertEquals(false, canDismissWorkspaceOverlay(WorkspaceOverlay.Confirmation(confirmationModel()), AddToItineraryUiState(), deleting))
        assertEquals(true, canDismissWorkspaceOverlay(WorkspaceOverlay.LayerMenu, AddToItineraryUiState(), saving))
        assertEquals(
            false,
            canDismissWorkspaceOverlay(
                WorkspaceOverlay.Confirmation(confirmationModel()),
                AddToItineraryUiState(),
                deleting,
                hasPlaceDeleteConfirmation = true,
                placeDeletionBusy = true,
            ),
        )
    }

    @Test fun timingMenuActionCreatesOnlyEditDraft() = runTest(dispatcher) {
        val model = itineraryModel()
        advanceUntilIdle()

        model.dispatch(DayItineraryAction.RequestTiming("item-1"))

        assertEquals("item-1", model.state.value.editDraft?.itemId)
        assertNull(model.state.value.crossDayMove)
        assertNull(model.state.value.deleteConfirmation)
    }

    @Test fun moveMenuActionCreatesOnlyCrossDayDraft() = runTest(dispatcher) {
        val model = itineraryModel()
        advanceUntilIdle()

        model.dispatch(DayItineraryAction.RequestCrossDay("item-1"))

        assertEquals("item-1", model.state.value.crossDayMove?.itemId)
        assertNull(model.state.value.editDraft)
        assertNull(model.state.value.deleteConfirmation)
    }

    @Test fun deleteMenuActionCreatesOnlyDeleteConfirmation() = runTest(dispatcher) {
        val model = itineraryModel()
        advanceUntilIdle()

        model.dispatch(DayItineraryAction.RequestDelete("item-1"))

        assertEquals("item-1", model.state.value.deleteConfirmation?.itemId)
        assertNull(model.state.value.editDraft)
        assertNull(model.state.value.crossDayMove)
    }

    @Test fun itineraryOverlayAppearsOnlyForEstablishedContext() {
        val edit = ItineraryEditDraft("item-1", "", "")
        val deletion = ItineraryDeleteConfirmation("item-1", "酒店")

        assertNull(itineraryOverlayToPresent(null, null, null, null))
        assertEquals(
            WorkspaceOverlay.EditItineraryItem("item-1"),
            itineraryOverlayToPresent(edit, null, null, null),
        )
        assertEquals(
            "移出酒店？",
            (itineraryOverlayToPresent(null, null, deletion, null) as WorkspaceOverlay.Confirmation).model.title,
        )
    }

    @Test fun crossDayAndModeOverlaysComeFromExistingEditors() {
        val move = CrossDayMoveDraft("item-2")
        val mode = RouteModeEditDraft("leg-3", TransportMode.WALK)

        assertEquals(
            WorkspaceOverlay.SelectMoveTargetDay("item-2"),
            itineraryOverlayToPresent(null, move, null, null),
        )
        assertEquals(
            WorkspaceOverlay.EditRouteLeg(stableWorkspaceOverlayId("leg-3")),
            itineraryOverlayToPresent(null, null, null, mode),
        )
    }

    @Test fun readyLegCreatesModeEditorForSameLeg() = runTest(dispatcher) {
        val model = itineraryModelWithLeg(com.yangchengwei.easytrip.core.model.RouteStatus.SUCCESS)
        advanceUntilIdle()

        model.dispatch(DayItineraryAction.RequestMode("leg-1"))

        assertEquals("leg-1", model.state.value.modeEditor?.legId)
    }

    @Test fun failedLegDoesNotCreateModeEditor() = runTest(dispatcher) {
        val model = itineraryModelWithLeg(com.yangchengwei.easytrip.core.model.RouteStatus.FAILED)
        advanceUntilIdle()

        model.dispatch(DayItineraryAction.RequestMode("leg-1"))

        assertNull(model.state.value.modeEditor)
    }

    @Test fun itineraryOverlayReplacesStaleContextAfterNewContextIsEstablished() {
        val edit = com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft("item-2", "", "")
        val deletion = com.yangchengwei.easytrip.itinerary.ui.ItineraryDeleteConfirmation("item-2", "博物馆")

        assertEquals(
            WorkspaceOverlay.EditItineraryItem("item-2"),
            itineraryOverlayUpdate(
                WorkspaceOverlay.EditItineraryItem("item-1"),
                edit,
                null,
                null,
                null,
            ),
        )
        assertEquals(
            "移出博物馆？",
            (itineraryOverlayUpdate(
                WorkspaceOverlay.Confirmation(
                    com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel(
                        title = "移出酒店？",
                        message = "仅从当天行程移出，收藏仍保留。",
                        confirmLabel = "确认移出",
                        dismissLabel = "取消",
                        deletedItems = emptyList(),
                        retainedItems = emptyList(),
                        destructive = true,
                        reversible = false,
                    ),
                ),
                null,
                null,
                deletion,
                null,
            ) as WorkspaceOverlay.Confirmation).model.title,
        )
        assertNull(
            itineraryOverlayUpdate(
                WorkspaceOverlay.EditItineraryItem("item-1"),
                null,
                null,
                null,
                null,
            ),
        )
    }

    @Test fun selectedPlaceDetailSupersedesMapDetailButNotAddFlow() {
        assertEquals(
            WorkspaceOverlay.PlaceDetail(stableWorkspaceOverlayId("saved-place")),
            placeDetailOverlayToPresent("saved-place", WorkspaceOverlay.PlaceDetail(1L)),
        )
        assertEquals(
            WorkspaceOverlay.PlaceDetail(stableWorkspaceOverlayId("saved-place")),
            placeDetailOverlayToPresent("saved-place", WorkspaceOverlay.None),
        )
        assertNull(placeDetailOverlayToPresent("saved-place", WorkspaceOverlay.SelectAddTargetDay))
        assertNull(placeDetailOverlayToPresent(null, WorkspaceOverlay.PlaceDetail(1L)))
    }

    @Test fun detailCloseDoesNotChangeWorkspaceNavigationOrViewport() = runTest(dispatcher) {
        val model = model(Trips(days("one", "two")))
        advanceUntilIdle()
        model.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()
        model.setSheetLevel(WorkspaceSheetLevel.EXPANDED)
        advanceUntilIdle()
        val sheetBeforeClose = model.state.value.sheetLevel
        model.openOverlay(WorkspaceOverlay.PlaceDetail(1L))

        model.closeOverlay()

        assertEquals(WorkspaceSection.ITINERARY, model.state.value.section)
        assertEquals(sheetBeforeClose, model.state.value.sheetLevel)
        assertEquals(WorkspaceOverlay.None, model.state.value.overlay)
    }

    @Test fun appendCompletionClosesOnlyTheAppendOverlay() {
        assertEquals(
            AppendDayCompletionDecision.CloseOverlayAndConsume(1L),
            appendDayCompletionDecision(WorkspaceOverlay.AddTripDay, completionToken = 1L),
        )
        assertEquals(
            AppendDayCompletionDecision.Consume(1L),
            appendDayCompletionDecision(WorkspaceOverlay.LayerMenu, completionToken = 1L),
        )
        assertEquals(
            AppendDayCompletionDecision.None,
            appendDayCompletionDecision(WorkspaceOverlay.AddTripDay, completionToken = null),
        )
    }

    @Test fun `old target-day draft cannot open from no overlay but active add flow can advance`() {
        val addState = AddToItineraryUiState(
            editingTarget = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.FromPlacePool,
            step = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep.SELECT_TARGET_DAY,
        )

        assertNull(addOverlayToPresent(WorkspaceOverlay.None, addState))
        assertNull(addOverlayToPresent(WorkspaceOverlay.LayerMenu, addState))
        assertEquals(
            WorkspaceOverlay.SelectAddTargetDay,
            addOverlayToPresent(WorkspaceOverlay.SelectAddPlaces, addState),
        )
    }

    @Test fun `aggregate mixed result advances active add overlay regardless of legacy outcome`() {
        val addState = AddToItineraryUiState(
            editingTarget = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace("hotel"),
            step = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep.SELECT_TARGET_DAY,
            result = com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome.Success("day-2", listOf("created-2")),
            submissionResult = com.yangchengwei.easytrip.itinerary.ui.AddToItinerarySubmissionResult(
                createdItemsByDay = listOf(com.yangchengwei.easytrip.itinerary.ui.UndoCreatedItemsBatch("day-2", listOf("created-2"))),
                failedAdditions = listOf(com.yangchengwei.easytrip.itinerary.ui.FailedItineraryAddition("day-1", "hotel")),
                retryTargetDayIds = listOf("day-1"),
            ),
        )

        assertEquals(
            WorkspaceOverlay.AddToItineraryResult,
            addOverlayToPresent(WorkspaceOverlay.SelectAddTargetDay, addState),
        )
    }

    @Test fun `selected saved-place detail remains open when not editing`() {
        assertFalse(
            shouldClosePlaceDetailOverlay(
                overlay = WorkspaceOverlay.PlaceDetail(1L),
                wasEditingPlace = false,
                hasEditingPlace = false,
                selectedDetailPlaceId = "place-1",
                hasSelectedMapPoi = false,
                hasSelectedMarker = false,
            ),
        )
        assertFalse(
            shouldClosePlaceDetailOverlay(
                overlay = WorkspaceOverlay.PlaceDetail(1L),
                wasEditingPlace = false,
                hasEditingPlace = false,
                selectedDetailPlaceId = null,
                hasSelectedMapPoi = false,
                hasSelectedMarker = false,
            ),
        )
        assertTrue(
            shouldClosePlaceDetailOverlay(
                overlay = WorkspaceOverlay.PlaceDetail(1L),
                wasEditingPlace = true,
                hasEditingPlace = false,
                selectedDetailPlaceId = null,
                hasSelectedMapPoi = false,
                hasSelectedMarker = false,
            ),
        )
    }

    @Test fun backClosesOverlayBeforeLeavingWorkspace() = runTest(dispatcher) {
        val model = model(Trips(days("one", "two")))
        advanceUntilIdle()
        model.openOverlay(WorkspaceOverlay.LayerMenu)

        assertEquals(true, model.handleBack())
        advanceUntilIdle()
        assertEquals(WorkspaceOverlay.None, model.state.value.overlay)
        assertEquals(false, model.handleBack())
    }

    @Test fun tabDayScopeAndSheetLevelRestoreFromSavedState() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                "workspace.section" to WorkspaceSection.ITINERARY.name,
                "workspace.itineraryScope" to "DAY:two",
                "workspace.sheet" to WorkspaceSheetLevel.EXPANDED.name,
            ),
        )

        val model = model(Trips(days("one", "two")), handle)
        advanceUntilIdle()

        assertEquals(WorkspaceSection.ITINERARY, model.state.value.section)
        assertEquals(ItineraryScope.Day("two"), model.state.value.itineraryScope)
        assertEquals(WorkspaceSheetLevel.EXPANDED, model.state.value.sheetLevel)
    }

    @Test fun `navigation is unified persisted and retains remembered itinerary scope`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val trips = Trips(days("one", "two"))
        val model = model(trips, handle)
        advanceUntilIdle()

        assertEquals(WorkspaceSection.PLACE_POOL, model.state.value.section)
        assertEquals(ItineraryScope.Day("one"), model.state.value.itineraryScope)
        assertEquals(MapScope.PLACE_POOL, model.state.value.mapScope)
        assertNull(model.state.value.selectedDayId)
        assertNull(model.selectedDayId.value)

        model.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()
        assertEquals(MapScope.SINGLE_DAY, model.state.value.mapScope)
        assertEquals("one", model.state.value.selectedDayId)

        model.selectItineraryScope(ItineraryScope.WholeTrip)
        advanceUntilIdle()
        assertEquals(MapScope.WHOLE_TRIP, model.state.value.mapScope)
        assertNull(model.state.value.selectedDayId)
        assertEquals(listOf("one", "two"), model.state.value.wholeTripDays.map { it.dayId })

        model.selectItineraryScope(ItineraryScope.Day("two"))
        model.selectSection(WorkspaceSection.PLACE_POOL)
        advanceUntilIdle()
        assertEquals(ItineraryScope.Day("two"), model.state.value.itineraryScope)
        assertNull(model.state.value.selectedDayId)
        assertNull(model.selectedDayId.value)

        model.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()
        assertEquals(ItineraryScope.Day("two"), model.state.value.itineraryScope)
        assertEquals("two", model.state.value.selectedDayId)
        assertEquals("two", model.selectedDayId.value)
        assertEquals(WorkspaceSection.ITINERARY.name, handle.get<String>("workspace.section"))
        assertEquals("DAY:two", handle.get<String>("workspace.itineraryScope"))
    }

    @Test fun `legacy navigation migrates once and cannot override new selections`() = runTest(dispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                "workspace.tab" to WorkspaceTab.ITINERARY.name,
                "workspace.scope" to MapScope.SINGLE_DAY.name,
                "workspace.selectedDay" to "two",
            ),
        )
        val trips = Trips(days("one", "two"))
        val first = model(trips, handle)
        advanceUntilIdle()
        assertEquals(WorkspaceSection.ITINERARY, first.state.value.section)
        assertEquals(ItineraryScope.Day("two"), first.state.value.itineraryScope)

        first.selectSection(WorkspaceSection.PLACE_POOL)
        first.selectItineraryScope(ItineraryScope.WholeTrip)
        advanceUntilIdle()
        val restored = model(trips, handle)
        advanceUntilIdle()
        assertEquals(WorkspaceSection.PLACE_POOL, restored.state.value.section)
        assertEquals(ItineraryScope.WholeTrip, restored.state.value.itineraryScope)
    }

    @Test fun `deleted selected day chooses successor then predecessor and no dates chooses whole trip`() = runTest(dispatcher) {
        val trips = Trips(days("one", "two", "three"))
        val model = model(trips)
        advanceUntilIdle()
        model.selectSection(WorkspaceSection.ITINERARY)
        model.selectItineraryScope(ItineraryScope.Day("two"))
        advanceUntilIdle()

        trips.value.value = trip(days("one", "three"))
        advanceUntilIdle()
        assertEquals(ItineraryScope.Day("three"), model.state.value.itineraryScope)

        trips.value.value = trip(days("one"))
        advanceUntilIdle()
        assertEquals(ItineraryScope.Day("one"), model.state.value.itineraryScope)

        trips.value.value = trip(emptyList())
        advanceUntilIdle()
        assertEquals(ItineraryScope.WholeTrip, model.state.value.itineraryScope)
        assertNull(model.state.value.selectedDayId)
    }

    @Test fun `deleted day never renders stale whole trip snapshots or emits an extra viewport`() = runTest(dispatcher) {
        val trips = Trips(days("one", "two"))
        val itineraries = MutableItineraries(
            mapOf(
                "one" to itinerary("one", item("one-item", 39.9)),
                "two" to itinerary("two", item("two-item", 31.2)),
            ),
        )
        val model = model(trips, itineraries = itineraries)
        advanceUntilIdle()
        model.selectSection(WorkspaceSection.ITINERARY)
        model.selectItineraryScope(ItineraryScope.WholeTrip)
        advanceUntilIdle()
        val requestId = model.state.value.map.viewportRequest?.id
        val observed = mutableListOf<Pair<Set<String>, Set<String>>>()
        val observedRequests = mutableListOf<Long?>()
        val job = launch {
            model.state.collect { state ->
                observed += state.days.map(TripDay::id).toSet() to
                    state.map.markers.flatMap { marker -> marker.occurrences.map { it.dayId } }.toSet()
                observedRequests += state.map.viewportRequest?.id
            }
        }

        trips.value.value = trip(days("one"))
        advanceUntilIdle()
        job.cancel()

        assertEquals(setOf("one"), model.state.value.map.markers.flatMap { it.occurrences }.map { it.dayId }.toSet())
        assertEquals(requestId?.plus(1), model.state.value.map.viewportRequest?.id)
        assertEquals(false, observed.any { (dayIds, markerDayIds) -> markerDayIds.any { it !in dayIds } })
        assertEquals(2, observedRequests.distinct().size)
    }

    @Test fun `stale day selection is ignored and real navigation changes request viewport once`() = runTest(dispatcher) {
        val itineraries = Itineraries(
            mapOf(
                "one" to listOf(item("one-item", 39.9)),
                "two" to listOf(item("two-item", 31.2)),
            ),
        )
        val model = model(Trips(days("one", "two")), itineraries = itineraries)
        advanceUntilIdle()
        assertNull(model.state.value.map.viewportRequest)

        model.selectSection(WorkspaceSection.ITINERARY)
        advanceUntilIdle()
        val singleDay = model.state.value.map.viewportRequest?.id
        assertEquals(1L, singleDay)

        model.selectSection(WorkspaceSection.ITINERARY)
        model.selectItineraryScope(ItineraryScope.Day("missing"))
        advanceUntilIdle()
        assertEquals(singleDay, model.state.value.map.viewportRequest?.id)
        assertEquals(ItineraryScope.Day("one"), model.state.value.itineraryScope)

        model.selectItineraryScope(ItineraryScope.WholeTrip)
        advanceUntilIdle()
        assertEquals(singleDay?.plus(1), model.state.value.map.viewportRequest?.id)
    }

    private fun model(
        trips: Trips,
        handle: SavedStateHandle = SavedStateHandle(),
        itineraries: ItineraryRepository = Itineraries(),
    ) = TripWorkspaceViewModel("trip", trips, Places(), itineraries, Legs(), handle)

    private fun itineraryModel() = DayItineraryViewModel(
        tripId = "trip",
        trips = Trips(days("one", "two")),
        itineraries = Itineraries(mapOf("one" to listOf(item("item-1", 39.9)))),
        routeLegs = Legs(),
        coordinator = null,
    )

    private fun itineraryModelWithLeg(status: com.yangchengwei.easytrip.core.model.RouteStatus) = DayItineraryViewModel(
        tripId = "trip",
        trips = Trips(days("one", "two")),
        itineraries = Itineraries(
            mapOf("one" to listOf(item("item-1", 39.9), item("item-2", 40.0))),
        ),
        routeLegs = Legs(
            listOf(
                com.yangchengwei.easytrip.route.data.RouteLegEntity(
                    id = "leg-1",
                    tripDayId = "one",
                    fromItemId = "item-1",
                    toItemId = "item-2",
                    recommendedMode = TransportMode.WALK,
                    status = status,
                    updatedAt = java.time.Instant.EPOCH,
                ),
            ),
        ),
        coordinator = null,
    )

    private fun item(id: String, latitude: Double) = ItineraryItem(
        id,
        ItineraryPlace("place-$id", id, "", GeoPoint(latitude, 116.4)),
        null,
        null,
    )
    private fun itinerary(dayId: String, vararg items: ItineraryItem) = DayItinerary(dayId, "trip", items.toList())

    private fun days(vararg ids: String) = ids.mapIndexed { index, id -> TripDay(id, index) }
    private fun trip(days: List<TripDay>) = TripWithDays("trip", "北京", LocalDate.of(2026, 8, 23), TravelMode.FLEXIBLE, days)

    private inner class Trips(initial: List<TripDay>) : TripRepository {
        val value = MutableStateFlow<TripWithDays?>(trip(initial))
        override fun observeTrip(tripId: String) = value
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class Places : SavedPlaceRepository {
        override fun observePlaces(tripId: String, tagIds: Set<String>) = flowOf(emptyList<SavedPlace>())
        override fun observeTags(tripId: String) = flowOf(emptyList<PlaceTag>())
        override fun observeSavedPoiIds(tripId: String) = flowOf(emptySet<String>())
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("place")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class MutableItineraries(initial: Map<String, DayItinerary>) : ItineraryRepository {
        private val days = initial.mapValues { MutableStateFlow(it.value) }
        override fun observeDay(dayId: String) = days.getValue(dayId)
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "item"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Itineraries(
        private val itemsByDay: Map<String, List<ItineraryItem>> = emptyMap(),
    ) : ItineraryRepository {
        override fun observeDay(dayId: String) = flowOf(DayItinerary(dayId, "trip", itemsByDay[dayId].orEmpty()))
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int) = "item"
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) = Unit
        override suspend fun deleteItem(itemId: String) = Unit
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) = Unit
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class Legs(
        private val legs: List<com.yangchengwei.easytrip.route.data.RouteLegEntity> = emptyList(),
    ) : RouteLegRepository {
        override fun observeDay(dayId: String) = flowOf(legs.filter { it.tripDayId == dayId })
        override fun observePending(): Flow<List<RouteLegWithEndpoints>> = flowOf(emptyList())
        override suspend fun get(legId: String) = null
        override suspend fun requeueTransientFailures() = 0
        override suspend fun recoverInterruptedCalculations(online: Boolean) = 0
        override suspend fun repairCorruptPolyline(legId: String, version: Long) = false
        override suspend fun claimIfVersionMatches(legId: String, version: Long) = false
        override suspend fun waitForNetworkIfVersionMatches(legId: String, version: Long) = false
        override suspend fun releaseClaimIfVersionMatches(legId: String, version: Long, online: Boolean) = false
        override suspend fun completeIfVersionMatches(legId: String, version: Long, result: RouteResult) = false
        override suspend fun failIfVersionMatches(legId: String, version: Long, failure: RoutePlanOutcome.Failure) = false
        override suspend fun overrideMode(legId: String, mode: com.yangchengwei.easytrip.core.model.TransportMode, online: Boolean) = false
        override suspend fun retry(legId: String, online: Boolean) = false
    }
}
