package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette
import com.yangchengwei.easytrip.expense.ExpenseCategory
import com.yangchengwei.easytrip.expense.ExpenseDraftRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditPlaceSectionsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun namedSectionsKeepExpenseActionsTogetherAndSaveOutsideScrollingContent() {
        val draft = mutableStateOf(ItineraryEditDraft(
            "hotel", "09:30", "120", placeName = "湖畔酒店",
            expenses = listOf(ExpenseDraftRow("first", amount = "600", category = ExpenseCategory.LODGING)),
        ))
        compose.setContent { EasyTripTheme {
            EditItineraryItemContent(
                draft.value,
                onArrivalTimeChange = { draft.value = draft.value.copy(arrivalTimeText = it) },
                onStayMinutesChange = { draft.value = draft.value.copy(stayMinutesText = it) },
                onNoteChange = { draft.value = draft.value.copy(noteText = it) },
                onSave = {}, onCancel = {},
                modifier = Modifier.width(390.dp).height(680.dp),
            )
        } }
        compose.onNodeWithText("时间安排").assertExists()
        compose.onNodeWithText("地点备注").assertExists()
        compose.onNodeWithTag("itinerary-section-timing").assertExists()
        compose.onNodeWithTag("itinerary-section-expenses").assertExists()
        compose.onNodeWithTag("itinerary-section-note").assertExists()
        compose.onNode(hasTestTag("expense-add") and
            hasAnyAncestor(hasTestTag("itinerary-section-expenses"))).assertExists()
        val saveBounds = compose.onNodeWithTag("itinerary-save").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("itinerary-note-input").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("itinerary-save").assertIsDisplayed().assertIsEnabled()
        assertEquals(saveBounds, compose.onNodeWithTag("itinerary-save").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("itinerary-timing-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("arrival-hour-picker").assertExists()
        compose.onNodeWithTag("expense-amount-first").assertTextContains("600")
        compose.onNodeWithTag("itinerary-save").assertIsDisplayed()
    }

    @Test fun emptyZeroIncompleteSavingAndMissingSourceKeepDistinctSaveStates() {
        val draft = mutableStateOf(ItineraryEditDraft("hotel", "", "", placeName = "湖畔酒店"))
        compose.setContent { EasyTripTheme {
            EditItineraryItemContent(draft.value, {}, {}, onSave = {}, onCancel = {},
                modifier = Modifier.width(390.dp).height(680.dp))
        } }
        compose.onNodeWithText("未记录").assertExists()
        compose.onNodeWithTag("itinerary-save").assertIsEnabled()
        capture("empty")
        compose.runOnIdle { draft.value = draft.value.copy(
            expenses = listOf(ExpenseDraftRow("first", amount = "0", category = ExpenseCategory.LODGING))) }
        compose.onNodeWithText("1笔 · ¥0").assertExists()
        compose.onNodeWithTag("itinerary-save").assertIsEnabled()
        compose.runOnIdle { draft.value = draft.value.copy(
            expenses = listOf(ExpenseDraftRow("first", amount = "600"))) }
        compose.onNodeWithText("请完善费用").assertExists()
        compose.onNodeWithTag("itinerary-save").assertIsNotEnabled()
        compose.runOnIdle { draft.value = draft.value.copy(
            expenses = listOf(ExpenseDraftRow("first", amount = "600", category = ExpenseCategory.LODGING)),
            isSaving = true) }
        compose.onNodeWithTag("itinerary-save").assertTextContains("保存中…").assertIsNotEnabled()
        compose.onNodeWithTag("itinerary-cancel").assertIsNotEnabled()
        compose.onNodeWithTag("expense-add").assertIsNotEnabled()
        compose.onNodeWithTag("itinerary-timing-toggle").assertIsNotEnabled()
        compose.runOnIdle { draft.value = draft.value.copy(isSaving = false, sourceMissing = true) }
        compose.onNodeWithText("本次安排已被移除，无法保存。草稿仍保留，可查看后放弃。").assertExists()
        compose.onNodeWithTag("itinerary-save").assertIsNotEnabled()
        compose.onNodeWithTag("expense-amount-first").assertTextContains("600")
        compose.onNodeWithTag("itinerary-cancel").assertIsEnabled()
    }

    @Test fun multipleExpensesOpenWithinTheirSectionWithoutLosingOtherDraftFields() {
        val draft = mutableStateOf(ItineraryEditDraft("hotel", "09:30", "120",
            placeName = "湖畔酒店", noteText = "到店后联系前台",
            expenses = listOf(
                ExpenseDraftRow("hotel", amount = "600", category = ExpenseCategory.LODGING, note = "房费"),
                ExpenseDraftRow("dinner", amount = "120", category = ExpenseCategory.FOOD, note = "晚餐"),
                ExpenseDraftRow("breakfast", amount = "40", category = ExpenseCategory.FOOD, note = "早餐"),
            )))
        compose.setContent { EasyTripTheme {
            EditItineraryItemContent(draft.value, {}, {}, onExpenseAction = {
                if (it is DayItineraryAction.ExpandExpense) draft.value = draft.value.copy(expandedExpenseKey = it.key)
            }, onSave = {}, onCancel = {}, modifier = Modifier.width(390.dp).height(740.dp))
        } }
        compose.onNodeWithText("3笔 · ¥760").assertExists()
        capture("multiple")
        compose.onNodeWithTag("expense-row-dinner").performScrollTo().performClick()
        compose.onNode(hasTestTag("expense-amount-dinner") and
            hasAnyAncestor(hasTestTag("itinerary-section-expenses"))).assertTextContains("120")
        compose.onNodeWithTag("expense-note-dinner").assertTextContains("晚餐")
        compose.onNodeWithTag("itinerary-note-input").assertTextContains("到店后联系前台")
        compose.onNodeWithTag("itinerary-timing-toggle").assertTextContains("09:30", substring = true)
        capture("multiple-expanded")
    }

    @Test fun narrowDoubleFontAndEveryPaletteKeepSectionsAndActionsReachable() {
        val palette = mutableStateOf(ThemePalette.LAKE)
        var scheduleCount = 0
        var saveCount = 0
        val draft = ItineraryEditDraft("hotel", "09:30", "120", placeName = "湖畔酒店",
            expenses = listOf(ExpenseDraftRow("first", amount = "600", category = ExpenseCategory.LODGING)))
        compose.setContent { EasyTripTheme(palette.value) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EditItineraryItemContent(draft, {}, {}, onScheduleAgain = { scheduleCount++ },
                    onSave = { saveCount++ }, onCancel = {},
                    modifier = Modifier.width(320.dp).height(680.dp))
            }
        } }
        for (theme in ThemePalette.entries) {
            compose.runOnIdle { palette.value = theme }
            compose.onNodeWithTag("itinerary-timing-toggle").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("expense-amount-first").performScrollTo().assertIsDisplayed()
            ExpenseCategory.entries.forEach {
                compose.onNodeWithTag("expense-category-first-${it.storageKey}").performScrollTo()
                    .assertIsDisplayed().assertHeightIsAtLeast(44.dp)
            }
            compose.onNodeWithTag("expense-remove-first").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("expense-add").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("itinerary-note-input").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("schedule-again-place").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("itinerary-save").assertIsDisplayed().performClick()
            compose.onNodeWithTag("itinerary-cancel").assertIsDisplayed()
            capture("large-${theme.id}")
        }
        assertEquals(ThemePalette.entries.size, scheduleCount)
        assertEquals(ThemePalette.entries.size, saveCount)
    }

    @Test fun lakeSectionsHaveContrastingPageBackgroundAndBottomActions() {
        val draft = ItineraryEditDraft("hotel", "09:30", "120", placeName = "湖畔酒店",
            expenses = listOf(ExpenseDraftRow("first", amount = "600", category = ExpenseCategory.LODGING)))
        compose.setContent { EasyTripTheme {
            EditItineraryItemContent(draft, {}, {}, onScheduleAgain = {}, onSave = {}, onCancel = {},
                modifier = Modifier.width(390.dp).height(740.dp))
        } }
        val pixels = compose.onNodeWithTag("itinerary-item-editor").captureToImage().toPixelMap()
        assertEquals(Color(0xFFE5EEF0), pixels[2, 40])
        val timing = compose.onNodeWithTag("itinerary-section-timing").captureToImage().toPixelMap()
        assertEquals(Color.White, timing[timing.width / 2, timing.height / 2])
        val actions = compose.onNodeWithTag("itinerary-editor-actions").captureToImage().toPixelMap()
        assertEquals(Color.White, actions[2, actions.height / 2])
        capture("single-top")
        compose.onNodeWithTag("itinerary-note-input").performScrollTo()
        capture("single-note")
        compose.onNodeWithTag("itinerary-timing-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("arrival-hour-picker").performScrollTo().assertIsDisplayed()
        capture("timing-expanded")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "edit-place-sections").apply { mkdirs() }
        java.io.File(directory, "$name.png").outputStream().use {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
