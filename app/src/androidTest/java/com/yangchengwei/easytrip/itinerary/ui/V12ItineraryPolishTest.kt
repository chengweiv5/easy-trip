package com.yangchengwei.easytrip.itinerary.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
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
import androidx.compose.ui.unit.Density
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
    private val longNote = "提前一天预约门票，记得携带身份证，从东门进入。傍晚到湖边看日落，结束后回酒店休息。"

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
        assertTrue("Collapsed note should occupy one 17sp line", collapsed <= 20.dp)
        assertTimingToggle("i")
        val collapsedBounds = compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val controls = compose.onNodeWithTag("more-i", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val collapsedLayout = noteLayout("itinerary-i")
        assertEquals(1, collapsedLayout.lineCount)
        assertTrue(collapsedLayout.isLineEllipsized(0))
        screenshot("itinerary-note-collapsed")
        compose.onNodeWithTag("note-toggle-itinerary-i").assertIsDisplayed().performClick()
        val expanded = compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue(expanded > collapsed)
        assertTimingToggle("i")
        val expandedBounds = compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(collapsedBounds.left, expandedBounds.left)
        assertEquals(collapsedBounds.right, expandedBounds.right)
        assertEquals(controls, compose.onNodeWithTag("more-i", useUnmergedTree = true).getUnclippedBoundsInRoot())
        compose.onNodeWithText("收起").assertIsDisplayed()
        screenshot("itinerary-note-expanded")
        compose.onNodeWithTag("note-toggle-itinerary-i").performClick()
        assertEquals(collapsed, compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).getUnclippedBoundsInRoot().let { it.bottom - it.top })
        compose.onNodeWithTag("note-toggle-itinerary-i").performClick()
        compose.runOnIdle { note.value = "晚上到站，直接去酒店" }
        compose.onNodeWithText("晚上到站，直接去酒店", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("note-toggle-itinerary-i").assertDoesNotExist()
        compose.runOnIdle { note.value = "  " }
        compose.onNodeWithTag("note-text-itinerary-i", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("itinerary-place-timing-i", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun wholeTripNoteKeepsSameWidthWhenExpandedAndIdentityChanges() {
        val itemId = mutableStateOf("whole")
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceRow(
                    ItineraryItemUi(itemId.value, "西湖", "杭州", LocalTime.of(9, 30), 120, note = longNote),
                    displayOrder = 1, modifier = Modifier.width(280.dp),
                )
            }
        }
        assertTimingToggle("whole")
        val width = noteLayout("itinerary-whole").size.width
        compose.onNodeWithTag("note-toggle-itinerary-whole").performClick()
        assertTimingToggle("whole")
        assertEquals(width, noteLayout("itinerary-whole").size.width)
        assertTrue(noteLayout("itinerary-whole").lineCount > 1)
        screenshot("whole-trip-note-expanded")
        compose.runOnIdle { itemId.value = "next" }
        compose.onNodeWithTag("note-toggle-itinerary-next").assertIsDisplayed()
        compose.onNodeWithText("展开").assertIsDisplayed()
        assertEquals(1, noteLayout("itinerary-next").lineCount)
    }

    @Test fun itineraryPreviewUsesWholeLineAndExpansionPreservesParagraphs() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceRow(
                    ItineraryItemUi("paragraph", "郑州东站", "郑州", null, null,
                        note = "晚上到达，直接去酒店\n休息\n第三行"),
                    displayOrder = 1, modifier = Modifier.width(280.dp),
                )
            }
        }
        compose.onNodeWithText("晚上到达，直接去酒店 休息 第三行", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(1, noteLayout("itinerary-paragraph").lineCount)
        assertTimingToggle("paragraph")
        compose.onNodeWithTag("note-toggle-itinerary-paragraph").performClick()
        compose.onNodeWithText("晚上到达，直接去酒店\n休息\n第三行", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(3, noteLayout("itinerary-paragraph").lineCount)
        assertTimingToggle("paragraph")
    }

    @Test fun itineraryTimingToggleRemainsReachableWithLargeTextAndChangingWidth() {
        val width = mutableStateOf(250.dp)
        val note = mutableStateOf(longNote)
        val scale = mutableStateOf(1.5f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale.value)) {
                EasyTripTheme {
                    ItineraryItemRow(
                        item = ItineraryItemUi("narrow", "西湖", "杭州", null, null, note = note.value),
                        index = 0, count = 1, onPreview = {}, onCommit = {}, onMenuAction = {},
                        modifier = Modifier.width(width.value),
                    )
                }
            }
        }
        assertTimingToggle("narrow")
        val before = compose.onNodeWithTag("note-text-itinerary-narrow", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val controls = compose.onNodeWithTag("drag-handle-narrow", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(before.right < controls.left)
        compose.onNodeWithTag("note-toggle-itinerary-narrow").performClick()
        assertTimingToggle("narrow")
        val after = compose.onNodeWithTag("note-text-itinerary-narrow", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(before.right - before.left, after.right - after.left)
        assertEquals(controls, compose.onNodeWithTag("drag-handle-narrow", useUnmergedTree = true).getUnclippedBoundsInRoot())
        compose.onNodeWithTag("more-narrow").performClick()
        compose.onNodeWithTag("menu-timing-narrow", useUnmergedTree = true).assertIsDisplayed().performClick()
        compose.runOnIdle { width.value = 390.dp; scale.value = 1f; note.value = "晚上到达，直接去酒店休息" }
        compose.onNodeWithTag("note-toggle-itinerary-narrow").assertDoesNotExist()
        compose.runOnIdle { width.value = 230.dp }
        compose.onNodeWithTag("note-toggle-itinerary-narrow").assertIsDisplayed()
        assertTimingToggle("narrow")
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
        assertInlineToggle("place-p")
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

    @Test fun noteRechecksOverflowWhenWidthChangesAndSupportsLargeText() {
        val width = mutableStateOf(140.dp)
        val fontScale = mutableStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                EasyTripTheme {
                    ExpandableNote("晚上到达，直接去酒店休息", "resize", Modifier.width(width.value))
                }
            }
        }
        compose.onNodeWithTag("note-toggle-resize").assertIsDisplayed()
        assertInlineToggle("resize")
        compose.runOnIdle { width.value = 340.dp }
        compose.onNodeWithTag("note-toggle-resize").assertDoesNotExist()
        compose.runOnIdle { width.value = 200.dp; fontScale.value = 1.5f }
        compose.onNodeWithTag("note-toggle-resize").assertIsDisplayed()
        assertInlineToggle("resize")
        compose.onNodeWithTag("note-toggle-resize").performClick()
        assertInlineToggle("resize")
        compose.onNodeWithText("收起").assertIsDisplayed()
    }

    private fun assertInlineToggle(identity: String) {
        val text = compose.onNodeWithTag("note-text-$identity", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val toggle = compose.onNodeWithTag("note-toggle-$identity", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Toggle should align with the first line", kotlin.math.abs((toggle.top - text.top).value) <= 2f)
        assertTrue("Toggle must be beside the text without overlapping", toggle.left >= text.right)
    }

    private fun assertTimingToggle(itemId: String) {
        val identity = "itinerary-$itemId"
        val text = compose.onNodeWithTag("note-text-$identity", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val toggle = compose.onNodeWithTag("note-toggle-$identity", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val timing = compose.onNodeWithTag("itinerary-place-timing-$itemId", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Toggle should be beside the stay duration", toggle.left >= timing.right)
        assertTrue("Toggle should align with the stay duration", kotlin.math.abs((toggle.top - timing.top).value) <= 2f)
        assertTrue("Note should be below the timing row", text.top >= toggle.bottom)
        assertTrue("Note should stop at the toggle's right edge", kotlin.math.abs((text.right - toggle.right).value) <= .5f)
    }

    private fun noteLayout(identity: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithTag("note-text-$identity", useUnmergedTree = true).fetchSemanticsNode()
        checkNotNull(node.config[SemanticsActions.GetTextLayoutResult].action).invoke(results)
        return results.single()
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
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "v1.2.0-evidence").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
