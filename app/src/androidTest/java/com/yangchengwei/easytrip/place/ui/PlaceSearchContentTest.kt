package com.yangchengwei.easytrip.place.ui

import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapPoiUi
import com.yangchengwei.easytrip.workspace.MapUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaceSearchContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun resultRowOpensDetailWhileBookmarkOnlyTogglesCollection() {
        val candidate = PlaceCandidate("poi-1", "故宫博物院", "北京市东城区景山前街4号", GeoPoint(39.916, 116.397), "010")
        val actions = mutableListOf<PlaceSearchAction>()
        setContent(
            PlaceSearchUiState(search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results)),
            onAction = actions::add,
        )

        compose.onNodeWithContentDescription("查看故宫博物院详情").performClick()
        compose.onNodeWithContentDescription("收藏故宫博物院").performClick()

        assertEquals(
            listOf(PlaceSearchAction.OpenDetail("poi-1"), PlaceSearchAction.ToggleCollection("poi-1")),
            actions,
        )
    }

    @Test fun rowAndBookmarkHaveIndependentButtonSemantics() {
        val candidate = PlaceCandidate("poi-1", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        setContent(PlaceSearchUiState(search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results)))

        compose.onNodeWithContentDescription("查看故宫博物院详情").assertHasClickAction()
        compose.onNodeWithContentDescription("收藏故宫博物院").assertHasClickAction()
        assertMinimumTouchSize("place-search-bookmark-touch-poi-1", 48f)
    }

    @Test fun detailBackRestoresQueryResultsAndListPosition() {
        val candidates = (0..30).map {
            PlaceCandidate("poi-$it", "地点$it", "地址$it", GeoPoint(39.916 + it, 116.397), "010")
        }
        val state = mutableStateOf(
            PlaceSearchUiState(search = PlaceSearchState("故宫", candidates, phase = PlaceSearchPhase.Results)),
        )
        val listState = LazyListState(firstVisibleItemIndex = 18, firstVisibleItemScrollOffset = 7)
        compose.setContent {
            EasyTripTheme {
                PlaceSearchContent(
                    state = state.value,
                    onAction = {},
                    resultsListState = listState,
                    detailContent = { Text("详情：${it.name}") },
                )
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            state.value = state.value.copy(displayMode = SearchDisplayMode.MapDetail("poi-18"))
        }
        compose.onNodeWithText("详情：地点18").assertIsDisplayed()
        compose.runOnIdle {
            state.value = state.value.copy(displayMode = SearchDisplayMode.Results)
        }
        compose.onNodeWithText("地点18").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("故宫", state.value.search.query)
            assertEquals(18, listState.firstVisibleItemIndex)
            assertEquals(7, listState.firstVisibleItemScrollOffset)
        }
    }

    @Test fun candidateWithoutCoordinatesUsesProductionFallbackAndDispatchesCollection() {
        val candidate = PlaceCandidate("poi-no-point", "未知地点", "地址暂不可用", null, "010")
        val state = mutableStateOf(
            PlaceSearchUiState(search = PlaceSearchState("未知", listOf(candidate), phase = PlaceSearchPhase.Results)),
        )
        val actions = mutableListOf<PlaceSearchAction>()
        compose.setContent {
            EasyTripTheme {
                PlaceSearchContent(
                    state = state.value,
                    onAction = { action ->
                        actions += action
                        if (action is PlaceSearchAction.OpenDetail) {
                            state.value = state.value.copy(displayMode = SearchDisplayMode.MapDetail(action.poiId))
                        }
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("查看未知地点详情").performClick()
        compose.onNodeWithText("未知地点").assertIsDisplayed()
        compose.onNodeWithContentDescription("收藏未知地点").performClick()

        assertEquals(
            listOf(
                PlaceSearchAction.OpenDetail("poi-no-point"),
                PlaceSearchAction.ToggleCollection("poi-no-point"),
            ),
            actions,
        )
        compose.onNodeWithTag("place-search-detail-map").assertDoesNotExist()
    }

    @Test fun resultAndDetailExposeNoAddToItineraryAction() {
        val candidate = PlaceCandidate("poi-1", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        val state = mutableStateOf(
            PlaceSearchUiState(search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results)),
        )
        compose.setContent {
            EasyTripTheme {
                PlaceSearchContent(
                    state = state.value,
                    onAction = {},
                    detailContent = { Text("详情：${it.name}") },
                )
            }
        }
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)

        compose.runOnIdle {
            state.value = state.value.copy(displayMode = SearchDisplayMode.MapDetail("poi-1"))
        }
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
    }

    @Test fun searchDetailRendersMapAndRecenterWithoutChangingSelection() {
        val candidate = PlaceCandidate("poi-map", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val actions = mutableListOf<PlaceSearchAction>()
        var renderedModel: MapUiModel? = null
        var markerClicks = 0
        var poiClicks = 0
        val host = object : AmapMapHost {
            override val view: View = View(context)
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
            ) {
                renderedModel = model
                onMarkerClick("search-other")
                markerClicks++
                onMapPoiClick(MapPoiUi("other", "其他地点", "", GeoPoint(1.0, 2.0)))
                poiClicks++
            }
        }
        setContent(
            state = PlaceSearchUiState(
                search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results),
                displayMode = SearchDisplayMode.MapDetail(candidate.poiId),
                detailMapRequestId = 4L,
            ),
            onAction = actions::add,
            consent = token,
            mapHostFactory = { host },
        )

        compose.waitUntil(5_000) { renderedModel != null }
        compose.onNodeWithTag("place-search-detail-map").assertIsDisplayed()
        compose.onNodeWithText("详情：故宫博物院").assertIsDisplayed()
        compose.onNodeWithContentDescription("回到故宫博物院").performClick()

        compose.runOnIdle {
            assertEquals("search-poi-map", renderedModel?.markers?.single()?.key)
            assertEquals(4L, renderedModel?.viewportRequest?.id)
            assertEquals(listOf(PlaceSearchAction.RecenterDetail), actions)
            assertTrue(markerClicks > 0)
            assertTrue(poiClicks > 0)
        }
    }

    @Test fun missingConsentKeepsDefaultDetailCollectionActionAvailable() {
        val candidate = PlaceCandidate("poi-no-consent", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        var action: PlaceSearchAction? = null
        compose.setContent {
            EasyTripTheme {
                PlaceSearchContent(
                    state = PlaceSearchUiState(
                        search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results),
                        displayMode = SearchDisplayMode.MapDetail(candidate.poiId),
                    ),
                    onAction = { action = it },
                    consent = null,
                )
            }
        }

        compose.onNodeWithText("故宫博物院").assertIsDisplayed()
        compose.onNodeWithContentDescription("收藏故宫博物院").performClick()
        assertEquals(PlaceSearchAction.ToggleCollection("poi-no-consent"), action)
    }

    @Test fun missingConsentOrCoordinatesKeepsDetailActionsAvailable() {
        val noPoint = PlaceCandidate("poi-no-point", "未知地点", "地址", null, "010")
        setContent(
            state = PlaceSearchUiState(
                search = PlaceSearchState("未知", listOf(noPoint), phase = PlaceSearchPhase.Results),
                displayMode = SearchDisplayMode.MapDetail(noPoint.poiId),
            ),
            consent = null,
        )

        compose.onNodeWithText("详情：未知地点").assertIsDisplayed()
        compose.onNodeWithTag("place-search-detail-map").assertDoesNotExist()
    }

    @Test fun productionFallbackDisablesBusyCollectionAndShowsCurrentDetailError() {
        val current = PlaceCandidate("poi-current", "当前地点", "地址", null, "010")
        val other = PlaceCandidate("poi-other", "其他地点", "地址", GeoPoint(39.916, 116.397), "010")
        val actions = mutableListOf<PlaceSearchAction>()
        setContent(
            state = PlaceSearchUiState(
                search = PlaceSearchState("地点", listOf(current, other), phase = PlaceSearchPhase.Results),
                displayMode = SearchDisplayMode.MapDetail(current.poiId),
                collectionBusyPoiIds = setOf(current.poiId),
                collectionError = "无法收藏缺少坐标的地点",
                collectionErrorPoiId = current.poiId,
            ),
            onAction = actions::add,
            detailContent = null,
        )

        compose.onNodeWithContentDescription("收藏当前地点").assertIsNotEnabled().performClick()
        compose.onNodeWithText("无法收藏缺少坐标的地点").assertIsDisplayed()
        assertEquals(emptyList<PlaceSearchAction>(), actions)
    }

    @Test fun productionFallbackDoesNotShowCollectionErrorWhileAnotherPoiIsBusy() {
        val current = PlaceCandidate("poi-current", "当前地点", "地址", GeoPoint(39.916, 116.397), "010")
        val other = PlaceCandidate("poi-other", "其他地点", "地址", GeoPoint(39.917, 116.398), "010")
        setContent(
            state = PlaceSearchUiState(
                search = PlaceSearchState("地点", listOf(current, other), phase = PlaceSearchPhase.Results),
                displayMode = SearchDisplayMode.MapDetail(current.poiId),
                collectionBusyPoiIds = setOf(other.poiId),
                collectionError = "其他地点收藏失败",
                collectionErrorPoiId = other.poiId,
            ),
            detailContent = null,
        )

        compose.onNodeWithContentDescription("收藏当前地点").assertHasClickAction()
        compose.onAllNodesWithText("其他地点收藏失败").assertCountEquals(0)
    }

    @Test fun mapHostCreationFailureKeepsProductionFallbackCollectionActionAvailable() {
        val candidate = PlaceCandidate("poi-map", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val gate = AmapPrivacyGate.create(context)
        gate.reportPrivacyShown()
        val token = requireNotNull(gate.reportUserDecision(true))
        val actions = mutableListOf<PlaceSearchAction>()
        setContent(
            state = PlaceSearchUiState(
                search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results),
                displayMode = SearchDisplayMode.MapDetail(candidate.poiId),
            ),
            onAction = actions::add,
            detailContent = null,
            consent = token,
            mapHostFactory = { error("map unavailable") },
        )

        compose.onNodeWithText("故宫博物院").assertIsDisplayed()
        compose.onNodeWithContentDescription("收藏故宫博物院").performClick()
        assertEquals(listOf(PlaceSearchAction.ToggleCollection("poi-map")), actions)
    }

    @Test fun loadingEmptyAndFailureMatchTheirActions() {
        val state = mutableStateOf(PlaceSearchUiState(search = PlaceSearchState("故宫", phase = PlaceSearchPhase.Loading)))
        var action: PlaceSearchAction? = null
        compose.setContent { EasyTripTheme { PlaceSearchContent(state.value, { action = it }) } }
        compose.onNodeWithText("正在搜索地点").assertIsDisplayed()

        compose.runOnIdle {
            state.value = PlaceSearchUiState(search = PlaceSearchState("不存在", phase = PlaceSearchPhase.Empty))
        }
        compose.onNodeWithText("没有找到相关地点").assertIsDisplayed()

        compose.runOnIdle {
            state.value = PlaceSearchUiState(search = PlaceSearchState("故宫", phase = PlaceSearchPhase.NetworkFailure("无法搜索新的地点。请检查网络连接后重试。")))
        }
        compose.onNodeWithText("网络连接失败", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("重新搜索").performClick()
        assertEquals(PlaceSearchAction.Retry, action)
    }

    @Test fun activeSearchSurfaceUsesOpaqueWhiteBackground() {
        setContent(PlaceSearchUiState(search = PlaceSearchState("故宫", phase = PlaceSearchPhase.Loading)))

        val image = compose.onNodeWithTag("place-search-surface").assertIsDisplayed().captureToImage()
        val pixel = image.toPixelMap()[image.width / 2, image.height / 4]
        assertTrue(pixel.alpha == 1f)
        assertEquals(Color.White, pixel)
    }

    @Test fun searchImeSubmitsAndEmptyStateCanClearQuery() {
        var action: PlaceSearchAction? = null
        setContent(
            PlaceSearchUiState(search = PlaceSearchState("不存在", phase = PlaceSearchPhase.Empty)),
            onAction = { action = it },
        )

        compose.onNodeWithContentDescription("搜索地点").performImeAction()
        assertEquals(PlaceSearchAction.Submit, action)
        compose.onNodeWithText("清空搜索").performClick()
        assertEquals(PlaceSearchAction.QueryChanged(""), action)
    }

    @Test fun routeBackPublishesCurrentSessionCollectionsThroughNavigationCallback() {
        val candidate = PlaceCandidate("poi-nav", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        val repository = TestSavedPlaces()
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            object : com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) = listOf(candidate)
            },
            SavedStateHandle(mapOf("query" to "故宫")),
        )
        var returnedPoiIds: Set<String>? = null
        compose.setContent {
            PlaceSearchRoute(model) { returnedPoiIds = model.recentlyCollectedPoiIds() }
        }
        compose.waitUntil(5_000) { model.state.value.search.results.isNotEmpty() }
        compose.onNodeWithTag("place-search-bookmark-touch-poi-nav").performClick()
        compose.waitUntil(5_000) { "poi-nav" in model.state.value.savedPoiIds }

        compose.onNodeWithContentDescription("返回地点池").performClick()

        compose.waitUntil(5_000) { returnedPoiIds != null }
        assertEquals(setOf("poi-nav"), returnedPoiIds)
    }

    @Test fun routeExitUsesLatestReplacedBackCallback() {
        val model = PlaceSearchViewModel("trip", TestSavedPlaces(), null, SavedStateHandle())
        val callbackVersion = mutableStateOf(1)
        var calledVersion = 0
        compose.setContent {
            val version = callbackVersion.value
            PlaceSearchRoute(model) { calledVersion = version }
        }
        compose.runOnIdle { callbackVersion.value = 2 }
        compose.runOnIdle { model.dispatch(PlaceSearchAction.Back) }
        compose.waitUntil(5_000) { calledVersion != 0 }

        assertEquals(2, calledVersion)
    }

    @Test fun repeatedBackAndRouteRecompositionInvokeExternalBackOnlyOnce() {
        val model = PlaceSearchViewModel(
            "trip",
            TestSavedPlaces(),
            null,
            SavedStateHandle(),
        )
        val recompose = mutableStateOf(0)
        var backCalls = 0
        compose.setContent {
            recompose.value
            PlaceSearchRoute(model) { backCalls++ }
        }

        compose.runOnIdle {
            model.dispatch(PlaceSearchAction.Back)
            model.dispatch(PlaceSearchAction.Back)
            recompose.value++
        }
        compose.waitUntil(5_000) { backCalls == 1 }
        compose.runOnIdle { recompose.value++ }

        compose.runOnIdle { assertEquals(1, backCalls) }
    }

    @Test fun systemBackPublishesCurrentSessionCollectionsThroughSameCallback() {
        val candidate = PlaceCandidate("poi-system", "天坛", "地址", GeoPoint(39.916, 116.397), "010")
        val repository = TestSavedPlaces()
        val model = PlaceSearchViewModel(
            "trip",
            repository,
            object : com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) = listOf(candidate)
            },
            SavedStateHandle(mapOf("query" to "天坛")),
        )
        var returnedPoiIds: Set<String>? = null
        compose.setContent { PlaceSearchRoute(model) { returnedPoiIds = model.recentlyCollectedPoiIds() } }
        compose.waitUntil(5_000) { model.state.value.search.results.isNotEmpty() }
        compose.onNodeWithTag("place-search-bookmark-touch-poi-system").performClick()
        compose.waitUntil(5_000) { "poi-system" in model.state.value.savedPoiIds }

        androidx.test.espresso.Espresso.pressBack()

        compose.waitUntil(5_000) { returnedPoiIds != null }
        assertEquals(setOf("poi-system"), returnedPoiIds)
    }

    @Test fun controlsUseSpecifiedVisualAndTouchBounds() {
        val candidate = PlaceCandidate("poi-1", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        setContent(PlaceSearchUiState(search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results)))

        assertSize("place-search-back", 44f, 44f)
        assertSize("place-search-field", expectedHeight = 48f)
        assertSize("place-search-place-icon-poi-1", 46f, 46f)
        assertSize("place-search-bookmark-visual-poi-1", 40f, 40f)
        assertSize("place-search-bookmark-touch-poi-1", 48f, 48f)
        compose.onNodeWithContentDescription("清空搜索").assertHasClickAction()
        assertSize("place-search-clear", 48f, 48f)
    }

    @Test fun narrowLargeFontKeepsResultsEmptyAndFailureInsideContainerWithoutOverlap() {
        val candidate = PlaceCandidate("poi-1", "故宫博物院", "地址", GeoPoint(39.916, 116.397), "010")
        val state = mutableStateOf(PlaceSearchUiState(search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results)))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                EasyTripTheme {
                    Box(Modifier.requiredWidth(280.dp).fillMaxHeight().testTag("narrow-container")) {
                        PlaceSearchContent(state.value, {})
                    }
                }
            }
        }

        assertHeaderInsideContainerWithoutOverlap()
        assertInside("place-search-result-row-poi-1", "place-search-surface")
        assertInside("place-search-place-icon-poi-1", "place-search-surface", useUnmergedTree = true)
        assertInside("place-search-result-text-poi-1", "place-search-surface", useUnmergedTree = true)
        assertInside("place-search-bookmark-visual-poi-1", "place-search-surface", useUnmergedTree = true)
        assertInside("place-search-bookmark-touch-poi-1", "place-search-surface")
        assertNoOverlap("place-search-place-icon-poi-1", "place-search-bookmark-touch-poi-1")
        assertNoOverlap("place-search-result-text-poi-1", "place-search-bookmark-touch-poi-1")
        assertMinimumTouchSize("place-search-bookmark-touch-poi-1", 48f)

        compose.runOnIdle { state.value = PlaceSearchUiState(search = PlaceSearchState("无", phase = PlaceSearchPhase.Empty)) }
        assertHeaderInsideContainerWithoutOverlap()
        assertInside("place-search-empty-body", "place-search-surface")
        assertInside("place-search-empty-icon", "place-search-surface")
        assertInside("place-search-empty-title", "place-search-surface")
        assertInside("place-search-empty-description", "place-search-surface")
        assertInside("place-search-empty-action", "place-search-surface")
        assertNoOverlap("place-search-empty-icon", "place-search-empty-title")
        assertNoOverlap("place-search-empty-title", "place-search-empty-description")
        assertNoOverlap("place-search-empty-description", "place-search-empty-action")
        assertMinimumTouchSize("place-search-empty-action", 48f)

        compose.runOnIdle {
            state.value = PlaceSearchUiState(search = PlaceSearchState("失败", phase = PlaceSearchPhase.NetworkFailure("网络不可用")))
        }
        assertHeaderInsideContainerWithoutOverlap()
        assertInside("place-search-network-failure-body", "place-search-surface")
        assertInside("place-search-network-failure-icon", "place-search-surface")
        assertInside("place-search-network-failure-title", "place-search-surface")
        assertInside("place-search-network-failure-description", "place-search-surface")
        assertInside("place-search-network-failure-action", "place-search-surface")
        assertNoOverlap("place-search-network-failure-icon", "place-search-network-failure-title")
        assertNoOverlap("place-search-network-failure-title", "place-search-network-failure-description")
        assertNoOverlap("place-search-network-failure-description", "place-search-network-failure-action")
        assertMinimumTouchSize("place-search-network-failure-action", 48f)
    }

    private fun assertHeaderInsideContainerWithoutOverlap() {
        val container = compose.onNodeWithTag("narrow-container").getUnclippedBoundsInRoot()
        val back = compose.onNodeWithTag("place-search-back").getUnclippedBoundsInRoot()
        val field = compose.onNodeWithTag("place-search-field").getUnclippedBoundsInRoot()
        assertTrue(back.left >= container.left && back.right <= container.right)
        assertTrue(field.left >= container.left && field.right <= container.right)
        assertTrue(back.right <= field.left)
    }

    private fun assertInside(tag: String, containerTag: String, useUnmergedTree: Boolean = false) {
        val container = compose.onNodeWithTag(containerTag).getUnclippedBoundsInRoot()
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree).getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= container.left)
        assertTrue(bounds.top >= container.top)
        assertTrue(bounds.right <= container.right)
        assertTrue(bounds.bottom <= container.bottom)
    }

    private fun assertNoOverlap(firstTag: String, secondTag: String) {
        val first = compose.onNodeWithTag(firstTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag(secondTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(
            first.right <= second.left || second.right <= first.left ||
                first.bottom <= second.top || second.bottom <= first.top,
        )
    }

    private fun assertMinimumTouchSize(tag: String, expectedSize: Float) {
        val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        assertTrue((bounds.right - bounds.left).value >= expectedSize)
        assertTrue((bounds.bottom - bounds.top).value >= expectedSize)
    }

    private fun assertSize(tag: String, expectedWidth: Float? = null, expectedHeight: Float? = null) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        expectedWidth?.let { assertEquals(it, (bounds.right - bounds.left).value, 0.5f) }
        expectedHeight?.let { assertEquals(it, (bounds.bottom - bounds.top).value, 0.5f) }
    }

    private fun setContent(
        state: PlaceSearchUiState,
        detailContent: (@androidx.compose.runtime.Composable (PlaceCandidate) -> Unit)? = { candidate ->
            Text(
                "详情：${candidate.name}",
                Modifier
                    .testTag("place-search-detail-action")
                    .clickable {}
                    .semantics { contentDescription = "详情操作" },
            )
        },
        onAction: (PlaceSearchAction) -> Unit = {},
        consent: com.yangchengwei.easytrip.amap.AmapConsentToken? = null,
        mapHostFactory: (android.content.Context) -> AmapMapHost = { error("unused map host") },
    ) {
        compose.setContent {
            EasyTripTheme {
                PlaceSearchContent(
                    state = state,
                    onAction = onAction,
                    detailContent = detailContent,
                    consent = consent,
                    mapHostFactory = mapHostFactory,
                )
            }
        }
    }

    private class TestSavedPlaces : SavedPlaceRepository {
        private val places = MutableStateFlow<List<SavedPlace>>(emptyList())
        private val ids = MutableStateFlow<Set<String>>(emptySet())
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = MutableStateFlow(emptyList())
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = ids
        override suspend fun save(tripId: String, candidate: PlaceCandidate): SavePlaceResult {
            val place = SavedPlace(candidate.poiId, tripId, candidate.poiId, candidate.name, candidate.address, candidate.point!!, "", emptyList())
            places.value += place
            ids.value += candidate.poiId
            return SavePlaceResult.Saved(place.id)
        }
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }
}
