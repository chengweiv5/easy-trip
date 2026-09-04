package com.yangchengwei.easytrip.workspace

internal class MapReadyTimeoutException : IllegalStateException("Map did not become ready before timeout")

internal class MapReadyWatchdog(
    timeoutMillis: Long,
    private val nowMillis: () -> Long = { android.os.SystemClock.elapsedRealtime() },
    private val onTimeout: () -> Unit,
) {
    private var remainingMillis = timeoutMillis.coerceAtLeast(0)
    private var resumedAtMillis: Long? = null
    var isTerminal = false
        private set

    @Synchronized
    fun resume() {
        if (isTerminal || resumedAtMillis != null) return
        resumedAtMillis = nowMillis()
        timeoutIfElapsed()
    }

    @Synchronized
    fun pause() {
        val resumedAt = resumedAtMillis ?: return
        remainingMillis = (remainingMillis - (nowMillis() - resumedAt).coerceAtLeast(0)).coerceAtLeast(0)
        resumedAtMillis = null
        if (remainingMillis == 0L) finishTimeout()
    }

    @Synchronized
    fun ready() = cancel()

    @Synchronized
    fun cancel() {
        if (isTerminal) return
        isTerminal = true
        resumedAtMillis = null
    }

    @Synchronized
    fun remainingMillis(): Long = resumedAtMillis?.let { resumedAt ->
        (remainingMillis - (nowMillis() - resumedAt).coerceAtLeast(0)).coerceAtLeast(0)
    } ?: remainingMillis

    @Synchronized
    fun timeoutIfElapsed(): Boolean {
        val resumedAt = resumedAtMillis ?: return false
        if (nowMillis() - resumedAt < remainingMillis) return false
        remainingMillis = 0
        resumedAtMillis = null
        return finishTimeout()
    }

    private fun finishTimeout(): Boolean {
        if (isTerminal) return false
        isTerminal = true
        onTimeout()
        return true
    }
}
