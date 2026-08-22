package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapViewportControllerTest {
    private val beijing = GeoPoint(39.9, 116.4)
    private val shanghai = GeoPoint(31.2, 121.5)
    private val changedBeijing = GeoPoint(39.91, 116.4)

    @Test fun `first nonempty place set emits initial request`() {
        val controller = MapViewportController()
        assertNull(controller.update(emptyList(), MapScope.PLACE_POOL, emptyList()))
        val request = controller.update(listOf(beijing, shanghai), MapScope.PLACE_POOL, listOf(beijing, shanghai))
        assertEquals(1L, request?.id)
        assertEquals(ViewportReason.INITIAL, request?.reason)
        assertEquals(listOf(beijing, shanghai), request?.points)
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

    @Test fun `switching scope emits visible points`() {
        val controller = initializedController()
        val request = controller.update(listOf(beijing, shanghai), MapScope.SINGLE_DAY, listOf(shanghai))
        assertEquals(2L, request?.id)
        assertEquals(ViewportReason.SCOPE_CHANGED, request?.reason)
        assertEquals(listOf(shanghai), request?.points)
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
