package com.yangchengwei.easytrip.place.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSavedPlaceRepositoryTest {
    private lateinit var database: EasyTripDatabase
    private lateinit var trips: RoomTripRepository
    private lateinit var places: RoomSavedPlaceRepository
    private var id = 0

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java).build()
        trips = RoomTripRepository(database.tripDao(), idFactory = { "id-${id++}" })
        places = RoomSavedPlaceRepository(database, idFactory = { "id-${id++}" })
    }
    @After fun tearDown() = database.close()

    @Test fun duplicatePoiInOneTripReturnsExistingIdButDifferentTripsCanSave() = runTest {
        val firstTrip = trips.createTrip(CreateTrip("First", 1))
        val secondTrip = trips.createTrip(CreateTrip("Second", 1))
        val first = places.save(firstTrip, candidate("poi")) as SavePlaceResult.Saved
        val duplicate = places.save(firstTrip, candidate("poi")) as SavePlaceResult.AlreadySaved
        val other = places.save(secondTrip, candidate("poi")) as SavePlaceResult.Saved
        assertEquals(first.id, duplicate.existingId)
        assertNotEquals(first.id, other.id)
    }

    @Test fun tagsAreTrimmedUnicodeNormalizedAndCaseFoldedWhileDisplayKeepsTrimmedText() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val saved = places.save(trip, candidate("a")) as SavePlaceResult.Saved
        places.updateDetails(saved.id, "note", setOf("  Café  ", "CAFE\u0301"))
        val value = places.observePlaces(trip, emptySet()).first().single()
        assertEquals(listOf("Café"), value.tags.map { it.name })
        assertEquals("note", value.note)
    }

    @Test fun unicodeCaseFoldingTreatsSharpSAndSsAsOneTag() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val saved = places.save(trip, candidate("a")) as SavePlaceResult.Saved
        places.updateDetails(saved.id, "", setOf("Straße", "STRASSE"))
        assertEquals(listOf("Straße"), places.observePlaces(trip, emptySet()).first().single().tags.map { it.name })
    }

    @Test fun invalidTagUpdateIsRejectedBeforeAnyExistingDetailsChange() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val saved = places.save(trip, candidate("a")) as SavePlaceResult.Saved
        places.updateDetails(saved.id, "original", setOf("kept"))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                places.updateDetails(saved.id, "changed", setOf("", "new"))
            }
        }

        val value = places.observePlaces(trip, emptySet()).first().single()
        assertEquals("original", value.note)
        assertEquals(listOf("kept"), value.tags.map { it.name })
    }

    @Test fun tagOverTwentyFourUnitsIsRejectedAtomically() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val saved = places.save(trip, candidate("a")) as SavePlaceResult.Saved
        places.updateDetails(saved.id, "original", setOf("kept"))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                places.updateDetails(saved.id, "changed", setOf("一二三四五六七八九十天地人"))
            }
        }

        val value = places.observePlaces(trip, emptySet()).first().single()
        assertEquals("original", value.note)
        assertEquals(listOf("kept"), value.tags.map { it.name })
    }

    @Test fun moreThanEightTagsIsRejectedAtomically() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val saved = places.save(trip, candidate("a")) as SavePlaceResult.Saved
        places.updateDetails(saved.id, "original", setOf("kept"))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                places.updateDetails(saved.id, "changed", (1..9).mapTo(mutableSetOf()) { "tag-$it" })
            }
        }

        val value = places.observePlaces(trip, emptySet()).first().single()
        assertEquals("original", value.note)
        assertEquals(listOf("kept"), value.tags.map { it.name })
    }

    @Test fun validReplacementRemainsAtomicAndDeletesOrphanTags() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val first = places.save(trip, candidate("a")) as SavePlaceResult.Saved
        val second = places.save(trip, candidate("b")) as SavePlaceResult.Saved
        places.updateDetails(first.id, "", setOf("shared", "orphan"))
        places.updateDetails(second.id, "", setOf("shared"))

        places.updateDetails(first.id, "updated", setOf("new"))

        assertEquals(setOf("shared", "new"), places.observeTags(trip).first().mapTo(mutableSetOf()) { it.name })
        assertEquals(setOf("new"), places.observePlaces(trip, emptySet()).first().first { it.id == first.id }.tags.mapTo(mutableSetOf()) { it.name })
    }

    @Test fun multipleTagFilterUsesIntersectionAndNeverCrossesTrips() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val otherTrip = trips.createTrip(CreateTrip("Other", 1))
        val both = places.save(trip, candidate("both")) as SavePlaceResult.Saved
        val one = places.save(trip, candidate("one")) as SavePlaceResult.Saved
        val other = places.save(otherTrip, candidate("other")) as SavePlaceResult.Saved
        places.updateDetails(both.id, "", setOf("food", "night"))
        places.updateDetails(one.id, "", setOf("food"))
        places.updateDetails(other.id, "", setOf("food", "night"))
        val tags = places.observeTags(trip).first().associateBy { it.name }
        val filtered = places.observePlaces(trip, setOf(tags.getValue("food").id, tags.getValue("night").id)).first()
        assertEquals(listOf(both.id), filtered.map { it.id })
    }

    @Test fun usageCountsRefreshWhenOnlyItineraryItemsChange() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val day = trips.observeTrip(trip).first()!!.days.single()
        val saved = places.save(trip, candidate("used")) as SavePlaceResult.Saved
        val next = async(start = CoroutineStart.UNDISPATCHED) {
            places.observeUsageCounts(trip).first { it[saved.id] == 1 }
        }

        withContext(Dispatchers.IO) {
            database.itineraryDao().insertItem(
                ItineraryItemEntity("item", day.id, trip, saved.id, 0),
            )
        }

        val usageCounts = withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) { next.await() }
        }
        assertEquals(1, usageCounts.getValue(saved.id))
    }

    @Test fun deletionImpactIsZeroForUnusedPlaceAndDoesNotMutateIt() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val saved = places.save(trip, candidate("unused")) as SavePlaceResult.Saved

        assertEquals(com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(0, 0), places.deletionImpact(saved.id))
        assertEquals(saved.id, places.observePlaces(trip, emptySet()).first().single().id)
    }

    @Test fun deletionImpactCountsEveryItineraryOccurrence() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val day = trips.observeTrip(trip).first()!!.days.single()
        val saved = places.save(trip, candidate("used")) as SavePlaceResult.Saved
        repeat(3) { index -> database.itineraryDao().insertItem(ItineraryItemEntity("item-$index", day.id, trip, saved.id, index * 1_000L)) }

        assertEquals(com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(3, 0), places.deletionImpact(saved.id))
        assertEquals(3, database.itineraryDao().countBySavedPlace(saved.id))
    }

    @Test fun deletionImpactCountsDistinctIncidentLegsWithoutDoubleCountingBothEndpoints() = runTest {
        val trip = trips.createTrip(CreateTrip("Trip", 1))
        val day = trips.observeTrip(trip).first()!!.days.single()
        val saved = places.save(trip, candidate("used")) as SavePlaceResult.Saved
        val other = places.save(trip, candidate("other")) as SavePlaceResult.Saved
        val first = ItineraryItemEntity("first", day.id, trip, saved.id, 0)
        val second = ItineraryItemEntity("second", day.id, trip, saved.id, 1_000)
        val third = ItineraryItemEntity("third", day.id, trip, other.id, 2_000)
        listOf(first, second, third).forEach { database.itineraryDao().insertItem(it) }
        insertLeg("both-ends", day.id, first.id, second.id)
        insertLeg("one-end", day.id, second.id, third.id)

        assertEquals(com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(2, 2), places.deletionImpact(saved.id))
        assertEquals(3, database.itineraryEditingDao().items(day.id).size)
        assertEquals(2, database.routeLegDao().legs(day.id).size)
    }

    private suspend fun insertLeg(id: String, dayId: String, from: String, to: String) {
        database.routeLegDao().insert(
            RouteLegEntity(id, dayId, from, to, TransportMode.WALK, status = RouteStatus.PENDING, updatedAt = Instant.EPOCH),
        )
    }

    private fun candidate(id: String) = PlaceCandidate(id, "Name $id", "Address $id", GeoPoint(39.9, 116.4), "010")
}
