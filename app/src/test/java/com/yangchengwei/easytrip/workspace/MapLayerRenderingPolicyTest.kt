package com.yangchengwei.easytrip.workspace

import com.amap.api.maps.AMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapLayerRenderingPolicyTest {
    @Test fun `standard satellite and satellite road layers are available`() {
        assertEquals(
            listOf(MapLayer.STANDARD, MapLayer.SATELLITE, MapLayer.SATELLITE_ROAD),
            MapLayer.entries,
        )
    }

    @Test fun `each layer maps to its AMap type and map text visibility`() {
        assertEquals(MapLayerRendering(AMap.MAP_TYPE_NORMAL, true), mapLayerRendering(null, MapLayer.STANDARD))
        assertEquals(MapLayerRendering(AMap.MAP_TYPE_SATELLITE, false), mapLayerRendering(null, MapLayer.SATELLITE))
        assertEquals(MapLayerRendering(AMap.MAP_TYPE_SATELLITE, true), mapLayerRendering(null, MapLayer.SATELLITE_ROAD))
    }

    @Test fun `each layer exposes its Pencil label and description`() {
        assertEquals("标准地图", MapLayer.STANDARD.label())
        assertEquals("道路、建筑和地点信息", MapLayer.STANDARD.description())
        assertEquals("卫星地图", MapLayer.SATELLITE.label())
        assertEquals("仅显示卫星影像", MapLayer.SATELLITE.description())
        assertEquals("卫星路网", MapLayer.SATELLITE_ROAD.label())
        assertEquals("卫星影像叠加道路", MapLayer.SATELLITE_ROAD.description())
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

    @Test fun `rollback failure marks applied layer unknown so retained layer is reapplied`() {
        val updates = mutableListOf<MapLayerRendering>()
        var attempt = 0
        val controller = MapLayerApplicationController { rendering ->
            updates += rendering
            attempt++
            if (attempt == 2 || attempt == 3) error("SDK update failed")
        }

        assertNull(controller.apply(MapLayer.STANDARD))
        val failure = controller.apply(MapLayer.SATELLITE_ROAD)
        assertNull(controller.apply(MapLayer.STANDARD))

        assertEquals(MapLayer.STANDARD, failure?.retainedLayer)
        assertEquals(
            listOf(
                MapLayerRendering(AMap.MAP_TYPE_NORMAL, true),
                MapLayerRendering(AMap.MAP_TYPE_SATELLITE, true),
                MapLayerRendering(AMap.MAP_TYPE_NORMAL, true),
                MapLayerRendering(AMap.MAP_TYPE_NORMAL, true),
            ),
            updates,
        )
    }
}
