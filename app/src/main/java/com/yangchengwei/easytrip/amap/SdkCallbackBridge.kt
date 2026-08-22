package com.yangchengwei.easytrip.amap

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal interface CallbackBoundary<T> {
    fun install(listener: (Result<T>) -> Unit)
    fun clear()
    fun start()
}

internal fun cleanupBoundary(boundary: CallbackBoundary<*>) {
    boundary.clear()
}

private enum class BridgeState { READY, STARTING, STARTED, CANCELLED, COMPLETED }

internal suspend fun <T, R> awaitSdkCallback(
    operation: String,
    boundary: CallbackBoundary<T>,
    beforeStart: () -> Unit = {},
    parse: (T) -> R,
): R = suspendCancellableCoroutine { continuation ->
    val state = AtomicReference(BridgeState.READY)
    fun clearError(): Throwable? = try {
        cleanupBoundary(boundary)
        null
    } catch (error: Throwable) {
        error
    }
    fun withCleanup(error: Throwable): Throwable {
        clearError()?.takeUnless { it === error }?.let(error::addSuppressed)
        return error
    }
    fun fail(suffix: String, error: Throwable) {
        if (error is CancellationException) { continuation.resumeWithException(error); return }
        if (error is AmapServiceException) continuation.resumeWithException(error)
        else {
            val structured = AmapServiceException("${operation}_$suffix", 0, error.message.orEmpty())
            error.suppressed.forEach(structured::addSuppressed)
            continuation.resumeWithException(structured)
        }
    }
    val callback: (Result<T>) -> Unit = callback@{ result ->
        while (true) {
            val current = state.get()
            if (current == BridgeState.CANCELLED || current == BridgeState.COMPLETED) return@callback
            if (state.compareAndSet(current, BridgeState.COMPLETED)) break
        }
        result.fold(
            onSuccess = { value ->
                try {
                    val parsed = parse(value)
                    val cleanup = clearError()
                    if (cleanup == null) continuation.resume(parsed) else fail("CLEANUP", cleanup)
                } catch (error: Throwable) {
                    fail("PARSE", withCleanup(error))
                }
            },
            onFailure = { fail("CALLBACK", withCleanup(it)) },
        )
    }
    try {
        boundary.install(callback)
    } catch (error: Throwable) {
        state.set(BridgeState.COMPLETED)
        val failure = withCleanup(error)
        if (failure is CancellationException) throw failure
        fail("INSTALL", failure)
        return@suspendCancellableCoroutine
    }
    continuation.invokeOnCancellation { cancellation ->
        while (true) {
            val current = state.get()
            if (current == BridgeState.CANCELLED || current == BridgeState.COMPLETED) break
            if (state.compareAndSet(current, BridgeState.CANCELLED)) {
                clearError()?.takeUnless { it === cancellation }?.let { cleanup -> cancellation?.addSuppressed(cleanup) }
                break
            }
        }
    }
    beforeStart()
    if (!state.compareAndSet(BridgeState.READY, BridgeState.STARTING)) return@suspendCancellableCoroutine
    try {
        boundary.start()
        state.compareAndSet(BridgeState.STARTING, BridgeState.STARTED)
    } catch (error: Throwable) {
        if (error is CancellationException) {
            if (state.compareAndSet(BridgeState.STARTING, BridgeState.CANCELLED)) throw withCleanup(error)
        } else if (state.compareAndSet(BridgeState.STARTING, BridgeState.COMPLETED)) {
            fail("START", withCleanup(error))
        }
    }
}
