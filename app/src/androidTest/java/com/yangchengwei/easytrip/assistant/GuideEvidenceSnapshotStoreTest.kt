package com.yangchengwei.easytrip.assistant

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.RoomGuideCreation
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.TravelMode
import java.util.UUID
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GuideEvidenceSnapshotStoreTest {
    @Test fun localDraftReferenceIsCompareAndSetAndRetainedMaterialDoesNotLoadUrlAgain() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "evidence-store-test-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val results = GuideImportResultStore(File(root, "results"))
            val materials = GuideMaterialStore(File(root, "materials"),
                AndroidAssistantImageStore(context, File(root, "images"), false), object : AssistantLinkLoader {
                    override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>,
                        onProgress: (String) -> Unit): LoadedAssistantLink = error("must not load URL")
                    override suspend fun prepare(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                loaded: LoadedAssistantLink, retryIds: Set<String>, onProgress: (String) -> Unit) = loaded
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
                }, results::localMaterialIds)
            val retained = materials.retainLoaded("原分享 https://xhslink.cn/o/fixture", "正文", true,
                UUID.randomUUID().toString(), emptyList(), materials.images)
            val trip = UUID.randomUUID().toString()
            val ref = results.write(PlaceAssistantState(input = "保留"))
            val draft = GuideLocalDraftRef(trip, UUID.randomUUID().toString(), 0, retained.ref, ref)
            assertTrue(results.replaceLocalDraft(trip, null, draft))
            assertEquals(draft, results.localDraft(trip))
            assertFalse(results.replaceLocalDraft(trip, null, draft.copy(revision = 1)))
            materials.release(retained.ref.id)
            assertEquals("正文", materials.read(retained.ref).modelText)
            assertFalse(results.replaceLocalDraft(trip, draft.copy(revision = 1), null))
            assertEquals(draft, results.localDraft(trip))
            assertTrue(results.replaceLocalDraft(trip, draft, null))
            materials.release(retained.ref.id)
            assertTrue(runCatching { materials.read(retained.ref) }.isFailure)
        } finally { root.deleteRecursively() }
    }

    @Test fun failedPartialHandoffCanRetainResultAndStaleAttemptCannotReplaceIt() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        try {
            val store = RoomGuideCreation(db)
            val command = CreateGuideTripCommand.create("测试旅行", null, 1, TravelMode.FLEXIBLE,
                AssistantCityScope(listOf("北市")), GuideMaterialRef(UUID.randomUUID().toString(), "a".repeat(64)))
            store.commit(command)
            val claim = requireNotNull(store.claim(command.operationId, command.digest, 0))
            val ref = UUID.randomUUID().toString()
            assertTrue(store.complete(command.operationId, claim.attempt, GuideHandoffStatus.FAILED, ref))
            assertEquals(ref, store.find(command.tripId)?.resultRef)
            assertEquals(GuideHandoffStatus.FAILED, store.find(command.tripId)?.status)
            assertFalse(store.complete(command.operationId, claim.attempt + 1, GuideHandoffStatus.FAILED,
                UUID.randomUUID().toString()))
            assertEquals(ref, store.find(command.tripId)?.resultRef)
        } finally { db.close() }
    }
}
