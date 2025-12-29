package uk.adedamola.asciicast.player

import kotlinx.coroutines.flow.Flow
import uk.adedamola.asciicast.vt.TermEvent
import uk.adedamola.asciicast.vt.TimedTermEvent

/**
 * Source of terminal events for playback.
 */
sealed interface PlaybackSource {
    /**
     * Initialize the source and return the initial terminal state.
     */
    suspend fun init(): TermEvent.Init

    /**
     * Flow of timed events.
     */
    fun events(): Flow<TimedTermEvent>

    /**
     * Optional metadata about the source.
     */
    val metadata: SourceMetadata
}

/**
 * Metadata about a playback source.
 */
data class SourceMetadata(
    val duration: Double? = null,
    val idleTimeLimit: Double? = null,
    val title: String? = null,
    val command: String? = null,
    val seekable: Boolean = false
)
