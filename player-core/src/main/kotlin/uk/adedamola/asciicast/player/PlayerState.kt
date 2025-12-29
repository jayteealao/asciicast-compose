package uk.adedamola.asciicast.player

/**
 * State of the asciinema player.
 */
sealed class PlayerState {
    /** Player is idle, no source loaded */
    data object Idle : PlayerState()

    /** Loading and initializing source */
    data object Loading : PlayerState()

    /** Playing events */
    data class Playing(val speed: Float = 1.0f) : PlayerState()

    /** Paused */
    data class Paused(val currentTimeMicros: Long) : PlayerState()

    /** Playback ended */
    data object Ended : PlayerState()

    /** Error occurred */
    data class Error(val message: String, val cause: Throwable? = null) : PlayerState()
}

/**
 * Marker/chapter in the recording.
 */
data class Marker(
    val timeMicros: Long,
    val label: String
)
