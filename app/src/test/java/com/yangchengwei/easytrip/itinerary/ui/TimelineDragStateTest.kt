package com.yangchengwei.easytrip.itinerary.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineDragStateTest {
    @Test
    fun `commit keeps the latest preview target when end arrives before recomposition`() {
        val started = TimelineDragState.start(itemId = "i1", originalIndex = 0, initialTargetIndex = 0)
        val previewed = started.preview(targetIndex = 2)

        assertEquals(TimelineMove(itemId = "i1", targetIndex = 2), previewed.commitMove())
    }

    @Test
    fun `cancel never yields a move to commit`() {
        val previewed = TimelineDragState
            .start(itemId = "i1", originalIndex = 0, initialTargetIndex = 0)
            .preview(targetIndex = 2)

        assertNull(previewed.cancel().commitMove())
    }
}
