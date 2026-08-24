package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.permission.LocationPermissionSnapshot
import com.yangchengwei.easytrip.permission.PermissionExplanationContent
import com.yangchengwei.easytrip.permission.WorkspaceEffect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspacePermissionFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mapConsentExplanationDescribesPurposeBeforeConfirmation() {
        var confirmations = 0
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    kind = PermissionKind.MAP_SERVICE_CONSENT,
                    onConfirm = { confirmations++ },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("地图展示和地点搜索使用高德地图服务。是否启用由地图服务隐私授权单独决定。").assertIsDisplayed()
        compose.onNodeWithTag("permission-explanation-confirm").performClick()
        assertEquals(1, confirmations)
    }

    @Test fun locationRationaleExplainsPurposeBeforePermissionRequest() {
        val coordinator = LocationPermissionCoordinator(SavedStateHandle())
        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = true))
        val explanation = requireNotNull(coordinator.explanation.value)
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    kind = explanation,
                    onConfirm = coordinator::confirmExplanation,
                    onDismiss = coordinator::dismissExplanation,
                )
            }
        }

        compose.onNodeWithText("定位仅在你主动点击后用于在地图上显示当前位置。地图服务授权与设备定位权限相互独立。").assertIsDisplayed()
        assertEquals(false, coordinator.effects.tryReceive().isSuccess)
        compose.onNodeWithTag("permission-explanation-confirm").performClick()
        assertEquals(WorkspaceEffect.RequestLocationPermission, coordinator.effects.tryReceive().getOrNull())
    }

    @Test fun permanentDenialConfirmationOpensApplicationSettings() = runTest {
        val coordinator = LocationPermissionCoordinator(
            SavedStateHandle(mapOf("location.permission.hasRequested" to true)),
        )
        coordinator.onLocateClick(LocationPermissionSnapshot(granted = false, shouldShowRationale = false))
        val explanation = requireNotNull(coordinator.explanation.value)
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    kind = explanation,
                    onConfirm = coordinator::confirmExplanation,
                    onDismiss = coordinator::dismissExplanation,
                )
            }
        }

        compose.onNodeWithText("打开应用设置").assertIsDisplayed()
        compose.onNodeWithTag("permission-explanation-confirm").performClick()
        assertEquals(WorkspaceEffect.OpenApplicationSettings, coordinator.effects.receive())
    }

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
