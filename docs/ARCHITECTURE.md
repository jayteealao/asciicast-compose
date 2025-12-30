# Architecture Guide

## Overview

asciicast-compose is designed as a **library-first** Android project with clean module boundaries and a pluggable VT (Virtual Terminal) abstraction layer.

## Design Principles

1. **Separation of Concerns**: Each module has a single, well-defined responsibility
2. **Pluggable Backends**: VT abstraction allows swapping terminal emulators
3. **Testability**: All modules are unit-testable with minimal dependencies
4. **Performance**: Efficient rendering via text runs (not per-cell drawing)
5. **Android-First**: Optimized for mobile, but core modules are pure JVM/Kotlin

## Module Dependency Graph

```
sample-app
    ├─> player-core
    │   ├─> vt-api
    │   ├─> formats
    │   └─> streaming-alis
    ├─> renderer-compose -> vt-api
    └─> vt-avt -> vt-api

Optional:
    vt-jediterm -> vt-api
    vt-termux -> vt-api
```

## Module Details

### vt-api (Kotlin JVM)

**Purpose**: Core abstraction layer.

**Exports**:
- `VirtualTerminal` interface
- Data models: `Theme`, `Color`, `CellStyle`, `TerminalFrame`, `TerminalDiff`
- Events: `TermEvent` (Init, Output, Resize, Marker, Exit, Eot)
- `TermCapabilities` flags

**Key Design**:
- Backend-agnostic: No knowledge of avt, JediTerm, or Termux
- Immutable data models suitable for Compose State
- Diff support optional (backends can return null)

### formats (Kotlin JVM)

**Purpose**: Parse asciicast v2 and v3 files.

**Key Classes**:
- `AsciicastParser`: Streaming NDJSON parser
- `AsciicastHeader`: Metadata (width, height, title, etc.)
- `RecordingSource`: Implements `PlaybackSource` from player-core

**Normalization**:
- Converts v2 absolute times → deltas
- Converts v3 interval times → deltas
- Parses resize strings ("80x24")
- Ignores unknown event codes (graceful degradation)

**Testing**: Full coverage of v2/v3 parsing, edge cases, unknown codes.

### streaming-alis (Kotlin JVM + OkHttp)

**Purpose**: Decode ALiS v1 binary protocol for live streams.

**Key Classes**:
- `LEB128`: Variable-length integer encoding/decoding
- `AlisDecoder`: Binary event decoder
- `AlisEvent`: Typed events (Init, Output, Resize, Marker, Exit, Eot)
- `LiveSource`: WebSocket client implementing `PlaybackSource`

**Protocol**:
- Magic: "ALiS\x01"
- Events: `[EventType byte][LEB128 payload...]`
- Strings: `[LEB128 length][UTF-8 bytes]`
- Theme encoding: variant (0=none, 1=8-color, 2=16-color)

**Testing**: LEB128 round-trips, event decoding with spec examples.

### player-core (Kotlin JVM + Coroutines)

**Purpose**: Playback engine.

**Key Classes**:
- `AsciinemaPlayer`: Main API, manages playback state
- `PlaybackSource`: Abstraction for recordings and live streams
- `PlayerState`: Idle, Loading, Playing, Paused, Ended, Error
- `Marker`: Chapter/annotation tracking

**Features**:
- Speed control (multiplier)
- Idle time compression (clamp long pauses)
- Marker collection
- StateFlow<TerminalFrame> for Compose integration
- Coroutine-based timing (delay for event deltas)

**Testing**: FakeTerminal records all operations, tests verify timing and state transitions.

### renderer-compose (Android Library + Compose)

**Purpose**: Draw `TerminalFrame` on Compose Canvas.

**Key Classes**:
- `TerminalCanvas`: Composable that renders frames
- `ScaleMode`: FitWidth, FitHeight, FitBoth, None

**Rendering Strategy**:
1. Measure cell dimensions using `TextMeasurer` ("M" character)
2. Compute scale factor based on canvas size and grid dimensions
3. Draw background rectangles per text run (not per cell)
4. Draw text runs using `drawText` (efficient batching)
5. Draw cursor as underline rectangle

**Performance**:
- No per-cell drawing (uses TextRun grouping)
- Background rects only for non-default colors
- TextMeasurer cached per font size change

**Testing**: Minimal instrumented tests (visual verification), focus on unit-testable logic.

### vt-avt (Android Library + Rust)

**Purpose**: High-performance VT backend using Rust avt via JNI.

**Architecture**:
```
Kotlin (AvtVirtualTerminal)
    ↓ JNI calls
Rust (lib.rs: vtNew, vtFeed, vtSnapshot, etc.)
    ↓ uses
avt crate (Parser, Vt)
```

**Data Flow**:
1. Kotlin calls `AvtNative.vtFeed(bytes)`
2. Rust feeds bytes to avt Parser → Vt
3. Kotlin calls `AvtNative.vtSnapshot()`
4. Rust encodes terminal state to compact binary
5. Kotlin decodes binary → `TerminalFrame`

**Snapshot Format** (binary):
- Varints for integers (LEB128)
- Style table (id → fg/bg/attrs)
- Lines as runs (colStart, textLen, styleId, textBytes)

**Status**: Scaffold complete, needs avt integration (see vt-avt/README.md).

### vt-jediterm (Stub)

**Purpose**: JVM-only backend using JetBrains JediTerm.

**Status**: Stub with TODO scaffolding.

**Use Case**: Desktop apps, server-side rendering.

**Notes**: Requires Swing/AWT (not Android-compatible).

### vt-termux (Stub)

**Purpose**: Android backend using Termux terminal-emulator.

**Status**: Stub with TODO scaffolding.

**Licensing**: GPLv3 (affects application licensing).

**Use Case**: Lightweight Android alternative to vt-avt.

### sample-app (Android Application)

**Purpose**: Demo app showing recording and live playback.

**Screens**:
- Recording Player: File picker → play .cast
- Live Player: WebSocket URL input → connect and play

**Status**: UI scaffold, needs wiring to player + renderer.

## Data Flow

### Recording Playback

```
.cast file → RecordingSource
    → AsciicastParser
    → TimedTermEvent stream
    → AsciinemaPlayer
    → feed to VirtualTerminal
    → snapshot() → TerminalFrame
    → TerminalCanvas → rendered
```

### Live Streaming

```
WebSocket → LiveSource
    → AlisDecoder
    → TimedTermEvent stream
    → AsciinemaPlayer
    → feed to VirtualTerminal
    → snapshot() → TerminalFrame
    → TerminalCanvas → rendered
```

## Key Abstraction: VirtualTerminal

The VT interface is the heart of the architecture. It decouples:

- **Parser/Player** (what events to apply)
- **Emulator** (how to interpret ANSI sequences)
- **Renderer** (how to draw the result)

### Contract

```kotlin
interface VirtualTerminal {
    fun reset(cols, rows, theme?, initData?)
    fun resize(cols, rows)
    fun feedUtf8(text: String)
    fun feed(bytes: ByteArray)
    fun snapshot(): TerminalFrame
    fun pollDiff(): TerminalDiff?
    fun close()
}
```

### Why It Matters

- **Testability**: Player tests use FakeTerminal
- **Performance**: Swap backends without changing player/renderer
- **Platform**: avt (Android/Rust), JediTerm (JVM), Termux (Android)
- **Future**: Add new backends (e.g., pure-Kotlin parser, libvterm wrapper)

## Thread Safety

- **VirtualTerminal**: NOT thread-safe, call from single dispatcher
- **AsciinemaPlayer**: Uses CoroutineScope, serializes VT calls
- **TerminalFrame**: Immutable, safe to share across threads
- **Renderer**: Reads StateFlow<TerminalFrame>, no locking needed

## Performance Considerations

1. **Rendering**: Text runs (not per-cell) reduce draw calls
2. **Diffs**: Optional pollDiff() for partial updates
3. **JNI**: Binary encoding minimizes overhead (avt)
4. **Idle Compression**: Clamp long pauses to avoid waiting
5. **Frame Throttling**: TODO max 60fps to avoid overdraw

## Error Handling

- **Unknown events**: Ignored gracefully
- **Invalid files**: AsciicastParser throws IllegalArgumentException
- **Network errors**: LiveSource surfaces via PlayerState.Error
- **VT errors**: Backend-specific (avt: Rust panics → JNI exception)

## Testing Strategy

| Module | Tests |
|--------|-------|
| vt-api | Data model tests (color resolution, theme) |
| formats | Parser tests (v2/v3, unknown codes, edge cases) |
| streaming-alis | LEB128 round-trips, ALiS decoding |
| player-core | FakeTerminal tests (timing, state, markers) |
| renderer-compose | Minimal instrumented tests |
| vt-avt | Rust unit tests + JNI integration tests |

## Future Enhancements

- **Seeking**: Requires event indexing or replay
- **Input**: Handle keyboard/mouse for interactive sessions
- **Recording**: Producer implementation (not just consumer)
- **Export**: Render to video (frame sequence → MP4)
- **Diff Rendering**: Optimize recomposition with TerminalDiff
