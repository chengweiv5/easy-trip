package com.yangchengwei.easytrip.assistant.data

import androidx.room.*
import com.yangchengwei.easytrip.trip.data.TripEntity

@Entity(tableName = "assistant_trip_destinations", foreignKeys = [
    ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)])
data class GuideDestinationEntity(@PrimaryKey val tripId: String, val cities: String)

@Entity(tableName = "assistant_guide_handoffs", indices = [Index(value = ["tripId"], unique = true)], foreignKeys = [
    ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)])
data class GuideHandoffEntity(
    @PrimaryKey val operationId: String, val tripId: String, val digest: String,
    val materialId: String, val materialDigest: String, val status: String, val attempt: Int,
    val resultRef: String?,
)

/** Kept after trip deletion so a stale command can never create the trip again. */
@Entity(tableName = "assistant_guide_creation_receipts")
data class GuideCreationReceiptEntity(@PrimaryKey val operationId: String, val tripId: String, val digest: String)

@Dao
interface GuideCreationDao {
    @Query("SELECT * FROM assistant_guide_creation_receipts WHERE operationId=:id") suspend fun receipt(id: String): GuideCreationReceiptEntity?
    @Query("SELECT * FROM assistant_trip_destinations WHERE tripId=:id") suspend fun destination(id: String): GuideDestinationEntity?
    @Query("SELECT * FROM assistant_guide_handoffs WHERE tripId=:id") suspend fun forTrip(id: String): GuideHandoffEntity?
    @Query("SELECT * FROM assistant_guide_handoffs WHERE operationId=:id") suspend fun handoff(id: String): GuideHandoffEntity?
    @Query("SELECT materialId FROM assistant_guide_handoffs WHERE status != 'FINISHED'") suspend fun referencedMaterials(): List<String>
    @Insert suspend fun insertReceipt(value: GuideCreationReceiptEntity)
    @Insert suspend fun insertDestination(value: GuideDestinationEntity)
    @Insert suspend fun insertHandoff(value: GuideHandoffEntity)
    @Query("UPDATE assistant_guide_handoffs SET status='CLAIMED' WHERE operationId=:id AND digest=:digest AND attempt=:attempt AND status='PENDING'")
    suspend fun claim(id: String, digest: String, attempt: Int): Int
    @Query("UPDATE assistant_guide_handoffs SET status='PENDING', attempt=attempt+1, resultRef=NULL WHERE operationId=:id AND attempt=:attempt AND status IN ('CLAIMED','IMPORTED','FAILED','CANCELLED')")
    suspend fun retry(id: String, attempt: Int): Int
    @Query("UPDATE assistant_guide_handoffs SET status=:status, resultRef=:resultRef WHERE operationId=:id AND attempt=:attempt AND status IN ('CLAIMED','IMPORTED','FAILED','CANCELLED')")
    suspend fun complete(id: String, attempt: Int, status: String, resultRef: String?): Int
}
