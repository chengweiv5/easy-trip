package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.yangchengwei.easytrip.itinerary.ui.AddToItinerarySubmissionResult
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryResultContent
import com.yangchengwei.easytrip.itinerary.ui.FailedItineraryAddition
import com.yangchengwei.easytrip.itinerary.ui.UndoCreatedItemsBatch
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
import com.yangchengwei.easytrip.permission.LocationPermissionSettingsContent
import com.yangchengwei.easytrip.trip.domain.TripDay
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
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi
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
data class DeclaredScenarioPath(val steps: List<ScenarioScreen>) {
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
    val declaredIdentity: V1ScenarioIdentity
        get() = error("Executable is not bound to a declared scenario identity")
    val fixture: ScenarioFixture
    val declaredPath: DeclaredScenarioPath
    val factoryIdentity: String
    fun setup()
    fun render(compose: V1ComposeRule)
    fun actions(compose: V1ComposeRule)
    fun assertions(compose: V1ComposeRule)
}

private class ComposeScenario(
    override val fixture: ScenarioFixture,
    override val declaredPath: DeclaredScenarioPath,
    private val reset: () -> Unit = {},
    private val content: @androidx.compose.runtime.Composable () -> Unit,
    private val interact: V1ComposeRule.() -> Unit = {},
    private val verify: V1ComposeRule.() -> Unit,
    override val factoryIdentity: String = fixture.id,
) : V1ScenarioExecutable {
    override fun setup() = reset()
    override fun render(compose: V1ComposeRule) = compose.setContent { EasyTripTheme { content() } }
    override fun actions(compose: V1ComposeRule) = compose.interact()
    override fun assertions(compose: V1ComposeRule) = compose.verify()
}

private class IdentityBoundScenario(
    override val declaredIdentity: V1ScenarioIdentity,
    executable: V1ScenarioExecutable,
) : V1ScenarioExecutable by executable

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

data class WorkspaceEmptyScenarioFixture(
    val ready: TripWorkspaceReadyState,
    val placeState: PlacePoolUiState,
    val itineraryState: DayItineraryUiState,
)

object V1ScenarioExecutableFactory {
    fun create(identity: V1ScenarioIdentity): V1ScenarioExecutable = create(identity) {
        IdentityBoundScenario(
            declaredIdentity = identity,
            executable = createDeclared(identity.parentNumber, identity.frameId, identity.fixtureId),
        )
    }

    internal fun create(
        identity: V1ScenarioIdentity,
        executableFactory: () -> V1ScenarioExecutable,
    ): V1ScenarioExecutable {
        require(identity in V1ScenarioFixtures.allIdentities) {
            "Unknown exact scenario identity: ${identity.signature}"
        }
        val executable = executableFactory()
        require(executable.declaredIdentity == identity) {
            "Scenario ${identity.signature} returned identity ${executable.declaredIdentity.signature}"
        }
        require(executable.fixture.id == identity.fixtureId) {
            "Scenario ${identity.parentNumber}/${identity.frameId} must use fixture ${identity.fixtureId}, was ${executable.fixture.id}"
        }
        require(executable.fixture.screen == identity.screen) {
            "Scenario ${identity.parentNumber}/${identity.frameId} must use screen ${identity.screen}, was ${executable.fixture.screen}"
        }
        require(executable.factoryIdentity == identity.factoryIdentity) {
            "Scenario ${identity.parentNumber}/${identity.frameId} must use factory ${identity.factoryIdentity}, was ${executable.factoryIdentity}"
        }
        return executable
    }

    private fun createDeclared(number: Int, frameId: String, fixtureId: String): V1ScenarioExecutable {
        require(
            V1ScenarioFixtures.allIdentities.any { identity ->
                identity.parentNumber == number &&
                    identity.frameId == frameId &&
                    identity.fixtureId == fixtureId
            },
        ) { "Unknown scenario identity: number=$number frame=$frameId fixture=$fixtureId" }
        return when (number to frameId) {
        1 to "K9h3r" -> existingTrips(fixtureId)
        1 to "d1sTtb" -> deletedTripFinalState(fixtureId)
        2 to "BrYVA" -> workspaceAllEmpty(fixtureId)
        2 to "jQhXs" -> shortPlacePool(fixtureId)
        4 to "WFOpg" -> itineraryAllEmpty(fixtureId)
        4 to "nAdK8" -> workspaceItinerary(fixtureId)
        4 to "mz2IS" -> workspaceItemEditComplete(fixtureId)
        4 to "eHTX3" -> workspaceSingleDayRoute(fixtureId)
        10 to "XsGon" -> onlyCollectedPlaceDetail(fixtureId)
        else -> when (number) {
        1 -> existingTrips(fixtureId)
        2 -> placePool(fixtureId)
        3 -> searchResults(fixtureId)
        4 -> workspaceItinerary(fixtureId)
        6 -> wholeTripItinerary(fixtureId)
        7 -> validCreate(fixtureId)
        8 -> tripSettings(fixtureId)
        9 -> singlePlaceMultiDaySelection(fixtureId)
        10 -> placeDetail(fixtureId)
        11 -> itemEdit(fixtureId)
        12 -> routeEdit(fixtureId)
        13 -> deleteTripConfirmation(fixtureId)
        14 -> statusMatrix(fixtureId)
        15 -> noTripDaysAddGuidance(fixtureId)
        16 -> workspaceSettings(fixtureId)
        17 -> mapLayer(fixtureId)
        18 -> addTripDay(fixtureId)
        19 -> selectedDayMultiPlacePicker(fixtureId)
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
        30 -> mapConsentExplanation(fixtureId)
        31 -> addSuccessResult(fixtureId)
        32 -> deleteItineraryItem(fixtureId)
        33 -> longAddTargetDayList(fixtureId)
        34 -> locationExplanation(fixtureId)
        35 -> locationSettingsRecovery(fixtureId)
        36 -> emptyTrips(fixtureId)
        37 -> emptyDay(fixtureId)
        38 -> searchState(fixtureId, PlaceSearchPhase.NetworkFailure("无法搜索新的地点"), "网络连接失败")
        39 -> addPartialSuccessResult(fixtureId)
        40 -> changeDateRange(fixtureId)
        41 -> targetDay(fixtureId, targetMissing = false, submitting = true)
        42 -> missingAddTargetDayResult(fixtureId)
        43 -> undoSuccess(fixtureId)
        44 -> searchLoading(fixtureId)
        45 -> mapLoading(fixtureId)
        46 -> mapFailure(fixtureId)
        47 -> invalidCreate(fixtureId)
        48 -> editSaveFailure(fixtureId)
        else -> error("Unsupported V1 scenario: $number")
        }
        }
    }

    private fun workspaceAllEmpty(fixtureId: String): V1ScenarioExecutable = workspaceEmptyScenario(
        fixtureId = fixtureId,
        section = WorkspaceSection.PLACE_POOL,
        isWorkspaceAllEmpty = true,
        isItineraryAllEmpty = true,
        expectedTag = "workspace-all-empty",
        expectedText = "旅行还是空的",
    )

    private fun itineraryAllEmpty(fixtureId: String): V1ScenarioExecutable = workspaceEmptyScenario(
        fixtureId = fixtureId,
        section = WorkspaceSection.ITINERARY,
        isWorkspaceAllEmpty = false,
        isItineraryAllEmpty = true,
        expectedTag = "itinerary-all-empty",
        expectedText = "还没有安排行程",
    )

    private fun itineraryAllEmptyFixture(): WorkspaceEmptyScenarioFixture {
        val day = TripDay("day-1", 0)
        val placeState = PlacePoolUiState(rows = listOf(SavedPlaceRowUi(savedPlace(), 0, false)))
        val wholeTripDays = listOf(WholeTripDayUi(day.id, 1, emptyList(), emptyList()))
        return WorkspaceEmptyScenarioFixture(
            ready = TripWorkspaceReadyState(
                tripName = "杭州周末",
                days = listOf(day),
                section = WorkspaceSection.ITINERARY,
                itineraryScope = ItineraryScope.Day(day.id),
                wholeTripDays = wholeTripDays,
                sheetLevel = WorkspaceSheetLevel.HALF,
                map = MapUiModel(),
                mapLayer = MapLayer.STANDARD,
                selectedMarker = null,
                selectedMarkerPoi = null,
                selectedMapPoi = null,
                overlay = WorkspaceOverlay.None,
                isItineraryAllEmpty = true,
                isWorkspaceAllEmpty = false,
            ),
            placeState = placeState,
            itineraryState = DayItineraryUiState(days = listOf(day), selectedDayId = day.id),
        )
    }

    private fun workspaceEmptyScenario(
        fixtureId: String,
        section: WorkspaceSection,
        isWorkspaceAllEmpty: Boolean,
        isItineraryAllEmpty: Boolean,
        expectedTag: String,
        expectedText: String,
    ): V1ScenarioExecutable {
        val day = TripDay("day-1", 0)
        val reachableFixture = if (isItineraryAllEmpty && !isWorkspaceAllEmpty) itineraryAllEmptyFixture() else null
        val ready = reachableFixture?.ready ?: TripWorkspaceReadyState(
            tripName = "杭州周末",
            days = listOf(day),
            section = section,
            itineraryScope = ItineraryScope.Day(day.id),
            wholeTripDays = emptyList(),
            sheetLevel = WorkspaceSheetLevel.HALF,
            map = MapUiModel(),
            mapLayer = MapLayer.STANDARD,
            selectedMarker = null,
            selectedMarkerPoi = null,
            selectedMapPoi = null,
            overlay = WorkspaceOverlay.None,
            isItineraryAllEmpty = isItineraryAllEmpty,
            isWorkspaceAllEmpty = isWorkspaceAllEmpty,
        )
        return ComposeScenario(
            ScenarioFixture(fixtureId, ScenarioScreen.WORKSPACE),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
            content = {
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(ready),
                    mapState = WorkspaceMapState.Ready,
                    onAction = {},
                    placeState = reachableFixture?.placeState ?: PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = reachableFixture?.itineraryState ?: DayItineraryUiState(days = listOf(day), selectedDayId = day.id),
                    onItineraryAction = {},
                    mapContent = { _ -> },
                )
            },
            verify = {
                onNodeWithTag(expectedTag).assertIsDisplayed()
                onNodeWithText(expectedText).assertIsDisplayed()
                if (isItineraryAllEmpty && !isWorkspaceAllEmpty) {
                    onAllNodesWithTag("itinerary-scope-rail").assertCountEquals(0)
                }
            },
        )
    }

    private fun savedPlace() = SavedPlace("place-1", "trip-1", "poi-1", "西湖", "杭州市西湖区", GeoPoint(30.25, 120.15), "湖边散步", emptyList())

    private fun placeRows() = listOf(SavedPlaceRowUi(savedPlace(), 1, true))

    private fun placePool(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<PlacePoolAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
            actions::clear,
            { com.yangchengwei.easytrip.place.ui.PlacePoolContent(PlacePoolUiState(rows = placeRows()), showSearch = false, onAction = actions::add) },
            { onNodeWithTag("start-add-to-itinerary").performClick() },
            { onNodeWithText("西湖").assertIsDisplayed(); check(actions == listOf(PlacePoolAction.StartAddToItinerary)) },
        )
    }

    private fun searchResults(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.SEARCH),
        DeclaredScenarioPath(listOf(ScenarioScreen.WORKSPACE, ScenarioScreen.SEARCH)),
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.PLACE_DETAIL)),
            { saved = false },
            { PlaceDetailContent(savedPlace(), PlaceDetailDraft("湖边散步", setOf("自然"), "place-1"), false, null, {}, {}, {}, { saved = true }) },
            { onNodeWithText("保存").performClick() },
            { onNodeWithTag("place-detail-title").assertIsDisplayed(); check(saved) },
        )
    }

    private fun shortPlacePool(id: String): V1ScenarioExecutable {
        val rows = (1..3).map { index ->
            SavedPlaceRowUi(savedPlace().copy(id = "place-$index", amapPoiId = "poi-$index", name = "收藏地点$index"), 0, false)
        }
        val ready = readyState()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.WORKSPACE),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
            content = {
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(ready.copy(section = WorkspaceSection.PLACE_POOL)),
                    mapState = WorkspaceMapState.Ready,
                    onAction = {},
                    placeState = PlacePoolUiState(rows = rows),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(days = ready.days, selectedDayId = ready.days.first().id),
                    onItineraryAction = {},
                    mapContent = { _ -> },
                )
            },
            verify = { rows.forEach { onNodeWithText(it.place.name).assertIsDisplayed() } },
        )
    }

    private fun onlyCollectedPlaceDetail(id: String): V1ScenarioExecutable {
        val place = savedPlace()
        val ready = readyState()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.WORKSPACE),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.PLACE_DETAIL)),
            content = {
                TripWorkspaceScreen(
                    pageState = TripWorkspacePageState.Ready(
                        ready.copy(
                            section = WorkspaceSection.PLACE_POOL,
                            overlay = WorkspaceOverlay.PlaceDetail(1L),
                            schedulesByPlaceId = mapOf(place.id to PlaceScheduleSummaryUi(isKnown = true)),
                        ),
                    ),
                    consent = null,
                    onAction = {},
                    onMarkerClick = {},
                    onMapPoiClick = {},
                    placeState = PlacePoolUiState(
                        rows = listOf(SavedPlaceRowUi(place, 0, false)),
                        selectedDetailPlaceId = place.id,
                        selectedDetailPlace = place,
                    ),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(days = ready.days, selectedDayId = ready.days.first().id),
                    onItineraryAction = {},
                    onCloseOverlay = {},
                    onDismissMapPlace = {},
                )
            },
            verify = {
                onNodeWithTag("place-detail-bookmark-outline").assertIsDisplayed()
                onAllNodesWithText("已加入行程").assertCountEquals(0)
                onNodeWithText("加入行程").assertIsDisplayed()
            },
        )
    }

    private fun selectPlaces(id: String): V1ScenarioExecutable {
        var continued = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
            { continued = false },
            { SelectPlacesContent(placeRows(), AddToItineraryUiState(selectedPlaceIds = listOf("place-1"), step = AddToItineraryStep.SELECT_PLACES), {}, { continued = true }, {}) },
            { onNodeWithTag("select-places-continue").performClick() },
            { onNodeWithText("已选 1 个").assertIsDisplayed(); check(continued) },
        )
    }

    private fun targetDay(id: String, targetMissing: Boolean, submitting: Boolean) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TARGET_DAY),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.TARGET_DAY)),
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
            else onNodeWithText("加入行程").assertIsDisplayed()
        },
    )

    private fun longTargetDays(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TARGET_DAY),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.TARGET_DAY)),
        content = { SelectTargetDayContent((0 until 30).map { TripDay("day-$it", it) }, AddToItineraryUiState(selectedPlaceIds = listOf("place-1"), validityInitialized = true, step = AddToItineraryStep.SELECT_TARGET_DAY), {}, {}, {}) },
        verify = { onNodeWithTag("select-target-day-list").assertIsDisplayed(); onNodeWithText("请选择旅行日").assertIsDisplayed() },
    )

    private fun singlePlaceMultiDaySelection(id: String): V1ScenarioExecutable {
        var state by mutableStateOf(
            AddToItineraryUiState(
                selectedPlaceIds = listOf("place-1"),
                editingTarget = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace("place-1"),
                validityInitialized = true,
                step = AddToItineraryStep.SELECT_TARGET_DAY,
            ),
        )
        val days = listOf(TripDay("day-1", 0), TripDay("day-2", 1))
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.TARGET_DAY),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.TARGET_DAY)),
            reset = { state = state.copy(selectedTargetDayIds = emptyList(), targetDayId = null) },
            content = {
                SelectTargetDayContent(
                    days = days,
                    state = state,
                    onSelectDay = {},
                    onToggleDay = { dayId ->
                        val selected = state.selectedTargetDayIds.toMutableList().apply {
                            if (!remove(dayId)) add(dayId)
                        }
                        state = state.copy(selectedTargetDayIds = selected, targetDayId = selected.firstOrNull())
                    },
                    onSubmit = {},
                    selectedPlaceName = "西湖",
                )
            },
            interact = { onNodeWithTag("target-day-day-1").performClick(); onNodeWithTag("target-day-day-2").performClick() },
            verify = { onNodeWithText("西湖").assertIsDisplayed(); onNodeWithText("加入 2 天").assertIsDisplayed() },
        )
    }

    private fun noTripDaysAddGuidance(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TARGET_DAY),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.TARGET_DAY)),
        content = {
            SelectTargetDayContent(
                days = emptyList(),
                state = AddToItineraryUiState(
                    selectedPlaceIds = listOf("place-1"),
                    editingTarget = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace("place-1"),
                    step = AddToItineraryStep.SELECT_TARGET_DAY,
                ),
                onSelectDay = {}, onSubmit = {}, selectedPlaceName = "西湖",
            )
        },
        verify = { onNodeWithText("还没有旅行日").assertIsDisplayed(); onNodeWithTag("go-to-itinerary-add-day").assertIsDisplayed() },
    )

    private fun longAddTargetDayList(id: String): V1ScenarioExecutable {
        val days = (0 until 30).map { TripDay("day-$it", it) }
        var state by mutableStateOf(
            AddToItineraryUiState(
                selectedPlaceIds = listOf("place-1"),
                editingTarget = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForPlace("place-1"),
                validityInitialized = true,
                step = AddToItineraryStep.SELECT_TARGET_DAY,
            ),
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.TARGET_DAY),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.TARGET_DAY)),
            reset = { state = state.copy(selectedTargetDayIds = emptyList(), targetDayId = null) },
            content = { SelectTargetDayContent(days, state, {}, { dayId -> state = state.copy(selectedTargetDayIds = listOf(dayId), targetDayId = dayId) }, {}, selectedPlaceName = "西湖") },
            interact = {
                onNodeWithTag("select-target-day-list").performTouchInput { swipeUp() }
                onNodeWithTag("target-day-day-29").performScrollTo().performClick()
            },
            verify = { onNodeWithText("加入第 30 天").assertIsDisplayed(); onNodeWithTag("select-target-day-submit").assertIsDisplayed() },
        )
    }

    private fun selectedDayMultiPlacePicker(id: String): V1ScenarioExecutable {
        val rows = listOf(
            SavedPlaceRowUi(savedPlace().copy(id = "place-1", name = "西湖"), 0, false),
            SavedPlaceRowUi(savedPlace().copy(id = "place-2", name = "灵隐寺"), 0, false),
        )
        var state by mutableStateOf(
            AddToItineraryUiState(targetDayId = "day-1", editingTarget = com.yangchengwei.easytrip.itinerary.ui.AddToItineraryEditingTarget.ForDay("day-1"), step = AddToItineraryStep.SELECT_PLACES),
        )
        var continued = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.PLACE_POOL)),
            reset = { state = state.copy(selectedPlaceIds = emptyList()); continued = false },
            content = { SelectPlacesContent(rows, state, { placeId -> state = state.copy(selectedPlaceIds = state.selectedPlaceIds.toMutableList().apply { if (!remove(placeId)) add(placeId) }) }, { continued = true }, {}) },
            interact = { onNodeWithTag("select-place-place-1").performClick(); onNodeWithTag("select-place-place-2").performClick(); onNodeWithTag("select-places-continue").performClick() },
            verify = { onNodeWithText("已选 2 个").assertIsDisplayed(); check(continued) },
        )
    }

    private fun addComplete(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.ITINERARY),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL, ScenarioScreen.TARGET_DAY, ScenarioScreen.ITINERARY)),
        content = { DayItineraryContent(DayItineraryUiState(days = listOf(TripDay("day-1", 0)), selectedDayId = "day-1", items = listOf(ItineraryItemUi("item-1", "西湖", "杭州", null, null)), previewOrder = listOf("item-1")), onAction = {}) },
        verify = { onNodeWithText("西湖").assertIsDisplayed() },
    )

    private fun emptyPlacePool(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.PLACE_POOL),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PLACE_POOL)),
        content = { com.yangchengwei.easytrip.place.ui.PlacePoolContent(PlacePoolUiState(), showSearch = false, onAction = {}) },
        verify = { onNodeWithText("还没有收藏地点").assertIsDisplayed(); onNodeWithText("搜索地点").assertIsDisplayed() },
    )

    private fun deletedTripFinalState(id: String): V1ScenarioExecutable = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TRIP_LIST),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST)),
        content = {
            TripListContent(
                TripListUiState(
                    page = TripListPageState.Content(
                        primaryTrip = tripCard("trip-2", "川西小环线", "10天9晚", "2026年5月15日", "自驾"),
                        otherTrips = listOf(
                            tripCard("trip-3", "泉州古城散步", "7天6晚", "2026年5月20日", "灵活"),
                        ),
                    ),
                ),
                onAction = {},
            )
        },
        verify = {
            onAllNodesWithTag("primary-trip-trip-1").assertCountEquals(0)
            onAllNodesWithTag("continue-trip-trip-1").assertCountEquals(0)
            onNodeWithTag("primary-trip-trip-2").assertIsDisplayed()
            onNodeWithText("川西小环线").assertIsDisplayed()
            onNodeWithText("泉州古城散步").assertIsDisplayed()
        },
        factoryIdentity = "variant-d1sTtb-deleted-final-state",
    )

    private fun existingTrips(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<TripListAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.TRIP_LIST),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST)),
            actions::clear,
            {
                TripListContent(
                    TripListUiState(
                        page = TripListPageState.Content(
                            primaryTrip = tripCard("trip-1", "杭州周末", "3天2晚", "2026年9月1日", "灵活"),
                            otherTrips = listOf(tripCard("trip-2", "东京秋日", "5天4晚", "2026年10月2日", "自驾")),
                        ),
                    ),
                    actions::add,
                )
            },
            { onNodeWithTag("primary-trip-trip-1").performClick() },
            {
                onNodeWithText("杭州周末").assertIsDisplayed()
                onNodeWithText("2026年9月1日 · 3天2晚").assertIsDisplayed()
                onNodeWithTag("primary-trip-trip-1").assertIsDisplayed()
                onAllNodesWithTag("continue-trip-trip-1").assertCountEquals(0)
                onNodeWithTag("trip-menu-trip-1").assertIsDisplayed()
                onNodeWithTag("trip-menu-trip-2").assertIsDisplayed()
                onAllNodesWithText("继续规划").assertCountEquals(0)
                check(actions == listOf(TripListAction.OpenTrip("trip-1")))
            },
        )
    }

    private fun workspaceItinerary(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val day = TripDay("day-1", 0)
        val items = itineraryItemsForBatch5()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            actions::clear,
            {
                WorkspaceItineraryContent(
                    days = listOf(day),
                    selected = ItineraryScope.Day(day.id),
                    wholeTripDays = listOf(WholeTripDayUi(day.id, 1, items, itineraryLegsForBatch5(items))),
                    onSelect = {},
                    dayContent = {
                        DayItineraryContent(
                            DayItineraryUiState(
                                days = listOf(day),
                                selectedDayId = day.id,
                                items = items,
                                previewOrder = items.map(ItineraryItemUi::id),
                                legs = itineraryLegsForBatch5(items),
                            ),
                            onAction = actions::add,
                        )
                    },
                )
            },
            { onNodeWithTag("more-item-1", useUnmergedTree = true).performClick() },
            { onNodeWithText("西湖").assertIsDisplayed(); onNodeWithTag("menu-timing-item-1", useUnmergedTree = true).assertIsDisplayed() },
        )
    }

    private fun workspaceItemEditComplete(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val item = ItineraryItemUi("item-1", "西湖", "杭州市西湖区", java.time.LocalTime.of(9, 30), 60, "二层入口集合")
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITEM_EDITOR),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ITEM_EDITOR)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(
                        days = listOf(TripDay("day-1", 0)),
                        selectedDayId = "day-1",
                        items = listOf(item),
                        previewOrder = listOf(item.id),
                        editDraft = ItineraryEditDraft(item.id, "09:30", "60", "二层入口集合"),
                    ),
                    onAction = actions::add,
                )
            },
            { onNodeWithText("取消").performClick() },
            {
                onNodeWithTag("arrival-time-input").assertTextContains("09:30")
                onNodeWithTag("stay-minutes-input").assertTextContains("60")
                onNodeWithTag("itinerary-note-input").assertTextContains("二层入口集合")
                check(actions == listOf(DayItineraryAction.DismissDialogs))
            },
        )
    }

    private fun workspaceSingleDayRoute(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val items = itineraryItemsForBatch5()
        val leg = itineraryLegsForBatch5(items).single()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ROUTE_EDITOR),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ROUTE_EDITOR)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(
                        days = listOf(TripDay("day-1", 0)),
                        selectedDayId = "day-1",
                        items = items,
                        previewOrder = items.map(ItineraryItemUi::id),
                        legs = listOf(leg),
                    ),
                    onAction = actions::add,
                )
            },
            { onNodeWithTag("edit-route-${leg.id}").performClick() },
            {
                onNodeWithTag("leg-${leg.id}").assertIsDisplayed()
                check(actions == listOf(DayItineraryAction.RequestMode(leg.id)))
            },
        )
    }

    private fun itineraryItemsForBatch5() = listOf(
        ItineraryItemUi("item-1", "西湖", "杭州市西湖区", java.time.LocalTime.of(9, 30), 60, "二层入口集合"),
        ItineraryItemUi("item-2", "博物馆", "杭州市上城区", null, null),
    )

    private fun itineraryLegsForBatch5(items: List<ItineraryItemUi>) = listOf(
        RouteLegUi("leg-1", items[0].id, items[1].id, TransportMode.DRIVE, RouteStatus.SUCCESS, 1_200, 900, null, 1_200, "避开拥堵"),
    )

    private fun wholeTripItinerary(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.ITINERARY),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.TRIP_SETTINGS)),
            { confirmed = false },
            { AddTripDayContent(false, null, { confirmed = true }, {}) },
            { onNodeWithText("添加一天").performClick() },
            { onNodeWithText("新的一天会追加到当前旅行末尾。").assertIsDisplayed(); check(confirmed) },
        )
    }

    private fun waitingForNetwork(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.ITINERARY),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
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
            onNodeWithText("等待联网后计算").assertIsDisplayed()
            onNodeWithContentDescription("离线，等待联网后计算").assertIsDisplayed()
            onAllNodesWithTag("edit-route-leg-1").assertCountEquals(0)
            onAllNodesWithTag("retry-leg-1").assertCountEquals(0)
            onNodeWithTag("add-places-to-selected-day").assertIsDisplayed()
        },
        factoryIdentity = "batch6-waiting-for-network",
    )

    private fun failedRoute(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val items = listOf(
            ItineraryItemUi("a", "西湖", "杭州", null, null),
            ItineraryItemUi("b", "灵隐寺", "杭州", null, null),
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
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
            {
                onNodeWithText("路线计算失败").assertIsDisplayed()
                onNodeWithText("路线失败").assertIsDisplayed()
                onAllNodesWithTag("edit-route-leg-1").assertCountEquals(0)
                check(actions == listOf(DayItineraryAction.Retry("leg-1", 0)))
            },
            factoryIdentity = "batch6-failed-route",
        )
    }

    private fun deleteItineraryItem(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(deleteConfirmation = ItineraryDeleteConfirmation("item-1", "西湖")),
                    onAction = actions::add,
                )
            },
            { onNodeWithText("确认移出").performClick() },
            {
                onNodeWithText("仅移除本次安排；收藏仍保留；相邻路线将重新计算。").assertIsDisplayed()
                check(actions == listOf(DayItineraryAction.ConfirmDelete))
            },
        )
    }

    private fun addSuccessResult(id: String) = addResultScenario(
        id = id,
        fixtureId = "add-success-result",
        state = AddToItineraryUiState(
            submissionResult = AddToItinerarySubmissionResult(createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("item-1")))),
            undoBatches = listOf(UndoCreatedItemsBatch("day-1", listOf("item-1"))),
        ),
        verify = { onNodeWithText("已加入第 1 天").assertIsDisplayed(); onNodeWithText("撤销").assertIsDisplayed() },
    )

    private fun addPartialSuccessResult(id: String) = addResultScenario(
        id = id,
        fixtureId = "add-partial-success-result",
        state = AddToItineraryUiState(
            submissionResult = AddToItinerarySubmissionResult(
                createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("item-1"))),
                failedAdditions = listOf(FailedItineraryAddition("day-2", "place-1")),
                retryTargetDayIds = listOf("day-2"),
            ),
            undoBatches = listOf(UndoCreatedItemsBatch("day-1", listOf("item-1"))),
        ),
        verify = { onNodeWithText("部分地点已加入行程").assertIsDisplayed(); onNodeWithText("第 2 天 · 西湖：未加入").assertIsDisplayed() },
    )

    private fun missingAddTargetDayResult(id: String) = addResultScenario(
        id = id,
        fixtureId = "missing-add-target-day-result",
        state = AddToItineraryUiState(
            submissionResult = AddToItinerarySubmissionResult(
                createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("item-1"))),
                missingTargetDayIds = listOf("day-2"),
                missingTargetDayLabels = mapOf("day-2" to "第 2 天"),
            ),
            undoBatches = listOf(UndoCreatedItemsBatch("day-1", listOf("item-1"))),
        ),
        verify = { onNodeWithText("所选旅行日已不存在").assertIsDisplayed(); onNodeWithText("请重新选择旅行日").assertIsDisplayed() },
    )

    private fun addResultScenario(
        id: String,
        fixtureId: String,
        state: AddToItineraryUiState,
        verify: V1ComposeRule.() -> Unit,
    ) = ComposeScenario(
        ScenarioFixture(fixtureId, ScenarioScreen.WORKSPACE),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
        content = {
            AddToItineraryResultContent(
                state = state,
                days = listOf(TripDay("day-1", 0), TripDay("day-2", 1)),
                placeNameForId = { "西湖" },
                onUndo = {}, onRetryFailed = {}, onReselectDates = {}, onViewResult = {}, onViewPlacePool = {}, onClose = {},
            )
        },
        verify = verify,
    )

    private fun partialRouteSuccess(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        val items = listOf(
            ItineraryItemUi("a", "西湖", "杭州", null, null),
            ItineraryItemUi("b", "灵隐寺", "杭州", null, null),
            ItineraryItemUi("c", "雷峰塔", "杭州", null, null),
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITINERARY),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
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
                check(actions == listOf(DayItineraryAction.Retry("failed", 0)))
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
        val primaryTrip = tripCard("trip-1", "杭州周末", "3天2晚", "2026年9月1日", "灵活")
        val retainedTrip = tripCard("trip-2", "东京秋日", "5天4晚", "2026年10月2日", "自驾")
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST)),
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
        var overlay by mutableStateOf<WorkspaceOverlay>(WorkspaceOverlay.None)
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.WORKSPACE),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
            reset = {
                actions.clear()
                overlay = WorkspaceOverlay.None
            },
            content = {
                val ready = readyState(overlay = overlay)
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(ready),
                    mapState = WorkspaceMapState.Ready,
                    onAction = { action ->
                        actions += action
                        overlay = when (action) {
                            is com.yangchengwei.easytrip.workspace.TripWorkspaceAction.OpenOverlay -> action.overlay
                            com.yangchengwei.easytrip.workspace.TripWorkspaceAction.CloseOverlay -> WorkspaceOverlay.None
                            else -> overlay
                        }
                    },
                    placeState = PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(days = ready.days, selectedDayId = ready.days.first().id),
                    onItineraryAction = {},
                    mapContent = { _ -> },
                )
            },
            interact = {
                onNodeWithTag("workspace-more").performClick()
                onNodeWithTag("more-menu-settings").performClick()
            },
            verify = {
                onNodeWithTag("workspace-top-bar").assertIsDisplayed()
                check(actions == listOf(
                    com.yangchengwei.easytrip.workspace.TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.MoreMenu),
                    com.yangchengwei.easytrip.workspace.TripWorkspaceAction.CloseOverlay,
                    com.yangchengwei.easytrip.workspace.TripWorkspaceAction.OpenSettings,
                ))
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
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
                    mapContent = { _ -> },
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
                onNodeWithText("此操作不可撤销，后续旅行日日期编号和路线将变化。").assertIsDisplayed()
                onNodeWithText("将删除").assertIsDisplayed()
                onNodeWithText("2 个行程项").assertIsDisplayed()
                onNodeWithText("1 个路线段").assertIsDisplayed()
                onNodeWithText("将保留").assertIsDisplayed()
                onNodeWithText("3 个收藏地点").assertIsDisplayed()
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

    private fun undoSuccess(id: String) = addResultScenario(
        id = id,
        fixtureId = "add-undo-success-result",
        state = AddToItineraryUiState(
            submissionResult = AddToItinerarySubmissionResult(createdItemsByDay = listOf(UndoCreatedItemsBatch("day-1", listOf("item-1")))),
        ),
        verify = { onNodeWithText("已从第 1 天移除，收藏地点仍保留").assertIsDisplayed(); onNodeWithText("查看地点池").assertIsDisplayed() },
    )

    private fun validCreate(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<CreateTripAction>()
        val state = CreateTripUiState(
            name = "杭州周末",
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 3),
            travelMode = TravelMode.FLEXIBLE,
        )
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.CREATE_TRIP),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.CREATE_TRIP)),
            actions::clear,
            { CreateTripContent(state, actions::add) },
            { onNodeWithTag("create-submit").performClick() },
            {
                onNodeWithTag("create-name").assertTextContains("杭州周末")
                onNodeWithText("2026-09-01").assertIsDisplayed()
                onNodeWithText("至 2026-09-03 · 3天2晚").assertIsDisplayed()
                onNodeWithTag("create-mode-FLEXIBLE").assertIsDisplayed()
                onNodeWithTag("create-submit").assertIsDisplayed()
                check(actions == listOf(CreateTripAction.Submit))
            },
        )
    }

    private fun dateSelection(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<CreateTripAction>()
        var state by mutableStateOf(CreateTripUiState(name = "杭州周末"))
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.DATE_PICKER),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.CREATE_TRIP, ScenarioScreen.DATE_PICKER)),
            reset = {
                actions.clear()
                state = CreateTripUiState(name = "杭州周末")
            },
            content = {
                CreateTripContent(
                    state,
                    { action ->
                        actions += action
                        if (action is CreateTripAction.DateRangeChanged) {
                            state = state.copy(startDate = action.startDate, endDate = action.endDate)
                        }
                    },
                    initialDateMillis = LocalDate.of(2026, 9, 1)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli(),
                )
            },
            interact = {
                onNodeWithTag("create-date-control").performClick()
                onNodeWithTag("trip-date-2026-09-01").performClick()
                onNodeWithTag("trip-date-2026-09-03").performClick()
                onNodeWithTag("trip-date-range-confirm").performClick()
            },
            verify = {
                onNodeWithText("2026-09-01").assertIsDisplayed()
                onNodeWithText("至 2026-09-03 · 3天2晚").assertIsDisplayed()
                check(actions == listOf(CreateTripAction.DateRangeChanged(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3))))
            },
        )
    }

    private fun itemEdit(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITEM_EDITOR),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ITEM_EDITOR)),
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ROUTE_EDITOR)),
            actions::clear,
            { DayItineraryContent(DayItineraryUiState(legs = listOf(leg), modeEditor = com.yangchengwei.easytrip.itinerary.ui.RouteModeEditDraft("leg-1", TransportMode.WALK)), onAction = actions::add) },
            { onNodeWithTag("route-mode-option-DRIVE").performClick() },
            { onNodeWithText("交通路段编辑").assertIsDisplayed(); check(actions == listOf(DayItineraryAction.SelectMode(TransportMode.DRIVE))) },
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
            DeclaredScenarioPath(listOf(ScenarioScreen.STATUS_MATRIX)),
            content = { Column { legs.forEach { RouteLegContent(it) } } },
            verify = {
                onNodeWithText("等待联网后计算").assertIsDisplayed()
                onNodeWithContentDescription("离线，等待联网后计算").assertIsDisplayed()
                onAllNodesWithText("正在计算路线").assertCountEquals(1)
                onNodeWithText("路线失败").assertIsDisplayed()
            },
        )
    }

    private fun searchLoading(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.SEARCH),
        DeclaredScenarioPath(listOf(ScenarioScreen.WORKSPACE, ScenarioScreen.SEARCH)),
        content = { PlaceSearchContent(PlaceSearchUiState(search = PlaceSearchState("故宫", phase = PlaceSearchPhase.Loading)), {}) },
        verify = { onNodeWithText("正在搜索地点").assertIsDisplayed(); onNodeWithText("正在查找“故宫”相关结果…").assertIsDisplayed() },
    )

    private fun tripCard(
        id: String,
        name: String,
        dayCountLabel: String,
        dateLabel: String,
        travelModeLabel: String,
    ) = TripCardUiModel(
        id = id,
        name = name,
        dayCountLabel = dayCountLabel,
        dateLabel = dateLabel,
        travelModeLabel = travelModeLabel,

        placeCount = 3,
        scheduledDayCount = 2,
        placeCountLabel = "3 个地点",
        tripDayCountLabel = "3 天行程",
        readinessPercent = 67,
        readinessLabel = "67%",
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
        factoryIdentity: String = id,
    ) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.WORKSPACE),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
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
                mapContent = { _ -> },
            )
        },
        interact,
        verify,
        factoryIdentity,
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
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.TRIP_SETTINGS)),
        content = {
            TripSettingsContent(
                state = state,
                onBack = {},
                onRename = {},
                onTravelMode = {},
                onSubmitDateRange = {},
                onCancelDateRange = {},
                onConfirmDateRange = onConfirmDateRange,
                onRetryDateRangeSync = {},
                onRequestDeleteDay = {},
                onRetryDeleteDay = {},
                onCancelDeleteDay = {},
                onConfirmDeleteDay = onConfirmDeleteDay,
                onDateRangeDraft = { _, _ -> },
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE)),
            content = {
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(ready),
                    mapState = WorkspaceMapState.Loading,
                    onAction = {},
                    placeState = PlacePoolUiState(),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(days = listOf(day), selectedDayId = day.id),
                    onItineraryAction = {},
                    mapContent = { _ -> },
                )
            },
            verify = {
                onNodeWithTag("workspace-map-loading").assertIsDisplayed()
                onNodeWithText("正在加载地图").assertIsDisplayed()
                onNodeWithText("地点和行程仍可继续查看").assertIsDisplayed()
                onNodeWithText("第1天 · 暂无行程").assertIsDisplayed()
            },
            factoryIdentity = "batch6-map-loading",
        )
    }

    private fun invalidCreate(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.CREATE_TRIP),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.CREATE_TRIP)),
        content = { CreateTripContent(CreateTripUiState(nameError = "请输入旅行名称", dateError = "请选择开始和结束日期"), {}) },
        verify = {
            listOf(
                "create-name" to "请输入旅行名称",
                "create-date-control" to "请选择开始和结束日期",
            ).forEach { (tag, message) ->
                onNodeWithText(message).assertIsDisplayed().assert(hasAnyAncestor(hasTestTag("$tag-container")))
            }
        },
    )

    private fun emptyTrips(id: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.TRIP_LIST),
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST)),
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
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
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
        DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY)),
        content = { RouteLegContent(RouteLegUi("leg", "a", "b", TransportMode.WALK, status, null, null, message)) },
        verify = { onNodeWithText(message).assertIsDisplayed() },
    )

    private fun searchState(id: String, phase: PlaceSearchPhase, message: String) = ComposeScenario(
        ScenarioFixture(id, ScenarioScreen.SEARCH),
        DeclaredScenarioPath(listOf(ScenarioScreen.WORKSPACE, ScenarioScreen.SEARCH)),
        content = { PlaceSearchContent(PlaceSearchUiState(search = PlaceSearchState("故宫", phase = phase)), {}) },
        verify = { onNodeWithText(message).assertIsDisplayed() },
    )

    private fun mapConsentExplanation(id: String): V1ScenarioExecutable {
        var allowed = false
        var declined = false
        var dialogVisible by mutableStateOf(true)
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PERMISSION),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PERMISSION)),
            { allowed = false; declined = false; dialogVisible = true },
            {
                val store = remember {
                    com.yangchengwei.easytrip.amap.AmapConsentStore(
                        persistence = object : com.yangchengwei.easytrip.amap.AmapConsentPersistence {
                            override fun readDecision(): Boolean? = null
                            override fun writeDecision(accepted: Boolean) = Unit
                        },
                        reporter = object : com.yangchengwei.easytrip.amap.AmapPrivacyReporter {
                            override suspend fun reportShown() = Unit
                            override suspend fun reportDecision(accepted: Boolean) {
                                if (accepted) allowed = true else declined = true
                            }
                        },
                        registry = com.yangchengwei.easytrip.amap.ConsentRegistry(),
                    )
                }
                if (dialogVisible) {
                    com.yangchengwei.easytrip.AmapConsentDialog(
                        store = store,
                        policyRead = true,
                        onPolicyReadChange = {},
                        onClose = { dialogVisible = false },
                        onDecisionSuccess = { dialogVisible = false },
                        context = LocalContext.current,
                    )
                }
            },
            { onNodeWithText("允许使用地图").performClick() },
            {
                onNodeWithText("允许 Easy Trip 使用地图").assertDoesNotExist()
                check(allowed)
                check(!declined)
                check(!dialogVisible)
            },
            factoryIdentity = "batch6-map-consent-explanation",
        )
    }

    private fun locationExplanation(id: String): V1ScenarioExecutable {
        var continued = false
        var dismissed = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PERMISSION),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PERMISSION)),
            { continued = false; dismissed = false },
            { PermissionExplanationContent({ continued = true }, { dismissed = true }) },
            { onNodeWithText("继续").performClick() },
            {
                onNodeWithText("允许 Easy Trip 获取你的位置").assertIsDisplayed()
                onNodeWithText("用于在地图上定位当前位置。只有点击定位按钮时才会使用，拒绝后仍可正常规划行程。").assertIsDisplayed()
                onNodeWithText("继续").assertIsDisplayed()
                onNodeWithText("暂不使用").assertIsDisplayed()
                check(continued)
                check(!dismissed)
            },
            factoryIdentity = "batch6-location-explanation",
        )
    }

    private fun locationSettingsRecovery(id: String): V1ScenarioExecutable {
        var opened = false
        var dismissed = false
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.PERMISSION),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.PERMISSION)),
            { opened = false; dismissed = false },
            { LocationPermissionSettingsContent({ opened = true }, { dismissed = true }) },
            { onNodeWithText("前往设置").performClick() },
            {
                onNodeWithText("定位权限未开启").assertIsDisplayed()
                onNodeWithText("请前往系统设置，为 Easy Trip 开启定位权限。地图和行程仍可正常使用。").assertIsDisplayed()
                onNodeWithText("前往设置").assertIsDisplayed()
                onNodeWithText("取消").assertIsDisplayed()
                check(opened)
                check(!dismissed)
            },
            factoryIdentity = "batch6-location-settings-recovery",
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
            factoryIdentity = "batch6-map-failure",
        )
    }

    private fun editSaveFailure(id: String): V1ScenarioExecutable {
        val actions = mutableListOf<DayItineraryAction>()
        return ComposeScenario(
            ScenarioFixture(id, ScenarioScreen.ITEM_EDITOR),
            DeclaredScenarioPath(listOf(ScenarioScreen.TRIP_LIST, ScenarioScreen.WORKSPACE, ScenarioScreen.ITINERARY, ScenarioScreen.ITEM_EDITOR)),
            actions::clear,
            {
                DayItineraryContent(
                    DayItineraryUiState(editDraft = ItineraryEditDraft("item-1", "09:30", "60", saveError = "保存失败")),
                    onAction = actions::add,
                )
            },
            { onNodeWithTag("itinerary-save-failure-retry").performClick() },
            {
                onNodeWithTag("itinerary-save-failure").assertIsDisplayed()
                onNodeWithText("修改尚未保存").assertIsDisplayed()
                onNodeWithText("到达时间、停留时长和备注仍保留在当前页面。请重新保存，或稍后再试。").assertIsDisplayed()
                onNodeWithText("继续编辑").assertIsDisplayed()
                onNodeWithText("重新保存").assertIsDisplayed()
                check(actions == listOf(DayItineraryAction.SaveEdit))
            },
            factoryIdentity = "batch6-edit-save-failure",
        )
    }

}
