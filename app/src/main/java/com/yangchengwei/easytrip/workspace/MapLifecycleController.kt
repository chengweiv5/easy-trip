package com.yangchengwei.easytrip.workspace

internal class MapLifecycleController(
    private val resume: () -> Unit,
    private val pause: () -> Unit,
    private val destroy: () -> Unit,
) {
    private var resumed = false
    private var destroyed = false
    fun syncResumed(value: Boolean) {
        if (destroyed || resumed == value) return
        resumed = value
        if (value) resume() else pause()
    }
    fun dispose() {
        if (destroyed) return
        if (resumed) pause()
        resumed = false
        destroyed = true
        destroy()
    }
}
