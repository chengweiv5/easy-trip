package com.yangchengwei.easytrip.assistant

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AssistantImageLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun repeatedClearIsIdempotentAndSelectionWaitsForCleanup() {
        val context = compose.activity
        val directory = File(context.noBackupFilesDir, "lifecycle-test-${UUID.randomUUID()}")
        val isolated = object : ContextWrapper(context) { override fun getNoBackupFilesDir() = directory.apply { mkdirs() } }
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val vms = ViewModelStore()
        val releaseGate = CompletableDeferred<Unit>()
        var releases = 0
        var selections = 0
        val store = object : AssistantImageStore {
            override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>): ImageSelectionResult {
                selections++; return ImageSelectionResult(emptyList(), emptyList())
            }
            override suspend fun prepare(sessionId: String, imageId: String): PreparedImage = error("not used")
            override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("not used")
            override suspend fun remove(sessionId: String, imageId: String) {}
            override suspend fun release(sessionId: String) { releases++; releaseGate.await() }
        }
        try {
            val vm = PlaceAssistantViewModel("trip", AssistantConfigStore(isolated), RoomPlaceImport(db), RoomSavedPlaceRepository(db), images = store)
            vms.put("vm", vm)
            compose.runOnIdle {
                vm.clearDraft(); vm.clearDraft()
                vm.selectImages(listOf(Uri.parse("content://fixture/a")))
            }
            compose.runOnIdle { assertEquals(1, releases); assertEquals(0, selections); assertTrue(vm.controller.state.value.attachmentSelectionBusy) }
            releaseGate.complete(Unit)
            compose.waitUntil { !vm.controller.state.value.attachmentSelectionBusy }
            compose.runOnIdle { vm.selectImages(listOf(Uri.parse("content://fixture/a"))) }
            compose.waitUntil { selections == 1 }
        } finally {
            releaseGate.complete(Unit)
            compose.runOnIdle { vms.clear() }
            db.close(); directory.deleteRecursively()
        }
    }
}
