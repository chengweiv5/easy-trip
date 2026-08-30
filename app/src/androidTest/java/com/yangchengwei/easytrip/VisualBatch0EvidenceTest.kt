package com.yangchengwei.easytrip

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.RouteLegContent
import com.yangchengwei.easytrip.itinerary.ui.RouteLegUi
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.ui.PlaceDetailPanel
import com.yangchengwei.easytrip.place.ui.PlaceDetailSource
import com.yangchengwei.easytrip.place.ui.PlacePoolContent
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.place.ui.PlaceSearchContent
import com.yangchengwei.easytrip.place.ui.PlaceSearchPhase
import com.yangchengwei.easytrip.place.ui.PlaceSearchState
import com.yangchengwei.easytrip.place.ui.PlaceSearchUiState
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import com.yangchengwei.easytrip.trip.domain.DateRangeChangeImpact
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.trip.ui.CreateTimeMode
import com.yangchengwei.easytrip.trip.ui.CreateTripContent
import com.yangchengwei.easytrip.trip.ui.CreateTripUiState
import com.yangchengwei.easytrip.trip.ui.DateRangeChangePhase
import com.yangchengwei.easytrip.trip.ui.DateRangeChangeRequest
import com.yangchengwei.easytrip.trip.ui.DateRangeChangeUiState
import com.yangchengwei.easytrip.trip.ui.DayUi
import com.yangchengwei.easytrip.trip.ui.TripListContent
import com.yangchengwei.easytrip.trip.ui.TripListPageState
import com.yangchengwei.easytrip.trip.ui.TripListUiState
import com.yangchengwei.easytrip.trip.ui.TripSettingsContent
import com.yangchengwei.easytrip.trip.ui.TripSettingsUiState
import com.yangchengwei.easytrip.workspace.ItineraryScope
import com.yangchengwei.easytrip.workspace.TripWorkspaceContent
import com.yangchengwei.easytrip.workspace.TripWorkspacePageState
import com.yangchengwei.easytrip.workspace.TripWorkspaceUiState
import com.yangchengwei.easytrip.workspace.WorkspaceMapState
import com.yangchengwei.easytrip.workspace.WorkspaceSection
import com.yangchengwei.easytrip.workspace.WorkspaceSheetLevel
import com.yangchengwei.easytrip.workspace.toReadyState
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VisualBatch0EvidenceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun settings_U06l7P() {
        render("U06l7P", "settings") {
            TripSettingsContent(settingsState(DateRangeChangePhase.Idle), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
        compose.onNodeWithText("整体出行日期").assertIsDisplayed()
    }

    @Test fun dateConfirmation_IKTv5() {
        render("IKTv5", "date-confirmation") {
            TripSettingsContent(settingsState(DateRangeChangePhase.AwaitingConfirmation(request(), impact())), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
        compose.onNodeWithText("确认修改日期范围？").assertIsDisplayed()
    }

    @Test fun search_ofdn5() {
        render("ofdn5", "search") {
            PlaceSearchContent(
                PlaceSearchUiState(
                    search = PlaceSearchState("西湖", searchCandidates(), phase = PlaceSearchPhase.Results),
                ),
                {},
            )
        }
        compose.onNodeWithText("西湖天地").assertIsDisplayed()
    }

    @Test fun placeDetail_p4G1tS() {
        render("p4G1tS", "place-detail") {
            PlaceDetailPanel(searchCandidates()[1], savedPlace(), null, PlaceDetailSource.Search, false, null, {})
        }
        compose.onNodeWithText("西湖天地").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_A9EKX() {
        render(
            "A9EKX",
            "workspace-place-pool",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.PLACE_POOL),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(),
                onItineraryAction = {},
                mapContent = { DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithText("添加到行程").assertIsDisplayed()
        compose.onNodeWithTag("workspace-place-list").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_LFmzR() {
        render(
            "LFmzR",
            "workspace-itinerary",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            val items = itineraryItems()
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(
                    days = workspaceDays(),
                    selectedDayId = "day-1",
                    items = items,
                    previewOrder = items.map { it.id },
                    legs = itineraryLegs(),
                ),
                onItineraryAction = {},
                mapContent = { DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithText("西湖天地").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-rail").assertIsDisplayed()
    }

    @Test fun productionComposeWorkspaceHostWithDeterministicFakeMapSurface_eHTX3() {
        render(
            "eHTX3",
            "workspace-day-itinerary",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            val items = itineraryItems()
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(
                    days = workspaceDays(),
                    selectedDayId = "day-1",
                    items = items,
                    previewOrder = items.map { it.id },
                    legs = itineraryLegs(),
                ),
                onItineraryAction = {},
                mapContent = { DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
        compose.onNodeWithText("西湖天地").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-scope-rail").assertIsDisplayed()
    }

    @Test fun workspaceSheetCollapsed_kCc5z() {
        render(
            "kCc5z",
            "workspace-sheet-collapsed",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY, WorkspaceSheetLevel.COLLAPSED),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = workspaceDays(), selectedDayId = "day-1", items = itineraryItems()),
                onItineraryAction = {},
                mapContent = { DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        compose.onNodeWithTag("workspace-collapsed-summary").assertIsDisplayed()
        compose.onAllNodesWithTag("day-itinerary-timeline").assertCountEquals(0)
    }

    @Test fun workspaceSheetHalf_sWTB3() {
        render(
            "sWTB3",
            "workspace-sheet-half",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY, WorkspaceSheetLevel.HALF),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = workspaceDays(), selectedDayId = "day-1", items = itineraryItems(), previewOrder = itineraryItems().map { it.id }, legs = itineraryLegs()),
                onItineraryAction = {},
                mapContent = { DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        assertWorkspaceHost()
    }

    @Test fun workspaceSheetExpanded_f2ieZ6() {
        render(
            "f2ieZ6",
            "workspace-sheet-expanded",
            evidenceHost = "production Compose workspace host",
            mapSurface = "deterministic fake map surface; not a real map host",
        ) {
            TripWorkspaceContent(
                pageState = workspaceState(WorkspaceSection.ITINERARY, WorkspaceSheetLevel.EXPANDED),
                mapState = WorkspaceMapState.Ready,
                onAction = {},
                placeState = placePoolState(),
                onPlaceAction = {},
                itineraryState = DayItineraryUiState(days = workspaceDays(), selectedDayId = "day-1", items = itineraryItems(), previewOrder = itineraryItems().map { it.id }, legs = itineraryLegs()),
                onItineraryAction = {},
                mapContent = { DeterministicFakeMapSurface() },
                modifier = Modifier.fillMaxSize().testTag("workspace-root"),
            )
        }
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("workspace-sheet").assertIsDisplayed()
        compose.onAllNodesWithTag("workspace-search-launcher").assertCountEquals(0)
    }

    @Test fun routeLegStates_batch5() {
        val longError = "路线规划失败：这是一段用于截图验证的较长错误说明，保持两行展示并提供重试操作"
        render("batch5", "route-leg-states") {
            Column {
                RouteLegContent(RouteLegUi("pending", "a", "b", TransportMode.WALK, RouteStatus.PENDING, null, null, null))
                RouteLegContent(RouteLegUi("calculating", "b", "c", TransportMode.WALK, RouteStatus.CALCULATING, null, null, null))
                RouteLegContent(RouteLegUi("waiting", "c", "d", TransportMode.WALK, RouteStatus.WAITING_NETWORK, null, null, null))
                RouteLegContent(RouteLegUi("failed", "d", "e", TransportMode.WALK, RouteStatus.FAILED, null, null, longError), onRetry = {})
            }
        }
        compose.onNodeWithText("等待计算路线").assertIsDisplayed()
        compose.onNodeWithText("正在计算路线").assertIsDisplayed()
        compose.onNodeWithText("联网后计算路线").assertIsDisplayed()
        compose.onNodeWithText(longError).assertIsDisplayed()
    }

    @Test fun emptyTrips_zIbEu() {
        render("zIbEu", "empty-trips") {
            TripListContent(TripListUiState(page = TripListPageState.Empty), {})
        }
        compose.onNodeWithTag("empty-illustration-trips").assertIsDisplayed()
    }

    @Test fun emptyPlacePool_lsr1I() {
        render("lsr1I", "empty-place-pool") {
            PlacePoolContent(state = PlacePoolUiState(), showSearch = false, onAction = {})
        }
        compose.onNodeWithTag("empty-illustration-places").assertIsDisplayed()
    }

    @Test fun emptyDay_Bcf6A() {
        render("Bcf6A", "empty-day") {
            DayItineraryContent(
                DayItineraryUiState(days = listOf(TripDay("day-1", 0)), selectedDayId = "day-1"),
                onAction = {},
            )
        }
        compose.onNodeWithTag("empty-illustration-itinerary").assertIsDisplayed()
    }

    @Test fun emptySearch_batch6() {
        render("batch6-search", "empty-search") {
            PlaceSearchContent(
                PlaceSearchUiState(search = PlaceSearchState("不存在", phase = PlaceSearchPhase.Empty)),
                {},
            )
        }
        compose.onNodeWithTag("empty-illustration-search").assertIsDisplayed()
    }

    @Test fun emptyFailure_batch6() {
        render("batch6-failure", "empty-failure") {
            PlaceSearchContent(
                PlaceSearchUiState(search = PlaceSearchState("西湖", phase = PlaceSearchPhase.NetworkFailure("网络不可用"))),
                {},
            )
        }
        compose.onNodeWithTag("empty-illustration-failure").assertIsDisplayed()
    }

    @Test fun create_dzhkC() {
        render("dzhkC", "create") {
            CreateTripContent(
                CreateTripUiState("杭州·春日慢游", "3", CreateTimeMode.DATED, LocalDate.of(2025, 4, 12), TravelMode.FLEXIBLE),
                {},
            )
        }
        compose.onNodeWithTag("create-submit").assertIsDisplayed()
    }

    private fun render(
        frameId: String,
        name: String,
        evidenceHost: String = "isolated Compose fixture",
        mapSurface: String = "not applicable",
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.setContent { EasyTripTheme { content() } }
        compose.waitForIdle()
        when (name) {
            "settings" -> compose.onNodeWithText("旅行设置").assertIsDisplayed()
            "date-confirmation" -> compose.onNodeWithText("确认修改日期范围？").assertIsDisplayed()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(300)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = context.resources.configuration
        val actualWidthDp = config.screenWidthDp
        check(actualWidthDp in 387..390) {
            "Visual evidence expects a target phone width of 387..390dp, but was ${actualWidthDp}dp"
        }
        val directory = File(context.filesDir, "evidence/batch-0").apply { mkdirs() }
        val png = File(directory, "$name-$frameId.png")
        val shot = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            FileOutputStream(png).use { check(shot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { shot.recycle() }
        File(directory, "$name-$frameId.manifest.json").writeText(
            JSONObject()
                .put("frame_id", frameId)
                .put("test_class", javaClass.name)
                .put("test_method", name)
                .put("evidence_host", evidenceHost)
                .put("map_surface", mapSurface)
                .put("baseline_width_dp", 390)
                .put("target_phone_width_dp_range", "387..390")
                .put("actual_width_dp", actualWidthDp)
                .put("git_sha", BuildConfig.GIT_SHA)
                .put("source_state", BuildConfig.SOURCE_STATE)
                .put("device_serial", "emulator-5554")
                .put("device_fingerprint", Build.FINGERPRINT)
                .put("device_model", Build.MODEL)
                .put("device_sdk", Build.VERSION.SDK_INT)
                .put("bitmap_width_px", shot.width)
                .put("bitmap_height_px", shot.height)
                .put("window_width_dp", config.screenWidthDp)
                .put("window_height_dp", config.screenHeightDp)
                .put("density_dpi", config.densityDpi)
                .put("font_scale", config.fontScale.toDouble())
                .put("png_sha256", sha256(png))
                .toString(2),
        )
    }

    @androidx.compose.runtime.Composable
    private fun DeterministicFakeMapSurface() {
        Box(Modifier.fillMaxSize().background(Color(0xFFDCE8DF)).testTag("deterministic-fake-map-surface"))
    }

    private fun assertWorkspaceHost() {
        compose.onNodeWithTag("workspace-root").assertIsDisplayed()
        compose.onNodeWithTag("workspace-map").assertIsDisplayed()
        compose.onNodeWithTag("deterministic-fake-map-surface").assertIsDisplayed()
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("workspace-search-launcher").assertIsDisplayed()
        compose.onNodeWithTag("workspace-locate").assertIsDisplayed()
        compose.onNodeWithTag("layer-menu").assertIsDisplayed()
        compose.onNodeWithTag("map-legend").assertIsDisplayed()
        compose.onNodeWithTag("workspace-tabs").assertIsDisplayed()
        compose.onNodeWithTag("workspace-sheet").assertIsDisplayed()
        compose.onNodeWithTag("workspace-sheet-handle").assertIsDisplayed()
    }

    private fun workspaceState(
        section: WorkspaceSection,
        sheetLevel: WorkspaceSheetLevel = WorkspaceSheetLevel.HALF,
    ) = TripWorkspacePageState.Ready(
        TripWorkspaceUiState(
            tripName = "杭州·春日慢游",
            dateLabel = "2025年4月12日 - 4月14日",
            days = workspaceDays(),
            section = section,
            itineraryScope = ItineraryScope.Day("day-1"),
            sheetLevel = sheetLevel,
        ).toReadyState(),
    )

    private fun placePoolState() = PlacePoolUiState(
        rows = searchCandidates().mapIndexed { index, candidate ->
            SavedPlaceRowUi(savedPlace(candidate, "saved-$index"), 0, scheduled = index == 0)
        },
        tags = listOf(PlaceTag("tag-1", "景点"), PlaceTag("tag-2", "散步")),
    )

    private fun workspaceDays() = listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2))

    private fun itineraryItems() = listOf(
        ItineraryItemUi("item-1", "西湖天地", "上城区南山路", java.time.LocalTime.of(9, 30), 90),
        ItineraryItemUi("item-2", "知味观·湖滨店", "仁和路83号", java.time.LocalTime.of(12, 0), 60),
        ItineraryItemUi("item-3", "龙井村", "西湖区龙井路", java.time.LocalTime.of(14, 30), 120),
    )

    private fun itineraryLegs() = listOf(
        RouteLegUi("leg-1", "item-1", "item-2", TransportMode.WALK, RouteStatus.SUCCESS, 1200, 1080, null),
        RouteLegUi("leg-2", "item-2", "item-3", TransportMode.DRIVE, RouteStatus.SUCCESS, 11000, 1680, null),
    )

    private fun request() = DateRangeChangeRequest(1, "trip", LocalDate.of(2025, 4, 12), listOf("day-1", "day-2", "day-3"), LocalDate.of(2025, 4, 12))
    private fun impact() = DateRangeChangeImpact(request(), listOf("day-1"), listOf("day-2", "day-3"), 3, 2, 8)
    private fun settingsState(phase: DateRangeChangePhase) = TripSettingsUiState(
        tripId = "trip", name = "杭州·春日慢游", travelMode = TravelMode.FLEXIBLE,
        days = listOf(DayUi("day-1", "2025-04-12"), DayUi("day-2", "2025-04-13"), DayUi("day-3", "2025-04-14")),
        dateRange = DateRangeChangeUiState(LocalDate.of(2025, 4, 12), LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 12), true, phase),
    )
    private fun searchCandidates() = listOf(
        PlaceCandidate("poi-1", "西湖风景名胜区", "西湖区龙井路1号", GeoPoint(30.23, 120.10), "0571"),
        PlaceCandidate("poi-2", "西湖天地", "上城区南山路147号", GeoPoint(30.22, 120.15), "0571"),
        PlaceCandidate("poi-3", "知味观·湖滨店", "仁和路83号", GeoPoint(30.25, 120.16), "0571"),
        PlaceCandidate("poi-4", "龙井村", "西湖区龙井路", GeoPoint(30.21, 120.11), "0571"),
    )
    private fun savedPlace(candidate: PlaceCandidate = searchCandidates()[1], id: String = "saved") = SavedPlace(id, "trip", candidate.poiId, candidate.name, candidate.address, candidate.point!!, "傍晚沿湖散步，预留看日落的时间", listOf(PlaceTag("tag-1", "景点")))
    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
