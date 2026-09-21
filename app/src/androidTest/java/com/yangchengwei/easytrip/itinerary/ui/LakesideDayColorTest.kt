package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.ui.theme.*
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.routeColorForDay
import com.yangchengwei.easytrip.workspace.routePalette
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

class LakesideDayColorTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun stop(id: String, name: String = "西湖天地") = ItineraryItemUi(id, name, "南山路", LocalTime.of(9, 30), 60)

    @Test fun selectingFirstEighthAndNinthDayUsesMapRouteColorInList() {
        val days = listOf(TripDay("first", 0), TripDay("eighth", 7), TripDay("ninth", 8))
        val selected = mutableStateOf(days.first().id)
        compose.setContent { EasyTripTheme { Surface(color = EasyTripBackground) {
            DayItineraryContent(
                DayItineraryUiState(days = days, selectedDayId = selected.value,
                    items = listOf(stop("lake"), stop("restaurant", "知味观")),
                    legs = listOf(RouteLegUi("walk", "lake", "restaurant", TransportMode.WALK, RouteStatus.SUCCESS, 950, 900, null))),
                Modifier.width(390.dp).height(600.dp), onAction = {},
            )
        } } }
        for (day in days) {
            compose.runOnIdle { selected.value = day.id }
            assertBadge("lake", Color(routeColorForDay(day.index)))
            assertBadge("restaurant", Color(routeColorForDay(day.index)))
        }
        save("v11-day-itinerary.png")
    }

    @Test fun wholeTripRetainsEachDaysColorAndCyclesAfterEightDays() {
        compose.setContent { EasyTripTheme { Surface(color = EasyTripBackground) {
            WholeTripItineraryContent(
                listOf(WholeTripDayUi("first", 1, listOf(stop("one")), emptyList()),
                    WholeTripDayUi("eighth", 8, listOf(stop("eight", "龙井村")), emptyList()),
                    WholeTripDayUi("ninth", 9, listOf(stop("nine", "灵隐寺")), emptyList())),
                modifier = Modifier.width(390.dp).height(680.dp),
            )
        } } }
        listOf("one" to 0, "eight" to 7, "nine" to 8).forEach { (id, index) ->
            compose.onNodeWithTag("itinerary-order-badge-$id", true).performScrollTo()
            assertBadge(id, Color(routeColorForDay(index)))
        }
        save("v11-whole-trip.png")
    }

    @Test fun textButtonsAndRouteNumbersMeetNormalTextContrast() {
        val pairs = listOf(EasyTripPrimaryDark to EasyTripBackground, EasyTripMuted to EasyTripBackground,
            EasyTripMuted to EasyTripSurfaceSoft, Color.White to EasyTripPrimary, EasyTripPrimary to EasyTripSurfaceSoft,
            EasyTripAccent to EasyTripSurface, EasyTripDanger to EasyTripErrorSurface) + routePalette().map { Color.White to Color(it) }
        pairs.forEach { (foreground, background) ->
            val a = foreground.luminance(); val b = background.luminance()
            val ratio = (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
            assertTrue("foreground=$foreground background=$background contrast=$ratio", ratio >= 4.5f)
        }
    }

    private fun assertBadge(id: String, expected: Color) {
        val pixels = compose.onNodeWithTag("itinerary-order-badge-$id", true).captureToImage().toPixelMap()
        val matches = (0 until pixels.width).sumOf { x -> (0 until pixels.height).count { y -> pixels[x,y] == expected } }
        assertTrue("$id must use map route color $expected", matches > pixels.width * pixels.height / 3)
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(compose.activity.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
