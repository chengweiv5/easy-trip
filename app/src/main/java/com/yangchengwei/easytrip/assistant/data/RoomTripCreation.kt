package com.yangchengwei.easytrip.assistant.data

import androidx.room.*
import com.yangchengwei.easytrip.assistant.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.util.UUID
import kotlinx.serialization.json.*

/** Intentionally no trip foreign key: deletion must not turn a committed operation into a new one. */
@Entity(tableName = "assistant_trip_creation_receipts")
data class TripCreationReceiptEntity(
    @PrimaryKey val operationId: String,
    val tripId: String,
    val digest: String,
    val resultsJson: String,
)

@Dao
interface TripCreationReceiptDao {
    @Query("SELECT * FROM assistant_trip_creation_receipts WHERE operationId=:id")
    suspend fun receipt(id: String): TripCreationReceiptEntity?
    @Insert suspend fun insert(value: TripCreationReceiptEntity)
}

class RoomTripCreation(
    private val database: EasyTripDatabase,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : TripCreation {
    override suspend fun recover(command: TripCreationCommand): TripCreationRecovery = database.withTransaction {
        command.validated()
        val previous = database.tripCreationReceiptDao().receipt(command.operationId)
            ?: return@withTransaction TripCreationRecovery.NotSubmitted
        require(previous.digest == command.digest && previous.tripId == command.tripId) { "操作内容与回执不一致" }
        if (database.tripDao().trip(command.tripId) == null) TripCreationRecovery.Deleted
        else TripCreationRecovery.Committed(previous.decode())
    }

    override suspend fun commit(command: TripCreationCommand): ImportReceipt = database.withTransaction {
        when (val recovery = recover(command)) {
            TripCreationRecovery.Deleted -> error("这次创建的旅行已被删除，不会重复创建")
            is TripCreationRecovery.Committed -> return@withTransaction recovery.receipt
            TripCreationRecovery.NotSubmitted -> Unit
        }
        require(database.tripDao().trip(command.tripId) == null) { "旅行标识已存在，请重新确认" }
        RoomTripRepository(database.tripDao(), idFactory = idFactory, database = database).createTrip(
            CreateTrip(command.name, command.dayCount, command.travelMode, command.startDate, command.tripId))
        // Keep the old receipt format and digest unchanged; the creation receipt has its own namespace.
        val imported = RoomPlaceImport(database, idFactory).commit(
            ImportConfirmation.create(command.tripId, command.items, "places:${command.operationId}"))
        val result = imported.copy(operationId = command.operationId, digest = command.digest)
        database.tripCreationReceiptDao().insert(TripCreationReceiptEntity(
            result.operationId, result.tripId, result.digest, buildJsonArray {
                result.results.forEach { r -> addJsonObject {
                    put("itemId", r.itemId); put("poiId", r.poiId); put("placeId", r.placeId); put("added", r.added)
                } }
            }.toString()))
        result
    }

    private fun TripCreationReceiptEntity.decode() = ImportReceipt(operationId, tripId, digest,
        Json.parseToJsonElement(resultsJson).jsonArray.map {
            val r = it.jsonObject
            ImportItemResult(r.getValue("itemId").jsonPrimitive.content, r.getValue("poiId").jsonPrimitive.content,
                r.getValue("placeId").jsonPrimitive.content, r.getValue("added").jsonPrimitive.boolean)
        })
}
