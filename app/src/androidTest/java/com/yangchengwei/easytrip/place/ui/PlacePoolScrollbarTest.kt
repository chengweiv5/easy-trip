package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.domain.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlacePoolScrollbarTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun thumbIsHiddenWhenAllPlacesFit() {
        compose.setContent { Content(places(2), Modifier.height(600.dp)) }

        compose.onNodeWithTag("place-pool-scrollbar-thumb").assertDoesNotExist()
    }

    @Test fun thumbTracksListScrollingAndCannotBeDraggedDirectly() {
        lateinit var state: LazyListState
        compose.setContent {
            state = androidx.compose.foundation.lazy.rememberLazyListState()
            Content(places(40), Modifier.height(240.dp), state)
        }
        compose.waitForIdle()
        val initialTop = compose.onNodeWithTag("place-pool-scrollbar-thumb")
            .assertExists()
            .getUnclippedBoundsInRoot().top

        compose.onNodeWithTag("saved-place-place-0").performTouchInput { swipeUp() }
        compose.waitUntil(5_000) { state.firstVisibleItemIndex > 0 }
        val movedTop = compose.onNodeWithTag("place-pool-scrollbar-thumb")
            .getUnclippedBoundsInRoot().top
        assertTrue(movedTop > initialTop)

        val indexBeforeThumbSwipe = state.firstVisibleItemIndex
        compose.onNodeWithTag("place-pool-scrollbar-thumb").performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertEquals(indexBeforeThumbSwipe, state.firstVisibleItemIndex)
    }

    @androidx.compose.runtime.Composable
    private fun Content(
        places: List<SavedPlace>,
        modifier: Modifier,
        state: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    ) {
        SavedPlacesContent(
            places = places,
            tags = emptyList(),
            selectedTagIds = emptySet(),
            onToggleTag = {},
            onEdit = {},
            onDelete = {},
            modifier = modifier,
            listState = state,
        )
    }

    private fun places(count: Int) = List(count) { index ->
        SavedPlace(
            id = "place-$index",
            tripId = "trip",
            amapPoiId = "poi-$index",
            name = "地点 $index",
            address = "地址 $index",
            point = GeoPoint(39.9 + index * 0.001, 116.4),
            note = "",
            tags = emptyList(),
        )
    }
}
