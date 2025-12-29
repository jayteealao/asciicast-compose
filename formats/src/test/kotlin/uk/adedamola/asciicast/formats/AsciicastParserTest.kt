package uk.adedamola.asciicast.formats

import uk.adedamola.asciicast.vt.TermEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AsciicastParserTest {
    private val parser = AsciicastParser()

    @Test
    fun `parse v2 asciicast with output events`() {
        val cast = """
            {"version":2,"width":80,"height":24,"timestamp":1234567890}
            [0.0,"o","Hello "]
            [0.5,"o","World\r\n"]
            [1.0,"o","$ "]
        """.trimIndent()

        val (header, events) = parser.parse(cast.byteInputStream())

        assertEquals(2, header.version)
        assertEquals(80, header.width)
        assertEquals(24, header.height)

        val eventList = events.toList()
        assertEquals(3, eventList.size)

        // First event at t=0
        assertIs<TermEvent.Output>(eventList[0].event)
        assertEquals("Hello ", (eventList[0].event as TermEvent.Output).data)
        assertEquals(0L, eventList[0].deltaMicros)

        // Second event at t=0.5, delta = 0.5s = 500000us
        assertIs<TermEvent.Output>(eventList[1].event)
        assertEquals("World\r\n", (eventList[1].event as TermEvent.Output).data)
        assertEquals(500_000L, eventList[1].deltaMicros)

        // Third event at t=1.0, delta = 0.5s = 500000us
        assertIs<TermEvent.Output>(eventList[2].event)
        assertEquals("$ ", (eventList[2].event as TermEvent.Output).data)
        assertEquals(500_000L, eventList[2].deltaMicros)
    }

    @Test
    fun `parse v3 asciicast with interval times`() {
        val cast = """
            {"version":3,"width":80,"height":24}
            [0.0,"o","First"]
            [0.5,"o","Second"]
            [0.25,"o","Third"]
        """.trimIndent()

        val (header, events) = parser.parse(cast.byteInputStream())

        assertEquals(3, header.version)

        val eventList = events.toList()
        assertEquals(3, eventList.size)

        // v3 uses interval times (delta from previous)
        assertEquals(0L, eventList[0].deltaMicros)
        assertEquals(500_000L, eventList[1].deltaMicros)
        assertEquals(250_000L, eventList[2].deltaMicros)
    }

    @Test
    fun `parse resize event`() {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"r","100x30"]
            [1.0,"o","After resize"]
        """.trimIndent()

        val (_, events) = parser.parse(cast.byteInputStream())
        val eventList = events.toList()

        assertIs<TermEvent.Resize>(eventList[0].event)
        val resize = eventList[0].event as TermEvent.Resize
        assertEquals(100, resize.cols)
        assertEquals(30, resize.rows)
    }

    @Test
    fun `parse marker event`() {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"m","Chapter 1"]
            [1.0,"o","content"]
        """.trimIndent()

        val (_, events) = parser.parse(cast.byteInputStream())
        val eventList = events.toList()

        assertIs<TermEvent.Marker>(eventList[0].event)
        val marker = eventList[0].event as TermEvent.Marker
        assertEquals("Chapter 1", marker.label)
    }

    @Test
    fun `parse exit event (v3)`() {
        val cast = """
            {"version":3,"width":80,"height":24}
            [0.0,"o","Running..."]
            [1.0,"x",0]
        """.trimIndent()

        val (_, events) = parser.parse(cast.byteInputStream())
        val eventList = events.toList()

        assertEquals(2, eventList.size)
        assertIs<TermEvent.Exit>(eventList[1].event)
        assertEquals(0, (eventList[1].event as TermEvent.Exit).status)
    }

    @Test
    fun `ignore unknown event codes`() {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"o","Before"]
            [0.5,"z","unknown event"]
            [1.0,"o","After"]
        """.trimIndent()

        val (_, events) = parser.parse(cast.byteInputStream())
        val eventList = events.toList()

        // Unknown event "z" should be ignored
        assertEquals(2, eventList.size)
        assertEquals("Before", (eventList[0].event as TermEvent.Output).data)
        assertEquals("After", (eventList[1].event as TermEvent.Output).data)
    }

    @Test
    fun `parse input event (ignored for playback)`() {
        val cast = """
            {"version":2,"width":80,"height":24}
            [0.0,"i","ls\r\n"]
            [0.5,"o","file1  file2"]
        """.trimIndent()

        val (_, events) = parser.parse(cast.byteInputStream())
        val eventList = events.toList()

        // Input events are parsed but typically ignored for read-only playback
        assertEquals(2, eventList.size)
        assertIs<TermEvent.Input>(eventList[0].event)
        assertIs<TermEvent.Output>(eventList[1].event)
    }

    @Test
    fun `header contains optional metadata`() {
        val cast = """
            {"version":2,"width":120,"height":40,"timestamp":1234567890,"duration":10.5,"idle_time_limit":2.0,"title":"Demo","command":"bash"}
            [0.0,"o","test"]
        """.trimIndent()

        val (header, _) = parser.parse(cast.byteInputStream())

        assertEquals(120, header.width)
        assertEquals(40, header.height)
        assertEquals(1234567890L, header.timestamp)
        assertEquals(10.5, header.duration)
        assertEquals(2.0, header.idle_time_limit)
        assertEquals("Demo", header.title)
        assertEquals("bash", header.command)
    }

    @Test
    fun `toInitEvent creates initial terminal state`() {
        val header = AsciicastHeader(
            version = 2,
            width = 80,
            height = 24,
            timestamp = null,
            duration = null
        )

        val initEvent = header.toInitEvent()

        assertEquals(80, initEvent.cols)
        assertEquals(24, initEvent.rows)
    }
}
