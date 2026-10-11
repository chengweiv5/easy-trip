package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.core.ui.theme.ThemePalette
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Real panel rendering with synthetic failures; no network, model or production database writes. */
class AssistantSelectionBarUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun violetFooterUsesSameBackgroundAsPanel() = withPanel {
        save("violet-footer")
        assertFooterBackground(ThemePalette.VIOLET)
    }

    @Test fun selectAllLabelStaysWithCheckboxInsteadOfSaveButton() = withPanel { assertFooterSpacing() }

    @Test fun violetNarrowDoubleFontKeepsFooterBlendedAndGrouped() = withPanel(small = true) {
        save("violet-footer-small")
        assertFooterBackground(ThemePalette.VIOLET)
        assertFooterSpacing()
        compose.onNodeWithTag("assistant-select-all").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test fun lakeFooterUsesSameBackgroundAsPanel() = withPanel(palette = ThemePalette.LAKE) {
        assertFooterBackground(ThemePalette.LAKE)
    }

    @Test fun forestFooterUsesSameBackgroundAsPanel() = withPanel(palette = ThemePalette.FOREST) {
        assertFooterBackground(ThemePalette.FOREST)
    }

    @Test fun sunsetFooterUsesSameBackgroundAsPanel() = withPanel(palette = ThemePalette.SUNSET) {
        assertFooterBackground(ThemePalette.SUNSET)
    }

    @Test fun roseFooterUsesSameBackgroundAsPanel() = withPanel(palette = ThemePalette.ROSE) {
        assertFooterBackground(ThemePalette.ROSE)
    }

    private fun assertFooterSpacing() {
        val checkbox = compose.onNodeWithTag("assistant-select-all-box", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val label = compose.onNodeWithText("全选", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val group = compose.onNodeWithTag("assistant-select-all").getUnclippedBoundsInRoot()
        val save = compose.onNodeWithTag("assistant-save").getUnclippedBoundsInRoot()
        val footer = compose.onNodeWithTag("assistant-selection-bar").getUnclippedBoundsInRoot()
        assertEquals("Pencil checkbox-to-label gap", 8f, (label.left - checkbox.right).value, .5f)
        assertEquals("Pencil selection group width", 86f, (group.right - group.left).value, .5f)
        assertEquals("Pencil selection-group-to-save gap", 16f, (save.left - group.right).value, .5f)
        assertTrue("Label must be closer to checkbox than save", label.left - checkbox.right < save.left - label.right)
        assertTrue("Label remains inside its shared touch target", label.left >= group.left && label.right <= group.right)
        assertTrue("Save remains inside footer", save.left >= footer.left && save.right <= footer.right)
        compose.onNodeWithTag("assistant-select-all").assertIsNotEnabled()
        compose.onNodeWithTag("assistant-save").assertIsNotEnabled()
    }

    private fun withPanel(
        palette: ThemePalette = ThemePalette.VIOLET,
        small: Boolean = false,
        assertions: () -> Unit,
    ) {
        assumeTrue("Only run synthetic UI fixtures on an emulator", Build.HARDWARE in setOf("ranchu", "goldfish"))
        val context = compose.activity.applicationContext
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val store = ViewModelStore()
        val model = PlaceAssistantViewModel("footer-fixture", AssistantConfigStore(context), object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("Must not write")
        }, RoomSavedPlaceRepository(db))
        store.put("assistant", model)
        val state = PlaceAssistantState(configured = true, consent = true, mapReady = true,
            items = listOf("昭德古街", "黄花溪", "九龙峪景区", "广福寺").mapIndexed { index, name ->
                IntakeItem("failed-$index", ('E' + index).toString(), PlaceIntent(name, "青州", name),
                    status = IntakeStatus.FAILED, detail = "地点查询失败，可仅重试此项。")
            })
        try {
            compose.setContent {
                EasyTripTheme(palette) {
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides Density(density, if (small) 2f else 1f)) {
                        Box(Modifier.requiredSize(if (small) 320.dp else 390.dp, 568.dp)
                            .background(palette.colors.background).testTag("footer-fixture")
                            .padding(horizontal = 16.dp)) {
                            PlaceAssistantPanel(AssistantWorkspace(model, state, true, null, "测试旅行", {}, {}, {}, {}))
                        }
                    }
                }
            }
            assertions()
        } finally {
            compose.runOnIdle { compose.activity.setContentView(View(compose.activity)); store.clear() }
            db.close()
        }
    }

    private fun assertFooterBackground(palette: ThemePalette) {
        val bitmap = compose.onNodeWithTag("assistant-selection-bar").captureToImage().asAndroidBitmap()
        for (x in listOf(1, bitmap.width / 2, bitmap.width - 2)) {
            assertEquals("Footer must blend into ${palette.id} panel, not paint a white plate",
                palette.colors.background.toArgb(), bitmap.getPixel(x, 1))
        }
    }

    private fun save(name: String) {
        val bitmap = compose.onNodeWithTag("footer-fixture").captureToImage().asAndroidBitmap()
        val dir = File(compose.activity.getExternalFilesDir(null), "assistant-footer").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
