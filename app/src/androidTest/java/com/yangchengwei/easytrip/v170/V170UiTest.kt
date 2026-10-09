package com.yangchengwei.easytrip.v170

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.expense.ExpenseSummary
import com.yangchengwei.easytrip.itinerary.ui.*
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.ui.*
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.ui.*
import com.yangchengwei.easytrip.workspace.ItineraryScope
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class V170UiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val days = (0..4).map { TripDay("day-$it", it) }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "v170-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun railLongPressCommitsOnceOnReleaseKeepsSelectionAndHasNoGrip() {
        var ordered by mutableStateOf(days)
        var selected by mutableStateOf<ItineraryScope>(ItineraryScope.Day("day-0"))
        val moves = mutableListOf<Pair<String, Int>>()
        compose.setContent { EasyTripTheme {
            ItineraryScopeRail(ordered, selected, { selected = it }, startDate = LocalDate.of(2026,10,1),
                modifier = Modifier.height(430.dp), onMoveDay = { id, target ->
                    moves += id to target
                    ordered = ordered.toMutableList().apply { add(target, removeAt(indexOfFirst { it.id == id })) }.mapIndexed { i, d -> d.copy(index=i) }
                })
        } }
        val source = compose.onNodeWithTag("itinerary-scope-day-0")
        val destination = compose.onNodeWithTag("itinerary-scope-day-2")
        val delta = destination.fetchSemanticsNode().positionInRoot.y - source.fetchSemanticsNode().positionInRoot.y
        source.performTouchInput {
            down(center); advanceEventTime(650); moveBy(Offset.Zero); advanceEventTime(16)
            moveBy(Offset(0f, delta))
            assertTrue(moves.isEmpty())
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf("day-0" to 2), moves)
            assertEquals(ItineraryScope.Day("day-0"), selected)
            assertEquals(listOf("day-1","day-2","day-0","day-3","day-4"), ordered.map { it.id })
        }
        compose.onNodeWithTag("itinerary-scope-day-0").assertIsSelected().assert(hasText("10/3"))
        compose.onAllNodes(hasTestTag("move-day-handle-day-0")).assertCountEquals(0)
        capture("rail")
    }
    @Test fun railDragAtEdgeScrollsToOffscreenDaysAndCommitsOnRelease() {
        val manyDays = (0 until 20).map { TripDay("day-$it", it) }
        val moves = mutableListOf<Int>()
        compose.setContent { ItineraryScopeRail(manyDays, ItineraryScope.WholeTrip, {},
            modifier=Modifier.height(250.dp), onMoveDay={_, target -> moves += target}) }
        compose.mainClock.autoAdvance = false
        val source = compose.onNodeWithTag("itinerary-scope-day-0")
        val rail = compose.onNodeWithTag("itinerary-scope-rail")
        val bottom = rail.fetchSemanticsNode().boundsInRoot.bottom - 4f
        val from = source.fetchSemanticsNode().boundsInRoot.center.y
        source.performTouchInput { down(center); advanceEventTime(650); moveBy(Offset.Zero); advanceEventTime(16); moveBy(Offset(0f,bottom-from)) }
        compose.mainClock.advanceTimeBy(2000)
        assertTrue(moves.isEmpty())
        rail.performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(1,moves.size); assertTrue(moves.single() > 4) }
    }

    @Test fun railDragCancellationAndTapDoNotReorder() {
        val moves = mutableListOf<Int>()
        var selected by mutableStateOf<ItineraryScope>(ItineraryScope.WholeTrip)
        compose.setContent { ItineraryScopeRail(days, selected, { selected=it }, modifier=Modifier.height(430.dp), onMoveDay={_,i->moves+=i}) }
        compose.onNodeWithTag("itinerary-scope-day-0").performClick()
        compose.runOnIdle { assertEquals(ItineraryScope.Day("day-0"),selected); assertTrue(moves.isEmpty()) }
        compose.onNodeWithTag("itinerary-scope-day-0").performTouchInput { down(center); advanceEventTime(650); moveBy(Offset(0f,120f)); cancel() }
        compose.runOnIdle { assertTrue(moves.isEmpty()) }
    }
    @Test fun savedPoolLocalSearchAndClearDoNotInvokeRemoteSearch() {
        fun row(id:String,name:String,note:String) = SavedPlaceRowUi(SavedPlace(id,"trip",id,name,"杭州",GeoPoint(30.0,120.0),note,emptyList(),"杭州市","330100"),2,true)
        var state by mutableStateOf(PlacePoolUiState(rows=listOf(row("a","西湖","赏月"),row("b","灵隐寺",""))))
        var remote = 0
        compose.setContent { EasyTripTheme { PlacePoolContent(state, showSearch=false, onSearch={remote++}, onAction={ if(it is PlacePoolAction.SetLocalQuery) state=state.copy(localQuery=it.value) }) } }
        compose.onNodeWithTag("place-pool-local-search").performTextInput("赏月")
        compose.onNodeWithText("西湖").assertIsDisplayed()
        compose.onNodeWithText("灵隐寺").assertDoesNotExist()
        compose.onNodeWithTag("place-pool-clear-search").performClick()
        compose.onNodeWithText("灵隐寺").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0,remote) }
        capture("search")
    }
    @Test fun itemExpenseFieldRetainsInvalidInputAndDisablesSave() {
        var draft by mutableStateOf(ItineraryEditDraft("a","08:00","60",placeName="西湖",
            expenses=listOf(com.yangchengwei.easytrip.expense.ExpenseDraftRow("first",category=com.yangchengwei.easytrip.expense.ExpenseCategory.ATTRACTION))))
        compose.setContent { EasyTripTheme { EditItineraryItemContent(draft,{}, {},
            onExpenseAction={if(it is DayItineraryAction.UpdateExpenseRow) draft=draft.copy(expenses=listOf(it.row))},onSave={},onCancel={}) } }
        compose.onNodeWithTag("expense-amount-first").performScrollTo().performTextInput("36.50")
        compose.onNodeWithText("保存").assertIsEnabled()
        capture("expense-editor")
        compose.onNodeWithTag("expense-amount-first").performTextReplacement("-1")
        compose.onNodeWithText("保存").assertIsNotEnabled()
        compose.onNodeWithTag("expense-amount-first").performTextReplacement("0")
        compose.onNodeWithText("保存").assertIsEnabled()
    }
    @Test fun wholeTripShowsUncollapsedExpenseTotals() {
        val item = ItineraryItemUi("a","西湖","杭州",null,60,expenseCents=8000)
        compose.setContent { EasyTripTheme { WholeTripItineraryContent(listOf(
            WholeTripDayUi("one",1,listOf(item),emptyList(),expenses=ExpenseSummary(11650,2,1)),
            WholeTripDayUi("two",2,emptyList(),emptyList(),collapsedItemCount=1,expenses=ExpenseSummary(5000,1,0))
        )) } }
        compose.onNodeWithText("全程已记 ¥166.50 · 1项未填").assertIsDisplayed()
        compose.onNodeWithText("当日已记 ¥50").assertIsDisplayed()
        capture("summary")
    }
    @Test fun settingsDragOnDatedTripCommitsWithoutSave() {
        val start = LocalDate.of(2026,10,1)
        var state by mutableStateOf(TripSettingsUiState("trip",name="杭州旅行",startDate=start,
            days=days.map { DayUi(it.id,"10/${it.index+1}") },dateRange=DateRangeChangeUiState(startDate=start,endDate=start.plusDays(4))))
        val moves=mutableListOf<Pair<String,Int>>()
        compose.setContent { EasyTripTheme { TripSettingsContent(state,onBack={},onRename={},onTravelMode={},onSubmitDateRange={},onCancelDateRange={},onConfirmDateRange={},onRetryDateRangeSync={},onRequestDeleteDay={},onRetryDeleteDay={},onCancelDeleteDay={},onConfirmDeleteDay={},onDateRangeDraft={_,_->},onMoveDay={day,target->
            moves+=day.id to target
            state=state.copy(days=state.days.toMutableList().apply { add(target,removeAt(indexOfFirst { it.id==day.id })) }.mapIndexed { i,d -> d.copy(label="10/${i+1}") })
        }) } }
        val source=compose.onNodeWithTag("move-day-handle-day-0")
        source.performScrollTo()
        val delta=compose.onNodeWithTag("move-day-handle-day-1").fetchSemanticsNode().positionInRoot.y-source.fetchSemanticsNode().positionInRoot.y
        source.performTouchInput { down(center);advanceEventTime(650);moveBy(Offset(0f,delta));up() }
        compose.runOnIdle { assertEquals(listOf("day-0" to 1),moves) }
        capture("settings")
    }
}
