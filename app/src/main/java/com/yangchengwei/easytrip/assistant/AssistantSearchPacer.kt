package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Shared by the application's assistant sessions, including manual retries.
 * Concurrency alone does not limit request frequency when the SDK responds quickly.
 */
class AssistantSearchPacer(
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val inFlight = Semaphore(2)
    private val starts = Mutex()
    private var lastStart: Long? = null

    suspend fun <T> execute(
        beforeStart: suspend () -> Unit = {},
        request: suspend () -> T,
    ): T = inFlight.withPermit {
        starts.withLock {
            lastStart?.let { previous ->
                delay((700 - (nowMillis() - previous)).coerceAtLeast(0))
            }
            currentCoroutineContext().ensureActive()
            // Potentially suspending diagnostics must not let later SDK calls overtake this one.
            beforeStart()
            currentCoroutineContext().ensureActive()
            lastStart = nowMillis()
        }
        // No suspension between releasing the start lock and entering the SDK request.
        request()
    }
}
