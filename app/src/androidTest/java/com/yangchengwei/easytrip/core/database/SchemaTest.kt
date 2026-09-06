package com.yangchengwei.easytrip.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.itinerary.data.ItineraryItemEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.place.data.TagEntity
import com.yangchengwei.easytrip.place.data.SavedPlaceTagCrossRef
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.trip.data.TripDayEntity
import com.yangchengwei.easytrip.trip.data.TripEntity
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchemaTest {
    private lateinit var db: EasyTripDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EasyTripDatabase::class.java)
            .allowMainThreadQueries().build()
    }
    @After fun tearDown() = db.close()

    @Test fun savedPlaceCanBeReferencedByMultipleItineraryItems() {
        val tripId = id(); val firstDayId = id(); val secondDayId = id(); val placeId = id()
        db.tripDao().insertTrip(trip(tripId))
        db.tripDao().insertDay(TripDayEntity(firstDayId, tripId, 1_000L))
        db.tripDao().insertDay(TripDayEntity(secondDayId, tripId, 2_000L))
        db.placeDao().insertSavedPlace(place(placeId, tripId, "poi-1"))
        db.itineraryDao().insertItem(item(firstDayId, tripId, placeId, 1_000L))
        db.itineraryDao().insertItem(item(secondDayId, tripId, placeId, 1_000L))
        assertEquals(2, db.itineraryDao().countBySavedPlace(placeId))
    }

    @Test fun itineraryItemCannotReferencePlaceFromAnotherTrip() {
        val tripA = id(); val tripB = id(); val dayA = id(); val placeB = id()
        insertTripDay(tripA, dayA)
        db.tripDao().insertTrip(trip(tripB))
        db.placeDao().insertSavedPlace(place(placeB, tripB, "poi-b"))
        assertThrows(SQLiteConstraintException::class.java) {
            db.itineraryDao().insertItem(item(dayA, tripA, placeB, 1_000L))
        }
    }

    @Test fun routeLegRejectsCrossDayEndpointAndAcceptsSameDayEndpoints() {
        val tripId = id(); val dayA = id(); val dayB = id(); val placeA = id(); val placeB = id()
        db.tripDao().insertTrip(trip(tripId))
        db.tripDao().insertDay(TripDayEntity(dayA, tripId, 1_000L)); db.tripDao().insertDay(TripDayEntity(dayB, tripId, 2_000L))
        db.placeDao().insertSavedPlace(place(placeA, tripId, "poi-a")); db.placeDao().insertSavedPlace(place(placeB, tripId, "poi-b"))
        val from = item(dayA, tripId, placeA, 1_000L); val sameDayTo = item(dayA, tripId, placeB, 2_000L); val otherDay = item(dayB, tripId, placeB, 1_000L)
        db.itineraryDao().insertItem(from); db.itineraryDao().insertItem(sameDayTo); db.itineraryDao().insertItem(otherDay)
        db.routeDao().insertLeg(leg(dayA, from.id, sameDayTo.id))
        assertEquals(1, db.routeDao().count())
        assertThrows(SQLiteConstraintException::class.java) { db.routeDao().insertLeg(leg(dayA, from.id, otherDay.id)) }
    }

    @Test fun duplicateRouteLegViolatesUniqueConstraint() {
        val fixture = sameDayFixture()
        db.routeDao().insertLeg(leg(fixture.dayId, fixture.fromId, fixture.toId))
        assertThrows(SQLiteConstraintException::class.java) { db.routeDao().insertLeg(leg(fixture.dayId, fixture.fromId, fixture.toId)) }
    }

    @Test fun deletingPlaceItemAndTripCascadesWithoutErrors() {
        val placeFixture = sameDayFixture()
        db.routeDao().insertLeg(leg(placeFixture.dayId, placeFixture.fromId, placeFixture.toId))
        db.placeDao().deleteSavedPlace(placeFixture.fromPlaceId)
        assertEquals(0, db.itineraryDao().countById(placeFixture.fromId)); assertEquals(0, db.routeDao().count())

        val itemFixture = sameDayFixture()
        db.routeDao().insertLeg(leg(itemFixture.dayId, itemFixture.fromId, itemFixture.toId))
        db.itineraryDao().deleteItem(itemFixture.toId)
        assertEquals(0, db.routeDao().count())

        val tripFixture = sameDayFixture()
        db.routeDao().insertLeg(leg(tripFixture.dayId, tripFixture.fromId, tripFixture.toId))
        db.tripDao().deleteTrip(tripFixture.tripId)
        assertEquals(0, db.tripDao().countDays(tripFixture.tripId)); assertEquals(0, db.placeDao().countByTrip(tripFixture.tripId)); assertEquals(0, db.routeDao().count())
    }

    @Test fun tripsSchemaStoresOnlyStartDateForDateRanges() {
        val cursor = db.openHelper.readableDatabase.query("PRAGMA table_info(trips)")
        val columns = buildSet {
            cursor.use {
                while (it.moveToNext()) add(it.getString(it.getColumnIndexOrThrow("name")))
            }
        }

        assertEquals(true, "startDate" in columns)
        assertEquals(false, "endDate" in columns)
    }

    @Test fun instantConverterUsesExactEpochMilliseconds() {
        val expected = Instant.ofEpochMilli(1_725_000_123_456L)
        val converters = Converters()
        assertEquals(expected, converters.longToInstant(converters.instantToLong(expected)))
    }

    @Test fun savedPlaceTagCrossRefEnforcesSameTripAndCascades() {
        val tripA=id(); val tripB=id(); val placeA=id(); val tagA=id(); val tagB=id()
        db.tripDao().insertTrip(trip(tripA)); db.tripDao().insertTrip(trip(tripB))
        db.placeDao().insertSavedPlace(place(placeA,tripA,"poi-a"))
        db.placeDao().insertTag(TagEntity(tagA,tripA,"A","a")); db.placeDao().insertTag(TagEntity(tagB,tripB,"B","b"))
        assertThrows(SQLiteConstraintException::class.java) { db.placeDao().insertCrossRef(SavedPlaceTagCrossRef(placeA,tagB,tripA)) }
        db.placeDao().insertCrossRef(SavedPlaceTagCrossRef(placeA,tagA,tripA)); assertEquals(1,db.placeDao().countCrossRefs())
        db.placeDao().deleteSavedPlace(placeA); assertEquals(0,db.placeDao().countCrossRefs())

        val place2=id(); val tag2=id(); db.placeDao().insertSavedPlace(place(place2,tripA,"poi-2")); db.placeDao().insertTag(TagEntity(tag2,tripA,"C","c")); db.placeDao().insertCrossRef(SavedPlaceTagCrossRef(place2,tag2,tripA))
        db.placeDao().deleteTag(tag2); assertEquals(0,db.placeDao().countCrossRefs())

        val place3=id(); val tag3=id(); db.placeDao().insertSavedPlace(place(place3,tripA,"poi-3")); db.placeDao().insertTag(TagEntity(tag3,tripA,"D","d")); db.placeDao().insertCrossRef(SavedPlaceTagCrossRef(place3,tag3,tripA))
        db.tripDao().deleteTrip(tripA); assertEquals(0,db.placeDao().countCrossRefs())
    }

    @Test fun declaredUniqueConstraintsRejectDuplicates() {
        val tripId=id(); val dayId=id(); db.tripDao().insertTrip(trip(tripId)); db.tripDao().insertDay(TripDayEntity(dayId,tripId,1_000L))
        val first=id(); val second=id(); db.placeDao().insertSavedPlace(place(first,tripId,"poi"))
        assertThrows(SQLiteConstraintException::class.java){db.placeDao().insertSavedPlace(place(second,tripId,"poi"))}
        db.placeDao().insertTag(TagEntity(id(),tripId,"Food","food"))
        assertThrows(SQLiteConstraintException::class.java){db.placeDao().insertTag(TagEntity(id(),tripId,"food","food"))}
        db.placeDao().insertSavedPlace(place(second,tripId,"poi-2")); db.itineraryDao().insertItem(item(dayId,tripId,first,1_000L))
        assertThrows(SQLiteConstraintException::class.java){db.itineraryDao().insertItem(item(dayId,tripId,second,1_000L))}
    }

    private data class Fixture(val tripId:String,val dayId:String,val fromId:String,val toId:String,val fromPlaceId:String)
    private fun sameDayFixture():Fixture {
        val tripId=id(); val dayId=id(); val a=id(); val b=id(); insertTripDay(tripId,dayId)
        db.placeDao().insertSavedPlace(place(a,tripId,"poi-$a")); db.placeDao().insertSavedPlace(place(b,tripId,"poi-$b"))
        val from=item(dayId,tripId,a,1_000L); val to=item(dayId,tripId,b,2_000L); db.itineraryDao().insertItem(from); db.itineraryDao().insertItem(to)
        return Fixture(tripId,dayId,from.id,to.id,a)
    }
    private fun insertTripDay(tripId:String,dayId:String){db.tripDao().insertTrip(trip(tripId));db.tripDao().insertDay(TripDayEntity(dayId,tripId,1_000L))}
    private fun id()=UUID.randomUUID().toString()
    private fun trip(id:String)=TripEntity(id,"Trip",TimeMode.DRAFT,travelMode=TravelMode.FLEXIBLE,createdAt=Instant.EPOCH,updatedAt=Instant.EPOCH)
    private fun place(id:String,tripId:String,poi:String)=SavedPlaceEntity(id,tripId,poi,"Place","Address",39.9,116.4)
    private fun item(dayId:String,tripId:String,placeId:String,position:Long)=ItineraryItemEntity(id(),dayId,tripId,placeId,position)
    private fun leg(dayId:String,fromId:String,toId:String)=RouteLegEntity(id(),dayId,fromId,toId,TransportMode.WALK,status=RouteStatus.PENDING,updatedAt=Instant.EPOCH)
}
