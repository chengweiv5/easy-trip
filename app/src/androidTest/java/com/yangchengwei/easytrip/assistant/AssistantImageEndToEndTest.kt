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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Production panel, image preparation, controller and Room import; no external model or real trips. */
class AssistantImageEndToEndTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun imageOnlyDoesNotUploadUntilClickAndSavesOnlyAfterConfirmation() = exercise(false)
    @Test fun narrowDoubleFontImageFlowKeepsSendAndConfirmReachable() = exercise(true)

    private fun exercise(small: Boolean) {
        val context = compose.activity
        val folder = File(context.noBackupFilesDir, "image-e2e-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(context) {
            override fun getNoBackupFilesDir() = folder
        }
        val config = AssistantConfigStore(isolated)
        val imageStore = AndroidAssistantImageStore(context, File(folder, "images"))
        val database = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val vmStore = ViewModelStore()
        val repo = RoomSavedPlaceRepository(database)
        val trip = runBlocking { RoomTripRepository(database.tripDao()).createTrip(CreateTrip("杭州 图片验证", 1)) }
        val modelCalls = AtomicInteger()
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
            val model = PlaceAssistantViewModel(trip, config, RoomPlaceImport(database), repo,
                images = imageStore, imageParserOverride = ImageIntentParser { image, ordinal, _, _, waiting ->
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
            compose.onNodeWithTag("assistant-preview-$imageId").performScrollTo().performClick()
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
            compose.onNodeWithTag("assistant-select-all").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-review").performScrollTo().performClick()
            assertTrue(runBlocking { repo.observePlaces(trip, emptySet()).first() }.isEmpty())
            compose.onNodeWithTag("assistant-confirm").performScrollTo().assertIsDisplayed()
            screenshot("confirm")
            compose.onNodeWithTag("assistant-confirm").performClick()
            compose.waitUntil(10_000) { model.controller.state.value.receipt != null }
            assertEquals(1, runBlocking { repo.observePlaces(trip, emptySet()).first() }.size)
            compose.runOnIdle { model.clearDraft() }
            compose.waitUntil { !model.controller.state.value.hasDraft }
        } finally {
            compose.runOnIdle { vmStore.clear() }
            database.close()
            folder.deleteRecursively()
        }
    }
}
