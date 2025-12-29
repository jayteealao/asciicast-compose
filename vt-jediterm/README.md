# JediTerm Virtual Terminal Backend (Stub)

This module provides a VirtualTerminal implementation using JetBrains JediTerm.

## Status

**NOT YET IMPLEMENTED** - This is a stub module with TODOs.

## Purpose

JediTerm is a pure-JVM terminal emulator used in IntelliJ IDEA's built-in terminal. It provides excellent VT100/xterm compatibility and is well-tested in production.

## Use Cases

- Desktop applications (JVM)
- Server-side rendering
- Development/testing environments

## NOT Suitable For

- Android apps (requires Swing/AWT)

## Implementation Guide

To implement this backend:

1. Add dependency in `build.gradle.kts`:
   ```kotlin
   implementation("org.jetbrains.jediterm:jediterm-pty:x.y.z")
   ```

2. Implement `JediTermVirtualTerminal.kt`:
   - Create `JediTerminal` instance in `reset()`
   - Wire up data stream to `feed()` methods
   - Convert `TerminalTextBuffer` to `TerminalFrame` in `snapshot()`
   - Optionally track dirty regions for `pollDiff()`

3. Handle threading:
   - JediTerm may require operations on specific threads
   - Consider using a dedicated executor or coroutine dispatcher

4. Map JediTerm styles to `CellStyle`:
   - Extract colors (TextStyle foreground/background)
   - Map attributes (bold, italic, etc.)

## References

- [JediTerm GitHub](https://github.com/JetBrains/jediterm)
- [VirtualTerminal API](../vt-api/)
