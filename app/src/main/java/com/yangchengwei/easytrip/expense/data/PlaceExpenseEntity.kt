package com.yangchengwei.easytrip.expense.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yangchengwei.easytrip.expense.ExpenseCategory
import com.yangchengwei.easytrip.expense.PlaceExpenseInput
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity

@Entity(
    tableName = "place_expenses",
    foreignKeys = [ForeignKey(
        entity = ItineraryItemEntity::class, parentColumns = ["id"],
        childColumns = ["itineraryItemId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("itineraryItemId")],
)
data class PlaceExpenseEntity(
    @PrimaryKey val id: String,
    val itineraryItemId: String,
    val cents: Long,
    val category: String?,
    val note: String?,
    val position: Int,
) {
    init {
        require(id.isNotBlank() && itineraryItemId.isNotBlank())
        require(cents >= 0 && position >= 0)
        ExpenseCategory.fromStorageKey(category)
    }

    fun input() = PlaceExpenseInput(id, cents, ExpenseCategory.fromStorageKey(category), note)
}
