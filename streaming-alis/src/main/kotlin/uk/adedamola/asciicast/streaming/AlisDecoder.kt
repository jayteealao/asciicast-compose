package uk.adedamola.asciicast.streaming

import okio.Buffer
import okio.BufferedSource
import uk.adedamola.asciicast.vt.Theme

/**
 * Decoder for ALiS v1 binary protocol.
 *
 * Decodes binary event messages from an asciinema server live stream.
 *
 * Protocol format:
 * - Magic: "ALiS\x01" (first message only)
 * - Events: [EventType byte][payload...]
 * - Integers: LEB128 unsigned
 * - Strings: LEB128 length + UTF-8 bytes
 */
class AlisDecoder {

    companion object {
        val MAGIC = byteArrayOf(0x41, 0x4C, 0x69, 0x53, 0x01) // "ALiS\x01"
    }

    /**
     * Verify magic bytes.
     */
    fun verifyMagic(bytes: ByteArray): Boolean {
        return bytes.contentEquals(MAGIC)
    }

    /**
     * Decode a single ALiS event from binary data.
     */
    fun decode(bytes: ByteArray): AlisEvent {
        val buffer = Buffer().write(bytes)
        return decodeEvent(buffer)
    }

    /**
     * Decode event from BufferedSource.
     */
    private fun decodeEvent(source: BufferedSource): AlisEvent {
        val eventType = source.readByte()

        return when (eventType) {
            AlisEventType.INIT -> decodeInit(source)
            AlisEventType.OUTPUT -> decodeOutput(source)
            AlisEventType.RESIZE -> decodeResize(source)
            AlisEventType.MARKER -> decodeMarker(source)
            AlisEventType.EXIT -> decodeExit(source)
            AlisEventType.EOT -> decodeEot(source)
            else -> throw IllegalArgumentException("Unknown ALiS event type: $eventType")
        }
    }

    private fun decodeInit(source: BufferedSource): AlisEvent.Init {
        val lastId = LEB128.readUnsigned(source)
        val relTimeMicros = LEB128.readUnsigned(source)
        val cols = LEB128.readUnsigned(source).toInt()
        val rows = LEB128.readUnsigned(source).toInt()

        // Theme (optional, encoded as variant)
        val themeVariant = LEB128.readUnsigned(source).toInt()
        val theme = decodeTheme(source, themeVariant)

        // InitData (optional string)
        val hasInitData = source.readByte().toInt() != 0
        val initData = if (hasInitData) {
            LEB128.readString(source)
        } else {
            null
        }

        return AlisEvent.Init(
            lastId = lastId,
            relTimeMicros = relTimeMicros,
            cols = cols,
            rows = rows,
            theme = theme,
            initData = initData
        )
    }

    private fun decodeOutput(source: BufferedSource): AlisEvent.Output {
        val id = LEB128.readUnsigned(source)
        val relTimeMicros = LEB128.readUnsigned(source)
        val data = LEB128.readString(source)

        return AlisEvent.Output(id, relTimeMicros, data)
    }

    private fun decodeResize(source: BufferedSource): AlisEvent.Resize {
        val id = LEB128.readUnsigned(source)
        val relTimeMicros = LEB128.readUnsigned(source)
        val cols = LEB128.readUnsigned(source).toInt()
        val rows = LEB128.readUnsigned(source).toInt()

        return AlisEvent.Resize(id, relTimeMicros, cols, rows)
    }

    private fun decodeMarker(source: BufferedSource): AlisEvent.Marker {
        val id = LEB128.readUnsigned(source)
        val relTimeMicros = LEB128.readUnsigned(source)
        val label = LEB128.readString(source)

        return AlisEvent.Marker(id, relTimeMicros, label)
    }

    private fun decodeExit(source: BufferedSource): AlisEvent.Exit {
        val id = LEB128.readUnsigned(source)
        val relTimeMicros = LEB128.readUnsigned(source)
        val status = LEB128.readUnsigned(source).toInt()

        return AlisEvent.Exit(id, relTimeMicros, status)
    }

    private fun decodeEot(source: BufferedSource): AlisEvent.Eot {
        val relTimeMicros = LEB128.readUnsigned(source)
        return AlisEvent.Eot(relTimeMicros)
    }

    /**
     * Decode theme based on variant.
     *
     * Variants:
     * - 0: None (no theme)
     * - 1: 8-color palette
     * - 2: 16-color palette
     */
    private fun decodeTheme(source: BufferedSource, variant: Int): Theme? {
        return when (variant) {
            0 -> null // No theme

            1 -> {
                // 8-color palette
                val palette = (0 until 8).map {
                    val r = source.readByte().toInt() and 0xFF
                    val g = source.readByte().toInt() and 0xFF
                    val b = source.readByte().toInt() and 0xFF
                    uk.adedamola.asciicast.vt.Color.Rgb(r, g, b)
                }
                Theme(palette8 = palette)
            }

            2 -> {
                // 16-color palette
                val palette = (0 until 16).map {
                    val r = source.readByte().toInt() and 0xFF
                    val g = source.readByte().toInt() and 0xFF
                    val b = source.readByte().toInt() and 0xFF
                    uk.adedamola.asciicast.vt.Color.Rgb(r, g, b)
                }
                Theme(palette16 = palette)
            }

            else -> null // Unknown variant, ignore
        }
    }
}
