package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlacePoolFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun placeDetailAllowsCollectionNoteAndTagsOnly() {
        val place = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "detail", "trip", "poi-detail", "西湖天地", "上城区南山路", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        compose.setContent {
            EasyTripTheme {
                PlaceDetailContent(
                    place = place,
                    draft = PlaceDetailDraft("", emptySet()),
                    saving = false,
                    error = null,
                    onNoteChange = {},
                    onTagsChange = {},
                    onToggleCollection = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithText("取消收藏").assertIsDisplayed()
        compose.onNodeWithText("备注").assertIsDisplayed()
        compose.onNodeWithText("标签（逗号分隔）").assertIsDisplayed()
        compose.onNodeWithText("加入行程").assertDoesNotExist()
        compose.onNodeWithText("已加入行程").assertDoesNotExist()
    }

    @Test fun mismatchedSearchPlacesDoNotCrashRowActions() {
        val rowPlace = com.yangchengwei.easytrip.place.domain.SavedPlace(
            "row", "trip", "poi-row", "独立地点", "地址", GeoPoint(39.9, 116.4), "", emptyList(),
        )
        var editedId: String? = null
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(
                        search = PlaceSearchState(savedPlaces = emptyList()),
                        rows = listOf(SavedPlaceRowUi(rowPlace, 0, false)),
                    ),
                    showSearch = false,
                    onAction = { action -> if (action is PlacePoolAction.Edit) editedId = action.place.id },
                )
            }
        }

        compose.onNodeWithText("编辑").performClick()
        assertEquals("row", editedId)
    }

    @Test fun tagsRemainReachableAtNarrowWidthAndLargeFont() {
        val tags = (1..8).map { com.yangchengwei.easytrip.place.domain.PlaceTag("$it", "很长标签$it") }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f, 2f)) {
                EasyTripTheme {
                    PlacePoolContent(
                        state = PlacePoolUiState(tags = tags, rows = emptyList()),
                        showSearch = true,
                        onAction = {},
                    )
                }
            }
        }

        repeat(4) {
            compose.onNodeWithTag("place-pool-tags").performTouchInput { swipeLeft() }
        }
        compose.onNodeWithTag("tag-8").assertIsDisplayed()
    }

    @Test fun emptyPoolProvidesSearchAction() {
        var searchRequested = false
        compose.setContent {
            EasyTripTheme {
                PlacePoolContent(
                    state = PlacePoolUiState(),
                    showSearch = false,
                    onAction = {},
                    onSearch = { searchRequested = true },
                )
            }
        }

        compose.onNodeWithText("还没有收藏地点").assertIsDisplayed()
        compose.onNodeWithText("搜索地点").performClick()
        assertEquals(true, searchRequested)
    }

    @Test fun savedSearchResultWithNoUsageRemovesImmediately() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("取消收藏旅行", 1)) }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place-${System.nanoTime()}"))
        val candidate = PlaceCandidate("poi-remove", "颐和园", "北京市海淀区", GeoPoint(39.999, 116.275), "010")
        runBlocking { repository.save(tripId, candidate) }
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(candidate)
        }
        val model = PlacePoolViewModel(tripId, repository, source)
        compose.setContent { PlacePoolSheet(model) }
        compose.onNodeWithText("搜索地点").performTextInput("颐和园")
        compose.waitUntil(5_000) {
            model.state.value.search.results.isNotEmpty() && "poi-remove" in model.state.value.savedPoiIds
        }

        compose.onNodeWithTag("save-result-poi-remove").performClick()

        compose.waitUntil(5_000) { "poi-remove" !in model.state.value.savedPoiIds }
        compose.onNodeWithText("收藏").assertExists()
        compose.onNodeWithText("确认取消收藏").assertDoesNotExist()
    }

    @Test fun referencedSavedResultRequiresConfirmationAndDismissPreservesData() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("引用收藏旅行", 1)) }
        val dayId = runBlocking { app.tripRepository.observeTrip(tripId).first()!!.days.single().id }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place-${System.nanoTime()}"))
        val candidate = PlaceCandidate("poi-used", "天坛", "北京市东城区", GeoPoint(39.883, 116.407), "010")
        val placeId = runBlocking {
            (repository.save(tripId, candidate) as com.yangchengwei.easytrip.place.domain.SavePlaceResult.Saved).id
        }
        runBlocking { app.itineraryRepository.addItem(dayId, placeId, 0) }
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(candidate)
        }
        val model = PlacePoolViewModel(tripId, repository, source)
        compose.setContent { PlacePoolSheet(model) }
        compose.onNodeWithText("搜索地点").performTextInput("天坛")
        compose.waitUntil(5_000) {
            model.state.value.search.results.isNotEmpty() && "poi-used" in model.state.value.savedPoiIds
        }

        compose.onNodeWithTag("save-result-poi-used").performClick()
        compose.onNodeWithText("将同时删除 1 次行程安排及受影响路线。").assertExists()
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(5_000) { model.state.value.pendingCollectionRemoval == null }
        assertEquals(1, runBlocking { repository.usageCount(placeId) })
        assertEquals(setOf("poi-used"), runBlocking { repository.observeSavedPoiIds(tripId).first() })

        compose.onNodeWithTag("save-result-poi-used").performClick()
        compose.onNodeWithText("确认取消收藏").performClick()
        compose.waitUntil(5_000) { "poi-used" !in model.state.value.savedPoiIds }
        assertEquals(0, runBlocking { app.database.itineraryEditingDao().items(dayId).size })
    }

    @Test fun searchSaveEditAndFilterThroughPlacePool() {
        val app = compose.activity.application as com.yangchengwei.easytrip.EasyTripApplication
        val tripId = runBlocking { app.tripRepository.createTrip(CreateTrip("地点旅行", 1)) }
        val repository = RoomSavedPlaceRepository(app.database, idFactory = sequence("place", "tag"))
        val source = object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = listOf(
                PlaceCandidate("poi-1", "故宫博物院", "北京市东城区", GeoPoint(39.916, 116.397), "010"),
                PlaceCandidate("poi-2", "国家博物馆", "北京市东城区", GeoPoint(39.905, 116.401), "010"),
            )
        }
        val model = PlacePoolViewModel(tripId, repository, source)
        compose.setContent { PlacePoolSheet(model) }
        compose.onNodeWithText("搜索地点").performTextInput("故宫")
        compose.waitUntil(5_000) { model.state.value.search.results.isNotEmpty() }
        compose.onNodeWithText("故宫博物院").assertExists()
        compose.onAllNodesWithText("收藏")[0].performClick()
        compose.waitUntil(5_000) { model.state.value.search.savedPlaces.size == 1 }
        compose.onAllNodesWithText("收藏")[0].performClick()
        compose.waitUntil(5_000) { model.state.value.savedPoiIds.size == 2 }
        compose.onAllNodesWithText("编辑")[0].performClick()
        compose.onNodeWithText("备注").performTextInput("必去")
        compose.onNodeWithText("标签（逗号分隔）").performTextInput("文化, 宫殿")
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(5_000) { model.state.value.tags.size == 2 }
        val cultureTagId = model.state.value.tags.single { it.name == "文化" }.id
        compose.onNodeWithTag("tag-$cultureTagId").performClick()
        compose.waitUntil(5_000) { model.state.value.search.savedPlaces.size == 1 }
        assertEquals("必去", model.state.value.search.savedPlaces.single().note)
        assertEquals(2, compose.onAllNodesWithText("已收藏").fetchSemanticsNodes().size)
    }

    private fun sequence(vararg prefixes: String): () -> String {
        var index = 0
        return { "${prefixes[index.coerceAtMost(prefixes.lastIndex)]}-${index++}" }
    }
}
