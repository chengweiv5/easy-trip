package com.yangchengwei.easytrip.expense

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.expense.data.RoomExpenseRepository
import com.yangchengwei.easytrip.expense.ui.*
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class ExpenseReviewNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), EasyTripDatabase::class.java).build()
    private val trips = RoomTripRepository(db.tripDao(), database = db)
    private val items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao())
    private val expenses = RoomExpenseRepository(db)
    @After fun close() = db.close()
    @Test fun productionRootNavigationNeedsNoTripOrMapAndPreservesSelectedYear() {
        var mapCreated = 0
        compose.setContent { EasyTripTheme {
            com.yangchengwei.easytrip.AppNavigation(com.yangchengwei.easytrip.trip.domain.TripService(trips), trips,
                com.yangchengwei.easytrip.trip.ui.RoomDeleteImpactProvider(db.deleteImpactDao()),
                dependencies = com.yangchengwei.easytrip.AppNavigationDependencies(
                    com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository(db), items,
                    com.yangchengwei.easytrip.route.data.RoomRouteLegRepository(db.routeLegDao()),
                    com.yangchengwei.easytrip.workspace.InMemoryMapPreferences(),
                    com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore(), expenseRepository = expenses),
                mapHostFactory = { mapCreated++; error("花费入口不应创建地图") })
        } }
        compose.onNodeWithTag("primary-expenses").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("expense-period-previous").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("expense-period-previous").performClick()
        compose.onNodeWithTag("expense-choose-year").assertTextContains("2025年", substring = true)
        compose.onNodeWithTag("primary-trips").performClick()
        compose.onNodeWithTag("primary-expenses").performClick()
        compose.onNodeWithTag("expense-choose-year").assertTextContains("2025年", substring = true)
        compose.onNodeWithTag("expense-total").assertTextEquals("—")
        assertEquals(0, mapCreated)
    }

    @Test fun undatedZeroExpenseIsRealRecordAndCanBeDeletedAtLargeFont() {
        runBlocking {
            val id = trips.createTrip(CreateTrip("未定日期旅行",1))
            val day = trips.observeTrip(id).first()!!.days.single().id
            db.savedPlaceDao().insertPlace(SavedPlaceEntity("park",id,"park","公园","地址",30.0,120.0))
            val item = items.addItem(day,"park",0)
            items.saveDetailsWithExpenses(item,null,null,null,emptyList(),listOf(PlaceExpenseInput(null,0,ExpenseCategory.ATTRACTION,null)))
        }
        val model = ExpenseReviewViewModel(expenses)
        compose.setContent { EasyTripTheme {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                ExpenseReviewScreen(model, onBack = {}, onOpenSource = {})
            }
        } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("review-undated").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("review-undated").performScrollTo().performClick()
        compose.onNodeWithTag("expense-total").assertTextEquals("¥0.00")
        compose.onNodeWithTag("expense-record-edit").performScrollTo().performClick()
        compose.onNodeWithTag("expense-record-delete").performScrollTo().performClick()
        compose.onNodeWithTag("expense-confirm-delete").performClick()
        compose.waitUntil(5000) { runBlocking { expenses.observeRecords().first().isEmpty() } }
        compose.onNodeWithText("尚无费用记录").assertExists()
    }

    @Test fun yearCategoryEditRefreshesSameScopeAndBackPreservesYear() {
        val trip = runBlocking {
            val id = trips.createTrip(CreateTrip("杭州", 1, startDate = LocalDate.of(2025,4,12)))
            val day = trips.observeTrip(id).first()!!.days.single().id
            db.savedPlaceDao().insertPlace(SavedPlaceEntity("hotel", id, "hotel", "湖畔酒店", "地址", 30.0, 120.0))
            val item = items.addItem(day, "hotel", 0)
            items.saveDetailsWithExpenses(item, LocalTime.NOON, 60, "地点备注", emptyList(), listOf(PlaceExpenseInput(null,6800,ExpenseCategory.FOOD,"晚餐")))
            id
        }
        val model = ExpenseReviewViewModel(expenses, Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC))
        compose.setContent { EasyTripTheme { ExpenseReviewScreen(model, onBack = {}, onOpenSource = {}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("expense-period-previous").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("expense-period-previous").performClick()
        compose.onNodeWithTag("review-category-food").performScrollTo().performClick()
        compose.onNodeWithTag("expense-record-edit").performScrollTo().performClick()
        compose.onNodeWithTag("expense-amount-record").performTextReplacement("80")
        compose.onNodeWithTag("expense-category-record-lodging").performClick()
        compose.onNodeWithTag("expense-record-save").performClick()
        compose.waitUntil(5000) { runBlocking { expenses.observeRecords().first().single().cents == 8000L } }
        compose.onNodeWithText("尚无费用记录").assertExists()
        compose.onNodeWithTag("expense-back").performClick()
        compose.onNodeWithTag("expense-choose-year").assertTextContains("2025年", substring = true)
        compose.onNodeWithTag("expense-choose-year").performScrollTo()
        compose.onRoot().captureToImage().let { image ->
            val bitmap = image.asAndroidBitmap()
            java.io.File(compose.activity.getExternalFilesDir(null), "v200-expense-review.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        }
        assertEquals(trip, runBlocking { expenses.observeRecords().first().single().tripId })
    }
}
