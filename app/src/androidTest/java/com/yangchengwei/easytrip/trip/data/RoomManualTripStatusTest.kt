package com.yangchengwei.easytrip.trip.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.trip.domain.CreateTrip
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class RoomManualTripStatusTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "manual-trip-status-test"
    private val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
        EasyTripDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory())
    @After fun cleanup() { context.deleteDatabase(name) }
    private fun open() = Room.databaseBuilder(context, EasyTripDatabase::class.java, name)
        .addMigrations(EasyTripDatabase.MIGRATION_5_6).build()

    @Test fun oldDatesStayPendingAndManualStatusSurvivesReopenAndDateEdits() = runBlocking {
        helper.createDatabase(name, 5).apply {
            execSQL("INSERT INTO trips (id,name,timeMode,startDate,travelMode,createdAt,updatedAt) VALUES ('old','旧旅行','DATED','2020-01-01','FLEXIBLE',1,2)")
            execSQL("INSERT INTO trip_days (id,tripId,position) VALUES ('day','old',0)")
            close()
        }
        helper.runMigrationsAndValidate(name, 6, true, EasyTripDatabase.MIGRATION_5_6).close()
        var db = open()
        var repo = RoomTripRepository(db.tripDao())
        try {
            assertFalse(repo.observeTrip("old").first()!!.hasTraveled)
            assertFalse(repo.observeTrips().first().single().hasTraveled)
            val newId = repo.createTrip(CreateTrip("新旅行", 1, startDate = LocalDate.of(2019, 1, 1)))
            assertFalse(repo.observeTrip(newId).first()!!.hasTraveled)
            repo.setHasTraveled("old", true)
            db.close()
            db = open()
            repo = RoomTripRepository(db.tripDao())
            assertTrue(repo.observeTrip("old").first()!!.hasTraveled)
            assertTrue(repo.observeTrips().first().single { it.id == "old" }.hasTraveled)
            repo.setStartDate("old", LocalDate.of(2030, 1, 1))
            repo.renameTrip("old", "旅行改名")
            assertTrue(repo.observeTrip("old").first()!!.hasTraveled)
            assertEquals(listOf("day"), repo.observeTrip("old").first()!!.days.map { it.id })
            repo.setHasTraveled("old", false)
            assertFalse(repo.observeTrip("old").first()!!.hasTraveled)
            try {
                repo.setHasTraveled("missing", true)
                fail("Missing trip must not succeed")
            } catch (_: IllegalArgumentException) { }
        } finally { db.close() }
    }
}
