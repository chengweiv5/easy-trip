package com.yangchengwei.easytrip

import android.content.Context
import android.content.res.XmlResourceParser
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconResourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun manifestLauncherIconsResolveToAdaptiveDrawables() {
        val resources = context.resources
        val packageName = context.packageName
        val launcherId = resources.getIdentifier("ic_launcher", "mipmap", packageName)
        val roundLauncherId = resources.getIdentifier("ic_launcher_round", "mipmap", packageName)
        val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)

        assertTrue(launcherId != 0)
        assertTrue(roundLauncherId != 0)
        assertEquals(launcherId, applicationInfo.icon)
        assertTrue(resources.getDrawable(launcherId, context.theme) is AdaptiveIconDrawable)
        assertTrue(resources.getDrawable(roundLauncherId, context.theme) is AdaptiveIconDrawable)
    }

    @Test
    fun android13AdaptiveIconDeclaresMonochromeDrawable() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val resources = context.resources
        val packageName = context.packageName
        val launcherId = resources.getIdentifier("ic_launcher", "mipmap", packageName)
        val monochromeId = resources.getIdentifier("ic_launcher_monochrome", "drawable", packageName)

        assertTrue(monochromeId != 0)
        val roundLauncherId = resources.getIdentifier("ic_launcher_round", "mipmap", packageName)
        listOf(launcherId, roundLauncherId).forEach { iconId ->
            resources.getXml(iconId).use { parser ->
                var declaredMonochromeId = 0
                while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
                    if (parser.eventType == XmlResourceParser.START_TAG && parser.name == "monochrome") {
                        declaredMonochromeId = parser.getAttributeResourceValue(
                            "http://schemas.android.com/apk/res/android",
                            "drawable",
                            0,
                        )
                        break
                    }
                    parser.next()
                }
                assertEquals(monochromeId, declaredMonochromeId)
            }
        }
        assertNotNull(resources.getDrawable(monochromeId, context.theme))
    }
}
