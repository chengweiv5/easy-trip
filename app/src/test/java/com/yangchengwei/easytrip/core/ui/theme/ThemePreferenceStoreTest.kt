package com.yangchengwei.easytrip.core.ui.theme

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemePreferenceStoreTest {
    private class MemoryPersistence(var id: String? = null) : ThemePersistence {
        var fail = false
        var writes = 0
        override fun read() = id
        override fun write(id: String) {
            writes++
            if (fail) throw IOException("disk full")
            this.id = id
        }
    }

    @Test fun unknownAndAbsentIdsUseDefault() = runTest {
        listOf(null, "deleted-theme", "").forEach {
            assertEquals(ThemePalette.LAKE, ThemePreferenceStore(MemoryPersistence(it)).theme.value)
        }
    }

    @Test fun successPublishesOnlyAfterWriteAndSurvivesRestart() = runTest {
        val persistence = MemoryPersistence()
        val store = ThemePreferenceStore(persistence, StandardTestDispatcher(testScheduler))
        val save = async { store.apply(ThemePalette.ROSE) }
        assertEquals(ThemePalette.LAKE, store.theme.value)
        advanceUntilIdle()
        assertTrue(save.await().isSuccess)
        assertEquals("rose", persistence.id)
        assertEquals(ThemePalette.ROSE, ThemePreferenceStore(persistence).theme.value)
    }

    @Test fun failedWriteKeepsPreviousThemeAndRetrySucceeds() = runTest {
        val persistence = MemoryPersistence("forest").apply { fail = true }
        val store = ThemePreferenceStore(persistence, StandardTestDispatcher(testScheduler))
        assertTrue(store.apply(ThemePalette.VIOLET).isFailure)
        assertEquals(ThemePalette.FOREST, store.theme.value)
        assertEquals("forest", persistence.id)
        persistence.fail = false
        assertTrue(store.apply(ThemePalette.VIOLET).isSuccess)
        assertEquals(ThemePalette.VIOLET, store.theme.value)
        assertEquals("violet", persistence.id)
    }

    @Test fun repeatedSelectionDoesNotWriteAndConcurrentSavesAreSerialized() = runTest {
        val persistence = MemoryPersistence("lake")
        val store = ThemePreferenceStore(persistence, StandardTestDispatcher(testScheduler))
        store.apply(ThemePalette.LAKE)
        assertEquals(0, persistence.writes)
        val first = async { store.apply(ThemePalette.FOREST) }
        val second = async { store.apply(ThemePalette.SUNSET) }
        first.await(); second.await()
        assertEquals(ThemePalette.SUNSET, store.theme.value)
        assertEquals("sunset", persistence.id)
    }

    @Test fun previewDiscardSavingGuardFailureAndRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = MemoryPersistence("forest")
            val store = ThemePreferenceStore(persistence, StandardTestDispatcher(testScheduler))
            val model = ThemePickerViewModel(store)
            model.open(); model.select(ThemePalette.ROSE)
            assertEquals(ThemePalette.FOREST, store.theme.value)
            model.dismiss(); model.open()
            assertEquals(ThemePalette.FOREST, model.state.value.preview)
            model.apply()
            assertFalse(model.state.value.saving)
            persistence.fail = true
            model.select(ThemePalette.VIOLET); model.apply()
            model.dismiss(); model.select(ThemePalette.SUNSET); model.apply()
            assertTrue(model.state.value.visible)
            assertTrue(model.state.value.saving)
            assertEquals(ThemePalette.VIOLET, model.state.value.preview)
            advanceUntilIdle()
            assertEquals(1, persistence.writes)
            assertTrue(model.state.value.failed)
            assertFalse(model.state.value.saving)
            assertEquals(ThemePalette.FOREST, store.theme.value)
            persistence.fail = false
            model.apply(); advanceUntilIdle()
            assertFalse(model.state.value.visible)
            assertEquals(ThemePalette.VIOLET, store.theme.value)
        } finally { Dispatchers.resetMain() }
    }
}
