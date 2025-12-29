package uk.adedamola.asciicast.vt.avt

import uk.adedamola.asciicast.vt.*
import java.nio.ByteBuffer

/**
 * VirtualTerminal implementation using Rust avt backend via JNI.
 *
 * This provides high-performance terminal emulation with accurate VT100/xterm
 * compatibility through the avt library.
 *
 * Thread safety: Not thread-safe. Call from a single thread or externally synchronize.
 */
class AvtVirtualTerminal(
    initialCols: Int = 80,
    initialRows: Int = 24
) : VirtualTerminal {

    private var handle: Long = AvtNative.vtNew(initialCols, initialRows)

    override var cols: Int = initialCols
        private set

    override var rows: Int = initialRows
        private set

    override val capabilities: TermCapabilities = TermCapabilities(
        trueColor = true,
        faint = true,
        italic = true,
        strikethrough = true,
        altScreen = true,
        mouse = false,
        bracketedPaste = false,
        differentialUpdates = true // avt provides dirty line tracking
    )

    private var currentTheme: Theme = Theme.DEFAULT

    override fun reset(cols: Int, rows: Int, theme: Theme?, initData: String?) {
        require(cols > 0 && rows > 0) { "cols and rows must be positive" }

        this.cols = cols
        this.rows = rows
        theme?.let { currentTheme = it }

        AvtNative.vtReset(handle, cols, rows)

        initData?.let {
            feedUtf8(it)
        }
    }

    override fun resize(cols: Int, rows: Int) {
        require(cols > 0 && rows > 0) { "cols and rows must be positive" }

        this.cols = cols
        this.rows = rows

        AvtNative.vtResize(handle, cols, rows)
    }

    override fun feedUtf8(text: String) {
        feed(text.toByteArray(Charsets.UTF_8))
    }

    override fun feed(bytes: ByteArray) {
        AvtNative.vtFeed(handle, bytes)
    }

    override fun snapshot(): TerminalFrame {
        val snapshotBytes = AvtNative.vtSnapshot(handle)

        return if (snapshotBytes.isEmpty()) {
            // Fallback for TODO implementation
            TerminalFrame.empty(cols, rows, currentTheme)
        } else {
            decodeSnapshot(snapshotBytes)
        }
    }

    override fun pollDiff(): TerminalDiff? {
        val diffBytes = AvtNative.vtPollDiff(handle)

        return if (diffBytes.isEmpty()) {
            null
        } else {
            decodeDiff(diffBytes)
        }
    }

    override fun close() {
        if (handle != 0L) {
            AvtNative.vtFree(handle)
            handle = 0
        }
    }

    /**
     * Decode binary snapshot format.
     *
     * TODO: Implement this to match the encoding in lib.rs
     */
    private fun decodeSnapshot(bytes: ByteArray): TerminalFrame {
        // TODO: Decode the binary format:
        // - Read cols, rows (varint)
        // - Read cursor position and visibility
        // - Read style table
        // - Read lines as runs
        //
        // For now, return placeholder
        return TerminalFrame.empty(cols, rows, currentTheme)
    }

    /**
     * Decode binary diff format.
     *
     * TODO: Implement this to match the encoding in lib.rs
     */
    private fun decodeDiff(bytes: ByteArray): TerminalDiff {
        // TODO: Decode the binary format:
        // - Read hasDiff flag
        // - Read dirty line indices
        // - Read cursor changed flag
        // - Read resized flag
        //
        // For now, return full redraw
        return TerminalDiff.FULL
    }

    /**
     * Helper to read varint from ByteBuffer.
     */
    private fun ByteBuffer.readVarint(): Int {
        var result = 0
        var shift = 0

        while (true) {
            val byte = get().toInt() and 0xFF
            result = result or ((byte and 0x7F) shl shift)

            if ((byte and 0x80) == 0) {
                break
            }

            shift += 7
        }

        return result
    }
}
