package com.yangchengwei.easytrip.place.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.AddToItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.SelectPlacesContent
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.workspace.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class PlacePoolSharedHeaderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val rows = listOf(
        row("lake", "西湖", "杭州市", "330100", 2),
        row("temple", "灵隐寺", "杭州市", "330100", 0),
        row("bund", "外滩", "上海市", "310000", 1),
    )

    @Test fun poolUsesCityThenScheduleThenCompactSearchAndKeepsToolbar() {
        compose.setContent {
            EasyTripTheme { Surface { Box(Modifier.width(390.dp).height(580.dp)) {
                PlacePoolContent(
                    PlacePoolUiState(rows = rows, allRows = rows, savedPoiIds = rows.map { it.place.amapPoiId }.toSet()),
                    showSearch = false, onAction = {},
                )
            } } }
        }
        compose.onNodeWithTag("place-pool-collection-total").assertTextEquals("已收藏 3 个")
        compose.onNodeWithTag("start-add-to-itinerary").assertIsDisplayed()
        compose.onNodeWithTag("place-pool-schedule-ALL").assertIsSelected()
        val city = compose.onNodeWithTag("place-city-filters").getUnclippedBoundsInRoot()
        val schedule = compose.onNodeWithTag("place-pool-schedule-ALL").getUnclippedBoundsInRoot()
        val search = compose.onNodeWithTag("place-pool-local-search").getUnclippedBoundsInRoot()
        assertTrue("City filters must precede schedule filters", city.bottom <= schedule.top)
        assertTrue("Schedule filters must precede search", schedule.bottom <= search.top)
        assertTrue("Use a compact search field instead of the outlined field", search.bottom - search.top <= 40.dp)
        compose.onNodeWithTag("select-places-close").assertDoesNotExist()
        compose.onNodeWithTag("select-places-continue").assertDoesNotExist()
        capture("pool")
    }

    @Test fun poolCombinesCityScheduleSearchAndTagsWithoutChangingCollectionCount() {
        val tag = PlaceTag("scenery", "风景")
        val taggedRows = rows.map { row ->
            row.copy(place = row.place.copy(note = if (row.id == "lake") "SUNSET 赏月" else "",
                tags = if (row.id == "lake") listOf(tag) else emptyList()))
        }
        var state by mutableStateOf(PlacePoolUiState(
            rows = taggedRows, allRows = taggedRows, tags = listOf(tag),
            savedPoiIds = taggedRows.map { it.place.amapPoiId }.toSet(),
        ))
        var remoteSearches = 0
        compose.setContent {
            EasyTripTheme { Surface {
                PlacePoolContent(state, showSearch = false, onSearch = { remoteSearches++ }, onAction = {
                    state = when (it) {
                        is PlacePoolAction.SelectCity -> state.copy(selectedCityKey = it.key)
                        is PlacePoolAction.SelectSchedule -> state.copy(scheduleFilter = it.filter)
                        is PlacePoolAction.SetLocalQuery -> state.copy(localQuery = it.value)
                        is PlacePoolAction.ToggleTag -> {
                            val selected = if (it.id in state.selectedTagIds) emptySet() else setOf(it.id)
                            state.copy(selectedTagIds = selected,
                                rows = taggedRows.filter { row -> selected.all { id -> row.place.tags.any { it.id == id } } })
                        }
                        else -> state
                    }
                })
            } }
        }
        compose.onNodeWithTag("place-city-330100").performClick()
        compose.onNodeWithTag("place-pool-schedule-UNSCHEDULED").performClick()
        compose.onNodeWithTag("saved-place-temple").assertIsDisplayed()
        compose.onNodeWithTag("saved-place-lake").assertDoesNotExist()
        compose.onNodeWithTag("saved-place-bund").assertDoesNotExist()
        compose.onNodeWithTag("place-pool-local-search").performTextInput("sunset")
        compose.onNodeWithTag("place-pool-filter-empty").assertIsDisplayed()
        compose.onNodeWithTag("place-pool-schedule-SCHEDULED").performClick()
        compose.onNodeWithTag("saved-place-lake").assertIsDisplayed()
        compose.onNodeWithTag("place-pool-clear-search").performClick()
        compose.onNodeWithTag("place-city-all").performClick()
        compose.onNodeWithTag("tag-scenery").performClick()
        compose.onNodeWithTag("saved-place-lake").assertIsDisplayed()
        compose.onNodeWithTag("saved-place-bund").assertDoesNotExist()
        compose.onNodeWithTag("place-pool-collection-total").assertTextEquals("已收藏 3 个")
        compose.onNodeWithContentDescription("杭州市，2 个地点").assertExists()
        compose.onNodeWithTag("place-pool-local-search").performTextInput("风景")
        compose.onNodeWithTag("saved-place-lake").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, remoteSearches) }
    }

    @Test fun pickerOwnsItsFiltersAndPoolKeepsItsSelectionWhenReturning() {
        val pool = PlacePoolUiState(rows = rows, allRows = rows, selectedCityKey = "330100",
            localQuery = "西湖", scheduleFilter = PlaceScheduleFilter.SCHEDULED)
        var showingPicker by mutableStateOf(false)
        var selection by mutableStateOf(AddToItineraryUiState(selectedPlaceIds = listOf("lake")))
        compose.setContent {
            EasyTripTheme { Surface {
                if (showingPicker) {
                    SelectPlacesContent(rows, selection,
                        onTogglePlace = { selection = selection.copy(selectedPlaceIds = selection.selectedPlaceIds + it) },
                        onContinue = {}, onClose = { showingPicker = false })
                } else {
                    PlacePoolContent(pool, showSearch = false, onAction = {
                        if (it == PlacePoolAction.StartAddToItinerary) showingPicker = true
                    })
                }
            } }
        }
        compose.onNodeWithTag("start-add-to-itinerary").performClick()
        compose.onNodeWithTag("place-city-all").assertIsSelected()
        compose.onNodeWithTag("place-schedule-ALL").assertIsSelected()
        assertEquals("", compose.onNodeWithTag("select-places-search")
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithTag("select-place-lake").assertIsSelected()
        compose.onNodeWithTag("place-schedule-UNSCHEDULED").performClick()
        compose.onNodeWithTag("select-place-temple").performClick()
        compose.onNodeWithTag("select-places-search").performTextInput("灵隐")
        compose.onNodeWithTag("select-places-close").performClick()
        compose.onNodeWithTag("place-city-330100").assertIsSelected()
        compose.onNodeWithTag("place-pool-schedule-SCHEDULED").assertIsSelected()
        compose.onNodeWithTag("place-pool-local-search").assertTextEquals("西湖")
        compose.runOnIdle { assertEquals(listOf("lake", "temple"), selection.selectedPlaceIds) }
    }

    @Test fun busyPickerDisablesAllHeaderControls() {
        var state by mutableStateOf(AddToItineraryUiState(selectedPlaceIds = listOf("lake")))
        compose.setContent {
            EasyTripTheme { Surface {
                SelectPlacesContent(rows, state, {}, {}, {})
            } }
        }
        compose.onNodeWithTag("select-places-search").performTextInput("西湖")
        compose.runOnIdle { state = state.copy(isSubmitting = true) }
        compose.onNodeWithTag("place-city-330100").assertIsNotEnabled()
        compose.onNodeWithTag("place-schedule-UNSCHEDULED").assertIsNotEnabled()
        compose.onNodeWithTag("select-places-search").assertIsNotEnabled()
        compose.onNodeWithTag("select-places-clear-search").assertIsNotEnabled()
        compose.onNodeWithTag("select-places-search").assertTextEquals("西湖")
        compose.onNodeWithTag("select-place-lake").assertIsSelected()
        capture("picker")
    }

    @Test fun narrowLargeFontHeaderKeepsSearchAndFiltersInsideBounds() {
        compose.setContent {
            EasyTripTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.6f)) {
                    Surface { Box(Modifier.width(320.dp).height(600.dp)) {
                        PlacePoolContent(PlacePoolUiState(rows = rows, allRows = rows, localQuery = "地点"),
                            showSearch = false, onAction = {})
                    } }
                }
            }
        }
        val filters = compose.onNodeWithTag("place-city-filters").getUnclippedBoundsInRoot()
        val search = compose.onNodeWithTag("place-pool-local-search").getUnclippedBoundsInRoot()
        val clear = compose.onNodeWithTag("place-pool-clear-search").getUnclippedBoundsInRoot()
        assertTrue(search.left >= filters.left && search.right <= clear.left)
        assertTrue(clear.right <= filters.right)
        compose.onNodeWithTag("place-pool-schedule-SCHEDULED").assertIsDisplayed()
        capture("pool-large-font")
    }

    @Test fun expandedWorkspaceKeepsHeaderAndPlaceListReachable() {
        compose.setContent {
            EasyTripTheme { Box(Modifier.width(390.dp).height(844.dp)) {
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(
                        TripWorkspaceUiState(tripName = "杭州旅行", section = WorkspaceSection.PLACE_POOL,
                            sheetLevel = WorkspaceSheetLevel.EXPANDED).toReadyState()),
                    mapState = WorkspaceMapState.Ready,
                    onAction = {},
                    placeState = PlacePoolUiState(rows = rows, allRows = rows,
                        savedPoiIds = rows.map { it.place.amapPoiId }.toSet()),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { _ -> Text("地图测试区域") },
                )
            } }
        }
        compose.onNodeWithTag("place-pool-local-search").assertIsDisplayed()
        compose.onNodeWithTag("place-pool-schedule-UNSCHEDULED").assertIsDisplayed()
        compose.onNodeWithTag("saved-place-lake").assertIsDisplayed()
        val search = compose.onNodeWithTag("place-pool-local-search").getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("workspace-place-list").getUnclippedBoundsInRoot()
        assertTrue(search.bottom <= list.top)
        capture("workspace")
    }

    private fun capture(name: String) {
        File(compose.activity.getExternalFilesDir(null), "shared-header-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun row(id: String, name: String, city: String, code: String, count: Int) = SavedPlaceRowUi(
        SavedPlace(id, "trip", id, name, "地址", GeoPoint(30.0, 120.0), "", emptyList(), city, code), count, count > 0,
    )
}
