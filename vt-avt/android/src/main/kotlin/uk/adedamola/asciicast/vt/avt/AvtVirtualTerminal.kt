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
            android.util.Log.w("AvtVT", "Snapshot bytes are empty")
            TerminalFrame.empty(cols, rows, currentTheme)
        } else {
            try {
                android.util.Log.d("AvtVT", "Decoding snapshot, bytes: ${snapshotBytes.size}")
                decodeSnapshot(snapshotBytes)
            } catch (e: Exception) {
                android.util.Log.e("AvtVT", "Error decoding snapshot", e)
                TerminalFrame.empty(cols, rows, currentTheme)
            }
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
     * Matches the encoding in lib.rs encode_snapshot()
     */
    private fun decodeSnapshot(bytes: ByteArray): TerminalFrame {
        val buffer = ByteBuffer.wrap(bytes)

        // Read size
        val cols = buffer.readVarint()
        val rows = buffer.readVarint()

        // Read cursor
        val cursorCol = buffer.readVarint()
        val cursorRow = buffer.readVarint()
        val cursorVisible = buffer.get() == 1.toByte()

        // Read lines
        val lines = mutableListOf<TerminalLine>()
        for (lineIdx in 0 until rows) {
            lines.add(decodeLine(buffer))
        }

        return TerminalFrame(
            cols = cols,
            rows = rows,
            lines = lines,
            cursor = Cursor(
                row = cursorRow,
                col = cursorCol,
                visible = cursorVisible
            ),
            theme = currentTheme,
            title = null
        )
    }

    private fun decodeLine(buffer: ByteBuffer): TerminalLine {
        val runCount = buffer.readVarint()
        val runs = mutableListOf<TextRun>()

        for (i in 0 until runCount) {
            val colStart = buffer.readVarint()
            val textLen = buffer.readVarint()
            val style = decodeCellStyle(buffer)

            val textBytes = ByteArray(textLen)
            buffer.get(textBytes)
            val text = String(textBytes, Charsets.UTF_8)

            runs.add(TextRun(
                colStart = colStart,
                text = text,
                style = style
            ))
        }

        return TerminalLine(runs = runs)
    }

    private fun decodeCellStyle(buffer: ByteBuffer): CellStyle {
        // Decode foreground
        val fgType = buffer.get().toInt()
        val foreground = when (fgType) {
            0 -> Color.Indexed(buffer.get().toInt() and 0xFF)
            1 -> Color.Rgb(
                r = buffer.get().toInt() and 0xFF,
                g = buffer.get().toInt() and 0xFF,
                b = buffer.get().toInt() and 0xFF
            )
            else -> null // 2 = None
        }

        // Decode background
        val bgType = buffer.get().toInt()
        val background = when (bgType) {
            0 -> Color.Indexed(buffer.get().toInt() and 0xFF)
            1 -> Color.Rgb(
                r = buffer.get().toInt() and 0xFF,
                g = buffer.get().toInt() and 0xFF,
                b = buffer.get().toInt() and 0xFF
            )
            else -> null // 2 = None
        }

        // Decode attributes
        val attrs = buffer.get().toInt() and 0xFF

        return CellStyle(
            foreground = foreground ?: Color.Default,
            background = background ?: Color.Default,
            bold = (attrs and 0x01) != 0,
            italic = (attrs and 0x02) != 0,
            underline = (attrs and 0x04) != 0,
            strikethrough = (attrs and 0x08) != 0,
            blink = (attrs and 0x10) != 0,
            reverse = (attrs and 0x20) != 0
        )
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
