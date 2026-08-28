package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.dispatchLocationPermissionRequest
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.permission.PermissionExplanationContent
import com.yangchengwei.easytrip.permission.WorkspaceEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspacePermissionFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun locationRationaleExplainsPurposeBeforePermissionRequest() {
        var confirmations = 0
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    onConfirm = { confirmations++ },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("定位仅在你主动点击后用于在地图上显示当前位置。地图服务授权与设备定位权限相互独立。").assertIsDisplayed()
        compose.onNodeWithTag("permission-explanation-confirm").performClick()
        assertEquals(1, confirmations)
    }

    @Test fun launcherStartedIsReportedOnlyAfterLaunchReturnsSuccessfully() = runBlocking {
        val store = InMemoryLocationPermissionRequestStore()
        val coordinator = LocationPermissionCoordinator(store)
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedSnapshot)
        coordinator.confirmExplanation()
        val effect = coordinator.effectFlow.first() as WorkspaceEffect.RequestLocationPermission

        dispatchLocationPermissionRequest(
            generation = effect.generation,
            launcher = { Result.success(Unit) },
            coordinator = coordinator,
        )

        assertTrue(store.hasRequested)
    }

    @Test fun launcherFailureDoesNotMarkPermissionRequested() = runBlocking {
        val store = InMemoryLocationPermissionRequestStore()
        val coordinator = LocationPermissionCoordinator(store)
        coordinator.attachWorkspace("trip-1")
        coordinator.onLocateClick(deniedSnapshot)
        coordinator.confirmExplanation()
        val effect = coordinator.effectFlow.first() as WorkspaceEffect.RequestLocationPermission

        dispatchLocationPermissionRequest(
            generation = effect.generation,
            launcher = { Result.failure(IllegalStateException("launcher unavailable")) },
            coordinator = coordinator,
        )

        assertFalse(store.hasRequested)
        assertTrue(coordinator.uiState.value.explanationVisible)
        assertEquals("无法打开系统权限请求，请重试", coordinator.uiState.value.error)
    }

    @Test fun declinedConsentShowsMapServiceDisabledRecovery() {
        var opens = 0
        compose.setContent {
            MaterialTheme {
                WorkspaceMapFallback(
                    state = WorkspaceMapState.ConsentRequired,
                    onOpenConsent = { opens++ },
                    onRetryMap = {},
                    onOpenLocationSettings = {},
                )
            }
        }

        compose.onNodeWithTag("map-consent-required").assertIsDisplayed()
        compose.onNodeWithTag("map-consent-open").performClick()
        assertEquals(1, opens)
    }

    @Test fun mapFailureShowsRetryInsteadOfConsentAction() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                WorkspaceMapFallback(
                    state = WorkspaceMapState.Failed("地图暂不可用"),
                    onOpenConsent = {},
                    onRetryMap = { retries++ },
                    onOpenLocationSettings = {},
                )
            }
        }

        compose.onNodeWithTag("map-load-failed").assertIsDisplayed()
        compose.onNodeWithTag("map-retry").performClick()
        assertEquals(1, retries)
    }

    @Test fun permanentLocationDenialShowsInlineSettingsAction() {
        var opens = 0
        compose.setContent {
            MaterialTheme {
                WorkspaceMapFallback(
                    state = WorkspaceMapState.LocationPermanentlyDenied,
                    onOpenConsent = {},
                    onRetryMap = {},
                    onOpenLocationSettings = { opens++ },
                )
            }
        }

        compose.onNodeWithTag("location-permission-denied").assertIsDisplayed()
        compose.onNodeWithTag("location-open-settings").performClick()
        assertEquals(1, opens)
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

    private val deniedSnapshot = com.yangchengwei.easytrip.permission.LocationPermissionSnapshot(
        granted = false,
        shouldShowRationale = true,
    )
}
