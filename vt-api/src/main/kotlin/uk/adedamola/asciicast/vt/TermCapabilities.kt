package uk.adedamola.asciicast.vt

/**
 * Terminal capabilities flags.
 * Indicates what features a VirtualTerminal backend supports.
 */
data class TermCapabilities(
    val trueColor: Boolean = true,
    val faint: Boolean = true,
    val italic: Boolean = true,
    val strikethrough: Boolean = true,
    val altScreen: Boolean = true,
    val mouse: Boolean = false,
    val bracketed Paste: Boolean = false,
    val differentialUpdates: Boolean = false
) {
    companion object {
        /** Full-featured terminal capabilities */
        val FULL = TermCapabilities(
            trueColor = true,
            faint = true,
            italic = true,
            strikethrough = true,
            altScreen = true,
            mouse = true,
            bracketedPaste = true,
            differentialUpdates = true
        )

        /** Basic capabilities (minimal feature set) */
        val BASIC = TermCapabilities(
            trueColor = false,
            faint = false,
            italic = false,
            strikethrough = false,
            altScreen = false,
            mouse = false,
            bracketedPaste = false,
            differentialUpdates = false
        )
    }
}
