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
        preferences.edit().putString(KEY_LAYER, layer.name).apply()
        mutableLayer.value = layer
    }

    private fun readLayer(): MapLayer {
        val stored = preferences.getString(KEY_LAYER, null)
        if (stored == LEGACY_SATELLITE) {
            preferences.edit().putString(KEY_LAYER, MapLayer.SATELLITE_ROAD.name).apply()
            return MapLayer.SATELLITE_ROAD
        }
        return MapLayer.entries.firstOrNull { it.name == stored } ?: MapLayer.STANDARD
    }

    companion object {
        private const val KEY_LAYER = "map.layer"
        private const val LEGACY_SATELLITE = "SATELLITE"
    }
}
