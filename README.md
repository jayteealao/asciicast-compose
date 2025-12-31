# Asciicast Compose

**A production-quality Android library for playing asciinema recordings and live streams.**

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Features

- 📼 **Asciicast playback**: Support for v2 and v3 formats (NDJSON)
- 🔴 **Live streaming**: Connect to asciinema servers via ALiS v1 over WebSocket
- 🎨 **Native rendering**: Jetpack Compose Canvas with efficient text-run rendering
- 🔧 **Pluggable backends**: Clean VT abstraction allows swapping terminal emulators
- ⚡ **High performance**: Rust-based avt backend via JNI (optional pure-JVM backends)
- 📦 **Library-first**: Designed for integration, not just a standalone app
- 🎯 **Ergonomic Compose API**: Idiomatic state holders with automatic lifecycle management

## Quick Start

### Play a Recording (Simple)

```kotlin
@Composable
fun PlayRecording(castUri: Uri) {
    val context = LocalContext.current

    val playerState = rememberRecordingPlayerState(
        context = context,
        uri = castUri,
        autoPlay = true
    )

    TerminalCanvas(
        frame = playerState.frame.value,
        modifier = Modifier.fillMaxSize()
    )
}
```

That's it! The ergonomic API handles:
- ✅ Player and VT creation
- ✅ InputStream management and cleanup
- ✅ Lifecycle-aware Flow collection (pauses when app is backgrounded)
- ✅ Automatic resource cleanup on disposal
- ✅ Source reloading when URI changes

### Play a Live Stream (Simple)

```kotlin
@Composable
fun PlayLiveStream(wsUrl: String) {
    val playerState = rememberLivePlayerState(
        wsUrl = wsUrl,
        autoPlay = true
    )

    TerminalCanvas(
        frame = playerState.frame.value,
        modifier = Modifier.fillMaxSize()
    )
}
```

### Player Controls

```kotlin
@Composable
fun PlayRecordingWithControls(castUri: Uri) {
    val context = LocalContext.current
    val playerState = rememberRecordingPlayerState(context, castUri)

    Column {
        TerminalCanvas(
            frame = playerState.frame.value,
            modifier = Modifier.weight(1f)
        )

        Row {
            Button(onClick = { playerState.play() }) { Text("Play") }
            Button(onClick = { playerState.pause() }) { Text("Pause") }
            Button(onClick = { playerState.setSpeed(2.0f) }) { Text("2x") }
        }
    }
}
```

### Advanced: Manual Wiring

For advanced use cases where you need full control:

```kotlin
@Composable
fun PlayRecordingManual(castUri: Uri) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vt = remember { AvtVirtualTerminal() }
    val player = remember { AsciinemaPlayer(vt, scope) }
    val frame by player.frame.collectAsStateWithLifecycle()

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

## API Reference

### State Holder

The `AsciinemaPlayerState` class is the main entry point for Compose apps:

```kotlin
@Stable
class AsciinemaPlayerState {
    val player: AsciinemaPlayer          // Controller for play/pause/seek
    val frame: State<TerminalFrame>      // Current terminal frame
    val playbackState: State<PlayerState> // Playback state (idle/loading/playing/etc)
    val markers: State<List<Marker>>     // Chapter markers

    fun play()
    fun pause()
    fun stop()
    fun setSpeed(speed: Float)
    fun setIdleTimeLimit(seconds: Double?)
}
```

### Remember Functions

```kotlin
// Core API - maximum flexibility
@Composable
fun rememberAsciinemaPlayerState(
    sourceKey: Any?,
    source: () -> PlaybackSource,
    vtFactory: () -> VirtualTerminal,
    autoPlay: Boolean = true,
    initialSpeed: Float = 1.0f,
    initialIdleTimeLimit: Double? = null
): AsciinemaPlayerState

// Convenience for recordings
@Composable
fun rememberRecordingPlayerState(
    context: Context,
    uri: Uri,
    autoPlay: Boolean = true,
    vtFactory: () -> VirtualTerminal = { FakeTerminal() },
    initialSpeed: Float = 1.0f,
    initialIdleTimeLimit: Double? = null
): AsciinemaPlayerState

// Convenience for live streams
@Composable
fun rememberLivePlayerState(
    wsUrl: String,
    autoPlay: Boolean = true,
    vtFactory: () -> VirtualTerminal = { FakeTerminal() }
): AsciinemaPlayerState
```

## Architecture

Clean modular design with pluggable VT backends:

- **vt-api**: Core abstractions (VirtualTerminal, Theme, TerminalFrame, TermEvent)
- **formats**: Asciicast v2/v3 parsers
- **streaming-alis**: ALiS v1 live streaming decoder
- **player-core**: Playback engine with timing, speed control, idle compression
- **renderer-compose**: Compose Canvas rendering + ergonomic state holders
- **vt-avt**: Rust avt backend via JNI (🚧 scaffold, needs implementation)
- **vt-jediterm**: JediTerm adapter stub (JVM-only)
- **vt-termux**: Termux adapter stub (Android, GPLv3)

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build vt-avt (requires Rust + cargo-ndk)
cd vt-avt/rust
cargo ndk --target arm64-v8a --platform 26 -- build --release
```

## Status

✅ **Complete**:
- VT abstraction and data models
- Asciicast v2/v3 parsers with tests
- ALiS v1 decoder with tests
- Player core with FakeTerminal tests
- Compose Canvas renderer
- Ergonomic Compose state holder API

🚧 **In Progress**:
- vt-avt Rust integration
- Sample app enhancements

📝 **Planned**:
- Seeking support
- Maven Central publishing

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md)
- [Compose API Design](docs/compose-api/API_DESIGN.md)
- [Compose API Research](docs/compose-api/RESEARCH_NOTES.md)
- [Adding VT Backends](docs/ADDING_VT_BACKEND.md)
- [Rendering Notes](docs/RENDERING_NOTES.md)
- [Building vt-avt](vt-avt/README.md)

## License

Apache 2.0 - See [LICENSE.txt](LICENSE.txt)

Copyright 2025 Adedamola Adeyemi
