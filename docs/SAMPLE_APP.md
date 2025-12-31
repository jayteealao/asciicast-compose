# Sample App

Demonstration application showing how to use the asciicast-compose library.

## Status

**🚧 UI Scaffold Complete** - Core screens implemented, wiring to player in progress.

## Features

### ✅ Implemented

1. **Main Activity**
   - Material3 theme integration
   - TabRow navigation between Recording and Live screens
   - Proper Compose UI structure

2. **Recording Player Screen**
   - File picker button (TODO: wire to SAF)
   - UI for loading `.cast` files
   - Player controls placeholder

3. **Live Player Screen**
   - WebSocket URL input field
   - Connect/Disconnect buttons
   - Connection status display

### 🚧 TODO (Wiring Required)

The UI exists but needs integration with the library:

#### Recording Player
```kotlin
// TODO in RecordingPlayerScreen.kt:

// 1. File Picker Integration
val filePickerLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { selectedFile = it }
}

Button(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
    Text("Open .cast file")
}

// 2. Player Setup
val player = remember {
    AsciinemaPlayer(
        virtualTerminal = AvtVirtualTerminal(),  // TODO: Use FakeTerminal until avt ready
        scope = rememberCoroutineScope()
    )
}

// 3. Load Recording
LaunchedEffect(selectedFileUri) {
    val inputStream = contentResolver.openInputStream(selectedFileUri) ?: return@LaunchedEffect
    val source = RecordingSource(inputStream)
    player.load(source)
}

// 4. Render Terminal
TerminalCanvas(
    frame = player.frame.collectAsState().value,
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
)

// 5. Player Controls
PlayerControlsBar(
    state = player.state.collectAsState().value,
    onPlay = { player.play() },
    onPause = { player.pause() },
    onStop = { player.stop() },
    onSpeedChange = { speed -> player.setSpeed(speed) }
)
```

#### Live Player
```kotlin
// TODO in LivePlayerScreen.kt:

// 1. WebSocket URL Validation
val isValidUrl = streamUrl.startsWith("wss://") || streamUrl.isNotBlank()

// 2. Construct URL from Token
val webSocketUrl = if (streamUrl.startsWith("wss://")) {
    streamUrl
} else {
    "wss://asciinema.org/ws/s/$streamUrl"
}

// 3. Player Setup
val player = remember {
    AsciinemaPlayer(
        virtualTerminal = AvtVirtualTerminal(),
        scope = rememberCoroutineScope()
    )
}

// 4. Connect to Stream
Button(onClick = {
    scope.launch {
        try {
            val source = LiveSource(webSocketUrl)
            player.load(source)
            player.play()
            isConnected = true
        } catch (e: Exception) {
            // Show error
        }
    }
}) {
    Text("Connect")
}

// 5. Render Terminal
TerminalCanvas(
    frame = player.frame.collectAsState().value,
    modifier = Modifier.fillMaxWidth().weight(1f)
)
```

## UI Components Needed

### PlayerControlsBar (Create New)

```kotlin
@Composable
fun PlayerControlsBar(
    state: PlayerState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var speed by remember { mutableStateOf(1f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        IconButton(onClick = {
            when (state) {
                is PlayerState.Playing -> onPause()
                is PlayerState.Paused, PlayerState.Idle -> onPlay()
                else -> {}
            }
        }) {
            Icon(
                imageVector = when (state) {
                    is PlayerState.Playing -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = "Play/Pause"
            )
        }

        // Stop button
        IconButton(onClick = onStop) {
            Icon(Icons.Default.Stop, "Stop")
        }

        // Speed control
        Column {
            Text("Speed: ${speed}x", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    onSpeedChange(it)
                },
                valueRange = 0.5f..3f,
                modifier = Modifier.width(120.dp)
            )
        }

        // State indicator
        when (state) {
            PlayerState.Idle -> Chip { Text("Idle") }
            PlayerState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            is PlayerState.Playing -> Chip { Text("Playing") }
            is PlayerState.Paused -> Chip { Text("Paused") }
            PlayerState.Ended -> Chip { Text("Ended") }
            is PlayerState.Error -> Chip(colors = ChipDefaults.chipColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Error")
            }
        }
    }
}

@Composable
fun Chip(
    colors: ChipColors = ChipDefaults.chipColors(),
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.containerColor
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            content()
        }
    }
}
```

### MarkerList (Create New)

```kotlin
@Composable
fun MarkerList(
    markers: List<Marker>,
    onMarkerClick: (Marker) -> Unit,
    modifier: Modifier = Modifier
) {
    if (markers.isEmpty()) return

    Column(modifier = modifier) {
        Text("Chapters", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        markers.forEach { marker ->
            OutlinedButton(
                onClick = { onMarkerClick(marker) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(marker.label)
                    Text(
                        formatTime(marker.timeMicros),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatTime(micros: Long): String {
    val seconds = micros / 1_000_000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}
```

## Permissions

Already configured in `AndroidManifest.xml`:
- ✅ `INTERNET` - For live streaming
- ✅ Storage access via SAF (no permission needed)

## Testing

### Manual Testing Steps

1. **Recording Playback**:
   ```bash
   # Put a .cast file in Downloads
   adb push demo.cast /sdcard/Download/

   # Launch app, go to Recording tab
   # Tap "Open .cast file"
   # Select demo.cast
   # Should start playing
   ```

2. **Live Streaming**:
   ```bash
   # Get a public stream token from asciinema.org
   # Launch app, go to Live tab
   # Enter token or full wss:// URL
   # Tap Connect
   # Should show live terminal output
   ```

### Test Files

Create test `.cast` files:

**Simple v2 test** (`simple-v2.cast`):
```json
{"version":2,"width":80,"height":24}
[0.0,"o","Hello, asciinema!\r\n"]
[1.0,"o","This is a test recording.\r\n"]
[2.0,"o","$ "]
```

**Simple v3 test** (`simple-v3.cast`):
```json
{"version":3,"term":{"cols":80,"rows":24}}
[0.0,"o","Hello from v3!\r\n"]
[0.5,"o","Using interval timing.\r\n"]
[1.0,"m","Chapter 1"]
[0.5,"o","$ "]
```

## Next Steps

1. Create `PlayerControlsBar.kt` composable
2. Create `MarkerList.kt` composable
3. Wire file picker to `RecordingPlayerScreen`
4. Wire `LiveSource` to `LivePlayerScreen`
5. Add error handling and loading states
6. Add player state persistence (ViewModel)
7. Add instrumented tests

## Known Issues

- TODO: Handle configuration changes (rotation)
- TODO: Background playback (Service)
- TODO: Notification controls for live streams
- TODO: Offline mode for recordings
- TODO: Export to video
