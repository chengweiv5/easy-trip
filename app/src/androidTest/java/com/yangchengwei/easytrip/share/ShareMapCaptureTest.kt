package com.yangchengwei.easytrip.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yangchengwei.easytrip.amap.AmapPrivacyGate
import com.yangchengwei.easytrip.amap.TestConsentGate
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import com.yangchengwei.easytrip.core.model.GeoPoint
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ShareMapCaptureTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun realSdkCaptureUnderOpaquePreviewRetainsMapAndCleansUpView() = runBlocking {
        val context=compose.activity
        val gate=TestConsentGate().apply { show() }
        val token=gate.decide(true)!!
        AmapPrivacyGate.create(context).apply { reportShown();reportDecision(true) }
        lateinit var host:FrameLayout
        compose.runOnUiThread { host=FrameLayout(context) }
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory={host},modifier=Modifier.size(342.dp,171.dp))
                Box(Modifier.fillMaxSize().background(Color.White)) { Text("地图截图测试") }
            }
        }
        compose.waitForIdle()
        val capture=ShareMapCapture(host,token)
        val day=shareFixture().days.first()
        val pending=async { capture.capture(day.copy(index=5)) }
        delay(100)
        pending.cancelAndJoin()
        withContext(Dispatchers.Main) { assertEquals(0,host.childCount) }
        val map=capture.capture(day)
        assertNotNull("real map must be captured: ${map.message}",map.file)
        assertTrue(map.file!!.length()>10_000)
        BitmapFactory.decodeFile(map.file!!.absolutePath).let { bitmap ->
            assertTrue(bitmap.width>=684);assertTrue(bitmap.height>=342)
            File(context.getExternalFilesDir(null),"share-real-map.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
        withContext(Dispatchers.Main) { assertEquals(0,host.childCount) }
        assertEquals(map.file,capture.capture(day).file)
        val nearby = day.copy(stops=day.stops.mapIndexed { i,stop ->
            stop.copy(point=GeoPoint(30.248+i*0.000001,120.149),leg=null)
        })
        assertNotNull("nearby numbers must remain exportable",capture.capture(nearby).file)
        val trip=shareFixture()
        ShareImageRenderer(24_000_000).render(trip,ShareOptions(),trip.days.associate { it.id to capture.capture(it) },File(context.getExternalFilesDir(null),"share-real-full.png"))
        gate.decide(false)
        assertNull(capture.capture(day).file)
        assertNull(ShareMapCapture(host,null).capture(day).file)
        assertNull(ShareMapCapture(host,token).capture(day.copy(stops=emptyList())).file)
    }
}
