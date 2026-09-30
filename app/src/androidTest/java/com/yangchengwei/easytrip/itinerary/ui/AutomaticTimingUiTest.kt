package com.yangchengwei.easytrip.itinerary.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.calendar.CalendarDay
import com.yangchengwei.easytrip.itinerary.calendar.CalendarDetail
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalTime

class AutomaticTimingUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun itineraryMenuSaysEditAndStillOpensTheExistingEditor() {
        var expanded by mutableStateOf(false)
        var action: ItineraryItemMenuAction? = null
        compose.setContent { EasyTripTheme { Surface {
            Box(Modifier.width(390.dp).height(360.dp).padding(24.dp)) {
                ItineraryItemMenu("stop", "西湖", expanded, { expanded = it }, { action = it })
            }
        } } }
        compose.onNodeWithTag("more-stop").performClick()
        compose.onNodeWithTag("menu-timing-stop").assertTextEquals("编辑")
        compose.onNodeWithText("编辑时间与停留时长").assertDoesNotExist()
        capture("menu", "menu-timing-stop")
        compose.onNodeWithTag("menu-timing-stop").performClick()
        compose.runOnIdle { assertEquals(ItineraryItemMenuAction.EditTiming, action) }
    }

    @Test fun compactTimelineShowsDefaultTimesAndAnOverflowWarning() {
        compose.setContent { EasyTripTheme { Surface {
            Column(Modifier.width(390.dp).padding(16.dp)) {
                ItineraryPlaceContent(ItineraryItemUi("first", "西湖", "杭州", LocalTime.of(8, 0), 60), 1, compactTimeline = true)
                ItineraryPlaceContent(ItineraryItemUi("late", "夜游钱塘江", "杭州", null, 60,
                    timingWarning = "预计时间超出当天，请调整"), 2, compactTimeline = true)
            }
        } } }
        compose.onNodeWithTag("itinerary-arrival-first").assertTextEquals("08:00")
        compose.onNodeWithTag("itinerary-place-timing-first").assertTextContains("停留 1 小时")
        compose.onNodeWithTag("itinerary-arrival-late").assertTextEquals("待定")
        compose.onNodeWithTag("itinerary-timing-warning-late").assertIsDisplayed()
        capture("timeline")
    }

    @Test fun calendarDetailsSaysEditAndDisplaysOverflowWarning() {
        val item = ItineraryItemUi("late", "夜游钱塘江", "杭州", null, 60,
            timingWarning = "预计时间超出当天，请调整")
        var edited = false
        compose.setContent { EasyTripTheme {
            CalendarDetail(CalendarDay("day", 1, listOf(item), emptyList(), listOf(item), emptyList()),
                item, null, false, emptyList(), emptyList(), {}, { edited = true }, {})
        } }
        compose.onNodeWithText("预计时间超出当天，请调整").assertIsDisplayed()
        compose.onNodeWithTag("calendar-detail-edit").assertTextEquals("编辑")
        compose.onNodeWithText("编辑时间和备注").assertDoesNotExist()
        capture("calendar", "calendar-detail")
        compose.onNodeWithTag("calendar-detail-edit").performClick()
        compose.runOnIdle { assertEquals(true, edited) }
    }

    private fun capture(name: String, tag: String? = null) {
        val node = if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag)
        File(compose.activity.getExternalFilesDir(null), "auto-timing-$name.png").outputStream().use {
            node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
