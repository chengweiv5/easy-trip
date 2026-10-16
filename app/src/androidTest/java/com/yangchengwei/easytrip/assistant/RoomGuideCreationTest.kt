package com.yangchengwei.easytrip.assistant

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.assistant.data.RoomGuideCreation
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import com.yangchengwei.easytrip.assistant.data.RoomPlaceImport
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class RoomGuideCreationTest {
    private lateinit var db: EasyTripDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build() }
    @After fun close() { db.close() }
    @Test fun lastWriteFailureRollsBackAndConcurrentCommitAndClaimRemainIdempotent() = runTest {
        val command = CreateGuideTripCommand.create("甲市之旅", null, 3, TravelMode.FLEXIBLE,
            AssistantCityScope(listOf("甲市")), GuideMaterialRef("c0408b2b-e236-4fa9-930a-b421d16292e2", "a".repeat(64)))
        val creation = RoomGuideCreation(db)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_guide_receipt BEFORE INSERT ON assistant_guide_creation_receipts BEGIN SELECT RAISE(ABORT,'fixture'); END")
        assertTrue(runCatching { creation.commit(command) }.isFailure)
        val trips = RoomTripRepository(db.tripDao(), database = db)
        assertTrue(trips.observeTrips().first().isEmpty())
        assertNull(creation.find(command.tripId))
        assertEquals(GuideCreationRecovery.NotSubmitted, creation.recover(command))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_guide_receipt")
        val receipts = coroutineScope { List(2) { async { creation.commit(command) } }.awaitAll() }
        assertEquals(receipts.first(), receipts.last())
        assertEquals(1, trips.observeTrips().first().size)
        val claims = coroutineScope { List(2) { async { creation.claim(command.operationId, command.digest, 0) } }.awaitAll() }
        assertEquals(1, claims.count { it != null })
        val retries = coroutineScope { List(2) { async { creation.requestRetry(command.operationId, 0) } }.awaitAll() }
        assertEquals(1, retries.count { it != null })
        assertEquals(1, creation.find(command.tripId)!!.attempt)
        assertFalse(creation.complete(command.operationId, 0, GuideHandoffStatus.FAILED, null))
    }
    @Test fun collectionFinishesHandoffAtomicallyAndLaterReceiptsDoNotHideIt() = runTest {
        val command = CreateGuideTripCommand.create("甲市之旅", null, 1, TravelMode.FLEXIBLE,
            AssistantCityScope(listOf("甲市")), GuideMaterialRef("c0408b2b-e236-4fa9-930a-b421d16292e2", "a".repeat(64)))
        val creation = RoomGuideCreation(db)
        creation.commit(command)
        creation.claim(command.operationId, command.digest, 0)
        val importer = RoomPlaceImport(db)
        val item = ImportItem("A", 0, PlaceCandidate("poi", "公园", "甲市", GeoPoint(30.0, 120.0), null, "甲市"), PlaceCategory.ATTRACTION)
        val receipt = importer.commit(ImportConfirmation.create(command.tripId, listOf(item), "guide:${command.operationId}:selection"))
        importer.commit(ImportConfirmation.create(command.tripId, listOf(item), "later-normal-batch"))
        assertEquals(receipt, importer.guideReceipt(command.operationId))
        assertEquals(GuideHandoffStatus.FINISHED, creation.find(command.tripId)!!.status)
        assertFalse(creation.complete(command.operationId, 0, GuideHandoffStatus.IMPORTED,
            "ef664cce-4c56-4dda-9d76-4ee4e6df8467"))
        assertNull(creation.requestRetry(command.operationId, 0))
        assertTrue(creation.complete(command.operationId, 0, GuideHandoffStatus.FINISHED, null))
    }
    @Test fun createsEmptyTripOnceAndClaimsOnceWithoutResurrectingDeletedTrip() = runTest {
        val c = CreateGuideTripCommand.create("甲市之旅", null, 3, TravelMode.FLEXIBLE,
            AssistantCityScope(listOf("甲市")), GuideMaterialRef("c0408b2b-e236-4fa9-930a-b421d16292e2", "a".repeat(64)))
        val service = RoomGuideCreation(db)
        val receipt = service.commit(c)
        assertEquals(receipt, service.commit(c))
        val trips = RoomTripRepository(db.tripDao(), database = db)
        assertEquals(1, trips.observeTrips().first().size)
        assertEquals(3, trips.observeTrip(c.tripId).first()!!.days.size)
        assertEquals(0, db.cascadeCountDao().places(c.tripId))
        assertEquals(listOf("甲市"), service.scope(c.tripId)!!.names)
        assertNotNull(service.claim(c.operationId, c.digest, 0))
        assertNull(service.claim(c.operationId, c.digest, 0))
        assertTrue(service.complete(c.operationId, 0, GuideHandoffStatus.FAILED, null))
        assertEquals(1, service.requestRetry(c.operationId, 0)!!.attempt)
        assertFalse(service.complete(c.operationId, 0, GuideHandoffStatus.IMPORTED,
            "6b43a5c0-182f-4c95-962d-520799f8d026"))
        trips.deleteTrip(c.tripId)
        assertEquals(GuideCreationRecovery.Deleted, service.recover(c))
        assertNull(service.find(c.tripId))
        assertTrue(runCatching { service.commit(c) }.isFailure)
    }
}
