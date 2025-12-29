package uk.adedamola.asciicast.vt

/**
 * Visual attributes for a terminal cell.
 */
data class CellStyle(
    val foreground: Color = Color.Default,
    val background: Color = Color.Default,
    val bold: Boolean = false,
    val faint: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val blink: Boolean = false,
    val reverse: Boolean = false,
    val strikethrough: Boolean = false
) {
    companion object {
        val DEFAULT = CellStyle()
    }
}

/**
 * A run of text with the same style.
 * Used to efficiently represent terminal lines.
 */
data class TextRun(
    val colStart: Int,
    val text: String,
    val style: CellStyle = CellStyle.DEFAULT
) {
    val length: Int get() = text.length
}

/**
 * A single line in the terminal buffer.
 */
data class TerminalLine(
    val runs: List<TextRun> = emptyList()
) {
    companion object {
        val EMPTY = TerminalLine(emptyList())
    }
}
