package com.yangchengwei.easytrip.expense

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.expense.data.RoomExpenseRepository
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.itinerary.domain.ItineraryRepository
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModel
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.permission.InMemoryLocationPermissionRequestStore
import com.yangchengwei.easytrip.permission.LocationPermissionCoordinator
import com.yangchengwei.easytrip.permission.LocationPermissionSnapshot
import com.yangchengwei.easytrip.workspace.TripWorkspaceViewModel
import com.yangchengwei.easytrip.workspace.TripWorkspaceRoute
import com.yangchengwei.easytrip.workspace.WorkspaceOverlay
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalTime
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class InlineExpenseEditorTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java,
    ).build()
    private val store = ViewModelStore()
    private lateinit var model: DayItineraryViewModel
    private lateinit var item: String
    private var failSave = false
    private var saveGate: CompletableDeferred<Unit>? = null
    private lateinit var workspace: TripWorkspaceViewModel
    @After fun close() { compose.runOnIdle { store.clear() }; db.close() }

    private fun open(fontScale: Float = 1f, inWorkspace: Boolean = false) {
        compose.runOnUiThread {
            val mainActivity = android.content.ComponentName(compose.activity, com.yangchengwei.easytrip.MainActivity::class.java)
            val mode = compose.activity.packageManager.getActivityInfo(mainActivity, 0).softInputMode
            compose.activity.window.setSoftInputMode(mode)
        }
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao())
        val trip = runBlocking {
            val trip = trips.createTrip(CreateTrip("费用测试", 1))
            val day = trips.observeTrip(trip).first()!!.days.single().id
            db.savedPlaceDao().insertPlace(SavedPlaceEntity("hotel", trip, "hotel", "湖畔酒店", "地址", 30.0, 120.0))
            item = items.addItem(day, "hotel", 0)
            trip
        }
        compose.runOnIdle {
            val controlled = object : ItineraryRepository by items {
                override suspend fun saveDetailsWithExpenses(
                    itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?, note: String?,
                    expectedExpenses: List<PlaceExpenseInput>, expenses: List<PlaceExpenseInput>,
                ) {
                    saveGate?.await()
                    if (failSave) error("测试写入失败")
                    items.saveDetailsWithExpenses(itemId, arrivalTime, stayMinutes, note, expectedExpenses, expenses)
                }
            }
            model = DayItineraryViewModel(trip, trips, controlled, RoomRouteLegRepository(db.routeLegDao()), null)
            store.put("editor", model)
            if (inWorkspace) {
                workspace = TripWorkspaceViewModel(trip, trips, RoomSavedPlaceRepository(db), items,
                    RoomRouteLegRepository(db.routeLegDao()), SavedStateHandle())
                store.put("workspace", workspace)
            }
        }
        compose.setContent {
            val state by model.state.collectAsState()
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)) {
                EasyTripTheme {
                    if (inWorkspace) TripWorkspaceRoute(
                        workspace, consent = null, onBack = {}, onSettings = {}, itineraryViewModel = model,
                        locationPermissionCoordinator = androidx.compose.runtime.remember {
                            LocationPermissionCoordinator(InMemoryLocationPermissionRequestStore())
                        },
                        locationPermissionSnapshot = { LocationPermissionSnapshot(false, false) },
                        onWorkspaceEffect = {},
                    ) else DayItineraryContent(state, onAction = model::dispatch)
                }
            }
        }
        compose.waitUntil(10_000) { model.state.value.items.isNotEmpty() }
        compose.runOnIdle { model.requestTiming(item) }
    }

    @Test fun firstExpenseIsInlineAndAddingAnotherDoesNotPersistUntilSave() {
        open()
        compose.onNodeWithTag("expense-amount-first").performTextInput("600")
        compose.onNodeWithTag("expense-category-first-lodging").performClick()
        assertTrue(runBlocking { RoomExpenseRepository(db).observeRecords().first().isEmpty() })
        compose.onNodeWithTag("itinerary-save").performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        assertEquals(60000L, runBlocking { RoomExpenseRepository(db).observeRecords().first().single().cents })
        compose.runOnIdle { model.requestTiming(item) }
        compose.onNodeWithTag("expense-add").performScrollTo().performClick()
        compose.onNodeWithTag("expense-amount-new-1").performScrollTo().performTextInput("120")
        compose.onNodeWithTag("expense-category-new-1-food").performClick()
        assertEquals(1, runBlocking { RoomExpenseRepository(db).observeRecords().first().size })
        compose.onNodeWithTag("itinerary-save").performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        val saved = runBlocking { RoomExpenseRepository(db).observeRecords().first() }
        assertEquals(listOf(60000L, 12000L), saved.map { it.cents })
        assertEquals(listOf(ExpenseCategory.LODGING, ExpenseCategory.FOOD), saved.map { it.category })
    }

    @Test fun cancelConfirmsWholeDraftAndRemoveCanBeUndoneBeforeSaving() {
        open()
        compose.onNodeWithTag("expense-amount-first").performTextInput("600")
        compose.onNodeWithTag("expense-category-first-lodging").performClick()
        compose.onNodeWithTag("itinerary-note-input").performScrollTo().performTextInput("整页草稿")
        compose.onNodeWithText("取消", useUnmergedTree = true).performClick()
        compose.onNodeWithText("放弃本次修改？").assertIsDisplayed()
        compose.onNodeWithText("继续编辑").performClick()
        compose.onNodeWithTag("itinerary-note-input").assertTextContains("整页草稿")
        compose.onNodeWithTag("expense-remove-first").performScrollTo().performClick()
        compose.onNodeWithText("撤销").performScrollTo().performClick()
        compose.onNodeWithTag("expense-amount-first").assertTextContains("600")
        compose.onNodeWithText("取消", useUnmergedTree = true).performClick()
        compose.onNodeWithText("放弃修改").performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        assertTrue(runBlocking { RoomExpenseRepository(db).observeRecords().first().isEmpty() })
        assertNull(model.state.value.items.single().note)
    }

    @Test fun saveFailureRetainsEveryFieldAndRetryCommitsTogether() {
        verifySaveFailure(inWorkspace = false)
    }

    @Test fun workspaceSaveFailureRetainsEveryFieldAndRetryCommitsTogether() {
        verifySaveFailure(inWorkspace = true)
    }

    private fun verifySaveFailure(inWorkspace: Boolean) {
        open(inWorkspace = inWorkspace)
        compose.onNodeWithTag("expense-amount-first").performScrollTo().performTextInput("600")
        compose.onNodeWithTag("expense-category-first-lodging").performScrollTo().performClick()
        compose.onNodeWithTag("itinerary-note-input").performScrollTo().performTextInput("保留整页")
        compose.runOnIdle {
            model.updateArrivalTime("09:30")
            model.updateStayMinutes("120")
            failSave = true
            saveGate = CompletableDeferred()
        }
        compose.onNodeWithTag("itinerary-save").assertIsEnabled().performClick()
        compose.onNodeWithText("保存中…").assertIsNotEnabled()
        compose.onNodeWithTag("itinerary-cancel").assertIsNotEnabled()
        assertTrue(runBlocking { RoomExpenseRepository(db).observeRecords().first().isEmpty() })
        compose.runOnIdle { saveGate!!.complete(Unit) }
        compose.onNodeWithText("修改尚未保存").assertIsDisplayed()
        if (inWorkspace) captureEditor("workspace-save-failure")
        compose.onNodeWithText("继续编辑").performClick()
        if (inWorkspace) captureEditor("workspace-retained-draft")
        compose.runOnIdle {
            val draft = model.state.value.editDraft!!
            assertEquals("09:30", draft.arrivalTimeText)
            assertEquals("120", draft.stayMinutesText)
            assertEquals("保留整页", draft.noteText)
            assertEquals("600", draft.expenses.single().amount)
            failSave = false
        }
        compose.onNodeWithTag("itinerary-save").performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        assertEquals(60000L, runBlocking { RoomExpenseRepository(db).observeRecords().first().single().cents })
        val saved = model.state.value.items.single()
        assertEquals(LocalTime.of(9, 30), saved.arrivalTime)
        assertEquals(120, saved.stayMinutes)
        assertEquals("保留整页", saved.note)
    }

    @Test fun workspaceKeyboardKeepsSaveAndCancelAboveImeWithoutPrematureSave() {
        open(fontScale = 2f, inWorkspace = true)
        compose.onNodeWithTag("expense-amount-first").performScrollTo().performClick().performTextInput("600")
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
        }
        compose.waitUntil(5_000) {
            val visibleFrame = android.graphics.Rect()
            val location = IntArray(2)
            compose.activity.window.decorView.getWindowVisibleDisplayFrame(visibleFrame)
            compose.activity.window.decorView.getLocationOnScreen(location)
            listOf("itinerary-save", "itinerary-cancel").all {
                val bounds = compose.onNodeWithTag(it).fetchSemanticsNode().boundsInWindow
                bounds.height > 0 && bounds.bottom + location[1] <= visibleFrame.bottom
            }
        }
        compose.onNodeWithTag("itinerary-save").assertIsDisplayed()
        compose.onNodeWithTag("itinerary-cancel").assertIsDisplayed()
        captureEditor("workspace-keyboard-double-font")
        assertTrue(runBlocking { RoomExpenseRepository(db).observeRecords().first().isEmpty() })
        compose.onNodeWithTag("expense-amount-first").performImeAction()
        compose.onNodeWithTag("expense-category-first-lodging").performScrollTo().performClick()
        compose.onNodeWithTag("itinerary-save").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        assertEquals(60000L, runBlocking { RoomExpenseRepository(db).observeRecords().first().single().cents })
    }

    private fun captureEditor(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = java.io.File(compose.activity.getExternalFilesDir(null), "edit-place-sections").apply { mkdirs() }
        java.io.File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun largeFontKeyboardDoneDoesNotSaveAndAllCategoriesRemainReachable() {
        open(fontScale = 2f)
        compose.onNodeWithTag("expense-amount-first").performScrollTo().performTextInput("0")
        compose.onNodeWithTag("expense-amount-first").performImeAction()
        compose.onNodeWithTag("expense-category-first-other").performScrollTo().performClick()
        assertTrue(runBlocking { RoomExpenseRepository(db).observeRecords().first().isEmpty() })
        compose.onNodeWithTag("itinerary-save").assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        val saved = runBlocking { RoomExpenseRepository(db).observeRecords().first().single() }
        assertEquals(0L, saved.cents)
        assertEquals(ExpenseCategory.OTHER, saved.category)
    }

    @Test fun productionWorkspaceBackKeepsDirtyEditorUntilExplicitDiscard() {
        open(inWorkspace = true)
        compose.onNodeWithTag("workspace-editor-sheet").assertIsDisplayed()
        compose.onNodeWithTag("expense-amount-first").performScrollTo().performTextInput("600")
        compose.onNodeWithTag("expense-category-first-lodging").performScrollTo().performClick()
        compose.onNodeWithTag("expense-amount-first").performImeAction()
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
        compose.runOnIdle { assertTrue(model.state.value.editDraft!!.isDirty) }
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("放弃本次修改？").assertIsDisplayed()
        compose.onNodeWithText("继续编辑").performClick()
        compose.onNodeWithTag("workspace-editor-sheet").assertIsDisplayed()
        compose.onNodeWithTag("expense-amount-first").assertTextContains("600")
        compose.onNodeWithTag("workspace-editor-sheet").captureToImage().asAndroidBitmap().let { bitmap ->
            java.io.File(compose.activity.getExternalFilesDir(null), "v200-inline-editor.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.onNodeWithTag("itinerary-cancel").performClick()
        compose.onNodeWithText("放弃修改").performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null && workspace.state.value.overlay == WorkspaceOverlay.None }
        assertTrue(runBlocking { RoomExpenseRepository(db).observeRecords().first().isEmpty() })
    }

    @Test fun existingManyExpensesExpandInPlaceAndRemovingOneSavesOnlyThatId() {
        open()
        compose.runOnIdle {
            val first = model.state.value.editDraft!!.expenses.single()
            model.updateExpenseRow(first.key, first.copy(amount = "100", category = ExpenseCategory.FOOD))
            repeat(4) {
                model.addExpense()
                val row = model.state.value.editDraft!!.expenses.last()
                model.updateExpenseRow(row.key, row.copy(amount = "100", category = ExpenseCategory.FOOD))
            }
            model.saveTiming()
        }
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        val before = runBlocking { RoomExpenseRepository(db).observeRecords().first() }
        compose.runOnIdle { model.requestTiming(item) }
        compose.onNodeWithText("查看全部 5 笔").performScrollTo().performClick()
        val last = before.last()
        compose.onNodeWithTag("expense-row-${last.key.id}").performScrollTo().performClick()
        compose.onNodeWithTag("expense-remove-${last.key.id}").performScrollTo().performClick()
        assertEquals(before, runBlocking { RoomExpenseRepository(db).observeRecords().first() })
        compose.onNodeWithTag("itinerary-save").performClick()
        compose.waitUntil(10_000) { model.state.value.editDraft == null }
        assertEquals(before.filterNot { it.key == last.key }, runBlocking { RoomExpenseRepository(db).observeRecords().first() })
    }

    @Test fun collapsedIncompleteExpenseExplainsErrorAndCanBeOpenedForCorrection() {
        open()
        compose.onNodeWithTag("expense-amount-first").performTextInput("600")
        compose.onNodeWithTag("expense-add").performScrollTo().performClick()
        compose.onNodeWithTag("expense-error-first").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("expense-category-first-lodging").performScrollTo().performClick()
        compose.onNodeWithTag("itinerary-save").assertIsEnabled()
    }
}
