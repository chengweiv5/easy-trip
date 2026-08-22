package com.yangchengwei.easytrip

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.data.TagEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceTagCrossRef
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.PlaceService
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CascadeDeleteTest {
    private lateinit var database: EasyTripDatabase
    private var id = 0

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
    }

    @After fun tearDown() = database.close()

    @Test fun deletingReferencedPlaceRepairsEachDayAndKeepsOtherPlaces() = runTest {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${id++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "id-${id++}" })
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), itemIdFactory = { "item-${id++}" }, legIdFactory = { "leg-${id++}" })
        val tripId = trips.createTrip(CreateTrip("Trip", 2))
        val days = trips.observeTrip(tripId).first()!!.days
        suspend fun save(name: String) = (places.save(tripId, PlaceCandidate(name, name, "", GeoPoint(1.0, id.toDouble()), null)) as SavePlaceResult.Saved).id
        val a = save("a"); val removed = save("hotel"); val c = save("c")
        val firstA = itineraries.addItem(days[0].id, a, 0)
        repeat(3) { itineraries.addItem(days[0].id, removed, it + 1) }
        val firstC = itineraries.addItem(days[0].id, c, 4)
        repeat(2) { itineraries.addItem(days[1].id, removed, it) }

        val service = PlaceService(places)
        assertEquals(5, service.deletionUsageCount(removed))
        service.deletePlaceAndReferences(removed)

        assertEquals(listOf(firstA, firstC), database.itineraryEditingDao().items(days[0].id).map { it.id })
        assertEquals(emptyList<String>(), database.itineraryEditingDao().items(days[1].id).map { it.id })
        val bridge = database.routeLegDao().legs(days[0].id).single()
        assertEquals(firstA to firstC, bridge.fromItemId to bridge.toItemId)
        assertEquals(setOf(a, c), places.observePlaces(tripId, emptySet()).first().map { it.id }.toSet())
    }

    @Test fun deletingTripCascadesAllOwnedRows() = runTest {
        val trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${id++}" })
        val places = RoomSavedPlaceRepository(database, idFactory = { "id-${id++}" })
        val itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao())
        val tripId = trips.createTrip(CreateTrip("Trip", 1))
        val day = trips.observeTrip(tripId).first()!!.days.single().id
        val place = (places.save(tripId, PlaceCandidate("p", "P", "", GeoPoint(1.0, 2.0), null)) as SavePlaceResult.Saved).id
        val second = (places.save(tripId, PlaceCandidate("q", "Q", "", GeoPoint(3.0, 4.0), null)) as SavePlaceResult.Saved).id
        itineraries.addItem(day, place, 0)
        itineraries.addItem(day, second, 1)
        places.updateDetails(place, "", setOf("tag"))
        trips.deleteTrip(tripId)
        val counts = database.cascadeCountDao()
        assertEquals(0, counts.trips(tripId))
        assertEquals(0, counts.days(tripId))
        assertEquals(0, counts.places(tripId))
        assertEquals(0, counts.tags(tripId))
        assertEquals(0, counts.crossRefs(tripId))
        assertEquals(0, counts.items(tripId))
        assertEquals(0, counts.legs(tripId))
    }
}
