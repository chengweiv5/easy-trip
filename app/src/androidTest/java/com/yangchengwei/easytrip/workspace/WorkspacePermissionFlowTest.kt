package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.permission.PermissionExplanationContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspacePermissionFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun locationExplanationConfirmsBeforePermissionRequest() {
        var confirmations = 0
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    kind = PermissionKind.DEVICE_LOCATION,
                    onConfirm = { confirmations++ },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("允许定位").assertIsDisplayed()
        compose.onNodeWithTag("permission-explanation-confirm").performClick()
        assertEquals(1, confirmations)
    }

    @Test fun permanentDenialOffersApplicationSettings() {
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    kind = PermissionKind.DEVICE_LOCATION_SETTINGS,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("打开应用设置").assertIsDisplayed()
    }

    @Test fun mapFailureRetryIsLocalAndLocationRemainsAvailable() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                WorkspaceMapFallback(
                    state = WorkspaceMapState.Failed("地图暂不可用"),
                    onPrivacySettings = {},
                    onRetry = { retries++ },
                )
            }
        }

        compose.onNodeWithText("重试地图").performClick()
        assertEquals(1, retries)
    }

    @Test fun mapControlsAlwaysOfferLocation() {
        var locateClicks = 0
        compose.setContent {
            MaterialTheme {
                MapControls(
                    layer = MapLayer.STANDARD,
                    overlay = WorkspaceOverlay.None,
                    onOpenLayerMenu = {},
                    onCloseOverlay = {},
                    onSelectLayer = {},
                    onLocate = { locateClicks++ },
                )
            }
        }

        compose.onNodeWithTag("workspace-locate").performClick()
        assertEquals(1, locateClicks)
    }
}
