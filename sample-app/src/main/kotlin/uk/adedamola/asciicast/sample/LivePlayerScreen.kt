package uk.adedamola.asciicast.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.adedamola.asciicast.player.AsciinemaPlayer
import uk.adedamola.asciicast.player.FakeTerminal
import uk.adedamola.asciicast.player.PlayerState
import uk.adedamola.asciicast.renderer.ScaleMode
import uk.adedamola.asciicast.renderer.TerminalCanvas
import uk.adedamola.asciicast.streaming.LiveSource

/**
 * Screen for playing live asciinema streams.
 */
@Composable
fun LivePlayerScreen() {
    val scope = rememberCoroutineScope()

    var streamUrl by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var liveSource by remember { mutableStateOf<LiveSource?>(null) }

    // Create player with FakeTerminal (TODO: Replace with AvtVirtualTerminal when ready)
    val player = remember {
        AsciinemaPlayer(
            virtualTerminal = FakeTerminal(),
            scope = scope
        )
    }

    val state by player.state.collectAsState()
    val frame by player.frame.collectAsState()
    val markers by player.markers.collectAsState()

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            liveSource?.close()
            player.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Live Stream Player",
            style = MaterialTheme.typography.headlineMedium
        )

        // URL input
        OutlinedTextField(
            value = streamUrl,
            onValueChange = {
                streamUrl = it
                errorMessage = null
            },
            label = { Text("Stream URL or Token") },
            placeholder = { Text("wss://asciinema.org/ws/s/TOKEN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isConnected,
            supportingText = {
                Text("Enter full WebSocket URL or just the token")
            }
        )

        // Connect/Disconnect buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            errorMessage = null

                            // Construct WebSocket URL
                            val wsUrl = if (streamUrl.startsWith("wss://") || streamUrl.startsWith("ws://")) {
                                streamUrl
                            } else {
                                "wss://asciinema.org/ws/s/$streamUrl"
                            }

                            // Create and connect to live source
                            val source = LiveSource(wsUrl)
                            liveSource = source

                            player.load(source)
                            player.play()
                            isConnected = true
                        } catch (e: Exception) {
                            errorMessage = "Connection failed: ${e.message}"
                            isConnected = false
                        }
                    }
                },
                enabled = streamUrl.isNotBlank() && !isConnected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Connect")
            }

            OutlinedButton(
                onClick = {
                    liveSource?.close()
                    liveSource = null
                    player.stop()
                    isConnected = false
                    errorMessage = null
                },
                enabled = isConnected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Disconnect")
            }
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Connection status
        if (isConnected) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Connected to live stream",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Terminal canvas
        if (isConnected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                TerminalCanvas(
                    frame = frame,
                    modifier = Modifier.fillMaxSize(),
                    scaleMode = ScaleMode.FitBoth
                )
            }

            // Markers list (live streams can have markers too)
            if (markers.isNotEmpty()) {
                MarkerList(
                    markers = markers,
                    onMarkerClick = { marker ->
                        // Markers in live streams are informational only
                        // No seeking supported
                    }
                )
            }

            // Player state indicator
            PlayerControlsBar(
                state = state,
                onPlay = { player.play() },
                onPause = { player.pause() },
                onStop = {
                    liveSource?.close()
                    liveSource = null
                    player.stop()
                    isConnected = false
                },
                onSpeedChange = { /* Speed control not applicable to live streams */ }
            )
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Not connected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Enter a stream URL or token to connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
