package com.yangchengwei.easytrip.assistant.data

import androidx.room.*
import com.yangchengwei.easytrip.trip.data.TripEntity

@Entity(
    tableName = "assistant_import_receipts",
    foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripId")],
)
data class ImportReceiptEntity(
    @PrimaryKey val operationId: String,
    val tripId: String,
    val digest: String,
    val resultsJson: String,
)

@Dao
interface ImportReceiptDao {
    @Query("SELECT * FROM assistant_import_receipts WHERE operationId=:operationId")
    suspend fun receipt(operationId: String): ImportReceiptEntity?
    @Query("SELECT * FROM assistant_import_receipts WHERE tripId=:tripId ORDER BY rowid DESC LIMIT 1")
    suspend fun latest(tripId: String): ImportReceiptEntity?
    @Insert suspend fun insert(receipt: ImportReceiptEntity)
}
