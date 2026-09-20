package com.yangchengwei.easytrip.route.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TimeMode
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.domain.PolylineCodec
import com.yangchengwei.easytrip.route.domain.RouteErrorKind
import com.yangchengwei.easytrip.route.domain.RouteResult
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FlexibleRouteDefaultsUpdateTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "route-defaults-${UUID.randomUUID()}.db"
    private var database: EasyTripDatabase? = null
    private val now = Instant.EPOCH

    @After fun close() {
        database?.close()
        context.deleteDatabase(name)
    }

    @Test fun reopeningUpdatesAutomaticDefaultsOnceAndPreservesAllManualChoices() = runTest {
        var db = open(update = false)
        val originals = linkedMapOf<String, RouteLegEntity>()
        for (status in RouteStatus.entries) {
            val id = "auto-$status"
            originals[id] = seed(db, id, TravelMode.FLEXIBLE, TransportMode.TRANSIT, null, status)
        }
        for (mode in TransportMode.entries) {
            val id = "manual-$mode"
            originals[id] = seed(db, id, TravelMode.FLEXIBLE, TransportMode.TRANSIT, mode)
        }
        originals["self-drive"] = seed(db, "self-drive", TravelMode.SELF_DRIVE, TransportMode.TRANSIT, null)
        originals["walk"] = seed(db, "walk", TravelMode.FLEXIBLE, TransportMode.WALK, null)
        originals["taxi"] = seed(db, "taxi", TravelMode.FLEXIBLE, TransportMode.TAXI, null)
        originals["manual-transit-new-default"] = seed(db, "manual-transit-new-default", TravelMode.FLEXIBLE, TransportMode.TAXI, TransportMode.TRANSIT)
        val items = originals.keys.associateWith { db.itineraryEditingDao().items(it) }

        db.close()
        db = open(update = true)
        val expected = originals.mapValues { (id, leg) ->
            when {
                id.startsWith("auto-") -> leg.copy(
                    recommendedMode = TransportMode.TAXI, version = leg.version + 1,
                    status = RouteStatus.PENDING, distanceMeters = null, durationSeconds = null,
                    polyline = null, errorKind = null, errorCode = null,
                )
                id.startsWith("manual-") && id != "manual-transit-new-default" -> leg.copy(recommendedMode = TransportMode.TAXI)
                else -> leg
            }
        }
        for ((id, leg) in expected) {
            assertEquals(id, leg, db.routeLegDao().leg(id))
            assertEquals(items[id], db.itineraryEditingDao().items(id))
        }
        val repository = RoomRouteLegRepository(db.routeLegDao())
        assertFalse(repository.completeIfVersionMatches(
            "auto-CALCULATING", 7, RouteResult(10, 20, listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0))),
        ))

        db.close()
        db = open(update = true)
        for ((id, leg) in expected) assertEquals("second open: $id", leg, db.routeLegDao().leg(id))

        // Clearing a manual transit override must now use the updated taxi default.
        db.routeLegDao().updateDetails("manual-TRANSIT", null, 900, "保留备注", false)
        val restored = requireNotNull(db.routeLegDao().leg("manual-TRANSIT"))
        assertEquals(TransportMode.TAXI, restored.recommendedMode)
        assertEquals(null, restored.selectedMode)
        assertEquals(RouteStatus.WAITING_NETWORK, restored.status)
        assertEquals("保留备注", restored.note)
    }

    private fun open(update: Boolean): EasyTripDatabase = Room.databaseBuilder(context, EasyTripDatabase::class.java, name)
        .apply { if (update) addCallback(FlexibleRouteDefaultsUpdate) }
        .build().also { database = it }

    private suspend fun seed(
        db: EasyTripDatabase,
        id: String,
        travelMode: TravelMode,
        recommended: TransportMode,
        selected: TransportMode?,
        status: RouteStatus = RouteStatus.SUCCESS,
    ): RouteLegEntity {
        db.tripDao().insertTrip(TripEntity(id, id, TimeMode.DRAFT, null, travelMode, now, now))
        db.tripDao().insertDay(TripDayEntity(id, id, 0))
        for (index in 0..1) {
            val placeId = "$id-$index"
            db.savedPlaceDao().insertPlace(SavedPlaceEntity(placeId, id, placeId, placeId, "", 0.0, index.toDouble()))
            db.itineraryEditingDao().insertItem(ItineraryItemEntity(placeId, id, id, placeId, index * 1000L, note = "行程备注"))
        }
        val leg = RouteLegEntity(id, id, "$id-0", "$id-1", recommended, selected, status,
            distanceMeters = 120_000, durationSeconds = 3600,
            polyline = PolylineCodec.encode(listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0))),
            errorKind = RouteErrorKind.NO_ROUTE, errorCode = "old", version = 7, updatedAt = now,
            durationOverrideSeconds = 900, note = "保留备注")
        db.routeLegDao().insert(leg)
        return leg
    }
}
