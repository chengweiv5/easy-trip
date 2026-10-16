package com.yangchengwei.easytrip.assistant

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPlaceImportTest {
    private lateinit var db: EasyTripDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
    }
    @After fun close() = db.close()
    private fun item(id: String) = ImportItem(id, 0, PlaceCandidate(id, "地点$id", "地址$id",
        GeoPoint(30.2, 120.1), null, "杭州"), PlaceCategory.ATTRACTION)

    @Test fun controllerDefaultsMatchOrdinarySavesAndDoNotOverwriteExplicitOrExistingCategories() = runBlocking {
        val trip = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("收藏类型", 1))
        val places = RoomSavedPlaceRepository(db)
        val candidates = listOf("新景点", "手动其他", "已有地点", "普通收藏").associateWith { name ->
            PlaceCandidate(name, name, "地址", GeoPoint(30.2, 120.1), null, "杭州")
        }
        places.save(trip, candidates.getValue("已有地点"))
        val existing = places.observePlaces(trip, emptySet()).first().single()
        places.updateDetails(existing.id, "保留用户备注", emptySet(), PlaceCategory.FOOD)
        places.save(trip, candidates.getValue("普通收藏"))
        val roomImporter = RoomPlaceImport(db)
        val confirmations = mutableListOf<ImportConfirmation>()
        val controller = PlaceAssistantController(trip, this,
            PlaceIntentParser { _, _ ->
                listOf("新景点", "手动其他", "已有地点").map { PlaceIntent(it, "杭州", it) }
            },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?) =
                    listOf(candidates.getValue(keyword.removePrefix("杭州")))
            },
            object : PlaceImport by roomImporter {
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt {
                    confirmations += confirmation
                    return roomImporter.commit(confirmation)
                }
            })
        controller.environment(true, true, true)
        controller.editCity("杭州")
        controller.editInput("新景点、手动其他、已有地点")
        controller.submit()
        withTimeout(10_000) { controller.state.first { !it.busy && it.eligible.size == 3 } }
        controller.category(controller.state.value.items.single { it.poi?.poiId == "手动其他" }.id,
            PlaceCategory.OTHER)
        controller.selectAll()
        controller.saveSelected()
        val receipt = withTimeout(10_000) { controller.state.first { it.receipt != null }.receipt!! }
        assertEquals(2, receipt.addedCount)
        assertEquals(receipt, roomImporter.commit(confirmations.single()))
        val saved = places.observePlaces(trip, emptySet()).first().associateBy { it.amapPoiId }
        assertEquals(4, saved.size)
        assertEquals(PlaceCategory.ATTRACTION, saved.getValue("普通收藏").category)
        assertEquals(PlaceCategory.ATTRACTION, saved.getValue("新景点").category)
        assertEquals(PlaceCategory.OTHER, saved.getValue("手动其他").category)
        assertEquals(PlaceCategory.FOOD, saved.getValue("已有地点").category)
        assertEquals("保留用户备注", saved.getValue("已有地点").note)
    }

    @Test fun hundredPlacesCommitAtomicallyAndHundredFirstIsRejectedBeforeWrites() = runTest {
        val trip = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("一百地点", 1))
        val places = RoomSavedPlaceRepository(db)
        val importer = RoomPlaceImport(db)
        val hundred = (1..100).map { item("place-$it") }
        val confirmation = ImportConfirmation.create(trip, hundred, "hundred")
        val receipt = importer.commit(confirmation)
        assertEquals(100, receipt.addedCount)
        assertEquals(receipt, importer.commit(confirmation))
        assertEquals(100, places.observePlaces(trip, emptySet()).first().size)
        assertTrue(runCatching { ImportConfirmation.create(trip, hundred + item("101")) }.isFailure)
        assertEquals(100, places.observePlaces(trip, emptySet()).first().size)
    }

    @Test fun batchAndReceiptArePersistentAndIdempotentWithoutOverwritingExisting() = runTest {
        val trip = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("测试", 1))
        val places = RoomSavedPlaceRepository(db)
        val importer = RoomPlaceImport(db)
        val confirmation = ImportConfirmation.create(trip, listOf(item("A"), item("B")), "op")
        val receipt = importer.commit(confirmation)
        assertEquals(2, receipt.addedCount)
        assertEquals(receipt, RoomPlaceImport(db).receipt("op"))
        assertEquals(receipt, importer.commit(confirmation))
        val saved = places.observePlaces(trip, emptySet()).first()
        assertEquals(2, saved.size)
        assertTrue(saved.all { it.category == PlaceCategory.ATTRACTION })
        places.updateDetails(saved.first().id, "保留备注", emptySet(), PlaceCategory.FOOD)
        val next = importer.commit(ImportConfirmation.create(trip, listOf(item(saved.first().amapPoiId))))
        assertEquals(0, next.addedCount)
        assertEquals("保留备注", places.observePlaces(trip, emptySet()).first().first { it.id == saved.first().id }.note)
    }

    @Test fun invalidSecondItemRollsBackTheWholeBatch() = runTest {
        val trip = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("测试", 1))
        val invalid = item("B").let { it.copy(poi = it.poi.copy(point = null)) }
        val confirmation = ImportConfirmation.create(trip, listOf(item("A"), invalid), "bad")
        assertTrue(runCatching { RoomPlaceImport(db).commit(confirmation) }.isFailure)
        assertTrue(RoomSavedPlaceRepository(db).observePlaces(trip, emptySet()).first().isEmpty())
        assertNull(RoomPlaceImport(db).receipt("bad"))
    }

    @Test fun sameOperationCannotSaveDifferentPayloadAndRepeatedPoiIsOnePlace() = runTest {
        val trip = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("测试", 1))
        val importer = RoomPlaceImport(db)
        val a = item("A")
        importer.commit(ImportConfirmation.create(trip, listOf(a, a.copy(itemId = "B")), "same"))
        assertEquals(1, RoomSavedPlaceRepository(db).observePlaces(trip, emptySet()).first().size)
        assertTrue(runCatching {
            importer.commit(ImportConfirmation.create(trip, listOf(item("C")), "same"))
        }.isFailure)
    }
    @Test fun storageFailureAfterFirstInsertRollsBackPlacesAndReceipt() = runTest {
        val trip = RoomTripRepository(db.tripDao()).createTrip(CreateTrip("测试", 1))
        var ids = 0
        val importer = RoomPlaceImport(db) { if (++ids == 2) error("injected second-write failure") else "first" }
        assertTrue(runCatching { importer.commit(ImportConfirmation.create(trip, listOf(item("A"), item("B")), "failed-write")) }.isFailure)
        assertTrue(RoomSavedPlaceRepository(db).observePlaces(trip, emptySet()).first().isEmpty())
        assertNull(importer.receipt("failed-write"))
    }

    @Test fun fileDatabaseReopensWithActualReceiptAndNoDuplicateWrite() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "v3-receipt-reopen-test"
        context.deleteDatabase(name)
        val disk = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
        val trip = RoomTripRepository(disk.tripDao()).createTrip(CreateTrip("回读", 1))
        val confirmation = ImportConfirmation.create(trip, listOf(item("persist")), "persist-op")
        val result = RoomPlaceImport(disk).commit(confirmation)
        disk.close()
        val reopened = Room.databaseBuilder(context, EasyTripDatabase::class.java, name).build()
        try {
            assertEquals(result, RoomPlaceImport(reopened).latestReceipt(trip))
            assertEquals(result, RoomPlaceImport(reopened).commit(confirmation))
            assertEquals(1, RoomSavedPlaceRepository(reopened).observePlaces(trip, emptySet()).first().size)
        } finally { reopened.close(); context.deleteDatabase(name) }
    }

}
