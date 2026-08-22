package com.yangchengwei.easytrip.place.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
