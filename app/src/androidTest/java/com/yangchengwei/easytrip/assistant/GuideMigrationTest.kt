package com.yangchengwei.easytrip.assistant

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.database.EasyTripDatabase
import org.junit.Assert.*
import org.junit.Test

class GuideMigrationTest {
    @Test fun twelveToThirteenPreservesAllLegacyTablesAndIndependentReceipts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "city-first-migration-${java.util.UUID.randomUUID()}"
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), EasyTripDatabase::class.java,
            emptyList(), FrameworkSQLiteOpenHelperFactory())
        val tableDefinitions = linkedMapOf<String, String>()
        try {
            helper.createDatabase(name, 12).use { db ->
                db.query("SELECT name,sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name!='room_master_table'").use {
                    while (it.moveToNext()) tableDefinitions[it.getString(0)] = it.getString(1)
                }
                db.execSQL("INSERT INTO trips (id,name,timeMode,startDate,travelMode,createdAt,updatedAt,hasTraveled) VALUES ('t','原旅行','DRAFT',NULL,'FLEXIBLE',0,0,0)")
                db.execSQL("INSERT INTO assistant_import_receipts VALUES ('old','t','digest','[]')")
                db.execSQL("INSERT INTO assistant_trip_creation_receipts VALUES ('create-trip:v1:old','t','old-digest','[]')")
            }
            helper.runMigrationsAndValidate(name, 13, true, EasyTripDatabase.MIGRATION_12_13).use { db ->
                tableDefinitions.forEach { (table, sql) ->
                    db.query("SELECT sql FROM sqlite_master WHERE name=?", arrayOf(table)).use {
                        assertTrue(it.moveToFirst()); assertEquals(sql, it.getString(0))
                    }
                }
                db.query("SELECT name FROM trips WHERE id='t'").use { assertTrue(it.moveToFirst()); assertEquals("原旅行", it.getString(0)) }
                db.query("SELECT digest FROM assistant_import_receipts").use { assertTrue(it.moveToFirst()); assertEquals("digest", it.getString(0)) }
                db.execSQL("PRAGMA foreign_keys=ON")
                db.execSQL("INSERT INTO assistant_trip_destinations VALUES ('t','[\"甲市\"]')")
                db.execSQL("INSERT INTO assistant_guide_handoffs VALUES ('operation','t','digest','material','materialDigest','PENDING',0,NULL)")
                db.execSQL("INSERT INTO assistant_guide_creation_receipts VALUES ('operation','t','digest')")
                db.execSQL("DELETE FROM trips WHERE id='t'")
                for (table in listOf("assistant_trip_destinations", "assistant_guide_handoffs"))
                    db.query("SELECT * FROM $table").use { assertEquals(0, it.count) }
                for (table in listOf("assistant_trip_creation_receipts", "assistant_guide_creation_receipts"))
                    db.query("SELECT * FROM $table").use { assertEquals(1, it.count) }
                db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            }
        } finally { context.deleteDatabase(name) }
    }
}
