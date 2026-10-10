package com.yangchengwei.easytrip.assistant.data

import androidx.room.withTransaction
import com.yangchengwei.easytrip.assistant.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import java.util.UUID
import kotlinx.serialization.json.*

class RoomPlaceImport(
    private val database: EasyTripDatabase,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : PlaceImport {
    override suspend fun latestReceipt(tripId: String): ImportReceipt? = database.importReceiptDao().latest(tripId)?.decode()

    override suspend fun receipt(operationId: String): ImportReceipt? =
        database.importReceiptDao().receipt(operationId)?.decode()

    override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = database.withTransaction {
        require(confirmation.operationId.isNotBlank())
        require(ImportConfirmation.create(confirmation.tripId, confirmation.items).digest == confirmation.digest)
        receipt(confirmation.operationId)?.let {
            require(it.tripId == confirmation.tripId && it.digest == confirmation.digest) { "确认内容已变化" }
            return@withTransaction it
        }
        require(database.tripDao().trip(confirmation.tripId) != null) { "旅行已不存在" }
        val places = database.savedPlaceDao()
        val results = confirmation.items.map { item ->
            val poi = item.poi
            require(poi.validPosition()) { "地点缺少有效坐标" }
            val point = requireNotNull(poi.point)
            val existing = places.placeId(confirmation.tripId, poi.poiId)
            if (existing != null) ImportItemResult(item.itemId, poi.poiId, existing, false)
            else {
                val id = idFactory()
                val inserted = places.insertPlace(SavedPlaceEntity(
                    id, confirmation.tripId, poi.poiId, poi.name, poi.address, point.latitude, point.longitude,
                    cityCode = poi.cityCode, cityName = poi.cityName, cityAdCode = poi.cityAdCode,
                    cityMetadataVersion = poi.cityMetadataVersion, category = item.category.storageKey,
                ))
                if (inserted == -1L) ImportItemResult(item.itemId, poi.poiId,
                    requireNotNull(places.placeId(confirmation.tripId, poi.poiId)), false)
                else ImportItemResult(item.itemId, poi.poiId, id, true)
            }
        }
        val result = ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest, results)
        database.importReceiptDao().insert(ImportReceiptEntity(result.operationId, result.tripId, result.digest,
            buildJsonArray {
                results.forEach { r -> addJsonObject {
                    put("itemId", r.itemId); put("poiId", r.poiId); put("placeId", r.placeId); put("added", r.added)
                } }
            }.toString()))
        result
    }

    private fun ImportReceiptEntity.decode() = ImportReceipt(operationId, tripId, digest,
        Json.parseToJsonElement(resultsJson).jsonArray.map { element ->
            val row = element.jsonObject
            ImportItemResult(row.getValue("itemId").jsonPrimitive.content, row.getValue("poiId").jsonPrimitive.content,
                row.getValue("placeId").jsonPrimitive.content, row.getValue("added").jsonPrimitive.boolean)
        })
}
