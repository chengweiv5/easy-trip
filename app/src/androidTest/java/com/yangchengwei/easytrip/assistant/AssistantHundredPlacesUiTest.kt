package com.yangchengwei.easytrip.assistant

import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Production panel and controller with synthetic POIs and an in-memory repository only. */
class AssistantHundredPlacesUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun hundredPlacesTailLabelsAndDirectSave() = exercise(false)
    @Test fun narrowDoubleFontHundredPlacesTailLabelsAndDirectSave() = exercise(true)

    private fun exercise(small: Boolean) {
        assumeTrue(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val context = compose.activity.applicationContext
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val stores = ViewModelStore()
        val saved = AtomicInteger()
        val names = (1..100).map { "公园$it" }
        val model = PlaceAssistantViewModel("hundred-fixture", AssistantConfigStore(context), object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                saved.set(confirmation.items.size)
                return ImportReceipt(confirmation.operationId, confirmation.tripId, confirmation.digest,
                    confirmation.items.map { ImportItemResult(it.itemId, it.poi.poiId, it.itemId, true) })
            }
        }, RoomSavedPlaceRepository(db), PlaceIntentParser { _, _ ->
            names.map { PlaceIntent(it, "杭州", it) }
        })
        stores.put("assistant", model)
        try {
            compose.runOnUiThread {
                model.controller.updateSource(object : PlaceSearchDataSource {
                    override suspend fun search(keyword: String, city: String?) =
                        listOf(PlaceCandidate(keyword, keyword, "测试地址", GeoPoint(30.2, 120.1), null, "杭州"))
                })
                model.controller.environment(true, true, true)
                model.controller.editCity("杭州"); model.controller.editInput(names.joinToString("、"))
                model.controller.submit()
            }
            compose.setContent {
                val state by model.controller.state.collectAsState()
                EasyTripTheme {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, if (small) 2f else 1f)) {
                        Box(Modifier.requiredSize(if (small) 320.dp else 390.dp, 568.dp)) {
                            PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "测试旅行", {}, {}, {}, {}))
                        }
                    }
                }
            }
            compose.waitUntil(100_000) { !model.controller.state.value.busy }
            assertEquals(100, model.controller.state.value.eligible.size)
            for (label in listOf("A", "Z", "AA", "AZ", "BA", "BZ", "CA", "CS", "CT", "CU", "CV")) {
                compose.onNodeWithTag("assistant-marker-$label").performScrollTo().assertIsDisplayed()
                    .assertWidthIsEqualTo(28.dp).assertHeightIsAtLeast(48.dp)
                compose.onNodeWithTag("assistant-badge-$label", useUnmergedTree = true)
                    .assertWidthIsEqualTo(28.dp).assertHeightIsEqualTo(28.dp)
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(label, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                saveScreenshot(if (small) "narrow-$label" else "normal-$label")
                // TextLayout uses integer pixel size, while the line edge may retain a subpixel.
                // Allow at most one rounding pixel, never a wrapped or clipped label.
                assertTrue("Badge $label must fit: ${layouts.map { "size=${it.size}, lines=${it.lineCount}, right=${it.getLineRight(0)}, height=${it.didOverflowHeight}" }}",
                    layouts.isNotEmpty() && layouts.all {
                        it.lineCount == 1 && !it.didOverflowHeight &&
                            it.getLineRight(0) <= it.size.width + 1f && it.getLineLeft(0) >= -1f
                    })
                val layout = layouts.single()
                with(layout.layoutInput.density) {
                    assertEquals(if (label.length == 1) 14f else 11f,
                        layout.layoutInput.style.fontSize.toPx() / density, .1f)
                }
                compose.onNodeWithTag("assistant-select-$label").assertHeightIsAtLeast(48.dp).performClick()
                assertEquals(0, saved.get())
            }
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsEnabled()
            saveScreenshot(if (small) "narrow-double" else "normal")
            compose.onNodeWithTag("assistant-select-all").performClick()
            compose.waitUntil { model.controller.state.value.selected.size == 100 }
            compose.onNodeWithTag("assistant-save").performClick()
            compose.waitUntil { saved.get() == 100 && model.controller.state.value.items.isEmpty() }
            assertEquals(100, saved.get())
        } finally {
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)); stores.clear() }
            db.close()
        }
    }

    private fun saveScreenshot(name: String) {
        val output = File(compose.activity.getExternalFilesDir(null), "assistant-hundred").apply { mkdirs() }
        val screenshot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(output, "$name.png").outputStream().use {
            requireNotNull(screenshot).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot?.recycle()
    }
}
