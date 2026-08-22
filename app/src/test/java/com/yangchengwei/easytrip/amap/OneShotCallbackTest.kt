package com.yangchengwei.easytrip.amap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneShotCallbackTest {
    @Test fun completionCleansListenerAndIgnoresDuplicate() {
        var active = true
        var clears = 0
        var values = 0
        val callback = OneShotCallback({ active }, { active = false; clears++ }) { values++ }
        assertTrue(callback.complete())
        assertFalse(callback.complete())
        assertEquals(1, values)
        assertEquals(1, clears)
    }

    @Test fun cancellationCleansListenerAndLateCallbackIsIgnored() {
        var active = true
        var clears = 0
        var values = 0
        val callback = OneShotCallback({ active }, { active = false; clears++ }) { values++ }
        callback.cancel()
        assertFalse(callback.complete())
        assertEquals(0, values)
        assertEquals(1, clears)
    }
}
