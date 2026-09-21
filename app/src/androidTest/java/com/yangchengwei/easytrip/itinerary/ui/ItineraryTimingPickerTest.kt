package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ItineraryTimingPickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun singleAndWholeTripRowsPlaceOrderAboveTime() {
        val item = ItineraryItemUi("day", "西湖天地", "湖滨路", LocalTime.of(9, 30), 90)
        compose.setContent { EasyTripTheme { Column {
            ItineraryItemRow(item, 0, 1, {}, {}, {})
            ItineraryPlaceRow(item.copy(id = "whole"), displayOrder = 7)
        } } }
        listOf("day", "whole").forEach { id ->
            val badge = compose.onNodeWithTag("itinerary-order-$id", true).fetchSemanticsNode().boundsInRoot
            val time = compose.onNodeWithTag("itinerary-arrival-$id", true).fetchSemanticsNode().boundsInRoot
            assertTrue("$badge must be above $time", badge.bottom <= time.top)
        }
        compose.onAllNodesWithText("停留 1.5 小时", true).assertCountEquals(2)
    }

    @Test fun slidingArrivalAndStayWheelsChangesDraftWithoutKeyboard() {
        val draft = mutableStateOf(ItineraryEditDraft("item", "09:30", "120"))
        compose.setContent { EasyTripTheme {
            EditItineraryItemContent(draft.value, { draft.value = draft.value.copy(arrivalTimeText = it) },
                { draft.value = draft.value.copy(stayMinutesText = it) }, onSave = {}, onCancel = {})
        } }
        compose.onNodeWithTag("arrival-hour-picker").performTouchInput { swipeUp(durationMillis = 500) }
        compose.waitUntil(5_000) { draft.value.arrivalTime?.hour != 9 }
        val oldMinute = draft.value.arrivalTime?.minute
        compose.onNodeWithTag("arrival-minute-picker").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(center.x, height * 0.35f),
                end = androidx.compose.ui.geometry.Offset(center.x, height * 0.85f), durationMillis = 500)
        }
        compose.waitUntil(5_000) { draft.value.arrivalTime?.minute != oldMinute }
        assertEquals(0, draft.value.arrivalTime!!.minute % 30)
        compose.onNodeWithTag("stay-hours-picker").performTouchInput { swipeUp(durationMillis = 500) }
        compose.waitUntil(5_000) { draft.value.stayMinutes != 120 }
        assertEquals(0, draft.value.stayMinutes!! % 60)
        compose.onNodeWithTag("arrival-hour-picker").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
        compose.onNodeWithTag("stay-hours-picker").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
        compose.runOnIdle {
            assertFalse(ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true)
        }
    }

    @Test fun historicalMinutesSurviveArrivalEditsAndRemainSelectable() {
        val draft = mutableStateOf(ItineraryEditDraft("item", "09:30", "75"))
        var saved: ItineraryEditDraft? = null
        compose.setContent { EasyTripTheme {
            EditItineraryItemContent(draft.value, { draft.value = draft.value.copy(arrivalTimeText = it) },
                { draft.value = draft.value.copy(stayMinutesText = it) }, onSave = { saved = draft.value }, onCancel = {})
        } }
        compose.assertStayHours("1.25")
        compose.selectArrivalTime(14, 30)
        compose.onNodeWithText("保存").performClick()
        assertEquals(75, saved?.stayMinutes)
        assertEquals(LocalTime.of(14, 30), saved?.arrivalTime)
        compose.selectStayHours(2)
        assertEquals(120, draft.value.stayMinutes)
        // The historical 1.25 h entry remains between 1 h and 2 h.
        val decrease = compose.onNodeWithTag("stay-hours-picker").fetchSemanticsNode().config[SemanticsActions.CustomActions].last()
        compose.runOnIdle { decrease.action() }
        compose.assertStayHours("1.25")
        assertEquals(75, draft.value.stayMinutes)
    }

    @Test fun unsetAndMidnightAndSavingAreDistinct() {
        val draft = mutableStateOf(ItineraryEditDraft("item", "", ""))
        compose.setContent { EasyTripTheme {
            EditItineraryItemContent(draft.value, { draft.value = draft.value.copy(arrivalTimeText = it) },
                { draft.value = draft.value.copy(stayMinutesText = it) }, onSave = {}, onCancel = {})
        } }
        compose.onNodeWithTag("arrival-minute-picker").assertIsNotEnabled()
        compose.selectArrivalTime(0, 0)
        assertEquals(LocalTime.MIDNIGHT, draft.value.arrivalTime)
        compose.selectStayHours(0)
        assertEquals(0, draft.value.stayMinutes)
        compose.onNodeWithTag("arrival-hour-picker").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.onNodeWithTag("stay-hours-picker").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        assertNull(draft.value.arrivalTime)
        assertNull(draft.value.stayMinutes)
        compose.runOnIdle { draft.value = draft.value.copy(isSaving = true) }
        listOf("arrival-hour-picker", "arrival-minute-picker", "stay-hours-picker").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsNotEnabled()
            compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { assertFalse(it(2f)) }
        }
        assertNull(draft.value.arrivalTime)
        assertNull(draft.value.stayMinutes)
    }

    @Test fun largeFontSmallSheetKeepsSaveAndCancelReachable() {
        var cancelled = false
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) { EasyTripTheme {
            EditItineraryItemContent(ItineraryEditDraft("item", "09:30", "120", placeName = "西湖天地"), {}, {},
                onSave = {}, onCancel = { cancelled = true }, modifier = Modifier.width(280.dp).height(320.dp))
        } } }
        compose.onNodeWithText("保存").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed().performClick()
        assertTrue(cancelled)
    }

    @Test fun timingEditorVisualEvidence() {
        compose.setContent { EasyTripTheme { Column(Modifier.width(360.dp)) {
            ItineraryPlaceRow(ItineraryItemUi("visual", "西湖天地", "湖滨路", LocalTime.of(9, 30), 120), displayOrder = 1)
            EditItineraryItemContent(ItineraryEditDraft("item", "09:30", "120", placeName = "西湖天地"), {}, {},
                onScheduleAgain = {}, onSave = {}, onCancel = {})
        } } }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(compose.activity.getExternalFilesDir(null), "itinerary-time-wheel.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
