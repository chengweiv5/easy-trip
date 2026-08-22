package com.yangchengwei.easytrip.route.domain
import com.yangchengwei.easytrip.core.model.GeoPoint
import org.junit.Assert.*
import org.junit.Test
class PolylineCodecTest { @Test fun `versioned polyline round trips`() { val v=listOf(GeoPoint(1.2,3.4),GeoPoint(-5.6,7.8)); assertEquals(v,PolylineCodec.decode(PolylineCodec.encode(v)).getOrThrow()) }; @Test fun `damaged polyline fails decode`() { assertTrue(PolylineCodec.decode("broken").isFailure) } }
