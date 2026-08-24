package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.RouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import com.yangchengwei.easytrip.trip.domain.*
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItineraryEditingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun absentExternalSelectionDefaultsToFirstDay() {
        val model = DayItineraryViewModel(
            "trip",
            FakeTrips(),
            FakeItineraries(),
            FakeLegs(),
            FakeCoordinator(),
        )

        compose.waitUntil(5_000) {
            model.state.value.selectedDayId == "day-1" && model.state.value.items.size == 3
        }
    }

    @Test fun emptyDayShowsEmptyState() {
        val model = DayItineraryViewModel(
            "trip",
            FakeTrips(),
            FakeItineraries(),
            FakeLegs(),
            FakeCoordinator(),
            selectedDays = flowOf("day-2"),
        )
        compose.setContent { DayItinerarySheet(model) }

        compose.onNodeWithText("第2天 · 暂无行程").assertIsDisplayed()
        compose.onNodeWithText("从地点池添加地点，开始安排这一天").assertIsDisplayed()
    }

    @Test fun waitingForNetworkKeepsAllPlaceActions() {
        val model = DayItineraryViewModel(
            "trip",
            FakeTrips(),
            FakeItineraries(),
            FakeLegs(waiting = true),
            FakeCoordinator(),
        )
        compose.setContent { DayItinerarySheet(model) }
        compose.waitUntil(5_000) { model.state.value.items.size == 3 }

        compose.onNodeWithText("等待联网").assertIsDisplayed()
        listOf("timing-i1", "move-i1", "delete-i1", "item-i2").forEach {
            compose.onNodeWithTag(it, useUnmergedTree = true).assertHasClickAction()
        }
    }

    @Test fun duplicateDragCrossDayTimingOverrideAndRetry() {
        val trips = FakeTrips()
        val itineraries = FakeItineraries()
        val legs = FakeLegs()
        val coordinator = FakeCoordinator()
        val selectedDay = MutableStateFlow<String?>("day-1")
        val model = DayItineraryViewModel(
            "trip",
            trips,
            itineraries,
            legs,
            coordinator,
            flowOf(listOf(SavedPlace("hotel", "trip", "poi", "酒店", "", GeoPoint(1.0, 2.0), "", emptyList()))),
            selectedDay,
        )
        compose.setContent { DayItinerarySheet(model) }
        compose.waitUntil(5_000) { model.state.value.items.size == 3 }

        compose.onNodeWithText("Day 1").assertDoesNotExist()
        compose.onNodeWithText("Day 2").assertDoesNotExist()
        compose.runOnIdle { selectedDay.value = null }
        compose.waitUntil(5_000) {
            model.state.value.selectedDayId == null &&
                model.state.value.items.isEmpty() &&
                model.state.value.legs.isEmpty() &&
                model.state.value.previewOrder.isEmpty()
        }
        compose.runOnIdle { trips.emitUpdate() }
        compose.waitForIdle()
        assertEquals(null, model.state.value.selectedDayId)
        assertEquals(emptyList<ItineraryItemUi>(), model.state.value.items)
        assertEquals(emptyList<RouteLegUi>(), model.state.value.legs)
        compose.runOnIdle { selectedDay.value = "day-1" }
        compose.waitUntil(5_000) { model.state.value.items.size == 3 }
        listOf("item-i1", "leg-leg-1", "item-i2", "leg-leg-2", "item-i3").forEach {
            compose.onNodeWithTag(it).assertIsDisplayed()
        }
        val timelineTops = listOf("item-i1", "leg-leg-1", "item-i2", "leg-leg-2", "item-i3")
            .map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(timelineTops.sorted(), timelineTops)
        val timelineTags = compose.onRoot().fetchSemanticsNode().timelineTags()
        assertEquals(listOf("item-i1", "leg-leg-1", "item-i2", "leg-leg-2", "item-i3"), timelineTags)
        compose.onNodeWithTag("leg-leg-1").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        compose.onNodeWithTag("leg-leg-2").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        compose.onNodeWithText("↓").assertDoesNotExist()
        compose.onNodeWithTag("item-i2")
            .assertContentDescriptionEquals("酒店，第 2 项，共 3 项")
            .assert(SemanticsMatcher("has both reorder actions") { node ->
                node.config[SemanticsActions.CustomActions].map { it.label } == listOf("上移", "下移")
            })
        val itemNode = compose.onNodeWithTag("item-i2", useUnmergedTree = true).fetchSemanticsNode()
        assertEquals(true, itemNode.config.isMergingSemanticsOfDescendants)
        listOf("timing-i2", "move-i2", "delete-i2").forEach { tag ->
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed().assertHasClickAction()
        }
        compose.onAllNodesWithText("酒店地址")[0].assertIsDisplayed()
        compose.onNodeWithText("1.1 公里", substring = true).assertIsDisplayed()
        compose.onNodeWithText("5 分钟", substring = true).assertIsDisplayed()

        compose.onNodeWithTag("add-place-hotel").performClick()
        compose.waitUntil(5_000) { itineraries.adds == listOf(Add("day-1", "hotel", 3)) }

        compose.onNodeWithTag("mode-leg-1").performClick()
        compose.onNodeWithTag("mode-option-WALK").performClick()
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(5_000) { coordinator.overrides.isNotEmpty() }
        assertEquals("leg-1" to TransportMode.WALK, coordinator.overrides.single())

        compose.onNodeWithTag("retry-leg-2").performClick()
        compose.waitUntil(5_000) { coordinator.retries.isNotEmpty() }
        assertEquals(listOf("leg-2"), coordinator.retries)

        compose.onNodeWithTag("item-i2").performTouchInput {
            val start = Offset(width * 0.15f, height * 0.25f)
            down(start)
            advanceEventTime(700)
            moveTo(Offset(start.x, start.y - 500f), 800)
            up()
        }
        compose.waitUntil(5_000) { itineraries.moves.isNotEmpty() }
        assertEquals(Move("i2", "day-1", 0), itineraries.moves.single())

        compose.onNodeWithTag("item-i2").performTouchInput {
            val start = Offset(width * 0.85f, height * 0.25f)
            down(start)
            advanceEventTime(700)
            moveTo(Offset(start.x, start.y + 500f), 800)
            up()
        }
        compose.waitUntil(5_000) { itineraries.moves.size == 2 }
        assertEquals(Move("i2", "day-1", 2), itineraries.moves.last())

        compose.onNodeWithTag("move-i3").performClick()
        compose.onNodeWithTag("move-to-day-2").performClick()
        compose.waitUntil(5_000) { itineraries.moves.size == 3 }
        assertEquals(Move("i3", "day-2", 0), itineraries.moves.last())

        compose.onNodeWithTag("timing-i1").performClick()
        compose.onNodeWithTag("arrival-time-input").performTextClearance()
        compose.onNodeWithTag("arrival-time-input").performTextInput("09:30")
        compose.onNodeWithTag("stay-minutes-input").performTextClearance()
        compose.onNodeWithTag("stay-minutes-input").performTextInput("480")
        compose.onNodeWithText("保存时间").performClick()
        compose.waitUntil(5_000) { itineraries.timings.isNotEmpty() }
        assertEquals(Timing("i1", LocalTime.of(9, 30), 480), itineraries.timings.single())

        compose.onNodeWithTag("delete-i1").performClick()
        compose.onNodeWithText("确认移出").performClick()
        compose.waitUntil(5_000) { itineraries.deletes == listOf("i1") }
    }

    private fun SemanticsNode.timelineTags(): List<String> =
        listOfNotNull(if (config.contains(SemanticsProperties.TestTag)) config[SemanticsProperties.TestTag].takeIf { it.startsWith("item-") || it.startsWith("leg-") } else null) +
            children.flatMap { it.timelineTags() }

    private class FakeTrips : TripRepository {
        private val trip = MutableStateFlow(TripWithDays("trip", "Trip", null, TravelMode.FLEXIBLE, listOf(TripDay("day-1", 0), TripDay("day-2", 1))))
        fun emitUpdate() { trip.value = trip.value.copy(name = "Updated Trip") }
        override fun observeTrip(tripId: String) = trip.map { it }
        override fun observeTrips() = flowOf(emptyList<TripSummary>())
        override suspend fun createTrip(command: CreateTrip) = "trip"
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: java.time.LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(dayId: String) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class FakeItineraries : ItineraryRepository {
        private val hotel = ItineraryPlace("hotel", "酒店", "酒店地址", GeoPoint(1.0, 2.0))
        private val museum = ItineraryPlace("museum", "博物馆", "", GeoPoint(1.1, 2.1))
        private val day1 = MutableStateFlow(DayItinerary("day-1", "trip", listOf(ItineraryItem("i1", hotel, null, null), ItineraryItem("i2", hotel, null, null), ItineraryItem("i3", museum, null, null))))
        private val day2 = MutableStateFlow(DayItinerary("day-2", "trip", emptyList()))
        val adds = mutableListOf<Add>(); val moves = mutableListOf<Move>(); val timings = mutableListOf<Timing>(); val deletes = mutableListOf<String>()
        override fun observeDay(dayId: String) = if (dayId == "day-1") day1 else day2
        override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String { adds += Add(dayId, savedPlaceId, targetIndex); return "new" }
        override suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int) { moves += Move(itemId, targetDayId, targetIndex) }
        override suspend fun deleteItem(itemId: String) { deletes += itemId }
        override suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?) { timings += Timing(itemId, arrivalTime, stayMinutes) }
        override suspend fun removePlaceOccurrences(placeId: String) = Unit
    }

    private class FakeLegs(private val waiting: Boolean = false) : RouteLegRepository {
        override fun observeDay(dayId: String) = if (dayId == "day-1") flowOf(listOf(
            leg("leg-1", "i1", "i2", if (waiting) RouteStatus.WAITING_NETWORK else RouteStatus.SUCCESS, TransportMode.TAXI, 1050, 300),
            leg("leg-2", "i2", "i3", RouteStatus.FAILED, TransportMode.WALK, null, null),
        )) else flowOf(emptyList())
        override fun observePending() = flowOf(emptyList<com.yangchengwei.easytrip.route.domain.RouteLegWithEndpoints>())
        override suspend fun get(legId: String) = null
        override suspend fun requeueTransientFailures() = 0
        override suspend fun recoverInterruptedCalculations(online: Boolean) = 0
        override suspend fun repairCorruptPolyline(legId: String, version: Long) = false
        override suspend fun claimIfVersionMatches(legId: String, version: Long) = false
        override suspend fun waitForNetworkIfVersionMatches(legId: String, version: Long) = false
        override suspend fun releaseClaimIfVersionMatches(legId: String, version: Long, online: Boolean) = false
        override suspend fun completeIfVersionMatches(legId: String, version: Long, result: com.yangchengwei.easytrip.route.domain.RouteResult) = false
        override suspend fun failIfVersionMatches(legId: String, version: Long, failure: com.yangchengwei.easytrip.route.domain.RoutePlanOutcome.Failure) = false
        override suspend fun overrideMode(legId: String, mode: TransportMode, online: Boolean) = false
        override suspend fun retry(legId: String, online: Boolean) = false
        private fun leg(id:String, from:String,to:String,status:RouteStatus,mode:TransportMode,distance:Int?,duration:Int?) = RouteLegEntity(id,"day-1",from,to,mode,status=status,distanceMeters=distance,durationSeconds=duration,errorCode=if(status==RouteStatus.FAILED)"no route" else null,version=1,updatedAt=Instant.EPOCH)
    }

    private class FakeCoordinator : RouteRefreshCoordinator {
        val overrides=mutableListOf<Pair<String,TransportMode>>(); val retries=mutableListOf<String>()
        override fun start(scope: kotlinx.coroutines.CoroutineScope)=Unit
        override suspend fun retry(legId:String):Boolean { retries+=legId; return true }
        override suspend fun overrideMode(legId:String,mode:TransportMode):Boolean { overrides+=legId to mode; return true }
    }
    data class Add(val day:String,val place:String,val index:Int)
    data class Move(val item:String,val day:String,val index:Int)
    data class Timing(val item:String,val time:LocalTime?,val minutes:Int?)
}
