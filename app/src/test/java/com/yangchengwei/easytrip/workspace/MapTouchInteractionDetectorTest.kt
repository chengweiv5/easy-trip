package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class MapTouchInteractionDetectorTest {
    @Test fun `tap does not report viewport interaction`() {
        var interactions = 0
        val detector = detector { interactions++ }

        detector.onDown(10f, 20f)
        detector.onMove(15f, 25f)
        detector.onUp()

        assertEquals(0, interactions)
    }

    @Test fun `drag beyond touch slop reports once`() {
        var interactions = 0
        val detector = detector { interactions++ }

        detector.onDown(10f, 20f)
        detector.onMove(19f, 20f)
        detector.onMove(30f, 20f)
        detector.onUp()

        assertEquals(1, interactions)
    }

    @Test fun `multi pointer interaction reports once`() {
        var interactions = 0
        val detector = detector { interactions++ }

        detector.onDown(10f, 20f)
        detector.onPointerDown()
        detector.onMove(40f, 20f)
        detector.onPointerDown()
        detector.onUp()

        assertEquals(1, interactions)
    }

    @Test fun `cancel resets an armed interaction`() {
        var interactions = 0
        val detector = detector { interactions++ }

        detector.onDown(10f, 20f)
        detector.onCancel()
        detector.onMove(30f, 20f)
        detector.onPointerDown()
        detector.onUp()

        assertEquals(0, interactions)
    }

    @Test fun `new sequence after cancel can report`() {
        var interactions = 0
        val detector = detector { interactions++ }

        detector.onDown(10f, 20f)
        detector.onCancel()
        detector.onDown(30f, 40f)
        detector.onMove(39f, 40f)

        assertEquals(1, interactions)
    }

    @Test fun `dispose resets and prevents late reporting`() {
        var interactions = 0
        val detector = detector { interactions++ }

        detector.onDown(10f, 20f)
        detector.dispose()
        detector.onMove(30f, 20f)
        detector.onPointerDown()
        detector.onDown(10f, 20f)
        detector.onMove(30f, 20f)

        assertEquals(0, interactions)
    }

    private fun detector(onInteraction: () -> Unit) = MapTouchInteractionDetector(
        touchSlop = 8f,
        onInteraction = onInteraction,
    )
}
