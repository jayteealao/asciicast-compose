package uk.adedamola.asciicast.vt

/**
 * Cursor position and visibility.
 */
data class Cursor(
    val row: Int,
    val col: Int,
    val visible: Boolean = true
)

/**
 * A complete snapshot of the terminal state.
 * This is an immutable representation suitable for rendering.
 */
data class TerminalFrame(
    val cols: Int,
    val rows: Int,
    val lines: List<TerminalLine>,
    val cursor: Cursor,
    val theme: Theme = Theme.DEFAULT,
    val title: String? = null
) {
    init {
        require(lines.size == rows) {
            "lines.size (${lines.size}) must equal rows ($rows)"
        }
    }

    companion object {
        /**
         * Create an empty terminal frame with the given dimensions.
         */
        fun empty(cols: Int, rows: Int, theme: Theme = Theme.DEFAULT): TerminalFrame {
            return TerminalFrame(
                cols = cols,
                rows = rows,
                lines = List(rows) { TerminalLine.EMPTY },
                cursor = Cursor(0, 0, visible = true),
                theme = theme
            )
        }
    }
}

/**
 * A diff representing changes to a terminal frame.
 * Used for efficient updates when only parts of the terminal changed.
 */
data class TerminalDiff(
    val dirtyLines: Set<Int> = emptySet(),
    val cursorChanged: Boolean = false,
    val titleChanged: Boolean = false,
    val resized: Boolean = false,
    val fullRedraw: Boolean = false
) {
    companion object {
        val NONE = TerminalDiff()
        val FULL = TerminalDiff(fullRedraw = true)
    }
}
