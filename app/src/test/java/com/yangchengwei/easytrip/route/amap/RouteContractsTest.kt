package com.yangchengwei.easytrip.route.amap

import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.route.domain.RouteMode
import com.yangchengwei.easytrip.route.domain.RouteRequest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class RouteContractsTest {
    @Test fun transitRequiresOriginCityAsStructuredFailure() {
        val request = RouteRequest(GeoPoint(1.0, 2.0), GeoPoint(3.0, 4.0), RouteMode.TRANSIT)
        val error = assertThrows(AmapServiceException::class.java) { validateRouteRequest(request) }
        assertEquals("TRANSIT_ARGUMENT", error.operation)
    }

    @Test fun crossCityDestinationIsRetained() {
        val request = RouteRequest(GeoPoint(1.0, 2.0), GeoPoint(3.0, 4.0), RouteMode.TRANSIT, "北京", "上海")
        assertEquals("上海", validateRouteRequest(request).destinationCity)
    }

    @Test fun firstUsablePathIsSelected() {
        val selected = selectUsablePath(listOf(RoutePathData(0, 0, emptyList()), RoutePathData(10, 20, listOf(GeoPoint(1.0, 2.0), GeoPoint(3.0, 4.0)))))
        assertEquals(10, selected.distanceMeters)
    }

    @Test fun routeParseCancellationExceptionPropagatesUnchanged() {
        val cancellation = CancellationException("parse cancelled")
        val error = assertThrows(CancellationException::class.java) {
            parseRouteResult("WALK_ROUTE", 1000) { throw cancellation }
        }
        assertSame(cancellation, error)
    }

    @Test fun emptyPathsAreStructuredFailure() {
        assertThrows(AmapServiceException::class.java) { selectUsablePath(emptyList()) }
    }
}
