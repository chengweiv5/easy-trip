package com.yangchengwei.easytrip.workspace

import com.amap.api.maps.AMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapLayerRenderingPolicyTest {
    @Test fun `only standard and satellite road layers are available`() {
        assertEquals(listOf(MapLayer.STANDARD, MapLayer.SATELLITE_ROAD), MapLayer.entries)
    }

    @Test fun `each layer maps to its AMap type and map text visibility`() {
        assertEquals(MapLayerRendering(AMap.MAP_TYPE_NORMAL, true), mapLayerRendering(null, MapLayer.STANDARD))
        assertEquals(MapLayerRendering(AMap.MAP_TYPE_SATELLITE, true), mapLayerRendering(null, MapLayer.SATELLITE_ROAD))
    }

    @Test fun `repeated layer produces no SDK update`() {
        MapLayer.entries.forEach { layer -> assertNull(mapLayerRendering(layer, layer)) }
    }

    @Test fun `changing layer produces the requested SDK update`() {
        assertEquals(
            MapLayerRendering(AMap.MAP_TYPE_SATELLITE, true),
            mapLayerRendering(MapLayer.STANDARD, MapLayer.SATELLITE_ROAD),
        )
    }
}
