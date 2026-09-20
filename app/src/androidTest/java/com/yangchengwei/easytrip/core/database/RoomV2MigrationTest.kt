package com.yangchengwei.easytrip.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomV2MigrationTest {
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EasyTripDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )
    private val databaseName = "room-v2-migration-test"

    @After
    fun tearDown() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromV1PreservesOldDataAndLeavesAddedFieldsNull() {
        helper.createDatabase(databaseName, 1).apply {
            execSQL("INSERT INTO trips (id, name, timeMode, startDate, travelMode, createdAt, updatedAt) VALUES ('trip', 'Trip', 'DRAFT', NULL, 'FLEXIBLE', 0, 0)")
            execSQL("INSERT INTO trip_days (id, tripId, position) VALUES ('day', 'trip', 0)")
            execSQL("INSERT INTO saved_places (id, tripId, amapPoiId, name, address, latitude, longitude, note, cityCode) VALUES ('a', 'trip', 'a', 'A', 'A', 1, 2, NULL, NULL)")
            execSQL("INSERT INTO saved_places (id, tripId, amapPoiId, name, address, latitude, longitude, note, cityCode) VALUES ('b', 'trip', 'b', 'B', 'B', 3, 4, NULL, NULL)")
            execSQL("INSERT INTO itinerary_items (id, tripDayId, tripId, savedPlaceId, position, arrivalTime, stayDurationMinutes) VALUES ('i1', 'day', 'trip', 'a', 0, '09:00', 30)")
            execSQL("INSERT INTO itinerary_items (id, tripDayId, tripId, savedPlaceId, position, arrivalTime, stayDurationMinutes) VALUES ('i2', 'day', 'trip', 'b', 1000, NULL, NULL)")
            execSQL("INSERT INTO route_legs (id, tripDayId, fromItemId, toItemId, recommendedMode, selectedMode, status, distanceMeters, durationSeconds, polyline, errorKind, errorCode, version, updatedAt) VALUES ('leg', 'day', 'i1', 'i2', 'WALK', NULL, 'SUCCESS', 42, 60, 'polyline', NULL, NULL, 7, 0)")
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            6,
            true,
            EasyTripDatabase.MIGRATION_1_2,
            EasyTripDatabase.MIGRATION_2_3,
            EasyTripDatabase.MIGRATION_3_4,
            EasyTripDatabase.MIGRATION_4_5,
            EasyTripDatabase.MIGRATION_5_6,
        ).close()

        val database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), EasyTripDatabase::class.java, databaseName)
            .addMigrations(EasyTripDatabase.MIGRATION_1_2, EasyTripDatabase.MIGRATION_2_3, EasyTripDatabase.MIGRATION_3_4, EasyTripDatabase.MIGRATION_4_5, EasyTripDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        try {
            val item = runBlocking { database.itineraryEditingDao().item("i1")!! }
            val leg = runBlocking { database.routeLegDao().legs("day").single() }
            assertEquals("trip", item.tripId)
            assertEquals(30, item.stayDurationMinutes)
            assertNull(item.note)
            assertNull(item.idempotencyKey)
            val legacyPlace = runBlocking { database.savedPlaceDao().place("a")!! }
            assertEquals("A", legacyPlace.name)
            assertNull(legacyPlace.cityName)
            assertNull(legacyPlace.cityAdCode)
            assertEquals(42, leg.distanceMeters)
            assertEquals(60, leg.durationSeconds)
            assertEquals("polyline", leg.polyline)
            assertNull(leg.durationOverrideSeconds)
            assertNull(leg.note)
            assertThrows(SQLiteConstraintException::class.java) {
                database.openHelper.writableDatabase.execSQL(
                    "INSERT INTO itinerary_items (id, tripDayId, tripId, savedPlaceId, position, arrivalTime, stayDurationMinutes, note) VALUES ('invalid', 'day', 'trip', 'missing', 2000, NULL, NULL, NULL)",
                )
            }
        } finally {
            database.close()
        }
    }
}
