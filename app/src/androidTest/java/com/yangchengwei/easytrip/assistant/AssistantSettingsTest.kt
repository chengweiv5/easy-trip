package com.yangchengwei.easytrip.assistant

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.settings.AppSettingsContent
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AssistantSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun leavingDuringImageCheckCancelsItAndKeepsCapabilityUnknown() {
        val directory = File(compose.activity.noBackupFilesDir, "image-check-${UUID.randomUUID()}")
        val isolated = object : android.content.ContextWrapper(compose.activity) {
            override fun getNoBackupFilesDir() = directory.apply { mkdirs() }
        }
        val store = AssistantConfigStore(isolated)
        val page = androidx.compose.runtime.mutableStateOf(true)
        val started = AtomicBoolean()
        val cancelled = AtomicBoolean()
        store.save(ProviderConfig("https://example.invalid", "test-vision", "test-key"))
        try {
            compose.setContent { EasyTripTheme {
                if (page.value) AssistantSettings(store, imageCheck = {
                    started.set(true)
                    try { awaitCancellation() } finally { cancelled.set(true) }
                }) { page.value = false }
                else androidx.compose.material3.Text("已返回设置")
            } }
            compose.onNodeWithTag("assistant-image-capability-check").performScrollTo().performClick()
            compose.waitUntil { started.get() }
            compose.onNodeWithTag("assistant-settings-back").performClick()
            compose.onNodeWithText("已返回设置").assertIsDisplayed()
            compose.waitUntil { cancelled.get() }
            assertEquals(ImageCapability.UNKNOWN, store.imageCapability().capability)
        } finally {
            compose.runOnIdle { page.value = false }
            directory.deleteRecursively()
        }
    }

    @Test fun globalEntryAndEncryptedConfigCanBeClearedWithoutTouchingTrips() {
        var opened = 0
        val context = compose.activity
        val store = AssistantConfigStore(context)
        val original = store.read()
        val value = ProviderConfig("https://example.invalid", "test-model", "fake-key-only-security-test")
        try {
            store.save(value)
            val reread = AssistantConfigStore(context).read()!!
            assertEquals(value.apiKey, reread.apiKey)
            assertFalse(File(context.noBackupFilesDir, "assistant-provider.enc").readBytes().toString(Charsets.ISO_8859_1).contains(value.apiKey))
            val page = androidx.compose.runtime.mutableStateOf(false)
            compose.setContent { EasyTripTheme {
                if (page.value) AssistantSettings(store) { page.value = false }
                else AppSettingsContent("湖畔晴空", true, "3.0.0", false, {}, {}, {}, {}, onAssistant = { opened++; page.value = true })
            } }
            compose.onNodeWithTag("app-settings-assistant").performScrollTo().performClick()
            assertEquals(1, opened)
            compose.onNodeWithText("App 全局设置 · 所有旅行共用").assertIsDisplayed()
            compose.runOnIdle { assertTrue(context.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0) }
            compose.onNodeWithTag("assistant-url").performScrollTo().performTextReplacement("https://changed.example.invalid")
            compose.onNodeWithTag("assistant-key").performScrollTo().assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
            assertEquals(value.apiKey, store.read()!!.apiKey) // Unsaved endpoint edit never replaces valid config.
            compose.onNodeWithText("清除模型配置").performScrollTo().performClick()
            compose.onNodeWithText("确认清除").performClick()
            assertNull(AssistantConfigStore(context).read())
            compose.onNodeWithTag("assistant-settings-back").performClick()
            compose.runOnIdle { assertEquals(0, context.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) }
        } finally { if (original != null) store.save(original) else store.clear() }
    }
}
