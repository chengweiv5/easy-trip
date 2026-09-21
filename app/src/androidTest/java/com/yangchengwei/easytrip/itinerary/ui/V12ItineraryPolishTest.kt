package com.yangchengwei.easytrip.itinerary.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.core.ui.component.DeferredLoading
import com.yangchengwei.easytrip.core.ui.component.ExpandableNote
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.ui.SavedPlaceRow
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import com.yangchengwei.easytrip.trip.domain.TripDay
import java.io.File
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V12ItineraryPolishTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val longNote = "提前一天预约门票。\n带身份证，从东门进入。\n傍晚到湖边看日落。\n结束后回酒店休息。"

    @Test fun itineraryNoteExpandsInPlaceAndUpdatedNoteResetsExpansion() {
        val note = mutableStateOf(longNote)
        compose.setContent {
            EasyTripTheme {
                DayItineraryContent(
                    DayItineraryUiState(
                        days = listOf(TripDay("day", 0)), selectedDayId = "day",
                        items = listOf(ItineraryItemUi("i", "西湖", "杭州", LocalTime.of(9, 30), 120, note = note.value)),
                        previewOrder = listOf("i"),
                    ), Modifier.width(390.dp), onAction = {},
                )
            }
        }
        val collapsed = compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).getUnclippedBoundsInRoot().let { it.bottom - it.top }
        screenshot("itinerary-note-collapsed")
        compose.onNodeWithTag("note-toggle-itinerary-i").assertIsDisplayed().performClick()
        val expanded = compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue(expanded > collapsed)
        compose.onNodeWithText("收起").assertIsDisplayed()
        screenshot("itinerary-note-expanded")
        compose.onNodeWithTag("note-toggle-itinerary-i").performClick()
        assertEquals(collapsed, compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).getUnclippedBoundsInRoot().let { it.bottom - it.top })
        compose.runOnIdle { note.value = "晚上到站，直接去酒店" }
        compose.onNodeWithText("晚上到站，直接去酒店", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("note-toggle-itinerary-i").assertDoesNotExist()
    }

    @Test fun placeNoteToggleDoesNotOpenPlaceDetails() {
        var opened = 0
        compose.setContent {
            EasyTripTheme {
                SavedPlaceRow(
                    SavedPlaceRowUi(SavedPlace("p", "trip", "poi", "西湖", "杭州", GeoPoint(30.25, 120.15), note = longNote, tags = emptyList()), 0, false),
                    onQuickAdd = null, onOpenDetail = { opened++ }, onEdit = {}, onDelete = {},
                    modifier = Modifier.width(390.dp),
                )
            }
        }
        compose.onNodeWithTag("note-toggle-place-p", useUnmergedTree = true).performClick()
        compose.onNodeWithText("收起", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, opened) }
        screenshot("place-note-expanded")
        compose.onNodeWithTag("open-place-detail-p").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun blankNoteTakesNoSpace() {
        compose.setContent { EasyTripTheme { Column { ExpandableNote("  \n ", "blank"); Text("下一站") } } }
        compose.onNodeWithTag("note-blank").assertDoesNotExist()
        compose.onNodeWithText("下一站").assertIsDisplayed()
    }

    @Test fun unloadedDayDoesNotShowEmptyStateButLoadedEmptyDayDoes() {
        val loaded = mutableStateOf(false)
        compose.setContent {
            EasyTripTheme {
                DayItineraryContent(DayItineraryUiState(
                    days = listOf(TripDay("day", 0)), selectedDayId = "day", isDayLoaded = loaded.value,
                ), onAction = {})
            }
        }
        compose.onNodeWithText("第 1 天 · 暂无行程").assertDoesNotExist()
        compose.runOnIdle { loaded.value = true }
        compose.onNodeWithText("第 1 天 · 暂无行程").assertIsDisplayed()
        compose.onNodeWithTag("day-itinerary-loading").assertDoesNotExist()
    }

    @Test fun fastLoadingNeverDisplaysIntermediateTextAndSlowLoadingStillHasFeedback() {
        val loading = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent { EasyTripTheme { if (loading.value) DeferredLoading { Text("加载中") } else Text("已就绪") } }
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithText("加载中").assertDoesNotExist()
        compose.runOnIdle { loading.value = false }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithText("已就绪").assertIsDisplayed()
        compose.runOnIdle { loading.value = true }
        compose.mainClock.advanceTimeBy(240)
        compose.onNodeWithText("加载中").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "v1.2.0-evidence").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
