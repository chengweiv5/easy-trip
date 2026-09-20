package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewportControllerTest {
    @Test fun switchingDayAfterGestureFocusesTheNewDayButLateResultsDoNot() {
        val c = MapViewportController()
        val a = GeoPoint(30.0, 110.0)
        val b = GeoPoint(35.0, 115.0)
        c.update(listOf(a,b), MapScope.SINGLE_DAY, listOf(a), "a")
        c.onUserGesture()
        assertEquals(listOf(b), c.update(listOf(a,b), MapScope.SINGLE_DAY, listOf(b), "b")?.points)
        c.onUserGesture()
        assertNull(c.update(listOf(a,b), MapScope.SINGLE_DAY, listOf(a,b), "b"))
    }

    private val beijing = GeoPoint(39.9, 116.4)
    private val shanghai = GeoPoint(31.2, 121.5)
    private val changedBeijing = GeoPoint(39.91, 116.4)

    @Test fun `zoom controller baselines a replacement host to already consumed requests`() {
        val controller = MapZoomRequestController(initialZoomInRequest = 3, initialZoomOutRequest = 4)

        assertFalse(controller.consumeZoomIn(3))
        assertFalse(controller.consumeZoomOut(4))
        assertEquals(true, controller.consumeZoomIn(4))
        assertEquals(true, controller.consumeZoomOut(5))
    }

    @Test fun `zoom controller consumes every request in a coalesced counter jump`() {
        val controller = MapZoomRequestController()

        assertTrue(controller.consumeZoomIn(2))
        assertTrue(controller.consumeZoomIn(2))
        assertFalse(controller.consumeZoomIn(2))
        assertTrue(controller.consumeZoomOut(2))
        assertTrue(controller.consumeZoomOut(2))
        assertFalse(controller.consumeZoomOut(2))
    }

    @Test fun `first nonempty place set emits initial request`() {
        val controller = MapViewportController()
        assertNull(controller.update(emptyList(), MapScope.PLACE_POOL, emptyList()))
        val request = controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai))
        assertEquals(1L, request?.id)
        assertEquals(ViewportReason.INITIAL, request?.reason)
        assertEquals(listOf(beijing, shanghai), request?.points)
    }

    @Test fun `first eight place points fit once and style-only recomposition does not refit`() {
        val controller = MapViewportController()
        val points = List(8) { index -> GeoPoint(30.0 + index, 110.0 + index) }

        val initial = controller.update(points, MapScope.PLACE_POOL, points)
        val styleOnly = controller.update(points, MapScope.PLACE_POOL, points)

        assertEquals(ViewportReason.INITIAL, initial?.reason)
        assertEquals(points, initial?.points)
        assertNull(styleOnly)
        assertEquals(initial, controller.currentRequest)
    }

    @Test fun `identical or reordered place set emits nothing`() {
        val controller = initializedController()
        assertNull(controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai)))
        assertNull(controller.update(listOf(shanghai, beijing), MapScope.PLACE_POOL, listOf(shanghai, beijing)))
    }

    @Test fun `adding deleting and changing coordinates emit place set changed`() {
        val controller = MapViewportController()
        controller.update(listOf(beijing), MapScope.PLACE_POOL, listOf(beijing))
        val added = controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai))
        val deleted = controller.update(listOf(shanghai), MapScope.PLACE_POOL, listOf(shanghai))
        val moved = controller.update(listOf(changedBeijing), MapScope.PLACE_POOL, listOf(changedBeijing))
        assertEquals(listOf(2L, 3L, 4L), listOf(added?.id, deleted?.id, moved?.id))
        assertEquals(List(3) { ViewportReason.PLACE_SET_CHANGED }, listOf(added?.reason, deleted?.reason, moved?.reason))
    }

    @Test fun `map card collection change updates place baseline without moving camera`() {
        val controller = initializedController()
        val before = controller.currentRequest
        controller.retainViewportForPlaceChange(changedBeijing)

        assertNull(
            controller.update(
                listOf(beijing, shanghai, changedBeijing),
                MapScope.PLACE_POOL,
                listOf(beijing, shanghai, changedBeijing),
            ),
        )
        assertEquals(before, controller.currentRequest)

        val laterDelete = controller.update(
            listOf(beijing, shanghai),
            MapScope.PLACE_POOL,
            listOf(beijing, shanghai),
        )
        assertEquals(ViewportReason.PLACE_SET_CHANGED, laterDelete?.reason)
    }

    @Test fun `map card removal updates place baseline without moving camera`() {
        val controller = initializedController()
        val before = controller.currentRequest
        controller.retainViewportForPlaceChange(shanghai)

        assertNull(controller.update(listOf(beijing), MapScope.PLACE_POOL, listOf(beijing)))
        assertEquals(before, controller.currentRequest)
    }

    @Test fun `retained point does not suppress unrelated place changes`() {
        val controller = initializedController()
        controller.retainViewportForPlaceChange(changedBeijing)

        val request = controller.update(listOf(beijing), MapScope.PLACE_POOL, listOf(beijing))

        assertEquals(ViewportReason.PLACE_SET_CHANGED, request?.reason)
    }

    @Test fun `switching scope emits visible points`() {
        val controller = initializedController()
        val request = controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(shanghai))
        assertEquals(2L, request?.id)
        assertEquals(ViewportReason.SCOPE_CHANGED, request?.reason)
        assertEquals(listOf(shanghai), request?.points)
    }

    @Test fun `manual map movement clears pending automatic request and permits explicit navigation refits`() {
        val controller = initializedController()

        controller.onUserGesture()

        assertNull(controller.currentRequest)
        assertTrue(controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(shanghai), "day-1") != null)
        assertTrue(controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(beijing), "day-2") != null)
        assertTrue(controller.update(listOf(beijing, shanghai), MapScope.WHOLE_TRIP, listOf(beijing, shanghai)) != null)
        assertTrue(controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai)) != null)
        assertTrue(controller.currentRequest != null)
    }

    @Test fun `in app zoom reports a user viewport operation before zoom and permits explicit tab fits`() {
        val controller = initializedController()
        val events = mutableListOf<String>()

        performUserViewportOperation(
            onUserViewportOperation = {
                events += "viewport"
                controller.onUserGesture()
            },
            operation = { events += "zoom-in" },
        )

        assertEquals(listOf("viewport", "zoom-in"), events)
        assertNull(controller.currentRequest)
        assertTrue(controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(shanghai), "day-1") != null)
        assertTrue(controller.update(listOf(beijing, shanghai), MapScope.WHOLE_TRIP, listOf(beijing, shanghai)) != null)
        assertTrue(controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai)) != null)
    }

    @Test fun `place change after manual movement refits once and clears suppression`() {
        val controller = initializedController()
        controller.onUserGesture()
        controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(shanghai), "day-1")

        val changed = controller.update(
            listOf(beijing, shanghai, changedBeijing),
            MapScope.SINGLE_DAY,
            listOf(shanghai, changedBeijing),
            "day-1",
        )

        assertEquals(ViewportReason.PLACE_SET_CHANGED, changed?.reason)
        assertNull(
            controller.update(
                listOf(beijing, shanghai, changedBeijing),
                MapScope.SINGLE_DAY,
                listOf(shanghai, changedBeijing),
                "day-1",
            ),
        )
        assertEquals(
            ViewportReason.SCOPE_CHANGED,
            controller.update(
                listOf(beijing, shanghai, changedBeijing),
                MapScope.WHOLE_TRIP,
                listOf(beijing, shanghai, changedBeijing),
            )?.reason,
        )
    }

    @Test fun `explicit search focus is not blocked by manual movement`() {
        val controller = initializedController()
        controller.onUserGesture()

        val request = controller.focusSearchResult(changedBeijing)

        assertEquals(ViewportReason.SEARCH_FOCUS, request.reason)
        assertEquals(listOf(changedBeijing), request.points)
    }

    @Test fun `manual movement after search focus clears the pending focus request`() {
        val controller = initializedController()
        controller.focusSearchResult(changedBeijing)

        controller.onUserGesture()

        assertNull(controller.currentRequest)
    }

    @Test fun `viewport request identity retains scope selected day and complete geometry`() {
        val controller = MapViewportController()
        val routeGeometry = listOf(beijing, changedBeijing, shanghai)

        val request = controller.update(
            placePoints = listOf(beijing, shanghai),
            scope = MapScope.SINGLE_DAY,
            selectedDayId = "day-1",
            visiblePoints = routeGeometry,
        )

        assertEquals(MapScope.SINGLE_DAY, request?.scope)
        assertEquals("day-1", request?.selectedDayId)
        assertEquals(routeGeometry, request?.points)
    }

    @Test fun `switching selected day refits changed single day points once`() {
        val controller = initializedController()
        controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(beijing))

        val request = controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(shanghai))

        assertEquals(ViewportReason.VISIBLE_SET_CHANGED, request?.reason)
        assertEquals(listOf(shanghai), request?.points)
        assertNull(controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(shanghai)))
    }

    @Test fun `editing selected day refits its complete changed route points once`() {
        val controller = initializedController()
        controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(beijing))

        val request = controller.update(
            listOf(beijing, shanghai),
            MapScope.SINGLE_DAY,
            listOf(beijing, changedBeijing, shanghai),
        )

        assertEquals(ViewportReason.VISIBLE_SET_CHANGED, request?.reason)
        assertEquals(listOf(beijing, changedBeijing, shanghai), request?.points)
        assertNull(
            controller.update(
                listOf(beijing, shanghai),
                MapScope.SINGLE_DAY,
                listOf(beijing, changedBeijing, shanghai),
            ),
        )
    }

    @Test fun `tab sheet and ordinary recomposition do not emit`() {
        val controller = initializedController()
        repeat(3) { assertNull(controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai))) }
        assertEquals(1L, controller.currentRequest?.id)
    }

    @Test fun `search focus emits one point with fixed zoom`() {
        val controller = initializedController()
        val request = controller.focusSearchResult(shanghai)
        assertEquals(2L, request.id)
        assertEquals(ViewportReason.SEARCH_FOCUS, request.reason)
        assertEquals(listOf(shanghai), request.points)
        assertEquals(SEARCH_FOCUS_ZOOM, request.singlePointZoom)
    }

    @Test fun `empty visible point list emits no request`() {
        val controller = initializedController()
        assertNull(controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, emptyList()))
        assertNull(controller.currentRequest)
    }

    @Test fun `empty automatic fit does not clear explicit search focus`() {
        val controller = MapViewportController()
        val focus = controller.focusSearchResult(shanghai)

        assertNull(controller.update(emptyList(), MapScope.PLACE_POOL, emptyList()))

        assertEquals(focus, controller.currentRequest)
    }

    private fun initializedController() = MapViewportController().apply {
        update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai))
    }
}
