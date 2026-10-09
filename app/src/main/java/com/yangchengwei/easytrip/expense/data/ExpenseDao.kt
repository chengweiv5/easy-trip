package com.yangchengwei.easytrip.expense.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.yangchengwei.easytrip.itinerary.data.TripDayReadSnapshot
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import kotlinx.coroutines.flow.Flow

data class ExpenseTripReadSnapshot(
    @Embedded val trip: TripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId", entity = TripDayEntity::class)
    val days: List<TripDayReadSnapshot>,
)

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM trips ORDER BY id")
    fun observeSnapshots(): Flow<List<ExpenseTripReadSnapshot>>

    @Transaction
    @Query("SELECT * FROM trips WHERE id=:tripId")
    suspend fun tripSnapshot(tripId: String): List<ExpenseTripReadSnapshot>

    @Query("UPDATE place_expenses SET cents=:cents,category=:category,note=:note WHERE id=:id AND itineraryItemId=:itemId")
    suspend fun update(id: String, itemId: String, cents: Long, category: String?, note: String?): Int

    @Query("DELETE FROM place_expenses WHERE id=:id AND itineraryItemId=:itemId")
    suspend fun delete(id: String, itemId: String): Int
}
