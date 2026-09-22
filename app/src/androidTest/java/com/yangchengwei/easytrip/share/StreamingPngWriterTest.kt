package com.yangchengwei.easytrip.share

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Random
import java.util.zip.CRC32
import android.graphics.BitmapFactory
import org.junit.Assert.*
import org.junit.Test

class StreamingPngWriterTest {
    @Test fun standardDecoderReadsEveryPixelAcrossMultipleChunks() {
        val width = 257
        val height = 513
        val random = Random(2026)
        val pixels = Array(height) { IntArray(width) { random.nextInt() or (0xff shl 24) } }
        val output = ByteArrayOutputStream()
        StreamingPngWriter(output, width, height).use { png -> pixels.forEach(png::writeRow) }
        val encoded = output.toByteArray()
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(encoded, 0, encoded.size))
        assertEquals(width, decoded.width)
        assertEquals(height, decoded.height)
        pixels.forEachIndexed { y, row ->
            val actual = IntArray(width)
            decoded.getPixels(actual, 0, width, 0, y, width, 1)
            assertArrayEquals("row $y", row, actual)
        }
        decoded.recycle()
        val input = DataInputStream(ByteArrayInputStream(encoded))
        val signature = ByteArray(8).also(input::readFully)
        assertArrayEquals(byteArrayOf(-119,80,78,71,13,10,26,10), signature)
        val types = mutableListOf<String>()
        while (input.available() > 0) {
            val length = input.readInt()
            val type = ByteArray(4).also(input::readFully)
            val bytes = ByteArray(length).also(input::readFully)
            val crc = CRC32().apply { update(type); update(bytes) }
            assertEquals(crc.value.toInt(), input.readInt())
            types += String(type, Charsets.US_ASCII)
        }
        assertEquals("IHDR", types.first())
        assertEquals("IEND", types.last())
        assertTrue(types.count { it == "IDAT" } > 2)
    }

    @Test fun incompleteImageCannotBeFinalized() {
        val output = ByteArrayOutputStream()
        val png = StreamingPngWriter(output, 2, 2)
        png.writeRow(intArrayOf(0, 0))
        assertThrows(IllegalStateException::class.java) { png.close() }
        assertFalse(output.toByteArray().toString(Charsets.ISO_8859_1).contains("IEND"))
        png.close() // Releasing a failed writer is safe.
    }

    @Test fun rejectsWrongRowAndAdditionalRows() {
        StreamingPngWriter(ByteArrayOutputStream(), 2, 1).use { png ->
            assertThrows(IllegalArgumentException::class.java) { png.writeRow(intArrayOf(0)) }
            png.writeRow(intArrayOf(0,0))
            assertThrows(IllegalStateException::class.java) { png.writeRow(intArrayOf(0,0)) }
        }
    }

    @Test fun outputFailureIsPropagated() {
        val output = object : OutputStream() {
            var size = 0
            override fun write(value: Int) { if (++size > 40) throw IOException("disk full") }
        }
        assertThrows(IOException::class.java) {
            StreamingPngWriter(output, 2, 1).use { it.writeRow(intArrayOf(0, 0)) }
        }
    }
}
