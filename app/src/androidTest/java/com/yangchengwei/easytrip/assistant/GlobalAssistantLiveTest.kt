package com.yangchengwei.easytrip.assistant

import android.os.Build
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.assistant.data.RoomTripCreation
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in only: public share -> production extraction -> explicit city gate -> real POI -> isolated Room creation. */
class GlobalAssistantLiveTest {
    @Test fun publicGuideCreatesAnUndatedTripOnlyAfterFinalConfirmation() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveGlobalAssistant") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish") &&
            (Build.FINGERPRINT.contains("/sdk_") || Build.MODEL.contains("sdk_gphone")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "v34-live-setup.json")
        val setup = try { Json.parseToJsonElement(file.readText()).jsonObject } finally { check(file.delete()) }
        val config = ProviderConfig(setup.getValue("base_url").jsonPrimitive.content,
            setup.getValue("model").jsonPrimitive.content, setup.getValue("api_key").jsonPrimitive.content).validated()
        check(config.baseUrl == "https://api.deepseek.com")
        val input = setup.getValue("share").jsonPrimitive.content
        val platform = requireNotNull(AssistantLinkInput.parse(input)).platform
        val requests = AtomicInteger()
        val statuses = java.util.concurrent.CopyOnWriteArrayList<Int>()
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).callTimeout(45, TimeUnit.SECONDS).addInterceptor {
                check(requests.incrementAndGet() <= 9) { "Live budget exceeded" }
                it.proceed(it.request()).also { response -> statuses += response.code }
            }.build()
        val session = UUID.randomUUID().toString()
        val store = AndroidAssistantImageStore(context, File(context.noBackupFilesDir, "v34-live-$session"))
        val text = DeepSeekPlaceParser({ config }, client, 20_000, AssistantMaterialPolicy.MAX_PER_SOURCE, true)
        val image = ImagePlaceParser({ config }, AssistantTransport(client), AssistantMaterialPolicy.MAX_PER_SOURCE, true)
        val normalizer = ModelPlaceNormalizer({ config }, AssistantTransport(client))
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val resultFile = File(context.filesDir, "v34-live-$platform.json")
        lateinit var controller: PlaceAssistantController
        var recordController: PlaceAssistantController? = null
        var created = false
        var diagnostic: String? = null
        try {
            AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
            val token = requireNotNull(TestConsentGate().apply { show() }.decide(true))
            controller = PlaceAssistantController("new-live-draft", scope, text, AmapPlaceDataSource(context, token),
                object : PlaceImport {
                    override suspend fun receipt(operationId: String): ImportReceipt? = null
                    override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("existing-trip save forbidden")
                }, processor = { AssistantInputProcessor(text, image, store, normalizer) }, sessionId = session,
                linkLoader = DefaultAssistantLinkLoader(AndroidAssistantLinkReader(context), store),
                target = AssistantTarget.NewTripDraft)
            recordController = controller
            withContext(Dispatchers.Main) {
                controller.environment(true, true, true); controller.editInput(input); controller.submit()
            }
            val extracted = withTimeout(720_000) { controller.state.first { !it.busy } }
            assertNull(extracted.error)
            assertTrue(extracted.awaitingCityConfirmation)
            assertTrue(extracted.items.size in 1..30)
            assertEquals(6, extracted.images.size)
            assertTrue(trips.observeTrips().first().isEmpty())
            assertTrue(extracted.items.all { it.candidates.isEmpty() })
            withContext(Dispatchers.Main) { controller.confirmCities() }
            val matched = withTimeout(180_000) { controller.state.first { !it.busy } }
            assertTrue(matched.eligible.isNotEmpty())
            withContext(Dispatchers.Main) { controller.selectAll() }
            val items = withContext(Dispatchers.Main) { controller.selectedImportItems() }
            val metadata = TripDraftInterpreter({ config }, AssistantTransport(client))
                .interpret(TripDraftRequest(input, LocalDate.of(2026, 10, 13), "Asia/Shanghai"))
            assertNull(metadata.dates.startDate); assertEquals(1, metadata.dates.dayCount)
            val command = TripCreationCommand.create("真实攻略验证", null, 1, TravelMode.FLEXIBLE, items, listOf(platform.name))
            assertTrue(trips.observeTrips().first().isEmpty())
            val creator = RoomTripCreation(db)
            val receipt = creator.commit(command)
            assertEquals(receipt, creator.commit(command))
            assertEquals(1, trips.observeTrips().first().size)
            assertEquals(items.size, trips.observeTrips().first().single().placeCount)
            assertEquals(0, trips.observeTrips().first().single().scheduledDayCount)
            created = true
            if (platform == AssistantLinkPlatform.CTRIP) {
                val dates = TripDraftInterpreter({ config }, AssistantTransport(client)).interpret(
                    TripDraftRequest("我计划明天出发，玩三天", LocalDate.of(2026, 10, 13), "Asia/Shanghai"))
                assertEquals(TripDateSuggestion(LocalDate.of(2026, 10, 14), 3), dates.dates)
            }
        } catch (e: Throwable) { diagnostic = e.javaClass.simpleName + ": " + e.message; throw e }
        finally {
            val s = recordController?.state?.value ?: PlaceAssistantState()
            resultFile.writeText(buildJsonObject {
                put("platform", platform.name); put("modelCalls", requests.get()); put("createdInIsolatedRoom", created)
                put("diagnostic", diagnostic); put("error", s.error); put("imageCount", s.images.size)
                putJsonArray("httpStatuses") { statuses.forEach { add(it) } }
                putJsonArray("items") { s.items.forEach { addJsonObject {
                    put("name", it.intent.query); put("city", it.intent.city); put("status", it.status.name); put("poi", it.poi?.name)
                } } }
            }.toString())
            scope.cancel()
            store.release(session)
            db.close()
        }
    }
}
