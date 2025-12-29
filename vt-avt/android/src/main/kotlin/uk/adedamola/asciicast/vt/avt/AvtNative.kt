package uk.adedamola.asciicast.vt.avt

/**
 * JNI bridge to Rust avt implementation.
 *
 * All methods are native and map directly to Rust functions in lib.rs.
 */
internal object AvtNative {
    init {
        System.loadLibrary("asciicast_vt_avt")
    }

    /**
     * Create a new VT instance.
     * @return Opaque handle to VT instance
     */
    external fun vtNew(cols: Int, rows: Int): Long

    /**
     * Free a VT instance.
     */
    external fun vtFree(handle: Long)

    /**
     * Reset VT to new dimensions.
     */
    external fun vtReset(handle: Long, cols: Int, rows: Int)

    /**
     * Resize VT.
     */
    external fun vtResize(handle: Long, cols: Int, rows: Int)

    /**
     * Feed bytes to VT.
     */
    external fun vtFeed(handle: Long, bytes: ByteArray)

    /**
     * Capture snapshot as encoded bytes.
     * @return Encoded snapshot, or empty array if handle invalid
     */
    external fun vtSnapshot(handle: Long): ByteArray

    /**
     * Poll for differential update.
     * @return Encoded diff, or empty array if no diff
     */
    external fun vtPollDiff(handle: Long): ByteArray
}
