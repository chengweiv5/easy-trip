package com.yangchengwei.easytrip.expense

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ExpenseCancellationUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
    private val store = ViewModelStore()

    @After fun close() {
        compose.runOnIdle { store.clear() }
        db.close()
    }

    @Test fun cancelButtonAndBackDismissalNeverShowACancellationBanner() {
        val prompter = ExpenseRemovalPrompter()
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val items = RoomItineraryRepository(
            db, db.itineraryEditingDao(), db.routeLegDao(),
            confirmExpenseRemoval = prompter::confirm,
        )
        val routes = RoomRouteLegRepository(db.routeLegDao())
        val (trip, day, ids) = runBlocking {
            val trip = trips.createTrip(CreateTrip("取消提示回归", 1))
            val day = trips.observeTrip(trip).first()!!.days.single().id
            listOf("a", "b").forEachIndexed { index, id ->
                db.savedPlaceDao().insertPlace(
                    SavedPlaceEntity(id, trip, id, "地点 $id", "地址", 30.0 + index, 120.0),
                )
            }
            val ids = listOf(items.addItem(day, "a", 0), items.addItem(day, "b", 1))
            items.saveSingleExpenseForTest(db, ids.first(), null, null, null, 8000)
            val leg = routes.observeDay(day).first().single()
            routes.updateDetailsWithExpense(leg.id, null, null, null, true, 3650)
            Triple(trip, day, ids)
        }
        lateinit var model: DayItineraryViewModel
        compose.runOnIdle {
            model = DayItineraryViewModel(trip, trips, items, routes, null)
            store.put("itinerary", model)
        }
        compose.setContent {
            val state by model.state.collectAsState()
            EasyTripTheme {
                DayItineraryContent(state, onAction = model::dispatch)
                ExpenseRemovalDialog(prompter)
            }
        }
        compose.waitUntil(10_000) { model.state.value.items.size == 2 }
        val before = runBlocking { items.observeDay(day).first() }

        repeat(2) { attempt ->
            compose.runOnIdle {
                model.previewMove(ids.last(), 0)
                model.commitMove(ids.last(), 0)
            }
            compose.waitUntil(10_000) { prompter.pending.value != null }
            if (attempt == 0) compose.onNodeWithText("取消调整").performClick()
            else pressBack()
            compose.waitUntil(10_000) {
                prompter.pending.value == null && model.state.value.previewOrder == ids
            }
            compose.onAllNodesWithText("已取消", substring = true).assertCountEquals(0)
            compose.onAllNodesWithText("操作失败", substring = true).assertCountEquals(0)
            compose.runOnIdle { assertNull(model.state.value.error) }
            runBlocking {
                assertEquals(before, items.observeDay(day).first())
                assertEquals(11650L, trips.observeTrips().first().single().expenseCents)
            }
        }
    }
}
