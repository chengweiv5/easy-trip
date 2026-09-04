package com.yangchengwei.easytrip.workspace

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.permission.LocationPermissionSettingsBody
import com.yangchengwei.easytrip.permission.LocationPermissionSettingsContent
import com.yangchengwei.easytrip.permission.PermissionExplanationBody
import com.yangchengwei.easytrip.permission.PermissionExplanationContent
import com.yangchengwei.easytrip.AmapConsentBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspacePermissionFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun locationRationaleUsesJFhZ7CopyAndOnlyContinueConfirms() {
        var confirmations = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    onConfirm = { confirmations++ },
                    onDismiss = { dismissals++ },
                )
            }
        }

        compose.onNodeWithText("允许 Easy Trip 获取你的位置").assertIsDisplayed()
        compose.onNodeWithText("用于在地图上定位当前位置。只有点击定位按钮时才会使用，拒绝后仍可正常规划行程。").assertIsDisplayed()
        compose.onNodeWithText("继续").performClick()
        assertEquals(1, confirmations)
        assertEquals(0, dismissals)
    }

    @Test fun locationExplanationBodyRemainsOrderedAndActionableAt280DpWithTwoTimesFontScale() {
        var confirmations = 0
        var dismissals = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    Box(
                        Modifier
                            .width(280.dp)
                            .height(600.dp)
                            .testTag("permission-explanation-container"),
                    ) {
                        PermissionExplanationBody(
                            onConfirm = { confirmations++ },
                            onDismiss = { dismissals++ },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("允许 Easy Trip 获取你的位置", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val title = compose.onNodeWithText("允许 Easy Trip 获取你的位置").getUnclippedBoundsInRoot()
        val body = compose.onNodeWithText("用于在地图上定位当前位置。只有点击定位按钮时才会使用，拒绝后仍可正常规划行程。")
            .getUnclippedBoundsInRoot()
        val dismiss = compose.onNodeWithText("暂不使用").assertHasClickAction().getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithText("继续").assertHasClickAction().getUnclippedBoundsInRoot()
        val container = compose.onNodeWithTag("permission-explanation-container").getUnclippedBoundsInRoot()
        assertTrue("title=$title body=$body", title.top < body.top)
        assertTrue("body=$body actions=$dismiss/$confirm", body.bottom <= dismiss.top && body.bottom <= confirm.top)
        assertTrue("dismiss=$dismiss confirm=$confirm", dismiss.left < confirm.left)
        listOf(dismiss, confirm).forEach { action ->
            assertTrue("action=$action", action.right - action.left >= 48.dp && action.bottom - action.top >= 48.dp)
            assertTrue("container=$container action=$action", action.left >= container.left && action.right <= container.right)
            assertTrue("container=$container action=$action", action.top >= container.top && action.bottom <= container.bottom)
        }
        assertTrue("dismiss=$dismiss confirm=$confirm", dismiss.right <= confirm.left)

        compose.onNodeWithText("继续").performClick()
        compose.onNodeWithText("暂不使用").performClick()
        assertEquals(1, confirmations)
        assertEquals(1, dismissals)
    }

    @Test fun locationSettingsUsesHYCsZCopyAndSeparatesOpenFromCancel() {
        var opens = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                LocationPermissionSettingsContent(
                    onOpenSettings = { opens++ },
                    onDismiss = { dismissals++ },
                )
            }
        }

        compose.onNodeWithText("定位权限未开启").assertIsDisplayed()
        compose.onNodeWithText("请前往系统设置，为 Easy Trip 开启定位权限。地图和行程仍可正常使用。").assertIsDisplayed()
        compose.onNodeWithText("前往设置").performClick()
        assertEquals(1, opens)
        assertEquals(0, dismissals)
    }

    @Test fun permissionExplanationShowsLaunchFailurePolitelyAndBusyDisablesContinue() {
        val busy = androidx.compose.runtime.mutableStateOf(true)
        var confirmations = 0
        compose.setContent {
            MaterialTheme {
                PermissionExplanationContent(
                    busy = busy.value,
                    error = "无法打开系统权限请求，请重试",
                    onConfirm = { confirmations++ },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithTag("permission-explanation-confirm").assertIsNotEnabled().performClick()
        compose.onNodeWithText("无法打开系统权限请求，请重试")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertEquals(0, confirmations)

        compose.runOnUiThread { busy.value = false }
        compose.onNodeWithTag("permission-explanation-confirm").assertIsEnabled().performClick()
        assertEquals(1, confirmations)
    }

    @Test fun locationSettingsBusyConsumesRepeatedClicksAndFailureRestoresRetry() {
        val busy = androidx.compose.runtime.mutableStateOf(false)
        val error = androidx.compose.runtime.mutableStateOf<String?>(null)
        var opens = 0
        compose.setContent {
            MaterialTheme {
                LocationPermissionSettingsContent(
                    busy = busy.value,
                    error = error.value,
                    onOpenSettings = {
                        opens++
                        busy.value = true
                    },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithTag("location-settings-open").assertIsEnabled().performClick()
        compose.onNodeWithTag("location-settings-open").assertIsNotEnabled().performClick()
        assertEquals(1, opens)

        compose.runOnUiThread {
            busy.value = false
            error.value = "无法打开应用设置，请重试"
        }
        compose.onNodeWithText("无法打开应用设置，请重试")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithTag("location-settings-open").assertIsEnabled().performClick()
        assertEquals(2, opens)
    }

    @Test fun locationSettingsBodyActionsRemainReachableAt280dpWithTwoTimesFontScale() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = 2f)) {
                MaterialTheme {
                    Box(
                        Modifier
                            .width(280.dp)
                            .height(600.dp)
                            .testTag("location-settings-container"),
                    ) {
                        LocationPermissionSettingsBody(
                            onOpenSettings = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("定位权限未开启", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val container = compose.onNodeWithTag("location-settings-container").getUnclippedBoundsInRoot()
        val open = compose.onNodeWithText("前往设置").assertIsDisplayed().getUnclippedBoundsInRoot()
        val cancel = compose.onNodeWithText("取消").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(open.left >= container.left && open.right <= container.right)
        assertTrue(open.top >= container.top && open.bottom <= container.bottom)
        assertTrue(cancel.left >= container.left && cancel.right <= container.right)
        assertTrue(cancel.top >= container.top && cancel.bottom <= container.bottom)
        assertTrue(open.right - open.left >= 48.dp)
        assertTrue(open.bottom - open.top >= 48.dp)
        assertTrue(cancel.right - cancel.left >= 48.dp)
        assertTrue(cancel.bottom - cancel.top >= 48.dp)
        assertTrue(open.left >= cancel.right || cancel.left >= open.right || open.top >= cancel.bottom || cancel.top >= open.bottom)
    }

    @Test fun mapConsentBodyKeepsScrollableCopyAndActionsReachableAt280DpTwoTimesFont() {
        val policyRead = androidx.compose.runtime.mutableStateOf(false)
        var allows = 0
        var declines = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    Box(
                        Modifier
                            .width(280.dp)
                            .height(420.dp)
                            .testTag("map-consent-container"),
                    ) {
                        AmapConsentBody(
                            policyRead = policyRead.value,
                            onPolicyReadChange = { policyRead.value = it },
                            onOpenPolicy = {},
                            onAllow = { allows++ },
                            onDecline = { declines++ },
                            allowEnabled = policyRead.value,
                            declineEnabled = true,
                            modifier = Modifier.width(280.dp).height(420.dp),
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("允许 Easy Trip 使用地图", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val container = compose.onNodeWithTag("map-consent-container").getUnclippedBoundsInRoot()
        val scroll = compose.onNodeWithTag("map-consent-scroll").getUnclippedBoundsInRoot()
        val allow = compose.onNodeWithText("允许使用地图").getUnclippedBoundsInRoot()
        val decline = compose.onNodeWithText("暂不允许").getUnclippedBoundsInRoot()
        listOf(allow, decline).forEach { action ->
            assertTrue("container=$container action=$action", action.left >= container.left && action.right <= container.right)
            assertTrue("container=$container action=$action", action.top >= container.top && action.bottom <= container.bottom)
            assertTrue("action=$action", action.right - action.left >= 48.dp && action.bottom - action.top >= 48.dp)
        }
        assertTrue("scroll=$scroll allow=$allow decline=$decline", scroll.bottom <= allow.top && scroll.bottom <= decline.top)
        assertTrue("allow=$allow decline=$decline", allow.top < decline.top)
        assertTrue("allow=$allow decline=$decline", allow.right <= decline.left || decline.right <= allow.left || allow.bottom <= decline.top || decline.bottom <= allow.top)
        compose.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
            useUnmergedTree = true,
        ).assertCountEquals(1)
        val toggle = compose.onNodeWithTag("map-consent-policy-confirmation")
            .fetchSemanticsNode().config[SemanticsActions.OnClick]
        compose.runOnIdle { checkNotNull(toggle.action).invoke() }
        compose.runOnIdle { assertTrue(policyRead.value) }
        compose.onNodeWithTag("map-consent-policy-confirmation").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, androidx.compose.ui.state.ToggleableState.On),
        )
        compose.onNodeWithText("允许使用地图").assertIsEnabled().performClick()
        compose.onNodeWithText("暂不允许").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, allows)
            assertEquals(1, declines)
        }
    }

    @Test fun declinedConsentShowsMapServiceDisabledRecovery() {
        var opens = 0
        compose.setContent {
            MaterialTheme {
                WorkspaceMapFallback(
                    state = WorkspaceMapState.ConsentRequired,
                    onOpenConsent = { opens++ },
                    onRetryMap = {},
                )
            }
        }

        compose.onNodeWithTag("map-consent-required").assertIsDisplayed()
        compose.onNodeWithTag("map-consent-open").performClick()
        assertEquals(1, opens)
    }

    @Test fun loadingAndFailureUseTheApprovedMapRecoveryCopy() {
        val state = androidx.compose.runtime.mutableStateOf<WorkspaceMapState>(WorkspaceMapState.Loading)
        compose.setContent {
            MaterialTheme {
                WorkspaceMapFallback(
                    state = state.value,
                    onOpenConsent = {},
                    onRetryMap = {},
                )
            }
        }

        compose.onNodeWithText("正在加载地图").assertIsDisplayed()
        compose.onNodeWithText("地点和行程仍可继续查看").assertIsDisplayed()
        compose.runOnUiThread { state.value = WorkspaceMapState.Failed("地图暂时无法加载") }

        compose.onNodeWithText("地图暂时无法加载").assertIsDisplayed()
        compose.onNodeWithText("地点和行程仍可查看，请稍后重试").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test fun mapLoadingAndFailureRemainAccessibleAt280DpWithTwoTimesFontScale() {
        val state = androidx.compose.runtime.mutableStateOf<WorkspaceMapState>(WorkspaceMapState.Loading)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    Box(
                        Modifier
                            .width(280.dp)
                            .height(480.dp)
                            .testTag("map-fallback-container"),
                    ) {
                        WorkspaceMapFallback(
                            state = state.value,
                            onOpenConsent = {},
                            onRetryMap = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("workspace-map-fallback").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
        compose.onNodeWithText("正在加载地图").assertIsDisplayed()
        compose.onNodeWithText("地点和行程仍可继续查看").assertIsDisplayed()
        compose.onNodeWithTag("workspace-map-loading").assertContentDescriptionEquals("地图正在加载")
        compose.onAllNodesWithTag("map-retry").assertCountEquals(0)
        compose.onAllNodesWithTag("map-consent-open").assertCountEquals(0)

        compose.runOnUiThread { state.value = WorkspaceMapState.Failed("地图服务错误详情") }
        compose.onNodeWithTag("workspace-map-fallback")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher("has no content description") {
                !it.config.contains(SemanticsProperties.ContentDescription)
            })
        compose.onNodeWithText("地图暂时无法加载").assertIsDisplayed()
        compose.onNodeWithText("地点和行程仍可查看，请稍后重试").assertIsDisplayed()
        compose.onAllNodesWithText("正在加载地图").assertCountEquals(0)
        compose.onAllNodesWithTag("workspace-map-loading").assertCountEquals(0)
        compose.onAllNodesWithTag("map-retry").assertCountEquals(1)
        compose.onAllNodesWithTag("map-consent-open").assertCountEquals(0)

        val container = compose.onNodeWithTag("map-fallback-container").getUnclippedBoundsInRoot()
        val retry = compose.onNodeWithTag("map-retry").getUnclippedBoundsInRoot()
        assertTrue("container=$container retry=$retry", retry.left >= container.left && retry.right <= container.right)
        assertTrue("container=$container retry=$retry", retry.top >= container.top && retry.bottom <= container.bottom)
        assertTrue("retry=$retry", retry.right - retry.left >= 48.dp && retry.bottom - retry.top >= 48.dp)
    }

    @Test fun mapFailureShowsRetryInsteadOfConsentAction() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                WorkspaceMapFallback(
                    state = WorkspaceMapState.Failed("地图暂不可用"),
                    onOpenConsent = {},
                    onRetryMap = { retries++ },
                )
            }
        }

        compose.onNodeWithTag("map-load-failed").assertIsDisplayed()
        compose.onNodeWithTag("map-retry").performClick()
        assertEquals(1, retries)
    }

    @Test fun locationSettingsCancelOnlyDismisses() {
        var opens = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                LocationPermissionSettingsContent(
                    onOpenSettings = { opens++ },
                    onDismiss = { dismissals++ },
                )
            }
        }

        compose.onNodeWithText("取消").performClick()
        assertEquals(0, opens)
        assertEquals(1, dismissals)
    }

    @Test fun mapControlsAlwaysOfferLocation() {
        var locateClicks = 0
        compose.setContent {
            MaterialTheme {
                MapControls(
                    active = false,
                    onOpenLayerMenu = {},
                    onLocate = { locateClicks++ },
                )
            }
        }

        compose.onNodeWithTag("workspace-locate").performClick()
        assertEquals(1, locateClicks)
    }
}
