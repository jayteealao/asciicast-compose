package uk.adedamola.asciicast.renderer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import uk.adedamola.asciicast.player.*
import uk.adedamola.asciicast.vt.TerminalFrame
import uk.adedamola.asciicast.vt.VirtualTerminal
import uk.adedamola.asciicast.vt.avt.AvtVirtualTerminal

/**
 * State holder for Asciinema player in Compose.
 *
 * This class manages the lifecycle of an [AsciinemaPlayer] instance and exposes
 * its state in a Compose-friendly way. It provides lifecycle-aware state collection
 * and automatic resource cleanup.
 *
 * **Usage:**
 * ```
 * val playerState = rememberAsciinemaPlayerState(
 *     sourceKey = castUri,
 *     source = { RecordingSource(contentResolver.openInputStream(castUri)!!) },
 *     autoPlay = true
 * )
 *
 * TerminalCanvas(frame = playerState.frame.value)
 * PlayerControls(
 *     onPlay = { playerState.play() },
 *     onPause = { playerState.pause() }
 * )
 * ```
 *
 * @property player The underlying player controller for play/pause/seek operations
 * @property frame Current terminal frame state
 * @property playbackState Current playback state (idle, loading, playing, paused, etc.)
 * @property markers Chapter markers in the recording
 *
 * @see rememberAsciinemaPlayerState
 * @see rememberRecordingPlayerState
 * @see rememberLivePlayerState
 */
@Stable
class AsciinemaPlayerState internal constructor(
    val player: AsciinemaPlayer,
    val frame: State<TerminalFrame>,
    val playbackState: State<PlayerState>,
    val markers: State<List<Marker>>
) {
    /**
     * Start or resume playback.
     */
    fun play() = player.play()

    /**
     * Pause playback.
     */
    fun pause() = player.pause()

    /**
     * Stop playback and unload source.
     */
    fun stop() = player.stop()

    /**
     * Set playback speed multiplier.
     * @param speed Speed multiplier (must be positive). 1.0 = normal speed.
     */
    fun setSpeed(speed: Float) = player.setSpeed(speed)

    /**
     * Set idle time compression limit.
     * @param seconds Maximum idle time in seconds, or null for no limit.
     */
    fun setIdleTimeLimit(seconds: Double?) = player.setIdleTimeLimit(seconds)

    /**
     * Seek to a specific time (TODO: not yet implemented).
     * @param timeMicros Time in microseconds
     */
    fun seekTo(timeMicros: Long) = player.seekTo(timeMicros)
}

/**
 * Remember an [AsciinemaPlayerState] that loads and plays a source.
 *
 * This function creates and remembers a player state that:
 * - Creates a player and VT when first composed
 * - Loads the source when [sourceKey] changes
 * - Optionally auto-plays after loading
 * - Collects frame updates lifecycle-aware (pauses when backgrounded)
 * - Cleans up resources when leaving composition
 *
 * **Lifecycle behavior:**
 * - Player and VT are created once and survive recomposition
 * - When [sourceKey] changes, current playback stops and new source loads
 * - When composable leaves composition, all resources are cleaned up
 * - Frame collection pauses when app is backgrounded (saves CPU/battery)
 *
 * @param sourceKey A key that uniquely identifies the source. When this changes,
 *   the current source is stopped and the new source is loaded.
 * @param source A lambda that creates the [PlaybackSource]. Called when [sourceKey] changes.
 *   The lambda should create a new source instance (e.g., open InputStream).
 * @param vtFactory Factory for creating the VirtualTerminal. Called once on first composition.
 * @param scope CoroutineScope for the player. Defaults to rememberCoroutineScope().
 * @param autoPlay Whether to automatically start playback after loading. Default: true.
 * @param initialSpeed Initial playback speed multiplier. Default: 1.0f.
 * @param initialIdleTimeLimit Initial idle time compression limit in seconds. Default: null (no limit).
 * @return An [AsciinemaPlayerState] that manages the player lifecycle
 *
 * **Example:**
 * ```
 * @Composable
 * fun MyPlayer(castUri: Uri) {
 *     val context = LocalContext.current
 *     val playerState = rememberAsciinemaPlayerState(
 *         sourceKey = castUri,
 *         source = {
 *             RecordingSource(context.contentResolver.openInputStream(castUri)!!)
 *         },
 *         autoPlay = true
 *     )
 *
 *     TerminalCanvas(frame = playerState.frame.value)
 * }
 * ```
 */
@Composable
fun rememberAsciinemaPlayerState(
    sourceKey: Any?,
    source: () -> PlaybackSource,
    vtFactory: () -> VirtualTerminal,
    scope: CoroutineScope = rememberCoroutineScope(),
    autoPlay: Boolean = true,
    initialSpeed: Float = 1.0f,
    initialIdleTimeLimit: Double? = null
): AsciinemaPlayerState {
    // Create VT and player once, survive recomposition
    val vt = remember { vtFactory() }
    val player = remember(vt, scope) {
        AsciinemaPlayer(vt, scope).apply {
            setSpeed(initialSpeed)
            setIdleTimeLimit(initialIdleTimeLimit)
        }
    }

    // Collect player state flows lifecycle-aware
    val frame by player.frame.collectAsStateWithLifecycle()
    val playbackState by player.state.collectAsStateWithLifecycle()
    val markers by player.markers.collectAsStateWithLifecycle()

    // Load source when sourceKey changes
    LaunchedEffect(sourceKey) {
        if (sourceKey != null) {
            android.util.Log.d("AsciinemaPlayer", "LaunchedEffect: Loading source for key: $sourceKey")
            try {
                val playbackSource = source()
                android.util.Log.d("AsciinemaPlayer", "LaunchedEffect: Calling player.load()")
                player.load(playbackSource)
                android.util.Log.d("AsciinemaPlayer", "LaunchedEffect: Load complete, autoPlay=$autoPlay")
                if (autoPlay) {
                    android.util.Log.d("AsciinemaPlayer", "LaunchedEffect: Calling player.play()")
                    player.play()
                    android.util.Log.d("AsciinemaPlayer", "LaunchedEffect: Play called")
                }
            } catch (e: Exception) {
                android.util.Log.e("AsciinemaPlayer", "LaunchedEffect: Error loading/playing", e)
                throw e
            }
        }
    }

    // Cleanup when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            player.close() // Also closes VT
        }
    }

    // Create state holder
    return remember(player) {
        AsciinemaPlayerState(
            player = player,
            frame = derivedStateOf { frame },
            playbackState = derivedStateOf { playbackState },
            markers = derivedStateOf { markers }
        )
    }
}

/**
 * Remember a player state for a recording file from a URI.
 *
 * Convenience wrapper around [rememberAsciinemaPlayerState] for cast files.
 * Automatically handles InputStream creation from the URI.
 *
 * **Note:** The InputStream is managed internally and will be closed when the
 * source is unloaded or the composable leaves composition.
 *
 * @param context Android context for opening the URI
 * @param uri URI of the .cast file (can be content://, file://, or asset://)
 * @param vtFactory VirtualTerminal factory. Defaults to AvtVirtualTerminal.
 * @param autoPlay Whether to start playing automatically. Default: true.
 * @param initialSpeed Initial playback speed. Default: 1.0f.
 * @param initialIdleTimeLimit Idle time compression limit in seconds. Default: null.
 * @return An [AsciinemaPlayerState] for the recording
 *
 * **Example:**
 * ```
 * @Composable
 * fun PlayCastFile(uri: Uri) {
 *     val context = LocalContext.current
 *     val playerState = rememberRecordingPlayerState(context, uri)
 *
 *     TerminalCanvas(frame = playerState.frame.value)
 * }
 * ```
 */
@Composable
fun rememberRecordingPlayerState(
    context: Context,
    uri: Uri,
    vtFactory: () -> VirtualTerminal = { AvtVirtualTerminal() },
    autoPlay: Boolean = true,
    initialSpeed: Float = 1.0f,
    initialIdleTimeLimit: Double? = null
): AsciinemaPlayerState {
    return rememberAsciinemaPlayerState(
        sourceKey = uri,
        source = {
            android.util.Log.d("AsciinemaPlayer", "Creating source for URI: $uri")
            val inputStream = if (uri.scheme == "asset") {
                // Handle asset:// URIs
                val assetPath = uri.path?.removePrefix("/") ?: uri.toString().removePrefix("asset://")
                android.util.Log.d("AsciinemaPlayer", "Opening asset: $assetPath")
                try {
                    context.assets.open(assetPath).also {
                        android.util.Log.d("AsciinemaPlayer", "Asset opened successfully")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AsciinemaPlayer", "Failed to open asset: $assetPath", e)
                    throw e
                }
            } else {
                // Handle content://, file://, etc.
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open URI: $uri")
            }
            android.util.Log.d("AsciinemaPlayer", "Creating RecordingSource")
            RecordingSource(inputStream).also {
                android.util.Log.d("AsciinemaPlayer", "RecordingSource created")
            }
        },
        vtFactory = vtFactory,
        autoPlay = autoPlay,
        initialSpeed = initialSpeed,
        initialIdleTimeLimit = initialIdleTimeLimit
    )
}

/**
 * Remember a player state for a live WebSocket stream.
 *
 * Convenience wrapper around [rememberAsciinemaPlayerState] for live streams.
 * Automatically handles WebSocket connection and cleanup.
 *
 * **Note:** The WebSocket connection is managed internally and will be closed
 * when the composable leaves composition or when a new [wsUrl] is provided.
 *
 * @param wsUrl WebSocket URL (e.g., "wss://asciinema.org/ws/s/TOKEN")
 * @param vtFactory VirtualTerminal factory. Defaults to AvtVirtualTerminal.
 * @param autoPlay Whether to start playing automatically. Default: true.
 * @return An [AsciinemaPlayerState] for the live stream
 *
 * **Example:**
 * ```
 * @Composable
 * fun PlayLiveStream(wsUrl: String) {
 *     val playerState = rememberLivePlayerState(wsUrl)
 *
 *     TerminalCanvas(frame = playerState.frame.value)
 * }
 * ```
 */
@Composable
fun rememberLivePlayerState(
    wsUrl: String,
    vtFactory: () -> VirtualTerminal = { AvtVirtualTerminal() },
    autoPlay: Boolean = true
): AsciinemaPlayerState {
    // Note: LiveSource needs to be closed separately
    var liveSourceRef by remember { mutableStateOf<uk.adedamola.asciicast.streaming.LiveSource?>(null) }

    val state = rememberAsciinemaPlayerState(
        sourceKey = wsUrl,
        source = {
            // Close previous LiveSource if exists
            liveSourceRef?.close()

            // Create new LiveSource
            val newSource = uk.adedamola.asciicast.streaming.LiveSource(wsUrl)
            liveSourceRef = newSource
            newSource
        },
        vtFactory = vtFactory,
        autoPlay = autoPlay
    )

    // Cleanup LiveSource on disposal
    DisposableEffect(Unit) {
        onDispose {
            liveSourceRef?.close()
        }
    }

    return state
}
