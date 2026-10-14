package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.cityfirst.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GuideMaterialStoreTest {
    @Test fun twentyImagesSurviveReopeningAndPinnedMaterialCannotBeOverwrittenOrReleased() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "guide-material-test-${UUID.randomUUID()}")
        val session = UUID.randomUUID().toString()
        val references = mutableListOf<String>()
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val links = object : AssistantLinkLoader {
            override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink = error("No link")
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) = Unit
        }
        val store = GuideMaterialStore(File(root, "metadata"), images, links) { references }
        try {
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            val bytes = java.io.ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
            bitmap.recycle()
            val selected = mutableListOf<AssistantImage>()
            repeat(20) { selected += images.importBytes(session, bytes, selected) }
            assertTrue(runCatching { images.importBytes(session, bytes, selected) }.isFailure)
            val material = store.prepare(session, "青州攻略", selected) {}
            references += session
            store.release(session)
            assertTrue(runCatching { store.prepare(session, "不同攻略", selected) {} }.isFailure)
            val reopenedImages = AndroidAssistantImageStore(context, File(root, "images"), false)
            val reopened = GuideMaterialStore(File(root, "metadata"), reopenedImages, links) { references }
            assertEquals(selected.map { it.id }, reopened.read(material.ref).images.map { it.id })
            val workspace = AndroidAssistantImageStore(context, File(root, "workspace"))
            val copied = workspace.restoreCopy(UUID.randomUUID().toString(), selected.first(), reopenedImages.prepare(session, selected.first().id))
            assertEquals(selected.first().id, copied.id)
            references.clear()
            reopened.release(session)
            assertTrue(runCatching { reopened.read(material.ref) }.isFailure)
        } finally { root.deleteRecursively() }
    }

    @Test fun oversizedAndMalformedJournalArePreservedRatherThanOverwritten() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "guide-journal-test-${UUID.randomUUID()}").apply { mkdirs() }
        val file = File(root, "pending.json")
        val journal = GuideCreationJournal(file)
        try {
            file.writeBytes(byteArrayOf(0xc3.toByte(), 0x28))
            assertTrue(runCatching { journal.read() }.isFailure)
            assertEquals(2, file.length())
            assertTrue(runCatching { journal.write("x".repeat(GUIDE_RECORD_LIMIT + 1)) }.isFailure)
            assertEquals(2, file.length())
            val valid = """{"version":1,"session":"${UUID.randomUUID()}"}"""
            assertTrue(runCatching { journal.write(valid) }.isFailure)
            assertEquals(2, file.length())
        } finally { root.deleteRecursively() }
    }

    @Test fun anotherSessionCannotOverwriteAnUnfinishedJournal() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "guide-journal-owner-${UUID.randomUUID()}").apply { mkdirs() }
        val journal = GuideCreationJournal(File(root, "pending.json"))
        val first = """{"version":1,"session":"${UUID.randomUUID()}"}"""
        val second = """{"version":1,"session":"${UUID.randomUUID()}"}"""
        try {
            journal.write(first)
            assertTrue(runCatching { journal.write(second) }.isFailure)
            assertEquals(first, journal.read())
        } finally { root.deleteRecursively() }
    }
}
