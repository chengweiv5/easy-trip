package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class GuideTestImages(
    val prepareImage: suspend (String) -> PreparedImage = { PreparedImage(it, "image/jpeg", byteArrayOf(1, 2, 3)) },
) : AssistantImageStore {
    override suspend fun prepare(sessionId: String, imageId: String) = prepareImage(imageId)
    override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>): ImageSelectionResult = error("unused")
    override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("unused")
    override suspend fun remove(sessionId: String, imageId: String) = error("must retain originals")
    override suspend fun release(sessionId: String) = error("must retain originals")
}
internal fun guideImages(count: Int) = (1..count).map { AssistantImage("local-$it", it, "image/jpeg", 3, 1, 1) }
internal fun guideRequest(count: Int = 2) = AssistantInputRequest("session", "北城美术馆", "北城",
    guideImages(count), travelMaterials = true)

class GuideBatchFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun cleanupPathResolutionFailureReleasesLeaseAndNextPrepareReclaimsRun() = runBlocking {
        val files = GuideBatchFiles(temporary.root, GuideTestImages(), Mutex())
        val active = files.prepare(guideRequest(1)) {}
        val inaccessible = object : File(active.workspace.path) {
            override fun getCanonicalFile(): File = throw java.io.IOException("path unavailable")
        }
        try {
            files.release(active.copy(workspace = inaccessible))
            assertTrue(active.workspace.isDirectory)
            val next = files.prepare(guideRequest(1)) {}
            assertFalse(active.workspace.exists())
            files.release(next)
        } finally { files.release(active) }
    }

    @Test fun nextSessionReclaimsMarkedAbandonedRunWithoutTouchingActiveOrUnknownFolders() = runBlocking {
        val abandoned = File(temporary.root, java.util.UUID.randomUUID().toString()).apply {
            mkdir()
            java.io.RandomAccessFile(File(this, ".guide-owner"), "rw").use { it.writeUTF("guide-batch-v1:$name") }
            File(this, "request.json").writeText("old temporary request")
        }
        val unknown = File(temporary.root, java.util.UUID.randomUUID().toString()).apply { mkdir() }
        File(unknown, "keep").writeText("unowned")
        val first = GuideBatchFiles(temporary.root, GuideTestImages(), Mutex())
        val active = first.prepare(guideRequest(1)) {}
        assertFalse(abandoned.exists())
        val second = GuideBatchFiles(temporary.root, GuideTestImages(), Mutex())
        val next = second.prepare(guideRequest(1)) {}
        assertTrue(active.workspace.exists())
        assertEquals("unowned", File(unknown, "keep").readText())
        first.release(active)
        second.release(next)
    }

    @Test fun cleanupDoesNotFollowSymlinkToUnownedFile() = runBlocking {
        val cache = temporary.newFolder("cache")
        val sentinel = temporary.newFile("keep").apply { writeText("unowned") }
        val files = GuideBatchFiles(cache, GuideTestImages(), Mutex())
        val batch = files.prepare(guideRequest(1)) {}
        val link = File(batch.workspace, "redirect")
        java.nio.file.Files.createSymbolicLink(link.toPath(), sentinel.toPath())
        files.release(batch)
        assertEquals("unowned", sentinel.readText())
        assertTrue(java.nio.file.Files.isSymbolicLink(link.toPath()))
        val next = files.prepare(guideRequest(1)) {}
        assertEquals("unowned", sentinel.readText())
        files.release(next)
    }

    @Test fun badImageKeepsOtherSourcesAndReleaseOnlyRemovesOwnedFiles() = runTest {
        val root = temporary.newFolder()
        val unrelated = File(root, "other-session").apply { mkdir(); File(this, "keep").writeText("keep") }
        val files = GuideBatchFiles(root, GuideTestImages {
            if (it == "local-1") throw AssistantFailure("图1损坏")
            PreparedImage(it, "image/jpeg", byteArrayOf(1, 2, 3))
        }, Mutex())
        val batch = files.prepare(guideRequest()) {}
        assertEquals(listOf("text", "local-2"), batch.sources.map { it.source.localId })
        assertEquals("local-1", batch.failures.single().sourceId)
        assertArrayEquals(byteArrayOf(1, 2, 3), batch.sources.last().file!!.readBytes())
        files.release(batch)
        assertFalse(batch.workspace.exists())
        assertEquals("keep", File(unrelated, "keep").readText())
    }

    @Test fun concurrentSessionsPrepareOneImageAtATimeAndCancellationCleansOwnRun() = runBlocking {
        withTimeout(10_000) {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val active = java.util.concurrent.atomic.AtomicInteger()
            val peak = java.util.concurrent.atomic.AtomicInteger()
            val store = GuideTestImages {
                val count = active.incrementAndGet(); peak.updateAndGet { old -> maxOf(old, count) }
                try {
                    entered.complete(Unit); release.await()
                    PreparedImage(it, "image/jpeg", byteArrayOf(1, 2, 3))
                } finally { active.decrementAndGet() }
            }
            val files = GuideBatchFiles(temporary.root, store, Mutex())
            val first = async { files.prepare(guideRequest(1)) {} }
            entered.await()
            val second = async { files.prepare(guideRequest(1).copy(sessionId = "other")) {} }
            first.cancelAndJoin()
            release.complete(Unit)
            val batch = second.await()
            assertEquals(1, peak.get())
            assertEquals(listOf(batch.workspace.name), temporary.root.list()!!.toList())
            files.release(batch)
            assertTrue(temporary.root.listFiles()!!.isEmpty())
        }
    }
}
