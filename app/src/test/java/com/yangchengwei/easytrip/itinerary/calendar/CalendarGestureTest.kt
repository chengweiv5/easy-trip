package com.yangchengwei.easytrip.itinerary.calendar

import com.yangchengwei.easytrip.itinerary.domain.*
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CalendarGestureTest {
    private fun timing(time: String?, stay: Int?) = ItineraryTiming(time?.let(LocalTime::parse), stay)
    @Test fun relativeStepsPreserveMinutesAndEdges() {
        val old = timing("09:40", 90)
        assertEquals(timing("10:10", 90), candidateTiming(old, CalendarDragMode.MOVE, 30f))
        assertEquals(timing("10:10", 60), candidateTiming(old, CalendarDragMode.START, 30f))
        assertEquals(timing("09:40", 120), candidateTiming(old, CalendarDragMode.END, 30f))
    }
    @Test fun noOpAndBoundariesPreserveNullAndOddMinutes() {
        val old = timing("00:10", null)
        assertEquals(old, candidateTiming(old, CalendarDragMode.MOVE, -120f))
        assertEquals(old, candidateTiming(old, CalendarDragMode.END, 2f))
        assertEquals(timing("00:10", 90), candidateTiming(old, CalendarDragMode.END, 30f))
        assertEquals(timing("23:00", 60), candidateTiming(timing("23:00", 30), CalendarDragMode.END, 120f))
        assertNull(candidateTiming(timing("23:40", 90), CalendarDragMode.MOVE, 30f))
    }
    @Test fun lateUnsetStayCanBeMovedEarlierOrShortenedIntoTheDay() {
        val old = timing("23:40", null)
        assertEquals(timing("22:40",60),candidateTiming(old,CalendarDragMode.MOVE,-60f))
        assertEquals(timing("23:30",30),candidateTiming(timing("23:30",null),CalendarDragMode.END,-30f))
        assertEquals(timing("23:30",null),candidateTiming(timing("23:30",null),CalendarDragMode.END,30f))
        assertEquals(old,candidateTiming(old,CalendarDragMode.MOVE,30f))
    }
    @Test fun pendingKeepsDurationOrUsesOneHourAndRejectsOutside() {
        assertEquals(timing("10:30", 90), candidateTiming(timing(null, 90), CalendarDragMode.PLACE, 622f))
        assertEquals(timing("10:00", 60), candidateTiming(timing(null, null), CalendarDragMode.PLACE, 600f))
        assertNull(candidateTiming(timing(null, null), CalendarDragMode.PLACE, 1440f))
        assertNull(candidateTiming(timing(null, 0), CalendarDragMode.PLACE, 600f))
    }
    @Test fun zeroMoveStaysZero() {
        assertEquals(timing("10:00", 0), candidateTiming(timing("09:30", 0), CalendarDragMode.MOVE, 30f))
    }
    @Test fun retryAndUndoKeepExactNullAndCannotOverwriteConcurrentChanges() = runTest {
        val change = ItineraryTimingChange("trip", "day", "item", timing(null, null), timing("10:00", 60))
        var stored = change.before
        var fail = true
        val controller = CalendarTimingController { request ->
            if (fail) error("disk")
            if (stored != request.before) false else { stored = request.after; true }
        }
        controller.commit(change)
        assertEquals(change.before, stored); assertNotNull(controller.state.value.retry)
        fail = false; controller.retry()
        assertEquals(change.after, stored)
        controller.undo(); assertEquals(change.before, stored)
        controller.commit(change); stored = timing("11:00", 60)
        controller.undo(); assertEquals(timing("11:00", 60), stored)
        assertNull(controller.state.value.undo)
    }
    @Test fun duplicateSavesAreIgnoredWhileFirstIsInFlight() = runTest {
        val gate = CompletableDeferred<Unit>(); var calls = 0
        val controller = CalendarTimingController { calls++; gate.await(); true }
        val change = ItineraryTimingChange("t", "d", "i", timing("09:00", 60), timing("10:00", 60))
        val job = launch { controller.commit(change) }
        testScheduler.runCurrent()
        controller.commit(change); assertEquals(1, calls)
        gate.complete(Unit); job.join()
    }
}
