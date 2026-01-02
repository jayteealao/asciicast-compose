package uk.adedamola.asciicast.streaming

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class LEB128Test {
    @Test
    fun `encode and decode small values`() {
        testRoundTrip(0L)
        testRoundTrip(1L)
        testRoundTrip(127L)
    }

    @Test
    fun `encode and decode medium values`() {
        testRoundTrip(128L)
        testRoundTrip(255L)
        testRoundTrip(16384L)
    }

    @Test
    fun `encode and decode large values`() {
        testRoundTrip(1_000_000L)
        testRoundTrip(Long.MAX_VALUE ushr 1) // Stay within safe range
    }

    @Test
    fun `read string with LEB128 length prefix`() {
        val buffer = Buffer()
        val testString = "Hello, ALiS!"

        LEB128.writeUnsigned(buffer, testString.length.toLong())
        buffer.writeUtf8(testString)

        val result = LEB128.readString(buffer)
        assertEquals(testString, result)
    }

    @Test
    fun `read empty string`() {
        val buffer = Buffer()
        LEB128.writeUnsigned(buffer, 0L)

        val result = LEB128.readString(buffer)
        assertEquals("", result)
    }

    private fun testRoundTrip(value: Long) {
        val buffer = Buffer()
        LEB128.writeUnsigned(buffer, value)
        val decoded = LEB128.readUnsigned(buffer)
        assertEquals(value, decoded, "Round trip failed for value: $value")
    }
}
