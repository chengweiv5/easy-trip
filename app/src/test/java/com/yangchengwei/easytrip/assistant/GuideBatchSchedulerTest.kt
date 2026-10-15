package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuideBatchSchedulerTest {
    @Test fun twoStartBeforeAnyResponseAndThirdWaitsWithoutDelay() = runTest {
        val scheduler = GuideBatchScheduler()
        val release = CompletableDeferred<Unit>()
        val started = mutableListOf<Int>()
        val jobs = (1..3).map { id -> launch { scheduler.withSlot { started += id; release.await() } } }
        try {
            runCurrent()
            assertEquals(listOf(1, 2), started)
            assertEquals(0L, testScheduler.currentTime)
            release.complete(Unit); jobs.joinAll()
            assertEquals(listOf(1, 2, 3), started)
        } finally { jobs.forEach { it.cancel() } }
    }

    @Test fun cancellingQueuedWorkNeverEntersSlotAndPermitsAreRecovered() = runTest {
        val scheduler = GuideBatchScheduler()
        val hold = CompletableDeferred<Unit>()
        val jobs = (1..2).map { launch { scheduler.withSlot { hold.await() } } }
        runCurrent()
        var entered = false
        val queued = launch { scheduler.withSlot { entered = true } }
        runCurrent(); queued.cancelAndJoin()
        jobs.forEach { it.cancelAndJoin() }
        assertFalse(entered)
        scheduler.withSlot { entered = true }
        assertTrue(entered)
    }
}
