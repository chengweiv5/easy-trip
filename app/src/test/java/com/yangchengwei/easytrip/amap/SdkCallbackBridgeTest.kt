package com.yangchengwei.easytrip.amap

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SdkCallbackBridgeTest {
    private class FakeBoundary<T> : CallbackBoundary<T> {
        var listener: ((Result<T>) -> Unit)? = null
        var starts = 0
        var clears = 0
        var installAction: (() -> Unit)? = null
        var startAction: (() -> Unit)? = null
        var installError: Throwable? = null
        var startError: Throwable? = null
        var clearError: Throwable? = null
        var clearAction: ((Int) -> Unit)? = null
        override fun install(listener: (Result<T>) -> Unit) { installError?.let { throw it }; this.listener = listener; installAction?.invoke() }
        override fun clear() { clears++; listener = null; clearAction?.invoke(clears); clearError?.let { throw it } }
        override fun start() { starts++; startAction?.invoke(); startError?.let { throw it } }
    }

    @Test fun cancellationDuringInstallClearsListenerAndDoesNotStart() = runTest {
        val fake = FakeBoundary<Int>()
        val task = async { awaitSdkCallback("TEST", fake) { it } }
        fake.installAction = { task.cancel() }
        runCatching { task.await() }
        assertNull(fake.listener)
        assertEquals(0, fake.starts)
    }

    @Test fun cancellationAfterInstallClearsAndLateDuplicateCallbacksDoNothing() = runTest {
        val fake = FakeBoundary<Int>()
        val task = async { awaitSdkCallback("TEST", fake) { it } }
        testScheduler.runCurrent()
        val callback = fake.listener!!
        task.cancel()
        callback(Result.success(1)); callback(Result.success(2))
        val error = runCatching { task.await() }.exceptionOrNull()
        assert(error is CancellationException)
        assertNull(fake.listener)
    }

    @Test fun callbackParseFailureClearsAndBecomesStructuredFailure() = runTest {
        val fake = FakeBoundary<Int>()
        val task = async(SupervisorJob() + StandardTestDispatcher(testScheduler)) { awaitSdkCallback("PARSE", fake) { error("bad parse") } }
        testScheduler.runCurrent()
        fake.listener!!(Result.success(1))
        val error = runCatching { task.await() }.exceptionOrNull()!!
        assertEquals("PARSE_PARSE", (error as AmapServiceException).operation)
        assertNull(fake.listener)
    }

    @Test fun installAndStartFailuresAreStructuredAndCleaned() = runTest {
        val install = FakeBoundary<Int>().apply { installError = IllegalStateException("install") }
        assertEquals("INSTALL_INSTALL", (runCatching { awaitSdkCallback("INSTALL", install) { it } }.exceptionOrNull() as AmapServiceException).operation)
        val start = FakeBoundary<Int>().apply { startError = IllegalStateException("start") }
        assertEquals("START_START", (runCatching { awaitSdkCallback("START", start) { it } }.exceptionOrNull() as AmapServiceException).operation)
        assertNull(start.listener)
    }

    @Test fun cancelWinsPreStartCasAndStartIsNeverCalled() = runTest {
        val fake = FakeBoundary<Int>()
        lateinit var task: kotlinx.coroutines.Deferred<Int>
        task = async { awaitSdkCallback("TEST", fake, beforeStart = { task.cancel() }) { it } }
        runCatching { task.await() }
        assertEquals(0, fake.starts)
        assertNull(fake.listener)
    }

    @Test fun cancellationExceptionFromInstallIsNotWrapped() = runTest {
        val cancellation = CancellationException("cancelled factory")
        val fake = FakeBoundary<Int>().apply { installError = cancellation }
        val error = runCatching { awaitSdkCallback("TEST", fake) { it } }.exceptionOrNull()
        assert(error === cancellation)
    }

    @Test fun cleanupCancellationExceptionPropagatesUnchanged() = runTest {
        val cancellation = CancellationException("cleanup cancelled")
        val fake = FakeBoundary<Int>().apply { clearError = cancellation }
        val error = runCatching { cleanupBoundary(fake) }.exceptionOrNull()
        assertSame(cancellation, error)
    }

    @Test fun cleanupFailureDoesNotReplaceCallbackFailure() = runTest {
        val original = IllegalStateException("callback failed")
        val cleanup = IllegalArgumentException("cleanup failed")
        val fake = FakeBoundary<Int>().apply { clearError = cleanup }
        val task = async(SupervisorJob() + StandardTestDispatcher(testScheduler)) { awaitSdkCallback("TEST", fake) { it } }
        testScheduler.runCurrent()
        fake.listener!!(Result.failure(original))
        val error = runCatching { task.await() }.exceptionOrNull() as AmapServiceException
        assertEquals("TEST_CALLBACK", error.operation)
        assertTrue(error.suppressed.contains(cleanup))
    }

    @Test fun callbackCompletionDuringStartCleansExactlyOnceWhenStartThenFails() = runTest {
        val startFailure = IllegalStateException("start failed after callback")
        val secondCleanupFailure = IllegalArgumentException("second cleanup failed")
        val fake = FakeBoundary<Int>().apply {
            startAction = { listener!!(Result.success(7)) }
            startError = startFailure
            clearAction = { attempt -> if (attempt == 2) throw secondCleanupFailure }
        }

        assertEquals(7, awaitSdkCallback("TEST", fake) { it })
        assertEquals(1, fake.clears)
        assertNull(fake.listener)
    }

    @Test fun cancellationDuringStartObservesCleanupCancellationExceptionWhenStartThenFails() = runTest {
        val cancellation = CancellationException("request cancelled")
        val cleanup = CancellationException("cleanup cancelled")
        val fake = FakeBoundary<Int>().apply { clearError = cleanup }
        lateinit var task: kotlinx.coroutines.Deferred<Int>
        task = async { awaitSdkCallback("TEST", fake) { it } }
        fake.startAction = { task.cancel(cancellation) }
        fake.startError = IllegalStateException("start failed after cancellation")

        val error = runCatching { task.await() }.exceptionOrNull()!!

        assertTrue(error is CancellationException)
        assertTrue(cancellation.suppressed.contains(cleanup))
        assertEquals(1, fake.clears)
        assertNull(fake.listener)
    }

    @Test fun successCompletesOnceAndClearsListener() = runTest {
        val fake = FakeBoundary<Int>()
        val task = async { awaitSdkCallback("TEST", fake) { it * 2 } }
        testScheduler.runCurrent()
        val callback = fake.listener!!
        callback(Result.success(2)); callback(Result.success(3))
        assertEquals(4, task.await())
        assertNull(fake.listener)
    }
}
