package com.yangchengwei.easytrip.trip.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripListContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loadingStateRenders() {
        setContent(TripListPageState.Loading)
        compose.onNodeWithText("正在加载旅行").assertIsDisplayed()
    }

    @Test fun emptyListShowsSingleCreateAction() {
        setContent(TripListPageState.Empty)

        compose.onNodeWithText("还没有旅行计划").assertIsDisplayed()
        compose.onAllNodesWithTag("create-trip").assertCountEquals(1)
    }

    @Test fun emptyStateRendersAndOpensCreate() {
        var action: TripListAction? = null
        setContent(TripListPageState.Empty) { action = it }
        compose.onNodeWithText("还没有旅行计划").assertIsDisplayed()
        compose.onNodeWithTag("create-trip").performClick()
        assertEquals(TripListAction.CreateTrip, action)
    }

    @Test fun titleUsesDisplaySizeAndTripCardsExposeActions() {
        setContent(
            TripListPageState.Content(
                listOf(
                    TripCardUiModel("trip-1", "京都", "3 天", null, "灵活"),
                    TripCardUiModel("trip-2", "东京", "2 天", null, "自驾"),
                ),
            ),
        )

        compose.onNodeWithTag("trip-list-title").assertIsDisplayed()
        compose.onNodeWithTag("primary-trip-trip-1").assertIsDisplayed()
        compose.onNodeWithText("其他旅行").assertIsDisplayed()
        compose.onNodeWithTag("trip-settings-trip-2").assertIsDisplayed()
        compose.onNodeWithTag("trip-delete-trip-2").assertIsDisplayed()
    }

    @Test fun contentRenders_andActionsEachFireExactlyOnceWithoutParentLeak() {
        val actions = mutableListOf<TripListAction>()
        setContent(
            TripListPageState.Content(
                listOf(TripCardUiModel("trip-1", "京都", "3 天", "2026年10月1日", "灵活")),
            ),
            actions::add,
        )

        compose.onNodeWithText("京都").assertIsDisplayed()
        compose.onNodeWithText("3 天").assertIsDisplayed()
        compose.onNodeWithText("2026年10月1日").assertIsDisplayed()

        compose.onNodeWithTag("trip-settings-trip-1").performClick()
        assertEquals(listOf(TripListAction.OpenSettings("trip-1")), actions)

        compose.onNodeWithTag("trip-delete-trip-1").performClick()
        assertEquals(
            listOf(TripListAction.OpenSettings("trip-1"), TripListAction.RequestDelete("trip-1")),
            actions,
        )

        compose.onNodeWithTag("trip-trip-1").performClick()
        assertEquals(
            listOf(
                TripListAction.OpenSettings("trip-1"),
                TripListAction.RequestDelete("trip-1"),
                TripListAction.OpenTrip("trip-1"),
            ),
            actions,
        )
    }

    @Test fun errorState_retries() {
        var action: TripListAction? = null
        setContent(TripListPageState.Error("无法加载旅行")) { action = it }
        compose.onNodeWithText("重试").performClick()
        assertEquals(TripListAction.Retry, action)
    }

    @Test fun tripCard_hasReadableAccessibilityLabels() {
        setContent(
            TripListPageState.Content(
                listOf(TripCardUiModel("trip-1", "京都", "3 天", null, "自驾")),
            ),
        )
        compose.onNodeWithContentDescription("打开旅行 京都").assertIsDisplayed()
        compose.onNodeWithContentDescription("设置 京都").assertIsDisplayed()
        compose.onNodeWithContentDescription("删除 京都").assertIsDisplayed()
    }

    private fun setContent(page: TripListPageState, onAction: (TripListAction) -> Unit = {}) {
        compose.setContent {
            EasyTripTheme { TripListContent(TripListUiState(page = page), onAction) }
        }
    }
}
