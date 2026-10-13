package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.assistant.data.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.ui.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real Compose/Room/MapView, deterministic model and POI boundary; never production trips or provider calls. */
class GlobalAssistantUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun emptyHomeToUndatedTripRequiresFinalConfirmation() = exercise(false)
    @Test fun narrowDoubleFontKeepsCreationActionsReachable() = exercise(true)
    @Test fun cleanupFailureCanReopenWithoutDuplicateCreation() = exercise(false, failFirstClear = true)
    @Test fun dayInputAndValidationPreserveSelection() = exercise(false, editForm = true)
    @Test fun recreatedCompositionCanSubmitWithoutRemountingCandidateMap() = exercise(false, recreate = true)
    @Test fun hundredPlacesCreateOneUndatedTripOnlyAfterFinalConfirmation() = exercise(false, placeCount = 100)
    @Test fun narrowDoubleFontHundredPlacesCanReachFinalCreation() = exercise(true, placeCount = 100)
    private fun exercise(small: Boolean, failFirstClear: Boolean = false, editForm: Boolean = false,
        recreate: Boolean = false, placeCount: Int = 1) {
        val context = compose.activity
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val places = RoomSavedPlaceRepository(db)
        val config = AssistantConfigStore(context)
        val original = config.read()
        val stores = ViewModelStore()
        var opened by mutableStateOf(false)
        var created by mutableStateOf<String?>(null)
        var composition by mutableIntStateOf(0)
        var searches = 0
        var clearFailures = if (failFirstClear) 1 else 0
        val journal = object : PendingTripCreation {
            var pending: TripCreationCommand? = null
            override fun read() = pending
            override fun write(command: TripCreationCommand) { check(pending == null || pending == command); pending = command }
            override fun clear(command: TripCreationCommand) {
                if (clearFailures-- > 0) error("Injected cleanup failure")
                check(pending == command); pending = null
            }
        }
        try {
            config.save(ProviderConfig("https://example.invalid", "test-model", "test-only-not-real"))
            runBlocking { AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) } }
            val token = TestConsentGate().run { show(); requireNotNull(decide(true)) }
            val source = object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    searches++
                    return listOf(PlaceCandidate(keyword, keyword.removePrefix("杭州"), "杭州市测试地址",
                        GeoPoint(30.230, 120.149), null, "杭州"))
                }
            }
            lateinit var assistant: PlaceAssistantViewModel
            lateinit var draft: TripDraftViewModel
            compose.runOnUiThread {
                assistant = PlaceAssistantViewModel(null, config, RoomPlaceImport(db), places,
                    PlaceIntentParser { _, _ -> (1..placeCount).map {
                        val name = if (placeCount == 1) "雷峰塔" else "公园$it"
                        PlaceIntent(name, "杭州", "杭州$name")
                    } },
                    target = AssistantTarget.NewTripDraft)
                draft = TripDraftViewModel(RoomTripCreation(db), journal) { TripDraftMetadata(TripDateSuggestion(null)) }
                stores.put("assistant", assistant); stores.put("draft", draft)
            }
            compose.setContent {
                EasyTripTheme {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,
                        if (small) 2f else LocalDensity.current.fontScale)) {
                        Box(if (small) Modifier.requiredSize(320.dp, 568.dp) else Modifier) {
                            if (!opened) TripListContent(TripListUiState(page = TripListPageState.Empty), {},
                                onAssistantCreate = { opened = true })
                            else key(composition) {
                                GlobalAssistantRoute(assistant, draft, source, token, {}, { opened = false }, { created = it })
                            }
                        }
                    }
                }
            }
            compose.onNodeWithTag("assistant-create-trip").assertIsDisplayed()
            compose.onNodeWithTag("create-trip").assertIsDisplayed()
            screenshot("home", small)
            compose.onNodeWithTag("assistant-create-trip").performClick()
            compose.waitUntil(10_000) { !draft.state.value.recovering && assistant.controller.state.value.configured }
            compose.onNodeWithTag("assistant-input").performScrollTo().performTextInput(
                if (placeCount == 1) "杭州雷峰塔" else (1..placeCount).joinToString("、") { "杭州公园$it" })
            compose.onNodeWithTag("assistant-submit").performScrollTo().performClick()
            compose.waitUntil(10_000) { assistant.controller.state.value.awaitingCityConfirmation }
            assertEquals(0, searches)
            assertTrue(runBlocking { trips.observeTrips().first().isEmpty() })
            compose.onNodeWithTag("assistant-confirm-cities").performScrollTo().performClick()
            compose.waitUntil(100_000) { assistant.controller.state.value.eligible.size == placeCount &&
                assistant.controller.state.value.mapReady }
            compose.onNodeWithTag("assistant-select-all").performClick()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().performClick()
            compose.waitUntil(5_000) { draft.state.value.confirming }
            assertTrue(runBlocking { trips.observeTrips().first().isEmpty() })
            if (recreate) {
                compose.runOnUiThread { composition++ }
                compose.waitForIdle()
                assertFalse(assistant.controller.state.value.mapReady)
                assertTrue(draft.state.value.confirming)
            }
            if (editForm) {
                compose.onNodeWithTag("trip-draft-name").performScrollTo().performTextReplacement(" ")
                compose.onNodeWithTag("trip-draft-submit").performScrollTo().assertIsNotEnabled()
                compose.onNodeWithTag("trip-draft-name").performScrollTo().performTextReplacement("手工旅行名称")
                compose.runOnUiThread { draft.editDates(java.time.LocalDate.of(2026, 10, 24)) }
                compose.onNodeWithTag("trip-draft-days").performScrollTo().performClick()
                compose.onNodeWithTag("trip-days-input").performTextReplacement("31")
                compose.onNodeWithTag("trip-days-confirm").assertIsNotEnabled()
                compose.onNodeWithTag("trip-days-input").performTextReplacement("3")
                compose.onNodeWithTag("trip-days-end-date").assertTextEquals("结束日期：2026-10-26")
                compose.onNodeWithTag("trip-days-confirm").performClick()
                compose.onNodeWithTag("trip-draft-clear-date").performScrollTo().performClick()
                compose.runOnIdle { assertEquals(3, draft.state.value.dayCount) }
            }
            compose.onNodeWithTag("trip-draft-submit").performScrollTo().assertIsDisplayed()
            screenshot("confirm", small)
            compose.onNodeWithTag("trip-draft-submit").performClick()
            if (failFirstClear) {
                compose.waitUntil(10_000) { compose.onAllNodesWithTag("trip-draft-reopen").fetchSemanticsNodes().isNotEmpty() }
                assertEquals(1, runBlocking { trips.observeTrips().first().size })
                compose.onNodeWithTag("trip-draft-reopen").performClick()
            }
            compose.waitUntil(10_000) { created != null }
            val result = runBlocking { trips.observeTrips().first().single() }
            assertEquals(created, result.id)
            assertNull(result.startDate); assertEquals(if (editForm) 3 else 1, result.dayCount); assertEquals(placeCount, result.placeCount)
            assertEquals(0, result.scheduledDayCount); assertNull(journal.pending)
        } finally {
            compose.runOnUiThread { stores.clear() }
            if (original != null) config.save(original) else config.clear()
            db.close()
        }
    }
    private fun screenshot(stage: String, small: Boolean) {
        val folder = File(compose.activity.getExternalFilesDir(null), "v34-ui").apply { mkdirs() }
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let {
            File(folder, "$stage-${if (small) "large" else "normal"}.png").outputStream().use { output -> it.compress(Bitmap.CompressFormat.PNG, 100, output) }
            it.recycle()
        }
    }
}
