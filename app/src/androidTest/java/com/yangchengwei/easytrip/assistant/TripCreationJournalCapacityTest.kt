package com.yangchengwei.easytrip.assistant

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class TripCreationJournalCapacityTest {
    @Test fun hundredLongPlacesSurviveJournalReopenAndOversizedInputIsPreserved() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val directory = Files.createTempDirectory(base.cacheDir.toPath(), "hundred-journal-").toFile()
        val context = object : ContextWrapper(base) {
            override fun getNoBackupFilesDir(): File = directory
        }
        try {
            val items = (1..100).map {
                ImportItem("item-$it", 0, PlaceCandidate("poi-$it", "景".repeat(200), "址".repeat(1000),
                    GeoPoint(30.2, 120.1), null, "杭州"), PlaceCategory.ATTRACTION)
            }
            val command = TripCreationCommand.create("百地点旅行", null, 1, TravelMode.FLEXIBLE, items,
                (1..60).map { "$it" + "源".repeat(497) })
            assertTrue(command.encode().toByteArray().size > 256 * 1024)
            TripCreationJournal(context).write(command)
            val reopened = TripCreationJournal(context)
            assertEquals(command, reopened.read())
            reopened.write(command)
            assertEquals(100, reopened.read()!!.items.size)
            reopened.clear(command)
            assertNull(reopened.read())
            val file = File(directory, "trip-creation-pending.json")
            file.writeBytes(ByteArray(1024 * 1024 + 1))
            assertThrows(IllegalArgumentException::class.java) { reopened.read() }
            assertEquals(1024 * 1024 + 1L, file.length())
        } finally {
            directory.deleteRecursively()
        }
    }
}
