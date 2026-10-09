package com.yangchengwei.easytrip.expense

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.expense.data.RoomExpenseRepository
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class ExpensePeriodDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),EasyTripDatabase::class.java).build()
    @After fun close() = db.close()
    @Test fun cancelLeavesOriginalPeriodAndConfirmRefreshesAnnualReview() {
        val prompter = ExpensePeriodPrompter()
        val trips = RoomTripRepository(db.tripDao(),database=db,confirmExpensePeriod=prompter::confirm)
        val expenses = RoomExpenseRepository(db)
        val id = runBlocking {
            val id = trips.createTrip(CreateTrip("跨年旅行",1,startDate=LocalDate.of(2025,12,31)))
            val day = trips.observeTrip(id).first()!!.days.single().id
            db.savedPlaceDao().insertPlace(SavedPlaceEntity("p",id,"p","晚餐","地址",30.0,120.0))
            val items = RoomItineraryRepository(db,db.itineraryEditingDao(),db.routeLegDao())
            val item = items.addItem(day,"p",0)
            items.saveDetailsWithExpenses(item,null,null,null,emptyList(),listOf(PlaceExpenseInput(null,30000,ExpenseCategory.FOOD,null)))
            id
        }
        val model = com.yangchengwei.easytrip.expense.ui.ExpenseReviewViewModel(expenses)
        model.selectPeriod(ExpensePeriod.Year(2025))
        compose.setContent { EasyTripTheme {
            com.yangchengwei.easytrip.expense.ui.ExpenseReviewScreen(model,onBack={},onOpenSource={})
            ExpensePeriodDialog(prompter)
        } }
        fun changeDate() { compose.activity.lifecycleScope.launch { try { trips.setStartDate(id,LocalDate.of(2026,1,1)) } catch (_: ExpenseRemovalCancelled) { } } }
        compose.runOnIdle { changeDate() }
        compose.waitUntil(5000) { prompter.pending.value != null }
        compose.onNodeWithText("花费年月归属将变化").assertIsDisplayed()
        compose.onNodeWithTag("expense-period-cancel").performClick()
        compose.waitUntil(5000) { prompter.pending.value == null }
        assertEquals(LocalDate.of(2025,12,31), runBlocking { trips.observeTrip(id).first()!!.startDate })
        compose.runOnIdle { changeDate() }
        compose.waitUntil(5000) { prompter.pending.value != null }
        compose.onNodeWithTag("expense-period-confirm").performClick()
        compose.waitUntil(5000) { runBlocking { expenses.observeRecords().first().single().date == LocalDate.of(2026,1,1) } }
        compose.onNodeWithTag("expense-total").assertTextEquals("—")
    }
}
