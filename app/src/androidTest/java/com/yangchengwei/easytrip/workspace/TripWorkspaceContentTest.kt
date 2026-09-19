package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi
import com.yangchengwei.easytrip.trip.domain.TripDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TripWorkspaceContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun notFoundShowsBackToTripsAction() {
        var action: TripWorkspaceAction? = null
        setContent(TripWorkspacePageState.NotFound, WorkspaceMapState.Loading, onAction = { action = it })
        compose.onNodeWithText("返回旅行列表").performClick()
        assertEquals(TripWorkspaceAction.Back, action)
    }

    @Test fun readyMapRemainsMountedWhilePermissionGuidanceUsesIndependentOverlay() {
        setContent(ready(), WorkspaceMapState.Ready)

        compose.onNodeWithText("地图就绪").assertIsDisplayed()
        compose.onNodeWithTag("workspace-map-fallback").assertDoesNotExist()
    }

    @Test fun jointWorkspaceEmptyShowsBrYVAWithoutContentCta() {
        setContent(
            TripWorkspacePageState.Ready(
                TripWorkspaceUiState(
                    tripName = "北京",
                    days = listOf(TripDay("day-1", 0)),
                    isItineraryAllEmpty = true,
                    isWorkspaceAllEmpty = true,
                ).toReadyState(),
            ),
            WorkspaceMapState.Ready,
        )

        compose.onNodeWithTag("workspace-all-empty").assertIsDisplayed()
        compose.onNodeWithText("旅行还是空的").assertIsDisplayed()
        compose.onNodeWithText("还没有收藏地点，也没有安排任何行程。先搜索想去的地方，收藏后再加入旅行日。").assertIsDisplayed()
        compose.onNodeWithTag("workspace-place-list").assertDoesNotExist()
        compose.onAllNodesWithText("搜索地点").assertCountEquals(0)
    }

    @Test fun itineraryFullEmptyShowsWFOpgForBothScopesWithoutRailOrContentCta() {
        val scope = mutableStateOf<ItineraryScope>(ItineraryScope.WholeTrip)
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(
                        TripWorkspaceUiState(
                            tripName = "北京",
                            days = listOf(TripDay("day-1", 0)),
                            section = WorkspaceSection.ITINERARY,
                            itineraryScope = scope.value,
                            isItineraryAllEmpty = true,
                        ).toReadyState(),
                    ),
                    mapState = WorkspaceMapState.Ready,
                    onAction = {},
                    placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { _ -> Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                )
            }
        }

        compose.onNodeWithTag("itinerary-all-empty").assertIsDisplayed()
        compose.onNodeWithText("还没有安排行程").assertIsDisplayed()
        compose.onNodeWithText("当前旅行的所有旅行日都没有行程项。先去地点池收藏地点，再添加到对应旅行日。").assertIsDisplayed()
        compose.onAllNodesWithTag("itinerary-scope-rail").assertCountEquals(0)
        compose.onAllNodesWithTag("add-places-to-selected-day").assertCountEquals(0)
        compose.runOnIdle { scope.value = ItineraryScope.Day("day-1") }
        compose.onNodeWithTag("itinerary-all-empty").assertIsDisplayed()
        compose.onAllNodesWithTag("itinerary-scope-rail").assertCountEquals(0)
    }

    @Test fun ordinaryPlaceEmptyRemainsWhenItineraryHasItems() {
        setContent(
            TripWorkspacePageState.Ready(
                TripWorkspaceUiState(
                    tripName = "北京",
                    days = listOf(TripDay("day-1", 0)),
                    isItineraryAllEmpty = false,
                    isWorkspaceAllEmpty = false,
                ).toReadyState(),
            ),
            WorkspaceMapState.Ready,
        )

        compose.onNodeWithTag("workspace-all-empty").assertDoesNotExist()
        compose.onNodeWithText("还没有收藏地点").assertIsDisplayed()
        compose.onNodeWithText("搜索地点").assertIsDisplayed()
    }

    @Test fun declinedConsentKeepsPlacePoolAndItineraryInteractive() {
        val actions = mutableListOf<TripWorkspaceAction>()
        setContent(ready(), WorkspaceMapState.ConsentRequired, actions::add)
        compose.onNodeWithTag("map-consent-required").assertIsDisplayed()
        compose.onNodeWithText("地点池").assertIsDisplayed()
        compose.onNodeWithText("还没有收藏地点").assertExists()
        compose.onNodeWithText("行程").performClick()
        compose.onNodeWithTag("workspace-more").performClick()

        assertEquals(
            listOf(
                TripWorkspaceAction.SelectSection(WorkspaceSection.ITINERARY),
                TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.MoreMenu),
            ),
            actions,
        )
    }

    @Test fun nonReadyMapHidesMapOnlyControlsButKeepsLocalWorkspaceUsable() {
        setContent(ready(), WorkspaceMapState.ConsentRequired)

        listOf("map-legend", "layer-menu", "zoom-in", "zoom-out", "workspace-locate").forEach { tag ->
            compose.onAllNodesWithTag(tag).assertCountEquals(0)
        }
        compose.onNodeWithText("地点池").assertIsDisplayed()
        compose.onNodeWithText("行程").assertIsDisplayed()
    }

    @Test fun mapFailureRetryIsAboveSheetAndInvokesDedicatedCallbackOnce() {
        val actions = mutableListOf<TripWorkspaceAction>()
        val retryCalls = AtomicInteger()
        setContent(
            ready(),
            WorkspaceMapState.Failed("地图加载失败"),
            onAction = actions::add,
            onMapRetry = { retryCalls.incrementAndGet() },
        )
        compose.waitForIdle()
        val retry = compose.onNodeWithTag("map-retry")
        val sheet = compose.onNodeWithTag("workspace-sheet")
        retry.assertIsDisplayed()
        assertTrue(
            "retry=${retry.getUnclippedBoundsInRoot()} sheet=${sheet.getUnclippedBoundsInRoot()}",
            retry.getUnclippedBoundsInRoot().bottom <= sheet.getUnclippedBoundsInRoot().top,
        )
        retry.performClick()
        compose.waitForIdle()
        assertEquals(1, retryCalls.get())
        assertTrue(actions.isEmpty())
    }

    @Test fun mapFailureRetryRemainsReachableAtEverySheetLevelInSmallWindow() {
        val level = mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        val retryCalls = AtomicInteger()
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f),
            ) {
                EasyTripTheme {
                    Box(Modifier.width(280.dp).height(280.dp).testTag("small-failed-workspace")) {
                        TripWorkspaceContent(
                            pageState = ready(level.value),
                            mapState = WorkspaceMapState.Failed("地图加载失败"),
                            onAction = {},
                            onMapRetry = { retryCalls.incrementAndGet() },
                            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                            onPlaceAction = {},
                            itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                            onItineraryAction = {},
                            mapContent = { _ -> Text("地图就绪") },
                            placeContent = { Text("地点内容保持可见", Modifier.testTag("failure-place-content")) },
                            modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                        )
                    }
                }
            }
        }

        WorkspaceSheetLevel.entries.forEachIndexed { index, sheetLevel ->
            compose.runOnIdle { level.value = sheetLevel }
            val retry = compose.onNodeWithTag("map-retry")
                .assertIsDisplayed()
                .assertHasClickAction()
            val bounds = retry.getUnclippedBoundsInRoot()
            assertTrue(
                "level=$sheetLevel retry=$bounds",
                bounds.right - bounds.left >= 48.dp && bounds.bottom - bounds.top >= 48.dp,
            )
            listOf(
                "workspace-top-bar",
                "workspace-trip-title",
                "workspace-back",
                "workspace-more",
                "workspace-tabs",
                "workspace-sheet-handle-control",
            ).forEach { tag ->
                assertNoOverlap(sheetLevel.name, bounds, compose.onNodeWithTag(tag).getUnclippedBoundsInRoot())
            }
            assertContainedBy(
                sheetLevel.name,
                bounds,
                compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot(),
            )
            compose.onAllNodesWithTag("map-retry").assertCountEquals(1)
            compose.onNodeWithTag("section-PLACE_POOL").assertIsSelected()
            retry.performClick()
            compose.runOnIdle { assertEquals(index + 1, retryCalls.get()) }
        }
    }

    @Test fun mapConsentRecoveryRemainsReachableAtEverySheetLevelInSmallWindow() {
        val level = mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        val openCalls = AtomicInteger()
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f),
            ) {
                EasyTripTheme {
                    Box(Modifier.width(280.dp).height(280.dp)) {
                        TripWorkspaceContent(
                            pageState = ready(level.value),
                            mapState = WorkspaceMapState.ConsentRequired,
                            onAction = {},
                            onOpenConsent = { openCalls.incrementAndGet() },
                            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                            onPlaceAction = {},
                            itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                            onItineraryAction = {},
                            mapContent = { _ -> Text("地图就绪") },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        WorkspaceSheetLevel.entries.forEachIndexed { index, sheetLevel ->
            compose.runOnIdle { level.value = sheetLevel }
            val open = compose.onNodeWithTag("map-consent-open")
                .assertIsDisplayed()
                .assertHasClickAction()
            val bounds = open.getUnclippedBoundsInRoot()
            assertTrue(
                "level=$sheetLevel open=$bounds",
                bounds.right - bounds.left >= 48.dp && bounds.bottom - bounds.top >= 48.dp,
            )
            assertContainedBy(
                sheetLevel.name,
                bounds,
                compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot(),
            )
            compose.onAllNodesWithTag("map-consent-open").assertCountEquals(1)
            open.performClick()
            compose.runOnIdle { assertEquals(index + 1, openCalls.get()) }
        }
    }

    @Test fun readyKeepsSearchBackAndItineraryActionsWhileMoreOpensOverlay() {
        val actions = mutableListOf<TripWorkspaceAction>()
        setContent(ready(), WorkspaceMapState.Ready, actions::add)
        compose.waitForIdle()
        actions.clear()
        compose.onNodeWithTag("workspace-back").performClick()
        compose.onNodeWithTag("workspace-more").performClick()
        compose.onNodeWithContentDescription("搜索地点").performClick()
        compose.onNodeWithText("地点池").assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        compose.onNodeWithText("行程").assertIsNotSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        assertEquals(
            listOf(
                TripWorkspaceAction.Back,
                TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.MoreMenu),
                TripWorkspaceAction.OpenSearch,
                TripWorkspaceAction.SelectSection(WorkspaceSection.PLACE_POOL),
                TripWorkspaceAction.SelectSection(WorkspaceSection.ITINERARY),
            ),
            actions,
        )
    }

    @Test fun searchLauncherStaysAboveSheetForPhysicalClicks() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()

        val launcher = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()

        assertTrue("launcher=$launcher sheet=$sheet", launcher.bottom <= sheet.top)
    }

    @Test fun searchLegendAndMapControlsStayAboveCurrentSheet() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()

        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        listOf("workspace-search-launcher", "map-legend", "workspace-locate", "layer-menu").forEach { tag ->
            val overlay = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("tag=$tag overlay=$overlay sheet=$sheet", overlay.bottom <= sheet.top)
        }
    }

    @Test fun controlsTrackSheetTopDuringActiveDragWithoutEnteringSheet() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()
        val initialSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 80f))
            advanceEventTime(100)
        }
        compose.waitForIdle()
        val draggedSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        assertTrue("initial=$initialSheetTop dragged=$draggedSheetTop", draggedSheetTop < initialSheetTop)
        listOf("layer-menu", "workspace-locate", "workspace-search-launcher").forEach { tag ->
            val controls = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("tag=$tag controls=$controls sheet=$draggedSheetTop", controls.bottom <= draggedSheetTop)
        }
    }

    @Test fun dragCancelReturnsSheetAndSearchToCurrentLevelTogether() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()
        val initialSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        val initialSearchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 80f))
            advanceEventTime(100)
            cancel()
        }
        compose.waitForIdle()

        assertEquals(initialSheetTop, compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top)
        assertEquals(initialSearchBottom, compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom)
    }

    @Test fun dragEndBelowThresholdReturnsSheetAndSearchToCurrentLevelTogether() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()
        val initialSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        val initialSearchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 10f))
            advanceEventTime(100)
            up()
        }
        compose.waitForIdle()

        assertEquals(initialSheetTop, compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top)
        assertEquals(initialSearchBottom, compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom)
    }

    @Test fun rejectedLevelChangeKeepsSheetAndSearchAtControlledLevel() {
        val requestedLevels = mutableListOf<WorkspaceSheetLevel>()
        setContent(ready(), WorkspaceMapState.Ready, onAction = { action ->
            if (action is TripWorkspaceAction.SetSheetLevel) requestedLevels += action.level
        })
        compose.waitForIdle()
        val initialSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        val initialSearchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 200f))
            advanceEventTime(100)
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf(WorkspaceSheetLevel.EXPANDED), requestedLevels)
        assertEquals(initialSheetTop, compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top)
        assertEquals(initialSearchBottom, compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom)
    }

    @Test fun delayedLevelAcceptanceKeepsGeometryTogetherBeforeAndAfterParentUpdate() {
        val level = mutableStateOf(WorkspaceSheetLevel.HALF)
        val requestedLevel = mutableStateOf<WorkspaceSheetLevel?>(null)
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = ready(level.value),
                    mapState = WorkspaceMapState.Ready,
                    onAction = { action ->
                        if (action is TripWorkspaceAction.SetSheetLevel) requestedLevel.value = action.level
                    },
                    placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { _ -> Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                )
            }
        }
        compose.waitForIdle()
        val initialSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        val initialSearchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 200f))
            advanceEventTime(100)
            up()
        }
        compose.waitForIdle()

        assertEquals(WorkspaceSheetLevel.EXPANDED, requestedLevel.value)
        assertEquals(initialSheetTop, compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top)
        assertEquals(initialSearchBottom, compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom)

        compose.runOnIdle { level.value = requestedLevel.value!! }
        compose.waitForIdle()
        val expandedSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        assertTrue("half=$initialSheetTop expanded=$expandedSheetTop", expandedSheetTop < initialSheetTop)
        compose.onAllNodesWithTag("workspace-search-launcher").assertCountEquals(0)
    }

    @Test fun inProgressDragKeepsSheetBottomAnchoredAndSharesVisibleHeightWithOverlays() {
        val dragDelta = mutableStateOf(0f)
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().height(600.dp).testTag("workspace-root")) {
                    val anchors = workspaceSheetAnchors(600.dp)
                    val visibleHeight = with(androidx.compose.ui.platform.LocalDensity.current) {
                        (anchors[WorkspaceSheetLevel.HALF].toPx() - dragDelta.value).toDp()
                    }
                    val metrics = workspaceLayoutMetrics(600.dp, visibleHeight)
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "overlay",
                            Modifier.align(Alignment.BottomCenter).padding(bottom = metrics.overlayBottomInset).testTag("drag-overlay"),
                        )
                    }
                    WorkspaceBottomSheet(
                        value = WorkspaceSheetLevel.HALF,
                        anchors = anchors,
                        onValueChange = {},
                        dragOffsetPx = dragDelta.value,
                        onDragOffsetChange = { dragDelta.value = it },
                        header = { Text("拖动") },
                        content = { Text("内容") },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        compose.waitForIdle()

        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val handle = compose.onNodeWithTag("workspace-sheet-handle")
        handle.performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 200f))
            advanceEventTime(100)
        }
        compose.waitForIdle()
        assertDragGeometry(root)

        handle.performTouchInput {
            moveTo(Offset(center.x, center.y + 1_000f))
            advanceEventTime(100)
        }
        compose.waitForIdle()
        assertDragGeometry(root)
    }

    private fun assertDragGeometry(root: androidx.compose.ui.unit.DpRect) {
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val overlay = compose.onNodeWithTag("drag-overlay").getUnclippedBoundsInRoot()
        assertEquals("sheet=$sheet root=$root", root.bottom, sheet.bottom)
        assertTrue("sheet=$sheet root=$root", sheet.top >= root.top && sheet.bottom <= root.bottom)
        assertTrue("overlay=$overlay sheet=$sheet", overlay.bottom <= sheet.top)
    }

    @Test fun activeGestureUsesLatestLevelChangeCallback() {
        val calls = mutableListOf<Int>()
        val callback = mutableStateOf<(WorkspaceSheetLevel) -> Unit>({ calls += 0 })
        compose.setContent {
            EasyTripTheme {
                WorkspaceScaffold(
                    sheetLevel = WorkspaceSheetLevel.HALF,
                    onSheetLevelChange = callback.value,
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    map = {},
                    topOverlay = {},
                    sheetHeader = { Text("拖动") },
                    sheetContent = {},
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 200f))
            advanceEventTime(100)
        }
        compose.runOnIdle { callback.value = { calls += 1 } }
        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            advanceEventTime(100)
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf(1), calls)
    }

    @Test fun overlayPositionsFollowMetricsUntilMapSpaceIsExhausted() {
        val level = mutableStateOf(WorkspaceSheetLevel.HALF)
        setContentForLevel(level)
        compose.waitForIdle()
        val halfSheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val halfSearch = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot()
        assertTrue("search=$halfSearch sheet=$halfSheet", halfSearch.bottom <= halfSheet.top)

        compose.runOnIdle { level.value = WorkspaceSheetLevel.EXPANDED }
        compose.waitForIdle()
        listOf("workspace-search-launcher", "map-legend", "workspace-locate", "layer-menu").forEach { tag ->
            compose.onAllNodesWithTag(tag).assertCountEquals(0)
        }
    }

    @Test fun expandedSheetKeepsTopBarAndHidesMapOverlaysWhenSpaceIsInsufficient() {
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(844.dp)) {
                    TripWorkspaceContent(
                        pageState = ready(WorkspaceSheetLevel.EXPANDED),
                        mapState = WorkspaceMapState.Ready,
                        onAction = {},
                        placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                        onPlaceAction = {},
                        itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                        onItineraryAction = {},
                        mapContent = { _ -> Text("地图就绪") },
                        modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        listOf("workspace-search-launcher", "workspace-locate", "layer-menu", "map-legend").forEach { tag ->
            compose.onAllNodesWithTag(tag).assertCountEquals(0)
        }
    }

    @Test fun expandedSheetHidesMapFailureFallbackAndKeepsRetryInsideSheet() {
        setContent(ready(WorkspaceSheetLevel.EXPANDED), WorkspaceMapState.Failed("地图加载失败"))

        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("map-load-failed").assertDoesNotExist()
        val retry = compose.onNodeWithTag("map-retry").assertIsDisplayed().assertHasClickAction()
        assertContainedBy(
            "EXPANDED",
            retry.getUnclippedBoundsInRoot(),
            compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot(),
        )
    }

    @Test fun layerMenuPanelStaysBelowTopBarAtStandardHalfHeight() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().height(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(
                            TripWorkspaceUiState(
                                tripName = "北京",
                                sheetLevel = WorkspaceSheetLevel.HALF,
                                overlay = WorkspaceOverlay.LayerMenu,
                            ).toReadyState(),
                        ),
                        mapState = WorkspaceMapState.Ready,
                        onAction = {},
                        placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                        onPlaceAction = {},
                        itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                        onItineraryAction = {},
                        mapContent = { _ -> Text("地图就绪") },
                        modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    )
                }
            }
        }
        compose.waitForIdle()

        val topBar = compose.onNodeWithTag("workspace-top-bar").getUnclippedBoundsInRoot()
        val panel = compose.onNodeWithTag("layer-menu-panel").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        assertTrue("topBar=$topBar panel=$panel", panel.top >= topBar.bottom)
        assertTrue("panel=$panel sheet=$sheet", panel.bottom <= sheet.top)
        compose.onNodeWithTag("map-legend").assertIsDisplayed()
    }

    @Test fun mapOverlaysHideAsOneGroupWhenLiveSheetTopCannotFitAllControls() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()
        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 1_000f))
            advanceEventTime(100)
        }
        compose.waitForIdle()

        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        listOf("workspace-search-launcher", "map-legend", "layer-menu", "zoom-in", "zoom-out", "workspace-locate").forEach { tag ->
            compose.onNodeWithTag(tag).assertDoesNotExist()
        }
    }

    @Test fun safeWorkspaceTooShortClosesLayerMenuInsteadOfKeepingInvisibleOverlayState() {
        val actions = mutableListOf<TripWorkspaceAction>()
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().height(400.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(
                            TripWorkspaceUiState(
                                tripName = "北京",
                                sheetLevel = WorkspaceSheetLevel.HALF,
                                overlay = WorkspaceOverlay.LayerMenu,
                            ).toReadyState(),
                        ),
                        mapState = WorkspaceMapState.Ready,
                        onAction = actions::add,
                        placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                        onPlaceAction = {},
                        itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                        onItineraryAction = {},
                        mapContent = { _ -> Text("地图就绪") },
                        modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("layer-menu-panel").assertDoesNotExist()
        assertEquals(listOf(TripWorkspaceAction.CloseOverlay), actions)
    }

    @Test fun mapFailureRetryRemainsAboveSheet() {
        setContent(ready(), WorkspaceMapState.Failed("地图加载失败"))
        compose.waitForIdle()

        val retry = compose.onNodeWithTag("map-retry").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        assertTrue("retry=$retry sheet=$sheet", retry.bottom <= sheet.top)
    }

    @Test fun workspaceTabsUseIndicatorAndTabSemantics() {
        setContent(ready(), WorkspaceMapState.Ready)

        compose.onNodeWithTag("workspace-tabs").assertExists()
        compose.onNodeWithTag("workspace-tab-indicator-PLACE_POOL", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("地点池").assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        compose.onNodeWithText("行程").assertIsNotSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }

    @Test fun allSheetLevelsKeepMapSubtreeAndUseDistinctConstrainedHeights() {
        val level = mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        setContentForLevel(level)
        val heights = WorkspaceSheetLevel.entries.map { nextLevel ->
            compose.runOnIdle { level.value = nextLevel }
            compose.waitForIdle()
            compose.onNodeWithTag("workspace-map").assertExists()
            compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().height
        }

        assertTrue("heights=$heights", heights[0] < heights[1] && heights[1] < heights[2])
    }

    @Test fun collapsedSheetKeepsTabsAndRealPlaceSummaryWithoutBusinessList() {
        setContent(ready(WorkspaceSheetLevel.COLLAPSED), WorkspaceMapState.Ready)

        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed()
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithText("还没有收藏地点").assertExists()
        compose.onNodeWithText("上滑展开").assertExists()
        compose.onNodeWithTag("workspace-place-list").assertDoesNotExist()
    }

    @Test fun collapsedSheetUsesTotalSavedPlaceCountWhenTagFilterHidesRows() {
        setContent(
            ready(WorkspaceSheetLevel.COLLAPSED),
            WorkspaceMapState.Ready,
            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                rows = emptyList(),
                selectedTagIds = setOf("tag"),
                savedPoiIds = setOf("poi-1", "poi-2"),
            ),
        )

        compose.onNodeWithText("已收藏 2 个地点").assertExists()
    }

    @Test fun collapsedSheetUsesActualItineraryDayAndPlaceCount() {
        setContent(
            itineraryReady(WorkspaceSheetLevel.COLLAPSED, ItineraryScope.Day("day-1")),
            WorkspaceMapState.Ready,
        )

        compose.onNodeWithText("第 1 天 · 0 个地点").assertExists()
        compose.onNodeWithTag("day-itinerary-timeline").assertDoesNotExist()
    }

    @Test fun sheetAlwaysStaysInsideWorkspaceRoot() {
        val level = mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        setContentForLevel(level)
        WorkspaceSheetLevel.entries.forEach { nextLevel ->
            compose.runOnIdle { level.value = nextLevel }
            compose.waitForIdle()

            val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
            val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
            assertTrue("level=$nextLevel root=$root sheet=$sheet", sheet.top >= root.top && sheet.bottom <= root.bottom)
        }
    }

    @Test fun halfSheetMatchesDesignProportionAndStaysBelowTopSafeArea() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()

        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val topBar = compose.onNodeWithTag("workspace-top-bar").getUnclippedBoundsInRoot()

        val anchors = workspaceSheetAnchors(root.height)
        assertTrue(
            "root=$root sheet=$sheet anchors=$anchors",
            kotlin.math.abs((sheet.height - anchors.half).value) <= 1f,
        )
        assertTrue("root=$root sheet=$sheet", sheet.bottom <= root.bottom)
        assertTrue("root=$root topBar=$topBar", topBar.top >= root.top)
        assertTrue("root=$root topBar=$topBar", topBar.top - root.top <= 48.dp)
    }

    @Test fun workspaceHeightBoundaryKeepsExpandedSheetContinuousAndAllAnchorsOrdered() {
        val boundaries = listOf(395.dp, 396.dp, 397.dp)
        val anchors = boundaries.map { workspaceSheetAnchors(it) }

        anchors.zipWithNext().forEach { (before, after) ->
            assertTrue("before=$before after=$after", after.collapsed >= before.collapsed)
            assertTrue("before=$before after=$after", after.half >= before.half)
            assertTrue("before=$before after=$after", after.expanded > before.expanded)
            assertTrue("before=$before after=$after", after.expanded - before.expanded < 2.dp)
        }
        anchors.forEach { anchor ->
            assertTrue("anchors=$anchor", anchor.collapsed < anchor.half && anchor.half < anchor.expanded)
        }
    }

    @Test fun constrainedHeightKeepsPlacePoolReachableAtSmallWindowAndLargeFont() {
        val places = (1..8).map { index ->
            com.yangchengwei.easytrip.place.domain.SavedPlace("$index", "trip", "poi-$index", "地点 $index", "地址", com.yangchengwei.easytrip.core.model.GeoPoint(39.9, 116.4), "", emptyList())
        }
        compose.setContent {
            val deviceDensity = androidx.compose.ui.platform.LocalDensity.current.density
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(deviceDensity, 2f),
            ) {
                EasyTripTheme {
                    androidx.compose.foundation.layout.Box(Modifier.height(280.dp).testTag("small-window")) {
                        TripWorkspaceContent(
                            pageState = ready(WorkspaceSheetLevel.EXPANDED),
                            mapState = WorkspaceMapState.Ready,
                            onAction = {},
                            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(rows = places.map { com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(it, 0, false) }),
                            onPlaceAction = {},
                            itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                            onItineraryAction = {},
                            mapContent = { _ -> Text("地图就绪") },
                            modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("workspace-place-list").performScrollToIndex(8)
        compose.onNodeWithTag("saved-place-8").assertIsDisplayed()
    }

    @Test fun searchReturnKeepsSheetAnchorAndHighlightsRecentCollections() {
        val places = listOf(
            com.yangchengwei.easytrip.place.domain.SavedPlace("new", "trip", "poi-new", "新收藏", "地址", com.yangchengwei.easytrip.core.model.GeoPoint(39.9, 116.4), "", emptyList()),
            com.yangchengwei.easytrip.place.domain.SavedPlace("old", "trip", "poi-old", "原收藏", "地址", com.yangchengwei.easytrip.core.model.GeoPoint(39.91, 116.4), "", emptyList()),
        )
        val searchReturn = mutableStateOf<WorkspaceSearchReturn?>(null)
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = ready(),
                    mapState = WorkspaceMapState.Ready,
                    onAction = {},
                    placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                        rows = places.map { com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(it, 0, false) },
                    ),
                    onPlaceAction = {},
                    itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { _ -> Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    searchReturn = searchReturn.value,
                )
            }
        }
        compose.waitForIdle()
        val regularHalfHeight = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().height
        compose.runOnIdle { searchReturn.value = WorkspaceSearchReturn(setOf("poi-new")) }
        compose.waitForIdle()

        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val returnedSheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val anchors = workspaceSheetAnchors(root.height)
        assertTrue(
            "root=$root regular=$regularHalfHeight returned=$returnedSheet anchors=$anchors",
            kotlin.math.abs((returnedSheet.height - regularHalfHeight).value) <= 1f &&
                kotlin.math.abs((returnedSheet.height - anchors.half).value) <= 1f,
        )
        compose.onNodeWithText("刚刚收藏 · 待安排行程").assertExists()
        compose.onNodeWithTag("workspace-place-list").performScrollToNode(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "saved-place-old"),
        )
        compose.onNodeWithText("原收藏").assertExists()
    }

    @Test fun halfSheetKeepsScopeRailAndSelectedDayReachable() {
        val actions = mutableListOf<TripWorkspaceAction>()
        setContent(itineraryReady(WorkspaceSheetLevel.HALF, ItineraryScope.Day("day-1")), WorkspaceMapState.Ready, actions::add)

        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val rail = compose.onNodeWithTag("itinerary-scope-rail").getUnclippedBoundsInRoot()
        val day = compose.onNodeWithTag("day-content").getUnclippedBoundsInRoot()
        assertTrue("sheet=$sheet rail=$rail", rail.left >= sheet.left && rail.right <= sheet.right)
        assertTrue("sheet=$sheet day=$day", day.left >= sheet.left && day.right <= sheet.right)
        compose.onNodeWithTag("itinerary-scope-WHOLE_TRIP").performClick()
        assertEquals(TripWorkspaceAction.SelectItineraryScope(ItineraryScope.WholeTrip), actions.last())
    }

    @Test fun expandedSheetKeepsWholeTripContentReachable() {
        val actions = mutableListOf<TripWorkspaceAction>()
        setContent(itineraryReady(WorkspaceSheetLevel.EXPANDED, ItineraryScope.WholeTrip), WorkspaceMapState.Ready, actions::add)

        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val wholeTrip = compose.onNodeWithTag("whole-trip-content").getUnclippedBoundsInRoot()
        assertTrue("sheet=$sheet content=$wholeTrip", wholeTrip.left >= sheet.left && wholeTrip.right <= sheet.right)
        compose.onNodeWithTag("itinerary-scope-day-1").performClick()
        assertEquals(TripWorkspaceAction.SelectItineraryScope(ItineraryScope.Day("day-1")), actions.last())
    }

    @Test fun placePoolListScrollsAndKeepsSecondCardAboveBottomInset() {
        val places = (1..8).map { index ->
            com.yangchengwei.easytrip.place.domain.SavedPlace("$index", "trip", "poi-$index", "地点 $index", "地址 $index", com.yangchengwei.easytrip.core.model.GeoPoint(39.9 + index / 1000.0, 116.4), "", emptyList())
        }
        setContent(
            ready(),
            WorkspaceMapState.Ready,
            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(rows = places.map { com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(it, 0, false) }),
        )
        compose.waitForIdle()

        compose.onNodeWithTag("workspace-place-list").performScrollToNode(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "saved-place-8"),
        )
        compose.waitForIdle()
        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val visibleCard = compose.onNodeWithTag("saved-place-8").getUnclippedBoundsInRoot()
        assertTrue("root=$root card=$visibleCard", visibleCard.bottom <= root.bottom - 24.dp)
    }

    private fun assertNoOverlap(
        label: String,
        first: androidx.compose.ui.unit.DpRect,
        second: androidx.compose.ui.unit.DpRect,
    ) {
        val overlaps = first.left < second.right && first.right > second.left &&
            first.top < second.bottom && first.bottom > second.top
        assertTrue("$label first=$first second=$second", !overlaps)
    }

    private fun assertContainedBy(
        label: String,
        child: androidx.compose.ui.unit.DpRect,
        parent: androidx.compose.ui.unit.DpRect,
    ) {
        assertTrue(
            "$label child=$child parent=$parent",
            child.left >= parent.left && child.right <= parent.right &&
                child.top >= parent.top && child.bottom <= parent.bottom,
        )
    }

    private fun setContentForLevel(level: androidx.compose.runtime.State<WorkspaceSheetLevel>) {
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = ready(level.value),
                    mapState = WorkspaceMapState.Ready,
                    onAction = {},
                    placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { _ -> Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                )
            }
        }
    }

    private fun setContent(
        page: TripWorkspacePageState,
        map: WorkspaceMapState,
        onAction: (TripWorkspaceAction) -> Unit = {},
        onMapRetry: () -> Unit = { onAction(TripWorkspaceAction.Retry) },
        placeState: com.yangchengwei.easytrip.place.ui.PlacePoolUiState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
        searchReturn: WorkspaceSearchReturn? = null,
    ) {
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = page,
                    mapState = map,
                    onAction = onAction,
                    onMapRetry = onMapRetry,
                    placeState = placeState,
                    onPlaceAction = {},
                    itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { _ -> Text("地图就绪") },
                    dayItineraryContent = { Text("单日内容", Modifier.testTag("day-content")) },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    searchReturn = searchReturn,
                )
            }
        }
    }

    private fun itineraryReady(level: WorkspaceSheetLevel, scope: ItineraryScope) = TripWorkspacePageState.Ready(
        TripWorkspaceUiState(
            tripName = "北京",
            days = listOf(TripDay("day-1", 0)),
            section = WorkspaceSection.ITINERARY,
            itineraryScope = scope,
            mapScope = WorkspaceSection.ITINERARY.toMapScope(scope),
            selectedDayId = scope.selectedDayId(),
            wholeTripDays = listOf(WholeTripDayUi("day-1", 1, emptyList(), emptyList())),
            sheetLevel = level,
        ).toReadyState(),
    )

    private fun ready(level: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF) = TripWorkspacePageState.Ready(
        TripWorkspaceUiState(tripName = "北京", sheetLevel = level).toReadyState(),
    )
}
