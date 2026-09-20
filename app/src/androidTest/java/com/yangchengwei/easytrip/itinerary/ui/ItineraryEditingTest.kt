package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.itinerary.ui.selectArrivalTime
import com.yangchengwei.easytrip.itinerary.ui.selectStayHours
import com.yangchengwei.easytrip.itinerary.ui.assertArrivalTime
import com.yangchengwei.easytrip.itinerary.ui.assertStayHours
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.pressBack
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

    @Test fun onlyReadyRouteStateOpensItsRealLegEditor() {
        val states = listOf(
            "pending" to RouteStatus.PENDING,
            "calculating" to RouteStatus.CALCULATING,
            "waiting" to RouteStatus.WAITING_NETWORK,
            "failed" to RouteStatus.FAILED,
            "ready" to RouteStatus.SUCCESS,
        )
        val editedLegIds = mutableListOf<String>()
        val retriedLegIds = mutableListOf<String>()
        compose.setContent {
            Column {
                states.forEach { (id, status) ->
                    RouteLegRow(
                        leg = RouteLegUi(id, "from-$id", "to-$id", TransportMode.TAXI, status, null, null, null),
                        onMode = { editedLegIds += id },
                        onRetry = { retriedLegIds += id },
                    )
                }
            }
        }

        listOf("pending", "calculating", "waiting", "failed").forEach { id ->
            compose.onAllNodesWithTag("edit-route-$id").assertCountEquals(0)
        }
        compose.onNodeWithTag("edit-route-ready").assertIsDisplayed().assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(listOf("ready"), editedLegIds) }
        compose.onNodeWithTag("retry-failed").assertIsDisplayed().assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(listOf("failed"), retriedLegIds) }
        listOf("pending", "calculating", "waiting", "ready").forEach { id ->
            compose.onAllNodesWithTag("retry-$id").assertCountEquals(0)
        }
    }

    @Test fun failedRouteShowsErrorAndRetryAction() {
        val coordinator = FakeCoordinator()
        val model = DayItineraryViewModel(
            "trip",
            FakeTrips(),
            FakeItineraries(),
            FakeLegs(),
            coordinator,
        )
        compose.setContent { DayItinerarySheet(model) }
        compose.waitUntil(5_000) { model.state.value.legs.any { it.status == RouteStatus.FAILED } }

        compose.onNodeWithTag("leg-leg-2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("no route").assertIsDisplayed()
        compose.onNodeWithTag("retry-leg-2").assertIsDisplayed().assertHasClickAction().performClick()
        compose.waitUntil(5_000) { coordinator.retries == listOf("leg-2") }
    }

    @Test fun deleteConfirmationExplainsRetentionAndAdjacentRouteRecalculation() {
        val itineraries = FakeItineraries()
        val model = DayItineraryViewModel(
            "trip",
            FakeTrips(),
            itineraries,
            FakeLegs(),
            FakeCoordinator(),
        )
        compose.setContent { DayItinerarySheet(model) }
        compose.waitUntil(5_000) { model.state.value.items.size == 3 }

        compose.onNodeWithTag("more-i2", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-delete-i2", useUnmergedTree = true).performClick()
        compose.onNodeWithText("仅移除本次安排；收藏仍保留；相邻路线将重新计算。").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { model.state.value.deleteConfirmation == null }
        assertEquals(emptyList<String>(), itineraries.deletes)
    }

    @Test fun itineraryEditSaveFailureShowsRecoveryInsteadOfBareEditorAndReturnsToSameInput() {
        var state by androidx.compose.runtime.mutableStateOf(
            DayItineraryUiState(
                editDraft = ItineraryEditDraft("item", "09:30", "60", "保留的备注", saveError = "保存失败"),
            ),
        )
        var saveCalls = 0
        compose.setContent {
            DayItineraryContent(
                state = state,
                onAction = { action ->
                    when (action) {
                        DayItineraryAction.DismissEditSaveError ->
                            state = state.copy(editDraft = state.editDraft?.copy(saveError = null))
                        DayItineraryAction.SaveEdit -> saveCalls++
                        else -> Unit
                    }
                },
            )
        }

        compose.onNodeWithTag("itinerary-save-failure").assertIsDisplayed()
        compose.onNodeWithText("修改尚未保存").assertIsDisplayed()
        compose.onNodeWithText("到达时间、停留时长和备注仍保留在当前页面。请重新保存，或稍后再试。").assertIsDisplayed()
        compose.onNodeWithText("当前编辑内容不会自动回滚").assertIsDisplayed()
        compose.onAllNodesWithTag("arrival-hour-picker").assertCountEquals(0)
        compose.onNodeWithTag("itinerary-save-failure-retry").performClick()
        compose.runOnIdle { assertEquals(1, saveCalls) }
        compose.onNodeWithTag("itinerary-save-failure-keep-editing").performClick()
        compose.onNodeWithTag("arrival-hour-picker").assertIsDisplayed()
        compose.onNodeWithTag("stay-hours-picker").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-note-input").assertIsDisplayed()
        compose.onNodeWithText("保留的备注").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, saveCalls) }
    }

    @Test fun saveFailureContinueEditingRestoresDraftAfterSystemImeWasVisible() {
        val itineraries = FakeItineraries(failDetails = true)
        val model = DayItineraryViewModel(
            "trip",
            FakeTrips(),
            itineraries,
            FakeLegs(),
            FakeCoordinator(),
        )
        compose.setContent { DayItinerarySheet(model) }
        compose.waitUntil(5_000) { model.state.value.items.size == 3 }

        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-i1", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("itinerary-note-input").performClick().performTextInput("系统键盘草稿")
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithText("保存时间").performClick()
        compose.waitUntil(5_000) { model.state.value.editDraft?.saveError == "保存失败" }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        compose.onNodeWithTag("itinerary-save-failure-keep-editing").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-save-failure-retry").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-save-failure-keep-editing").performClick()

        compose.onNodeWithTag("itinerary-note-input").assertIsDisplayed()
        compose.onNodeWithText("系统键盘草稿").assertIsDisplayed()
        compose.onNodeWithText("保存时间").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed()
        compose.runOnIdle { assertEquals("系统键盘草稿", model.state.value.editDraft?.noteText) }
    }

    @Test fun backFromSaveFailureKeepsDraftAndReturnsToInputsThroughDayItinerarySheet() {
        val itineraries = FakeItineraries(failDetails = true)
        val model = DayItineraryViewModel(
            "trip",
            FakeTrips(),
            itineraries,
            FakeLegs(),
            FakeCoordinator(),
        )
        compose.setContent { DayItinerarySheet(model) }
        compose.waitUntil(5_000) { model.state.value.items.size == 3 }

        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-i1", useUnmergedTree = true).performClick()
        compose.selectArrivalTime(9, 30)
        compose.selectStayHours(2)
        compose.onNodeWithTag("itinerary-note-input").performTextInput("保留备注")
        compose.onNodeWithText("保存时间").performClick()
        compose.waitUntil(5_000) { model.state.value.editDraft?.saveError == "保存失败" }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }

        pressBack()

        compose.waitUntil(5_000) { model.state.value.editDraft?.saveError == null }
        compose.onNodeWithTag("arrival-hour-picker").assertIsDisplayed()
        compose.onNodeWithTag("stay-hours-picker").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-note-input").assertIsDisplayed()
        compose.onNodeWithText("保留备注").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("i1", model.state.value.editDraft?.itemId)
            assertEquals("09:30", model.state.value.editDraft?.arrivalTimeText)
            assertEquals("120", model.state.value.editDraft?.stayMinutesText)
            assertEquals("保留备注", model.state.value.editDraft?.noteText)
            assertEquals(1, itineraries.timings.size)
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

        compose.onNodeWithTag("empty-illustration-itinerary").assertIsDisplayed()
        compose.onNodeWithText("第 2 天 · 暂无行程").assertIsDisplayed()
        compose.onNodeWithText("从地点池添加地点，开始安排这一天").assertIsDisplayed()
        compose.onNodeWithTag("add-places-to-selected-day").assertIsDisplayed()
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

        compose.onNodeWithText("等待联网后计算").assertIsDisplayed()
        compose.onNodeWithTag("item-i2")
            .assert(SemanticsMatcher("has both reorder actions") { node ->
                node.config[SemanticsActions.CustomActions].map { it.label } == listOf("上移", "下移")
            })
        compose.onNodeWithTag("more-i2", useUnmergedTree = true).assertHasClickAction().performClick()
        listOf("menu-timing-i2", "menu-move-i2", "menu-delete-i2").forEach { tag ->
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertHasClickAction()
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
            .assertContentDescriptionEquals("酒店，第 2 项，共 3 项", "拖动调整 酒店 的顺序")
            .assert(SemanticsMatcher("has both reorder actions") { node ->
                node.config[SemanticsActions.CustomActions].map { it.label } == listOf("上移", "下移")
            })
        val itemNode = compose.onNodeWithTag("item-i2", useUnmergedTree = true).fetchSemanticsNode()
        assertEquals(true, itemNode.config.isMergingSemanticsOfDescendants)
        compose.onNodeWithTag("more-i2", useUnmergedTree = true).assertIsDisplayed().assertHasClickAction()
        compose.onAllNodesWithText("酒店地址")[0].assertIsDisplayed()
        compose.onNodeWithText("1.1 公里", substring = true).assertIsDisplayed()
        compose.onNodeWithText("5 分钟", substring = true).assertIsDisplayed()

        compose.onNodeWithTag("add-place-hotel").performClick()
        compose.waitUntil(5_000) { itineraries.adds == listOf(Add("day-1", "hotel", 3)) }

        compose.onNodeWithTag("mode-leg-1").performClick()
        compose.onNodeWithTag("route-mode-option-WALK").performClick()
        compose.onNodeWithText("保存路段").performClick()
        compose.waitUntil(5_000) { coordinator.details.isNotEmpty() }
        assertEquals(RouteDetails("leg-1", TransportMode.WALK, null, null), coordinator.details.single())

        compose.onNodeWithTag("retry-leg-2").performClick()
        compose.waitUntil(5_000) { coordinator.retries.isNotEmpty() }
        assertEquals(listOf("leg-2"), coordinator.retries)

        val reorderStepPx = compose.activity.resources.displayMetrics.density * 120f
        compose.onNodeWithTag("drag-handle-i2", useUnmergedTree = true).performTouchInput {
            val start = center
            down(start)
            advanceEventTime(700)
            moveTo(start.copy(y = start.y - reorderStepPx - 10f), 800)
            up()
        }
        compose.waitUntil(5_000) { itineraries.moves.isNotEmpty() }
        assertEquals(Move("i2", "day-1", 0), itineraries.moves.single())

        compose.onNodeWithTag("drag-handle-i2", useUnmergedTree = true).performTouchInput {
            val start = center
            down(start)
            advanceEventTime(700)
            moveTo(start.copy(y = start.y + reorderStepPx * 2f + 10f), 800)
            up()
        }
        compose.waitUntil(5_000) { itineraries.moves.size == 2 }
        assertEquals(Move("i2", "day-1", 2), itineraries.moves.last())

        compose.onNodeWithTag("more-i3", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-move-i3", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("move-to-day-2").performClick()
        compose.waitUntil(5_000) { itineraries.moves.size == 3 }
        assertEquals(Move("i3", "day-2", 0), itineraries.moves.last())

        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-timing-i1", useUnmergedTree = true).performClick()
        compose.selectArrivalTime(9, 30)
        compose.selectStayHours(8)
        compose.onNodeWithText("保存时间").performClick()
        compose.waitUntil(5_000) { itineraries.timings.isNotEmpty() }
        assertEquals(Timing("i1", LocalTime.of(9, 30), 480), itineraries.timings.single())

        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("menu-delete-i1", useUnmergedTree = true).performClick()
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
        override suspend fun setHasTraveled(tripId: String, hasTraveled: Boolean) = Unit
        override suspend fun renameTrip(tripId: String, name: String) = Unit
        override suspend fun setStartDate(tripId: String, startDate: java.time.LocalDate?) = Unit
        override suspend fun dateRangeDeletionCounts(tripId: String, dayIds: List<String>) = com.yangchengwei.easytrip.trip.domain.DateRangeDeletionCounts(0, 0, 0)
        override suspend fun applyDateRange(command: com.yangchengwei.easytrip.trip.domain.DateRangeApply) = Unit
        override suspend fun setTravelMode(tripId: String, mode: TravelMode) = Unit
        override suspend fun insertDay(tripId: String, anchorDayId: String?, side: InsertSide) = "day"
        override suspend fun moveDay(tripId: String, dayId: String, targetIndex: Int) = Unit
        override suspend fun deleteDay(command: com.yangchengwei.easytrip.trip.domain.DayDeletion) = Unit
        override suspend fun deleteTrip(tripId: String) = Unit
    }

    private class FakeItineraries(private val failDetails: Boolean = false) : ItineraryRepository {
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
        override suspend fun updateDetails(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?) {
            timings += Timing(itemId, arrivalTime, stayMinutes)
            if (failDetails) throw IllegalStateException("保存失败")
            day1.value = day1.value.copy(items = day1.value.items.map { item ->
                if (item.id == itemId) item.copy(arrivalTime = arrivalTime, stayMinutes = stayMinutes, note = note) else item
            })
        }
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
        override suspend fun updateDetails(legId: String, selectedModeOverride: TransportMode?, durationOverrideSeconds: Int?, note: String?, online: Boolean): Boolean = error("Fake route details are not modeled")
        override suspend fun retry(legId: String, online: Boolean) = false
        private fun leg(id:String, from:String,to:String,status:RouteStatus,mode:TransportMode,distance:Int?,duration:Int?) = RouteLegEntity(id,"day-1",from,to,mode,status=status,distanceMeters=distance,durationSeconds=duration,errorCode=if(status==RouteStatus.FAILED)"no route" else null,version=1,updatedAt=Instant.EPOCH)
    }

    private class FakeCoordinator : RouteRefreshCoordinator {
        val overrides=mutableListOf<Pair<String,TransportMode>>(); val details=mutableListOf<RouteDetails>(); val retries=mutableListOf<String>()
        override fun start(scope: kotlinx.coroutines.CoroutineScope)=Unit
        override suspend fun retry(legId:String):Boolean { retries+=legId; return true }
        override suspend fun retry(legId:String,expectedVersion:Long):Boolean { retries+=legId; return true }
        override suspend fun updateDetails(legId:String,selectedModeOverride:TransportMode?,durationOverrideSeconds:Int?,note:String?):Boolean { details += RouteDetails(legId, selectedModeOverride, durationOverrideSeconds, note); return true }
    }
    data class Add(val day:String,val place:String,val index:Int)
    data class Move(val item:String,val day:String,val index:Int)
    data class Timing(val item:String,val time:LocalTime?,val minutes:Int?)
    data class RouteDetails(val legId:String,val selectedModeOverride:TransportMode?,val durationOverrideSeconds:Int?,val note:String?)
}
