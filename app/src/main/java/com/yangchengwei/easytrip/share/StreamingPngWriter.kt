package com.yangchengwei.easytrip.share

import java.io.Closeable
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/** One continuous RGB PNG, with only a row and a compressed chunk buffered in memory. */
internal class StreamingPngWriter(output: OutputStream, private val width: Int, private val height: Int) : Closeable {
    init { require(width in 1..16_384 && height > 0) }

    private val target = DataOutputStream(output)
    private val row = ByteArray(1 + width * 3)
    private val deflater = Deflater(Deflater.BEST_SPEED)
    private var rowsWritten = 0
    private var closed = false
    private val compressed = DeflaterOutputStream(object : OutputStream() {
        override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            chunk("IDAT", bytes, offset, length)
        }
    }, deflater, 32 * 1024)

    init {
        try {
            target.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
            val header = java.io.ByteArrayOutputStream(13)
            DataOutputStream(header).apply {
                writeInt(width); writeInt(height)
                writeByte(8); writeByte(2) // 8-bit RGB; the renderer paints an opaque background.
                writeByte(0); writeByte(0); writeByte(0)
            }
            chunk("IHDR", header.toByteArray())
        } catch (failure: Throwable) {
            deflater.end()
            throw failure
        }
    }

    fun writeRow(pixels: IntArray) {
        check(!closed && rowsWritten < height)
        require(pixels.size == width)
        row[0] = 1 // PNG Sub filter improves compression of text and large flat backgrounds.
        var previous = 0
        for (x in pixels.indices) {
            val pixel = pixels[x]
            val offset = 1 + x * 3
            row[offset] = ((pixel ushr 16 and 255) - (previous ushr 16 and 255)).toByte()
            row[offset + 1] = ((pixel ushr 8 and 255) - (previous ushr 8 and 255)).toByte()
            row[offset + 2] = ((pixel and 255) - (previous and 255)).toByte()
            previous = pixel
        }
        compressed.write(row)
        rowsWritten++
    }

    private fun chunk(type: String, bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        val name = type.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(name); update(bytes, offset, length) }
        target.writeInt(length)
        target.write(name)
        target.write(bytes, offset, length)
        target.writeInt(crc.value.toInt())
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            check(rowsWritten == height) { "图片内容不完整" }
            compressed.finish()
            chunk("IEND", byteArrayOf())
            target.flush()
        } finally { deflater.end() }
    }
}
