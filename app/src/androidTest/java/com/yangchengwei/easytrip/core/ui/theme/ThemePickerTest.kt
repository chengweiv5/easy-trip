package com.yangchengwei.easytrip.core.ui.theme

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.trip.ui.*
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ThemePickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private class Memory(var id: String? = null) : ThemePersistence {
        @Volatile var fail = false
        @Volatile var gate: CountDownLatch? = null
        override fun read() = id
        override fun write(id: String) {
            check(gate?.await(10, TimeUnit.SECONDS) != false)
            if (fail) throw IOException("disk full")
            this.id = id
        }
    }

    private fun start(store: ThemePreferenceStore) {
        compose.setContent { EasyTripThemeHost(store) {
            TripListContent(TripListUiState(page = TripListPageState.Content(
                TripCardUiModel("preview", "杭州 · 春日慢游", "3 天", "4月12日 — 4月14日", "公共交通", 9, 2, "9 个地点", "3 天行程", 67, "已安排 2 天"), emptyList(),
            )), {})
        } }
    }

    @Test fun fiveThemesPreviewDiscardApplyAndKeepAccessibleSelection() {
        val memory = Memory()
        val store = ThemePreferenceStore(memory)
        start(store)
        ThemePalette.entries.forEach { theme ->
            compose.onNodeWithTag("theme-entry").performClick()
            compose.onNodeWithTag("theme-apply").assertIsNotEnabled()
            compose.onNodeWithTag("theme-option-${theme.id}").performScrollTo().performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(ThemePalette.fromId(memory.id), store.theme.value) }
            save("picker-${theme.id}", "theme-picker")
            if (theme != store.theme.value) {
                compose.onNodeWithTag("theme-apply").performClick()
                compose.waitUntil(5_000) { store.theme.value == theme }
                compose.onNodeWithTag("theme-picker").assertDoesNotExist()
            } else compose.onNodeWithTag("theme-back").performClick()
            save("list-${theme.id}")
            compose.onNodeWithTag("theme-entry").performClick()
            compose.onNodeWithText("已使用${theme.displayName}").assertIsDisplayed()
            val other = if (theme == ThemePalette.ROSE) ThemePalette.LAKE else ThemePalette.ROSE
            compose.onNodeWithTag("theme-option-${other.id}").performScrollTo().performClick()
            androidx.test.espresso.Espresso.pressBack()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("theme-picker").fetchSemanticsNodes().isEmpty() }
            compose.runOnIdle { assertEquals(theme, store.theme.value) }
        }
        assertEquals("rose", memory.id)
    }

    @Test fun savingDisablesSelectionAndAllBackActionsThenFailureCanRetry() {
        val memory = Memory("forest").apply { fail = true; gate = CountDownLatch(1) }
        val store = ThemePreferenceStore(memory)
        start(store)
        compose.onNodeWithTag("theme-entry").performClick()
        compose.onNodeWithTag("theme-option-violet").performScrollTo().performClick()
        compose.onNodeWithTag("theme-apply").performClick()
        compose.onNodeWithText("正在应用…").assertIsDisplayed()
        compose.onNodeWithTag("theme-back").assertIsNotEnabled()
        compose.onNodeWithTag("theme-apply").assertIsNotEnabled()
        compose.onNodeWithTag("theme-option-rose").assertIsNotEnabled()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("theme-picker").assertIsDisplayed()
        memory.gate!!.countDown()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("theme-error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("重新应用山岚暮紫").assertIsDisplayed()
        compose.onNodeWithTag("theme-back").assertIsEnabled()
        assertEquals(ThemePalette.FOREST, store.theme.value)
        save("save-failure", "theme-picker")
        memory.fail = false
        compose.onNodeWithTag("theme-apply").performClick()
        compose.waitUntil(5_000) { store.theme.value == ThemePalette.VIOLET }
        compose.onNodeWithTag("theme-picker").assertDoesNotExist()
    }

    @Test fun smallScreenAndLargeFontsKeepLastOptionAndApplyReachable() {
        val state = mutableStateOf(ThemePickerState(visible = true))
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                Box(Modifier.requiredSize(320.dp, 568.dp)) {
                    ThemePickerScreen(state.value, ThemePalette.LAKE, { state.value = state.value.copy(preview = it) }, {}, {})
                }
            }
        }
        compose.onNodeWithTag("theme-option-rose").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(62.dp).performClick()
        compose.onNodeWithTag("theme-apply").assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("theme-back").assertIsDisplayed()
        save("picker-large-font", "theme-picker")
    }

    @Test fun emptyIllustrationsFollowPaletteWithoutLosingSoftAccents() {
        val palette = mutableStateOf(ThemePalette.LAKE)
        compose.setContent { EasyTripTheme(palette.value) {
            com.yangchengwei.easytrip.core.ui.component.EmptyIllustrationImage(com.yangchengwei.easytrip.core.ui.component.EmptyIllustration.Trips)
        } }
        ThemePalette.entries.forEach { theme ->
            compose.runOnIdle { palette.value = theme }
            val pixels = compose.onNodeWithTag("empty-illustration-trips").captureToImage().toPixelMap()
            var primary = 0
            var soft = 0
            for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
                if (pixels[x,y] == theme.colors.primary) primary++
                if (pixels[x,y] == theme.colors.primaryContainer) soft++
            }
            assertTrue(primary > 50)
            assertTrue(soft > 10)
        }
    }

    @Test fun atomicPreferenceSurvivesRestartAndInterruptedWriteKeepsSavedValue() {
        val file = File(compose.activity.cacheDir, "theme-test-${System.nanoTime()}")
        try {
            val persistence = AtomicThemePersistence(file)
            assertNull(persistence.read())
            persistence.write("forest")
            assertEquals(ThemePalette.FOREST, ThemePreferenceStore(AtomicThemePersistence(file)).theme.value)
            val atomic = android.util.AtomicFile(file)
            val interrupted = atomic.startWrite()
            interrupted.write("rose".toByteArray())
            atomic.failWrite(interrupted)
            assertEquals("forest", AtomicThemePersistence(file).read())
            ThemePalette.entries.forEach {
                persistence.write(it.id)
                assertEquals(it, ThemePreferenceStore(AtomicThemePersistence(file)).theme.value)
            }
        } finally { android.util.AtomicFile(file).delete() }
    }

    private fun save(name: String, tag: String? = null) {
        val bitmap = (if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag)).captureToImage().asAndroidBitmap()
        val dir = File(compose.activity.getExternalFilesDir(null), "themes").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
