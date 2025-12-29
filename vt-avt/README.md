# vt-avt: Rust avt Backend for Android

This module provides a high-performance VirtualTerminal implementation using the Rust [avt](https://github.com/asciinema/avt) library via JNI.

## Status

**SCAFFOLD ONLY** - Core structure is in place, but implementation requires:
1. Adding avt crate dependency
2. Implementing Rust wrapper logic
3. Implementing snapshot/diff encoding/decoding
4. Setting up NDK build automation

## Architecture

```
vt-avt/
├── rust/               # Rust library (cdylib for JNI)
│   ├── Cargo.toml     # Rust dependencies (TODO: add avt)
│   └── src/lib.rs     # JNI bridge functions
├── android/           # Kotlin JNI wrapper
│   └── src/main/kotlin/
│       └── ...avt/
│           ├── AvtNative.kt              # Native method declarations
│           └── AvtVirtualTerminal.kt     # VirtualTerminal implementation
└── build.gradle.kts   # Android library + Rust build tasks
```

## Building the Rust Library

### Prerequisites

1. **Rust toolchain**: Install from https://rustup.rs/
2. **cargo-ndk**: For Android NDK builds
   ```bash
   cargo install cargo-ndk
   ```
3. **Android NDK**: Install via Android Studio SDK Manager or standalone

### Build Steps

1. Navigate to `rust/` directory:
   ```bash
   cd vt-avt/rust
   ```

2. Build for Android targets:
   ```bash
   # Build for all supported ABIs
   cargo ndk \
     --target armeabi-v7a \
     --target arm64-v8a \
     --target x86_64 \
     --platform 26 \
     -- build --release
   ```

3. Copy `.so` files to `jniLibs`:
   ```bash
   cp target/armv7-linux-androideabi/release/libasciicast_vt_avt.so \
      ../android/src/main/jniLibs/armeabi-v7a/

   cp target/aarch64-linux-android/release/libasciicast_vt_avt.so \
      ../android/src/main/jniLibs/arm64-v8a/

   cp target/x86_64-linux-android/release/libasciicast_vt_avt.so \
      ../android/src/main/jniLibs/x86_64/
   ```

### Gradle Integration (TODO)

Add a Gradle task to automate Rust builds:

```kotlin
// In vt-avt/build.gradle.kts

tasks.register<Exec>("buildRustLibs") {
    workingDir = file("rust")

    commandLine(
        "cargo", "ndk",
        "--target", "armeabi-v7a",
        "--target", "arm64-v8a",
        "--target", "x86_64",
        "--platform", "26",
        "--", "build", "--release"
    )

    doLast {
        // Copy .so files to jniLibs
        // ...
    }
}

// Run before Android build
tasks.named("preBuild") {
    dependsOn("buildRustLibs")
}
```

## Implementation Guide

### Step 1: Add avt Dependency

In `rust/Cargo.toml`:
```toml
[dependencies]
avt = { version = "0.x", features = [...] }
jni = "0.21"
```

### Step 2: Implement AvtState Wrapper

In `rust/src/lib.rs`, create a wrapper around avt types:

```rust
use avt::{Parser, Vt};

struct AvtState {
    vt: Vt,
    parser: Parser,
    dirty_lines: HashSet<usize>,
}

impl AvtState {
    fn new(cols: usize, rows: usize) -> Self {
        // Initialize avt Vt and Parser
    }

    fn feed(&mut self, bytes: &[u8]) {
        // Parse bytes and update vt
        // Track dirty lines
    }

    fn encode_snapshot(&self) -> Vec<u8> {
        // Encode terminal state to compact binary format
        // See snapshot format in lib.rs comments
    }

    fn poll_diff(&mut self) -> Option<Vec<u8>> {
        // Encode dirty line indices
        // Clear dirty_lines after polling
    }
}
```

### Step 3: Implement Snapshot/Diff Encoding

Design a compact binary format to minimize JNI overhead:

**Snapshot Format:**
- Use varints for integers (LEB128)
- Pack style table (id -> foreground, background, attributes)
- Encode lines as runs (colStart, length, styleId, text)

**Diff Format:**
- List of dirty line indices
- Flags for cursor/resize changes

### Step 4: Implement Kotlin Decoders

In `AvtVirtualTerminal.kt`, implement:
- `decodeSnapshot()`: Parse binary snapshot into TerminalFrame
- `decodeDiff()`: Parse binary diff into TerminalDiff

### Step 5: Testing

Create tests:
- Unit tests for snapshot/diff encoding (Rust)
- Integration tests calling JNI functions (Kotlin instrumented tests)
- Verify correctness with known ANSI sequences

## Performance Notes

- **JNI overhead**: Minimize by using binary encoding (not JSON/protobuf)
- **Dirty tracking**: Only send changed lines to reduce data transfer
- **Batch updates**: Player should apply events in batches before snapshot
- **Memory**: avt uses efficient buffer representation

## References

- [avt GitHub](https://github.com/asciinema/avt)
- [cargo-ndk](https://github.com/bbqsrc/cargo-ndk)
- [JNI crate](https://docs.rs/jni/)
- [Android NDK Guide](https://developer.android.com/ndk/guides)
