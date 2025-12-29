package uk.adedamola.asciicast.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import uk.adedamola.asciicast.formats.AsciicastParser
import uk.adedamola.asciicast.formats.toInitEvent
import uk.adedamola.asciicast.vt.TermEvent
import uk.adedamola.asciicast.vt.TimedTermEvent
import java.io.InputStream

/**
 * Playback source from an asciicast recording file.
 */
class RecordingSource(
    private val inputStream: InputStream
) : PlaybackSource {

    private val parser = AsciicastParser()
    private lateinit var parsedHeader: uk.adedamola.asciicast.formats.AsciicastHeader
    private lateinit var parsedEvents: Sequence<TimedTermEvent>

    override suspend fun init(): TermEvent.Init {
        val (header, events) = parser.parse(inputStream)
        parsedHeader = header
        parsedEvents = events

        return header.toInitEvent()
    }

    override fun events(): Flow<TimedTermEvent> {
        return parsedEvents.asFlow()
    }

    override val metadata: SourceMetadata
        get() = SourceMetadata(
            duration = parsedHeader.duration,
            idleTimeLimit = parsedHeader.idle_time_limit,
            title = parsedHeader.title,
            command = parsedHeader.command,
            seekable = false // TODO: Implement seeking with event indexing
        )
}
