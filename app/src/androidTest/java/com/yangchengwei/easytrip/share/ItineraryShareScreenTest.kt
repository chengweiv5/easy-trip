package com.yangchengwei.easytrip.share

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import java.io.File
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ItineraryShareScreenTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun start(source:ShareSnapshotSource=ShareSnapshotSource { shareFixture() }) {
        compose.setContent { EasyTripTheme { ItineraryShareScreen("share-test",source,null,{}) } }
    }
    private fun ready()=compose.waitUntil(20_000) { runCatching { compose.onAllNodesWithTag("share-image-preview").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
    @Test fun defaultSingleDayNotesAndEnlargedPreviewUseCurrentOutput() {
        start();ready()
        compose.onNodeWithTag("share-all").assertIsSelected()
        compose.onNodeWithTag("share-notes").assertIsOn()
        compose.onNodeWithTag("share-send").assertIsEnabled()
        repeat(3) { compose.onNodeWithTag("share-image-preview").performTouchInput { swipeUp() } }
        repeat(3) { compose.onNodeWithTag("share-image-preview").performTouchInput { swipeDown() } }
        save("share-preview-fallback.png")
        compose.onNodeWithTag("share-single").performClick()
        compose.onNodeWithTag("share-day-1").performClick();ready()
        compose.onNodeWithTag("share-day-picker").assertTextContains("第 2 天",substring=true)
        compose.onNodeWithTag("share-notes").performClick();ready()
        compose.onNodeWithTag("share-notes").assertIsOff()
        compose.onNodeWithTag("share-expand").performClick()
        compose.onNodeWithText("长图预览").assertExists()
        compose.onNodeWithText("返回").performClick();ready()
        compose.onNodeWithTag("share-save").assertIsEnabled()
    }
    @Test fun fourteenDayTripCanPreviewAndShareFullTrip() {
        start(ShareSnapshotSource { multiDayShareFixture(14) });ready()
        compose.onNodeWithTag("share-all").assertIsSelected()
        compose.onNodeWithTag("share-notes").assertIsOn()
        compose.onNodeWithTag("share-send").assertIsEnabled()
        compose.onNodeWithTag("share-save").assertIsEnabled()
        compose.onNodeWithText("行程较长，请选择一天生成").assertDoesNotExist()
        repeat(4) { compose.onNodeWithTag("share-image-preview").performTouchInput { swipeUp() } }
        compose.onNodeWithTag("share-expand").performClick()
        compose.onNodeWithText("长图预览").assertExists()
    }
    @Test fun changingOptionsDisablesExportDuringRegenerationAndKeepsLatestChoice() {
        start(ShareSnapshotSource { delay(600);shareFixture() });ready()
        compose.onNodeWithTag("share-notes").performClick()
        compose.onNodeWithTag("share-send").assertIsNotEnabled()
        compose.onNodeWithTag("share-notes").performClick()
        compose.onNodeWithTag("share-single").performClick()
        compose.onNodeWithTag("share-day-2").performClick();ready()
        compose.onNodeWithTag("share-notes").assertIsOn()
        compose.onNodeWithTag("share-day-picker").assertTextContains("第 3 天",substring=true)
    }
    @Test fun emptyAndLoadFailureProvideRecovery() {
        var attempt=0
        start(ShareSnapshotSource { if(attempt++==0)error("test read failure") else shareFixture().copy(days=emptyList()) })
        compose.waitUntil(5_000) { compose.onAllNodesWithText("行程加载失败").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("重试").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("还没有可以分享的行程").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("share-send").assertIsNotEnabled()
    }
    @Test fun shareOpensSystemChooserAndCancelReturnsToPreview() {
        start();ready()
        compose.onNodeWithTag("share-send").performClick()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        fun currentActivities() = automation.executeShellCommand("dumpsys activity activities").use {
            ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()
        }
        val deadline=System.currentTimeMillis()+10_000
        var activities=currentActivities()
        while(automation.rootInActiveWindow?.packageName?.toString()!="com.android.intentresolver" && System.currentTimeMillis()<deadline) {
            Thread.sleep(100);activities=currentActivities()
        }
        assertTrue("Android chooser must open",activities.contains("ChooserActivity"))
        assertEquals("com.android.intentresolver",automation.rootInActiveWindow?.packageName?.toString())
        Thread.sleep(1_000) // Wait for the system sheet entrance animation before visual evidence.
        File(compose.activity.getExternalFilesDir(null),"share-system-chooser.png").outputStream().use {
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)
        }
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ready()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("share-send").fetchSemanticsNodes().all { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        }
    }
    private fun save(name:String) {
        val image=compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
