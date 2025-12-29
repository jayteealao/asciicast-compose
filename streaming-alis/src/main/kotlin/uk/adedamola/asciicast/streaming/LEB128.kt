package uk.adedamola.asciicast.streaming

import okio.Buffer
import okio.BufferedSource

/**
 * LEB128 (Little Endian Base 128) encoding utilities.
 * Used by ALiS protocol for variable-length integer encoding.
 */
object LEB128 {
    /**
     * Read an unsigned LEB128 integer from a BufferedSource.
     */
    fun readUnsigned(source: BufferedSource): Long {
        var result = 0L
        var shift = 0

        while (true) {
            val byte = source.readByte().toInt() and 0xFF

            result = result or ((byte and 0x7F).toLong() shl shift)

            if ((byte and 0x80) == 0) {
                break
            }

            shift += 7

            if (shift >= 64) {
                throw IllegalArgumentException("LEB128 value too large")
            }
        }

        return result
    }

    /**
     * Write an unsigned LEB128 integer to a Buffer.
     */
    fun writeUnsigned(buffer: Buffer, value: Long) {
        var remaining = value

        while (true) {
            val byte = (remaining and 0x7F).toInt()
            remaining = remaining ushr 7

            if (remaining == 0L) {
                buffer.writeByte(byte)
                break
            } else {
                buffer.writeByte(byte or 0x80)
            }
        }
    }

    /**
     * Read a UTF-8 string prefixed by LEB128 length.
     */
    fun readString(source: BufferedSource): String {
        val length = readUnsigned(source)
        return source.readUtf8(length)
    }
}
