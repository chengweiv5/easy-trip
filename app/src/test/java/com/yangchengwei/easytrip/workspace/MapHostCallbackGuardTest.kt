package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class MapHostCallbackGuardTest {
    @Test
    fun reportsReadyOnlyOnceWhileActive() {
        val guard = MapHostCallbackGuard()
        var readyCount = 0

        val generation = guard.beginRender()
        guard.reportReady(generation) { readyCount++ }
        guard.reportReady(generation) { readyCount++ }

        assertEquals(1, readyCount)
    }

    @Test
    fun errorIsTerminalAndPreventsReadyOrEvents() {
        val guard = MapHostCallbackGuard()
        var readyCount = 0
        var errorCount = 0
        var eventCount = 0

        val generation = guard.beginRender()
        guard.reportError(generation, IllegalStateException("failed")) { errorCount++ }
        guard.reportReady(generation) { readyCount++ }
        guard.dispatch(generation) { eventCount++ }

        assertEquals(1, errorCount)
        assertEquals(0, readyCount)
        assertEquals(0, eventCount)
    }

    @Test
    fun ignoresCallbacksFromOlderRenderGeneration() {
        val guard = MapHostCallbackGuard()
        val oldGeneration = guard.beginRender()
        val currentGeneration = guard.beginRender()
        var readyCount = 0
        var errorCount = 0
        var eventCount = 0

        guard.reportReady(oldGeneration) { readyCount++ }
        guard.reportError(oldGeneration, IllegalStateException("old")) { errorCount++ }
        guard.dispatch(oldGeneration) { eventCount++ }
        guard.reportReady(currentGeneration) { readyCount++ }

        assertEquals(1, readyCount)
        assertEquals(0, errorCount)
        assertEquals(0, eventCount)
    }

    @Test
    fun ignoresCallbacksAfterDeactivation() {
        val guard = MapHostCallbackGuard()
        var readyCount = 0
        var errorCount = 0

        val generation = guard.beginRender()
        guard.deactivate()
        guard.reportReady(generation) { readyCount++ }
        guard.reportError(generation, IllegalStateException("late")) { errorCount++ }

        assertEquals(0, readyCount)
        assertEquals(0, errorCount)
    }
}
