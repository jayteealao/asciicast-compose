# Termux Virtual Terminal Backend (Stub)

This module provides a VirtualTerminal implementation using Termux's terminal-emulator library.

## Status

**NOT YET IMPLEMENTED** - This is a stub module with TODOs.

## Purpose

The Termux terminal-emulator is a lightweight, Android-optimized VT100 emulator extracted from the Termux app. It's battle-tested in production and optimized for mobile devices.

## Use Cases

- Android apps
- Lightweight terminal rendering
- Mobile/embedded environments

## ⚠️ IMPORTANT: Licensing

**terminal-emulator is licensed under GPLv3.**

If you use this backend, your application must comply with GPLv3 licensing requirements, which may require making your app open-source.

**Before implementing:**
- Review GPLv3 implications for your project
- Consider legal review if building proprietary software
- Alternative: Use the avt backend (permissive license) or JediTerm (Apache 2.0)

## Implementation Guide

To implement this backend:

1. Add dependency in `build.gradle.kts`:
   ```kotlin
   implementation("com.termux:terminal-emulator:x.y.z")
   ```
   Check [Termux releases](https://github.com/termux/termux-app/releases) for current version.

2. Implement `TermuxVirtualTerminal.kt`:
   - Create `TerminalEmulator` in `reset()`
   - Feed bytes using `append(byte[], int, int)`
   - Read `TerminalBuffer.mScreen` for `snapshot()`
   - Use `mDirtyLines` for efficient `pollDiff()`

3. Map Termux's color/style system:
   - Extract style from `TerminalRow` cells
   - Convert to `CellStyle` (foreground, background, attributes)
   - Handle color palette mapping

4. Lifecycle management:
   - Properly dispose resources in `close()`
   - Consider Android lifecycle (configuration changes, backgrounding)

## Performance Notes

- Termux provides dirty line tracking → efficient `pollDiff()` implementation
- Lightweight memory footprint compared to JediTerm
- Optimized for mobile CPUs

## References

- [Termux terminal-emulator](https://github.com/termux/termux-app/tree/master/terminal-emulator)
- [GPLv3 License](https://www.gnu.org/licenses/gpl-3.0.en.html)
- [VirtualTerminal API](../vt-api/)
