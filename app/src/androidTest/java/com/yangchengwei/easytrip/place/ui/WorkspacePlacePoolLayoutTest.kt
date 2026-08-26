package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.workspace.TripWorkspaceContent
import com.yangchengwei.easytrip.workspace.TripWorkspacePageState
import com.yangchengwei.easytrip.workspace.TripWorkspaceUiState
import com.yangchengwei.easytrip.workspace.WorkspaceMapState
import com.yangchengwei.easytrip.workspace.toReadyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspacePlacePoolLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun longPlaceNameKeepsActionsInsideRow() {
        val place = SavedPlace(
            "place",
            "trip",
            "poi-place",
            "西湖风景名胜区附近一个非常长的收藏地点名称",
            "这是一条足以触发省略但不能挤出编辑和删除操作的地址",
            GeoPoint(39.9, 116.4),
            "",
            emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(rows = listOf(SavedPlaceRowUi(place, 0, false))),
                    showSearch = false,
                    onAction = {},
                )
            }
        }

        val row = compose.onNodeWithTag("saved-place-place").getUnclippedBoundsInRoot()
        val quickAdd = compose.onNodeWithTag("quick-add-place-place").getUnclippedBoundsInRoot()
        val more = compose.onNodeWithTag("more-place-place").getUnclippedBoundsInRoot()
        assertTrue("row=$row quickAdd=$quickAdd", quickAdd.right <= row.right)
        assertTrue("row=$row more=$more", more.right <= row.right)
    }

    @Test fun workspacePlacePoolUsesOnlySheetHorizontalInset() {
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = TripWorkspacePageState.Ready(TripWorkspaceUiState(tripName = "北京").toReadyState()),
                    mapState = WorkspaceMapState.Ready,
                    onAction = {},
                    placeState = PlacePoolUiState(
                        rows = listOf(
                            SavedPlaceRowUi(
                                SavedPlace("place", "trip", "poi-place", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList()),
                                0,
                                false,
                            ),
                        ),
                    ),
                    onPlaceAction = {},
                    itineraryState = DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                )
            }
        }

        val tabs = compose.onNodeWithTag("workspace-tabs").getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("workspace-place-list").getUnclippedBoundsInRoot()
        assertEquals(tabs.left, list.left)
        assertEquals(tabs.right, list.right)
    }
}
