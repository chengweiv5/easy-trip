package com.yangchengwei.easytrip.assistant

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
