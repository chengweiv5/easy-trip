package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.component.ConfirmationDialog
import com.yangchengwei.easytrip.core.ui.component.ConfirmationUiModel
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.domain.AddPlacesOutcome
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStep
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.AddTripDayContent
import com.yangchengwei.easytrip.itinerary.ui.SelectPlacesContent
import com.yangchengwei.easytrip.itinerary.ui.SelectTargetDayContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.ItineraryDeleteConfirmation
import com.yangchengwei.easytrip.itinerary.ui.ItineraryEditDraft
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.RouteLegContent
import com.yangchengwei.easytrip.itinerary.ui.RouteLegUi
import com.yangchengwei.easytrip.itinerary.ui.WholeTripDayUi
import com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.WorkspaceItineraryContent
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.ui.PlaceDetailContent
import com.yangchengwei.easytrip.place.ui.PlaceDetailDraft
import com.yangchengwei.easytrip.place.ui.PlacePoolAction
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import com.yangchengwei.easytrip.place.ui.PlaceSearchContent
import com.yangchengwei.easytrip.place.ui.PlaceSearchPhase
import com.yangchengwei.easytrip.place.ui.PlaceSearchState
import com.yangchengwei.easytrip.place.ui.PlaceSearchUiState
import com.yangchengwei.easytrip.permission.PermissionExplanationContent
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.ui.CreateTimeMode
import com.yangchengwei.easytrip.trip.ui.CreateTripAction
import com.yangchengwei.easytrip.trip.ui.CreateTripContent
import com.yangchengwei.easytrip.trip.ui.CreateTripUiState
import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import com.yangchengwei.easytrip.trip.ui.DateRangeChangePhase
import com.yangchengwei.easytrip.trip.ui.DateRangeChangeRequest
import com.yangchengwei.easytrip.trip.ui.DateRangeChangeUiState
import com.yangchengwei.easytrip.trip.ui.DayDeleteImpact
import com.yangchengwei.easytrip.trip.ui.DayUi
import com.yangchengwei.easytrip.trip.ui.PendingDayDeletion
import com.yangchengwei.easytrip.trip.ui.TripCardUiModel
import com.yangchengwei.easytrip.trip.ui.TripSettingsContent
import com.yangchengwei.easytrip.trip.ui.TripSettingsUiState
import com.yangchengwei.easytrip.trip.ui.TripDeletionUiState
import com.yangchengwei.easytrip.trip.ui.TripListAction
import com.yangchengwei.easytrip.trip.ui.TripListContent
import com.yangchengwei.easytrip.trip.ui.TripListPageState
import com.yangchengwei.easytrip.trip.ui.TripListUiState
import com.yangchengwei.easytrip.workspace.ItineraryScope
import com.yangchengwei.easytrip.workspace.MapLayer
import com.yangchengwei.easytrip.workspace.MapUiModel
import com.yangchengwei.easytrip.workspace.TripWorkspaceContent
import com.yangchengwei.easytrip.workspace.TripWorkspacePageState
import com.yangchengwei.easytrip.workspace.TripWorkspaceReadyState
import com.yangchengwei.easytrip.workspace.TripWorkspaceScreen
import com.yangchengwei.easytrip.workspace.WorkspaceMapState
import com.yangchengwei.easytrip.workspace.WorkspaceOverlay
import com.yangchengwei.easytrip.workspace.WorkspaceSection
import com.yangchengwei.easytrip.workspace.WorkspaceSheetLevel
import java.time.LocalDate

internal typealias V1ComposeRule = AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

data class ScenarioFixture(val id: String, val screen: ScenarioScreen)
data class ScenarioPath(val steps: List<ScenarioScreen>) {
    init { require(steps.isNotEmpty()) }
    val description: String get() = steps.joinToString(" → ") { it.label }
}

enum class ScenarioScreen(val label: String) {
    TRIP_LIST("我的旅行"),
    CREATE_TRIP("创建旅行"),
    DATE_PICKER("选择日期"),
    WORKSPACE("工作台"),
    ITINERARY("行程"),
    ITEM_EDITOR("行程项"),
    ROUTE_EDITOR("交通路段"),
    SEARCH("搜索地点"),
    PLACE_POOL("地点池"),
    PLACE_DETAIL("地点详情"),
    TARGET_DAY("选择旅行日"),
    TRIP_SETTINGS("旅行设置"),
    PERMISSION("权限说明"),
    STATUS_MATRIX("状态矩阵"),
}

interface V1ScenarioExecutable {
    val fixture: ScenarioFixture
    val reachablePath: ScenarioPath
    fun setup()
    fun render(compose: V1ComposeRule)
    fun actions(compose: V1ComposeRule)
    fun assertions(compose: V1ComposeRule)
}

private class ComposeScenario(
    override val fixture: ScenarioFixture,
    override val reachablePath: ScenarioPath,
    private val reset: () -> Unit = {},
    private val content: @androidx.compose.runtime.Composable () -> Unit,
    private val interact: V1ComposeRule.() -> Unit = {},
    private val verify: V1ComposeRule.() -> Unit,
) : V1ScenarioExecutable {
    override fun setup() = reset()
    override fun render(compose: V1ComposeRule) = compose.setContent { EasyTripTheme { content() } }
    override fun actions(compose: V1ComposeRule) = compose.interact()
    override fun assertions(compose: V1ComposeRule) = compose.verify()
}

data class WorkspaceSheetScenarioSpec(
    val number: Int,
    val frameId: String,
    val fixtureId: String,
    val level: WorkspaceSheetLevel,
) {
    fun requireIdentity(number: Int, frameId: String, fixtureId: String) {
        require(number == this.number) { "Workspace sheet scenario must be ${this.number}, was $number" }
        require(frameId == this.frameId) { "Workspace sheet scenario ${this.number} must use frame ${this.frameId}, was $frameId" }
        require(fixtureId == this.fixtureId) { "Workspace sheet scenario ${this.number} must use fixture ${this.fixtureId}, was $fixtureId" }
    }
}

object V1ScenarioExecutableFactory {
    fun create(number: Int, frameId: String, fixtureId: String): V1ScenarioExecutable = when (number) {
        1 -> existingTrips(fixtureId)
        2 -> placePool(fixtureId)
        3 -> searchResults(fixtureId)
        4 -> workspaceItinerary(fixtureId)
        6 -> wholeTripItinerary(fixtureId)
        7 -> validCreate(fixtureId)
        8 -> tripSettings(fixtureId)
        9 -> dateSelection(fixtureId)
        10 -> placeDetail(fixtureId)
        11 -> itemEdit(fixtureId)
        12 -> routeEdit(fixtureId)
        13 -> deleteTripConfirmation(fixtureId)
        14 -> statusMatrix(fixtureId)
        15 -> noTripDays(fixtureId)
        16 -> workspaceSettings(fixtureId)
        17 -> mapLayer(fixtureId)
        18 -> addTripDay(fixtureId)
        19 -> selectPlaces(fixtureId)
        20 -> targetDay(fixtureId, targetMissing = false, submitting = false)
        21 -> addComplete(fixtureId)
        22 -> workspaceSheet(number, frameId, fixtureId, WorkspaceSheetScenarioSpec(22, "kCc5z", "workspace-drawer-collapsed", WorkspaceSheetLevel.COLLAPSED))
        23 -> workspaceSheet(number, frameId, fixtureId, WorkspaceSheetScenarioSpec(23, "sWTB3", "workspace-drawer-half", WorkspaceSheetLevel.HALF))
        24 -> workspaceSheet(number, frameId, fixtureId, WorkspaceSheetScenarioSpec(24, "f2ieZ6", "workspace-drawer-expanded", WorkspaceSheetLevel.EXPANDED))
        25 -> deleteTripDay(fixtureId)
        26 -> emptyPlacePool(fixtureId)
        27 -> searchState(fixtureId, PlaceSearchPhase.Empty, "没有找到相关地点")
        28 -> waitingForNetwork(fixtureId)
        29 -> failedRoute(fixtureId)
        30 -> permission(fixtureId)
        31 -> addComplete(fixtureId)
        32 -> deleteItineraryItem(fixtureId)
        33 -> longTargetDays(fixtureId)
        34 -> permission(fixtureId)
        35 -> permission(fixtureId, clickConfirm = true)
        36 -> emptyTrips(fixtureId)
        37 -> emptyDay(fixtureId)
        38 -> searchState(fixtureId, PlaceSearchPhase.NetworkFailure("无法搜索新的地点"), "网络连接失败")
        39 -> partialRouteSuccess(fixtureId)
        40 -> changeDateRange(fixtureId)
        41 -> targetDay(fixtureId, targetMissing = false, submitting = true)
        42 -> targetDay(fixtureId, targetMissing = true, submitting = false)
        43 -> undoSuccess(fixtureId)
        44 -> searchLoading(fixtureId)
        45 -> mapLoading(fixtureId)
        46 -> mapFailure(fixtureId)
        47 -> invalidCreate(fixtureId)
        48 -> editSaveFailure(fixtureId)
        else -> error("Unsupported V1 scenario: $number")
    }

    private fun savedPlace() = SavedPlace("place-1", "trip-1", "poi-1", "西湖", "杭州市西湖区", GeoPoint(30.25, 120.15), "湖边散步", emptyList())

    private fun placeRows() = listOf(SavedPlaceRowUi(savedPlace(), 1, true))

    private fun placePool(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<PlacePoolAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
            actions::clear,
            { com.yangchengwei.easytrip.place.ui.PlacePoolContent(PlacePoolUiState(rows = placeRows()), showSearch = false, onAction = actions::add) },
            { onNodeWithTag("start-add-to-itinerary").performClick() },
            { onNodeWithText("西湖").assertIsDisplayed(); check(actions == listOf(PlacePoolAction.StartAddToItinerary)) },
        )
    }

    private fun searchResults(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.SEARCH),
        ScenarioPath(listOf(ScenarioScreen.WORKSPACE, ScenarioScreen.SEARCH)),
        content = {
            PlaceSearchContent(
                PlaceSearchUiState(search = PlaceSearchState("西湖", listOf(PlaceCandidate("poi-1", "西湖", "杭州市西湖区", GeoPoint(30.25, 120.15), "0571")), phase = PlaceSearchPhase.Results)),
                {},
            )
        },
        verify = {
            onNodeWithTag("place-search-result-row-poi-1").assertIsDisplayed()
            onNodeWithTag("place-search-result-row-poi-1").assertTextContains("西湖")
        },
    )

    private fun placeDetail(id: String): V1ScenarioExecutable {
        var saved = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PLACE_DETAIL),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.PLACE_DETAIL)),
            { saved = false },
            { PlaceDetailContent(savedPlace(), PlaceDetailDraft("湖边散步", setOf("自然"), "place-1"), false, null, {}, {}, {}, { saved = true }) },
            { onNodeWithText("保存").performClick() },
            { onNodeWithTag("place-detail-title").assertIsDisplayed(); check(saved) },
        )
    }

    private fun selectPlaces(id: String): V1ScenarioExecutable {
        var continued = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
            { continued = false },
            { SelectPlacesContent(placeRows(), AddToItineraryUiState(selectedPlaceIds = listOf("place-1"), step = AddToItineraryStep.SELECT_PLACES), {}, { continued = true }, {}) },
            { onNodeWithTag("select-places-continue").performClick() },
            { onNodeWithText("已选 1 个").assertIsDisplayed(); check(continued) },
        )
    }

    private fun targetDay(id: String, targetMissing: Boolean, submitting: Boolean) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TARGET_DAY),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.TARGET_DAY)),
        content = {
            SelectTargetDayContent(
                listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
                AddToItineraryUiState(
                    selectedPlaceIds = listOf("place-1"),
                    targetDayId = if (targetMissing) null else "day-1",
                    validityInitialized = true,
                    step = AddToItineraryStep.SELECT_TARGET_DAY,
                    isSubmitting = submitting,
                    result = if (targetMissing) AddPlacesOutcome.TargetDayMissing(listOf("place-1")) else null,
                ), {}, {}, {},
            )
        },
        verify = {
            if (targetMissing) onNodeWithText("所选旅行日已不存在，请重新选择").assertIsDisplayed()
            else if (submitting) onNodeWithText("正在创建行程项，请勿重复操作").assertIsDisplayed()
            else onNodeWithText("加入哪一天？").assertIsDisplayed()
        },
    )

    private fun longTargetDays(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TARGET_DAY),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.TARGET_DAY)),
        content = { SelectTargetDayContent((0 until 30).map { TripDay("day-$it", it) }, AddToItineraryUiState(selectedPlaceIds = listOf("place-1"), validityInitialized = true, step = AddToItineraryStep.SELECT_TARGET_DAY), {}, {}, {}) },
        verify = { onNodeWithTag("select-target-day-list").assertIsDisplayed(); onNodeWithText("请选择旅行日").assertIsDisplayed() },
    )

    private fun addComplete(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.ITINERARY),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.TARGET_DAY, ScenarioScreen.ITINERARY)),
        content = { DayItineraryContent(DayItineraryUiState(days = listOf(TripDay("day-1", 0)), selectedDayId = "day-1", items = listOf(ItineraryItemUi("item-1", "西湖", "杭州", null, null)), previewOrder = listOf("item-1")), onAction = {}) },
        verify = { onNodeWithText("西湖").assertIsDisplayed() },
    )

    private fun emptyPlacePool(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
        content = { com.yangchengwei.easytrip.place.ui.PlacePoolContent(PlacePoolUiState(), showSearch = false, onAction = {}) },
        verify = { onNodeWithText("还没有收藏地点").assertIsDisplayed(); onNodeWithText("搜索地点").assertIsDisplayed() },
    )

    private fun existingTrips(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<TripListAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.TRIP_LIST),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST)),
            actions::clear,
            {
                TripListContent(
                    TripListUiState(
                        page = TripListPageState.Content(
                            primaryTrip = TripCardUiModel("trip-1", "杭州周末", "3 天", "2026年9月1日", "灵活"),
                            otherTrips = listOf(TripCardUiModel("trip-2", "东京秋日", "5 天", "2026年10月2日", "自驾")),
                        ),
                    ),
                    actions::add,
                )
            },
            { onNodeWithTag("continue-trip-trip-1").performClick() },
            {
                onNodeWithText("杭州周末").assertIsDisplayed()
                onNodeWithText("2026年9月1日 · 3 天 · 灵活").assertIsDisplayed()
                onNodeWithTag("primary-trip-trip-1").assertIsDisplayed()
                onNodeWithTag("continue-trip-trip-1").assertIsDisplayed()
                onNodeWithTag("trip-menu-trip-1").assertIsDisplayed()
                onNodeWithTag("trip-menu-trip-2").assertIsDisplayed()
                onAllNodesWithText("继续规划").assertCountEquals(1)
                check(actions == listOf(TripListAction.OpenTrip("trip-1")))
            },
        )
    }

    private fun workspaceItinerary(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val day = TripDay("day-1", 0)
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            actions::clear,
            {
                WorkspaceItineraryContent(
                    days = listOf(day),
                    selected = ItineraryScope.Day(day.id),
                    wholeTripDays = emptyList(),
                    onSelect = {},
                    dayContent = { DayItineraryContent(DayItineraryUiState(days = listOf(day), selectedDayId = day.id), onAction = actions::add) },
                )
            },
            { onNodeWithTag("add-places-to-selected-day").performClick() },
            { onNodeWithText("第1天 · 暂无行程").assertIsDisplayed(); check(actions == listOf(DayItineraryAction.AddPlaces)) },
        )
    }

    private fun wholeTripItinerary(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.ITINERARY),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
        content = {
            WholeTripItineraryContent(
                listOf(
                    WholeTripDayUi(
                        "day-1",
                        1,
                        listOf(ItineraryItemUi("item-1", "西湖", "杭州市西湖区", null, 90)),
                        emptyList(),
                    ),
                    WholeTripDayUi("day-2", 2, emptyList(), emptyList()),
                ),
            )
        },
        verify = {
            onNodeWithTag("whole-trip-day-day-1").assertIsDisplayed()
            onNodeWithText("西湖").assertIsDisplayed()
            onNodeWithText("暂无行程").assertIsDisplayed()
        },
    )

    private fun noTripDays(id: String): V1ScenarioExecutable {
        var addRequested = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            { addRequested = false },
            { Column { WholeTripItineraryContent(emptyList()); AddTripDayContent(false, null, { addRequested = true }, {}) } },
            { onNodeWithText("添加一天").performClick() },
            { onNodeWithText("暂无旅行日").assertIsDisplayed(); check(addRequested) },
        )
    }

    private fun addTripDay(id: String): V1ScenarioExecutable {
        var confirmed = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.TRIP_SETTINGS),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.TRIP_SETTINGS)),
            { confirmed = false },
            { AddTripDayContent(false, null, { confirmed = true }, {}) },
            { onNodeWithText("添加一天").performClick() },
            { onNodeWithText("新的一天会追加到当前旅行末尾。").assertIsDisplayed(); check(confirmed) },
        )
    }

    private fun waitingForNetwork(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.ITINERARY),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
        content = {
            val items = listOf(
                ItineraryItemUi("a", "西湖", "杭州", null, null),
                ItineraryItemUi("b", "灵隐寺", "杭州", null, null),
            )
            DayItineraryContent(
                DayItineraryUiState(
                    days = listOf(TripDay("day-1", 0)),
                    selectedDayId = "day-1",
                    items = items,
                    previewOrder = items.map(ItineraryItemUi::id),
                    legs = listOf(RouteLegUi("leg-1", "a", "b", TransportMode.WALK, RouteStatus.WAITING_NETWORK, null, null, null)),
                ),
                onAction = {},
            )
        },
        verify = {
            onNodeWithText("联网后计算路线").assertIsDisplayed()
            onNodeWithContentDescription("离线，联网后计算路线").assertIsDisplayed()
            onNodeWithTag("add-places-to-selected-day").assertIsDisplayed()
        },
    )

    private fun failedRoute(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val items = listOf(
            ItineraryItemUi("a", "西湖", "杭州", null, null),
            ItineraryItemUi("b", "灵隐寺", "杭州", null, null),
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(
                        items = items,
                        previewOrder = items.map(ItineraryItemUi::id),
                        legs = listOf(RouteLegUi("leg-1", "a", "b", TransportMode.WALK, RouteStatus.FAILED, null, null, "路线失败")),
                    ),
                    onAction = actions::add,
                )
            },
            { onNodeWithTag("retry-leg-1").performClick() },
            { onNodeWithText("路线失败").assertIsDisplayed(); check(actions == listOf(DayItineraryAction.Retry("leg-1"))) },
        )
    }

    private fun deleteItineraryItem(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(deleteConfirmation = ItineraryDeleteConfirmation("item-1", "西湖")),
                    onAction = actions::add,
                )
            },
            { onNodeWithText("确认移出").performClick() },
            {
                onNodeWithText("仅从当天行程移出，收藏仍保留；相邻路线将重新计算。").assertIsDisplayed()
                check(actions == listOf(DayItineraryAction.ConfirmDelete))
            },
        )
    }

    private fun partialRouteSuccess(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val items = listOf(
            ItineraryItemUi("a", "西湖", "杭州", null, null),
            ItineraryItemUi("b", "灵隐寺", "杭州", null, null),
            ItineraryItemUi("c", "雷峰塔", "杭州", null, null),
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(
                        items = items,
                        previewOrder = items.map(ItineraryItemUi::id),
                        legs = listOf(
                            RouteLegUi("ok", "a", "b", TransportMode.WALK, RouteStatus.SUCCESS, 800, 600, null),
                            RouteLegUi("failed", "b", "c", TransportMode.WALK, RouteStatus.FAILED, null, null, "部分路线失败"),
                        ),
                    ),
                    onAction = actions::add,
                )
            },
            { onNodeWithTag("retry-failed").performClick() },
            {
                onNodeWithText("800 米 · 10 分钟").assertIsDisplayed()
                onNodeWithText("部分路线失败").assertIsDisplayed()
                check(actions == listOf(DayItineraryAction.Retry("failed")))
            },
        )
    }

    private fun tripSettings(id: String) = settingsScenario(
        id,
        TripSettingsUiState(
            tripId = "trip-1",
            name = "杭州周末",
            startDate = LocalDate.of(2026, 9, 1),
            dateRange = DateRangeChangeUiState(
                startDate = LocalDate.of(2026, 9, 1),
                baselineEndDate = LocalDate.of(2026, 9, 2),
                endDate = LocalDate.of(2026, 9, 2),
            ),
            days = listOf(DayUi("day-1", "Day 1 · 9月1日"), DayUi("day-2", "Day 2 · 9月2日")),
        ),
        verify = {
            onNodeWithTag("settings-date-row").performScrollTo().assertIsDisplayed()
            onNodeWithText("整体出行日期").assertIsDisplayed()
            onNodeWithText("旅行日").assertIsDisplayed()
        },
    )

    private fun deleteTripConfirmation(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<TripListAction>()
        val primaryTrip = TripCardUiModel("trip-1", "杭州周末", "3 天", "2026年9月1日", "灵活")
        val retainedTrip = TripCardUiModel("trip-2", "东京秋日", "5 天", "2026年10月2日", "自驾")
        var page by mutableStateOf<TripListPageState>(TripListPageState.Content(primaryTrip, listOf(retainedTrip)))
        var deletion by mutableStateOf<TripDeletionUiState>(TripDeletionUiState.Idle)
        val confirmation = ConfirmationUiModel(
            "删除杭州周末？",
            "此操作将永久删除旅行及其中的所有内容，无法撤销。",
            listOf("3 个旅行日", "2 个收藏地点", "4 个标签", "5 个行程项", "4 个路线段"),
            listOf("其他旅行及其内容"),
            "确认删除旅行",
            "取消",
            destructive = true,
            reversible = false,
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.TRIP_LIST),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST)),
            {
                actions.clear()
                page = TripListPageState.Content(primaryTrip, listOf(retainedTrip))
                deletion = TripDeletionUiState.Idle
            },
            {
                TripListContent(
                    TripListUiState(
                        page = page,
                        deletion = deletion,
                    ),
                    onAction = { action ->
                        actions += action
                        if (action == TripListAction.RequestDelete("trip-1")) {
                            deletion = TripDeletionUiState.Ready("trip-1", "杭州周末", confirmation)
                        }
                    },
                )
                if (deletion is TripDeletionUiState.Ready) {
                    ConfirmationDialog(
                        confirmation,
                        {
                            actions += TripListAction.ConfirmDelete
                            page = TripListPageState.Content(retainedTrip, emptyList())
                            deletion = TripDeletionUiState.Idle
                        },
                        {},
                    )
                }
            },
            {
                onNodeWithTag("trip-menu-trip-1").performClick()
                onNodeWithTag("trip-menu-delete-trip-1").performClick()
                onNodeWithText("删除杭州周末？").assertIsDisplayed()
                listOf("3 个旅行日", "2 个收藏地点", "4 个标签", "5 个行程项", "4 个路线段", "其他旅行及其内容")
                    .forEach { onNodeWithText(it).assertIsDisplayed() }
                onNodeWithTag("confirmation-confirm").performClick()
            },
            {
                onNodeWithTag("primary-trip-trip-1").assertDoesNotExist()
                onNodeWithText("东京秋日").assertIsDisplayed()
                check(actions == listOf(TripListAction.RequestDelete("trip-1"), TripListAction.ConfirmDelete))
            },
        )
    }

    private fun workspaceSettings(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<com.yangchengwei.easytrip.workspace.TripWorkspaceAction>()
        return workspaceScenario(id, actions, readyState(), WorkspaceMapState.Ready,
            interact = { onNodeWithTag("workspace-more").performClick() },
            verify = {
                onNodeWithTag("workspace-top-bar").assertIsDisplayed()
                check(actions == listOf(com.yangchengwei.easytrip.workspace.TripWorkspaceAction.OpenSettings))
            },
        )
    }

    private fun mapLayer(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<com.yangchengwei.easytrip.workspace.TripWorkspaceAction>()
        return workspaceScenario(
            id,
            actions,
            readyState(overlay = WorkspaceOverlay.LayerMenu),
            WorkspaceMapState.Ready,
            interact = { onNodeWithTag("layer-SATELLITE_ROAD").performClick() },
            verify = {
                onNodeWithTag("layer-menu-panel").assertIsDisplayed()
                check(actions == listOf(
                    com.yangchengwei.easytrip.workspace.TripWorkspaceAction.SelectMapLayer(MapLayer.SATELLITE_ROAD),
                    com.yangchengwei.easytrip.workspace.TripWorkspaceAction.CloseOverlay,
                ))
            },
        )
    }

    private fun workspaceSheet(
        scenarioNumber: Int,
        frameId: String,
        fixtureId: String,
        spec: WorkspaceSheetScenarioSpec,
    ): V1ScenarioExecutable {
        val level = spec.level
        val actions = mutableListOf<com.yangchengwei.easytrip.workspace.TripWorkspaceAction>()
        var currentLevel by mutableStateOf(level)
        val heights = mutableMapOf<WorkspaceSheetLevel, Float>()
        val collapsedSummaryVisibility = mutableMapOf<WorkspaceSheetLevel, Boolean>()
        val gestureTarget = when (level) {
            WorkspaceSheetLevel.COLLAPSED -> WorkspaceSheetLevel.HALF
            WorkspaceSheetLevel.HALF -> WorkspaceSheetLevel.EXPANDED
            WorkspaceSheetLevel.EXPANDED -> WorkspaceSheetLevel.HALF
        }
        return ComposeScenario(
            ScenarioFixture(fixtureId, ScenarioScreen.WORKSPACE),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
            reset = {
                spec.requireIdentity(scenarioNumber, frameId, fixtureId)
                actions.clear()
                heights.clear()
                collapsedSummaryVisibility.clear()
                currentLevel = level
            },
            content = {
                val ready = readyState(sheetLevel = currentLevel)
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(ready),
                    mapState = WorkspaceMapState.Ready,
                    onAction = { action ->
                        actions += action
                        if (action is com.yangchengwei.easytrip.workspace.TripWorkspaceAction.SetSheetLevel) {
                            currentLevel = action.level
                        }
                    },
                    placeState = PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(days = ready.days, selectedDayId = ready.days.first().id),
                    onItineraryAction = {},
                    mapContent = {},
                )
            },
            interact = {
                WorkspaceSheetLevel.entries.forEach { target ->
                    runOnIdle { currentLevel = target }
                    waitForIdle()
                    heights[target] = onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot().let { it.bottom.value - it.top.value }
                    collapsedSummaryVisibility[target] = runCatching {
                        onNodeWithTag("workspace-collapsed-summary").assertIsDisplayed()
                    }.isSuccess
                }
                runOnIdle { currentLevel = level; actions.clear() }
                waitForIdle()
                onNodeWithTag("workspace-sheet-handle").performTouchInput {
                    down(center)
                    val delta = if (level == WorkspaceSheetLevel.EXPANDED) 200f else -200f
                    moveTo(Offset(center.x, center.y + delta))
                    advanceEventTime(100)
                    up()
                }
            },
            verify = {
                spec.requireIdentity(scenarioNumber, frameId, fixtureId)
                check(heights.getValue(WorkspaceSheetLevel.COLLAPSED) < heights.getValue(WorkspaceSheetLevel.HALF))
                check(heights.getValue(WorkspaceSheetLevel.HALF) < heights.getValue(WorkspaceSheetLevel.EXPANDED))
                check(collapsedSummaryVisibility == mapOf(
                    WorkspaceSheetLevel.COLLAPSED to true,
                    WorkspaceSheetLevel.HALF to false,
                    WorkspaceSheetLevel.EXPANDED to false,
                ))
                check(actions == listOf(com.yangchengwei.easytrip.workspace.TripWorkspaceAction.SetSheetLevel(gestureTarget)))
                onNodeWithTag("workspace-map").assertIsDisplayed()
            },
        )
    }

    private fun deleteTripDay(id: String): V1ScenarioExecutable {
        var confirmed = false
        return settingsScenario(
            id,
            TripSettingsUiState(
                tripId = "trip-1",
                name = "杭州周末",
                days = listOf(DayUi("day-1", "Day 1"), DayUi("day-2", "Day 2")),
                pendingDayDeletion = PendingDayDeletion(DayUi("day-2", "Day 2"), DayDeleteImpact(2, 1, 3)),
            ),
            onConfirmDeleteDay = { confirmed = true },
            interact = { onNodeWithText("确认删除").performClick() },
            verify = {
                onNodeWithText("将删除 2 个行程项和 1 个路线段；3 个收藏地点会保留。此操作不可撤销，后续旅行日日期编号和路线将变化。").assertIsDisplayed()
                check(confirmed)
            },
        )
    }

    private fun changeDateRange(id: String): V1ScenarioExecutable {
        var confirmed = false
        return settingsScenario(
            id,
            TripSettingsUiState(
                tripId = "trip-1",
                name = "杭州周末",
                days = listOf(DayUi("day-1", "Day 1"), DayUi("day-2", "Day 2")),
                dateRange = DateRangeChangeUiState(
                    startDate = LocalDate.of(2026, 9, 1),
                    baselineEndDate = LocalDate.of(2026, 9, 2),
                    endDate = LocalDate.of(2026, 9, 1),
                    isDirty = true,
                    phase = DateRangeChangeRequest(
                        1,
                        "trip-1",
                        LocalDate.of(2026, 9, 1),
                        listOf("day-1", "day-2"),
                        LocalDate.of(2026, 9, 1),
                    ).let { request ->
                        DateRangeChangePhase.AwaitingConfirmation(
                            request,
                            DateRangeChangeImpact(
                                request,
                                listOf("day-1"), listOf("day-2"), 2, 1, 3,
                            ),
                        )
                    },
                ),
            ),
            onConfirmDateRange = { confirmed = true },
            interact = { onNodeWithText("确认修改").performClick() },
            verify = { onNodeWithText("确认修改日期范围？").assertIsDisplayed(); check(confirmed) },
        )
    }

    private fun undoSuccess(id: String): V1ScenarioExecutable {
        var closed = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
            reset = { closed = false },
            content = {
                val ready = readyState(overlay = WorkspaceOverlay.AddToItineraryResult)
                TripWorkspaceScreen(
                    pageState = TripWorkspacePageState.Ready(ready),
                    consent = null,
                    onAction = {},
                    onMarkerClick = {},
                    onMapPoiClick = {},
                    placeState = PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(days = ready.days, selectedDayId = ready.days.first().id),
                    addToItineraryState = AddToItineraryUiState(
                        step = AddToItineraryStep.COMPLETED,
                        result = AddPlacesOutcome.Success("day-1", listOf("item-1")),
                        undoBatches = emptyList(),
                    ),
                    onItineraryAction = {},
                    onCloseOverlay = { closed = true },
                    onDismissMapPlace = {},
                )
            },
            interact = {
                onNodeWithText("已撤销本次新增，收藏地点仍保留。").assertIsDisplayed()
                onNodeWithText("撤销").assertIsNotEnabled()
                onNodeWithText("关闭").performClick()
            },
            verify = { check(closed) },
        )
    }

    private fun validCreate(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<CreateTripAction>()
        val state = CreateTripUiState(
            name = "杭州周末",
            dayCount = "3",
            timeMode = CreateTimeMode.DATED,
            startDate = LocalDate.of(2026, 9, 1),
            travelMode = TravelMode.FLEXIBLE,
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.CREATE_TRIP),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.CREATE_TRIP)),
            actions::clear,
            { CreateTripContent(state, actions::add) },
            { onNodeWithTag("create-submit").performClick() },
            {
                onNodeWithTag("create-name").assertTextContains("杭州周末")
                onNodeWithText("2026-09-01").assertIsDisplayed()
                onNodeWithText("2026-09-03").assertIsDisplayed()
                onNodeWithTag("create-day-count").assertTextContains("3")
                onNodeWithTag("create-mode-FLEXIBLE").assertIsDisplayed()
                onNodeWithTag("create-submit").assertIsDisplayed()
                check(actions == listOf(CreateTripAction.Submit))
            },
        )
    }

    private fun dateSelection(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<CreateTripAction>()
        var state by mutableStateOf(CreateTripUiState("杭州周末", "3", CreateTimeMode.DRAFT))
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.DATE_PICKER),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.CREATE_TRIP, ScenarioScreen.DATE_PICKER)),
            reset = {
                actions.clear()
                state = CreateTripUiState("杭州周末", "3", CreateTimeMode.DRAFT)
            },
            content = {
                CreateTripContent(
                    state,
                    { action ->
                        actions += action
                        state = when (action) {
                            is CreateTripAction.TimeModeChanged -> state.copy(timeMode = action.value)
                            is CreateTripAction.StartDateChanged -> state.copy(startDate = action.value)
                            else -> state
                        }
                    },
                    initialDateMillis = 1_788_134_400_000,
                )
            },
            interact = {
                onNodeWithTag("create-time-DATED").performClick()
                onNodeWithTag("create-date-confirm").performClick()
            },
            verify = {
                onNodeWithText("2026-08-31").assertIsDisplayed()
                onNodeWithText("2026-09-02").assertIsDisplayed()
                check(actions.first() == CreateTripAction.TimeModeChanged(CreateTimeMode.DATED))
                check(actions.last() == CreateTripAction.StartDateChanged(LocalDate.of(2026, 8, 31)))
            },
        )
    }

    private fun itemEdit(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITEM_EDITOR),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ITEM_EDITOR)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(editDraft = ItineraryEditDraft("item-1", "09:30", "60")),
                    onAction = actions::add,
                )
            },
            { onNodeWithText("保存时间").performClick() },
            { onNodeWithText("到达与停留").assertIsDisplayed(); check(actions == listOf(DayItineraryAction.SaveEdit)) },
        )
    }

    private fun routeEdit(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val leg = RouteLegUi("leg-1", "from", "to", TransportMode.WALK, RouteStatus.SUCCESS, 800, 600, null)
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ROUTE_EDITOR),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ROUTE_EDITOR)),
            actions::clear,
            { DayItineraryContent(DayItineraryUiState(legs = listOf(leg), modeEditor = com.yangchengwei.easytrip.itinerary.ui.RouteModeEditDraft("leg-1", TransportMode.WALK)), onAction = actions::add) },
            { onNodeWithTag("mode-option-DRIVE").performClick() },
            { onNodeWithText("选择交通方式").assertIsDisplayed(); check(actions == listOf(DayItineraryAction.SelectMode(TransportMode.DRIVE))) },
        )
    }

    private fun statusMatrix(id: String): V1ScenarioExecutable {
        val legs = listOf(
            RouteLegUi("waiting", "a", "b", TransportMode.WALK, RouteStatus.WAITING_NETWORK, null, null, null),
            RouteLegUi("pending", "b", "c", TransportMode.TAXI, RouteStatus.PENDING, null, null, null),
            RouteLegUi("calculating", "c", "d", TransportMode.DRIVE, RouteStatus.CALCULATING, null, null, null),
            RouteLegUi("success", "d", "e", TransportMode.TRANSIT, RouteStatus.SUCCESS, 1200, 900, null),
            RouteLegUi("failed", "e", "f", TransportMode.WALK, RouteStatus.FAILED, null, null, "路线失败"),
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.STATUS_MATRIX),
            ScenarioPath(listOf(ScenarioScreen.STATUS_MATRIX)),
            content = { Column { legs.forEach { RouteLegContent(it) } } },
            verify = {
                onNodeWithText("联网后计算路线").assertIsDisplayed()
                onNodeWithContentDescription("离线，联网后计算路线").assertIsDisplayed()
                onAllNodesWithText("正在计算路线").assertCountEquals(1)
                onNodeWithText("路线失败").assertIsDisplayed()
            },
        )
    }

    private fun searchLoading(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.SEARCH),
        ScenarioPath(listOf(ScenarioScreen.WORKSPACE, ScenarioScreen.SEARCH)),
        content = { PlaceSearchContent(PlaceSearchUiState(search = PlaceSearchState("故宫", phase = PlaceSearchPhase.Loading)), {}) },
        verify = { onNodeWithText("正在搜索地点").assertIsDisplayed(); onNodeWithText("正在查找“故宫”相关结果…").assertIsDisplayed() },
    )

    private fun readyState(
        sheetLevel: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF,
        overlay: WorkspaceOverlay = WorkspaceOverlay.None,
    ): TripWorkspaceReadyState {
        val day = TripDay("day-1", 0)
        return TripWorkspaceReadyState(
            tripName = "杭州周末",
            days = listOf(day),
            section = WorkspaceSection.ITINERARY,
            itineraryScope = ItineraryScope.Day(day.id),
            wholeTripDays = emptyList(),
            sheetLevel = sheetLevel,
            map = MapUiModel(),
            mapLayer = MapLayer.STANDARD,
            selectedMarker = null,
            selectedMarkerPoi = null,
            selectedMapPoi = null,
            overlay = overlay,
        )
    }

    private fun workspaceScenario(
        id: String,
        actions: MutableList<com.yangchengwei.easytrip.workspace.TripWorkspaceAction>,
        ready: TripWorkspaceReadyState,
        mapState: WorkspaceMapState,
        reset: () -> Unit = {},
        onMapRetry: () -> Unit = { actions.add(com.yangchengwei.easytrip.workspace.TripWorkspaceAction.Retry) },
        interact: V1ComposeRule.() -> Unit = {},
        verify: V1ComposeRule.() -> Unit,
    ) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.WORKSPACE),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
        {
            actions.clear()
            reset()
        },
        {
            val day = ready.days.firstOrNull()
            TripWorkspaceContent(
                pageState = TripWorkspacePageState.Ready(ready),
                mapState = mapState,
                onAction = actions::add,
                onMapRetry = onMapRetry,
                placeState = PlacePoolUiState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = ready.days, selectedDayId = day?.id),
                onItineraryAction = {},
                mapContent = {},
            )
        },
        interact,
        verify,
    )

    private fun settingsScenario(
        id: String,
        state: TripSettingsUiState,
        onConfirmDateRange: () -> Unit = {},
        onConfirmDeleteDay: () -> Unit = {},
        interact: V1ComposeRule.() -> Unit = {},
        verify: V1ComposeRule.() -> Unit,
    ) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TRIP_SETTINGS),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.TRIP_SETTINGS)),
        content = {
            TripSettingsContent(
                state, {}, {}, {}, {}, {}, {}, onConfirmDateRange, {},
                {}, {}, {}, onConfirmDeleteDay,
            )
        },
        interact = interact,
        verify = verify,
    )

    private fun mapLoading(id: String): V1ScenarioExecutable {
        val day = TripDay("day-1", 0)
        val ready = TripWorkspaceReadyState(
            tripName = "杭州周末",
            days = listOf(day),
            section = com.yangchengwei.easytrip.workspace.WorkspaceSection.ITINERARY,
            itineraryScope = ItineraryScope.Day(day.id),
            wholeTripDays = emptyList(),
            sheetLevel = WorkspaceSheetLevel.HALF,
            map = MapUiModel(),
            mapLayer = MapLayer.STANDARD,
            selectedMarker = null,
            selectedMarkerPoi = null,
            selectedMapPoi = null,
            overlay = WorkspaceOverlay.None,
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.WORKSPACE),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
            content = {
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(ready),
                    mapState = WorkspaceMapState.Loading,
                    onAction = {},
                    placeState = PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(days = listOf(day), selectedDayId = day.id),
                    onItineraryAction = {},
                    mapContent = {},
                )
            },
            verify = {
                onNodeWithTag("workspace-map-loading").assertIsDisplayed()
                onNodeWithText("地图加载中").assertIsDisplayed()
                onNodeWithText("第1天 · 暂无行程").assertIsDisplayed()
            },
        )
    }

    private fun invalidCreate(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.CREATE_TRIP),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.CREATE_TRIP)),
        content = { CreateTripContent(CreateTripUiState(nameError = "请输入旅行名称", dayCountError = "请输入至少 1 天", dateError = "请选择开始日期", timeMode = CreateTimeMode.DATED), {}) },
        verify = {
            listOf(
                "create-name" to "请输入旅行名称",
                "create-day-count" to "请输入至少 1 天",
                "create-date-control" to "请选择开始日期",
            ).forEach { (tag, message) ->
                onNodeWithText(message).assertIsDisplayed().assert(hasAnyAncestor(hasTestTag("$tag-container")))
            }
        },
    )

    private fun emptyTrips(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TRIP_LIST),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST)),
        content = { TripListContent(TripListUiState(page = TripListPageState.Empty), {}) },
        verify = {
            onNodeWithText("开始规划一次旅行").assertIsDisplayed()
            onNodeWithText("创建旅行后，可以收藏地点并按天安排行程").assertIsDisplayed()
            onNodeWithTag("create-trip").assertIsDisplayed()
        },
    )

    private fun emptyDay(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(days = listOf(TripDay("day-2", 1)), selectedDayId = "day-2"),
                    onAction = actions::add,
                )
            },
            { onNodeWithTag("add-places-to-selected-day").performClick() },
            { onNodeWithText("第2天 · 暂无行程").assertIsDisplayed(); check(actions == listOf(DayItineraryAction.AddPlaces)) },
        )
    }

    private fun routeState(id: String, status: RouteStatus, message: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.ITINERARY),
        ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
        content = { RouteLegContent(RouteLegUi("leg", "a", "b", TransportMode.WALK, status, null, null, message)) },
        verify = { onNodeWithText(message).assertIsDisplayed() },
    )

    private fun searchState(id: String, phase: PlaceSearchPhase, message: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.SEARCH),
        ScenarioPath(listOf(ScenarioScreen.WORKSPACE, ScenarioScreen.SEARCH)),
        content = { PlaceSearchContent(PlaceSearchUiState(search = PlaceSearchState("故宫", phase = phase)), {}) },
        verify = { onNodeWithText(message).assertIsDisplayed() },
    )

    private fun permission(
        id: String,
        clickConfirm: Boolean = false,
    ): V1ScenarioExecutable {
        var confirmed = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PERMISSION),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PERMISSION)),
            { confirmed = false },
            { PermissionExplanationContent({ confirmed = true }, {}) },
            { if (clickConfirm) onNodeWithTag("permission-explanation-confirm").performClick() },
            {
                onNodeWithTag("permission-explanation-confirm").assertIsDisplayed()
                if (clickConfirm) check(confirmed)
            },
        )
    }

    private fun mapFailure(id: String): V1ScenarioExecutable {
        var retryCalls = 0
        return workspaceScenario(
            id,
            mutableListOf(),
            readyState(),
            WorkspaceMapState.Failed("地图加载失败"),
            reset = { retryCalls = 0 },
            onMapRetry = { retryCalls++ },
            interact = { onNodeWithTag("map-retry").performClick() },
            verify = {
                onNodeWithTag("map-load-failed").assertIsDisplayed()
                onNodeWithText("第1天 · 暂无行程").assertIsDisplayed()
                check(retryCalls == 1)
            },
        )
    }

    private fun editSaveFailure(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITEM_EDITOR),
            ScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ITEM_EDITOR)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(editDraft = ItineraryEditDraft("item-1", "09:30", "60", saveError = "保存失败")),
                    onAction = actions::add,
                )
            },
            { onNodeWithText("保存时间").performClick() },
            { onNodeWithText("保存失败").assertIsDisplayed(); check(actions == listOf(DayItineraryAction.SaveEdit)) },
        )
    }

}
