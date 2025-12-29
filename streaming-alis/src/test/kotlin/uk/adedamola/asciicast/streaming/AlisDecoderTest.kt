package uk.adedamola.asciicast.streaming

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlisDecoderTest {

    private val decoder = AlisDecoder()

    @Test
    fun `verify magic bytes`() {
        val magic = byteArrayOf(0x41, 0x4C, 0x69, 0x53, 0x01) // "ALiS\x01"
        assertTrue(decoder.verifyMagic(magic))
    }

    @Test
    fun `decode Init event without theme`() {
        val buffer = Buffer()
        buffer.writeByte(AlisEventType.INIT.toInt())
        LEB128.writeUnsigned(buffer, 0L) // lastId
        LEB128.writeUnsigned(buffer, 1_000_000L) // relTimeMicros
        LEB128.writeUnsigned(buffer, 80L) // cols
        LEB128.writeUnsigned(buffer, 24L) // rows
        LEB128.writeUnsigned(buffer, 0L) // theme variant (none)
        buffer.writeByte(0) // no initData

        val event = decoder.decode(buffer.readByteArray())

        assertTrue(event is AlisEvent.Init)
        assertEquals(0L, event.lastId)
        assertEquals(1_000_000L, event.relTimeMicros)
        assertEquals(80, event.cols)
        assertEquals(24, event.rows)
        assertNull(event.theme)
        assertNull(event.initData)
    }

    @Test
    fun `decode Init event with initData`() {
        val buffer = Buffer()
        buffer.writeByte(AlisEventType.INIT.toInt())
        LEB128.writeUnsigned(buffer, 0L) // lastId
        LEB128.writeUnsigned(buffer, 0L) // relTimeMicros
        LEB128.writeUnsigned(buffer, 80L) // cols
        LEB128.writeUnsigned(buffer, 24L) // rows
        LEB128.writeUnsigned(buffer, 0L) // theme variant (none)
        buffer.writeByte(1) // has initData
        LEB128.writeUnsigned(buffer, 6L) // initData length
        buffer.writeUtf8("$ echo") // initData

        val event = decoder.decode(buffer.readByteArray())

        assertTrue(event is AlisEvent.Init)
        assertEquals("$ echo", event.initData)
    }

    @Test
    fun `decode Output event`() {
        val buffer = Buffer()
        buffer.writeByte(AlisEventType.OUTPUT.toInt())
        LEB128.writeUnsigned(buffer, 1L) // id
        LEB128.writeUnsigned(buffer, 500_000L) // relTimeMicros
        LEB128.writeUnsigned(buffer, 5L) // data length
        buffer.writeUtf8("Hello") // data

        val event = decoder.decode(buffer.readByteArray())

        assertTrue(event is AlisEvent.Output)
        assertEquals(1L, event.id)
        assertEquals(500_000L, event.relTimeMicros)
        assertEquals("Hello", event.data)
    }

    @Test
    fun `decode Resize event`() {
        val buffer = Buffer()
        buffer.writeByte(AlisEventType.RESIZE.toInt())
        LEB128.writeUnsigned(buffer, 2L) // id
        LEB128.writeUnsigned(buffer, 100_000L) // relTimeMicros
        LEB128.writeUnsigned(buffer, 120L) // cols
        LEB128.writeUnsigned(buffer, 40L) // rows

        val event = decoder.decode(buffer.readByteArray())

        assertTrue(event is AlisEvent.Resize)
        assertEquals(2L, event.id)
        assertEquals(100_000L, event.relTimeMicros)
        assertEquals(120, event.cols)
        assertEquals(40, event.rows)
    }

    @Test
    fun `decode Marker event`() {
        val buffer = Buffer()
        buffer.writeByte(AlisEventType.MARKER.toInt())
        LEB128.writeUnsigned(buffer, 3L) // id
        LEB128.writeUnsigned(buffer, 1_000_000L) // relTimeMicros
        LEB128.writeUnsigned(buffer, 9L) // label length
        buffer.writeUtf8("Chapter 1") // label

        val event = decoder.decode(buffer.readByteArray())

        assertTrue(event is AlisEvent.Marker)
        assertEquals(3L, event.id)
        assertEquals(1_000_000L, event.relTimeMicros)
        assertEquals("Chapter 1", event.label)
    }

    @Test
    fun `decode Exit event`() {
        val buffer = Buffer()
        buffer.writeByte(AlisEventType.EXIT.toInt())
        LEB128.writeUnsigned(buffer, 4L) // id
        LEB128.writeUnsigned(buffer, 5_000_000L) // relTimeMicros
        LEB128.writeUnsigned(buffer, 0L) // status

        val event = decoder.decode(buffer.readByteArray())

        assertTrue(event is AlisEvent.Exit)
        assertEquals(4L, event.id)
        assertEquals(5_000_000L, event.relTimeMicros)
        assertEquals(0, event.status)
    }

    @Test
    fun `decode EOT event`() {
        val buffer = Buffer()
        buffer.writeByte(AlisEventType.EOT.toInt())
        LEB128.writeUnsigned(buffer, 10_000_000L) // relTimeMicros

        val event = decoder.decode(buffer.readByteArray())

        assertTrue(event is AlisEvent.Eot)
        assertEquals(10_000_000L, event.relTimeMicros)
    }
}
