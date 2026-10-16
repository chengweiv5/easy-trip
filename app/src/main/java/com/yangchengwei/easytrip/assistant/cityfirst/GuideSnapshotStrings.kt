package com.yangchengwei.easytrip.assistant.cityfirst

import kotlinx.serialization.json.*

/** Lossless v2 storage dictionary. Only repeated string values are interned, never model structures. */
internal object GuideSnapshotStrings {
    private const val REF = "_guideString"
    fun pack(root: JsonObject): JsonObject {
        val counts = linkedMapOf<String, Int>()
        fun count(v: JsonElement) {
            when (v) {
                is JsonObject -> v.values.forEach(::count)
                is JsonArray -> v.forEach(::count)
                is JsonPrimitive -> if (v.isString && v.content.toByteArray().size >= 24)
                    counts[v.content] = (counts[v.content] ?: 0) + 1
            }
        }
        count(root)
        val values = counts.filterValues { it > 1 }.keys.toList()
        val indexes = values.withIndex().associate { it.value to it.index }
        fun rewrite(v: JsonElement): JsonElement = when (v) {
            is JsonObject -> JsonObject(v.mapValues { rewrite(it.value) })
            is JsonArray -> JsonArray(v.map(::rewrite))
            is JsonPrimitive -> if (v.isString && v.content in indexes)
                buildJsonObject { put(REF, indexes.getValue(v.content)) } else v
        }
        return JsonObject(rewrite(root).jsonObject + ("stringTable" to JsonArray(values.map(::JsonPrimitive))))
    }
    fun unpack(root: JsonObject): JsonObject {
        val table = root["stringTable"] ?: return root
        require(root.getValue("version").jsonPrimitive.int in 2..3)
        val values = table.jsonArray
        require(values.size <= 10_000 && values.all { it is JsonPrimitive && it.isString && it.content.length <= 20_000 })
        var expandedBytes = 0L
        fun rewrite(v: JsonElement, depth: Int): JsonElement {
            require(depth <= 32)
            return when (v) {
                is JsonObject -> if (v.size == 1 && REF in v) {
                    val index = v.getValue(REF).jsonPrimitive
                    require(!index.isString)
                    val value = values[index.int]
                    expandedBytes += value.jsonPrimitive.content.toByteArray().size
                    require(expandedBytes <= 8L * 1024 * 1024)
                    value
                } else JsonObject(v.mapValues { rewrite(it.value, depth + 1) })
                is JsonArray -> JsonArray(v.map { rewrite(it, depth + 1) })
                is JsonPrimitive -> v
            }
        }
        return rewrite(JsonObject(root - "stringTable"), 0).jsonObject
    }
}
