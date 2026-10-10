package com.yangchengwei.easytrip.place.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.calendar.CalendarCard
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.ItineraryPlaceRow
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PlaceCategoryShortcutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun poolIconOpensQuickPickerAndPersistsWithoutOpeningDetailOrLosingNotes() {
        val database = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val repository = RoomSavedPlaceRepository(database)
        val trip = runBlocking { RoomTripRepository(database.tripDao()).createTrip(CreateTrip("快捷分类", 1)) }
        val id = runBlocking {
            (repository.save(trip, PlaceCandidate("poi", "龙井村", "杭州", GeoPoint(30.2, 120.1), null)) as SavePlaceResult.Saved).id
        }
        runBlocking { repository.updateDetails(id, "保留收藏备注", setOf("亲子")) }
        val model = PlacePoolViewModel(trip, repository, null)
        compose.setContent {
            EasyTripTheme {
                val state by model.state.collectAsStateWithLifecycle()
                Box(Modifier.safeDrawingPadding()) {
                    PlacePoolContent(state, onAction = model::dispatch, showSearch = false)
                }
            }
        }
        compose.waitUntil(5_000) { model.state.value.rows.size == 1 }
        compose.onNodeWithText("其他").assertDoesNotExist()
        compose.onNodeWithTag("place-category-button-$id").performTouchInput { click() }
        compose.onNodeWithTag("place-category-quick-dialog").assertIsDisplayed()
        compose.onNodeWithTag("quick-category-other").assertIsSelected()
        compose.onNodeWithTag("quick-category-food").performTouchInput { click() }
        compose.waitUntil(5_000) { model.state.value.categoryPicker == null && model.state.value.rows.single().place.category == PlaceCategory.FOOD }
        compose.onNodeWithTag("place-category-button-$id").assertContentDescriptionEquals("修改龙井村分类，当前餐饮")
        assertNull(model.state.value.selectedDetailPlaceId)
        assertEquals("保留收藏备注", model.state.value.rows.single().place.note)
        assertEquals(listOf("亲子"), model.state.value.rows.single().place.tags.map { it.name })
        capture("pool-quick-saved")
        compose.onNodeWithTag("place-category-button-$id").performClick()
        compose.onNodeWithTag("quick-category-food").assertIsSelected()
        capture("pool-quick-picker")
        compose.onNodeWithText("取消").performClick()
        // Stop collectors before closing their in-memory database.
        compose.runOnIdle { androidx.lifecycle.ViewModelStore().apply { put("pool", model); clear() } }
        database.close()
    }

    @Test fun itineraryAndCalendarIconsHandleActualTouchWithoutOpeningCardOrStartingDrag() {
        var edited: String? = null
        var cardOpened = false
        var dragged = false
        val item = ItineraryItemUi("i", "知味观 · 湖滨店", "杭州", null, 60,
            placeId = "p", placeCategory = PlaceCategory.FOOD)
        compose.setContent {
            CompositionLocalProvider(LocalPlaceCategoryEdit provides { edited = it }) {
                EasyTripTheme {
                    Column(Modifier.safeDrawingPadding().width(320.dp).padding(12.dp)) {
                        ItineraryPlaceRow(item, 1)
                        CalendarCard("1. ${item.name}", "09:00–10:00", Modifier.width(220.dp).height(72.dp),
                            category = item.placeCategory, placeId = item.placeId,
                            editable = true, edges = true,
                            onClick = { cardOpened = true }, onStart = { _, _, _ -> dragged = true })
                    }
                }
            }
        }
        compose.onNodeWithTag("place-category-button-itinerary-i").performTouchInput { click() }
        compose.runOnIdle { assertEquals("p", edited); edited = null }
        compose.onNodeWithTag("place-category-button-calendar-p", useUnmergedTree = true).performTouchInput { click() }
        compose.runOnIdle { assertEquals("p", edited); assertFalse(cardOpened); assertFalse(dragged) }
        capture("itinerary-calendar-icons")
    }

    @Test fun failureAndSavingStatesRemainReadableAtLargeFontAndPreventDuplicateActions() {
        val state = mutableStateOf(PlaceCategoryPickerState("p", "名称很长的收藏地点 · 龙井村", PlaceCategory.OTHER,
            error = "保存失败，请重新选择分类重试"))
        var selected: PlaceCategory? = null
        var dismissed = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                EasyTripTheme {
                    Surface {
                        PlaceCategoryQuickDialog(state.value, { selected = it }, { dismissed = true })
                    }
                }
            }
        }
        compose.onNodeWithText("保存失败，请重新选择分类重试").performScrollTo().assertIsDisplayed()
        capture("quick-error-large-font")
        compose.onNodeWithTag("quick-category-lodging").performScrollTo().performClick()
        assertEquals(PlaceCategory.LODGING, selected)
        compose.runOnIdle { state.value = state.value.copy(saving = true, error = null) }
        compose.onNodeWithTag("quick-category-food").assertIsNotEnabled()
        compose.onNodeWithText("取消").assertIsNotEnabled()
        assertFalse(dismissed)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun calendarCategoryKeyboardActivationDoesNotOpenOrDragCard() {
        var categoryClicks = 0
        var cardClicks = 0
        var dragStarts = 0
        lateinit var inputModeManager: InputModeManager
        compose.setContent {
            EasyTripTheme {
                inputModeManager = LocalInputModeManager.current
                CompositionLocalProvider(LocalPlaceCategoryEdit provides { categoryClicks++ }) {
                    CalendarCard("龙井村", "09:00–10:00",
                        Modifier.safeDrawingPadding().width(220.dp).height(72.dp),
                        category = PlaceCategory.OTHER, placeId = "p", editable = true,
                        onClick = { cardClicks++ }, onKeyboardStart = { dragStarts++ })
                }
            }
        }
        val icon = compose.onNodeWithTag("place-category-button-calendar-p", useUnmergedTree = true)
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        icon.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        icon.assertIsFocused()
        icon.performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, categoryClicks); assertEquals(0, cardClicks); assertEquals(0, dragStarts) }
        icon.performKeyInput { pressKey(Key.Spacebar) }
        compose.runOnIdle { assertEquals(2, categoryClicks); assertEquals(0, cardClicks); assertEquals(0, dragStarts) }
    }

    @Test fun listShowsOnlyOneCategoryIndicatorForEachOfFiveCategories() {
        compose.setContent {
            EasyTripTheme {
                Column(Modifier.safeDrawingPadding().width(360.dp)) {
                    PlaceCategory.entries.forEach { category ->
                        val place = SavedPlace(category.storageKey, "trip", category.storageKey, "示例地点", "杭州",
                            GeoPoint(30.2, 120.1), "", emptyList(), category = category)
                        SavedPlaceRow(SavedPlaceRowUi(place, 0, false), null, {}, {}, {},
                            onEditCategory = {})
                    }
                }
            }
        }
        PlaceCategory.entries.forEach {
            compose.onAllNodesWithText(it.label).assertCountEquals(0)
            compose.onNodeWithTag("place-category-button-${it.storageKey}").assertIsDisplayed().assertHasClickAction()
        }
        capture("five-categories-compact-list")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val dir = File(compose.activity.getExternalFilesDir(null), "v2.2-category-shortcut").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
