package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapReadyWatchdogTest {
    @Test fun neverReadyTimesOutExactlyOnceAndIgnoresLateReady() {
        var now = 0L
        var timedOut = 0
        val watchdog = MapReadyWatchdog(timeoutMillis = 20, nowMillis = { now }) { timedOut++ }

        watchdog.resume()
        now += 20
        watchdog.timeoutIfElapsed()
        watchdog.timeoutIfElapsed()
        watchdog.ready()

        assertEquals(1, timedOut)
        assertTrue(watchdog.isTerminal)
    }

    @Test fun pauseDoesNotConsumeTimeoutAndResumeContinuesRemainingTime() {
        var now = 0L
        var timedOut = 0
        val watchdog = MapReadyWatchdog(timeoutMillis = 20, nowMillis = { now }) { timedOut++ }

        watchdog.resume()
        now += 9
        watchdog.pause()
        now += 100
        assertFalse(watchdog.isTerminal)

        watchdog.resume()
        now += 10
        assertFalse(watchdog.timeoutIfElapsed())
        now += 1
        watchdog.timeoutIfElapsed()

        assertEquals(1, timedOut)
    }

    @Test fun multiplePauseResumeIntervalsAccumulateOnlyForegroundTime() {
        var now = 0L
        var timedOut = 0
        val watchdog = MapReadyWatchdog(timeoutMillis = 100, nowMillis = { now }) { timedOut++ }

        watchdog.resume()
        now += 30
        watchdog.pause()
        now += 70
        watchdog.resume()
        now += 50
        watchdog.pause()
        assertFalse(watchdog.isTerminal)

        watchdog.resume()
        now += 19
        assertFalse(watchdog.timeoutIfElapsed())
        now += 1
        assertTrue(watchdog.timeoutIfElapsed())
        assertEquals(1, timedOut)
    }

    @Test fun pauseImmediatelyTerminatesWhenForegroundTimeIsAlreadyExhausted() {
        var now = 0L
        var timedOut = 0
        val watchdog = MapReadyWatchdog(timeoutMillis = 20, nowMillis = { now }) { timedOut++ }

        watchdog.resume()
        now += 20
        watchdog.pause()

        assertTrue(watchdog.isTerminal)
        assertEquals(1, timedOut)
    }

    @Test fun zeroOrNegativeTimeoutTerminatesImmediatelyOnResume() {
        var zeroTimeouts = 0
        val zero = MapReadyWatchdog(timeoutMillis = 0, nowMillis = { 0L }) { zeroTimeouts++ }
        zero.resume()

        var negativeTimeouts = 0
        val negative = MapReadyWatchdog(timeoutMillis = -1, nowMillis = { 0L }) { negativeTimeouts++ }
        negative.resume()

        assertTrue(zero.isTerminal)
        assertTrue(negative.isTerminal)
        assertEquals(1, zeroTimeouts)
        assertEquals(1, negativeTimeouts)
    }

    @Test fun readyOrCancelPreventsLaterTimeout() {
        var readyTimeouts = 0
        var now = 0L
        val ready = MapReadyWatchdog(timeoutMillis = 10, nowMillis = { now }) { readyTimeouts++ }
        ready.resume()
        ready.ready()
        now += 100
        ready.timeoutIfElapsed()

        var cancelledTimeouts = 0
        val cancelled = MapReadyWatchdog(timeoutMillis = 10, nowMillis = { now }) { cancelledTimeouts++ }
        cancelled.resume()
        cancelled.cancel()
        now += 100
        cancelled.timeoutIfElapsed()

        assertEquals(0, readyTimeouts)
        assertEquals(0, cancelledTimeouts)
    }

    @Test fun timeoutCallbackIsReentrantSafeAndRunsOnlyOnce() {
        var now = 0L
        var timedOut = 0
        lateinit var watchdog: MapReadyWatchdog
        watchdog = MapReadyWatchdog(timeoutMillis = 1, nowMillis = { now }) {
            timedOut++
            watchdog.timeoutIfElapsed()
        }

        watchdog.resume()
        now += 1
        watchdog.timeoutIfElapsed()

        assertEquals(1, timedOut)
    }
}
