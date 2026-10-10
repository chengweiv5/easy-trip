package com.yangchengwei.easytrip.place.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import com.yangchengwei.easytrip.itinerary.data.RoomItineraryRepository
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class PlaceCategoryMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "place-category-migration-776a"
    private val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
        EasyTripDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory())

    @After fun cleanup() { context.deleteDatabase(name) }

    @Test fun upgradePreservesAllOldColumnsAndDefaultsPlacesToOther() = runBlocking {
        val tables = listOf("trips", "trip_days", "saved_places", "tags", "saved_place_tags",
            "itinerary_items", "route_legs", "place_expenses")
        lateinit var columns: Map<String, String>
        lateinit var before: Map<String, List<List<String?>>>
        helper.createDatabase(name, 9).use { db ->
            db.execSQL("INSERT INTO trips (id,name,timeMode,startDate,travelMode,createdAt,updatedAt,hasTraveled) VALUES ('trip','旅行','DRAFT',NULL,'FLEXIBLE',0,0,0)")
            db.execSQL("INSERT INTO trip_days (id,tripId,position) VALUES ('day','trip',9000)")
            listOf("place", "second").forEachIndexed { i, id ->
                db.execSQL("INSERT INTO saved_places (id,tripId,amapPoiId,name,address,latitude,longitude,note,cityMetadataVersion) VALUES ('$id','trip','$id','$id','地址',1,2,'收藏备注',0)")
                db.execSQL("INSERT INTO itinerary_items (id,tripDayId,tripId,savedPlaceId,position,note,autoTimingPending) VALUES ('$id','day','trip','$id',${i * 1000},'行程备注',0)")
            }
            db.execSQL("INSERT INTO tags (id,tripId,name,tagNameNormalized) VALUES ('tag','trip','亲子','亲子')")
            db.execSQL("INSERT INTO saved_place_tags (savedPlaceId,tagId,tripId) VALUES ('place','tag','trip')")
            db.execSQL("INSERT INTO place_expenses (id,itineraryItemId,cents,category,note,position) VALUES ('legacy:item','place',0,NULL,'旧费用',0)")
            db.execSQL("INSERT INTO place_expenses (id,itineraryItemId,cents,category,note,position) VALUES ('food','place',12345,'food','午饭',1)")
            db.execSQL("INSERT INTO route_legs (id,tripDayId,fromItemId,toItemId,recommendedMode,status,version,updatedAt,expenseCents) VALUES ('leg','day','place','second','WALK','SUCCESS',1,0,5850)")
            columns = tables.associateWith { table ->
                db.query("SELECT * FROM $table LIMIT 0").use { it.columnNames.joinToString(",") }
            }
            before = columns.mapValues { (table, names) -> rows(db, "SELECT $names FROM $table ORDER BY 1") }
        }
        helper.runMigrationsAndValidate(name, 10, true, EasyTripDatabase.MIGRATION_9_10).use { db ->
            columns.forEach { (table, names) ->
                assertEquals(table, before[table], rows(db, "SELECT $names FROM $table ORDER BY 1"))
            }
            assertEquals(listOf(listOf("other"), listOf("other")), rows(db, "SELECT category FROM saved_places ORDER BY id"))
            assertTrue(rows(db, "PRAGMA foreign_key_check").isEmpty())
        }
        repeat(2) {
            val db = Room.databaseBuilder(context, EasyTripDatabase::class.java, name)
                .addMigrations(EasyTripDatabase.MIGRATION_9_10).build()
            try {
                val places = RoomSavedPlaceRepository(db)
                assertTrue(places.observePlaces("trip", emptySet()).first().all { it.category == PlaceCategory.OTHER })
                val items = RoomItineraryRepository(db, db.itineraryEditingDao(), db.routeLegDao())
                    .observeDay("day").first().items
                assertEquals(listOf(0L, 12345L), items.first().expenses.map { it.cents })
                assertNull(items.first().expenses.first().category)
                assertEquals("行程备注", items.first().note)
            } finally { db.close() }
        }
    }

    private fun rows(db: SupportSQLiteDatabase, sql: String): List<List<String?>> =
        db.query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((0 until cursor.columnCount).map {
                    if (cursor.isNull(it)) null else cursor.getString(it)
                })
            }
        }
}
