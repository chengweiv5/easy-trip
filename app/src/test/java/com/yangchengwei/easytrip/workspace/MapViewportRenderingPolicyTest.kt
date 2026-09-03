package com.yangchengwei.easytrip.workspace

import androidx.compose.ui.unit.dp
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

    @Test fun `single point offsets map center away from asymmetric safe insets`() {
        val insets = MapViewportInsets(leftPx = 20, topPx = 72, rightPx = 68, bottomPx = 510)

        val result = viewportRendering(null, request(2, listOf(beijing), insets))

        assertEquals(
            ViewportCommand.SinglePoint(beijing, 15f, ViewportCenterOffset(xPx = 24, yPx = 219)),
            result.command,
        )
    }

    @Test fun `multiple points use bounds with padding`() {
        val result = viewportRendering(null, request(2, listOf(beijing, shanghai)))
        assertEquals(ViewportCommand.Bounds(listOf(beijing, shanghai), MapViewportInsets()), result.command)
    }

    @Test fun `zero points are a no-op but consume request id`() {
        val result = viewportRendering(null, request(3, emptyList()))
        assertEquals(3L, result.consumedRequestId)
        assertNull(result.command)
    }

    @Test fun `bounds preserve asymmetric safe insets`() {
        val insets = MapViewportInsets(leftPx = 24, topPx = 112, rightPx = 56, bottomPx = 428)

        val result = viewportRendering(null, request(4, listOf(beijing, shanghai), insets))

        assertEquals(ViewportCommand.Bounds(listOf(beijing, shanghai), insets), result.command)
    }

    @Test fun `same request id is not consumed twice`() {
        assertNull(viewportRendering(4L, request(4, listOf(beijing))).command)
        assertEquals(4L, viewportRendering(4L, request(4, listOf(beijing))).consumedRequestId)
    }

    @Test fun `same single point request with stable safe insets does not move again`() {
        val insets = MapViewportInsets(leftPx = 20, topPx = 72, rightPx = 68, bottomPx = 510)
        val request = request(5, listOf(beijing), insets)

        val result = viewportRendering(
            consumedRequestId = request.id,
            request = request,
            consumedSafeInsets = insets,
        )

        assertNull(result.command)
    }

    @Test fun `same request id rerenders bounds once when stable sheet anchor changes`() {
        val request = request(5, listOf(beijing, shanghai), MapViewportInsets(leftPx = 20, topPx = 72, rightPx = 68, bottomPx = 510))

        val result = viewportRendering(
            consumedRequestId = 5L,
            request = request,
            consumedSafeInsets = MapViewportInsets(leftPx = 20, topPx = 72, rightPx = 68, bottomPx = 390),
        )

        assertEquals(ViewportCommand.Bounds(request.points, request.safeInsets), result.command)
        assertEquals(request.safeInsets, result.consumedSafeInsets)
    }

    private fun request(
        id: Long,
        points: List<GeoPoint>,
        safeInsets: MapViewportInsets = MapViewportInsets(),
    ) = MapViewportRequest(
        id = id,
        reason = ViewportReason.INITIAL,
        points = points,
        safeInsets = safeInsets,
    )
}
