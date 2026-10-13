package com.yangchengwei.easytrip.assistant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.amap.TestConsentGate
import com.yangchengwei.easytrip.assistant.data.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class TripDraftRecoveryUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun unsubmittedRecoveryCanEditMetadataAndCreateWithoutAStaleMapSession() {
        val db = Room.inMemoryDatabaseBuilder(compose.activity, EasyTripDatabase::class.java).build()
        val config = AssistantConfigStore(compose.activity)
        val original = config.read()
        val stores = ViewModelStore()
        val command = TripCreationCommand.create("原名称", null, 1, TravelMode.FLEXIBLE,
            listOf(ImportItem("A", 0, PlaceCandidate("p", "雷峰塔", "杭州", GeoPoint(30.2, 120.1), null, "杭州"),
                PlaceCategory.ATTRACTION)), emptyList())
        val journal = object : PendingTripCreation {
            var pending: TripCreationCommand? = command
            override fun read() = pending
            override fun write(command: TripCreationCommand) { check(pending == null || pending == command); pending = command }
            override fun clear(command: TripCreationCommand) { check(pending == command); pending = null }
        }
        val token = TestConsentGate().run { show(); requireNotNull(decide(true)) }
        var opened: String? = null
        try {
            config.save(ProviderConfig("https://example.invalid", "test", "not-real"))
            lateinit var assistant: PlaceAssistantViewModel
            lateinit var draft: TripDraftViewModel
            compose.runOnUiThread {
                assistant = PlaceAssistantViewModel(null, config, RoomPlaceImport(db), RoomSavedPlaceRepository(db))
                draft = TripDraftViewModel(RoomTripCreation(db), journal) { error("Recovery must not call model") }
                stores.put("assistant", assistant); stores.put("draft", draft)
            }
            compose.setContent { EasyTripTheme {
                GlobalAssistantRoute(assistant, draft, null, token, {}, {}, { opened = it })
            } }
            compose.waitUntil(10_000) { draft.state.value.confirming && !draft.state.value.recovering }
            assertTrue(runBlocking { RoomTripRepository(db.tripDao()).observeTrips().first().isEmpty() })
            compose.onNodeWithText("核对结果并修改信息").performScrollTo().performClick()
            compose.waitUntil(10_000) { !draft.state.value.submissionLocked && assistant.controller.state.value.selected.size == 1 }
            compose.onNodeWithTag("trip-draft-name").performScrollTo().performTextReplacement("恢复后修改")
            // Wait for the real IME transition before scrolling; its bring-into-view request
            // otherwise races the test scroll and moves the submit button off screen.
            compose.waitUntil(5_000) {
                ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.waitUntil(5_000) {
                ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == false
            }
            compose.onNodeWithTag("trip-draft-submit").performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(10_000) { opened != null }
            val trip = runBlocking { RoomTripRepository(db.tripDao()).observeTrips().first().single() }
            assertEquals("恢复后修改", trip.name)
            assertNotEquals(command.tripId, trip.id)
            assertEquals(1, trip.placeCount)
            assertNull(journal.pending)
        } finally {
            compose.runOnUiThread { stores.clear() }
            if (original == null) config.clear() else config.save(original)
            db.close()
        }
    }
}
