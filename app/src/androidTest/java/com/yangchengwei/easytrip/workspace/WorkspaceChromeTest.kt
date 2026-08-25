package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
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
