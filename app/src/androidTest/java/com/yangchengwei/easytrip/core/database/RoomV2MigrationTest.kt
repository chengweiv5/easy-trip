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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
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
            10,
            true,
            EasyTripDatabase.MIGRATION_1_2,
            EasyTripDatabase.MIGRATION_2_3,
            EasyTripDatabase.MIGRATION_3_4,
            EasyTripDatabase.MIGRATION_4_5,
            EasyTripDatabase.MIGRATION_5_6,
            EasyTripDatabase.MIGRATION_6_7,
            EasyTripDatabase.MIGRATION_7_8,
            EasyTripDatabase.MIGRATION_8_9,
            EasyTripDatabase.MIGRATION_9_10,
        ).close()

        val database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), EasyTripDatabase::class.java, databaseName)
            .addMigrations(EasyTripDatabase.MIGRATION_1_2, EasyTripDatabase.MIGRATION_2_3, EasyTripDatabase.MIGRATION_3_4, EasyTripDatabase.MIGRATION_4_5, EasyTripDatabase.MIGRATION_5_6, EasyTripDatabase.MIGRATION_6_7, EasyTripDatabase.MIGRATION_7_8, EasyTripDatabase.MIGRATION_8_9, EasyTripDatabase.MIGRATION_9_10)
            .allowMainThreadQueries()
            .build()
        try {
            val item = runBlocking { database.itineraryEditingDao().item("i1")!! }
            val leg = runBlocking { database.routeLegDao().legs("day").single() }
            assertEquals("trip", item.tripId)
            assertEquals(30, item.stayDurationMinutes)
            assertNull(item.expenseCents)
            assertNull(leg.expenseCents)
            assertNull(item.note)
            assertNull(item.idempotencyKey)
            assertFalse(item.autoTimingPending)
            assertNull(item.autoTimingAnchorId)
            assertNull(item.timingWarning)
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

    @Test
    fun migrationFromV7PreservesExplicitTimingExpensesNotesAndRoutesWithoutOptingInOldItems() {
        helper.createDatabase(databaseName, 7).apply {
            execSQL("INSERT INTO trips (id, name, timeMode, startDate, travelMode, createdAt, updatedAt) VALUES ('trip', 'Trip', 'DRAFT', NULL, 'FLEXIBLE', 0, 0)")
            execSQL("INSERT INTO trip_days (id, tripId, position) VALUES ('day', 'trip', 0)")
            for (id in listOf("a", "b")) {
                execSQL("INSERT INTO saved_places (id, tripId, amapPoiId, name, address, latitude, longitude) VALUES ('$id', 'trip', '$id', '$id', '地址', 30, 120)")
            }
            execSQL("INSERT INTO itinerary_items (id, tripDayId, tripId, savedPlaceId, position, arrivalTime, stayDurationMinutes, note, expenseCents) VALUES ('i1', 'day', 'trip', 'a', 0, '11:20', 45, '原备注', 3500)")
            execSQL("INSERT INTO itinerary_items (id, tripDayId, tripId, savedPlaceId, position) VALUES ('i2', 'day', 'trip', 'b', 1000)")
            execSQL("INSERT INTO route_legs (id, tripDayId, fromItemId, toItemId, recommendedMode, status, distanceMeters, durationSeconds, polyline, version, updatedAt, durationOverrideSeconds, note, expenseCents) VALUES ('leg', 'day', 'i1', 'i2', 'WALK', 'SUCCESS', 42, 60, 'polyline', 7, 0, 120, '交通备注', 250)")
            close()
        }
        helper.runMigrationsAndValidate(databaseName, 10, true, EasyTripDatabase.MIGRATION_7_8, EasyTripDatabase.MIGRATION_8_9, EasyTripDatabase.MIGRATION_9_10).close()
        val database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), EasyTripDatabase::class.java, databaseName)
            .addMigrations(EasyTripDatabase.MIGRATION_7_8, EasyTripDatabase.MIGRATION_8_9, EasyTripDatabase.MIGRATION_9_10).build()
        try {
            runBlocking {
                database.itineraryEditingDao().refreshAutomaticTimings("day")
                val first = database.itineraryEditingDao().item("i1")!!
                val second = database.itineraryEditingDao().item("i2")!!
                val leg = database.routeLegDao().legs("day").single()
                assertEquals(java.time.LocalTime.of(11, 20), first.arrivalTime)
                assertEquals(45, first.stayDurationMinutes)
                assertEquals("原备注", first.note)
                assertNull(first.expenseCents)
                val records = com.yangchengwei.easytrip.expense.data.RoomExpenseRepository(database)
                    .observeRecords().first()
                assertEquals(3500L, records.single { it.key.id == "legacy:i1" }.cents)
                assertFalse(first.autoTimingPending)
                assertFalse(second.autoTimingPending)
                assertNull(second.arrivalTime)
                assertNull(second.stayDurationMinutes)
                assertNull(first.autoTimingAnchorId)
                assertNull(first.timingWarning)
                assertEquals(120, leg.durationOverrideSeconds)
                assertEquals(60, leg.durationSeconds)
                assertEquals("polyline", leg.polyline)
                assertEquals("交通备注", leg.note)
                assertEquals(250L, leg.expenseCents)
                assertEquals(7L, leg.version)
            }
        } finally {
            database.close()
        }
    }
}
