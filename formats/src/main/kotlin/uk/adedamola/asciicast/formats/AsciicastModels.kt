package uk.adedamola.asciicast.formats

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Terminal definition for v3 format.
 */
@Serializable
data class AsciicastTerm(
    val cols: Int,
    val rows: Int,
    val theme: JsonObject? = null,
)

/**
 * Asciicast header (both v2 and v3).
 *
 * v2 format: { version: 2, width: 80, height: 24, ... }
 * v3 format: { version: 3, term: { cols: 80, rows: 24, ... }, ... }
 */
@Serializable
data class AsciicastHeader(
    val version: Int,
    // v2 fields
    val width: Int? = null,
    val height: Int? = null,
    // v3 field
    val term: AsciicastTerm? = null,
    // Common fields
    val timestamp: Long? = null,
    val duration: Double? = null,
    val idle_time_limit: Double? = null,
    val command: String? = null,
    val title: String? = null,
    val env: JsonObject? = null,
    val theme: JsonObject? = null,
) {
    /** Get cols from either v2 or v3 format */
    val cols: Int get() = term?.cols ?: width ?: 80

    /** Get rows from either v2 or v3 format */
    val rows: Int get() = term?.rows ?: height ?: 24

    /** Get theme from either v2 or v3 format */
    val termTheme: JsonObject? get() = term?.theme ?: theme
}

/**
 * Event codes as per asciicast spec.
 */
object EventCode {
    const val OUTPUT = "o"
    const val INPUT = "i"
    const val RESIZE = "r"
    const val MARKER = "m"
    const val EXIT = "x" // v3 only
}

/**
 * Raw asciicast event (generic representation).
 */
sealed class RawAsciicastEvent {
    data class Output(val time: Double, val data: String) : RawAsciicastEvent()

    data class Input(val time: Double, val data: String) : RawAsciicastEvent()

    data class Resize(val time: Double, val cols: Int, val rows: Int) : RawAsciicastEvent()

    data class Marker(val time: Double, val label: String) : RawAsciicastEvent()

    data class Exit(val time: Double, val status: Int) : RawAsciicastEvent()

    data class Unknown(val time: Double, val code: String, val data: JsonElement?) : RawAsciicastEvent()
}
