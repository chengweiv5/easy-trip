package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
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

        compose.onNodeWithTag("workspace-back").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("workspace-more").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("workspace-trip-title").assertIsDisplayed()
        compose.onNodeWithText("8月23日 — 8月25日").assertIsDisplayed()
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
            .assertHeightIsEqualTo(68.dp)
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
        compose.onNodeWithTag("workspace-sheet-handle").assertHeightIsEqualTo(68.dp)
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

        compose.onNodeWithTag("workspace-sheet").assertHeightIsEqualTo(86.dp)
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithTag("minimum-window-summary").assertIsDisplayed()
    }

    @Test fun workspaceTabsUse44DpTouchHeightAndThreeDpIndicator() {
        compose.setContent {
            EasyTripTheme {
                WorkspaceTabs(WorkspaceSection.PLACE_POOL, {})
            }
        }

        compose.onNodeWithTag("section-PLACE_POOL").assertHeightIsEqualTo(44.dp)
        compose.onNodeWithTag("workspace-tab-indicator-PLACE_POOL", useUnmergedTree = true).assertHeightIsEqualTo(3.dp)
    }

    @Test fun mapControlsUseIconsWithoutPlaceholderActionText() {
        compose.setContent {
            EasyTripTheme {
                MapControls(
                    layer = MapLayer.STANDARD,
                    overlay = WorkspaceOverlay.LayerMenu,
                    onOpenLayerMenu = {},
                    onCloseOverlay = {},
                    onSelectLayer = {},
                    onLocate = {},
                )
            }
        }

        compose.onNodeWithContentDescription("定位").assertHasClickAction()
        compose.onNodeWithContentDescription("关闭图层菜单").assertHasClickAction()
        compose.onNodeWithText("定位").assertDoesNotExist()
        compose.onNodeWithText("关闭").assertDoesNotExist()
        compose.onNodeWithText("✓ 标准").assertDoesNotExist()
        compose.onNodeWithTag("layer-STANDARD").assertIsSelected()
    }
}
