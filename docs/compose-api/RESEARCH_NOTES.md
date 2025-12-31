# Compose API Redesign - Research Notes

## Research Date
December 31, 2025

## Objective
Research Jetpack Compose best practices for designing idiomatic, ergonomic state holder APIs to replace the current manual-wiring approach in asciicast-compose.

---

## 1. State Hoisting & State Holders

### Key References
- [Where to hoist state | Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/state-hoisting)
- [State and Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/state)
- [Best Practices for State Management in Compose | Medium](https://medium.com/@sixtinbydizora/best-practices-for-state-management-in-compose-931243e22517)

### Key Findings

**Definition**: State hoisting is moving state from a child composable to its caller to make it stateless and reusable.

**Best Practices**:
- Hoist to the **lowest common ancestor** - don't lift higher than needed
- For **complex UI logic** with multiple state fields, delegate to **state holder classes**
- State holders are **plain classes** that make composables testable and reduce complexity
- Follow **unidirectional data flow**: state flows down, events flow up
- Maintain **single source of truth** to avoid bugs

**When to Use State Holders**:
> "If the amount of state to keep track of increases, or the logic to perform in composable functions arises, it's a good practice to delegate the logic and state responsibilities to other classes: state holders."

**Key Principle**:
- Pass `State<T>` (immutable) and callbacks to modify, NOT `MutableState`
- Encapsulate multiple related state values into dedicated state holder classes

---

## 2. Side Effects & Lifecycle Management

### Key References
- [Side-effects in Compose | Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/side-effects)
- [Understanding Execution Order: DisposableEffect, LaunchedEffect | droidcon](https://www.droidcon.com/2025/04/22/understanding-execution-order-in-jetpack-compose-disposableeffect-launchedeffect-and-composables/)
- [Advanced State and Side Effects in Jetpack Compose | Android Developers](https://developer.android.com/codelabs/jetpack-compose-advanced-state-side-effects)

### Key Findings

**LaunchedEffect**:
- Used for **asynchronous operations** (coroutines)
- Launches when first composed, **auto-cancels** when composable leaves or keys change
- Takes **key arguments** - changing keys cancels old effect and starts new one
- **Async/scheduled** execution after synchronous effects

**DisposableEffect**:
- Used for **resource cleanup** requiring explicit registration/unregistration
- **Synchronous** - runs immediately after composition
- **MUST include `onDispose` clause** as final statement
- Perfect for: closing resources, unregistering listeners, releasing handles

**Key Patterns**:
```kotlin
// LaunchedEffect for suspend work
LaunchedEffect(key) {
    // Suspend operations
    // Auto-cancels on key change or leaving composition
}

// DisposableEffect for cleanup
DisposableEffect(key) {
    // Setup
    onDispose {
        // Cleanup - REQUIRED
    }
}
```

**Common Pitfalls**:
- Forgetting `onDispose` leaks resources
- Using constant keys when dependency can change
- Not using `rememberUpdatedState` for non-key variables in effect blocks

---

## 3. Lifecycle-Aware Flow Collection

### Key References
- [Consuming flows safely in Jetpack Compose | Android Developers Medium](https://medium.com/androiddevelopers/consuming-flows-safely-in-jetpack-compose-cde014d0d5a3)
- [Lifecycle-Aware Flow Collection in Jetpack Compose | Medium](https://medium.com/@aartisingla36/lifecycle-aware-flow-collection-in-jetpack-compose-part-2-of-lifecycle-kotlin-flow-best-7587214f08bb)
- [Why collectAsStateWithLifecycle Outperforms collectAsState | softAai](https://softaai.com/lifecycle-aware-state-in-compose-collectasstate/)

### Key Findings

**Best Practice**: Use `collectAsStateWithLifecycle()` instead of `collectAsState()`

**Why**:
- `collectAsState()` follows **Composition lifecycle** - keeps collecting even in background
- `collectAsStateWithLifecycle()` follows **Android lifecycle** - pauses when app is backgrounded
- **Resource management**: Saves CPU, battery, network when not needed
- **Performance**: Avoids wasted work and unnecessary recompositions

**Implementation**:
```kotlin
// Requires: androidx.lifecycle:lifecycle-runtime-compose
val state by flow.collectAsStateWithLifecycle()
```

**Default behavior**: Stops collecting when lifecycle is below `STARTED` state

**Google's Recommendation**:
> "For Android apps with Jetpack Compose, the recommended approach is to use the collectAsStateWithLifecycle API"

This is now the **official best practice**, not optional.

---

## 4. Jetpack Library API Patterns

### Key References
- [Getting started with Compose-based UI | Media3 Android Developers](https://developer.android.com/media/media3/ui/compose)
- [PagerState | API reference | Android Developers](https://developer.android.com/reference/kotlin/androidx/compose/foundation/pager/PagerState)
- [Pager in Compose | Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/layouts/pager)

### Key Findings

**Common Pattern**: `rememberXState()` returns a state holder object

**Example 1: Pager**
```kotlin
val pagerState = rememberPagerState(
    initialPage = 0,
    pageCount = { 10 }
)
HorizontalPager(state = pagerState) { page ->
    // Content
}
```

**Example 2: Media3**
```kotlin
// State holders for player controls
val playPauseState = rememberPlayPauseButtonState(player)
val progressState = rememberProgressStateWithTickInterval(player, tickIntervalMs)

// State holder classes like PlayPauseButtonState listen to player events
// and convert Player state into UI state
```

**Pattern Characteristics**:
- State holder is a **@Stable class**
- Created via `rememberXState()` function
- Accepts **configuration parameters** (initial state, callbacks, keys)
- Handles **lifecycle internally** (setup, updates, cleanup)
- Returns **immutable state** to composables
- Exposes **controller methods** for user actions (play, pause, seek)
- Survives **recomposition** via `remember`

**Key Insight**: State holders **abstract complexity** - users don't wire coroutines, flows, or lifecycle manually.

---

## Conclusions for asciicast-compose API Design

### What We Should Do

1. **Create State Holder Class**
   - `@Stable class AsciinemaPlayerState`
   - Internal: owns player, VT, flows, coroutine scope
   - Exposes: immutable frame state, playback state, controller methods

2. **Provide `rememberAsciinemaPlayerState()` API**
   - Accepts: source/sourceKey, VT factory, autoPlay, config
   - Handles: player creation, source loading, lifecycle, cleanup
   - Returns: `AsciinemaPlayerState` object

3. **Lifecycle Correctness**
   - Use `LaunchedEffect(sourceKey)` for load/play (auto-cancel on change)
   - Use `DisposableEffect(Unit)` for resource cleanup (player.close(), vt.close())
   - Use `collectAsStateWithLifecycle()` for frame Flow collection

4. **Ergonomic Convenience APIs**
   - `rememberRecordingPlayerState(uri: Uri, ...)`
   - `rememberLivePlayerState(wsUrl: String, ...)`
   - Optionally: `AsciinemaTerminal(...)` all-in-one composable

5. **Keep Low-Level API Intact**
   - Don't break `AsciinemaPlayer(vt, scope)` constructor
   - Don't break `player.frame` Flow API
   - Add ergonomic layer on top

### Target API Experience

```kotlin
@Composable
fun PlayRecording(uri: Uri) {
    val playerState = rememberAsciinemaPlayerState(
        sourceKey = uri,
        source = RecordingSource.fromUri(uri),
        autoPlay = true
    )

    TerminalCanvas(
        frame = playerState.frame,
        modifier = Modifier.fillMaxSize()
    )

    PlayerControls(playerState.player)
}
```

**Result**: ≤ 10 lines, no manual Flow collection, no manual cleanup, no scope wiring.

---

## Dependencies Required

```gradle
// For collectAsStateWithLifecycle
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
```

Already present in sample-app, may need to add to renderer-compose module.
