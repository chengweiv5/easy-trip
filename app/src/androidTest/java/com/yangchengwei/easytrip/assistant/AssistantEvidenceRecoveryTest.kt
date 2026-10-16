package com.yangchengwei.easytrip.assistant

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.*
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated Room and actual VM lifecycle; no external service or real user data. */
class AssistantEvidenceRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun pastedGuideRestoresBeforeSaveAndOneCollectionFinishesRound() = exercise(false)
    @Test fun globalHandoffRestoresBeforeSaveAndOneCollectionFinishesRound() = exercise(true)
    @Test fun pastedGuideCommitBeforeProcessDeathDoesNotRestoreStaleDraft() = exercise(false, true)
    @Test fun globalGuideCommitBeforeProcessDeathDoesNotRestoreStaleDraft() = exercise(true, true)

    private fun exercise(global: Boolean, interruptAfterCommit: Boolean = false) {
        val context = compose.activity
        val root = File(context.noBackupFilesDir, "evidence-recovery-${UUID.randomUUID()}").apply { mkdirs() }
        val config = AssistantConfigStore(object : ContextWrapper(context) {
            override fun getNoBackupFilesDir() = root
        })
        config.save(ProviderConfig("https://api.deepseek.com", "deepseek-flash", "fixture-not-a-key"))
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val creation = RoomGuideCreation(db)
        val results = GuideImportResultStore(File(root, "results"))
        val reads = AtomicInteger(); val extracts = AtomicInteger(); val resolves = AtomicInteger()
        val searches = AtomicInteger()
        val materialImages = AndroidAssistantImageStore(context, File(root, "material-images"), false)
        var loadingImages = materialImages
        val imageBytes = java.io.ByteArrayOutputStream().use { out ->
            android.graphics.Bitmap.createBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888).also {
                it.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                it.recycle()
            }
            out.toByteArray()
        }
        val loader = object : AssistantLinkLoader {
            override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                onProgress: (String) -> Unit): LoadedAssistantLink {
                reads.incrementAndGet()
                return LoadedAssistantLink("北城美术馆",
                    listOf(loadingImages.importBytes(sessionId, imageBytes, existing)))
            }
            override suspend fun discard(sessionId: String, images: List<AssistantImage>) {}
        }
        val materials = GuideMaterialStore(File(root, "materials"),
            materialImages, loader) {
            creation.referencedMaterialsForTest(db) + results.localMaterialIds()
        }
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = Buffer(); chain.request().body!!.writeTo(buffer)
            val body = Json.parseToJsonElement(buffer.readUtf8()).jsonObject
            val tool = body["tools"]!!.jsonArray.single().jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content
            assertEquals("extract_guide_evidence", tool); extracts.incrementAndGet()
            val arguments = Json.parseToJsonElement("""{"sources":[{"sourceId":"text","status":"OK","evidence":[
                {"rawQuote":"北城美术馆","kind":"BODY_EXCERPT","basis":"RECOMMENDED","uncertain":false,"detail":""},
                {"rawQuote":"美术馆","kind":"BODY_EXCERPT","basis":"RECOMMENDED","uncertain":false,"detail":""}]},
                {"sourceId":"image_1","status":"NO_PLACES","evidence":[]}]}""").jsonObject
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(batchedDeviceReply(arguments, tool).toResponseBody()).build()
        }.build()
        val resolver = GuideEvidenceResolver { evidence, ctx, frozen ->
            val number = resolves.incrementAndGet()
            val groups = GuideEntityProtocol.decode(batchedDeviceReply(Json.parseToJsonElement(
                """{"evidenceResults":[{"e":0,"status":"PARSED","mentions":[{"literal":"美术馆","occurrence":0,"city":"","cityEvidenceRefs":[],"role":"PLACE","parentLiteral":"","partKind":null,"uncertain":false}]},{"e":1,"status":"REVIEW"}],"groups":[{"members":[[0,0]],"relation":"SAME_LITERAL","evidenceRefs":[0],"frozenAnchors":[]}]}"""
            ).jsonObject, "resolve_guide_entities"), evidence, ctx, frozen)
            val group = groups.groups.single()
            val decision = GuideNameSelector.select(group, evidence)
            GuideResolution(listOf(GuideResolvedPlace(
                PlaceIntent("美术馆", "北城", "北城美术馆", materialKeys = group.members.map { it.id }.toSet()),
                group.members, decision, group.relation, group.relationEvidenceIds, emptySet())),
                if (number == 1) groups.pending else emptyList(), emptySet())
        }
        val models = ViewModelStore()
        var interruptCommit = interruptAfterCommit
        val roomImporter = RoomPlaceImport(db)
        val importer = object : PlaceImport by roomImporter {
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                val receipt = roomImporter.commit(confirmation)
                if (interruptCommit) {
                    interruptCommit = false
                    throw CancellationException("fixture: process stopped after durable transaction, before UI cleanup")
                }
                return receipt
            }
        }
        try {
            val command = runBlocking {
                val retained = materials.prepare(UUID.randomUUID().toString(), "北城 https://xhslink.cn/o/fixture", emptyList()) {}
                CreateGuideTripCommand.create("北城之旅", null, 1, TravelMode.FLEXIBLE,
                    AssistantCityScope(listOf("北城")), retained.ref).also { creation.commit(it) }
            }
            if (!global) runBlocking {
                // This fixture creates a trip through the repository, but removes its unrelated pending handoff.
                creation.complete(command.operationId, 0, GuideHandoffStatus.FINISHED, null)
            }
            fun create(): PlaceAssistantViewModel {
                val images = AndroidAssistantImageStore(context, File(root, "vm-images"))
                loadingImages = images
                return PlaceAssistantViewModel(command.tripId, config, importer,
                    RoomSavedPlaceRepository(db), images = images, linkLoader = loader,
                    guideStore = if (global) creation else null, guideMaterials = materials, guideResults = results,
                    guideEvidenceParserFactory = { snapshot ->
                        BatchedGuideMaterialParser(snapshot, GuideBatchFiles(File(root, "bodies"), images, Mutex()),
                            GuideBatchBody(EvidenceGuideWireProtocol), AssistantTransport(http),
                            GuideBatchScheduler(), protocol = EvidenceGuideWireProtocol)
                    }, evidenceResolverOverride = resolver).also { vm ->
                    vm.controller.environment(true, true, true)
                    vm.controller.updateSource(object : PlaceSearchDataSource {
                        override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                            searches.incrementAndGet()
                            return listOf(PlaceCandidate("museum", "美术馆", "地址", GeoPoint(30.2, 120.1), null, "北城"))
                        }
                    })
                }
            }
            lateinit var vm: PlaceAssistantViewModel
            compose.runOnUiThread { vm = create(); models.put("vm", vm) }
            compose.waitUntil(10_000) { !vm.controller.state.value.attachmentSelectionBusy &&
                (!global || vm.guide.value != null) }
            compose.runOnIdle {
                if (global) vm.consumePendingGuide()
                else { vm.controller.editCity("北城"); vm.controller.editInput("北城 https://xhslink.cn/o/fixture"); vm.controller.submit() }
            }
            compose.waitUntil(10_000) { !vm.controller.state.value.busy && vm.controller.state.value.eligible.size == 1 }
            compose.waitUntil(10_000) { runBlocking {
                if (global) creation.find(command.tripId)?.resultRef != null
                else results.localDraft(command.tripId) != null
            } }
            val readsBefore = reads.get()
            val imageBefore = vm.controller.state.value.images.single()
            if (!interruptAfterCommit) {
                // Unsaved drafts still recover without another URL read, model call or POI query.
                compose.runOnUiThread { models.put("vm", object : ViewModel() {}) }
                runBlocking { delay(250) }
                compose.runOnUiThread { vm = create(); models.put("vm", vm) }
                if (global) {
                    compose.waitUntil(10_000) { vm.guide.value != null }
                    compose.runOnIdle { vm.consumePendingGuide() }
                }
                compose.waitUntil(10_000) { vm.controller.state.value.eligible.size == 1 &&
                    !vm.controller.state.value.attachmentSelectionBusy }
                assertEquals(imageBefore, vm.controller.state.value.images.single())
                assertEquals(readsBefore, reads.get())
                assertEquals(1, extracts.get()); assertEquals(1, resolves.get()); assertEquals(1, searches.get())
            }
            compose.setContent {
                val state by vm.controller.state.collectAsState()
                EasyTripTheme {
                    Box(Modifier.widthIn(max = 320.dp).fillMaxSize()) {
                        PlaceAssistantPanel(AssistantWorkspace(vm, state, true, null, "北城之旅", {}, {}, {}, {}))
                    }
                }
            }
            compose.onNodeWithText("部分地点整理未完成，成功结果可继续标记和收藏。")
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("assistant-select-A").performScrollTo().performClick()
            compose.onNodeWithTag("assistant-save").assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(10_000) { runBlocking {
                !vm.controller.state.value.saving &&
                    RoomSavedPlaceRepository(db).observePlaces(command.tripId, emptySet()).first().size == 1
            } }
            if (!interruptAfterCommit) {
                compose.waitUntil(10_000) { !vm.controller.state.value.hasDraft && vm.guide.value == null }
                compose.onNodeWithTag("assistant-save").assertDoesNotExist()
                compose.onNodeWithTag("assistant-retry-images").assertDoesNotExist()
                assertNull(vm.focus.value)
                assertNull(vm.detail.value)
            } else {
                assertTrue(vm.controller.state.value.hasDraft)
            }
            if (global) assertEquals(GuideHandoffStatus.FINISHED, runBlocking { creation.find(command.tripId) }!!.status)
            compose.runOnUiThread { models.put("vm", object : ViewModel() {}) }
            runBlocking { delay(250) }
            compose.runOnUiThread { vm = create(); models.put("vm", vm) }
            compose.waitUntil(10_000) { !vm.controller.state.value.attachmentSelectionBusy &&
                vm.controller.state.value.receipt != null }
            compose.runOnIdle { vm.consumePendingGuide() }
            compose.waitForIdle()
            assertEquals(readsBefore, reads.get()); assertEquals(1, extracts.get()); assertEquals(1, resolves.get())
            assertEquals(1, searches.get())
            assertFalse(vm.controller.state.value.hasDraft)
            assertNull(vm.guide.value)
            assertNull(vm.controller.state.value.guideCheckpoint)
            assertNull(runBlocking { results.localDraft(command.tripId) })
            assertEquals(1, vm.controller.state.value.receipt?.addedCount)
            compose.runOnIdle { vm.controller.editInput("下一批") }
            assertEquals("下一批", vm.controller.state.value.input)
        } finally {
            compose.runOnUiThread { models.clear() }
            runBlocking { delay(300) }
            db.close(); root.deleteRecursively()
        }
    }
}

private suspend fun RoomGuideCreation.referencedMaterialsForTest(db: EasyTripDatabase) =
    db.guideCreationDao().referencedMaterials()
