package uk.adedamola.asciicast.vt

/**
 * Virtual Terminal abstraction.
 *
 * This interface provides a clean separation between the player/renderer
 * and the terminal emulation backend. Implementations can use different
 * emulation engines (avt, JediTerm, Termux, etc.) without affecting
 * the rest of the player architecture.
 *
 * Thread safety: Implementations should be called from a single thread
 * or provide internal synchronization. The player-core will serialize
 * all calls to a VT instance.
 */
interface VirtualTerminal : AutoCloseable {
    /**
     * Current number of columns.
     */
    val cols: Int

    /**
     * Current number of rows.
     */
    val rows: Int

    /**
     * Terminal capabilities supported by this backend.
     */
    val capabilities: TermCapabilities

    /**
     * Reset the terminal to initial state.
     *
     * @param cols Number of columns
     * @param rows Number of rows
     * @param theme Optional theme to apply
     * @param initData Optional initialization data to feed after reset
     */
    fun reset(
        cols: Int,
        rows: Int,
        theme: Theme? = null,
        initData: String? = null
    )

    /**
     * Resize the terminal.
     *
     * @param cols New number of columns
     * @param rows New number of rows
     */
    fun resize(cols: Int, rows: Int)

    /**
     * Feed UTF-8 text to the terminal.
     * This is a convenience method for text input.
     *
     * @param text UTF-8 text to process
     */
    fun feedUtf8(text: String)

    /**
     * Feed raw bytes to the terminal.
     * This is the high-performance path for binary data.
     *
     * @param bytes Raw bytes to process
     */
    fun feed(bytes: ByteArray)

    /**
     * Capture a complete snapshot of the terminal state.
     *
     * @return Immutable terminal frame
     */
    fun snapshot(): TerminalFrame

    /**
     * Poll for a differential update since last snapshot/pollDiff.
     * May return null if backend doesn't support diffs or nothing changed.
     *
     * @return Terminal diff, or null if not available
     */
    fun pollDiff(): TerminalDiff?

    /**
     * Close and release resources.
     * Called when the terminal is no longer needed.
     */
    override fun close()
}
