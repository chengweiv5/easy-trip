package com.yangchengwei.easytrip.place.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.BuildConfig
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class PlaceSearchEvidenceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun captureLoadingEvidence_s1OvvX_controlledStateRender() {
        val evidence = evidenceCase("s1OvvX", "captureLoadingEvidence_s1OvvX_controlledStateRender", "v1-44-s1OvvX-loading-controlled.png")
        clean(evidence)
        render(PlaceSearchUiState(search = PlaceSearchState(query = QUERY, phase = PlaceSearchPhase.Loading)))
        assertSharedStructure()
        compose.onNodeWithText("正在搜索地点").assertIsDisplayed()
        compose.onNodeWithText("正在查找“西湖”相关结果…").assertIsDisplayed()
        assertTextInside("正在搜索地点", "place-search-surface")
        assertTextInside("正在查找“西湖”相关结果…", "place-search-surface")
        assertTextDoesNotOverlap("正在搜索地点", "正在查找“西湖”相关结果…")
        captureEvidence(evidence)
    }

    @Test fun captureNetworkFailureEvidence_GJo79_controlledStateRender() {
        val evidence = evidenceCase("GJo79", "captureNetworkFailureEvidence_GJo79_controlledStateRender", "v1-38-GJo79-network-failure-controlled.png")
        clean(evidence)
        render(
            PlaceSearchUiState(
                search = PlaceSearchState(query = QUERY, phase = PlaceSearchPhase.NetworkFailure(NETWORK_FAILURE_MESSAGE)),
            ),
        )
        assertSharedStructure()
        compose.onNodeWithText("网络连接失败", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(NETWORK_FAILURE_MESSAGE).assertIsDisplayed()
        compose.onNodeWithText("重新搜索").assertIsDisplayed().assertHasClickAction()
        assertInside("place-search-network-failure-body", "place-search-surface")
        assertInside("place-search-network-failure-icon", "place-search-surface")
        assertInside("place-search-network-failure-title", "place-search-surface")
        assertInside("place-search-network-failure-description", "place-search-surface")
        assertInside("place-search-network-failure-action", "place-search-surface")
        assertSize("place-search-network-failure-icon", 48f, 48f)
        assertSize("place-search-network-failure-action", expectedHeight = 48f)
        assertTagsDoNotOverlap("place-search-network-failure-icon", "place-search-network-failure-title")
        assertTagsDoNotOverlap("place-search-network-failure-title", "place-search-network-failure-description")
        assertTagsDoNotOverlap("place-search-network-failure-description", "place-search-network-failure-action")
        captureEvidence(evidence)
    }

    private fun evidenceCase(frameId: String, testMethod: String, fileName: String): EvidenceCase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "evidence/batch-2")
        check(directory.isDirectory || directory.mkdirs())
        return EvidenceCase(directory, frameId, testMethod, fileName)
    }

    private fun clean(evidence: EvidenceCase) {
        evidence.files.forEach { check(!it.exists() || it.delete()) }
    }

    private fun render(state: PlaceSearchUiState) {
        compose.setContent { EasyTripTheme { PlaceSearchContent(state, {}) } }
    }

    private fun assertSharedStructure() {
        compose.onNodeWithContentDescription("返回地点池").assertIsDisplayed()
        compose.onNodeWithContentDescription("搜索地点").assertIsDisplayed()
        compose.onNodeWithText(QUERY).assertIsDisplayed()
        compose.onNodeWithTag("place-search-surface").assertIsDisplayed()
        assertSize("place-search-back", 44f, 44f)
        assertSize("place-search-field", expectedHeight = 48f)
        val windowWidthDp = compose.activity.resources.configuration.screenWidthDp
        val expectedSurfaceWidth = if (windowWidthDp == 390) 350f else windowWidthDp - 40f
        assertSize("place-search-surface", expectedWidth = expectedSurfaceWidth, tolerance = 1f)
        assertInsideWindow("place-search-surface", windowWidthDp)
        assertInsideWindow("place-search-back", windowWidthDp)
        assertInsideWindow("place-search-field", windowWidthDp)
        assertTagsDoNotOverlap("place-search-back", "place-search-field")
    }

    private fun assertInside(tag: String, containerTag: String) {
        val container = compose.onNodeWithTag(containerTag).getUnclippedBoundsInRoot()
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= container.left)
        assertTrue(bounds.top >= container.top)
        assertTrue(bounds.right <= container.right)
        assertTrue(bounds.bottom <= container.bottom)
    }

    private fun assertInsideWindow(tag: String, windowWidthDp: Int) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(bounds.left.value >= 0f)
        assertTrue(bounds.right.value <= windowWidthDp.toFloat())
        assertTrue(bounds.top.value >= 0f)
        assertTrue(bounds.bottom.value <= compose.activity.resources.configuration.screenHeightDp.toFloat())
    }

    private fun assertTagsDoNotOverlap(firstTag: String, secondTag: String) {
        val first = compose.onNodeWithTag(firstTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag(secondTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(first.right <= second.left || second.right <= first.left || first.bottom <= second.top || second.bottom <= first.top)
    }

    private fun assertTextInside(text: String, containerTag: String) {
        val container = compose.onNodeWithTag(containerTag).getUnclippedBoundsInRoot()
        val bounds = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= container.left)
        assertTrue(bounds.top >= container.top)
        assertTrue(bounds.right <= container.right)
        assertTrue(bounds.bottom <= container.bottom)
    }

    private fun assertTextDoesNotOverlap(firstText: String, secondText: String) {
        val first = compose.onNodeWithText(firstText, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText(secondText, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(first.right <= second.left || second.right <= first.left || first.bottom <= second.top || second.bottom <= first.top)
    }

    private fun assertSize(
        tag: String,
        expectedWidth: Float? = null,
        expectedHeight: Float? = null,
        tolerance: Float = 0.5f,
    ) {
        val bounds: DpRect = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        expectedWidth?.let { assertEquals(it, (bounds.right - bounds.left).value, tolerance) }
        expectedHeight?.let { assertEquals(it, (bounds.bottom - bounds.top).value, tolerance) }
    }

    private fun captureEvidence(evidence: EvidenceCase) {
        try {
            check(BuildConfig.GIT_SHA.matches(Regex("[0-9a-f]{40}")))
            check(BuildConfig.SOURCE_STATE == "CLEAN")
            compose.waitForIdle()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            val bitmapWidth = screenshot.width
            val bitmapHeight = screenshot.height
            try {
                FileOutputStream(evidence.pngTemp).use { stream ->
                    check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream))
                    stream.fd.sync()
                }
            } finally {
                screenshot.recycle()
            }
            check(evidence.pngTemp.length() > 0L)
            val pngHash = sha256(evidence.pngTemp)
            val configuration = context.resources.configuration
            val expected = ExpectedEvidence(
                evidence = evidence,
                gitSha = BuildConfig.GIT_SHA,
                sourceState = BuildConfig.SOURCE_STATE,
                fingerprint = Build.FINGERPRINT,
                model = Build.MODEL,
                sdk = Build.VERSION.SDK_INT,
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight,
                windowWidthDp = configuration.screenWidthDp,
                windowHeightDp = configuration.screenHeightDp,
                densityDpi = configuration.densityDpi,
                fontScale = configuration.fontScale.toDouble(),
                locale = configuration.locales[0].toLanguageTag(),
                pngHash = pngHash,
            )
            validateIndependentInvariants(expected)
            check(evidence.pngTemp.renameTo(evidence.png))
            val manifest = JSONObject()
                .put("schema_version", expected.schemaVersion)
                .put("evidence_type", expected.evidenceType)
                .put("not_production_real_trigger", expected.notProductionRealTrigger)
                .put("frame_id", evidence.frameId)
                .put("test_class", expected.testClass)
                .put("test_method", evidence.testMethod)
                .put("query", expected.query)
                .put("git_sha", expected.gitSha)
                .put("source_state", expected.sourceState)
                .put("device_serial", expected.deviceSerial)
                .put("device_fingerprint", expected.fingerprint)
                .put("device_model", expected.model)
                .put("device_sdk", expected.sdk)
                .put("bitmap_width_px", expected.bitmapWidth)
                .put("bitmap_height_px", expected.bitmapHeight)
                .put("window_width_dp", expected.windowWidthDp)
                .put("window_height_dp", expected.windowHeightDp)
                .put("target_window_width_dp", expected.targetWindowWidthDp)
                .put("density_dpi", expected.densityDpi)
                .put("font_scale", expected.fontScale)
                .put("locale", expected.locale)
                .put("png_file", evidence.png.name)
                .put("png_sha256", expected.pngHash)
            evidence.manifestTemp.writeText(manifest.toString(2))
            check(evidence.manifestTemp.length() > 0L)
            check(evidence.manifestTemp.renameTo(evidence.manifest))
            validateEvidencePayload(expected)
            evidence.completeTemp.writeText(sha256(evidence.manifest))
            check(evidence.completeTemp.renameTo(evidence.complete))
            validatePublishedEvidence(expected)
        } catch (failure: Throwable) {
            clean(evidence)
            throw failure
        }
    }

    private fun validateEvidencePayload(expected: ExpectedEvidence) {
        validateIndependentInvariants(expected)
        val evidence = expected.evidence
        val json = JSONObject(evidence.manifest.readText())
        val required = setOf(
            "schema_version", "evidence_type", "not_production_real_trigger", "frame_id", "test_class", "test_method", "query",
            "git_sha", "source_state", "device_serial", "device_fingerprint", "device_model", "device_sdk", "bitmap_width_px",
            "bitmap_height_px", "window_width_dp", "window_height_dp", "target_window_width_dp", "density_dpi", "font_scale",
            "locale", "png_file", "png_sha256",
        )
        assertEquals(required, json.keys().asSequence().toSet())
        assertJsonInteger(json, "schema_version", expected.schemaVersion)
        assertJsonString(json, "evidence_type", expected.evidenceType)
        assertJsonBoolean(json, "not_production_real_trigger", expected.notProductionRealTrigger)
        assertJsonString(json, "frame_id", evidence.frameId)
        assertJsonString(json, "test_class", expected.testClass)
        assertJsonString(json, "test_method", evidence.testMethod)
        assertJsonString(json, "query", expected.query)
        assertJsonString(json, "git_sha", expected.gitSha)
        assertJsonString(json, "source_state", expected.sourceState)
        assertJsonString(json, "device_serial", expected.deviceSerial)
        assertJsonString(json, "device_fingerprint", expected.fingerprint)
        assertJsonString(json, "device_model", expected.model)
        assertJsonInteger(json, "device_sdk", expected.sdk)
        assertJsonInteger(json, "bitmap_width_px", expected.bitmapWidth)
        assertJsonInteger(json, "bitmap_height_px", expected.bitmapHeight)
        assertJsonInteger(json, "window_width_dp", expected.windowWidthDp)
        assertJsonInteger(json, "window_height_dp", expected.windowHeightDp)
        assertJsonInteger(json, "target_window_width_dp", expected.targetWindowWidthDp)
        assertJsonInteger(json, "density_dpi", expected.densityDpi)
        assertJsonDouble(json, "font_scale", expected.fontScale)
        assertJsonString(json, "locale", expected.locale)
        assertJsonString(json, "png_file", evidence.png.name)
        assertJsonString(json, "png_sha256", expected.pngHash)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(evidence.png.path, options)
        assertEquals(expected.bitmapWidth, options.outWidth)
        assertEquals(expected.bitmapHeight, options.outHeight)
        assertEquals(expected.pngHash, sha256(evidence.png))
    }

    private fun validateIndependentInvariants(expected: ExpectedEvidence) {
        check(expected.sourceState == "CLEAN")
        check(expected.targetWindowWidthDp == 390)
        check(expected.windowWidthDp > 40)
    }

    private fun validatePublishedEvidence(expected: ExpectedEvidence) {
        val evidence = expected.evidence
        check(evidence.complete.isFile)
        assertEquals(sha256(evidence.manifest), evidence.complete.readText())
        validateEvidencePayload(expected)
    }

    private fun assertJsonString(json: JSONObject, key: String, expected: String) {
        val actual = json.get(key)
        assertTrue("$key must be String", actual is String)
        assertEquals(expected, actual)
    }

    private fun assertJsonBoolean(json: JSONObject, key: String, expected: Boolean) {
        val actual = json.get(key)
        assertTrue("$key must be Boolean", actual is Boolean)
        assertEquals(expected, actual)
    }

    private fun assertJsonInteger(json: JSONObject, key: String, expected: Int) {
        val actual = json.get(key)
        assertTrue("$key must be Int", actual is Int)
        assertEquals(expected, actual)
    }

    private fun assertJsonDouble(json: JSONObject, key: String, expected: Double) {
        val actual = json.get(key)
        assertTrue("$key must be Number", actual is Number)
        assertEquals(expected, (actual as Number).toDouble(), 0.0)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class ExpectedEvidence(
        val evidence: EvidenceCase,
        val gitSha: String,
        val sourceState: String,
        val fingerprint: String,
        val model: String,
        val sdk: Int,
        val bitmapWidth: Int,
        val bitmapHeight: Int,
        val windowWidthDp: Int,
        val windowHeightDp: Int,
        val densityDpi: Int,
        val fontScale: Double,
        val locale: String,
        val pngHash: String,
        val schemaVersion: Int = 1,
        val evidenceType: String = "controlled-state-render",
        val notProductionRealTrigger: Boolean = true,
        val testClass: String = PlaceSearchEvidenceTest::class.java.name,
        val query: String = QUERY,
        val deviceSerial: String = "unavailable",
        val targetWindowWidthDp: Int = 390,
    )

    private data class EvidenceCase(val directory: File, val frameId: String, val testMethod: String, val fileName: String) {
        val png = File(directory, fileName)
        val manifest = File(directory, "$fileName.manifest.json")
        val complete = File(directory, "$fileName.complete")
        val pngTemp = File(directory, "$fileName.png.tmp")
        val manifestTemp = File(directory, "$fileName.manifest.tmp")
        val completeTemp = File(directory, "$fileName.complete.tmp")
        val files = listOf(png, manifest, complete, pngTemp, manifestTemp, completeTemp)
    }

    private companion object {
        const val QUERY = "西湖"
        const val NETWORK_FAILURE_MESSAGE = "无法搜索新的地点。请检查网络连接后重试。"
    }
}
