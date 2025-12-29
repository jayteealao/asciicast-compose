package uk.adedamola.asciicast.formats

import kotlinx.serialization.json.*
import uk.adedamola.asciicast.vt.TermEvent
import uk.adedamola.asciicast.vt.TimedTermEvent
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parser for asciicast v2 and v3 formats (NDJSON).
 *
 * Supports:
 * - v2: [time, code, data] where time is absolute seconds since start
 * - v3: [interval, code, data] where interval is seconds since previous event
 *
 * Unknown event codes are ignored (no crash).
 */
class AsciicastParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse asciicast from an InputStream.
     *
     * @return Pair of header and sequence of timed events
     */
    fun parse(input: InputStream): Pair<AsciicastHeader, Sequence<TimedTermEvent>> {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))

        // Parse header (first line)
        val headerLine = reader.readLine()
            ?: throw IllegalArgumentException("Empty asciicast file")

        val header = json.decodeFromString<AsciicastHeader>(headerLine)

        if (header.version !in listOf(2, 3)) {
            throw IllegalArgumentException("Unsupported asciicast version: ${header.version}")
        }

        // Parse events
        val events = sequence {
            var previousTime = 0.0

            for (line in reader.lineSequence()) {
                if (line.isBlank()) continue

                val event = parseEvent(line, header.version, previousTime) ?: continue

                val (termEvent, absoluteTime) = event
                previousTime = absoluteTime

                yield(termEvent)
            }
        }

        return header to events
    }

    /**
     * Parse a single event line.
     *
     * @return Pair of TimedTermEvent and absolute time, or null if event should be skipped
     */
    private fun parseEvent(
        line: String,
        version: Int,
        previousTime: Double
    ): Pair<TimedTermEvent, Double>? {
        val array = json.parseToJsonElement(line).jsonArray

        if (array.size < 2) {
            // Invalid event, skip
            return null
        }

        val timeValue = array[0].jsonPrimitive.double
        val code = array[1].jsonPrimitive.content

        // Calculate absolute time based on version
        val absoluteTime = when (version) {
            2 -> timeValue // v2: absolute time
            3 -> previousTime + timeValue // v3: delta time
            else -> return null
        }

        // Calculate delta in microseconds
        val deltaMicros = ((absoluteTime - previousTime) * 1_000_000).toLong()

        val termEvent = when (code) {
            EventCode.OUTPUT -> {
                val data = array.getOrNull(2)?.jsonPrimitive?.content ?: ""
                TermEvent.Output(data)
            }

            EventCode.INPUT -> {
                val data = array.getOrNull(2)?.jsonPrimitive?.content ?: ""
                TermEvent.Input(data)
            }

            EventCode.RESIZE -> {
                val sizeStr = array.getOrNull(2)?.jsonPrimitive?.content ?: "80x24"
                val (cols, rows) = parseSizeString(sizeStr)
                TermEvent.Resize(cols, rows)
            }

            EventCode.MARKER -> {
                val label = array.getOrNull(2)?.jsonPrimitive?.content ?: ""
                TermEvent.Marker(label)
            }

            EventCode.EXIT -> {
                val status = array.getOrNull(2)?.jsonPrimitive?.intOrNull ?: 0
                TermEvent.Exit(status)
            }

            else -> {
                // Unknown event code, ignore as per spec
                return null
            }
        }

        return TimedTermEvent(termEvent, deltaMicros) to absoluteTime
    }

    /**
     * Parse size string like "80x24" into cols and rows.
     */
    private fun parseSizeString(size: String): Pair<Int, Int> {
        val parts = size.split("x", "X")
        return if (parts.size == 2) {
            val cols = parts[0].toIntOrNull() ?: 80
            val rows = parts[1].toIntOrNull() ?: 24
            cols to rows
        } else {
            80 to 24
        }
    }
}

/**
 * Create an initial Init event from the header.
 */
fun AsciicastHeader.toInitEvent(): TermEvent.Init {
    return TermEvent.Init(
        cols = width,
        rows = height,
        theme = null, // TODO: Parse theme from header if present
        initData = null
    )
}
