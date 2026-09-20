package com.yangchengwei.easytrip.place.ui

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
import com.yangchengwei.easytrip.itinerary.ui.*
import com.yangchengwei.easytrip.place.domain.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class PlaceCityGroupingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val rows = listOf(
        row("lake", "西湖风景名胜区", "杭州市", "330100", "西湖区 · 龙井路 1 号"),
        row("temple", "灵隐寺", "杭州市", "330100", "西湖区 · 法云弄 1 号"),
        row("bund", "外滩", "上海市", "310000", "黄浦区 · 中山东一路"),
        row("garden", "拙政园", "苏州市", "320500", "姑苏区 · 东北街"),
    )

    @Test fun pickerPreservesCrossCitySelectionThroughScopedSearchAndClear() {
        val state = mutableStateOf(AddToItineraryUiState(step = AddToItineraryStep.SELECT_PLACES))
        var submitted = emptyList<String>()
        compose.setContent {
            EasyTripTheme { Surface { Box(Modifier.width(390.dp).height(578.dp).padding(16.dp)) {
                SelectPlacesContent(rows, state.value,
                    onTogglePlace = { id -> state.value = state.value.copy(selectedPlaceIds = state.value.selectedPlaceIds.toMutableList().apply { if (!remove(id)) add(id) }) },
                    onContinue = { submitted = state.value.selectedPlaceIds }, onClose = {})
            } } }
        }
        compose.onNodeWithTag("place-city-330100").performClick()
        compose.onNodeWithTag("select-place-bund").assertDoesNotExist()
        compose.onNodeWithTag("select-place-lake").performClick()
        compose.onNodeWithTag("place-city-310000").performClick()
        compose.onNodeWithTag("select-place-bund").performClick()
        compose.onNodeWithTag("select-places-search").performTextInput("龙井路")
        compose.onNodeWithTag("select-places-empty").assertIsDisplayed()
        compose.onNodeWithContentDescription("杭州市，2 个地点").assertExists()
        compose.onNodeWithText("已选 2 个地点").assertIsDisplayed()
        compose.onNodeWithTag("place-city-all").performClick()
        compose.onNodeWithTag("select-place-lake").assertIsSelected()
        compose.onNodeWithTag("select-places-clear-search").performClick()
        compose.onNodeWithTag("select-place-order-lake", true).assertTextEquals("1")
        compose.onNodeWithTag("select-place-order-bund", true).assertTextEquals("2")
        compose.onNodeWithTag("select-places-continue").performClick()
        assertEquals(listOf("lake", "bund"), submitted)
        screenshot("city-picker.png")
    }

    @Test fun poolSwitchesCitiesKeepsToolbarFixedAndRestoresAllWhenCityDisappears() {
        val pool = mutableStateOf(rows)
        val city = mutableStateOf<String?>(null)
        val actions = mutableListOf<PlacePoolAction>()
        compose.setContent {
            EasyTripTheme { Surface { Box(Modifier.width(390.dp).height(430.dp)) {
                PlacePoolContent(PlacePoolUiState(rows = pool.value, allRows = pool.value, selectedCityKey = city.value, savedPoiIds = pool.value.map { it.place.amapPoiId }.toSet()), showSearch = false, onAction = {
                    actions += it
                    if (it is PlacePoolAction.SelectCity) city.value = it.key
                })
            } } }
        }
        compose.onNodeWithTag("place-city-330100").performClick()
        compose.onNodeWithTag("saved-place-lake").assertIsDisplayed()
        compose.onNodeWithTag("saved-place-bund").assertDoesNotExist()
        compose.onNodeWithTag("place-pool-collection-total").assertTextEquals("已收藏 4 个")
        compose.onNodeWithTag("start-add-to-itinerary").performClick()
        assertEquals(PlacePoolAction.StartAddToItinerary, actions.last())
        screenshot("city-pool.png")
        compose.runOnIdle { pool.value = rows.filterNot { it.place.cityAdCode == "330100" } }
        compose.onNodeWithTag("place-city-all").assertIsSelected()
        compose.onNodeWithTag("saved-place-bund").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun row(id: String, name: String, city: String, code: String, address: String) = SavedPlaceRowUi(
        SavedPlace(id, "trip", "poi-$id", name, address, GeoPoint(30.0, 120.0), "", emptyList(), city, code), 0, false,
    )
}
