package uk.adedamola.asciicast.sample

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import uk.adedamola.asciicast.renderer.TerminalCanvas
import uk.adedamola.asciicast.renderer.ScaleMode
import uk.adedamola.asciicast.renderer.rememberRecordingPlayerState

/**
 * Screen for playing asciicast recordings from files.
 *
 * Demonstrates the ergonomic Compose API for asciinema playback.
 */
@Composable
fun RecordingPlayerScreen() {
    android.util.Log.d("RecordingPlayer", "RecordingPlayerScreen composable called")
    val context = LocalContext.current

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            errorMessage = null
        }
    }

    // Ergonomic player state - handles lifecycle, cleanup, and Flow collection automatically
    val playerState = selectedFileUri?.let { uri ->
        android.util.Log.d("RecordingPlayer", "Creating player state for URI: $uri")
        rememberRecordingPlayerState(
            context = context,
            uri = uri,
            autoPlay = true
        )
    }

    LaunchedEffect(playerState) {
        android.util.Log.d("RecordingPlayer", "LaunchedEffect triggered, playerState: $playerState")
        if (playerState != null) {
            try {
                // Player state initialization happens here
                android.util.Log.d("RecordingPlayer", "Player state initialized successfully")
            } catch (e: Exception) {
                errorMessage = "Error loading file: ${e.message}"
                android.util.Log.e("RecordingPlayer", "Error in LaunchedEffect", e)
            }
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
            text = "Recording Player",
            style = MaterialTheme.typography.headlineMedium
        )

        // File selection
        Button(
            onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open .cast file from device")
        }

        // Sample files section
        Text(
            text = "Or try a sample recording:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Helper function to load a sample
        val loadSample: (String) -> Unit = { filename ->
            try {
                errorMessage = null
                selectedFileUri = Uri.parse("asset:///recordings/$filename")
                android.util.Log.d("RecordingPlayer", "Loading sample: $filename, URI: $selectedFileUri")
            } catch (e: Exception) {
                errorMessage = "Error loading sample: ${e.message}"
                android.util.Log.e("RecordingPlayer", "Error loading sample", e)
            }
        }

        // All sample files in a grid layout
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "simple.cast" to "Simple",
                    "theme.cast" to "Theme",
                    "input.cast" to "Input"
                ).forEach { (filename, label) ->
                    OutlinedButton(
                        onClick = {
                            android.util.Log.d("RecordingPlayer", "Button clicked: $label")
                            loadSample(filename)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "markers.cast" to "Markers",
                    "markers-input.cast" to "Mkrs+Input",
                    "resizing.cast" to "Resize"
                ).forEach { (filename, label) ->
                    OutlinedButton(
                        onClick = {
                            android.util.Log.d("RecordingPlayer", "Button clicked: $label")
                            loadSample(filename)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "loop.cast" to "Loop",
                    "long.cast" to "Long",
                    "bold-inverse-indexed.cast" to "Bold/Inv"
                ).forEach { (filename, label) ->
                    OutlinedButton(
                        onClick = {
                            android.util.Log.d("RecordingPlayer", "Button clicked: $label")
                            loadSample(filename)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Show selected file
        selectedFileUri?.let { uri ->
            Text(
                text = "Playing: ${uri.lastPathSegment ?: uri.path?.substringAfterLast('/') ?: "sample"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        // Terminal canvas and controls
        if (playerState != null) {
            val currentFrame = playerState.frame.value
            androidx.compose.runtime.SideEffect {
                android.util.Log.d("RecordingPlayer", "Frame update: $currentFrame")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                TerminalCanvas(
                    frame = currentFrame,
                    modifier = Modifier.fillMaxSize(),
                    scaleMode = ScaleMode.FitBoth
                )
            }

            // Markers list
            if (playerState.markers.value.isNotEmpty()) {
                MarkerList(
                    markers = playerState.markers.value,
                    onMarkerClick = { marker ->
                        // TODO: Implement seeking when supported
                        // playerState.seekTo(marker.timeMicros)
                    }
                )
            }

            // Player controls
            PlayerControlsBar(
                state = playerState.playbackState.value,
                onPlay = { playerState.play() },
                onPause = { playerState.pause() },
                onStop = {
                    playerState.stop()
                    selectedFileUri = null
                },
                onSpeedChange = { speed -> playerState.setSpeed(speed) }
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
                        "No file selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Select a sample recording or open a .cast file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
