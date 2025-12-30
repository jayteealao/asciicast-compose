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

## Quick Start

### Play a Recording

```kotlin
@Composable
fun PlayRecording(castUri: Uri) {
    val scope = rememberCoroutineScope()
    val vt = remember { AvtVirtualTerminal() }
    val player = remember { AsciinemaPlayer(vt, scope) }
    val frame by player.frame.collectAsState()

    LaunchedEffect(castUri) {
        val source = RecordingSource(contentResolver.openInputStream(castUri)!!)
        player.load(source)
        player.play()
    }

    TerminalCanvas(frame = frame, modifier = Modifier.fillMaxSize())
}
```

### Play a Live Stream

```kotlin
val source = LiveSource("wss://asciinema.org/ws/s/YOUR_TOKEN")
player.load(source)
player.play()
```

## Architecture

Clean modular design with pluggable VT backends:

- **vt-api**: Core abstractions (VirtualTerminal, Theme, TerminalFrame, TermEvent)
- **formats**: Asciicast v2/v3 parsers
- **streaming-alis**: ALiS v1 live streaming decoder
- **player-core**: Playback engine with timing, speed control, idle compression
- **renderer-compose**: Compose Canvas terminal rendering
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

🚧 **In Progress**:
- vt-avt Rust integration
- Sample app UI wiring

📝 **Planned**:
- Seeking support
- Maven Central publishing
- CI/CD pipeline

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md)
- [Adding VT Backends](docs/ADDING_VT_BACKEND.md)
- [Rendering Notes](docs/RENDERING_NOTES.md)
- [Building vt-avt](vt-avt/README.md)

## License

Apache 2.0 - See [LICENSE.txt](LICENSE.txt)

Copyright 2025 Adedamola Adeyemi
