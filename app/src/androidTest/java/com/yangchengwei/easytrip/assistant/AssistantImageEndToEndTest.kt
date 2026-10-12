package com.yangchengwei.easytrip.assistant

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Production panel, image preparation, controller and Room import; no external model or real trips. */
class AssistantImageEndToEndTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun imageOnlySavesOnceThenStartsAnEmptyRound() = exercise(false)
    @Test fun narrowDoubleFontImageFlowStartsAnEditableNextRound() = exercise(true)
    @Test fun failedSaveRetainsImagesAndDelayedCleanupDoesNotDeleteNextRound() = exercise(false, true)

    private fun exercise(small: Boolean, failAndDelayCleanup: Boolean = false) {
        val context = compose.activity
        val folder = File(context.noBackupFilesDir, "image-e2e-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) {
            override fun getNoBackupFilesDir() = folder
        }
        val config = AssistantConfigStore(isolated)
        val localImages = AndroidAssistantImageStore(context, File(folder, "images"))
        val cleanupStarted = CompletableDeferred<Unit>()
        val cleanupGate = CompletableDeferred<Unit>()
        val cleanupFinished = CompletableDeferred<Unit>()
        val imageStore = if (!failAndDelayCleanup) localImages else object : AssistantImageStore by localImages {
            override suspend fun remove(sessionId: String, imageId: String) {
                cleanupStarted.complete(Unit)
                cleanupGate.await()
                localImages.remove(sessionId, imageId)
                cleanupFinished.complete(Unit)
            }
        }
        val database = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val vmStore = ViewModelStore()
        val repo = RoomSavedPlaceRepository(database)
        val trip = runBlocking { RoomTripRepository(database.tripDao()).createTrip(CreateTrip("杭州 图片验证", 1)) }
        val modelCalls = AtomicInteger()
        val saveRequests = mutableListOf<ImportConfirmation>()
        val savedState = SavedStateHandle()
        val roomImport = RoomPlaceImport(database)
        val importer = object : PlaceImport by roomImport {
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                saveRequests += confirmation
                if (failAndDelayCleanup && saveRequests.size == 1) error("temporary storage failure")
                return roomImport.commit(confirmation)
            }
        }
        val source = File(folder, "source.png")
        Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.eraseColor(android.graphics.Color.WHITE)
            android.graphics.Canvas(bitmap).drawText("杭州 · 灵隐寺", 40f, 150f,
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 56f })
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            fun screenshot(stage: String) {
                val evidence = File(context.filesDir, "v31-image-evidence").apply { mkdirs() }
                val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                File(evidence, "${if (small) "small" else "normal"}-$stage.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            config.save(ProviderConfig("https://example.invalid", "test-vision", "not-a-real-key"))
            val model = PlaceAssistantViewModel(trip, config, importer, repo,
                images = imageStore, savedState = savedState, imageParserOverride = ImageIntentParser { image, ordinal, _, _, waiting ->
                    modelCalls.incrementAndGet()
                    assertEquals("image/jpeg", image.mimeType)
                    waiting()
                    listOf(PlaceIntent("灵隐寺", "杭州", "灵隐寺",
                        sources = listOf(IntakeSource.Image(image.id, ordinal, "灵隐寺"))))
                })
            vmStore.put("assistant", model)
            model.controller.updateSource(object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) =
                    listOf(PlaceCandidate("lingyin", "灵隐寺", "杭州灵隐路", GeoPoint(30.2,120.1), null, "杭州"))
            })
            compose.setContent {
                val state by model.controller.state.collectAsState()
                val density = LocalDensity.current.density
                EasyTripTheme {
                    CompositionLocalProvider(LocalDensity provides Density(density, if (small) 2f else 1f)) {
                        Box(Modifier.requiredSize(if (small) 320.dp else 390.dp, 568.dp).padding(horizontal = 16.dp)) {
                            PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "杭州 图片验证", {}, {}, {}, {}))
                        }
                    }
                }
            }
            compose.runOnIdle {
                model.controller.environment(true, true, true)
                model.selectImages(listOf(Uri.fromFile(source)))
            }
            compose.waitUntil(10_000) { model.controller.state.value.images.size == 1 && !model.controller.state.value.attachmentSelectionBusy }
            assertEquals(0, modelCalls.get())
            val imageId = model.controller.state.value.images.single().id
            // The thumbnail is in a LazyRow: scroll its outer vertical container first.
            compose.onNodeWithTag("assistant-image-list").performScrollTo()
            compose.onNodeWithTag("assistant-preview-$imageId").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("关闭预览").assertIsDisplayed()
            screenshot("preview")
            compose.onNodeWithText("关闭预览").performClick()
            assertEquals(0, modelCalls.get())
            compose.onNodeWithTag("assistant-submit").performScrollTo().assertIsEnabled().assertHeightIsAtLeast(48.dp)
            screenshot("selected")
            compose.onNodeWithTag("assistant-submit").performClick()
            compose.onNodeWithText("同意并发送本次内容").assertDoesNotExist()
            compose.waitUntil(10_000) { !model.controller.state.value.busy && model.controller.state.value.eligible.size == 1 }
            assertEquals(1, modelCalls.get())
            assertTrue(runBlocking { repo.observePlaces(trip, emptySet()).first() }.isEmpty())
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithTag("assistant-select-all").assertIsDisplayed().performClick()
            assertTrue(runBlocking { repo.observePlaces(trip, emptySet()).first() }.isEmpty())
            compose.onNodeWithTag("assistant-review").assertDoesNotExist()
            compose.onNodeWithTag("assistant-confirm").assertDoesNotExist()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed()
            screenshot("selection")
            compose.runOnIdle { model.showDetail(model.controller.state.value.items.single().id, AssistantDetail.CATEGORY) }
            compose.onNodeWithTag("assistant-save").performClick()
            if (failAndDelayCleanup) {
                compose.waitUntil { model.controller.state.value.error != null }
                assertEquals(listOf(imageId), model.controller.state.value.images.map { it.id })
                assertEquals(1, model.controller.state.value.selected.size)
                assertTrue(savedState.get<Boolean>("assistant-had-images") == true)
                assertFalse(cleanupStarted.isCompleted)
                runBlocking { imageStore.preview(model.sessionId, imageId).recycle() }
                compose.onNodeWithTag("assistant-save").assertIsEnabled().performClick()
            }
            compose.waitUntil(10_000) { model.controller.state.value.receipt != null }
            assertEquals(if (failAndDelayCleanup) 2 else 1, saveRequests.size)
            assertEquals(1, saveRequests.map { it.operationId }.distinct().size)
            assertEquals(1, runBlocking { repo.observePlaces(trip, emptySet()).first() }.size)
            compose.waitUntil { !model.controller.state.value.hasDraft }
            assertNull(model.focus.value)
            assertNull(model.detail.value)
            assertTrue(model.controller.state.value.images.isEmpty())
            assertTrue(model.controller.state.value.sourceProgress.isEmpty())
            assertTrue(savedState.get<Boolean>("assistant-had-images") == false)
            assertTrue(assistantMarkers(model.controller.state.value, model.focus.value).isEmpty())
            compose.onNodeWithTag("assistant-save").assertDoesNotExist()
            assertEquals("", model.controller.state.value.input)
            compose.onNodeWithTag("assistant-input").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("清空这次输入和候选？").assertDoesNotExist()
            var nextImageId: String? = null
            if (failAndDelayCleanup) {
                compose.waitUntil { cleanupStarted.isCompleted }
                compose.runOnIdle { model.selectImages(listOf(Uri.fromFile(source))) }
                compose.waitUntil(10_000) { model.controller.state.value.images.size == 1 && !model.controller.state.value.attachmentSelectionBusy }
                nextImageId = model.controller.state.value.images.single().id
                assertNotEquals(imageId, nextImageId)
                cleanupGate.complete(Unit)
                compose.waitUntil(10_000) { cleanupFinished.isCompleted }
                runBlocking { imageStore.preview(model.sessionId, requireNotNull(nextImageId)).recycle() }
                assertTrue(savedState.get<Boolean>("assistant-had-images") == true)
            }
            compose.waitUntil(10_000) {
                runBlocking { runCatching { imageStore.preview(model.sessionId, imageId).recycle() }.isFailure }
            }
            screenshot("next-round")
            compose.onNodeWithTag("assistant-input").performTextReplacement("西湖")
            compose.onNodeWithTag("assistant-submit").performScrollTo().assertIsEnabled()
            nextImageId?.let { assertEquals(it, model.controller.state.value.images.single().id) }
        } finally {
            cleanupGate.complete(Unit)
            compose.runOnIdle { vmStore.clear() }
            database.close()
            folder.deleteRecursively()
        }
    }
}
