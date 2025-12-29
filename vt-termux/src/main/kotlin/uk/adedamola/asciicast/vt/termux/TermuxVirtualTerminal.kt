package uk.adedamola.asciicast.vt.termux

import uk.adedamola.asciicast.vt.*

/**
 * Termux terminal-emulator based Virtual Terminal implementation.
 *
 * TODO: Implement this adapter to use Termux's terminal-emulator library.
 *
 * Implementation notes:
 * - Add dependency: com.termux:terminal-emulator (check current version)
 * - IMPORTANT: Check licensing! terminal-emulator is GPLv3, which may affect your app
 * - Map TerminalBuffer to TerminalFrame
 * - Use TerminalSession or TerminalEmulator classes
 * - Convert screen lines to TextRun format
 * - Leverage mDirtyFlags or similar for differential updates
 * - This is Android-compatible (unlike JediTerm)
 *
 * Benefits:
 * - Optimized for Android
 * - Lightweight, no Swing dependencies
 * - Good VT100 compatibility
 * - Used in Termux app (well-tested in production)
 *
 * Limitations:
 * - GPLv3 license (affects app licensing)
 * - API may change (maintained by Termux community)
 * - Requires careful lifecycle management on Android
 *
 * Licensing considerations:
 * - If using GPLv3 code, your app must also be GPLv3 or compatible
 * - Consider dual-licensing strategy or alternative backends for proprietary apps
 * - Document license requirements clearly in README
 *
 * @see https://github.com/termux/termux-app/tree/master/terminal-emulator
 */
class TermuxVirtualTerminal(
    initialCols: Int = 80,
    initialRows: Int = 24
) : VirtualTerminal {

    override var cols: Int = initialCols
        private set

    override var rows: Int = initialRows
        private set

    override val capabilities: TermCapabilities = TermCapabilities.FULL.copy(
        differentialUpdates = true // Termux provides dirty line tracking
    )

    override fun reset(cols: Int, rows: Int, theme: Theme?, initData: String?) {
        TODO("Implement Termux reset: create new TerminalEmulator, set colors, feed initData")
    }

    override fun resize(cols: Int, rows: Int) {
        TODO("Implement Termux resize: call TerminalEmulator.resize()")
    }

    override fun feedUtf8(text: String) {
        TODO("Implement Termux feed: convert text to UTF-8 bytes and append to input")
    }

    override fun feed(bytes: ByteArray) {
        TODO("Implement Termux feed: append bytes to TerminalEmulator input")
        // Termux typically uses append(byte[], int, int)
    }

    override fun snapshot(): TerminalFrame {
        TODO("Implement Termux snapshot: read TerminalBuffer screen and convert to TerminalFrame")
        // Pseudocode:
        // - Access TerminalBuffer.mScreen or equivalent
        // - For each line (TerminalRow), extract runs:
        //   - Group consecutive cells with same style
        //   - Extract text and style attributes
        // - Get cursor from TerminalEmulator
        // - Return TerminalFrame
    }

    override fun pollDiff(): TerminalDiff {
        TODO("Implement Termux pollDiff: use mDirtyLines or similar")
        // Pseudocode:
        // - Check TerminalBuffer dirty flags
        // - Collect dirty line indices
        // - Check if cursor moved
        // - Clear dirty flags after reading
        // - Return TerminalDiff with dirtyLines set
    }

    override fun close() {
        TODO("Implement Termux cleanup: finish TerminalSession if used")
    }
}

/**
 * Helper to convert Termux style to CellStyle.
 */
private fun convertTermuxStyle(/* termux style object */): CellStyle {
    TODO("Map Termux style attributes to CellStyle")
    // - Extract foreground/background colors
    // - Map bold/italic/underline flags
    // - Handle reverse video
}
