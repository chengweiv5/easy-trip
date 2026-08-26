package com.yangchengwei.easytrip.itinerary.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItineraryTimelineContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun compactPlaceRowShowsArrivalNameStayAndAddress() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceRow(
                    item = itineraryItem(
                        id = "i1",
                        name = "灵隐寺",
                        address = "浙江省杭州市西湖区法云弄1号",
                        arrivalTime = "09:30",
                        stayMinutes = 120,
                    ),
                    displayOrder = 1,
                )
            }
        }

        compose.onNodeWithText("09:30").assertIsDisplayed()
        compose.onNodeWithText("灵隐寺").assertIsDisplayed()
        compose.onNodeWithText("停留 120 分钟").assertIsDisplayed()
        compose.onNodeWithText("浙江省杭州市西湖区法云弄1号").assertIsDisplayed()
    }

    @Test
    fun longTextDoesNotPushTrailingActionOutsideRow() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceContent(
                    item = itineraryItem(
                        id = "long",
                        name = "一段很长很长很长很长很长很长的地点名称",
                        address = "一段很长很长很长很长很长很长很长很长的地点地址",
                        arrivalTime = "09:30",
                        stayMinutes = 120,
                    ),
                    displayOrder = 1,
                    modifier = Modifier.width(320.dp),
                    trailingAction = { Box(Modifier.size(40.dp).testTag("trailing-long")) },
                )
            }
        }

        val row = compose.onNodeWithTag("item-long").getUnclippedBoundsInRoot()
        val trailing = compose.onNodeWithTag("trailing-long").getUnclippedBoundsInRoot()
        assertTrue("row=$row trailing=$trailing", trailing.right <= row.right)
    }

    @Test
    fun editableAndReadOnlyRowsSharePlaceContentGeometry() {
        compose.setContent {
            EasyTripTheme {
                Row {
                    ItineraryPlaceRow(
                        item = itineraryItem("read-only", "灵隐寺", "法云弄1号", "09:30", 120),
                        displayOrder = 1,
                        modifier = Modifier.width(180.dp),
                    )
                    ItineraryPlaceContent(
                        item = itineraryItem("editable", "灵隐寺", "法云弄1号", "09:30", 120),
                        displayOrder = 1,
                        modifier = Modifier.width(180.dp),
                    )
                }
            }
        }

        val readOnly = compose.onNodeWithTag("item-read-only").getUnclippedBoundsInRoot()
        val editable = compose.onNodeWithTag("item-editable").getUnclippedBoundsInRoot()
        assertEquals(readOnly.right - readOnly.left, editable.right - editable.left)
        assertEquals(readOnly.bottom - readOnly.top, editable.bottom - editable.top)
    }

    @Test
    fun itemShowsOnlyHandleAndMoreAsPermanentActions() {
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = {},
                )
            }
        }

        compose.onNodeWithTag("drag-handle-i1", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("more-i1", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag("timing-i1", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("move-i1", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("delete-i1", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun itemMenuDispatchesTimingMoveAndDeleteForCurrentItem() {
        val actions = mutableListOf<ItineraryItemMenuAction>()
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = actions::add,
                )
            }
        }

        listOf(
            "menu-timing-i1" to ItineraryItemMenuAction.EditTiming,
            "menu-move-i1" to ItineraryItemMenuAction.MoveToOtherDay,
            "menu-delete-i1" to ItineraryItemMenuAction.Delete,
        ).forEach { (tag, expected) ->
            compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
            compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
            compose.runOnIdle { assertEquals(expected, actions.last()) }
        }
    }

    @Test
    fun dismissingMenuDoesNotDispatchBusinessAction() {
        val actions = mutableListOf<ItineraryItemMenuAction>()
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = actions::add,
                )
            }
        }

        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onNodeWithText("编辑时间与停留时长").assertIsDisplayed()
        compose.onNodeWithTag("more-i1", useUnmergedTree = true).performClick()
        compose.onAllNodesWithText("编辑时间与停留时长").assertCountEquals(0)
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
    }

    @Test
    fun moreMenuDescriptionContainsPlaceName() {
        compose.setContent {
            EasyTripTheme {
                ItineraryItemRow(
                    item = itineraryItem("i1", "灵隐寺", "法云弄1号", "09:30", 120),
                    index = 0,
                    count = 1,
                    onPreview = {},
                    onCommit = {},
                    onMenuAction = {},
                )
            }
        }

        compose.onNodeWithTag("more-i1", useUnmergedTree = true)
            .assertContentDescriptionEquals("灵隐寺，更多行程项操作")
    }

    @Test
    fun normalPlaceRowHasZeroElevationAndNoPermanentDeleteAction() {
        compose.setContent {
            EasyTripTheme {
                ItineraryPlaceRow(
                    item = itineraryItem("plain", "灵隐寺", "法云弄1号", "09:30", 120),
                    displayOrder = 1,
                )
            }
        }

        compose.onNodeWithTag("item-plain").assertIsDisplayed()
        compose.onAllNodesWithTag("delete-plain").assertCountEquals(0)
        compose.onAllNodesWithText("删除").assertCountEquals(0)
    }

    private fun itineraryItem(
        id: String,
        name: String,
        address: String,
        arrivalTime: String,
        stayMinutes: Int,
    ) = ItineraryItemUi(id, name, address, LocalTime.parse(arrivalTime), stayMinutes)
}
