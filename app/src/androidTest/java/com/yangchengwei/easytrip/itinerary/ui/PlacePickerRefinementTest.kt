package com.yangchengwei.easytrip.itinerary.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class PlacePickerRefinementTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun row(id: String, name: String, address: String, scheduled: Boolean = false) =
        SavedPlaceRowUi(SavedPlace(id, "trip", "poi-$id", name, address, GeoPoint(30.2, 120.1), "", emptyList()), if (scheduled) 1 else 0, scheduled)

    @Test fun scheduleFilterCombinesWithSearchAndPreservesSelection() {
        val state = mutableStateOf(AddToItineraryUiState(step = AddToItineraryStep.SELECT_PLACES))
        compose.setContent { EasyTripTheme { Surface { Box(Modifier.width(390.dp).height(620.dp)) {
            SelectPlacesContent(listOf(row("a", "少林寺", "登封", true), row("b", "嵩山", "登封")), state.value,
                onTogglePlace = { state.value = state.value.copy(selectedPlaceIds = state.value.selectedPlaceIds + it) }, onContinue = {}, onClose = {})
        } } } }
        compose.onNodeWithTag("place-schedule-UNSCHEDULED").performClick()
        compose.onNodeWithTag("select-place-a").assertDoesNotExist()
        compose.onNodeWithTag("select-place-b").performClick()
        compose.onNodeWithTag("place-schedule-SCHEDULED").performClick()
        compose.onNodeWithTag("select-place-b").assertDoesNotExist()
        compose.onNodeWithTag("select-place-a").assertExists()
        compose.onNodeWithTag("select-places-search").performTextInput("少林")
        compose.onNodeWithTag("select-place-a").assertExists()
        compose.onNodeWithTag("place-schedule-ALL").performClick()
        compose.onNodeWithTag("select-places-clear-search").performClick()
        compose.onNodeWithTag("select-place-order-b", true).assertTextEquals("1")
    }

    @Test fun filteringPreservesSelectionOrderAndClearingRestoresRows() {
        val state = mutableStateOf(AddToItineraryUiState(step = AddToItineraryStep.SELECT_PLACES))
        var continued = 0
        var closed = 0
        compose.setContent {
            EasyTripTheme {
                Surface {
                    Box(Modifier.width(390.dp).height(504.dp).padding(horizontal = 16.dp)) {
                        SelectPlacesContent(
                            rows = listOf(row("lake", "西湖", "龙井路"), row("temple", "灵隐寺", "法云弄"), row("square", "西湖文化广场", "环城北路")),
                            state = state.value,
                            onTogglePlace = { id -> state.value = state.value.copy(selectedPlaceIds = state.value.selectedPlaceIds.toMutableList().apply { if (!remove(id)) add(id) }) },
                            onContinue = { continued++ }, onClose = { closed++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("select-places-continue").assertIsNotEnabled()
        compose.onNodeWithTag("select-place-temple").performClick()
        compose.onNodeWithTag("select-places-search").performTextInput("龙井路")
        compose.onNodeWithTag("select-place-temple").assertDoesNotExist()
        compose.onNodeWithTag("select-place-lake").performClick()
        compose.onNodeWithText("已选 2 个地点").assertIsDisplayed()
        compose.onNodeWithTag("select-places-search").performTextReplacement("找不到")
        compose.onNodeWithTag("select-places-empty").assertIsDisplayed()
        compose.onNodeWithTag("select-places-continue").assertIsEnabled()
        compose.onNodeWithTag("select-places-clear-search").performClick()
        compose.onNodeWithTag("select-place-order-temple", useUnmergedTree = true).assertTextEquals("1")
        compose.onNodeWithTag("select-place-order-lake", useUnmergedTree = true).assertTextEquals("2")
        compose.onNodeWithTag("select-place-temple").performClick()
        compose.onNodeWithTag("select-place-order-lake", useUnmergedTree = true).assertTextEquals("1")
        compose.onNodeWithTag("select-places-continue").performClick()
        compose.onNodeWithTag("select-places-close").performClick()
        assertEquals(listOf("lake"), state.value.selectedPlaceIds)
        assertEquals(1, continued)
        assertEquals(1, closed)
    }

    @Test fun dayEntryShowsNamedActionAndSelectionVisual() {
        compose.setContent {
            EasyTripTheme {
                Surface {
                    Box(Modifier.width(390.dp).height(538.dp).padding(horizontal = 16.dp, vertical = 12.dp)) {
                        SelectPlacesContent(
                            rows = listOf(
                                row("temple", "灵隐寺", "西湖区 · 法云弄 1 号"),
                                row("lake", "西湖风景名胜区", "西湖区 · 龙井路 1 号", true),
                                row("square", "西湖文化广场", "拱墅区 · 环城北路"),
                                row("street", "西湖天地", "上城区 · 南山路 147 号", true),
                                row("village", "龙井村", "西湖区 · 龙井路", true),
                            ),
                            state = AddToItineraryUiState(selectedPlaceIds = listOf("temple", "lake"), targetDayId = "day-1", editingTarget = AddToItineraryEditingTarget.ForDay("day-1"), step = AddToItineraryStep.SELECT_PLACES),
                            onTogglePlace = {}, onContinue = {}, onClose = {}, targetDayLabel = "第 1 天",
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("加入第 1 天").assertIsDisplayed()
        compose.onNodeWithText("第 1 天 · 已安排地点可重复添加").assertIsDisplayed()
        compose.onNodeWithTag("select-places-continue").assertIsEnabled()
        compose.onNodeWithTag("select-place-temple").assertIsSelected()
        compose.onNodeWithTag("select-place-lake").assertIsSelected()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "place-picker-final.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
