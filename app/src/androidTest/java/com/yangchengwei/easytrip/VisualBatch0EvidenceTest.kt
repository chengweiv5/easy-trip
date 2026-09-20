package com.yangchengwei.easytrip

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep
import com.yangchengwei.easytrip.itinerary.ui.AddToItinerarySubmissionResult
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.FailedItineraryAddition
import com.yangchengwei.easytrip.itinerary.ui.UndoCreatedItemsBatch
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.RouteLegContent
import com.yangchengwei.easytrip.itinerary.ui.RouteLegUi
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.ui.PlacePoolContent
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.place.ui.PlaceSearchContent
import com.yangchengwei.easytrip.place.ui.PlaceSearchPhase
import com.yangchengwei.easytrip.place.ui.PlaceSearchState
import com.yangchengwei.easytrip.place.ui.PlaceSearchUiState
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.ui.CreateTripContent
import com.yangchengwei.easytrip.trip.ui.CreateTripUiState
import com.yangchengwei.easytrip.trip.ui.DateRangeChangePhase
import com.yangchengwei.easytrip.trip.ui.DateRangeChangeRequest
import com.yangchengwei.easytrip.trip.ui.DateRangeChangeUiState
import com.yangchengwei.easytrip.trip.ui.DayUi
import com.yangchengwei.easytrip.trip.ui.TripListContent
import com.yangchengwei.easytrip.trip.ui.TripListPageState
import com.yangchengwei.easytrip.trip.ui.TripListUiState
import com.yangchengwei.easytrip.trip.ui.TripSettingsContent
import com.yangchengwei.easytrip.trip.ui.TripSettingsUiState
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.ItineraryScope
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi
import com.yangchengwei.easytrip.workspace.TripWorkspaceContent
import com.yangchengwei.easytrip.workspace.TripWorkspaceScreen
import com.yangchengwei.easytrip.workspace.WorkspaceOverlay
import com.yangchengwei.easytrip.workspace.TripWorkspacePageState
import com.yangchengwei.easytrip.workspace.TripWorkspaceUiState
import com.yangchengwei.easytrip.workspace.WorkspaceMapState
import com.yangchengwei.easytrip.workspace.WorkspaceSection
import com.yangchengwei.easytrip.workspace.WorkspaceSheetLevel
import com.yangchengwei.easytrip.workspace.toReadyState
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class VisualBatch0EvidenceTest {
    data class ControlledWorkspaceEvidence(
        val host: String,
        val stateSource: String,
        val mapSurface: String,
    )

    companion object {
        val batch4WorkspaceHostEvidence = setOf("xQfD0", "cRdBn", "Pqdkf", "p7U8B", "yNKT4", "mGhKO", "D3XZi", "KPBBb")
            .associateWith {
                ControlledWorkspaceEvidence(
                    host = "production Compose workspace host",
                    stateSource = "controlled UiState; not Room-triggered",
                    mapSurface = "deterministic fake map surface; not a real map host",
                )
            }
        private val batch5Frames = setOf(
            "nAdK8", "K336N", "mz2IS", "l2xCsM", "T7aESo",
            "eHTX3", "FTIOF", "Bcf6A", "zvO9Z", "J7PZ7u",
        )
        val batch6Evidence: Map<String, List<Batch6FrameEvidence>> = Batch6TypedScenario.entries
            .associate { typed ->
                val scenario = V1ScenarioFixtures.scenarios.single { it.number == typed.number }
                val checkpoint = Batch6FrameCheckpoint.entries.single { it.frameId == typed.frameId }
                typed.frameId to batch6EvidenceFor(scenario, checkpoint, typed.fixtureId)
            }
        val batch5Evidence: Map<String, Batch5FrameEvidence> = V1ScenarioFixtures.scenarios
            .flatMap { scenario ->
                listOf(
                    V1ScenarioVariant(
                        parentNumber = scenario.number,
                        name = scenario.name,
                        frameId = scenario.frameId,
                        declaredIdentity = scenario.declaredIdentity,
                    ),
                ) + scenario.variants
            }
            .filter { it.frameId in batch5Frames }
            .associateBy(V1ScenarioVariant::frameId) { scenario ->
                Batch5FrameEvidence(
                    scenario = scenario,
                    checkpoint = Batch5FrameCheckpoint.entries.single { it.frameId == scenario.frameId },
                    executable = Batch5ExecutableEvidence.ProductionNavigationMainFlow,
                    host = EvidenceHost.ProductionAppNavigation,
                    stateSource = EvidenceStateSource.InMemoryRoomNavigation,
                    mapSurface = EvidenceMapSurface.RecordingFakeMapHost,
                )
            }

        private fun batch6EvidenceFor(
            scenario: V1Scenario,
            checkpoint: Batch6FrameCheckpoint,
            fixtureId: String,
        ): List<Batch6FrameEvidence> {
            val controlled = Batch6FrameEvidence(
                scenario = scenario,
                checkpoint = checkpoint,
                fixtureId = fixtureId,
                host = EvidenceHost.ProductionCompose,
                stateSource = EvidenceStateSource.ControlledUiState,
                mapSurface = EvidenceMapSurface.None,
                permissionSurface = if (checkpoint in setOf(
                    Batch6FrameCheckpoint.MapConsentExplanation,
                    Batch6FrameCheckpoint.LocationExplanation,
                    Batch6FrameCheckpoint.LocationSettingsRecovery,
                )) EvidencePermissionSurface.ControlledSnapshot else EvidencePermissionSurface.None,
                automatedEntry = "com.yangchengwei.easytrip.V1ScenarioMetadataTest#${checkpoint.frameId}ExecutesBatch6TypedScenario",
                limitations = "production Composable with controlled UiState; not Room, navigation, SDK, or Android-system evidence",
            )
            val higherLevel = when (checkpoint) {
                Batch6FrameCheckpoint.WaitingForNetwork -> Batch6FrameEvidence(
                    scenario, checkpoint, fixtureId,
                    EvidenceHost.ProductionRepository,
                    EvidenceStateSource.FileBackedRoomReopen,
                    EvidenceMapSurface.None,
                    EvidencePermissionSurface.None,
                    "com.yangchengwei.easytrip.OfflineRecoveryTest#persistedRoutesRecoverInterruptedWorkWithoutTouchingSuccess",
                    "reopens a file-backed Room database, then starts production RoomRouteLegRepository + DefaultRouteRefreshCoordinator across offline-to-online recovery; planner remains controlled, and this is not a full navigation flow or installed-app restart",
                )
                Batch6FrameCheckpoint.FailedRoute -> Batch6FrameEvidence(
                    scenario, checkpoint, fixtureId,
                    EvidenceHost.ProductionCompose,
                    EvidenceStateSource.ControlledUiState,
                    EvidenceMapSurface.None,
                    EvidencePermissionSurface.None,
                    "com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest#failedRouteShowsErrorAndRetryAction",
                    "production itinerary host with controlled repositories; not Room persistence or real AMap evidence",
                )
                Batch6FrameCheckpoint.MapConsentExplanation -> Batch6FrameEvidence(
                    scenario, checkpoint, fixtureId,
                    EvidenceHost.ProductionAppNavigation,
                    EvidenceStateSource.HandwrittenRepositoryFakes,
                    EvidenceMapSurface.None,
                    EvidencePermissionSurface.ControlledSnapshot,
                    "com.yangchengwei.easytrip.workspace.WorkspaceFlowTest#undecidedWorkspaceEntryShowsConsentExplanationUntilDecision",
                    "consent store and handwritten repository fakes are controlled; no map host is created, and this is not Android system permission or real AMap evidence",
                )
                Batch6FrameCheckpoint.LocationExplanation,
                Batch6FrameCheckpoint.LocationSettingsRecovery -> Batch6FrameEvidence(
                    scenario, checkpoint, fixtureId,
                    EvidenceHost.ProductionAppNavigation,
                    EvidenceStateSource.HandwrittenRepositoryFakes,
                    EvidenceMapSurface.RecordingFakeMapHost,
                    EvidencePermissionSurface.ActivityResultContract,
                    "com.yangchengwei.easytrip.workspace.WorkspaceFlowTest#appNavigationBindsPermissionCallbackToRequestThenSettingsResumeGrantShowsLocationOnce",
                    "Activity Result contract and handwritten repository fakes are injected; LocationRecordingHost records the single post-settings location call, while Android system settings UI is not exercised",
                )
                Batch6FrameCheckpoint.MapLoading -> Batch6FrameEvidence(
                    scenario, checkpoint, fixtureId,
                    EvidenceHost.ProductionCompose,
                    EvidenceStateSource.ControlledUiState,
                    EvidenceMapSurface.RecordingFakeMapHost,
                    EvidencePermissionSurface.None,
                    "com.yangchengwei.easytrip.workspace.WorkspaceFlowTest#neverReadyMapShowsApprovedFallbackKeepsContentAndRetryRecreatesOnlyHost",
                    "production workspace host records controlled fake-host creation, disposal, and retry; not real AMap rendering",
                )
                Batch6FrameCheckpoint.MapFailure -> Batch6FrameEvidence(
                    scenario, checkpoint, fixtureId,
                    EvidenceHost.ProductionCompose,
                    EvidenceStateSource.ControlledUiState,
                    EvidenceMapSurface.FailureInjectingFakeMapHost,
                    EvidencePermissionSurface.None,
                    "com.yangchengwei.easytrip.workspace.WorkspaceFlowTest#mapFailureKeepsLocalTabsAndActionsReachable",
                    "production workspace host uses a failure-injecting fake map host; not real AMap rendering",
                )
                Batch6FrameCheckpoint.EditSaveFailure -> Batch6FrameEvidence(
                    scenario, checkpoint, fixtureId,
                    EvidenceHost.ProductionAppNavigation,
                    EvidenceStateSource.InMemoryRoomNavigation,
                    EvidenceMapSurface.RecordingFakeMapHost,
                    EvidencePermissionSurface.None,
                    "com.yangchengwei.easytrip.V2AcceptanceTest#productionNavigationSaveFailurePreservesDraftAndRetriesOnceAgainstRoom",
                    "in-memory Room and recording fake map host; no installed-app restart or real AMap evidence",
                )
            }
            return listOf(controlled, higherLevel)
        }
    }

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val testName = TestName()

    @Test fun batch6EvidenceManifestHasEveryControlledFrameAndNoUnobservedSurface() {
        val evidence = batch6Evidence

        assertEquals(Batch6FrameCheckpoint.entries.map(Batch6FrameCheckpoint::frameId).toSet(), evidence.keys)
        evidence.values.flatten().forEach { record ->
            assertEquals(record.fixtureId, record.executable().fixture.id)
            assertTrue(record.limitations.isNotBlank())
        }
        assertTrue(evidence.values.flatten().none {
            it.host == EvidenceHost.PhysicalDevice ||
                it.stateSource == EvidenceStateSource.InstalledAppRestart ||
                it.mapSurface == EvidenceMapSurface.RealAmap ||
                it.permissionSurface == EvidencePermissionSurface.AndroidSystem
        })
    }

    @Test fun batch4FramesRenderThroughProductionWorkspaceHostWithControlledState() {
        val place = savedPlace(searchCandidates()[1], "batch4-place")
        val frames = listOf(
            "xQfD0" to Batch4HostState(WorkspaceOverlay.SelectAddTargetDay, AddToItineraryUiState(selectedPlaceIds = listOf(place.id), editingTarget = AddToItineraryEditingTarget.ForPlace(place.id), validityInitialized = true, step = AddToItineraryStep.SELECT_TARGET_DAY), listOf(TripDay("day-1", 0), TripDay("day-2", 1)), "加入行程"),
            "cRdBn" to Batch4HostState(WorkspaceOverlay.SelectAddTargetDay, AddToItineraryUiState(selectedPlaceIds = listOf(place.id), editingTarget = AddToItineraryEditingTarget.ForPlace(place.id), validityInitialized = true, step = AddToItineraryStep.SELECT_TARGET_DAY), (0 until 30).map { TripDay("day-$it", it) }, "加入行程"),
            "Pqdkf" to Batch4HostState(WorkspaceOverlay.SelectAddPlaces, AddToItineraryUiState(selectedPlaceIds = listOf(place.id), targetDayId = "day-1", editingTarget = AddToItineraryEditingTarget.ForDay("day-1"), step = AddToItineraryStep.SELECT_PLACES), workspaceDays(), "从地点池添加"),
            "p7U8B" to Batch4HostState(WorkspaceOverlay.SelectAddTargetDay, AddToItineraryUiState(selectedPlaceIds = listOf(place.id), editingTarget = AddToItineraryEditingTarget.ForPlace(place.id), step = AddToItineraryStep.SELECT_TARGET_DAY), emptyList(), "还没有旅行日"),
            "yNKT4" to Batch4HostState(WorkspaceOverlay.AddToItineraryResult, AddToItineraryUiState(submissionResult = AddToItinerarySubmissionResult(createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("item")))), undoBatches = listOf(UndoCreatedItemsBatch("day-1", listOf("item")))), workspaceDays(), "已加入第 1 天"),
            "mGhKO" to Batch4HostState(WorkspaceOverlay.AddToItineraryResult, AddToItineraryUiState(submissionResult = AddToItinerarySubmissionResult(createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("item"))), failedAdditions = listOf(FailedItineraryAddition("day-2", place.id)), retryTargetDayIds = listOf("day-2")), undoBatches = listOf(UndoCreatedItemsBatch("day-1", listOf("item")))), workspaceDays(), "部分地点已加入行程"),
            "D3XZi" to Batch4HostState(WorkspaceOverlay.AddToItineraryResult, AddToItineraryUiState(submissionResult = AddToItinerarySubmissionResult(missingTargetDayIds = listOf("day-2"), missingTargetDayLabels = mapOf("day-2" to "第 2 天"))), workspaceDays(), "所选旅行日已不存在"),
            "KPBBb" to Batch4HostState(WorkspaceOverlay.AddToItineraryResult, AddToItineraryUiState(submissionResult = AddToItinerarySubmissionResult(createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("item"))))), workspaceDays(), "已从第 1 天移除"),
        )
        val current = androidx.compose.runtime.mutableStateOf(frames.first())
        compose.setContent {
            val (_, hostState) = current.value
            TripWorkspaceScreen(
                pageState = TripWorkspacePageState.Ready(workspaceState(WorkspaceSection.PLACE_POOL).content.copy(days = hostState.days, overlay = hostState.overlay)),
                consent = acceptedConsentToken(), onAction = {}, onMarkerClick = {}, onMapPoiClick = {},
                placeState = PlacePoolUiState(rows = listOf(SavedPlaceRowUi(place, 0, false))), onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = hostState.days, selectedDayId = hostState.days.firstOrNull()?.id),
                addToItineraryState = hostState.addState, onItineraryAction = {}, onCloseOverlay = {}, onDismissMapPlace = {},
                mapHostFactory = ::DeterministicFakeMapHost,
            )
        }
        frames.forEach { frame ->
            compose.runOnIdle { current.value = frame }
            compose.waitForIdle()
            compose.onNodeWithText(frame.second.expectedText, substring = true).assertIsDisplayed()
        }
    }

    private data class Batch4HostState(
        val overlay: WorkspaceOverlay,
        val addState: AddToItineraryUiState,
        val days: List<TripDay>,
        val expectedText: String,
    )

    @Test fun evidenceManifestRecordsJUnitMethodAndRuntimeSerial() {
        val manifest = evidenceManifest(
            frameId = "metadata",
            evidenceName = "metadata-check",
            testMethod = testName.methodName,
            deviceSerial = "runtime-serial",
        )

        assertEquals(testName.methodName, manifest.getString("test_method"))
        assertEquals("metadata-check", manifest.getString("evidence_name"))
        assertEquals("runtime-serial", manifest.getString("device_serial"))
    }

    @Test fun missingDeviceSerialIsRecordedAsUnknown() {
        assertEquals("unknown", normalizedDeviceSerial(null))
        assertEquals("unknown", normalizedDeviceSerial("   "))
    }

    @Test fun settings_U06l7P() {
        render("U06l7P", "settings") {
            TripSettingsContent(
                state = settingsState(DateRangeChangePhase.Idle),
                onBack = {}, onRename = {}, onTravelMode = {}, onSubmitDateRange = {},
                onCancelDateRange = {}, onConfirmDateRange = {}, onRetryDateRangeSync = {},
                onRequestDeleteDay = {}, onRetryDeleteDay = {}, onCancelDeleteDay = {}, onConfirmDeleteDay = {},
                onDateRangeDraft = { _, _ -> },
            )
        }
        compose.onNodeWithText("整体出行日期").assertIsDisplayed()
    }

    @Test fun dateConfirmation_IKTv5() {
        render("IKTv5", "date-confirmation") {
            TripSettingsContent(
                state = settingsState(DateRangeChangePhase.AwaitingConfirmation(request(), impact())),
                onBack = {}, onRename = {}, onTravelMode = {}, onSubmitDateRange = {},
                onCancelDateRange = {}, onConfirmDateRange = {}, onRetryDateRangeSync = {},
                onRequestDeleteDay = {}, onRetryDeleteDay = {}, onCancelDeleteDay = {}, onConfirmDeleteDay = {},
                onDateRangeDraft = { _, _ -> },
            )
        }
        compose.onNodeWithText("确认修改日期范围？").assertIsDisplayed()
    }

    @Test fun search_ofdn5() {
        render("ofdn5", "search") {
            PlaceSearchContent(
                PlaceSearchUiState(
                    search = PlaceSearchState("西湖", searchCandidates(), phase = PlaceSearchPhase.Results),
                ),
                {},
            )
        }
        compose.onNodeWithText("西湖天地").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_p4G1tS() {
        val place = savedPlace(searchCandidates()[1], "saved-scheduled")
        render(
            "p4G1tS",
            "workspace-place-detail-scheduled",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceScreen(
                pageState = workspaceState(WorkspaceSection.PLACE_POOL).let { state ->
                    TripWorkspacePageState.Ready(
                        state.content.copy(
                            overlay = WorkspaceOverlay.PlaceDetail(1L),
                            schedulesByPlaceId = mapOf(
                                place.id to PlaceScheduleSummaryUi(
                                    isKnown = true,
                                    totalOccurrences = 3,
                                    days = listOf(
                                        com.yangchengwei.easytrip.workspace.PlaceScheduleDayUi("day-1", 0, 2),
                                        com.yangchengwei.easytrip.workspace.PlaceScheduleDayUi("day-3", 2, 1),
                                    ),
                                ),
                            ),
                        ),
                    )
                },
                consent = acceptedConsentToken(),
                onAction = {}, onMarkerClick = {}, onMapPoiClick = {},
                placeState = PlacePoolUiState(
                    rows = listOf(SavedPlaceRowUi(place, 3, scheduled = true)),
                    savedPoiIds = setOf(place.amapPoiId),
                    selectedDetailPlaceId = place.id,
                    selectedDetailPlace = place,
                ),
                onPlaceAction = {}, itineraryState = DayItineraryUiState(), onItineraryAction = {},
                onCloseOverlay = {}, onDismissMapPlace = {}, mapHostFactory = ::DeterministicFakeMapHost,
            )
        }
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        compose.onNodeWithText("已加入行程").assertIsDisplayed()
        compose.onAllNodesWithTag("place-detail-start-add").assertCountEquals(0)
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_shoPV() {
        render(
            "shoPV",
            "workspace-map-layer",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(
                    section = WorkspaceSection.PLACE_POOL,
                    overlay = com.yangchengwei.easytrip.workspace.WorkspaceOverlay.LayerMenu,
                ),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        compose.onNodeWithTag("layer-menu-panel").assertIsDisplayed()
        compose.onNodeWithTag("layer-menu-scrim").assertIsDisplayed()
        compose.onNodeWithTag("layer-STANDARD").assertIsDisplayed()
        compose.onNodeWithTag("layer-SATELLITE").assertIsDisplayed()
        compose.onNodeWithTag("layer-SATELLITE_ROAD").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_BrYVA() {
        render(
            "BrYVA",
            "workspace-all-empty",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(
                    section = WorkspaceSection.PLACE_POOL,
                    isItineraryAllEmpty = true,
                    isWorkspaceAllEmpty = true,
                ),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithTag("workspace-all-empty").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_WFOpg() {
        render(
            "WFOpg",
            "workspace-itinerary-all-empty",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(
                    section = WorkspaceSection.ITINERARY,
                    isItineraryAllEmpty = true,
                ),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithTag("itinerary-all-empty").assertIsDisplayed()
        compose.onAllNodesWithTag("itinerary-scope-rail").assertCountEquals(0)
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_jQhXs() {
        render(
            "jQhXs",
            "workspace-place-pool-short-list",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.PLACE_POOL),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(rows = 3),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithText("西湖风景名胜区").assertIsDisplayed()
        compose.onNodeWithText("知味观·湖滨店").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_XsGon() {
        val place = savedPlace(searchCandidates()[1], "saved-only")
        render(
            "XsGon",
            "workspace-place-detail-only-collected",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceScreen(
                pageState = workspaceState(WorkspaceSection.PLACE_POOL).let { state ->
                    TripWorkspacePageState.Ready(
                        state.content.copy(
                            overlay = WorkspaceOverlay.PlaceDetail(1L),
                            schedulesByPlaceId = mapOf(place.id to PlaceScheduleSummaryUi(isKnown = true)),
                        ),
                    )
                },
                consent = acceptedConsentToken(),
                onAction = {}, onMarkerClick = {}, onMapPoiClick = {},
                placeState = PlacePoolUiState(
                    rows = listOf(SavedPlaceRowUi(place, 0, scheduled = false)),
                    savedPoiIds = setOf(place.amapPoiId),
                    selectedDetailPlaceId = place.id,
                    selectedDetailPlace = place,
                ),
                onPlaceAction = {}, itineraryState = DayItineraryUiState(), onItineraryAction = {},
                onCloseOverlay = {}, onDismissMapPlace = {}, mapHostFactory = ::DeterministicFakeMapHost,
            )
        }
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-bookmark-outline").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-start-add").assertIsDisplayed()
        compose.onAllNodesWithTag("place-detail-schedule").assertCountEquals(0)
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_A9EKX() {
        render(
            "A9EKX",
            "workspace-place-pool",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.PLACE_POOL),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithTag("start-add-to-itinerary").assertIsDisplayed()
        compose.onNodeWithTag("workspace-place-list").assertIsDisplayed()
        compose.onNodeWithTag("map-collection-summary").assertIsDisplayed()
        compose.onNodeWithText("4 个收藏地点").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_LFmzR() {
        render(
            "LFmzR",
            "workspace-itinerary",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            val items = itineraryItems()
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(
                    days = workspaceDays(),
                    selectedDayId = "day-1",
                    items = items,
                    previewOrder = items.map { it.id },
                    legs = itineraryLegs(),
                ),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithText("西湖天地").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-rail").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_eHTX3() {
        render(
            "eHTX3",
            "workspace-day-itinerary",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            val items = itineraryItems()
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(
                    days = workspaceDays(),
                    selectedDayId = "day-1",
                    items = items,
                    previewOrder = items.map { it.id },
                    legs = itineraryLegs(),
                ),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithText("西湖天地").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-rail").assertIsDisplayed()
    }

    @Test fun workspaceSheetCollapsed_kCc5z() {
        render(
            "kCc5z",
            "workspace-sheet-collapsed",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY, WorkspaceSheetLevel.COLLAPSED),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = workspaceDays(), selectedDayId = "day-1", items = itineraryItems()),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        compose.onNodeWithTag("workspace-collapsed-summary").assertIsDisplayed()
        compose.onAllNodesWithTag("day-itinerary-timeline").assertCountEquals(0)
    }

    @Test fun workspaceSheetHalf_sWTB3() {
        render(
            "sWTB3",
            "workspace-sheet-half",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY, WorkspaceSheetLevel.HALF),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = workspaceDays(), selectedDayId = "day-1", items = itineraryItems(), previewOrder = itineraryItems().map { it.id }, legs = itineraryLegs()),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
    }

    @Test fun workspaceSheetExpanded_f2ieZ6() {
        render(
            "f2ieZ6",
            "workspace-sheet-expanded",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY, WorkspaceSheetLevel.EXPANDED),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = workspaceDays(), selectedDayId = "day-1", items = itineraryItems(), previewOrder = itineraryItems().map { it.id }, legs = itineraryLegs()),
                onItineraryAction = {},
                mapContent = { _ -> DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("workspace-sheet").assertIsDisplayed()
        compose.onAllNodesWithTag("workspace-search-launcher").assertCountEquals(0)
    }

    @Test fun routeLegStates_batch5() {
        val longError = "路线规划失败：这是一段用于截图验证的较长错误说明，保持两行展示并提供重试操作"
        render("batch5", "route-leg-states") {
            Column {
                RouteLegContent(RouteLegUi("pending", "a", "b", TransportMode.WALK, RouteStatus.PENDING, null, null, null))
                RouteLegContent(RouteLegUi("calculating", "b", "c", TransportMode.WALK, RouteStatus.CALCULATING, null, null, null))
                RouteLegContent(RouteLegUi("waiting", "c", "d", TransportMode.WALK, RouteStatus.WAITING_NETWORK, null, null, null))
                RouteLegContent(RouteLegUi("failed", "d", "e", TransportMode.WALK, RouteStatus.FAILED, null, null, longError), onRetry = {})
            }
        }
        compose.onNodeWithText("等待计算路线").assertIsDisplayed()
        compose.onNodeWithText("正在计算路线").assertIsDisplayed()
        compose.onNodeWithText("等待联网后计算").assertIsDisplayed()
        compose.onNodeWithText(longError).assertIsDisplayed()
    }

    @Test fun emptyTrips_zIbEu() {
        render("zIbEu", "empty-trips") {
            TripListContent(TripListUiState(page = TripListPageState.Empty), {})
        }
        compose.onNodeWithTag("empty-illustration-trips").assertIsDisplayed()
    }

    @Test fun emptyPlacePool_lsr1I() {
        render("lsr1I", "empty-place-pool") {
            PlacePoolContent(state = PlacePoolUiState(), showSearch = false, onAction = {})
        }
        compose.onNodeWithTag("empty-illustration-places").assertIsDisplayed()
    }

    @Test fun emptyDay_Bcf6A() {
        render("Bcf6A", "empty-day") {
            DayItineraryContent(
                DayItineraryUiState(days = listOf(TripDay("day-1", 0)), selectedDayId = "day-1"),
                onAction = {},
            )
        }
        compose.onNodeWithTag("empty-illustration-itinerary").assertIsDisplayed()
    }

    @Test fun emptySearch_batch6() {
        render("batch6-search", "empty-search") {
            PlaceSearchContent(
                PlaceSearchUiState(search = PlaceSearchState("不存在", phase = PlaceSearchPhase.Empty)),
                {},
            )
        }
        compose.onNodeWithTag("empty-illustration-search").assertIsDisplayed()
    }

    @Test fun emptyFailure_batch6() {
        render("batch6-failure", "empty-failure") {
            PlaceSearchContent(
                PlaceSearchUiState(search = PlaceSearchState("西湖", phase = PlaceSearchPhase.NetworkFailure("网络不可用"))),
                {},
            )
        }
        compose.onNodeWithTag("empty-illustration-failure").assertIsDisplayed()
    }

    @Test fun create_dzhkC() {
        render("dzhkC", "create") {
            CreateTripContent(
                CreateTripUiState("杭州·春日慢游", LocalDate.of(2025, 4, 12), LocalDate.of(2025, 4, 14), TravelMode.FLEXIBLE),
                {},
            )
        }
        compose.onNodeWithTag("create-submit").assertIsDisplayed()
    }

    @Test fun home_K9h3r() {
        val featured = com.yangchengwei.easytrip.trip.ui.TripCardUiModel(
            "trip", "杭州 · 春日慢游", "3天2晚", "4月12日 — 4月14日", "灵活出行",
            8, 2, "8 个地点", "3 天行程", 72, "72%",
        )
        render("K9h3r", "home") {
            TripListContent(TripListUiState(page = TripListPageState.Content(featured, listOf(
                featured.copy(id = "other-1", name = "川西小环线", dateLabel = null, travelModeLabel = "自驾"),
                featured.copy(id = "other-2", name = "泉州古城散步", dateLabel = "5月2日 — 5月4日"),
            ))), {})
        }
        compose.onNodeWithTag("create-trip").assertIsDisplayed()
    }

    @Test fun itemEditor_K336N() {
        renderEditor("K336N", "item-editor", WorkspaceOverlay.EditItineraryItem("item-2"),
            DayItineraryUiState(editDraft = com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft(
                "item-2", "12:00", "60", placeId = "saved-1", placeName = "知味观 · 湖滨店",
            )))
        compose.onNodeWithTag("arrival-hour-picker").assertIsDisplayed()
        compose.onNodeWithText("保存时间").assertIsDisplayed()
    }

    @Test fun routeEditor_T7aESo() {
        renderEditor("T7aESo", "route-editor", WorkspaceOverlay.EditRouteLeg("leg-2"),
            DayItineraryUiState(modeEditor = com.yangchengwei.easytrip.itinerary.ui.RouteModeEditDraft(
                "leg-2", TransportMode.DRIVE, fromPlaceName = "知味观 · 湖滨店", toPlaceName = "龙井村",
                distanceMeters = 11000, plannedDurationSeconds = 1680,
            )))
        compose.onNodeWithTag("save-route").assertIsDisplayed()
    }

    @Test fun wholeTrip_FTIOF() {
        render("FTIOF", "whole-trip") {
            TripWorkspaceContent(
                pageState = TripWorkspacePageState.Ready(workspaceState(WorkspaceSection.ITINERARY).content.copy(
                    itineraryScope = ItineraryScope.WholeTrip,
                    wholeTripDays = listOf(com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi("day-1", 1, itineraryItems(), itineraryLegs())),
                )),
                mapState = WorkspaceMapState.Ready, onAction = {}, placeState = placePoolState(), onPlaceAction = {},
                itineraryState = DayItineraryUiState(), onItineraryAction = {}, mapContent = { _ -> DeterministicFakeMapSurface() },
            )
        }
        compose.onNodeWithTag("whole-trip-content").assertIsDisplayed()
    }

    @Test fun longTargetDays_cRdBn() {
        val days = (0 until 30).map { TripDay("long-day-$it", it) }
        render("cRdBn", "long-target-days", evidenceHost = "production Compose workspace host", mapSurface = "deterministic fake map surface") {
            TripWorkspaceScreen(
                pageState = TripWorkspacePageState.Ready(workspaceState(WorkspaceSection.PLACE_POOL).content.copy(days = days, overlay = WorkspaceOverlay.SelectAddTargetDay)),
                consent = acceptedConsentToken(), onAction = {}, onMarkerClick = {}, onMapPoiClick = {},
                placeState = placePoolState(), onPlaceAction = {}, itineraryState = DayItineraryUiState(), onItineraryAction = {},
                addToItineraryState = AddToItineraryUiState(selectedPlaceIds = listOf("saved-1"), editingTarget = AddToItineraryEditingTarget.ForPlace("saved-1"), selectedTargetDayIds = listOf("long-day-0")),
                onCloseOverlay = {}, onDismissMapPlace = {}, mapHostFactory = ::DeterministicFakeMapHost,
            )
        }
        compose.onNodeWithTag("select-target-day-submit").assertIsDisplayed()
        compose.onNodeWithTag("select-target-day-list").performScrollToNode(hasTestTag("target-day-long-day-29"))
        compose.onNodeWithTag("target-day-long-day-29").assertIsDisplayed()
        compose.onNodeWithTag("select-target-day-submit").assertIsDisplayed()
    }

    private fun renderEditor(frame: String, name: String, overlay: WorkspaceOverlay, state: DayItineraryUiState) {
        render(frame, name, evidenceHost = "production Compose workspace host", mapSurface = "deterministic fake map surface") {
            TripWorkspaceScreen(
                pageState = workspaceState(WorkspaceSection.ITINERARY, overlay = overlay),
                consent = acceptedConsentToken(), onAction = {}, onMarkerClick = {}, onMapPoiClick = {},
                placeState = placePoolState(), onPlaceAction = {}, itineraryState = state, onItineraryAction = {},
                onCloseOverlay = {}, onDismissMapPlace = {}, mapHostFactory = ::DeterministicFakeMapHost,
            )
        }
        compose.onNodeWithTag("workspace-editor-sheet").assertIsDisplayed()
    }

    private fun render(
        frameId: String,
        name: String,
        evidenceHost: String = "isolated Compose fixture",
        mapSurface: String = "not applicable",
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.setContent { EasyTripTheme { content() } }
        compose.waitForIdle()
        when (name) {
            "settings" -> compose.onNodeWithText("旅行设置").assertIsDisplayed()
            "date-confirmation" -> compose.onNodeWithText("确认修改日期范围？").assertIsDisplayed()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(300)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = context.resources.configuration
        val actualWidthDp = config.screenWidthDp
        check(actualWidthDp in 387..390) {
            "Visual evidence expects a target phone width of 387..390dp, but was ${actualWidthDp}dp"
        }
        val directory = File(context.filesDir, "evidence/batch-0").apply { mkdirs() }
        val png = File(directory, "$name-$frameId.png")
        val shot = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            FileOutputStream(png).use { check(shot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { shot.recycle() }
        File(directory, "$name-$frameId.manifest.json").writeText(
            evidenceManifest(
                frameId = frameId,
                evidenceName = name,
                testMethod = testName.methodName,
                deviceSerial = runtimeDeviceSerial(),
                evidenceHost = evidenceHost,
                mapSurface = mapSurface,
                actualWidthDp = actualWidthDp,
                bitmapWidthPx = shot.width,
                bitmapHeightPx = shot.height,
                windowHeightDp = config.screenHeightDp,
                densityDpi = config.densityDpi,
                fontScale = config.fontScale.toDouble(),
                pngSha256 = sha256(png),
            ).toString(2),
        )
    }

    private fun evidenceManifest(
        frameId: String,
        evidenceName: String,
        testMethod: String,
        deviceSerial: String,
        evidenceHost: String = "isolated Compose fixture",
        mapSurface: String = "not applicable",
        actualWidthDp: Int = 390,
        bitmapWidthPx: Int = 0,
        bitmapHeightPx: Int = 0,
        windowHeightDp: Int = 0,
        densityDpi: Int = 0,
        fontScale: Double = 1.0,
        pngSha256: String = "",
    ) = JSONObject()
        .put("frame_id", frameId)
        .put("evidence_name", evidenceName)
        .put("test_class", javaClass.name)
        .put("test_method", testMethod)
        .put("evidence_host", evidenceHost)
        .put("map_surface", mapSurface)
        .put("baseline_width_dp", 390)
        .put("target_phone_width_dp_range", "387..390")
        .put("actual_width_dp", actualWidthDp)
        .put("git_sha", BuildConfig.GIT_SHA)
        .put("source_state", BuildConfig.SOURCE_STATE)
        .put("device_serial", normalizedDeviceSerial(deviceSerial))
        .put("device_fingerprint", Build.FINGERPRINT)
        .put("device_model", Build.MODEL)
        .put("device_sdk", Build.VERSION.SDK_INT)
        .put("bitmap_width_px", bitmapWidthPx)
        .put("bitmap_height_px", bitmapHeightPx)
        .put("window_width_dp", actualWidthDp)
        .put("window_height_dp", windowHeightDp)
        .put("density_dpi", densityDpi)
        .put("font_scale", fontScale)
        .put("png_sha256", pngSha256)

    private fun runtimeDeviceSerial(): String {
        val arguments = InstrumentationRegistry.getArguments()
        val provided = arguments.getString("device_serial") ?: arguments.getString("deviceSerial")
        if (!provided.isNullOrBlank()) return provided
        return runCatching { Build.getSerial() }
            .getOrNull()
            .let(::normalizedDeviceSerial)
    }

    private fun normalizedDeviceSerial(value: String?): String = value?.trim().takeUnless { it.isNullOrEmpty() || it == Build.UNKNOWN } ?: "unknown"

    private fun acceptedConsentToken(): com.yangchengwei.easytrip.amap.AmapConsentToken {
        val gate = com.yangchengwei.easytrip.amap.TestConsentGate()
        gate.show()
        return requireNotNull(gate.decide(true))
    }

    private class DeterministicFakeMapHost(context: Context) : AmapMapHost {
        override val view = View(context).apply { setBackgroundColor(android.graphics.Color.rgb(220, 232, 223)) }
        override fun canRenderBeforeReady() = true
        override fun onCreate() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun onDestroy() = Unit
        override fun render(
            model: MapUiModel,
            layer: MapLayer,
            onMarkerClick: (String) -> Unit,
            onMapPoiClick: (MapPoiUi) -> Unit,
            onLayerError: (Throwable, MapLayer) -> Unit,
        ) = Unit
    }

    @androidx.compose.runtime.Composable
    private fun DeterministicFakeMapSurface() {
        Box(Modifier.fillMaxSize().background(Color(0xFFDCE8DF)).testTag("deterministic-fake-map-surface"))
    }

    private fun assertWorkspaceHost() {
        compose.onNodeWithTag("workspace-root").assertIsDisplayed()
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        compose.onNodeWithTag("deterministic-fake-map-surface").assertIsDisplayed()
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("workspace-search-launcher").assertIsDisplayed()
        compose.onNodeWithTag("workspace-locate").assertIsDisplayed()
        compose.onNodeWithTag("layer-menu").assertIsDisplayed()
        compose.onNodeWithTag("map-legend").assertIsDisplayed()
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithTag("workspace-sheet").assertIsDisplayed()
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed()
    }

    private fun workspaceState(
        section: WorkspaceSection,
        sheetLevel: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF,
        overlay: com.yangchengwei.easytrip.workspace.WorkspaceOverlay = com.yangchengwei.easytrip.workspace.WorkspaceOverlay.None,
        isItineraryAllEmpty: Boolean = false,
        isWorkspaceAllEmpty: Boolean = false,
    ) = TripWorkspacePageState.Ready(
        TripWorkspaceUiState(
            tripName = "杭州·春日慢游",
            dateLabel = "4月12日 — 4月14日",
            startDate = LocalDate.of(2025, 4, 12),
            days = workspaceDays(),
            section = section,
            itineraryScope = ItineraryScope.Day("day-1"),
            sheetLevel = sheetLevel,
            overlay = overlay,
            isItineraryAllEmpty = isItineraryAllEmpty,
            isWorkspaceAllEmpty = isWorkspaceAllEmpty,
        ).toReadyState(),
    )

    private fun placePoolState(rows: Int = searchCandidates().size) = PlacePoolUiState(
        savedPoiIds = searchCandidates().take(rows).map { it.poiId }.toSet(),
        rows = searchCandidates().take(rows).mapIndexed { index, candidate ->
            SavedPlaceRowUi(savedPlace(candidate, "saved-$index"), 0, scheduled = index == 0)
        },
        tags = listOf(PlaceTag("tag-1", "景点"), PlaceTag("tag-2", "散步")),
    )

    private fun workspaceDays() = listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2))

    private fun itineraryItems() = listOf(
        ItineraryItemUi("item-1", "西湖天地", "上城区南山路", java.time.LocalTime.of(9, 30), 90),
        ItineraryItemUi("item-2", "知味观·湖滨店", "仁和路83号", java.time.LocalTime.of(12, 0), 60),
        ItineraryItemUi("item-3", "龙井村", "西湖区龙井路", java.time.LocalTime.of(14, 30), 120),
    )

    private fun itineraryLegs() = listOf(
        RouteLegUi("leg-1", "item-1", "item-2", TransportMode.WALK, RouteStatus.SUCCESS, 1200, 1080, null),
        RouteLegUi("leg-2", "item-2", "item-3", TransportMode.DRIVE, RouteStatus.SUCCESS, 11000, 1680, null),
    )

    private fun request() = DateRangeChangeRequest(1, "trip", LocalDate.of(2025, 4, 12), listOf("day-1", "day-2", "day-3"), LocalDate.of(2025, 4, 12), LocalDate.of(2025, 4, 12))
    private fun impact() = DateRangeChangeImpact(request(), listOf("day-1"), listOf("day-2", "day-3"), 3, 2, 8)
    private fun settingsState(phase: DateRangeChangePhase) = TripSettingsUiState(
        tripId = "trip", name = "杭州·春日慢游", travelMode = TravelMode.FLEXIBLE,
        days = listOf(DayUi("day-1", "2025-04-12"), DayUi("day-2", "2025-04-13"), DayUi("day-3", "2025-04-14")),
        dateRange = DateRangeChangeUiState(LocalDate.of(2025, 4, 12), LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 12), true, phase),
    )
    private fun searchCandidates() = listOf(
        PlaceCandidate("poi-1", "西湖风景名胜区", "西湖区龙井路1号", GeoPoint(30.23, 120.10), "0571"),
        PlaceCandidate("poi-2", "西湖天地", "上城区南山路147号", GeoPoint(30.22, 120.15), "0571"),
        PlaceCandidate("poi-3", "知味观·湖滨店", "仁和路83号", GeoPoint(30.25, 120.16), "0571"),
        PlaceCandidate("poi-4", "龙井村", "西湖区龙井路", GeoPoint(30.21, 120.11), "0571"),
    )
    private fun savedPlace(candidate: PlaceCandidate = searchCandidates()[1], id: String = "saved") = SavedPlace(id, "trip", candidate.poiId, candidate.name, candidate.address, candidate.point!!, "傍晚沿湖散步，预留看日落的时间", listOf(PlaceTag("tag-1", "景点")))
    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
