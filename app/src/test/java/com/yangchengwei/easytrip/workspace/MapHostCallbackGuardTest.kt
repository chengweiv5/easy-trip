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
    fun hostCallbackUsesLatestCallbackWithoutReplacingRegisteredListener() {
        val guard = MapHostCallbackGuard()
        var firstCallbackCount = 0
        var latestCallbackCount = 0

        guard.updateHostCallback { firstCallbackCount++ }
        val registeredListener: () -> Unit = { guard.dispatchHost() }
        guard.updateHostCallback { latestCallbackCount++ }
        registeredListener()

        assertEquals(0, firstCallbackCount)
        assertEquals(1, latestCallbackCount)
    }

    @Test
    fun ignoresCallbacksAfterDeactivation() {
        val guard = MapHostCallbackGuard()
        var readyCount = 0
        var errorCount = 0
        var hostEventCount = 0

        val generation = guard.beginRender()
        guard.updateHostCallback { hostEventCount++ }
        guard.deactivate()
        guard.reportReady(generation) { readyCount++ }
        guard.reportError(generation, IllegalStateException("late")) { errorCount++ }
        guard.dispatchHost()

        assertEquals(0, readyCount)
        assertEquals(0, errorCount)
        assertEquals(0, hostEventCount)
    }

    @Test
    fun deactivatedGuardDoesNotReportLateDisposalError() {
        val guard = MapHostCallbackGuard()
        var errors = 0

        guard.deactivate()
        guard.reportDisposalError(IllegalStateException("late destroy")) { errors++ }

        assertEquals(0, errors)
    }

    @Test
    fun activeGuardReportsDisposalErrorOnlyOnce() {
        val guard = MapHostCallbackGuard()
        var errors = 0

        guard.reportDisposalError(IllegalStateException("destroy")) { errors++ }
        guard.reportDisposalError(IllegalStateException("again")) { errors++ }

        assertEquals(1, errors)
    }

    @Test
    fun readyIgnoresLaterInitialFailuresButKeepsDispatchActive() {
        val guard = MapHostCallbackGuard()
        val generation = guard.beginRender()
        var ready = 0
        var errors = 0
        var dispatched = 0

        guard.reportReady(generation) { ready++ }
        guard.reportError(generation, IllegalStateException("render after ready")) { errors++ }
        guard.reportHostError(IllegalStateException("lifecycle after ready")) { errors++ }
        guard.reportDisposalError(IllegalStateException("destroy after ready")) { errors++ }
        guard.dispatch(generation) { dispatched++ }

        assertEquals(1, ready)
        assertEquals(0, errors)
        assertEquals(1, dispatched)
    }

    @Test
    fun hostFailureBeforeReadyBlocksReadyExactlyOnce() {
        val guard = MapHostCallbackGuard()
        val generation = guard.beginRender()
        var ready = 0
        var errors = 0

        guard.reportHostError(IllegalStateException("resume")) { errors++ }
        guard.reportReady(generation) { ready++ }
        guard.reportError(generation, IllegalStateException("again")) { errors++ }

        assertEquals(0, ready)
        assertEquals(1, errors)
    }
}
