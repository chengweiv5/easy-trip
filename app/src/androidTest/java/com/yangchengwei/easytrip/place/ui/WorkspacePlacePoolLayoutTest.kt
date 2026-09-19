package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripPrimaryDark
import com.yangchengwei.easytrip.core.ui.theme.EasyTripSecondary
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

    @Test fun placeRowIsCompactAndQuickAddUsesTwentyEightDpVisualAndTouchTarget() {
        val place = SavedPlace("place", "trip", "poi-place", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(rows = listOf(SavedPlaceRowUi(place, 0, false))),
                    showSearch = false,
                    onAction = {},
                )
            }
        }

        compose.onNodeWithTag("quick-add-place-place").assertHeightIsEqualTo(28.dp)
        compose.onNodeWithTag("quick-add-place-place").assertWidthIsEqualTo(28.dp)
        compose.onNodeWithTag("more-place-place").assertHeightIsEqualTo(48.dp)
        assertTrue(kotlin.math.abs(16f - compose.onNodeWithTag("workspace-place-list").getUnclippedBoundsInRoot().left.value) < 0.5f)
    }

    @Test fun batchAddStaysAboveListAndScrollbarAfterScrollingAndDispatchesOriginalAction() {
        val place = SavedPlace("place", "trip", "poi-place", "西湖", "地址", GeoPoint(39.9, 116.4), "", emptyList())
        val rows = (0 until 30).map { index ->
            SavedPlaceRowUi(place.copy(id = "place-$index", amapPoiId = "poi-$index"), 0, false)
        }
        val actions = mutableListOf<PlacePoolAction>()
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(
                        rows = rows,
                        savedPoiIds = rows.mapTo(mutableSetOf()) { it.place.amapPoiId },
                    ),
                    showSearch = false,
                    onAction = actions::add,
                )
            }
        }

        compose.onAllNodesWithTag("place-pool-marker-legend").assertCountEquals(0)
        compose.onNodeWithText("批量添加").assertIsDisplayed()
        val batchAdd = compose.onNodeWithContentDescription("批量添加到行程")
            .getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("workspace-place-list").getUnclippedBoundsInRoot()
        assertTrue("button=$batchAdd list=$list", batchAdd.bottom <= list.top)
        assertTrue(batchAdd.right <= list.right)
        compose.onNodeWithTag("workspace-place-list").performScrollToNode(hasTestTag("saved-place-place-29"))
        compose.onNodeWithTag("saved-place-place-29").assertIsDisplayed()
        compose.onNodeWithText("已收藏 30 个").assertIsDisplayed()
        compose.onNodeWithContentDescription("批量添加到行程").assertIsDisplayed()
        assertEquals(batchAdd, compose.onNodeWithContentDescription("批量添加到行程").getUnclippedBoundsInRoot())
        val scrollbar = compose.onNodeWithTag("place-pool-scrollbar-thumb", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("button=$batchAdd scrollbar=$scrollbar", batchAdd.bottom <= scrollbar.top)
        compose.onNodeWithContentDescription("批量添加到行程").performClick()
        assertEquals(listOf(PlacePoolAction.StartAddToItinerary), actions)
    }

    @Test fun scheduledPlaceRowUsesApprovedSurfaceAndSegmentColors() {
        val place = SavedPlace(
            "place",
            "trip",
            "poi-place",
            "西湖",
            "杭州市西湖区龙井路1号",
            GeoPoint(30.2, 120.1),
            "旅行备注",
            listOf(com.yangchengwei.easytrip.place.domain.PlaceTag("family", "亲子")),
        )
        val status = "已排入 2 次 · 旅行备注 · 亲子"
        compose.setContent {
            EasyTripTheme {
                SavedPlaceRow(
                    place = SavedPlaceRowUi(place, itineraryOccurrenceCount = 2, scheduled = true),
                    onQuickAdd = null,
                    onOpenDetail = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        val pixels = compose.onNodeWithTag("saved-place-place").captureToImage().toPixelMap()
        assertEquals(Color(0xFFE0E7DC), pixels[0, pixels.height / 2])
        assertEquals(Color(0xFFEEF2EB), pixels[pixels.width / 2, 12])
        assertEquals(
            EasyTripPrimaryDark,
            compose.onNodeWithText(place.name, useUnmergedTree = true)
                .fetchSemanticsNode()
                .textLayout()
                .layoutInput
                .style
                .color,
        )
        assertEquals(
            Color(0xFF486A86),
            compose.onNodeWithText(place.address, useUnmergedTree = true)
                .fetchSemanticsNode()
                .textLayout()
                .layoutInput
                .style
                .color,
        )
        val layout = compose.onNodeWithText(status, useUnmergedTree = true).fetchSemanticsNode().textLayout()
        val spans = layout.layoutInput.text.spanStyles
        fun colorAt(index: Int) = spans.lastOrNull { index >= it.start && index < it.end }
            ?.item?.color?.takeIf { it != Color.Unspecified } ?: layout.layoutInput.style.color
        assertEquals(Color(0xFFA05220), colorAt(0))
        assertEquals(EasyTripSecondary, colorAt(status.indexOf("旅行备注")))
        assertEquals(EasyTripSecondary, colorAt(status.indexOf("亲子")))
    }

    @Test fun recentlyCollectedTakesPriorityOverScheduledStatus() {
        val place = SavedPlace(
            "place",
            "trip",
            "poi-place",
            "西湖",
            "地址",
            GeoPoint(30.2, 120.1),
            "旅行备注",
            listOf(com.yangchengwei.easytrip.place.domain.PlaceTag("family", "亲子")),
        )
        val status = "刚刚收藏 · 待安排行程 · 旅行备注 · 亲子"
        compose.setContent {
            EasyTripTheme {
                SavedPlaceRow(
                    place = SavedPlaceRowUi(
                        place = place,
                        itineraryOccurrenceCount = 2,
                        scheduled = true,
                        recentlyCollected = true,
                    ),
                    onQuickAdd = null,
                    onOpenDetail = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        val layout = compose.onNodeWithText(status, useUnmergedTree = true).fetchSemanticsNode().textLayout()
        assertEquals(EasyTripSecondary, layout.layoutInput.style.color)
        assertTrue(layout.layoutInput.text.spanStyles.none { it.item.color == Color(0xFFA05220) })
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
                    mapContent = { _ -> Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                )
            }
        }

        val tabs = compose.onNodeWithTag("workspace-tabs").getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("workspace-place-list").getUnclippedBoundsInRoot()
        assertEquals(tabs.left, list.left)
        assertEquals(tabs.right, list.right)
    }

    private fun SemanticsNode.textLayout(): androidx.compose.ui.text.TextLayoutResult {
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        checkNotNull(config[SemanticsActions.GetTextLayoutResult].action).invoke(results)
        return results.single()
    }
}
