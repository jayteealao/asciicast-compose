# Compose API Design - AsciinemaPlayerState

## Design Goals

1. **Ergonomic**: ≤ 10 lines for simple use case
2. **Idiomatic**: Follow Compose state hoisting patterns
3. **Lifecycle-aware**: Automatic cleanup, no resource leaks
4. **Type-safe**: Clear separation between controller and state
5. **Testable**: Plain state holder class, separate from UI

---

## API Signatures

### 1. State Holder Class

```kotlin
package uk.adedamola.asciicast.renderer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import uk.adedamola.asciicast.player.AsciinemaPlayer
import uk.adedamola.asciicast.player.PlayerState
import uk.adedamola.asciicast.vt.TerminalFrame
import uk.adedamola.asciicast.player.Marker

/**
 * State holder for Asciinema player in Compose.
 *
 * This class manages the lifecycle of an [AsciinemaPlayer] instance and exposes
 * its state in a Compose-friendly way. It provides lifecycle-aware state collection
 * and automatic resource cleanup.
 *
 * Usage:
 * ```
 * val playerState = rememberAsciinemaPlayerState(
 *     sourceKey = castUri,
 *     source = { RecordingSource(contentResolver.openInputStream(castUri)!!) },
 *     autoPlay = true
 * )
 *
 * TerminalCanvas(frame = playerState.frame.value)
 * ```
 *
 * @property player The underlying player controller for play/pause/seek operations
 * @property frame Current terminal frame state
 * @property playbackState Current playback state (idle, loading, playing, paused, etc.)
 * @property markers Chapter markers in the recording
 */
@Stable
class AsciinemaPlayerState internal constructor(
    val player: AsciinemaPlayer,
    val frame: State<TerminalFrame>,
    val playbackState: State<PlayerState>,
    val markers: State<List<Marker>>
) {
    /**
     * Convenience methods that delegate to the player controller
     */
    fun play() = player.play()
    fun pause() = player.pause()
    fun stop() = player.stop()
    fun setSpeed(speed: Float) = player.setSpeed(speed)
    fun setIdleTimeLimit(seconds: Double?) = player.setIdleTimeLimit(seconds)
}
```

### 2. Core Remember API

```kotlin
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
 * @param sourceKey A key that uniquely identifies the source. When this changes,
 *   the current source is stopped and the new source is loaded.
 * @param source A lambda that creates the [PlaybackSource]. Called when [sourceKey] changes.
 * @param vtFactory Factory for creating the VirtualTerminal. Defaults to AvtVirtualTerminal.
 * @param autoPlay Whether to automatically start playback after loading. Default: true.
 * @param initialSpeed Initial playback speed multiplier. Default: 1.0f.
 * @param initialIdleTimeLimit Initial idle time compression limit in seconds. Default: null (no limit).
 * @return An [AsciinemaPlayerState] that manages the player lifecycle
 *
 * @sample
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
    vtFactory: () -> VirtualTerminal = { AvtVirtualTerminal() },
    autoPlay: Boolean = true,
    initialSpeed: Float = 1.0f,
    initialIdleTimeLimit: Double? = null
): AsciinemaPlayerState
```

### 3. Convenience APIs for Common Cases

```kotlin
/**
 * Remember a player state for a recording file.
 *
 * Convenience wrapper around [rememberAsciinemaPlayerState] for cast files.
 * Automatically handles InputStream creation and cleanup.
 *
 * @param uri URI of the .cast file
 * @param autoPlay Whether to start playing automatically
 * @param vtFactory VirtualTerminal factory
 * @param initialSpeed Initial playback speed
 * @param initialIdleTimeLimit Idle time compression limit in seconds
 */
@Composable
fun rememberRecordingPlayerState(
    uri: Uri,
    autoPlay: Boolean = true,
    vtFactory: () -> VirtualTerminal = { AvtVirtualTerminal() },
    initialSpeed: Float = 1.0f,
    initialIdleTimeLimit: Double? = null
): AsciinemaPlayerState {
    val context = LocalContext.current

    return rememberAsciinemaPlayerState(
        sourceKey = uri,
        source = {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open URI: $uri")
            RecordingSource(inputStream)
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
 * @param wsUrl WebSocket URL (e.g., "wss://asciinema.org/ws/s/TOKEN")
 * @param autoPlay Whether to start playing automatically
 * @param vtFactory VirtualTerminal factory
 */
@Composable
fun rememberLivePlayerState(
    wsUrl: String,
    autoPlay: Boolean = true,
    vtFactory: () -> VirtualTerminal = { AvtVirtualTerminal() }
): AsciinemaPlayerState {
    return rememberAsciinemaPlayerState(
        sourceKey = wsUrl,
        source = { LiveSource(wsUrl) },
        vtFactory = vtFactory,
        autoPlay = autoPlay
    )
}
```

### 4. Optional: All-in-One Composable

```kotlin
/**
 * A complete terminal player with built-in canvas rendering.
 *
 * This is the simplest API - an all-in-one composable that renders a terminal
 * and handles all player lifecycle automatically.
 *
 * @param uri URI of the .cast file to play
 * @param modifier Modifier for the canvas
 * @param autoPlay Whether to start playing automatically
 * @param scaleMode How to scale the terminal to fit
 * @param fontSize Font size in SP
 */
@Composable
fun AsciinemaTerminal(
    uri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    scaleMode: ScaleMode = ScaleMode.FitBoth,
    fontSize: Int = 14
) {
    val playerState = rememberRecordingPlayerState(uri, autoPlay)

    TerminalCanvas(
        frame = playerState.frame.value,
        modifier = modifier,
        fontSize = fontSize,
        scaleMode = scaleMode
    )
}
```

---

## Implementation Requirements

### Lifecycle Management

The `rememberAsciinemaPlayerState` implementation must:

1. **Create player once** using `remember { ... }`
2. **Load source** when `sourceKey` changes using `LaunchedEffect(sourceKey) { ... }`
3. **Auto-play** if enabled, within the LaunchedEffect
4. **Collect flows** lifecycle-aware using `collectAsStateWithLifecycle()`
5. **Cleanup resources** using `DisposableEffect(Unit) { onDispose { ... } }`

### Resource Ownership

- The state holder **owns** the player and VT
- The state holder **must close** player and VT on disposal
- If `source` lambda creates closeable resources (InputStream, WebSocket), they must be closed

### Recomposition Safety

- Player/VT creation must be in `remember { }` to survive recomposition
- `sourceKey` changes must **stop current playback** before loading new source
- Multiple rapid `sourceKey` changes must cancel previous loads (LaunchedEffect auto-cancels)

---

## Before/After Comparison

### Before (Manual Wiring)

```kotlin
@Composable
fun PlayRecording(castUri: Uri) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vt = remember { AvtVirtualTerminal() }
    val player = remember { AsciinemaPlayer(vt, scope) }
    val frame by player.frame.collectAsState()

    LaunchedEffect(castUri) {
        val inputStream = context.contentResolver.openInputStream(castUri)!!
        val source = RecordingSource(inputStream)
        player.load(source)
        player.play()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.close()
            vt.close()
        }
    }

    TerminalCanvas(frame = frame, modifier = Modifier.fillMaxSize())
}
```

**Issues:**
- 18 lines of code
- Manual scope management
- Manual Flow collection
- Manual cleanup
- No lifecycle-aware collection (wastes resources in background)
- InputStream never closed (LEAK!)

### After (Ergonomic API)

```kotlin
@Composable
fun PlayRecording(castUri: Uri) {
    val playerState = rememberRecordingPlayerState(castUri, autoPlay = true)

    TerminalCanvas(
        frame = playerState.frame.value,
        modifier = Modifier.fillMaxSize()
    )
}
```

**Improvements:**
- 7 lines of code (60% reduction)
- No manual scope management
- No manual Flow collection
- No manual cleanup
- Lifecycle-aware by default
- InputStream properly managed and closed

---

## Dependencies Required

```kotlin
// renderer-compose/build.gradle.kts
dependencies {
    // Existing
    api(project(":vt-api"))
    api(project(":player-core"))  // Add player-core

    // For lifecycle-aware collection
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Existing compose deps...
}
```

---

## Testing Strategy

1. **Unit test state holder**:
   - Verify player is created once
   - Verify load is called when sourceKey changes
   - Verify cleanup is called

2. **Compose runtime test** (if feasible):
   - Verify disposal when leaving composition
   - Verify recomposition doesn't recreate player
   - Verify sourceKey changes reload

3. **Manual test** in sample app:
   - Switch between recordings rapidly
   - Background app and verify no crashes
   - Check no resource leaks with profiler
