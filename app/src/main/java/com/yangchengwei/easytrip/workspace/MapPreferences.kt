package com.yangchengwei.easytrip.workspace

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MapPreferences {
    val layer: StateFlow<MapLayer>
    fun setLayer(layer: MapLayer)
}

class InMemoryMapPreferences(initial: MapLayer = MapLayer.STANDARD) : MapPreferences {
    private val mutableLayer = MutableStateFlow(initial)
    override val layer: StateFlow<MapLayer> = mutableLayer.asStateFlow()
    override fun setLayer(layer: MapLayer) { mutableLayer.value = layer }
}

class SharedPreferencesMapPreferences(
    private val preferences: SharedPreferences,
) : MapPreferences {
    private val mutableLayer = MutableStateFlow(readLayer())
    override val layer: StateFlow<MapLayer> = mutableLayer.asStateFlow()

    override fun setLayer(layer: MapLayer) {
        preferences.edit().putString(KEY_LAYER, layer.token()).apply()
        mutableLayer.value = layer
    }

    private fun readLayer(): MapLayer {
        val layer = when (preferences.getString(KEY_LAYER, null)) {
            TOKEN_STANDARD, LEGACY_STANDARD -> MapLayer.STANDARD
            TOKEN_SATELLITE -> MapLayer.SATELLITE
            TOKEN_SATELLITE_ROAD, LEGACY_SATELLITE, LEGACY_SATELLITE_ROAD -> MapLayer.SATELLITE_ROAD
            else -> MapLayer.STANDARD
        }
        if (preferences.getString(KEY_LAYER, null) in setOf(LEGACY_STANDARD, LEGACY_SATELLITE, LEGACY_SATELLITE_ROAD)) {
            preferences.edit().putString(KEY_LAYER, layer.token()).apply()
        }
        return layer
    }

    private fun MapLayer.token(): String = when (this) {
        MapLayer.STANDARD -> TOKEN_STANDARD
        MapLayer.SATELLITE -> TOKEN_SATELLITE
        MapLayer.SATELLITE_ROAD -> TOKEN_SATELLITE_ROAD
    }

    companion object {
        private const val KEY_LAYER = "map.layer"
        private const val TOKEN_STANDARD = "standard"
        private const val TOKEN_SATELLITE = "satellite"
        private const val TOKEN_SATELLITE_ROAD = "satellite_road"
        private const val LEGACY_STANDARD = "STANDARD"
        private const val LEGACY_SATELLITE = "SATELLITE"
        private const val LEGACY_SATELLITE_ROAD = "SATELLITE_ROAD"
    }
}
