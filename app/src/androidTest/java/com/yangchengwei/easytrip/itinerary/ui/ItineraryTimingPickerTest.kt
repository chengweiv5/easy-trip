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

    @Test fun compactPeriodControlAcceptsTouchesAboveAndBelowItsVisual() {
        val draft = mutableStateOf(ItineraryEditDraft("item", "09:30", "120"))
        compose.setContent { EasyTripTheme {
            ItineraryTimingPickers(draft.value, { draft.value = draft.value.copy(arrivalTimeText = it) }, {})
        } }
        compose.onNodeWithTag("arrival-period-pm").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(center.x, 1f))
        }
        compose.assertArrivalTime(21, 30)
        compose.onNodeWithTag("arrival-period-am").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(center.x, height - 1f))
        }
        compose.assertArrivalTime(9, 30)
    }

    @Test fun periodFilterPreservesMinutesAndSeparatesNoonFromMidnight() {
        val draft = mutableStateOf(ItineraryEditDraft("item", "00:30", "75"))
        compose.setContent { EasyTripTheme {
            ItineraryTimingPickers(draft.value, { draft.value = draft.value.copy(arrivalTimeText = it) }, {})
        } }
        compose.onNodeWithTag("arrival-period-am").assertIsSelected()
        compose.onNodeWithTag("arrival-period-pm").performClick().assertIsSelected()
        compose.assertArrivalTime(12, 30)
        assertEquals(LocalTime.of(12, 30), draft.value.arrivalTime)
        assertEquals(75, draft.value.stayMinutes)
        compose.onNodeWithTag("arrival-hour-picker").performSemanticsAction(SemanticsActions.SetProgress) { it(12f) }
        compose.assertArrivalTime(23, 30)
        compose.onNodeWithTag("arrival-period-am").performClick()
        compose.assertArrivalTime(11, 30)
        compose.onNodeWithTag("arrival-period-am").performClick()
        assertEquals(LocalTime.of(11, 30), draft.value.arrivalTime)
        compose.selectArrivalTime(9, 30)
        compose.onNodeWithTag("arrival-period-pm").performClick()
        compose.assertArrivalTime(21, 30)
    }

    @Test fun unsetTimeCanFilterWithoutWritingAndEditSessionResetsPeriod() {
        val draft = mutableStateOf(ItineraryEditDraft("item", "", "120"))
        var changes = 0
        compose.setContent { EasyTripTheme {
            ItineraryTimingPickers(draft.value, { changes++; draft.value = draft.value.copy(arrivalTimeText = it) }, {})
        } }
        compose.onNodeWithTag("arrival-period-pm").performClick().assertIsSelected()
        assertEquals(0, changes)
        assertNull(draft.value.arrivalTime)
        compose.onNodeWithTag("arrival-minute-picker").assertIsNotEnabled()
        compose.onNodeWithTag("arrival-hour-picker").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.assertArrivalTime(12, 0)
        compose.onNodeWithTag("arrival-hour-picker").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        assertNull(draft.value.arrivalTime)
        compose.onNodeWithTag("arrival-period-pm").assertIsSelected()
        compose.runOnIdle { draft.value = draft.value.copy(generation = 1) }
        compose.onNodeWithTag("arrival-period-am").assertIsSelected()
        compose.runOnIdle { draft.value = draft.value.copy(itemId = "other", arrivalTimeText = "18:30") }
        compose.onNodeWithTag("arrival-period-pm").assertIsSelected()
        compose.assertArrivalTime(18, 30)
        compose.runOnIdle { draft.value = draft.value.copy(isSaving = true) }
        listOf("arrival-period-am", "arrival-period-pm").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsNotEnabled().performClick()
        }
        assertEquals(LocalTime.of(18, 30), draft.value.arrivalTime)
    }

    @Test fun narrowLargeFontPeriodControlsRemainReachableAndWheelsAlign() {
        val draft = mutableStateOf(ItineraryEditDraft("item", "09:30", "120"))
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) { EasyTripTheme {
            Column(Modifier.width(280.dp)) {
                ItineraryTimingPickers(draft.value, { draft.value = draft.value.copy(arrivalTimeText = it) }, {})
            }
        } } }
        compose.onNodeWithTag("arrival-period-pm").assertIsDisplayed().performClick()
        compose.assertArrivalTime(21, 30)
        val am = compose.onNodeWithTag("arrival-period-am").fetchSemanticsNode().boundsInRoot
        val pm = compose.onNodeWithTag("arrival-period-pm").fetchSemanticsNode().boundsInRoot
        assertTrue(am.right <= pm.left)
        val hour = compose.onNodeWithTag("arrival-hour-picker").fetchSemanticsNode().boundsInRoot
        val stay = compose.onNodeWithTag("stay-hours-picker").fetchSemanticsNode().boundsInRoot
        assertEquals(hour.top, stay.top, 1f)
        assertTrue(pm.right <= stay.left)
        saveEvidence("itinerary-time-wheel-large-font.png")
    }

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
        val draft = mutableStateOf(ItineraryEditDraft("item", "09:30", "120", placeName = "西湖天地"))
        compose.setContent { EasyTripTheme { Column(Modifier.width(360.dp)) {
            ItineraryPlaceRow(ItineraryItemUi("visual", "西湖天地", "湖滨路", LocalTime.of(9, 30), 120), displayOrder = 1)
            EditItineraryItemContent(draft.value, { draft.value = draft.value.copy(arrivalTimeText = it) }, {},
                onScheduleAgain = {}, onSave = {}, onCancel = {})
        } } }
        listOf("am", "pm").forEach { period ->
            compose.onNodeWithTag("arrival-period-$period").performClick()
            saveEvidence("itinerary-time-wheel-$period.png")
        }
        compose.onNodeWithTag("arrival-hour-picker").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        saveEvidence("itinerary-time-wheel-unset.png")
    }

    private fun saveEvidence(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(compose.activity.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
