package uk.adedamola.asciicast.vt.jediterm

import uk.adedamola.asciicast.vt.*

/**
 * JediTerm-based Virtual Terminal implementation.
 *
 * TODO: Implement this adapter to use JetBrains JediTerm library.
 *
 * Implementation notes:
 * - Add dependency: org.jetbrains.jediterm:jediterm-pty
 * - Map JediTerm's TerminalTextBuffer to TerminalFrame
 * - Convert line-based buffer to TextRun format
 * - Implement differential updates using JediTerm's damage tracking if available
 * - Handle JVM-only constraints (this backend won't work on Android)
 * - Consider thread safety: JediTerm may require EDT/UI thread for some operations
 *
 * Benefits:
 * - Pure JVM, works in desktop applications
 * - Well-tested, used in IntelliJ IDEA terminal
 * - Good VT100/xterm compatibility
 *
 * Limitations:
 * - Not suitable for Android (Swing dependencies)
 * - Heavier weight than pure parsing backends
 *
 * @see https://github.com/JetBrains/jediterm
 */
class JediTermVirtualTerminal(
    initialCols: Int = 80,
    initialRows: Int = 24
) : VirtualTerminal {

    override var cols: Int = initialCols
        private set

    override var rows: Int = initialRows
        private set

    override val capabilities: TermCapabilities = TermCapabilities.FULL.copy(
        differentialUpdates = false // TODO: Enable if JediTerm provides damage tracking
    )

    override fun reset(cols: Int, rows: Int, theme: Theme?, initData: String?) {
        TODO("Implement JediTerm reset: create new terminal buffer, apply theme, feed initData")
    }

    override fun resize(cols: Int, rows: Int) {
        TODO("Implement JediTerm resize: call terminal buffer resize")
    }

    override fun feedUtf8(text: String) {
        TODO("Implement JediTerm feed: convert text to bytes and call terminal processor")
    }

    override fun feed(bytes: ByteArray) {
        TODO("Implement JediTerm feed: pass bytes to terminal data stream")
    }

    override fun snapshot(): TerminalFrame {
        TODO("Implement JediTerm snapshot: read TerminalTextBuffer and convert to TerminalFrame")
        // Pseudocode:
        // - Get buffer lines from JediTerm
        // - For each line, extract TextRuns (group consecutive cells with same style)
        // - Get cursor position and visibility
        // - Return TerminalFrame
    }

    override fun pollDiff(): TerminalDiff? {
        TODO("Implement JediTerm pollDiff: track dirty regions or return null")
        // Option 1: Return null (full snapshots only)
        // Option 2: Track last snapshot and compare
        // Option 3: Use JediTerm's damage info if available
    }

    override fun close() {
        TODO("Implement JediTerm cleanup: dispose terminal resources")
    }
}
