package com.yangchengwei.easytrip.amap

import java.util.concurrent.atomic.AtomicBoolean

internal class OneShotCallback(
    private val isActive: () -> Boolean,
    private val clear: () -> Unit,
    private val deliver: () -> Unit,
) {
    private val finished = AtomicBoolean(false)
    fun complete(): Boolean {
        if (!isActive() || !finished.compareAndSet(false, true)) return false
        clear()
        deliver()
        return true
    }
    fun cancel() {
        if (finished.compareAndSet(false, true)) clear()
    }
}
