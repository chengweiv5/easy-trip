package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Rule
import org.junit.Test

class WorkspaceChromeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun topBarKeepsActionsVisibleWithLongTripName() {
        compose.setContent {
            EasyTripTheme {
                WorkspaceTopBar(
                    title = "这是一个非常非常长但仍然不能挤掉返回和更多按钮的旅行名称",
                    dateLabel = "8月23日 — 8月25日",
                    onBack = {},
                    onMore = {},
                )
            }
        }

        compose.onNodeWithTag("workspace-top-bar").assertHeightIsEqualTo(42.dp)
        compose.onNodeWithTag("workspace-back").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("workspace-more").assertIsDisplayed().assertHasClickAction()
        val title = compose.onNodeWithTag("workspace-trip-title").assertIsDisplayed().getUnclippedBoundsInRoot()
        val date = compose.onNodeWithText("8月23日 — 8月25日").assertIsDisplayed().getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("title=$title date=$date", title.right <= date.left)
        org.junit.Assert.assertTrue("title=$title date=$date", title.top <= date.top && title.bottom >= date.bottom)
    }

    @Test fun workspaceChromeMatchesCompactTopAndFiveControlGeometry() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(
                            TripWorkspaceUiState(
                                tripName = "北京",
                                sheetLevel = WorkspaceSheetLevel.HALF,
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

        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val topBar = compose.onNodeWithTag("workspace-top-bar").getUnclippedBoundsInRoot()
        val layer = compose.onNodeWithTag("layer-menu").getUnclippedBoundsInRoot()
        val zoomIn = compose.onNodeWithTag("zoom-in").getUnclippedBoundsInRoot()
        val zoomOut = compose.onNodeWithTag("zoom-out").getUnclippedBoundsInRoot()
        val locate = compose.onNodeWithTag("workspace-locate").getUnclippedBoundsInRoot()
        val search = compose.onNodeWithTag("workspace-search-control").getUnclippedBoundsInRoot()

        org.junit.Assert.assertEquals(12.dp, topBar.left - root.left)
        org.junit.Assert.assertEquals(42.dp, topBar.height)
        org.junit.Assert.assertEquals(12.dp, root.right - topBar.right)
        org.junit.Assert.assertEquals(12.dp, root.right - search.right)
        listOf(layer, zoomIn, zoomOut, locate, search).forEach { bounds ->
            org.junit.Assert.assertEquals(28.dp, bounds.width)
            org.junit.Assert.assertEquals(28.dp, bounds.height)
        }
        org.junit.Assert.assertEquals(5.dp, zoomIn.top - layer.bottom)
        org.junit.Assert.assertEquals(5.dp, zoomOut.top - zoomIn.bottom)
        org.junit.Assert.assertEquals(5.dp, locate.top - zoomOut.bottom)
        org.junit.Assert.assertEquals(5.dp, search.top - locate.bottom)
    }

    @Test fun fifthMapControlDispatchesOpenSearch() {
        val actions = mutableListOf<TripWorkspaceAction>()
        compose.setContent {
            EasyTripTheme {
                MapControls(
                    active = false,
                    onOpenLayerMenu = {},
                    onOpenSearch = { actions += TripWorkspaceAction.OpenSearch },
                )
            }
        }

        compose.onNodeWithTag("workspace-search-control").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(listOf(TripWorkspaceAction.OpenSearch), actions)
        }
    }

    @Test fun moreMenuShowsThreeActionsAndClosesOnOutsideTap() {
        val actions = mutableListOf<TripWorkspaceAction>()
        var overlay by mutableStateOf<WorkspaceOverlay>(WorkspaceOverlay.None)
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(
                            TripWorkspaceUiState(
                                tripName = "北京",
                                sheetLevel = WorkspaceSheetLevel.HALF,
                                overlay = overlay,
                            ).toReadyState(),
                        ),
                        mapState = WorkspaceMapState.Ready,
                        onAction = { action ->
                            actions += action
                            overlay = when (action) {
                                is TripWorkspaceAction.OpenOverlay -> action.overlay
                                TripWorkspaceAction.CloseOverlay -> WorkspaceOverlay.None
                                else -> overlay
                            }
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
        }

        compose.onNodeWithTag("workspace-more").performClick()
        compose.onNodeWithTag("more-menu-panel").assertIsDisplayed()
        listOf("旅行设置", "地图授权", "返回我的旅行").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        listOf("修改名称、日期和旅行日", "管理高德地图权限", "回到旅行列表").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        val panel = compose.onNodeWithTag("more-menu-panel").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue(panel.right - panel.left >= 180.dp && panel.right - panel.left <= 210.dp)
        org.junit.Assert.assertTrue(panel.bottom - panel.top >= 170.dp)
        compose.onNodeWithTag("more-menu-scrim").performClick()
        org.junit.Assert.assertEquals(
            listOf(
                TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.MoreMenu),
                TripWorkspaceAction.CloseOverlay,
            ),
            actions,
        )
    }

    @Test fun moreMenuActionsDispatchRoutesWithoutReplacingOverlayModel() {
        val actions = mutableListOf<TripWorkspaceAction>()
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(
                            TripWorkspaceUiState(
                                tripName = "北京",
                                sheetLevel = WorkspaceSheetLevel.HALF,
                                overlay = WorkspaceOverlay.MoreMenu,
                            ).toReadyState(),
                        ),
                        mapState = WorkspaceMapState.ConsentRequired,
                        onAction = actions::add,
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

        compose.onNodeWithTag("more-menu-settings").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(
                listOf(TripWorkspaceAction.CloseOverlay, TripWorkspaceAction.OpenSettings),
                actions,
            )
        }
    }

    @Test fun moreMenuKeepsThirdActionReachableAtTwoTimesFontScale() {
        var backCalls = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                EasyTripTheme {
                    Box(Modifier.fillMaxWidth().requiredHeight(360.dp)) {
                        WorkspaceMoreMenu(
                            onOpenSettings = {},
                            onOpenConsent = {},
                            onBackToTrips = { backCalls++ },
                        )
                    }
                }
            }
        }

        val panel = compose.onNodeWithTag("more-menu-panel").getUnclippedBoundsInRoot()
        val action = compose.onNodeWithTag("more-menu-back-to-trips")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(52.dp)
            .getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("panel=$panel action=$action", action.top >= panel.top && action.bottom <= panel.bottom)
        compose.onNodeWithTag("more-menu-back-to-trips").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, backCalls) }
    }

    @Test fun moreMenuDoesNotCoverAnotherOverlay() {
        val actions = mutableListOf<TripWorkspaceAction>()
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(
                            TripWorkspaceUiState(
                                tripName = "北京",
                                sheetLevel = WorkspaceSheetLevel.HALF,
                                overlay = WorkspaceOverlay.Confirmation(
                                    com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel(
                                        title = "保存中",
                                        message = "请稍候",
                                        deletedItems = emptyList(),
                                        retainedItems = emptyList(),
                                        confirmLabel = "确认",
                                        dismissLabel = "取消",
                                        destructive = false,
                                        reversible = false,
                                    ),
                                ),
                            ).toReadyState(),
                        ),
                        mapState = WorkspaceMapState.Ready,
                        onAction = actions::add,
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

        compose.onNodeWithTag("workspace-more").performClick()
        compose.onNodeWithTag("more-menu-panel").assertDoesNotExist()
        org.junit.Assert.assertTrue(actions.isEmpty())
    }

    @Test fun collapsedSheetPreservesHandleTabsSummaryAndExpandsOnDrag() {
        var level by mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        compose.setContent {
            EasyTripTheme {
                WorkspaceBottomSheet(
                    value = level,
                    anchors = WorkspaceSheetAnchors(collapsed = 108.dp, half = 432.dp, expanded = 720.dp),
                    onValueChange = { level = it },
                    dragOffsetPx = 0f,
                    onDragOffsetChange = {},
                    header = {
                        androidx.compose.foundation.layout.Column {
                            WorkspaceSheetHandle()
                            WorkspaceTabs(WorkspaceSection.PLACE_POOL, {})
                        }
                    },
                    content = { androidx.compose.material3.Text("业务列表", Modifier.testTag("business-list")) },
                    collapsedContent = { androidx.compose.material3.Text("已收藏 2 个地点", Modifier.testTag("sheet-summary")) },
                )
            }
        }

        compose.onNodeWithTag("workspace-sheet-handle")
            .assertHeightIsEqualTo(60.dp)
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithTag("sheet-summary").assertIsDisplayed()
        compose.onAllNodesWithTag("business-list").assertCountEquals(0)
        compose.onNodeWithTag("workspace-sheet-handle").performTouchInput { swipeUp() }
        compose.runOnIdle { org.junit.Assert.assertEquals(WorkspaceSheetLevel.HALF, level) }
    }

    @Test fun designAnchorsRenderThreeDistinctHeights() {
        var level by mutableStateOf(WorkspaceSheetLevel.COLLAPSED)
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Box(Modifier.requiredHeight(782.dp)) {
                    WorkspaceBottomSheet(
                        value = level,
                        anchors = WorkspaceSheetAnchors(collapsed = 108.dp, half = 432.dp, expanded = 720.dp),
                        onValueChange = { level = it },
                        dragOffsetPx = 0f,
                        onDragOffsetChange = {},
                        header = { WorkspaceSheetHandle() },
                        content = {},
                    )
                }
            }
        }

        val heights = mutableListOf<androidx.compose.ui.unit.Dp>()
        WorkspaceSheetLevel.entries.forEach { next ->
            compose.runOnIdle { level = next }
            heights += compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().height
        }
        org.junit.Assert.assertEquals(108.dp, heights[0])
        org.junit.Assert.assertEquals(432.dp, heights[1])
        org.junit.Assert.assertEquals(720.dp, heights[2])
    }

    @Test fun expandedSheetKeepsHandleTabsAndContentInSeparateVerticalSpace() {
        compose.setContent {
            EasyTripTheme {
                WorkspaceBottomSheet(
                    value = WorkspaceSheetLevel.HALF,
                    anchors = WorkspaceSheetAnchors(collapsed = 34.dp, half = 160.dp, expanded = 280.dp),
                    onValueChange = {},
                    dragOffsetPx = 0f,
                    onDragOffsetChange = {},
                    header = {
                        androidx.compose.foundation.layout.Column {
                            WorkspaceSheetHandle()
                            WorkspaceTabs(WorkspaceSection.PLACE_POOL, {})
                        }
                    },
                    content = { androidx.compose.material3.Text("内容", Modifier.testTag("sheet-content")) },
                )
            }
        }

        val tabs = compose.onNodeWithTag("workspace-tabs").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("sheet-content").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("tabs=$tabs content=$content", tabs.bottom <= content.top)
    }

    @Test fun smallWindowCollapsedSheetKeepsFullHeaderAndSummaryVisible() {
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Box(Modifier.requiredHeight(280.dp)) {
                    val anchors = workspaceSheetAnchors(280.dp)
                    WorkspaceBottomSheet(
                        value = WorkspaceSheetLevel.COLLAPSED,
                        anchors = anchors,
                        onValueChange = {},
                        dragOffsetPx = 0f,
                        onDragOffsetChange = {},
                        header = {
                            androidx.compose.foundation.layout.Column {
                                WorkspaceSheetHandle()
                                WorkspaceTabs(WorkspaceSection.PLACE_POOL, {})
                            }
                        },
                        content = {},
                        collapsedContent = { androidx.compose.material3.Text("摘要", Modifier.testTag("small-window-summary")) },
                    )
                }
            }
        }

        compose.onNodeWithTag("workspace-sheet").assertHeightIsEqualTo(96.dp)
        compose.onNodeWithTag("workspace-sheet-handle").assertHeightIsEqualTo(60.dp)
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithTag("small-window-summary").assertIsDisplayed()
    }

    @Test fun minimumSupportedWindowKeepsCollapsedSummaryVisible() {
        compose.setContent {
            EasyTripTheme {
                androidx.compose.foundation.layout.Box(Modifier.requiredHeight(92.dp)) {
                    WorkspaceBottomSheet(
                        value = WorkspaceSheetLevel.COLLAPSED,
                        anchors = workspaceSheetAnchors(92.dp),
                        onValueChange = {},
                        dragOffsetPx = 0f,
                        onDragOffsetChange = {},
                        header = {
                            androidx.compose.foundation.layout.Column {
                                WorkspaceSheetHandle()
                                WorkspaceTabs(WorkspaceSection.PLACE_POOL, {})
                            }
                        },
                        content = {},
                        collapsedContent = { androidx.compose.material3.Text("摘要", Modifier.testTag("minimum-window-summary")) },
                    )
                }
            }
        }

        val sheet = compose.onNodeWithTag("workspace-sheet").assertHeightIsEqualTo(86.dp).getUnclippedBoundsInRoot()
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        val summary = compose.onNodeWithTag("minimum-window-summary").assertIsDisplayed().getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("sheet=$sheet summary=$summary", summary.top >= sheet.top && summary.bottom <= sheet.bottom)
    }

    @Test fun workspaceTabsUse36DpTouchHeightAndThreeDpIndicator() {
        compose.setContent {
            EasyTripTheme {
                WorkspaceTabs(WorkspaceSection.PLACE_POOL, {})
            }
        }

        compose.onNodeWithTag("section-PLACE_POOL").assertHeightIsEqualTo(36.dp)
        compose.onNodeWithTag("workspace-tab-indicator-PLACE_POOL", useUnmergedTree = true).assertHeightIsEqualTo(3.dp)
    }

    @Test fun layerMenuScrimBlocksBottomSheetAndOutsideTapClosesOnlyOverlay() {
        val actions = mutableListOf<TripWorkspaceAction>()
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
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

        compose.onNodeWithTag("layer-menu-scrim").assertIsDisplayed()
        val shield = compose.onNodeWithTag("layer-menu-hit-shield").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("shield=$shield sheet=$sheet", shield.bottom >= sheet.bottom)
        compose.onNodeWithTag("workspace-root").performTouchInput { click(Offset(32f, 450f)) }
        org.junit.Assert.assertEquals(listOf(TripWorkspaceAction.CloseOverlay), actions)
    }

    @Test fun layerMenuHasThreeRadioOptionsAndStaysInsideWorkspaceBounds() {
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
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

        compose.onNodeWithTag("layer-menu-panel").assertHeightIsEqualTo(289.dp)
        listOf("STANDARD", "SATELLITE", "SATELLITE_ROAD").forEach { option ->
            compose.onNodeWithTag("layer-$option").assertIsDisplayed().assertHeightIsEqualTo(62.dp)
        }
        compose.onNodeWithText("标准地图").assertIsDisplayed()
        compose.onNodeWithText("卫星地图").assertIsDisplayed()
        compose.onNodeWithText("卫星路网").assertIsDisplayed()
        compose.onNodeWithText("选择将应用到所有旅行，并在下次打开时保留。").assertIsDisplayed()
        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val panel = compose.onNodeWithTag("layer-menu-panel").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("root=$root panel=$panel", panel.left >= root.left && panel.right <= root.right && panel.top >= root.top && panel.bottom <= root.bottom)
    }

    @Test fun layerMenuPickerOptionDispatchesSelectThenCloseExactlyOnce() {
        val actions = mutableListOf<TripWorkspaceAction>()
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(TripWorkspaceUiState(tripName = "北京", sheetLevel = WorkspaceSheetLevel.HALF, overlay = WorkspaceOverlay.LayerMenu).toReadyState()),
                        mapState = WorkspaceMapState.Ready,
                        onAction = actions::add,
                        placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(), onPlaceAction = {},
                        itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(), onItineraryAction = {},
                        mapContent = { _ -> Text("地图就绪") }, modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("layer-SATELLITE").performClick()
        org.junit.Assert.assertEquals(listOf(TripWorkspaceAction.SelectMapLayer(MapLayer.SATELLITE), TripWorkspaceAction.CloseOverlay), actions)
    }

    @Test fun layerMenuPickerBlankCoordinateDoesNotClose() {
        val actions = mutableListOf<TripWorkspaceAction>()
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(TripWorkspaceUiState(tripName = "北京", sheetLevel = WorkspaceSheetLevel.HALF, overlay = WorkspaceOverlay.LayerMenu).toReadyState()),
                        mapState = WorkspaceMapState.Ready,
                        onAction = actions::add,
                        placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(), onPlaceAction = {},
                        itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(), onItineraryAction = {},
                        mapContent = { _ -> Text("地图就绪") }, modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    )
                }
            }
        }
        compose.waitForIdle()
        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val panel = compose.onNodeWithTag("layer-menu-panel").getUnclippedBoundsInRoot()
        val blankInsidePanel = with(compose.density) {
            Offset((panel.right - root.left).toPx() - 4f, (panel.bottom - root.top).toPx() - 4f)
        }
        compose.onNodeWithTag("workspace-root").performTouchInput { click(blankInsidePanel) }
        org.junit.Assert.assertTrue(actions.isEmpty())
    }

    @Test fun layerMenuRootCoordinatesBlockUnderlyingWorkspaceInteractions() {
        val actions = mutableListOf<TripWorkspaceAction>()
        var mapClicks = 0
        compose.setContent {
            EasyTripTheme {
                Box(Modifier.fillMaxWidth().requiredHeight(844.dp)) {
                    TripWorkspaceContent(
                        pageState = TripWorkspacePageState.Ready(TripWorkspaceUiState(tripName = "北京", sheetLevel = WorkspaceSheetLevel.HALF, overlay = WorkspaceOverlay.LayerMenu).toReadyState()),
                        mapState = WorkspaceMapState.Ready,
                        onAction = actions::add,
                        placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(), onPlaceAction = {},
                        itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(), onItineraryAction = {},
                        mapContent = { _ -> Box(Modifier.fillMaxSize().clickable { mapClicks++ }) },
                        modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    )
                }
            }
        }
        compose.waitForIdle()

        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        fun clickRootAt(tag: String) {
            val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            compose.onNodeWithTag("workspace-root").performTouchInput {
                click(with(compose.density) {
                    Offset(
                        ((bounds.left + bounds.right) / 2 - root.left).toPx(),
                        ((bounds.top + bounds.bottom) / 2 - root.top).toPx(),
                    )
                })
            }
        }

        listOf(
            "workspace-search-control" to TripWorkspaceAction.OpenSearch,
            "workspace-locate" to TripWorkspaceAction.Locate,
            "layer-menu" to TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.LayerMenu),
            "section-PLACE_POOL" to TripWorkspaceAction.SelectSection(WorkspaceSection.PLACE_POOL),
        ).forEach { (tag, forbiddenAction) ->
            actions.clear()
            clickRootAt(tag)
            org.junit.Assert.assertFalse("$tag leaked $actions", forbiddenAction in actions)
        }

        actions.clear()
        compose.onNodeWithTag("workspace-root").performTouchInput { click(Offset(24f, 260f)) }
        org.junit.Assert.assertEquals(0, mapClicks)

        actions.clear()
        val handle = compose.onNodeWithTag("workspace-sheet-handle").getUnclippedBoundsInRoot()
        val start = with(compose.density) {
            Offset(
                (((handle.left + handle.right) / 2) - root.left).toPx(),
                (((handle.top + handle.bottom) / 2) - root.top).toPx(),
            )
        }
        compose.onNodeWithTag("workspace-root").performTouchInput {
            swipe(start = start, end = start - Offset(0f, 180f), durationMillis = 300)
        }
        org.junit.Assert.assertTrue(actions.none { it is TripWorkspaceAction.SetSheetLevel })
    }

    @Test fun mapControlsUseIconsWithoutPlaceholderActionText() {
        compose.setContent {
            EasyTripTheme {
                MapControls(
                    active = false,
                    onOpenLayerMenu = {},
                    onLocate = {},
                )
            }
        }

        compose.onNodeWithContentDescription("定位").assertHasClickAction()
        compose.onNodeWithText("定位").assertDoesNotExist()
        compose.onNodeWithText("关闭").assertDoesNotExist()
        compose.onNodeWithText("✓ 标准").assertDoesNotExist()
    }
}
