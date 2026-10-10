package com.yangchengwei.easytrip.expense

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette
import com.yangchengwei.easytrip.expense.ui.*
import java.time.*
import kotlinx.coroutines.flow.*
import org.junit.Rule
import org.junit.Test

class ExpenseSectionsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val clock = Clock.fixed(Instant.parse("2025-12-01T00:00:00Z"), ZoneOffset.UTC)

    @Test fun thousandsAppearInTotalsAndMonthlyBarsWithoutChangingYear() {
        val store = Store()
        store.records.value = listOf(store.records.value!!.first().copy(cents = 1234560))
        val model = ExpenseReviewViewModel(store, clock)
        compose.setContent { EasyTripTheme { ExpenseReviewScreen(model, {}, {}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("expense-total").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("expense-total").assertTextEquals("¥12,345.60")
        compose.onNodeWithTag("expense-choose-year").assertTextContains("2025年", substring = true)
        capture("v220-thousands")
        compose.onNodeWithText("12,345.6").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("review-month-12").performScrollTo().performClick()
        compose.onNodeWithTag("expense-total").assertTextEquals("¥12,345.60")
    }
    @Test fun monthNavigationAndSelectedPickerKeepParentYear() {
        val model = ExpenseReviewViewModel(Store(), clock)
        compose.setContent { EasyTripTheme { ExpenseReviewScreen(model, {}, {}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("expense-total").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("expense-section-months").assertExists()
        compose.onNodeWithTag("expense-all-years").assertExists()
        compose.onNodeWithTag("review-month-12").performScrollTo().performClick()
        compose.onNodeWithTag("expense-period-next").performClick()
        compose.onNodeWithTag("expense-choose-month").assertTextContains("2026年1月", substring = true)
        compose.onNodeWithTag("expense-choose-month").performClick()
        compose.onNodeWithTag("picker-month-1").assertIsSelected()
        compose.onNodeWithTag("expense-picker-cancel").performClick()
        compose.onNodeWithTag("expense-back").performClick()
        compose.onNodeWithTag("expense-choose-year").assertTextContains("2025年", substring = true)
    }

    @Test fun loadingAndFailureKeepPeriodControlsAndRetryUsesCurrentRange() {
        val store = Store().apply { failure = true }
        val model = ExpenseReviewViewModel(store, clock)
        compose.setContent { EasyTripTheme { ExpenseReviewScreen(model, {}, {}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("expense-load-error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("expense-period-next").performClick()
        compose.onNodeWithTag("expense-choose-year").assertTextContains("2026年", substring = true)
        capture("error")
        compose.runOnIdle { store.failure = false; store.records.value = null; model.retry() }
        compose.onNodeWithTag("expense-loading").assertExists()
        compose.onNodeWithTag("expense-period-previous").performClick()
        compose.onNodeWithTag("expense-choose-year").assertTextContains("2025年", substring = true)
        capture("loading")
        compose.onNodeWithTag("expense-choose-year").performClick()
        compose.onAllNodesWithText("尚无记录").assertCountEquals(0)
        compose.onNodeWithTag("picker-year-2025").assertTextContains("统计待加载", substring = true)
        compose.onNodeWithTag("expense-picker-cancel").performClick()
        compose.runOnIdle { store.records.value = emptyList() }
        compose.onNodeWithTag("expense-total").assertTextEquals("—")
        capture("empty")
    }

    @Test fun screenshotsCoverAnnualHistoryMonthDetailsEditorUndatedAndZero() {
        val store = Store()
        val model = ExpenseReviewViewModel(store, clock)
        compose.setContent { EasyTripTheme { ExpenseReviewScreen(model, {}, {}, onTrips = {}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("expense-total").fetchSemanticsNodes().isNotEmpty() }
        capture("annual")
        compose.onNodeWithTag("expense-choose-year").performClick()
        compose.onNodeWithTag("picker-year-2025").assertIsSelected()
        capture("year-picker")
        compose.onNodeWithTag("expense-picker-cancel").performClick()
        compose.onNodeWithTag("expense-all-years").performClick()
        compose.onNodeWithText("历年花费").assertExists()
        capture("history")
        compose.onNodeWithTag("expense-back").performClick()
        compose.onNodeWithTag("review-month-12").performScrollTo().performClick()
        capture("month")
        compose.onNodeWithTag("expense-choose-month").performClick()
        capture("month-picker")
        compose.onNodeWithTag("expense-picker-cancel").performClick()
        compose.onNodeWithTag("review-category-food").performScrollTo().performClick()
        capture("details")
        compose.onNodeWithTag("expense-record-edit").performScrollTo().performClick()
        capture("editor")
        compose.onNodeWithTag("expense-record-delete").performScrollTo().performClick()
        capture("delete")
        compose.onNode(hasText("取消") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("expense-back").performClick()
        compose.onNodeWithTag("expense-back").performClick()
        compose.onNodeWithTag("review-undated").performScrollTo().performClick()
        compose.onNodeWithTag("expense-total").assertTextEquals("¥0.00")
        capture("undated-zero")
        compose.onNodeWithTag("review-category-attraction").performScrollTo().assertTextContains("¥0.00", substring = true)
        compose.onNodeWithTag("review-category-food").assertTextContains("尚无记录", substring = true)
    }

    @Test fun largeFontAndAllPalettesKeepNavigationAndCategoryTargetsReachable() {
        val model = ExpenseReviewViewModel(Store(), clock)
        var palette by mutableStateOf(ThemePalette.LAKE)
        compose.setContent {
            EasyTripTheme(palette) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    Box(Modifier.widthIn(max = 320.dp)) {
                        ExpenseReviewScreen(model, {}, {})
                    }
                }
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("expense-total").fetchSemanticsNodes().isNotEmpty() }
        ThemePalette.entries.forEach { theme ->
            compose.runOnIdle { palette = theme }
            compose.onNodeWithTag("expense-period-previous").assertWidthIsAtLeast(44.dp).assertHeightIsAtLeast(44.dp).performClick()
            compose.onNodeWithTag("expense-period-next").performClick()
            capture("large-${theme.id}")
        }
        compose.onNodeWithTag("review-month-12").performScrollTo().performClick()
        compose.onNodeWithTag("expense-period-next").performScrollTo().performClick()
        compose.onNodeWithTag("expense-choose-month").assertTextContains("2026年1月", substring = true).performClick()
        compose.onNodeWithTag("picker-month-1").assertIsSelected()
        capture("large-month-picker")
        compose.onNodeWithTag("expense-picker-cancel").performClick()
        compose.onNodeWithTag("review-category-food").performScrollTo().performClick()
        compose.onNodeWithTag("expense-total").assertTextEquals("—")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Compose's idling does not cover Android dialog window enter/exit animations.
        android.os.SystemClock.sleep(350)
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(compose.activity.getExternalFilesDir(null), "v210-$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private class Store : ExpenseRepository {
        var failure = false
        val records = MutableStateFlow<List<ExpenseRecord>?>(listOf(
            ExpenseRecord(ExpenseKey(ExpenseSourceKind.PLACE, "food"), "trip", "day", "item",
                "湖畔餐厅", LocalDate.of(2025, 12, 31), 33800, ExpenseCategory.FOOD, "晚餐", "杭州旅行", 1),
            ExpenseRecord(ExpenseKey(ExpenseSourceKind.PLACE, "hotel"), "trip", "day", "item",
                "湖畔酒店", LocalDate.of(2025, 4, 12), 60000, ExpenseCategory.LODGING, null, "杭州旅行", 1),
            ExpenseRecord(ExpenseKey(ExpenseSourceKind.PLACE, "zero"), "undated", "undated-day", "park",
                "免费公园", null, 0, ExpenseCategory.ATTRACTION, null, "未定日期旅行", 1),
        ))
        override fun observeRecords(): Flow<List<ExpenseRecord>> = flow {
            check(!failure) { "读取失败，请重试" }
            emitAll(records.filterNotNull())
        }
        override suspend fun updatePlaceExpense(itemId: String, expected: ExpenseRecord, value: PlaceExpenseInput) = Unit
        override suspend fun deletePlaceExpense(itemId: String, expected: ExpenseRecord) = Unit
    }
}
