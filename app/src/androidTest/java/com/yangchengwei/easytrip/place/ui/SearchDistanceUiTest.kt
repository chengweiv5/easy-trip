package com.yangchengwei.easytrip.place.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchDistanceUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val candidates = listOf(
        PlaceCandidate("near", "很长的地点名称用于确认距离不会与收藏按钮发生重叠的景点", "杭州市西湖区一条很长的地址，保留完整的两行空间", GeoPoint(30.005, 120.0), "0571"),
        PlaceCandidate("far", "另一个地点", "杭州市上城区", GeoPoint(30.01, 120.0), "0571"),
        PlaceCandidate("unknown", "没有坐标的地点", "地址待确认", null, null),
    )

    @Test fun distancesUpdateAndDisappearWithoutAffectingRowOrBookmarkActions() {
        val state = mutableStateOf(PlaceSearchUiState(
            search = PlaceSearchState("景点", candidates, phase = PlaceSearchPhase.Results),
        ))
        val actions = mutableListOf<PlaceSearchAction>()
        compose.setContent { EasyTripTheme {
            PlaceSearchContent(state.value, actions::add, autoFocusSearch = false)
        } }
        compose.onNodeWithTag("place-search-distance-near", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(currentLocation = GeoPoint(30.0, 120.0)) }
        compose.onNodeWithTag("place-search-distance-near", useUnmergedTree = true).assertTextEquals("距我 560 米 · 直线").assertIsDisplayed()
        compose.onNodeWithTag("place-search-distance-far", useUnmergedTree = true).assertTextEquals("距我 1.1 公里 · 直线")
        compose.onNodeWithTag("place-search-distance-unknown", useUnmergedTree = true).assertDoesNotExist()
        val distance = compose.onNodeWithTag("place-search-distance-near", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val bookmark = compose.onNodeWithTag("place-search-bookmark-touch-near", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(distance.right <= bookmark.left)
        compose.onNodeWithContentDescription("收藏${candidates.first().name}").performClick()
        compose.onNodeWithContentDescription("查看${candidates.first().name}详情").performClick()
        assertEquals(listOf(PlaceSearchAction.ToggleCollection("near"), PlaceSearchAction.OpenDetail("near")), actions)
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(compose.activity.getExternalFilesDir(null), "search-distances.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.runOnIdle { state.value = state.value.copy(currentLocation = GeoPoint(30.01, 120.0)) }
        compose.onNodeWithTag("place-search-distance-far", useUnmergedTree = true).assertTextEquals("距我 100 米内 · 直线")
        compose.runOnIdle { state.value = state.value.copy(currentLocation = null) }
        compose.onNodeWithTag("place-search-distance-near", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("place-search-distance-far", useUnmergedTree = true).assertDoesNotExist()
    }
}
