package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Rule
import org.junit.Test

class WorkspaceChromeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

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
