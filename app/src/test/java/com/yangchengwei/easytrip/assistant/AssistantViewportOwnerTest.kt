package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.workspace.*
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantViewportOwnerTest {
    private val initial = MapViewportRequest(1, ViewportReason.INITIAL, emptyList())
    private val candidate = MapViewportRequest(100, ViewportReason.SEARCH_RESULTS, emptyList())

    @Test fun drawerVisibilityNeverReplaysEitherCameraRequest() {
        val owner = AssistantViewportOwner()
        assertEquals(initial, owner.resolve(initial, null, false))
        assertEquals(candidate, owner.resolve(initial, candidate, true))
        assertEquals(candidate, owner.resolve(initial, candidate, false))
        assertEquals(candidate, owner.resolve(initial, candidate, true))
        assertEquals(null, owner.resolve(initial, null, true))
        assertEquals(null, owner.resolve(initial, null, false))
    }

    @Test fun hiddenResultsDoNotMoveCameraOrReplayWhenReopened() {
        val owner = AssistantViewportOwner()
        owner.resolve(initial, null, false)
        assertEquals(initial, owner.resolve(initial, candidate, false))
        assertEquals(initial, owner.resolve(initial, candidate, true))
        val next = candidate.copy(id = 101)
        assertEquals(next, owner.resolve(initial, next, true))
        assertEquals(null, owner.resolve(initial, next.copy(id = 102), false))
        assertEquals(null, owner.resolve(initial, next.copy(id = 102), true))
    }

    @Test fun gestureAndWorkspaceSearchKeepOwnershipUntilNewVisibleAssistantIntent() {
        val owner = AssistantViewportOwner()
        owner.resolve(initial, candidate)
        assertEquals(null, owner.resolve(null, candidate))
        assertEquals(null, owner.resolve(null, candidate, false))
        assertEquals(null, owner.resolve(null, candidate, true))
        val search = initial.copy(id = 2, reason = ViewportReason.SEARCH_FOCUS)
        assertEquals(search, owner.resolve(search, candidate))
        assertEquals(search, owner.resolve(search, null))
        assertEquals(candidate.copy(id = 101), owner.resolve(search, candidate.copy(id = 101)))
    }

    @Test fun ordinarySearchRegainsCameraEvenWhileDraftIsRetained() {
        val owner = AssistantViewportOwner()
        val initial = MapViewportRequest(1, ViewportReason.INITIAL, emptyList())
        val assistant = MapViewportRequest(100, ViewportReason.SEARCH_RESULTS, emptyList())
        val search = MapViewportRequest(2, ViewportReason.SEARCH_FOCUS, emptyList())
        assertEquals(initial, owner.resolve(initial, null))
        assertEquals(assistant, owner.resolve(initial, assistant))
        assertEquals(search, owner.resolve(search, assistant))
        assertEquals(search, owner.resolve(search, null))
    }
    @Test fun selectedCandidateFocusesOnlyItsPositionAndCanReturnToAll() {
        val a = com.yangchengwei.easytrip.place.amap.PlaceCandidate("a", "地点A", "地址A", com.yangchengwei.easytrip.core.model.GeoPoint(30.2,120.1), null, "杭州")
        val b = a.copy(poiId = "b", name = "地点B", point = com.yangchengwei.easytrip.core.model.GeoPoint(31.1,121.2))
        val state = PlaceAssistantState(items = listOf(
            IntakeItem("a", "A", PlaceIntent("地点A", "杭州", "地点A"), status = IntakeStatus.READY, poi = a),
            IntakeItem("b", "B", PlaceIntent("地点B", "杭州", "地点B"), status = IntakeStatus.READY, poi = b)))
        assertEquals(listOf(a.point, b.point), assistantViewportPoints(state, null))
        assertEquals(listOf(b.point), assistantViewportPoints(state, "b"))
    }

}
