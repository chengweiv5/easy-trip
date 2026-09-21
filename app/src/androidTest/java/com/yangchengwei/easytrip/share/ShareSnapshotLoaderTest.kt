package com.yangchengwei.easytrip.share

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.core.model.*
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.route.data.RoomRouteLegRepository
import com.yangchengwei.easytrip.trip.data.RoomTripRepository
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ShareSnapshotLoaderTest {
    @Test fun readsRealRepositoriesWithoutMutatingRepeatedVisitsOrRoutes() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,EasyTripDatabase::class.java).build()
        try {
            val trips=RoomTripRepository(db.tripDao(),database=db,isOnline={false})
            val items=RoomItineraryRepository(db,db.itineraryEditingDao(),db.routeLegDao(),isOnline={false})
            val routes=RoomRouteLegRepository(db.routeLegDao())
            val id=trips.createTrip(CreateTrip("快照验证",2,startDate=LocalDate.of(2026,4,12)))
            val trip=trips.observeTrip(id).first()!!
            db.savedPlaceDao().insertPlace(SavedPlaceEntity("hotel",id,"hotel","酒店","西湖",30.25,120.15))
            val first=items.addItem(trip.days[0].id,"hotel",0)
            val second=items.addItem(trip.days[0].id,"hotel",1)
            items.updateDetails(first,LocalTime.of(9,30),60,"保留完整备注")
            val beforeItems=items.observeDay(trip.days[0].id).first()
            val beforeRoutes=routes.observeDay(trip.days[0].id).first()
            val output=ShareSnapshotLoader(trips,items,routes).load(id)
            assertEquals(listOf(first,second),output.days[0].stops.map { it.id })
            assertEquals("保留完整备注",output.days[0].stops[0].note)
            assertEquals(LocalDate.of(2026,4,13),output.days[1].date)
            assertTrue(output.days[1].stops.isEmpty())
            assertEquals(beforeItems,items.observeDay(trip.days[0].id).first())
            assertEquals(beforeRoutes,routes.observeDay(trip.days[0].id).first())
        } finally { db.close() }
    }
}
