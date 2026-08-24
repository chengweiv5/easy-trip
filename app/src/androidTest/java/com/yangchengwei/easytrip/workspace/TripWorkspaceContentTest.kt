package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripWorkspaceContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun notFoundShowsBackToTripsAction() {
        var action: TripWorkspaceAction? = null
        setContent(TripWorkspacePageState.NotFound, WorkspaceMapState.Loading, onAction = { action = it })
        compose.onNodeWithText("返回旅行列表").performClick()
        assertEquals(TripWorkspaceAction.Back, action)
    }

    @Test fun consentRequiredKeepsLocalContentAndControls() {
        setContent(ready(), WorkspaceMapState.ConsentRequired)
        compose.onNodeWithText("同意高德隐私政策后显示地图").assertIsDisplayed()
        compose.onNodeWithText("地点池").assertIsDisplayed()
        compose.onNodeWithText("还没有收藏地点").assertExists()
        compose.onNodeWithText("设置").assertIsDisplayed()
        compose.onNodeWithText("返回").assertIsDisplayed()
    }

    @Test fun mapFailureShowsPersistentRetryAndKeepsContent() {
        var action: TripWorkspaceAction? = null
        setContent(ready(), WorkspaceMapState.Failed("地图加载失败"), onAction = { action = it })
        compose.onNodeWithText("地图加载失败").assertIsDisplayed()
        compose.onNodeWithText("还没有收藏地点").assertExists()
        compose.onNodeWithText("重试地图").performClick()
        assertEquals(TripWorkspaceAction.Retry, action)
    }

    @Test fun readyKeepsSearchSettingsBackAndItineraryActions() {
        val actions = mutableListOf<TripWorkspaceAction>()
        setContent(ready(), WorkspaceMapState.Ready, actions::add)
        compose.waitForIdle()
        actions.clear()
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithContentDescription("搜索地点").performClick()
        compose.onNodeWithText("地点池").assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        compose.onNodeWithText("行程").assertIsNotSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        assertEquals(
            listOf(
                TripWorkspaceAction.Back,
                TripWorkspaceAction.OpenSettings,
                TripWorkspaceAction.OpenSearch,
                TripWorkspaceAction.SelectSection(WorkspaceSection.PLACE_POOL),
                TripWorkspaceAction.SelectSection(WorkspaceSection.ITINERARY),
            ),
            actions,
        )
    }

    @Test fun halfSheetMatchesDesignProportionAndStaysBelowTopSafeArea() {
        setContent(ready(), WorkspaceMapState.Ready)
        compose.waitForIdle()

        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        val topBar = compose.onNodeWithTag("workspace-top-bar").getUnclippedBoundsInRoot()

        assertTrue("root=$root sheet=$sheet", sheet.height in 395.dp..397.dp)
        assertTrue("root=$root sheet=$sheet", sheet.bottom <= root.bottom)
        assertTrue("root=$root topBar=$topBar", topBar.top >= root.top + 24.dp)
    }

    @Test fun searchReturnSheetIsRaisedAndHighlightsRecentCollections() {
        val places = listOf(
            com.yangchengwei.easytrip.place.domain.SavedPlace("new", "trip", "poi-new", "新收藏", "地址", com.yangchengwei.easytrip.core.model.GeoPoint(39.9, 116.4), "", emptyList()),
            com.yangchengwei.easytrip.place.domain.SavedPlace("old", "trip", "poi-old", "原收藏", "地址", com.yangchengwei.easytrip.core.model.GeoPoint(39.91, 116.4), "", emptyList()),
        )
        setContent(
            ready(),
            WorkspaceMapState.Ready,
            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(
                rows = places.map { com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(it, 0, false) },
            ),
            searchReturn = WorkspaceSearchReturn(setOf("poi-new")),
        )
        compose.waitForIdle()

        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("workspace-sheet").getUnclippedBoundsInRoot()
        assert(root.bottom - sheet.top >= 396.dp)
        compose.onNodeWithText("刚刚收藏 · 待安排行程").assertExists()
        compose.onNodeWithTag("workspace-place-list").performScrollToNode(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "saved-place-old"),
        )
        compose.onNodeWithText("原收藏").assertExists()
    }

    @Test fun placePoolListScrollsAndKeepsSecondCardAboveBottomInset() {
        val places = (1..8).map { index ->
            com.yangchengwei.easytrip.place.domain.SavedPlace("$index", "trip", "poi-$index", "地点 $index", "地址 $index", com.yangchengwei.easytrip.core.model.GeoPoint(39.9 + index / 1000.0, 116.4), "", emptyList())
        }
        setContent(
            ready(),
            WorkspaceMapState.Ready,
            placeState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(rows = places.map { com.yangchengwei.easytrip.place.ui.SavedPlaceRowUi(it, 0, false) }),
        )
        compose.waitForIdle()

        compose.onNodeWithTag("workspace-place-list").performScrollToNode(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "saved-place-5"),
        )
        compose.waitForIdle()
        val root = compose.onNodeWithTag("workspace-root").getUnclippedBoundsInRoot()
        val visibleCard = compose.onNodeWithTag("saved-place-5").getUnclippedBoundsInRoot()
        assert(visibleCard.bottom <= root.bottom - 24.dp)
    }

    private fun setContent(
        page: TripWorkspacePageState,
        map: WorkspaceMapState,
        onAction: (TripWorkspaceAction) -> Unit = {},
        placeState: com.yangchengwei.easytrip.place.ui.PlacePoolUiState = com.yangchengwei.easytrip.place.ui.PlacePoolUiState(),
        searchReturn: WorkspaceSearchReturn? = null,
    ) {
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = page,
                    mapState = map,
                    onAction = onAction,
                    placeState = placeState,
                    onPlaceAction = {},
                    itineraryState = com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState(),
                    onItineraryAction = {},
                    mapContent = { Text("地图就绪") },
                    modifier = Modifier.fillMaxSize().testTag("workspace-root"),
                    searchReturn = searchReturn,
                )
            }
        }
    }

    private fun ready() = TripWorkspacePageState.Ready(TripWorkspaceUiState(tripName = "北京").toReadyState())
}
