package uk.adedamola.asciicast.player

import uk.adedamola.asciicast.vt.*

/**
 * Fake VirtualTerminal implementation for testing.
 *
 * Captures all operations for verification and provides simple in-memory state.
 */
class FakeTerminal(
    initialCols: Int = 80,
    initialRows: Int = 24,
) : VirtualTerminal {
    override var cols: Int = initialCols
        private set

    override var rows: Int = initialRows
        private set

    override val capabilities: TermCapabilities = TermCapabilities.BASIC

    // Captured operations
    val operations = mutableListOf<Operation>()
    val fedText = StringBuilder()

    private var currentTheme: Theme = Theme.DEFAULT
    private var currentCursor = Cursor(0, 0, visible = true)

    sealed class Operation {
        data class Reset(val cols: Int, val rows: Int, val theme: Theme?, val initData: String?) : Operation()

        data class Resize(val cols: Int, val rows: Int) : Operation()

        data class FeedUtf8(val text: String) : Operation()

        data class Feed(val bytes: ByteArray) : Operation() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Feed) return false
                return bytes.contentEquals(other.bytes)
            }

            override fun hashCode(): Int = bytes.contentHashCode()
        }

        data object Snapshot : Operation()

        data object PollDiff : Operation()

        data object Close : Operation()
    }

    override fun reset(
        cols: Int,
        rows: Int,
        theme: Theme?,
        initData: String?,
    ) {
        operations.add(Operation.Reset(cols, rows, theme, initData))
        this.cols = cols
        this.rows = rows
        theme?.let { currentTheme = it }
        fedText.clear()
        initData?.let { fedText.append(it) }
    }

    override fun resize(
        cols: Int,
        rows: Int,
    ) {
        operations.add(Operation.Resize(cols, rows))
        this.cols = cols
        this.rows = rows
    }

    override fun feedUtf8(text: String) {
        operations.add(Operation.FeedUtf8(text))
        fedText.append(text)
    }

    override fun feed(bytes: ByteArray) {
        operations.add(Operation.Feed(bytes))
        fedText.append(String(bytes, Charsets.UTF_8))
    }

    override fun snapshot(): TerminalFrame {
        operations.add(Operation.Snapshot)
        return TerminalFrame.empty(cols, rows, currentTheme).copy(
            cursor = currentCursor,
        )
    }

    override fun pollDiff(): TerminalDiff? {
        operations.add(Operation.PollDiff)
        return null // Fake doesn't support diffs
    }

    override fun close() {
        operations.add(Operation.Close)
    }

    /**
     * Helper to get all fed text as a single string.
     */
    fun getAllFedText(): String = fedText.toString()

    /**
     * Helper to count operations of a specific type.
     */
    inline fun <reified T : Operation> countOperations(): Int {
        return operations.count { it is T }
    }

    /**
     * Helper to clear recorded operations.
     */
    fun clearOperations() {
        operations.clear()
        fedText.clear()
    }
}
