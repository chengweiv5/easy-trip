package com.yangchengwei.easytrip.assistant

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class AssistantImageCapabilitySettingsTest {
    @Test fun capabilityBelongsToExactConfigurationAndLateChecksAreRejected() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.noBackupFilesDir, "capability-test-${UUID.randomUUID()}")
        val isolated = object : ContextWrapper(context) { override fun getNoBackupFilesDir() = directory.apply { mkdirs() } }
        try {
            val store = AssistantConfigStore(isolated)
            val a = ProviderConfig("https://example.test", "model-a", "test-key-a")
            val b = ProviderConfig("https://example.test", "model-b", "test-key-b")
            store.save(a)
            assertEquals(ImageCapability.UNKNOWN, store.imageCapability().capability)
            assertTrue(store.saveImageCapability(a, ImageCapabilityResult(ImageCapability.AVAILABLE, 100)))
            assertEquals(ImageCapability.AVAILABLE, store.imageCapability().capability)
            store.save(b)
            assertFalse(store.saveImageCapability(a, ImageCapabilityResult(ImageCapability.AVAILABLE, 200)))
            assertEquals(ImageCapability.UNKNOWN, store.imageCapability().capability)
        } finally { directory.deleteRecursively() }
    }
}
