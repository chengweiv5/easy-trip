package com.yangchengwei.easytrip.workspace

import android.os.Handler
import android.os.Looper
import android.view.Choreographer

internal fun interface MapReleaseScheduler {
    fun afterNextFrame(release: () -> Unit)
}

internal object AndroidMapReleaseScheduler : MapReleaseScheduler {
    override fun afterNextFrame(release: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        Choreographer.getInstance().postFrameCallback {
            // Posting from the frame callback lets traversal finish before AMap waits for GL cleanup.
            handler.post { release() }
        }
    }
}
