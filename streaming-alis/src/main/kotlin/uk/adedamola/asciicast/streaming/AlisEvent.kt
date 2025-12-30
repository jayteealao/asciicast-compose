package uk.adedamola.asciicast.streaming

import uk.adedamola.asciicast.vt.Theme

/**
 * ALiS v1 event types.
 *
 * Per official spec: https://docs.asciinema.org/manual/server/streaming/
 * Most event types use ASCII character codes (o, i, r, m, x)
 */
object AlisEventType {
    const val INIT: Byte = 0x01    // Init event
    const val OUTPUT: Byte = 0x6F  // 'o' - Output event (111 decimal)
    const val INPUT: Byte = 0x69   // 'i' - Input event (105 decimal)
    const val RESIZE: Byte = 0x72  // 'r' - Resize event (114 decimal)
    const val MARKER: Byte = 0x6D  // 'm' - Marker event (109 decimal)
    const val EXIT: Byte = 0x78    // 'x' - Exit event (120 decimal)
    const val EOT: Byte = 0x04     // End of transmission (4 decimal)
}

/**
 * Decoded ALiS events.
 */
sealed class AlisEvent {
    /**
     * Init event - initializes or reinitializes the terminal.
     *
     * @property lastId Last event ID (for resumption)
     * @property relTimeMicros Relative time in microseconds since stream start
     * @property cols Number of columns
     * @property rows Number of rows
     * @property theme Optional theme
     * @property initData Optional initialization data
     */
    data class Init(
        val lastId: Long,
        val relTimeMicros: Long,
        val cols: Int,
        val rows: Int,
        val theme: Theme?,
        val initData: String?
    ) : AlisEvent()

    /**
     * Output event - terminal output data.
     *
     * @property id Event ID
     * @property relTimeMicros Relative time in microseconds since last event
     * @property data Output data
     */
    data class Output(
        val id: Long,
        val relTimeMicros: Long,
        val data: String
    ) : AlisEvent()

    /**
     * Resize event - terminal size change.
     *
     * @property id Event ID
     * @property relTimeMicros Relative time in microseconds since last event
     * @property cols New number of columns
     * @property rows New number of rows
     */
    data class Resize(
        val id: Long,
        val relTimeMicros: Long,
        val cols: Int,
        val rows: Int
    ) : AlisEvent()

    /**
     * Marker event - chapter/annotation.
     *
     * @property id Event ID
     * @property relTimeMicros Relative time in microseconds since last event
     * @property label Marker label
     */
    data class Marker(
        val id: Long,
        val relTimeMicros: Long,
        val label: String
    ) : AlisEvent()

    /**
     * Exit event - process exited.
     *
     * @property id Event ID
     * @property relTimeMicros Relative time in microseconds since last event
     * @property status Exit status code
     */
    data class Exit(
        val id: Long,
        val relTimeMicros: Long,
        val status: Int
    ) : AlisEvent()

    /**
     * End of transmission - reset to pre-init state.
     *
     * @property relTimeMicros Relative time in microseconds since last event
     */
    data class Eot(
        val relTimeMicros: Long
    ) : AlisEvent()
}
