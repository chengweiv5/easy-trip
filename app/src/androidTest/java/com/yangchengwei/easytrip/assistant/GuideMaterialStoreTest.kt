package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.cityfirst.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GuideMaterialStoreTest {
    @Test fun titleOnlyMaterialKeepsMissingBodyWhenPinnedCopyIsPrepared() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "title-only-material-${UUID.randomUUID()}")
        val references = mutableListOf<String>()
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { input ->
            AssistantLinkDocument(input.platform, input.url, "北城攻略", "", emptyList(), 0,
                bodyAvailable = false)
        }, images, AssistantLinkImageFetcher { _, _ -> error("no images") })
        val store = GuideMaterialStore(File(root, "metadata"), images, loader) { references }
        try {
            val material = store.prepare(UUID.randomUUID().toString(),
                "https://m.ctrip.com/webapp/you/community/detail?articleId=123", emptyList()) {}
            assertFalse(store.read(material.ref).bodyAvailable)
            references += material.ref.id
            val fork = store.prepareOptional(material)
            assertNotEquals(material.ref, fork.ref)
            assertFalse(fork.bodyAvailable)
            assertFalse(store.read(fork.ref).bodyAvailable)
            assertFalse(store.read(material.ref).bodyAvailable)
            val retained = store.retainLoaded(material.originalText, material.modelText, true,
                material.imageSessionId, material.images, images, material.imagePlan, material.bodyAvailable)
            assertFalse(store.read(retained.ref).bodyAvailable)
        } finally { root.deleteRecursively() }
    }
    @Test fun twentyImagesSurviveReopeningAndPinnedMaterialCannotBeOverwrittenOrReleased() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "guide-material-test-${UUID.randomUUID()}")
        val session = UUID.randomUUID().toString()
        val references = mutableListOf<String>()
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val links = object : AssistantLinkLoader {
            override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit): LoadedAssistantLink = error("No link")
            override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
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

    @Test fun optionalImagesKeepTextAndStableSourcesWhenOneFileDisappears() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "optional-material-${UUID.randomUUID()}")
        val references = mutableListOf<String>()
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        var downloads = 0
        val bitmap = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888)
        val png = java.io.ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
        bitmap.recycle()
        val input = "https://m.ctrip.com/webapp/you/community/detail?articleId=123"
        val loader = DefaultAssistantLinkLoader(AssistantLinkReader { link ->
            AssistantLinkDocument(link.platform, link.url, "青州", "青州古城", listOf(
                "https://dimg04.c-ctrip.com/images/1.jpg", "https://dimg04.c-ctrip.com/images/2.jpg"), 2)
        }, images, AssistantLinkImageFetcher { _, _ -> downloads++; png })
        val store = GuideMaterialStore(File(root, "metadata"), images, loader) { references }
        try {
            val read = store.prepare(UUID.randomUUID().toString(), input, emptyList()) {}
            assertEquals(0, downloads)
            assertEquals(2, read.imagePlan.assets.size)
            val prepared = store.prepareOptional(read)
            assertEquals(2, prepared.images.size)
            // A journal may still reference the earlier immutable snapshot after cancellation.
            assertEquals(read.ref, store.read(read.ref).ref)
            assertTrue(store.read(read.ref).images.isEmpty())
            images.remove(prepared.imageSessionId, prepared.images.first().id)
            val recovered = store.read(prepared.ref)
            assertEquals("青州古城", recovered.modelText.substringAfterLast("\n"))
            assertEquals(OptionalImageSkip.MISSING, recovered.imagePlan.assets.first().skip)
            assertEquals(OptionalImageStatus.AVAILABLE, recovered.imagePlan.assets.last().status)
            assertEquals(prepared.ref, recovered.ref)
            references += prepared.ref.id
            val fork = store.prepareOptional(recovered)
            assertNotEquals(prepared.ref.id, fork.ref.id)
            assertEquals(prepared.ref, store.read(prepared.ref).ref)
            assertEquals(2, downloads)
            assertTrue(runCatching { store.read(prepared.ref.copy(digest = "f".repeat(64))) }.isFailure)
        } finally { root.deleteRecursively() }
    }

    @Test fun legacyMaterialWithMissingImageKeepsTextAndRejectsCorruptMetadata() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "legacy-material-${UUID.randomUUID()}")
        val id = UUID.randomUUID().toString(); val imageId = UUID.randomUUID().toString()
        val image = AssistantImage(imageId, 1, "image/jpeg", 3, 1, 1)
        val payload = buildJsonObject {
            put("version", 1); put("session", id); put("original", "北城公园"); put("intent", "北城公园")
            put("text", "北城公园"); put("link", false); put("images", encodeGuideImages(listOf(image)))
            putJsonObject("checksums") { put(imageId, "a".repeat(64)) }
        }.toString()
        val metadata = File(root, "metadata").apply { mkdirs() }
        val file = File(metadata, "$id.json"); file.writeText(payload)
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val store = GuideMaterialStore(metadata, images, optionalLinkFixture(images, "", 0, byteArrayOf())) { emptyList() }
        val ref = GuideMaterialRef(id, guideDigest(payload.toByteArray()))
        try {
            val recovered = store.read(ref)
            assertEquals("北城公园", recovered.modelText)
            assertEquals(OptionalImageSkip.MISSING, recovered.imagePlan.assets.single().skip)
            assertEquals(image, recovered.images.single())
            file.appendText(" ")
            assertTrue(runCatching { store.read(ref) }.isFailure)
            assertTrue(file.exists())
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
