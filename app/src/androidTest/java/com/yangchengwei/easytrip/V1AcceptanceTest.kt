package com.yangchengwei.easytrip

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepository
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.workspace.DayMapSnapshot
import com.yangchengwei.easytrip.workspace.MapScope
import com.yangchengwei.easytrip.workspace.MapUiModelMapper
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V1AcceptanceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "v1-acceptance.db"
    private val ids = AtomicInteger()
    private lateinit var database: EasyTripDatabase

    @Before fun setUp() {
        context.deleteDatabase(databaseName)
        database = open()
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes() = runTest {
        var trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${ids.getAndIncrement()}" })
        var places = RoomSavedPlaceRepository(database, idFactory = { "id-${ids.getAndIncrement()}" })
        var itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao(), itemIdFactory = { "item-${ids.getAndIncrement()}" }, legIdFactory = { "leg-${ids.getAndIncrement()}" })
        var routes = RoomRouteLegRepository(database.routeLegDao())
        val tripId = trips.createTrip(CreateTrip("三日旅行", 3))
        val days = trips.observeTrip(tripId).first()!!.days
        val saved = (0 until 10).associateWith { index ->
            (places.save(tripId, PlaceCandidate("poi-$index", if (index == 0) "酒店" else "地点 $index", "地址 $index", GeoPoint(30.0 + index / 100.0, 104.0 + index / 100.0), "028")) as SavePlaceResult.Saved).id
        }
        days.forEach { day ->
            itineraries.addItem(day.id, saved.getValue(0), 0)
            itineraries.addItem(day.id, saved.getValue(0), 1)
        }
        val firstDay = days.first().id
        val third = itineraries.addItem(firstDay, saved.getValue(1), 2)
        val fourth = itineraries.addItem(firstDay, saved.getValue(2), 3)
        val before = database.routeLegDao().legs(firstDay)
        val stable = before.first()
        assertTrue(routes.claimIfVersionMatches(stable.id, stable.version))
        assertTrue(routes.completeIfVersionMatches(stable.id, stable.version, RouteResult(120, 90, listOf(GeoPoint(30.0, 104.0), GeoPoint(30.01, 104.01)))))

        itineraries.moveItem(fourth, firstDay, 2)
        itineraries.deleteItem(third)
        val after = database.routeLegDao().legs(firstDay)
        assertEquals(RouteStatus.SUCCESS, after.first { it.id == stable.id }.status)
        assertTrue(after.filterNot { it.id == stable.id }.all { it.status == RouteStatus.PENDING })
        val changed = after.last()
        assertTrue(routes.updateDetails(changed.id, TransportMode.WALK, null, null, online = true))
        assertEquals(TransportMode.WALK, database.routeLegDao().legs(firstDay).last().selectedMode)

        val snapshots = days.map { day -> DayMapSnapshot(itineraries.observeDay(day.id).first(), database.routeLegDao().observeLegs(day.id).first()) }
        val single = MapUiModelMapper.map(MapScope.SINGLE_DAY, emptyList(), days, snapshots, firstDay)
        val whole = MapUiModelMapper.map(MapScope.WHOLE_TRIP, emptyList(), days, snapshots)
        assertTrue(single.markers.isNotEmpty())
        assertTrue(single.markers.flatMap { it.occurrences }.all { it.dayId == firstDay })
        assertTrue(whole.markers.any { it.occurrences.size >= 6 })
        assertEquals(days.map { it.id }.toSet(), whole.markers.flatMap { it.occurrences }.map { it.dayId }.toSet())

        database.close()
        database = open()
        trips = RoomTripRepository(database.tripDao())
        places = RoomSavedPlaceRepository(database)
        itineraries = RoomItineraryRepository(database, database.itineraryEditingDao(), database.routeLegDao())
        routes = RoomRouteLegRepository(database.routeLegDao())
        assertEquals(3, trips.observeTrip(tripId).first()!!.days.size)
        assertEquals(10, places.observePlaces(tripId, emptySet()).first().size)
        assertEquals(3, itineraries.observeDay(firstDay).first().items.size)
        assertNotNull(routes.get(stable.id))
        assertEquals(RouteStatus.SUCCESS, routes.get(stable.id)!!.status)
    }

    private fun open() = Room.databaseBuilder(context, EasyTripDatabase::class.java, databaseName).build()
}
