package uk.adedamola.asciicast.vt

/**
 * ANSI color representation supporting 8/16/256 color palettes and true color.
 */
sealed class Color {
    /** Default foreground or background color */
    data object Default : Color()

    /** Indexed color (0-255) */
    data class Indexed(val index: Int) : Color()

    /** True color RGB */
    data class Rgb(val r: Int, val g: Int, val b: Int) : Color() {
        init {
            require(r in 0..255 && g in 0..255 && b in 0..255) {
                "RGB values must be 0-255"
            }
        }
    }
}

/**
 * Theme defines the color palette for terminal rendering.
 *
 * @property foreground Default foreground color
 * @property background Default background color
 * @property palette8 Optional 8-color palette (indices 0-7)
 * @property palette16 Optional 16-color palette (indices 0-15, extends palette8)
 */
data class Theme(
    val foreground: Color.Rgb = Color.Rgb(204, 204, 204),
    val background: Color.Rgb = Color.Rgb(0, 0, 0),
    val palette8: List<Color.Rgb>? = null,
    val palette16: List<Color.Rgb>? = null
) {
    init {
        palette8?.let { require(it.size == 8) { "palette8 must have exactly 8 colors" } }
        palette16?.let { require(it.size == 16) { "palette16 must have exactly 16 colors" } }
    }

    companion object {
        /** Standard ANSI 8-color palette */
        val STANDARD_8: List<Color.Rgb> = listOf(
            Color.Rgb(0, 0, 0),       // Black
            Color.Rgb(205, 0, 0),     // Red
            Color.Rgb(0, 205, 0),     // Green
            Color.Rgb(205, 205, 0),   // Yellow
            Color.Rgb(0, 0, 238),     // Blue
            Color.Rgb(205, 0, 205),   // Magenta
            Color.Rgb(0, 205, 205),   // Cyan
            Color.Rgb(229, 229, 229)  // White
        )

        /** Standard ANSI 16-color palette */
        val STANDARD_16: List<Color.Rgb> = STANDARD_8 + listOf(
            Color.Rgb(127, 127, 127), // Bright Black (Gray)
            Color.Rgb(255, 0, 0),     // Bright Red
            Color.Rgb(0, 255, 0),     // Bright Green
            Color.Rgb(255, 255, 0),   // Bright Yellow
            Color.Rgb(92, 92, 255),   // Bright Blue
            Color.Rgb(255, 0, 255),   // Bright Magenta
            Color.Rgb(0, 255, 255),   // Bright Cyan
            Color.Rgb(255, 255, 255)  // Bright White
        )

        /** Default theme with standard 16-color palette */
        val DEFAULT = Theme(
            foreground = Color.Rgb(204, 204, 204),
            background = Color.Rgb(0, 0, 0),
            palette8 = STANDARD_8,
            palette16 = STANDARD_16
        )
    }

    /**
     * Resolve a Color to RGB, using palette or defaults.
     */
    fun resolve(color: Color): Color.Rgb = when (color) {
        is Color.Default -> foreground
        is Color.Rgb -> color
        is Color.Indexed -> {
            when {
                color.index < 8 -> palette8?.getOrNull(color.index) ?: STANDARD_8[color.index]
                color.index < 16 -> palette16?.getOrNull(color.index) ?: STANDARD_16[color.index]
                color.index < 256 -> xterm256Color(color.index)
                else -> foreground
            }
        }
    }

    /**
     * Compute xterm 256-color palette RGB values.
     */
    private fun xterm256Color(index: Int): Color.Rgb {
        return when {
            index < 16 -> STANDARD_16.getOrNull(index) ?: foreground
            index < 232 -> {
                // 6x6x6 color cube (indices 16-231)
                val i = index - 16
                val r = (i / 36) * 51
                val g = ((i % 36) / 6) * 51
                val b = (i % 6) * 51
                Color.Rgb(r, g, b)
            }
            else -> {
                // Grayscale (indices 232-255)
                val gray = 8 + (index - 232) * 10
                Color.Rgb(gray, gray, gray)
            }
        }
    }
}
