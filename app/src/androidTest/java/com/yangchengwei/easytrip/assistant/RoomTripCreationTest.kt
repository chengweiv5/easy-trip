package com.yangchengwei.easytrip.assistant

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.assistant.data.RoomTripCreation
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class RoomTripCreationTest {
    private lateinit var db: EasyTripDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build() }
    @After fun close() { db.close() }
    private fun command() = TripCreationCommand.create("新旅行", null, 3, TravelMode.FLEXIBLE,
        listOf("A", "B").map { ImportItem(it, 0, PlaceCandidate(it, "地点$it", "地址", GeoPoint(30.2, 120.1), null, "杭州"), PlaceCategory.ATTRACTION) },
        listOf("作者推荐"), "create-trip:v1:op", "new-trip")

    @Test fun createsOnlyOnceAndNeverOverwritesLaterEditsOrResurrectsDeletedTrip() = runTest {
        val creator = RoomTripCreation(db)
        val trips = RoomTripRepository(db.tripDao(), database = db)
        val cmd = command()
        assertEquals(TripCreationRecovery.NotSubmitted, creator.recover(cmd))
        val result = creator.commit(cmd)
        assertEquals(2, result.addedCount)
        assertEquals(1, trips.observeTrips().first().size)
        assertEquals(3, trips.observeTrip(cmd.tripId).first()!!.days.size)
        assertNull(trips.observeTrip(cmd.tripId).first()!!.startDate)
        assertEquals(0, trips.observeTrips().first().single().scheduledDayCount)
        assertEquals(2, RoomSavedPlaceRepository(db).observePlaces(cmd.tripId, emptySet()).first().size)
        trips.renameTrip(cmd.tripId, "后来改名")
        assertEquals(result, creator.commit(cmd))
        assertEquals("后来改名", trips.observeTrip(cmd.tripId).first()!!.name)
        val different = TripCreationCommand.create("不同命令", null, 3, TravelMode.FLEXIBLE, cmd.items, cmd.sources, cmd.operationId, cmd.tripId)
        assertTrue(runCatching { creator.commit(different) }.isFailure)
        trips.deleteTrip(cmd.tripId)
        assertEquals(TripCreationRecovery.Deleted, RoomTripCreation(db).recover(cmd))
        assertTrue(runCatching { creator.commit(cmd) }.isFailure)
        assertTrue(trips.observeTrips().first().isEmpty())
    }

    @Test fun failureAfterCreatingDaysRollsBackAllRowsAndCanRetrySameCommand() = runTest {
        var count = 0
        val cmd = command()
        val creator = RoomTripCreation(db, idFactory = { if (++count == 5) error("injected failure") else "generated-$count" })
        assertTrue(runCatching { creator.commit(cmd) }.isFailure)
        assertTrue(RoomTripRepository(db.tripDao()).observeTrips().first().isEmpty())
        assertEquals(TripCreationRecovery.NotSubmitted, creator.recover(cmd))
        assertEquals(2, RoomTripCreation(db).commit(cmd).addedCount)
    }
}
