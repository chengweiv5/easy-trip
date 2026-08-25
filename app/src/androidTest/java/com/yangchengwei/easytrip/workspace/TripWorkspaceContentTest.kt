package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
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

    @Test fun consentRequiredKeepsLocalContentAndControls() {
        setContent(ready(), WorkspaceMapState.ConsentRequired)
        compose.onNodeWithText("同意高德隐私政策后显示地图").assertIsDisplayed()
        compose.onNodeWithText("地点池").assertIsDisplayed()
        compose.onNodeWithText("还没有收藏地点").assertExists()
        compose.onNodeWithTag("workspace-more").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("workspace-back").assertIsDisplayed().assertHasClickAction()
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
        val retry = compose.onNodeWithTag("workspace-map-retry")
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

    @Test fun readyKeepsSearchSettingsBackAndItineraryActions() {
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
                TripWorkspaceAction.OpenSettings,
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

    @Test fun searchTracksSheetTopDuringActiveDrag() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()
        val initialSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        val initialSearchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom

        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y - 80f))
            advanceEventTime(100)
        }
        compose.waitForIdle()
        val draggedSheetTop = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().top
        val draggedSearchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom
        assertTrue("initial=$initialSheetTop dragged=$draggedSheetTop", draggedSheetTop < initialSheetTop)
        assertTrue(
            "initialSearch=$initialSearchBottom draggedSearch=$draggedSearchBottom sheet=$draggedSheetTop",
            draggedSearchBottom < initialSearchBottom && draggedSearchBottom <= draggedSheetTop,
        )
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
                    mapContent = { Text("地图就绪") },
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
        val expandedSearchBottom = compose.onNodeWithTag("workspace-search-launcher").getUnclippedBoundsInRoot().bottom
        assertTrue("half=$initialSheetTop expanded=$expandedSheetTop", expandedSheetTop < initialSheetTop)
        assertTrue(
            "half=$initialSearchBottom expanded=$expandedSearchBottom sheet=$expandedSheetTop",
            expandedSearchBottom < initialSearchBottom && expandedSearchBottom <= expandedSheetTop,
        )
    }

    @Test fun activeGestureUsesLatestLevelChangeCallback() {
        val calls = mutableListOf<Int>()
        val callback = mutableStateOf<(WorkspaceSheetLevel) -> Unit>({ calls += 0 })
        compose.setContent {
            EasyTripTheme {
                WorkspaceScaffold(
                    sheetLevel = WorkspaceSheetLevel.HALF,
                    searchReturn = false,
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

    @Test fun overlayPositionsMoveBetweenHalfAndExpandedLevels() {
        val level = mutableStateOf(WorkspaceSheetLevel.HALF)
        setContentForLevel(level)
        compose.waitForIdle()
        val halfPositions = listOf("workspace-search-launcher", "map-legend", "workspace-locate").associateWith { tag ->
            compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
        }

        compose.runOnIdle { level.value = WorkspaceSheetLevel.EXPANDED }
        compose.waitForIdle()
        val expandedSheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        halfPositions.forEach { (tag, halfBottom) ->
            val expandedBottom = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom
            assertTrue("tag=$tag half=$halfBottom expanded=$expandedBottom", expandedBottom < halfBottom)
            assertTrue("tag=$tag expanded=$expandedBottom sheet=$expandedSheet", expandedBottom <= expandedSheet.top)
        }
    }

    @Test fun mapFailureRetryRemainsAboveSheet() {
        setContent(ready(), WorkspaceMapState.Failed("地图加载失败"))
        compose.waitForIdle()

        val retry = compose.onNodeWithTag("workspace-map-retry").getUnclippedBoundsInRoot()
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

    @Test fun collapsedSheetKeepsHandleAndHidesBusinessContent() {
        setContent(ready(WorkspaceSheetLevel.COLLAPSED), WorkspaceMapState.Ready)

        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed()
        compose.onAllNodesWithText("还没有收藏地点").assertCountEquals(0)
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

        val anchors = workspaceSheetAnchors(root.height, searchReturn = false)
        assertTrue(
            "root=$root sheet=$sheet anchors=$anchors",
            kotlin.math.abs((sheet.height - anchors.half).value) <= 1f,
        )
        assertTrue("root=$root sheet=$sheet", sheet.bottom <= root.bottom)
        assertTrue("root=$root topBar=$topBar", topBar.top >= root.top)
        assertTrue("root=$root topBar=$topBar", topBar.top - root.top <= 48.dp)
    }

    @Test fun constrainedHeightKeepsPlacePoolReachableAtSmallWindowAndLargeFont() {
        val places = (1..8).map { index ->
            com.yangchengwei.easytrip.place.domain.SavedPlace("$index", "trip", "poi-$index", "地点 $index", "地址", com.yangchengwei.easytrip.core.model.GeoPoint(39.9, 116.4), "", emptyList())
        }
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 2f)) {
                EasyTripTheme {
                    androidx.compose.foundation.layout.Box(Modifier.height(280.dp).testTag("small-window")) {
                        TripWorkspaceContent(
                            pageState = ready(),
                            mapState = WorkspaceMapState.Ready,
                            onAction = {},
                            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(rows = places.map { com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(it, 0, false) }),
                            onPlaceAction = {},
                            itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                            onItineraryAction = {},
                            mapContent = { Text("地图就绪") },
                            modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("workspace-place-list").performScrollToNode(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "saved-place-8"),
        )
        compose.onNodeWithTag("saved-place-8").assertIsDisplayed()
    }

    @Test fun searchReturnSheetIsRaisedAndHighlightsRecentCollections() {
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
                    mapContent = { Text("地图就绪") },
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
        val anchors = workspaceSheetAnchors(root.height, searchReturn = true)
        assertTrue(
            "root=$root regular=$regularHalfHeight returned=$returnedSheet anchors=$anchors",
            returnedSheet.height > regularHalfHeight &&
                kotlin.math.abs((returnedSheet.height - anchors.half).value) <= 1f,
        )
        compose.onNodeWithText("刚刚收藏 · 待安排行程").assertExists()
        compose.onNodeWithTag("workspace-place-list").performScrollToNode(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "saved-place-old"),
        )
        compose.onNodeWithText("原收藏").assertExists()
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
                    mapContent = { Text("地图就绪") },
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
                    mapContent = { Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    searchReturn = searchReturn,
                )
            }
        }
    }

    private fun ready(level: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF) = TripWorkspacePageState.Ready(
        TripWorkspaceUiState(tripName = "北京", sheetLevel = level).toReadyState(),
    )
}
