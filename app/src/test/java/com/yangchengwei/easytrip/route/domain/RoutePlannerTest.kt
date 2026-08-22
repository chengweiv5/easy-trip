package com.yangchengwei.easytrip.route.domain
import com.yangchengwei.easytrip.amap.AmapServiceException
import com.yangchengwei.easytrip.core.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
class RoutePlannerTest { private val leg=RouteLegWithEndpoints("leg",1,RouteStatus.PENDING,GeoPoint(1.0,2.0),GeoPoint(3.0,4.0),"010","021",TransportMode.TRANSIT,null)
 private fun source(error:Throwable)=object:RouteDataSource{override suspend fun plan(request:RouteRequest):RouteResult=throw error}
 @Test fun `unknown exception is permanent`()=runTest { assertEquals(RouteErrorKind.PERMANENT,(DataSourceRoutePlanner(source(IllegalStateException("bad"))).plan(leg) as RoutePlanOutcome.Failure).kind) }
 @Test fun `no usable path is no route`()=runTest { assertEquals(RouteErrorKind.NO_ROUTE,(DataSourceRoutePlanner(source(AmapServiceException("ROUTE_EMPTY",1000,"no usable path"))).plan(leg) as RoutePlanOutcome.Failure).kind) }
}
