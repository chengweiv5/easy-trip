package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.security.MessageDigest
import java.util.UUID

data class ImportItem(val itemId: String, val generation: Int, val poi: PlaceCandidate, val category: PlaceCategory)
data class ImportConfirmation(
    val operationId: String,
    val tripId: String,
    val items: List<ImportItem>,
    val digest: String,
) {
    companion object {
        fun create(tripId: String, items: List<ImportItem>, operationId: String = UUID.randomUUID().toString()): ImportConfirmation {
            require(tripId.isNotBlank() && items.size in 1..20)
            val sorted = items.sortedBy { it.itemId }
            val parts = listOf(tripId) + sorted.flatMap { i ->
                listOf(i.itemId, i.generation.toString(), i.poi.poiId, i.poi.name, i.poi.address,
                    i.poi.point?.latitude.toString(), i.poi.point?.longitude.toString(), i.poi.cityName.orEmpty(),
                    i.poi.cityAdCode.orEmpty(), i.poi.cityCode.orEmpty(), i.poi.cityMetadataVersion.toString(), i.category.storageKey)
            }
            val bytes = parts.joinToString("") { "${it.length}:$it" }.toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            return ImportConfirmation(operationId, tripId, sorted, digest)
        }
    }
}
data class ImportItemResult(val itemId: String, val poiId: String, val placeId: String, val added: Boolean)
data class ImportReceipt(val operationId: String, val tripId: String, val digest: String, val results: List<ImportItemResult>) {
    val addedCount get() = results.count { it.added }
}
interface PlaceImport {
    suspend fun latestReceipt(tripId: String): ImportReceipt? = null
    suspend fun commit(confirmation: ImportConfirmation): ImportReceipt
    suspend fun receipt(operationId: String): ImportReceipt?
}
