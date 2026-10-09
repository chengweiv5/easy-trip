package com.yangchengwei.easytrip.expense

import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeRecords(): Flow<List<ExpenseRecord>>
    suspend fun updatePlaceExpense(itemId: String, expected: ExpenseRecord, value: PlaceExpenseInput)
    suspend fun deletePlaceExpense(itemId: String, expected: ExpenseRecord)
}
