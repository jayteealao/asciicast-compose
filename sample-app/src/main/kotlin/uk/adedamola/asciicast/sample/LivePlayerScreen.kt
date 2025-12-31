package uk.adedamola.asciicast.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.adedamola.asciicast.renderer.ScaleMode
import uk.adedamola.asciicast.renderer.TerminalCanvas
import uk.adedamola.asciicast.renderer.rememberLivePlayerState

/**
 * Screen for playing live asciinema streams.
 *
 * Demonstrates the ergonomic Compose API for live stream playback.
 */
@Composable
fun LivePlayerScreen() {
    var streamUrl by remember { mutableStateOf("") }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Ergonomic player state - handles WebSocket lifecycle automatically
    val playerState = activeStreamUrl?.let { wsUrl ->
        try {
            rememberLivePlayerState(
                wsUrl = wsUrl,
                autoPlay = true
            )
        } catch (e: Exception) {
            errorMessage = "Connection failed: ${e.message}"
            null
        }
    }

    val isConnected = activeStreamUrl != null

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
                    try {
                        errorMessage = null

                        // Construct WebSocket URL
                        val wsUrl = if (streamUrl.startsWith("wss://") || streamUrl.startsWith("ws://")) {
                            streamUrl
                        } else {
                            "wss://asciinema.org/ws/s/$streamUrl"
                        }

                        activeStreamUrl = wsUrl
                    } catch (e: Exception) {
                        errorMessage = "Connection failed: ${e.message}"
                    }
                },
                enabled = streamUrl.isNotBlank() && !isConnected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Connect")
            }

            OutlinedButton(
                onClick = {
                    activeStreamUrl = null
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

        // Terminal canvas and controls
        if (playerState != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                TerminalCanvas(
                    frame = playerState.frame.value,
                    modifier = Modifier.fillMaxSize(),
                    scaleMode = ScaleMode.FitBoth
                )
            }

            // Markers list (live streams can have markers too)
            if (playerState.markers.value.isNotEmpty()) {
                MarkerList(
                    markers = playerState.markers.value,
                    onMarkerClick = { marker ->
                        // Markers in live streams are informational only
                        // No seeking supported
                    }
                )
            }

            // Player state indicator
            PlayerControlsBar(
                state = playerState.playbackState.value,
                onPlay = { playerState.play() },
                onPause = { playerState.pause() },
                onStop = {
                    playerState.stop()
                    activeStreamUrl = null
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
