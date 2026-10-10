package com.yangchengwei.easytrip.place.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Runs real Compose hosts on the dedicated emulator; captures are not design mockups. */
class PlaceCategoryVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val place = SavedPlace("visual", "trip", "poi", "西湖风景名胜区", "杭州市西湖区龙井路1号",
        GeoPoint(30.25, 120.15), "下午散步，留意开放时间。", listOf(PlaceTag("family", "亲子")),
        category = PlaceCategory.ATTRACTION)
    private val tags = setOf("亲子", "朋友推荐", "备选", "带孩子慢慢逛的城市公园和自然博物馆")

    @Test fun listAndReadOnlyDetailUseTheSameFiveCategories() {
        val selected = mutableStateOf<SavedPlace?>(null)
        compose.setContent { EasyTripTheme {
            Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                val detail = selected.value
                if (detail == null) Column(Modifier.padding(16.dp)) {
                    PlaceCategory.entries.forEach { category ->
                        val row = place.copy(id = category.storageKey, name = "${category.label}收藏地点",
                            category = category, note = "", tags = emptyList())
                        SavedPlaceRow(SavedPlaceRowUi(row, 0, false), {}, { selected.value = row }, {}, {})
                    }
                } else PlaceDetailPanel(detail.toCandidate(), detail, null, PlaceDetailSource.PlacePool,
                    false, null, { if (it == PlaceDetailPanelAction.Dismiss) selected.value = null })
            }
        } }
        PlaceCategory.entries.forEach { compose.onNodeWithText(it.label).assertIsDisplayed() }
        capture("list-five-categories")
        compose.onNodeWithTag("open-place-detail-lodging").performClick()
        compose.onNodeWithText("住宿").assertIsDisplayed()
        capture("saved-detail-lodging")
    }

    @Test fun selectionChangesBackgroundAsWellAsBorderAndCheckmark() {
        val category = mutableStateOf(PlaceCategory.OTHER)
        compose.setContent { EasyTripTheme {
            PlaceCategorySelector(category.value, true, { category.value = it })
        } }
        val food = compose.onNodeWithTag("place-category-food")
        fun background() = food.captureToImage().toPixelMap().let { it[it.width / 6, it.height / 2] }
        val before = background()
        food.performClick().assertIsSelected()
        assertTrue("Selected background must differ from unselected background", before != background())
    }

    @Before fun matchProductionWindow() {
        compose.runOnUiThread {
            val main = android.content.ComponentName(compose.activity, com.yangchengwei.easytrip.MainActivity::class.java)
            compose.activity.window.setSoftInputMode(compose.activity.packageManager.getActivityInfo(main, 0).softInputMode)
        }
    }

    @Test fun editorFiveThemesFiveCategoriesSmallScreenLargeTextAndSaveStates() {
        val palette = mutableStateOf(ThemePalette.LAKE)
        val width = mutableStateOf(390)
        val font = mutableStateOf(1f)
        val draft = mutableStateOf(PlaceDetailEditState(place.id, place.note, tags, category = place.category))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font.value)) {
                EasyTripTheme(palette.value) {
                    Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                        Box(Modifier.requiredWidth(width.value.dp)) {
                            PlaceDetailPanel(place.toCandidate(), place, draft.value, PlaceDetailSource.PlacePool,
                                false, null, { action ->
                                    when (action) {
                                        is PlaceDetailPanelAction.CategoryChanged -> draft.value = draft.value.copy(category = action.value)
                                        PlaceDetailPanelAction.CancelEdit -> draft.value = draft.value.copy(showDiscardConfirmation = true)
                                        PlaceDetailPanelAction.ContinueEditing -> draft.value = draft.value.copy(showDiscardConfirmation = false)
                                        else -> Unit
                                    }
                                })
                        }
                    }
                }
            }
        }
        ThemePalette.entries.forEachIndexed { index, theme ->
            compose.runOnIdle { palette.value = theme }
            val category = PlaceCategory.entries[index]
            compose.onNodeWithTag("place-category-${category.storageKey}").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("place-detail-dismiss").performScrollTo()
            capture("theme-${theme.id}-${category.storageKey}")
        }
        compose.runOnIdle { width.value = 320; font.value = 2f }
        compose.onNodeWithTag("place-category-other").performScrollTo().assertIsDisplayed()
        capture("editor-320-double-font")
        compose.onNodeWithTag("place-detail-tags-input").performScrollTo().assertIsDisplayed()
        capture("long-tags-320-double-font")
        compose.onNodeWithTag("place-detail-save").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-cancel").assertIsDisplayed()
        compose.runOnIdle { draft.value = draft.value.copy(isSaving = true) }
        compose.onNodeWithTag("place-detail-save").assertIsNotEnabled()
        capture("saving-320-double-font")
        compose.runOnIdle { draft.value = draft.value.copy(isSaving = false, errorMessage = "保存失败，请稍后重试") }
        compose.onNodeWithTag("place-detail-save-error").assertIsDisplayed()
        capture("save-failure-320-double-font")
        compose.onNodeWithTag("place-detail-cancel").performClick()
        compose.onNodeWithTag("place-detail-discard-confirm").assertIsDisplayed()
        capture("discard-320-double-font")
        compose.onNodeWithTag("place-detail-continue-editing").performClick()
        compose.onNodeWithTag("place-detail-save-error").assertIsDisplayed()
    }

    @Test fun workspaceKeyboardKeepsInputAndBothActionsVisible() = verifyKeyboard("workspace")
    @Test fun searchKeyboardKeepsInputAndBothActionsVisibleWithoutConsent() = verifyKeyboard("search")
    @Test fun compatibilityDialogKeyboardKeepsBothActionsVisible() = verifyKeyboard("dialog")

    private fun verifyKeyboard(host: String) {
        val note = mutableStateOf(place.note)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                EasyTripTheme {
                    Box(Modifier.fillMaxSize().requiredWidth(320.dp)) {
                        val draft = PlaceDetailDraft(note.value, tags, place.id, category = place.category)
                        val edit = PlaceDetailEditState(place.id, note.value, tags, category = place.category)
                        when (host) {
                            "workspace" -> WorkspacePlaceDetailSheet(
                                PlacePoolUiState(editing = place, detailDraft = draft),
                                PlaceScheduleSummaryUi(isKnown = true), {
                                    if (it is PlaceDetailPanelAction.NoteChanged) note.value = it.value
                                })
                            "search" -> PlaceSearchContent(
                                PlaceSearchUiState(
                                    search = PlaceSearchState(results = listOf(place.toCandidate())),
                                    displayMode = SearchDisplayMode.MapDetail(place.amapPoiId),
                                    savedPlacesByPoiId = mapOf(place.amapPoiId to place), detailDraft = edit),
                                onAction = { if (it is PlaceSearchAction.UpdateEditNote) note.value = it.value },
                                mapHostFactory = { error("Map must not be created before consent") })
                            else -> PlaceDetailDialog(place, draft, false, null, PlaceDetailSource.PlacePool,
                                { note.value = it }, {}, {}, {}, {})
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("place-detail-note-input").performScrollTo().performClick().performTextInput("键盘输入")
        compose.waitUntil(5_000) { ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true || host == "dialog" }
        compose.onNodeWithTag("place-detail-note-input").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("place-detail-save").assertIsDisplayed()
        compose.onNodeWithTag("place-detail-cancel").assertIsDisplayed()
        assertTrue(note.value.contains("键盘输入"))
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        compose.waitUntil(5_000) {
            automation.windows.any { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        }
        compose.waitUntil(5_000) {
            val windows = automation.windows
            val ime = windows.first { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            val application = windows.first { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }
            val imeBounds = android.graphics.Rect().also(ime::getBoundsInScreen)
            val applicationBounds = android.graphics.Rect().also(application::getBoundsInScreen)
            listOf("place-detail-save", "place-detail-cancel").all {
                val bounds = compose.onNodeWithTag(it).fetchSemanticsNode().boundsInWindow
                bounds.height > 0 && bounds.bottom + applicationBounds.top <= imeBounds.top
            }
        }
        capture("$host-ime-320-double-font")
        pressBack()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // UiAutomation screenshots include platform window/ripple animations outside Compose's clock.
        android.os.SystemClock.sleep(500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = File(compose.activity.getExternalFilesDir(null), "v2.2-place-categories").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
