package com.yangchengwei.easytrip.assistant

import android.os.Build
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.amap.*
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.*
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.amap.AmapPlaceDataSource
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in, isolated emulator and in-memory DB only; production platform readers, DeepSeek and AMap. */
class GuideCityFirstLiveTest {
    @Test fun publicGuideDestinationThenEmptyTripThenExistingAssistant() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveCityFirst") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish") && Build.FINGERPRINT.contains("/sdk_"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val setupFile = File(context.filesDir, "city-first-live-setup.json")
        val setup = try { Json.parseToJsonElement(setupFile.readText()).jsonObject } finally { check(setupFile.delete()) }
        val config = ProviderConfig(setup.getValue("base_url").jsonPrimitive.content,
            setup.getValue("model").jsonPrimitive.content, setup.getValue("api_key").jsonPrimitive.content).validated()
        check(config.baseUrl == "https://api.deepseek.com" && config.model == "deepseek-flash")
        val share = setup.getValue("share").jsonPrimitive.content
        val platform = requireNotNull(AssistantLinkInput.parse(share)).platform.name
        val calls = AtomicInteger()
        val statuses = CopyOnWriteArrayList<Int>()
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .callTimeout(45, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS).addInterceptor {
                check(it.request().url.host == "api.deepseek.com")
                check(calls.incrementAndGet() <= 10) { "Explicit live request budget exceeded" }
                it.proceed(it.request()).also { response -> statuses += response.code }
            }.build()
        val id = UUID.randomUUID().toString()
        val root = File(context.noBackupFilesDir, "city-first-live-$id")
        val images = AndroidAssistantImageStore(context, File(root, "images"), false)
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val creation = RoomGuideCreation(db)
        val materials = GuideMaterialStore(File(root, "material"), images,
            DefaultAssistantLinkLoader(AndroidAssistantLinkReader(context), images), db.guideCreationDao()::referencedMaterials)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var controller: PlaceAssistantController? = null
        var destinations: List<String> = emptyList()
        var imageCount = 0
        var beforePlaces = -1
        var finalPlaces = -1
        var error: String? = null
        var finalState: PlaceAssistantState? = null
        var explicitNormalizationRetries = 0
        try {
            val prepared = materials.prepare(id, share, emptyList()) {}
            imageCount = prepared.images.size
            val destination = ModelGuideDestinationInterpreter({ config }, AssistantTransport(client))
                .interpret(prepared.modelText, prepared.images.map { image -> image to suspend { images.prepare(id, image.id) } })
            destinations = destination.cities.map { it.name }
            assertTrue(destinations.isNotEmpty())
            assertTrue(destinations.all { sameCity(it, "青州") })
            assertTrue(trips.observeTrips().first().isEmpty())
            val command = CreateGuideTripCommand.create(destination.suggestedName.ifBlank { "青州之旅" }, null, 1,
                TravelMode.FLEXIBLE, AssistantCityScope(destinations), prepared.ref)
            materials.withRetained(prepared.ref) { creation.commit(command) }
            beforePlaces = trips.observeTrips().first().single().placeCount
            assertEquals(0, beforePlaces)
            val claimed = requireNotNull(creation.claim(command.operationId, command.digest, 0))
            assertNull(creation.claim(command.operationId, command.digest, 0))
            AmapPrivacyGate.create(context).apply { reportShown(); reportDecision(true) }
            val token = requireNotNull(TestConsentGate().apply { show() }.decide(true))
            val text = DeepSeekPlaceParser({ config }, client, 20_000, AssistantMaterialPolicy.MAX_PER_SOURCE, true)
            val image = ImagePlaceParser({ config }, AssistantTransport(client), AssistantMaterialPolicy.MAX_PER_SOURCE, true,
                cityScope = { claimed.scope })
            val normalizer = ModelPlaceNormalizer({ config }, AssistantTransport(client), cityScope = { claimed.scope })
            val c = PlaceAssistantController(command.tripId, scope, text, AmapPlaceDataSource(context, token),
                RoomPlaceImport(db), processor = { AssistantInputProcessor(text, image, images, normalizer, text, image) },
                sessionId = id)
            controller = c
            withContext(Dispatchers.Main) {
                c.environment(true, true, true)
                check(c.acceptGuideInput(claimed.operationId, prepared.originalText, prepared.images,
                    LoadedAssistantLink(prepared.modelText, prepared.images), claimed.scope))
                c.submit()
            }
            var result = withTimeout(900_000) { c.state.first { !it.busy } }
            if (result.error?.startsWith("地点整理结果不完整") == true && calls.get() < 10) {
                explicitNormalizationRetries++
                withContext(Dispatchers.Main) { c.submit() }
                result = withTimeout(180_000) { c.state.first { !it.busy } }
            }
            finalState = result
            assertNull(result.error)
            assertTrue(result.items.isNotEmpty())
            assertTrue(result.items.none { it.intent.city.isBlank() })
            assertTrue(result.items.any { it.status == IntakeStatus.READY })
            assertEquals(0, trips.observeTrips().first().single().placeCount)
            withContext(Dispatchers.Main) { c.selectAll(); c.saveSelected() }
            withTimeout(30_000) { c.state.first { !it.saving } }
            finalPlaces = trips.observeTrips().first().single().placeCount
            assertEquals(result.eligible.size, finalPlaces)
            assertEquals(GuideHandoffStatus.FINISHED, creation.find(command.tripId)!!.status)
        } catch (e: Throwable) {
            error = e.javaClass.simpleName + ": " + (e as? AssistantFailure)?.userMessage.orEmpty()
            throw e
        } finally {
            File(context.filesDir, "city-first-live-$platform.json").writeText(buildJsonObject {
                put("platform", platform); put("model", config.model); put("calls", calls.get())
                put("statuses", JsonArray(statuses.map(::JsonPrimitive))); put("cities", JsonArray(destinations.map(::JsonPrimitive)))
                put("images", imageCount); put("placesBeforeImport", beforePlaces); put("collected", finalPlaces); put("error", error)
                put("explicitNormalizationRetries", explicitNormalizationRetries)
                putJsonArray("items") { finalState?.items?.forEach { item -> addJsonObject {
                    put("query", item.intent.query); put("city", item.intent.city); put("status", item.status.name)
                    put("poi", item.poi?.name); put("detail", item.detail)
                } } }
            }.toString())
            withContext(Dispatchers.Main) { controller?.cancel() }
            scope.cancel()
            db.close()
            root.deleteRecursively()
        }
    }
}
