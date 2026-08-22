package com.yangchengwei.easytrip.trip.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceTagCrossRef
import com.yangchengwei.easytrip.place.data.TagEntity
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDeleteImpactProviderTest {
    private lateinit var database: EasyTripDatabase
    private lateinit var provider: RoomDeleteImpactProvider

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            EasyTripDatabase::class.java,
        ).build()
        provider = RoomDeleteImpactProvider(database.deleteImpactDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun countsStructuredTripAndDayImpactWithoutCrossTripLeakageAndUpdatesAfterDelete() = runTest {
        val now = Instant.parse("2026-08-22T00:00:00Z")
        database.tripDao().insertTrip(TripEntity("trip-1", "First", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, now, now))
        database.tripDao().insertTrip(TripEntity("trip-2", "Second", TimeMode.DRAFT, null, TravelMode.FLEXIBLE, now, now))
        database.tripDao().insertDay(TripDayEntity("day-1", "trip-1", 0))
        database.tripDao().insertDay(TripDayEntity("day-2", "trip-1", 1_000))
        database.tripDao().insertDay(TripDayEntity("other-day", "trip-2", 0))

        insertPlace("place-1", "trip-1")
        insertPlace("place-2", "trip-1")
        insertPlace("other-place", "trip-2")
        insertTag("tag-1", "trip-1")
        insertTag("tag-2", "trip-1")
        insertTag("other-tag", "trip-2")
        database.placeDao().insertCrossRef(SavedPlaceTagCrossRef("place-1", "tag-1", "trip-1"))
        database.placeDao().insertCrossRef(SavedPlaceTagCrossRef("place-2", "tag-2", "trip-1"))
        database.placeDao().insertCrossRef(SavedPlaceTagCrossRef("other-place", "other-tag", "trip-2"))

        insertItem("item-1", "day-1", "trip-1", "place-1", 0)
        insertItem("item-2", "day-1", "trip-1", "place-2", 1_000)
        insertItem("item-3", "day-2", "trip-1", "place-1", 0)
        insertItem("other-item", "other-day", "trip-2", "other-place", 0)
        database.routeDao().insertLeg(routeLeg("leg-1", "day-1", "item-1", "item-2", now))

        assertEquals(TripDeleteImpact(2, 2, 2, 3, 1), provider.trip("trip-1"))
        assertEquals(DayDeleteImpact(2, 1), provider.day("day-1"))
        assertEquals(TripDeleteImpact(1, 1, 1, 1, 0), provider.trip("trip-2"))

        database.itineraryDao().deleteItem("item-2")

        assertEquals(TripDeleteImpact(2, 2, 2, 2, 0), provider.trip("trip-1"))
        assertEquals(DayDeleteImpact(1, 0), provider.day("day-1"))
        assertEquals(TripDeleteImpact(1, 1, 1, 1, 0), provider.trip("trip-2"))
    }

    private fun insertPlace(id: String, tripId: String) {
        database.placeDao().insertSavedPlace(
            SavedPlaceEntity(id, tripId, "poi-$id", id, "address", 1.0, 2.0),
        )
    }

    private fun insertTag(id: String, tripId: String) {
        database.placeDao().insertTag(TagEntity(id, tripId, id, id))
    }

    private fun insertItem(
        id: String,
        dayId: String,
        tripId: String,
        placeId: String,
        position: Long,
    ) {
        database.itineraryDao().insertItem(
            ItineraryItemEntity(id, dayId, tripId, placeId, position),
        )
    }

    private fun routeLeg(
        id: String,
        dayId: String,
        fromItemId: String,
        toItemId: String,
        now: Instant,
    ) = RouteLegEntity(
        id = id,
        tripDayId = dayId,
        fromItemId = fromItemId,
        toItemId = toItemId,
        recommendedMode = TransportMode.DRIVE,
        status = RouteStatus.SUCCESS,
        updatedAt = now,
    )
}
