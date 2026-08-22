package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class MapLifecycleControllerTest {
    @Test fun startsResumedWithoutDuplicatePause() {
        val events = mutableListOf<String>()
        val controller = MapLifecycleController({ events += "resume" }, { events += "pause" }, { events += "destroy" })
        controller.syncResumed(true)
        controller.syncResumed(true)
        controller.dispose()
        controller.dispose()
        assertEquals(listOf("resume", "pause", "destroy"), events)
    }

    @Test fun pausedDisposeDoesNotPauseAgain() {
        val events = mutableListOf<String>()
        val controller = MapLifecycleController({ events += "resume" }, { events += "pause" }, { events += "destroy" })
        controller.syncResumed(true)
        controller.syncResumed(false)
        controller.dispose()
        assertEquals(listOf("resume", "pause", "destroy"), events)
    }
}
