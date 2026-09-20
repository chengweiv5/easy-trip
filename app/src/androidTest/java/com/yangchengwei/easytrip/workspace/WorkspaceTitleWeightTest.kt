package com.yangchengwei.easytrip.workspace

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspaceTitleWeightTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun chineseWorkspaceTitleHasVisiblyHeavierStrokesThanRegular() {
        val title = "旅行工作台"
        compose.setContent { EasyTripTheme {
            Column(Modifier.width(360.dp).background(Color.White)) {
                WorkspaceTopBar(title, null, {}, {})
                Text(title, Modifier.testTag("regular-title"),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal))
                Text(title, Modifier.testTag("system-bold-title"),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold))
            }
        } }
        fun ink(tag: String): Double {
            val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
            var sum = 0.0
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                sum += 1.0 - (r + g + b) / 765.0
            }
            return sum
        }
        val actual = ink("workspace-trip-title")
        val regular = ink("regular-title")
        val reference = ink("system-bold-title")
        val comparison = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "workspace-title-weight.png").outputStream().use {
            comparison.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        println("Title ink: actual=$actual regular=$regular systemBold=$reference ratio=${actual / regular}")
        assertTrue("中文标题笔画应明显粗于常规字体: actual=$actual regular=$regular ratio=${actual / regular}", actual > regular * 1.12)
        assertTrue("中文标题应达到系统粗体的笔画强度", actual > reference * .9)
    }
}
