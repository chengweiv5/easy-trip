package com.yangchengwei.easytrip.assistant

import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.cityfirst.GuideImportResultStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Real AtomicFile storage; neither writes to a user trip nor changes permissions. */
class GuideBatchSnapshotStoreTest {
    @Test fun oversizedSnapshotAndUnwritableTargetLeavePreviousReferenceReadable(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "batch-snapshot-test-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val store = GuideImportResultStore(File(root, "valid"))
            val previous = PlaceAssistantState(input = "原攻略", city = "北城")
            val ref = store.write(previous)
            val images = (1..4).map { AssistantImage("source-$it", it, "image/jpeg", 1, 1, 1) }
            val materials = images.map { image ->
                ParsedSource(image.id, (1..100).map { index ->
                    val name = "美术馆${image.ordinal}_$index"
                    val span = name + "字".repeat(500 - name.length)
                    PlaceIntent(name, "北城", span, sources = listOf(
                        IntakeSource.Image(image.id, image.ordinal, span,
                            ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "图".repeat(300)))))
                })
            }
            val oversized = previous.copy(images = images, parsedMaterials = materials)
            assertTrue(runCatching { store.write(oversized) }.exceptionOrNull() is IllegalArgumentException)
            assertEquals(previous, store.read(ref))
            assertEquals(1, File(root, "valid").listFiles()!!.size)
            val notDirectory = File(root, "blocked").apply { writeText("owned test sentinel") }
            assertTrue(runCatching { GuideImportResultStore(notDirectory).write(previous) }.isFailure)
            assertEquals("owned test sentinel", notDirectory.readText())
            assertEquals(previous, store.read(ref))
            assertEquals(400, oversized.parsedMaterials.sumOf { it.intents.size })
        } finally { root.deleteRecursively() }
    }
}
