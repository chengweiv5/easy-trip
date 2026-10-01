package com.yangchengwei.easytrip.settings

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.workspace.WorkspaceMoreMenu
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppSettingsTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val release=OfficialRelease("v1.8.0","测试更新说明",false,false,ReleaseAsset("easy-trip-v1.8.0-release.apk","https://github.com/chengweiv5/easy-trip/releases/download/v1.8.0/easy-trip-v1.8.0-release.apk",60_000_000,"a".repeat(64)))
    @Test fun globalSettingsShowsVersionAndIndependentActions() {
        var theme=0;var consent=0;var update=0
        compose.setContent { EasyTripTheme { AppSettingsContent("湖畔晴空",true,"1.7.0",false,{}, {theme++},{consent++},{update++}) } }
        compose.onNodeWithText("v1.7.0").assertIsDisplayed()
        compose.onNodeWithTag("app-settings-theme").performClick();compose.onNodeWithTag("app-settings-map").performClick()
        compose.onNodeWithTag("app-settings-update").performScrollTo().performClick()
        assertEquals(listOf(1,1,1),listOf(theme,consent,update))
        save("settings")
    }
    @Test fun smallScreenLargeFontsKeepsVersionAndUpdateReachable() {
        compose.setContent { EasyTripTheme { val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,2f)) { Box(Modifier.requiredSize(320.dp,568.dp)) {
                AppSettingsContent("湖畔晴空",false,"1.7.0",false,{},{},{},{})
            } }
        } }
        compose.onNodeWithTag("app-version").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("app-settings-update").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }
    @Test fun updateStatesExposeOnlyValidActionsAndNeverDowngrade() {
        val state=mutableStateOf<UpdateState>(UpdateState.Idle)
        compose.setContent { EasyTripTheme { UpdateContent(state.value,"1.7.0",9,{},{},{},{},{}) } }
        compose.onNodeWithText("尚未检查更新").assertIsDisplayed()
        compose.runOnIdle { state.value=UpdateState.Checking }
        compose.onNodeWithTag("update-check").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("update-cancel").assertIsDisplayed()
        compose.runOnIdle { state.value=UpdateState.Ahead }
        compose.onNodeWithText("当前版本已领先正式版").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("update-download").assertDoesNotExist()
        compose.runOnIdle { state.value=UpdateState.Available(release) }
        compose.onNodeWithTag("update-download").performScrollTo().assertIsDisplayed()
        save("update-available")
        compose.runOnIdle { state.value=UpdateState.Ready(release,File("fake.apk")) }
        compose.onNodeWithTag("update-install").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value=UpdateState.Failed("网络失败",release) }
        compose.onNodeWithText("重新下载").performScrollTo().assertIsDisplayed()
    }
    @Test fun mapConsentRequiresReadAndConfirmsRevocationWithoutRemovingData() {
        var persisted:Boolean?=null;var revoked=0
        val store=AmapConsentStore(object:AmapConsentPersistence {
            override fun readDecision()=persisted
            override fun writeDecision(accepted:Boolean){persisted=accepted}
        },object:AmapPrivacyReporter { override suspend fun reportShown()=Unit;override suspend fun reportDecision(accepted:Boolean)=Unit },ConsentRegistry())
        compose.setContent { EasyTripTheme { MapConsentContent(store,{},{},{revoked++}) } }
        compose.onNodeWithTag("map-allow").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("map-policy-read").performScrollTo().performClick()
        compose.onNodeWithTag("map-allow").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil { persisted==true }
        compose.onNodeWithTag("map-revoke").performScrollTo().performClick()
        compose.onNodeWithText("取消").performClick();assertEquals(true,persisted)
        compose.onNodeWithTag("map-revoke").performClick()
        compose.onNodeWithTag("map-revoke-confirm").performClick()
        compose.waitUntil { persisted==false };assertEquals(1,revoked)
        compose.onNodeWithTag("map-allow").performScrollTo().assertIsNotEnabled()
        save("map-consent")
    }
    @Test fun workspaceMenuContainsOnlyTripOperations() {
        compose.setContent { EasyTripTheme { WorkspaceMoreMenu({}, {}, onShareItinerary={}) } }
        compose.onNodeWithTag("more-menu-settings").assertIsDisplayed()
        compose.onNodeWithTag("more-menu-share").assertIsDisplayed()
        compose.onNodeWithTag("more-menu-back-to-trips").assertIsDisplayed()
        compose.onNodeWithTag("more-menu-theme").assertDoesNotExist()
        compose.onNodeWithTag("more-menu-consent").assertDoesNotExist()
    }
    private fun save(name:String) {
        val folder=File(compose.activity.getExternalFilesDir(null),"settings-verification").apply {mkdirs()}
        File(folder,"$name.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
