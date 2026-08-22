package com.yangchengwei.easytrip.workspace

import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapViewportRenderingPolicyTest {
    private val beijing = GeoPoint(39.9, 116.4)
    private val shanghai = GeoPoint(31.2, 121.5)

    @Test fun `single point uses zoom 15 and consumes request id`() {
        val result = viewportRendering(null, request(1, listOf(beijing)))
        assertEquals(1L, result.consumedRequestId)
        assertEquals(ViewportCommand.SinglePoint(beijing, 15f), result.command)
    }

    @Test fun `multiple points use bounds with padding`() {
        val result = viewportRendering(null, request(2, listOf(beijing, shanghai)))
        assertEquals(ViewportCommand.Bounds(listOf(beijing, shanghai), 96), result.command)
    }

    @Test fun `zero points are a no-op but consume request id`() {
        val result = viewportRendering(null, request(3, emptyList()))
        assertEquals(3L, result.consumedRequestId)
        assertNull(result.command)
    }

    @Test fun `same request id is not consumed twice`() {
        assertNull(viewportRendering(4L, request(4, listOf(beijing))).command)
        assertEquals(4L, viewportRendering(4L, request(4, listOf(beijing))).consumedRequestId)
    }

    private fun request(id: Long, points: List<GeoPoint>) = MapViewportRequest(
        id = id,
        reason = ViewportReason.INITIAL,
        points = points,
    )
}
