package uk.adedamola.asciicast.formats

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Asciicast header (both v2 and v3).
 */
@Serializable
data class AsciicastHeader(
    val version: Int,
    val width: Int,
    val height: Int,
    val timestamp: Long? = null,
    val duration: Double? = null,
    val idle_time_limit: Double? = null,
    val command: String? = null,
    val title: String? = null,
    val env: JsonObject? = null,
    val theme: JsonObject? = null
)

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
