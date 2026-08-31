package com.yangchengwei.easytrip.workspace

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class MapPreferencesTest {
    @Test fun `missing value defaults to standard`() {
        assertEquals(MapLayer.STANDARD, SharedPreferencesMapPreferences(MemoryPreferences()).layer.value)
    }

    @Test fun `all layer values are written as stable lowercase tokens and emitted`() {
        val storage = MemoryPreferences()
        val preferences = SharedPreferencesMapPreferences(storage)
        val tokens = mapOf(
            MapLayer.STANDARD to "standard",
            MapLayer.SATELLITE to "satellite",
            MapLayer.SATELLITE_ROAD to "satellite_road",
        )

        tokens.forEach { (layer, token) ->
            preferences.setLayer(layer)
            assertEquals(layer, preferences.layer.value)
            assertEquals(token, storage.getString("map.layer", null))
        }
    }

    @Test fun `lowercase stored tokens restore matching layers`() {
        mapOf(
            "standard" to MapLayer.STANDARD,
            "satellite" to MapLayer.SATELLITE,
            "satellite_road" to MapLayer.SATELLITE_ROAD,
        ).forEach { (token, layer) ->
            assertEquals(layer, SharedPreferencesMapPreferences(MemoryPreferences(mutableMapOf("map.layer" to token))).layer.value)
        }
    }

    @Test fun `legacy uppercase values migrate to compatible lowercase tokens`() {
        mapOf(
            "STANDARD" to (MapLayer.STANDARD to "standard"),
            "SATELLITE" to (MapLayer.SATELLITE_ROAD to "satellite_road"),
            "SATELLITE_ROAD" to (MapLayer.SATELLITE_ROAD to "satellite_road"),
        ).forEach { (legacy, expected) ->
            val storage = MemoryPreferences(mutableMapOf("map.layer" to legacy))

            assertEquals(expected.first, SharedPreferencesMapPreferences(storage).layer.value)
            assertEquals(expected.second, storage.getString("map.layer", null))
        }
    }

    @Test fun `new tokens survive recreation exactly`() {
        MapLayer.entries.forEach { layer ->
            val storage = MemoryPreferences()
            SharedPreferencesMapPreferences(storage).setLayer(layer)

            val stored = storage.getString("map.layer", null)
            assertEquals(layer, SharedPreferencesMapPreferences(storage).layer.value)
            assertEquals(stored, storage.getString("map.layer", null))
        }
    }

    @Test fun `reconstruction preserves last layer`() {
        val storage = MemoryPreferences()
        SharedPreferencesMapPreferences(storage).setLayer(MapLayer.SATELLITE_ROAD)

        assertEquals(MapLayer.SATELLITE_ROAD, SharedPreferencesMapPreferences(storage).layer.value)
    }

    @Test fun `invalid stored value falls back to standard`() {
        val storage = MemoryPreferences(mutableMapOf("map.layer" to "TERRAIN"))

        assertEquals(MapLayer.STANDARD, SharedPreferencesMapPreferences(storage).layer.value)
    }

    private class MemoryPreferences(
        private val values: MutableMap<String, Any?> = mutableMapOf(),
    ) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") ((values[key] as? Set<String>)?.toMutableSet() ?: defValues)
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor(values)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class Editor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private var clear = false
            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = values }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = null }
            override fun clear(): SharedPreferences.Editor = apply { clear = true }
            override fun commit(): Boolean { applyChanges(); return true }
            override fun apply() = applyChanges()
            private fun applyChanges() {
                if (clear) values.clear()
                updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
            }
        }
    }
}
