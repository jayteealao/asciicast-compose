package uk.adedamola.asciicast.vt

/**
 * Terminal events from asciicast or live streams.
 * These represent the normalized event model consumed by the player.
 */
sealed class TermEvent {
    /**
     * Initialize or reset the terminal.
     *
     * @property cols Number of columns
     * @property rows Number of rows
     * @property theme Optional theme to apply
     * @property initData Optional initialization data (e.g., shell prompt)
     */
    data class Init(
        val cols: Int,
        val rows: Int,
        val theme: Theme? = null,
        val initData: String? = null
    ) : TermEvent()

    /**
     * Terminal output data.
     *
     * @property data UTF-8 text to feed to the terminal
     */
    data class Output(
        val data: String
    ) : TermEvent()

    /**
     * Terminal input (from user).
     * Currently ignored for read-only playback.
     *
     * @property data Input text
     */
    data class Input(
        val data: String
    ) : TermEvent()

    /**
     * Terminal resize.
     *
     * @property cols New number of columns
     * @property rows New number of rows
     */
    data class Resize(
        val cols: Int,
        val rows: Int
    ) : TermEvent()

    /**
     * Marker for chapters/annotations.
     *
     * @property label Marker label
     */
    data class Marker(
        val label: String
    ) : TermEvent()

    /**
     * Exit event (v3 only).
     *
     * @property status Exit status code
     */
    data class Exit(
        val status: Int = 0
    ) : TermEvent()

    /**
     * End of transmission (live streams).
     * Signals that the terminal should reset to pre-init state.
     */
    data object Eot : TermEvent()
}

/**
 * A terminal event with timing information.
 *
 * @property event The event
 * @property deltaMicros Time delta since previous event in microseconds
 */
data class TimedTermEvent(
    val event: TermEvent,
    val deltaMicros: Long
)
