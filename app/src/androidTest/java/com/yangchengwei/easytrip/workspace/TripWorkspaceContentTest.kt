package com.yangchengwei.easytrip.workspace

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripWorkspaceContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun notFoundShowsBackToTripsAction() {
        var action: TripWorkspaceAction? = null
        setContent(TripWorkspacePageState.NotFound, WorkspaceMapState.Loading) { action = it }
        compose.onNodeWithText("返回旅行列表").performClick()
        assertEquals(TripWorkspaceAction.Back, action)
    }

    @Test fun consentRequiredKeepsLocalContentAndControls() {
        setContent(ready(), WorkspaceMapState.ConsentRequired)
        compose.onNodeWithText("同意高德隐私政策后显示地图").assertIsDisplayed()
        compose.onNodeWithText("地点池").assertIsDisplayed()
        compose.onNodeWithText("本地点池").assertIsDisplayed()
        compose.onNodeWithText("设置").assertIsDisplayed()
        compose.onNodeWithText("返回").assertIsDisplayed()
    }

    @Test fun mapFailureShowsPersistentRetryAndKeepsContent() {
        var action: TripWorkspaceAction? = null
        setContent(ready(), WorkspaceMapState.Failed("地图加载失败"), { action = it })
        compose.onNodeWithText("地图加载失败").assertIsDisplayed()
        compose.onNodeWithText("本地点池").assertIsDisplayed()
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

    private fun setContent(
        page: TripWorkspacePageState,
        map: WorkspaceMapState,
        onAction: (TripWorkspaceAction) -> Unit = {},
    ) {
        compose.setContent {
            EasyTripTheme {
                TripWorkspaceContent(
                    pageState = page,
                    mapState = map,
                    onAction = onAction,
                    placeContent = { Text("本地点池") },
                    dayItineraryContent = { Text("单日行程") },
                    mapContent = { Text("地图就绪") },
                )
            }
        }
    }

    private fun ready() = TripWorkspacePageState.Ready(TripWorkspaceUiState(tripName = "北京").toReadyState())
}
