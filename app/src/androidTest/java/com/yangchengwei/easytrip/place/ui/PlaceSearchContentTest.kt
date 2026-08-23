package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
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
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaceSearchContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun resultRowsExposeBookmarkButNoScheduleAction() {
        val candidate = PlaceCandidate("poi-1", "故宫博物院", "北京市东城区景山前街4号", GeoPoint(39.916, 116.397), "010")
        var action: PlaceSearchAction? = null
        setContent(PlaceSearchUiState(search = PlaceSearchState("故宫", listOf(candidate), phase = PlaceSearchPhase.Results))) {
            action = it
        }

        compose.onNodeWithText("故宫博物院").assertIsDisplayed()
        compose.onNodeWithContentDescription("收藏故宫博物院").assertHasClickAction().performClick()
        compose.onAllNodesWithText("加入行程").assertCountEquals(0)
        assertEquals(PlaceSearchAction.ToggleCollection("poi-1"), action)
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
        setContent(PlaceSearchUiState(search = PlaceSearchState("不存在", phase = PlaceSearchPhase.Empty))) {
            action = it
        }

        compose.onNodeWithContentDescription("搜索地点").performImeAction()
        assertEquals(PlaceSearchAction.Submit, action)
        compose.onNodeWithText("清空搜索").performClick()
        assertEquals(PlaceSearchAction.QueryChanged(""), action)
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
        assertInside("place-search-place-icon-poi-1", "place-search-surface")
        assertInside("place-search-result-text-poi-1", "place-search-surface")
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
        val first = compose.onNodeWithTag(firstTag).getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag(secondTag).getUnclippedBoundsInRoot()
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

    private fun setContent(state: PlaceSearchUiState, onAction: (PlaceSearchAction) -> Unit = {}) {
        compose.setContent { EasyTripTheme { PlaceSearchContent(state, onAction) } }
    }
}
