package com.yangchengwei.easytrip.assistant

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/** Synthetic input and in-memory repository; never uses real model credentials or user trips. */
class AssistantDirectSaveUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun selectionFailureRetryAndInlineEditsStayOnResults() = exercise(false)
    @Test fun narrowLargeTextKeepsActionsVisibleAndEditsScrollable() = exercise(true)

    @Test fun savingDisablesSourceRetryAndStopControls() {
        val context = compose.activity.applicationContext
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val store = ViewModelStore()
        val model = PlaceAssistantViewModel("test-trip", AssistantConfigStore(context), object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("Must not write")
        }, RoomSavedPlaceRepository(db))
        store.put("assistant", model)
        var processing by mutableStateOf(true)
        try {
            compose.setContent {
                EasyTripTheme {
                    val state = PlaceAssistantState(configured = true, consent = true, mapReady = true,
                        busy = processing, saving = true,
                        items = listOf(IntakeItem("one", "A", PlaceIntent("甲", "杭州", "甲"),
                            status = IntakeStatus.QUERYING)),
                        sourceProgress = listOf(SourceProgress("text", SourceStage.FAILED)))
                    PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "测试旅行", {}, {}, {}, {}))
                }
            }
            compose.onNodeWithTag("assistant-cancel").performScrollTo().assertIsNotEnabled()
            compose.runOnIdle { processing = false }
            compose.onNodeWithTag("assistant-retry-images").performScrollTo().assertIsNotEnabled()
        } finally {
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)); store.clear() }
            db.close()
        }
    }

    private fun exercise(small: Boolean) {
        val context = compose.activity.applicationContext
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val store = ViewModelStore()
        val config = AssistantConfigStore(context)
        val gate = CompletableDeferred<Unit>()
        val requests = CopyOnWriteArrayList<ImportConfirmation>()
        val queries = CopyOnWriteArrayList<String>()
        val model = PlaceAssistantViewModel("test-trip", config, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                requests += confirmation
                if (requests.size == 1) { gate.await(); error("first attempt fails before writing") }
                return ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest,
                    confirmation.items.map { ImportItemResult(it.itemId, it.poi.poiId, it.itemId, true) })
            }
        }, RoomSavedPlaceRepository(db), PlaceIntentParser { _, _ ->
            listOf("甲", "乙", "失败").map { PlaceIntent(it, "杭州", it,
                sources = listOf(IntakeSource.Text("攻略里的$it"))) }
        })
        store.put("assistant", model)
        model.controller.updateSource(object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                queries += keyword
                if (keyword == "失败") error("retry needs correction")
                return listOf(PlaceCandidate(keyword, keyword, "杭州市测试地址", GeoPoint(30.2, 120.1), null, "杭州"))
            }
        })
        try {
            compose.setContent {
                val state by model.controller.state.collectAsState()
                val focus by model.focus.collectAsState()
                val density = LocalDensity.current.density
                EasyTripTheme {
                    CompositionLocalProvider(LocalDensity provides Density(density, if (small) 2f else 1f)) {
                        Box(Modifier.requiredSize(if (small) 320.dp else 390.dp, 568.dp).padding(horizontal = 16.dp)) {
                            PlaceAssistantPanel(AssistantWorkspace(model, state, true, focus, "测试旅行", {}, {}, {}, {}))
                        }
                    }
                }
            }
            compose.runOnIdle {
                model.controller.environment(true, true, true)
                model.controller.editInput("甲、乙、失败")
                model.controller.submit()
            }
            compose.waitUntil { !model.controller.state.value.busy && model.controller.state.value.items.size == 3 }
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithTag("assistant-marker-A").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(model.controller.state.value.items.first().id, model.focus.value)
                assertTrue(model.controller.state.value.selected.isEmpty())
                assertTrue(requests.isEmpty())
            }
            compose.onNodeWithTag("assistant-category-A").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-category-A-food").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-sources-A").performScrollTo().performClick()
            compose.onNodeWithText("原文：攻略里的甲").performScrollTo().assertIsDisplayed()
            assertTrue(requests.isEmpty())
            compose.onNodeWithTag("assistant-select-A").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-select-all").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
            compose.onNodeWithTag("assistant-select-C").assertDoesNotExist()

            compose.onNodeWithTag("assistant-edit-C").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-edit-name-C").performScrollTo().performTextReplacement("修正地点")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("assistant-edit-retry-C").performScrollTo().performClick()
            compose.waitUntil { model.controller.state.value.eligible.size == 3 }
            assertEquals(1, queries.count { it == "甲" })
            assertEquals(1, queries.count { it == "乙" })
            assertEquals(1, model.controller.state.value.selected.size)
            compose.onNodeWithTag("assistant-select-all").performClick().assertIsOn()
            compose.onNodeWithTag("assistant-select-all").performClick().assertIsOff()
            compose.onNodeWithTag("assistant-select-A").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().performClick()
            compose.waitUntil { requests.size == 1 }
            compose.onNodeWithTag("assistant-save").assertIsNotEnabled()
            compose.onNodeWithTag("assistant-select-all").assertIsNotEnabled()
            compose.onNodeWithTag("assistant-select-A").assertIsNotEnabled()
            compose.onNodeWithTag("assistant-review").assertDoesNotExist()
            compose.onNodeWithTag("assistant-confirm").assertDoesNotExist()
            gate.complete(Unit)
            compose.waitUntil { model.controller.state.value.error != null }
            compose.onNodeWithTag("assistant-error").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil { model.controller.state.value.receipt != null }
            assertEquals(2, requests.size)
            assertEquals(requests[0].operationId, requests[1].operationId)
            assertEquals(PlaceCategory.FOOD, requests.last().items.single().category)
            compose.onNodeWithTag("assistant-item-B").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("assistant-select-A").assertDoesNotExist()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsNotEnabled()
            assertEquals(3, model.controller.state.value.items.size)
        } finally {
            gate.complete(Unit)
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)); store.clear() }
            db.close()
        }
    }
}
