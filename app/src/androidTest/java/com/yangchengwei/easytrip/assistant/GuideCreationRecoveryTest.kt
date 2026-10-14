package com.yangchengwei.easytrip.assistant

import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.RoomGuideCreation
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

/** Recreate the production ViewModel against retained files and Room, without model/network calls. */
class GuideCreationRecoveryTest {
    private class Fixture {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "guide-recovery-test-${UUID.randomUUID()}")
        val config = AssistantConfigStore(context)
        val original = config.read()
        val db = Room.inMemoryDatabaseBuilder(context, EasyTripDatabase::class.java).build()
        val creator = RoomGuideCreation(db)
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val file = File(root, "journal.json")
        val journal = GuideCreationJournal(file)
        val stores = ViewModelStore()
        val materials = GuideMaterialStore(File(root, "material"),
            AndroidAssistantImageStore(context, File(root, "images"), false), object : AssistantLinkLoader {
                override suspend fun load(sessionId: String, input: AssistantLinkInput, existing: List<AssistantImage>,
                    onProgress: (String) -> Unit) = LoadedAssistantLink("青州三日游：公园", emptyList())
                override suspend fun discard(sessionId: String, images: List<AssistantImage>) = Unit
            }, db.guideCreationDao()::referencedMaterials)
        init { config.save(ProviderConfig("https://example.invalid", "fixture", "not-real")) }
        suspend fun model(creation: GuideTripCreation = creator,
            destination: suspend (GuideMaterials) -> GuideDestinationResult = {
                GuideDestinationResult(listOf(GuideCityEvidence("青州", "text", "青州")), "青州之旅")
            },
            dates: suspend (TripDraftRequest) -> TripDraftMetadata = { TripDraftMetadata(TripDateSuggestion(null)) },
        ): GuideCreationViewModel {
            val vm = withContext(Dispatchers.Main) {
                GuideCreationViewModel(materials, config, creation, journal, destination, dates)
                    .also { stores.put("global", it) }
            }
            withTimeout(10_000) { vm.state.first { !it.recovering } }
            return vm
        }
        suspend fun confirmed(vm: GuideCreationViewModel, input: String = "青州三日游 https://xhslink.cn/o/example") {
            withContext(Dispatchers.Main) { vm.editInput(input); vm.identify() }
            withTimeout(10_000) { vm.state.first { !it.busy && it.confirming } }
        }
        suspend fun close() {
            withContext(Dispatchers.Main) { stores.clear() }
            delay(100)
            if (original == null) config.clear() else config.save(original)
            db.close()
            root.deleteRecursively()
        }
    }
    private fun exercise(block: suspend Fixture.() -> Unit) = runBlocking {
        val fixture = Fixture()
        try { fixture.block() } finally { fixture.close() }
    }

    @Test fun confirmedDraftReopensWithManualCitiesAndOptionalDatesWithoutNetwork() = exercise {
        val first = model(dates = { error("Share title must not invoke date model") })
        confirmed(first)
        withContext(Dispatchers.Main) {
            first.editCities("青州、济南")
            first.editName("我的旅行")
            first.editDates(LocalDate.of(2026, 10, 24), 3)
        }
        withTimeout(10_000) {
            while (withContext(Dispatchers.IO) { journal.read() }?.contains("我的旅行") != true) delay(20)
        }
        val restored = model(destination = { error("No automatic reinterpretation") })
        assertEquals("青州、济南", restored.state.value.cityText)
        assertEquals("我的旅行", restored.state.value.name)
        assertEquals(LocalDate.of(2026, 10, 24), restored.state.value.startDate)
        assertEquals(3, restored.state.value.dayCount)
        assertTrue(restored.state.value.canCreate)
        assertTrue(trips.observeTrips().first().isEmpty())
        withContext(Dispatchers.Main) { restored.backToInput(); restored.identify() }
        withTimeout(10_000) { restored.state.first { !it.busy } }
        assertTrue(restored.state.value.confirming)
        assertEquals("青州、济南", restored.state.value.cityText)
        assertEquals("我的旅行", restored.state.value.name)
        assertEquals(LocalDate.of(2026, 10, 24), restored.state.value.startDate)
    }

    @Test fun commitWithLostReturnRecoversReceiptAndDoesNotCreateTwiceOrResurrect() = exercise {
        var commits = 0
        val service = object : GuideTripCreation {
            override suspend fun recover(command: CreateGuideTripCommand) = creator.recover(command)
            override suspend fun commit(command: CreateGuideTripCommand): GuideCreationReceipt {
                commits++
                creator.commit(command)
                error("Simulate lost commit response")
            }
        }
        val vm = model(service)
        confirmed(vm)
        withContext(Dispatchers.Main) { vm.create(); vm.create() }
        val created = withTimeout(10_000) { vm.state.first { it.createdTripId != null && !it.saving } }
        assertEquals(1, commits)
        assertEquals(1, trips.observeTrips().first().size)
        assertEquals(0, trips.observeTrips().first().single().placeCount)
        val restored = model(service)
        assertEquals(created.createdTripId, restored.state.value.createdTripId)
        trips.deleteTrip(requireNotNull(created.createdTripId))
        val deleted = model(service)
        assertTrue(deleted.state.value.deleted)
        withContext(Dispatchers.Main) { deleted.create() }
        assertEquals(1, commits)
        assertTrue(trips.observeTrips().first().isEmpty())
    }

    @Test fun cancellationRejectsLateDestinationWithoutOverwritingNewInput() = exercise {
        val started = CompletableDeferred<Unit>()
        val late = CompletableDeferred<GuideDestinationResult>()
        val vm = model(destination = {
            started.complete(Unit)
            withContext(NonCancellable) { late.await() }
        })
        withContext(Dispatchers.Main) { vm.editInput("青州公园"); vm.identify() }
        withTimeout(10_000) { started.await() }
        withContext(Dispatchers.Main) { vm.cancel(); vm.editInput("济南公园") }
        late.complete(GuideDestinationResult(listOf(GuideCityEvidence("青州", "text", "青州")), "旧结果"))
        delay(200)
        assertEquals("济南公园", vm.state.value.input)
        assertEquals("", vm.state.value.cityText)
        assertFalse(vm.state.value.confirming)
        assertTrue(trips.observeTrips().first().isEmpty())
    }

    @Test fun missingPreparedMaterialRetainsTextForReidentification() = exercise {
        val vm = model()
        confirmed(vm)
        materials.release(vm.sessionId)
        val restored = model()
        assertFalse(restored.state.value.confirming)
        assertFalse(restored.state.value.recoveryFailed)
        assertTrue(restored.state.value.input.contains("xhslink.cn"))
        withContext(Dispatchers.Main) { restored.identify() }
        withTimeout(10_000) { restored.state.first { it.confirming && !it.busy } }
        assertTrue(restored.state.value.canCreate)
    }

    @Test fun malformedJournalLocksWithoutOverwritingAndDoesNotCreate() = exercise {
        file.parentFile!!.mkdirs()
        file.writeText("not-json")
        val vm = model()
        assertTrue(vm.state.value.recoveryFailed)
        withContext(Dispatchers.Main) { vm.editInput("不能覆盖"); vm.identify(); vm.create() }
        assertEquals("not-json", file.readText())
        assertTrue(trips.observeTrips().first().isEmpty())
    }
}
