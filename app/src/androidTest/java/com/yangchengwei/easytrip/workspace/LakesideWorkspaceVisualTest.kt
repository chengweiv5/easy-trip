package com.yangchengwei.easytrip.workspace

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.*
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import com.yangchengwei.easytrip.trip.domain.TripDay
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean

class LakesideWorkspaceVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realMapKeepsLakesideChromeAcrossPoolAndItinerary() {
        com.amap.api.maps.MapsInitializer.updatePrivacyShow(compose.activity, true, true)
        com.amap.api.maps.MapsInitializer.updatePrivacyAgree(compose.activity, true)
        val gate = TestConsentGate().also { it.show() }
        val token = requireNotNull(gate.decide(true))
        val loaded = AtomicBoolean(false)
        val section = mutableStateOf(WorkspaceSection.PLACE_POOL)
        val points = listOf(GeoPoint(30.239, 120.152), GeoPoint(30.247, 120.162), GeoPoint(30.221, 120.113))
        val names = listOf("西湖天地", "知味观", "龙井村")
        val rows = points.indices.map { i -> SavedPlaceRowUi(
            SavedPlace("place-$i", "visual", "poi-$i", names[i], "杭州市 · 西湖区", points[i], "", emptyList(), "杭州市", "330100", "0571", 1), 1, true,
        ) }
        val days = listOf(TripDay("day-1", 0), TripDay("day-2", 1), TripDay("day-3", 2))
        val itinerary = DayItineraryUiState(days = days, selectedDayId = "day-1",
            items = names.mapIndexed { i, name -> ItineraryItemUi("item-$i", name, "杭州市", LocalTime.of(9 + i * 2, 30), 60) },
            legs = listOf(RouteLegUi("walk", "item-0", "item-1", TransportMode.WALK, RouteStatus.SUCCESS, 950, 900, null),
                RouteLegUi("taxi", "item-1", "item-2", TransportMode.TAXI, RouteStatus.SUCCESS, 11000, 1500, null)))
        compose.setContent { EasyTripTheme {
            val isItinerary = section.value == WorkspaceSection.ITINERARY
            val markers = points.indices.map { i -> MapMarkerUi("place-$i", points[i], names[i], emptyList(),
                if (isItinerary) MapMarkerKind.SAVED_ITINERARY else MapMarkerKind.SAVED_PLACE_POOL,
                badgeText = (i + 1).toString(), savedPlaceId = "place-$i", scheduled = true,
                badgeSegments = if (isItinerary) listOf(MapMarkerBadgeSegment((i + 1).toString(), routeColorForDay(0))) else emptyList()) }
            val model = MapUiModel(markers = markers)
            TripWorkspaceContent(
                pageState = TripWorkspacePageState.Ready(TripWorkspaceUiState(
                    tripName = "杭州 · 春日慢游", startDate = LocalDate.of(2026, 4, 12), dateLabel = "4月12日 — 4月14日",
                    days = days, section = section.value, itineraryScope = ItineraryScope.Day("day-1"),
                    sheetLevel = WorkspaceSheetLevel.HALF, map = model,
                ).toReadyState()),
                mapState = WorkspaceMapState.Ready, onAction = {},
                placeState = PlacePoolUiState(rows = rows, savedPoiIds = rows.map { it.place.amapPoiId }.toSet()),
                onPlaceAction = {}, itineraryState = itinerary, onItineraryAction = {},
                mapContent = { layout -> AmapComposeMap(
                    model = model.copy(viewportRequest = MapViewportRequest(if (isItinerary) 2 else 1, ViewportReason.INITIAL,
                        points, safeInsets = layout.fitInsets)),
                    onMarkerClick = {}, consent = token,
                    hostFactory = { context ->
                        val actual = RealAmapMapHost(context)
                        object : AmapMapHost by actual {
                            override fun setOnReadyListener(listener: (() -> Unit)?) {
                                actual.setOnReadyListener(listener?.let { callback -> { loaded.set(true); callback() } })
                            }
                        }
                    },
                    visibleInsets = layout.visibleInsets, modifier = Modifier.fillMaxSize(),
                ) },
                modifier = Modifier.fillMaxSize().testTag("v11-workspace"),
            )
        } }
        compose.waitUntil(30_000) { loaded.get() }
        compose.onNodeWithTag("workspace-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("saved-place-place-0", true).assertIsDisplayed()
        android.os.SystemClock.sleep(2_000)
        save("v11-workspace-pool.png")
        compose.runOnIdle { section.value = WorkspaceSection.ITINERARY }
        compose.onNodeWithTag("itinerary-order-item-0", true).assertTextEquals("1")
        compose.onNodeWithTag("itinerary-order-item-1", true).assertTextEquals("2")
        android.os.SystemClock.sleep(1_000)
        save("v11-workspace-itinerary.png")
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        java.io.File(compose.activity.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
