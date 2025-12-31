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
import kotlinx.coroutines.launch
import uk.adedamola.asciicast.player.AsciinemaPlayer
import uk.adedamola.asciicast.player.FakeTerminal
import uk.adedamola.asciicast.player.PlayerState
import uk.adedamola.asciicast.player.RecordingSource
import uk.adedamola.asciicast.renderer.TerminalCanvas
import uk.adedamola.asciicast.renderer.ScaleMode

/**
 * Screen for playing asciicast recordings from files.
 */
@Composable
fun RecordingPlayerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            errorMessage = null

            // Load the recording
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    if (inputStream != null) {
                        val source = RecordingSource(inputStream)
                        player.load(source)
                        player.play()
                    } else {
                        errorMessage = "Failed to open file"
                    }
                } catch (e: Exception) {
                    errorMessage = "Error loading file: ${e.message}"
                }
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "simple.cast" to "Simple",
                "markers.cast" to "Markers",
                "theme.cast" to "Theme",
                "resizing.cast" to "Resize"
            ).forEach { (filename, label) ->
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                errorMessage = null
                                val inputStream = context.assets.open("recordings/$filename")
                                val source = RecordingSource(inputStream)
                                player.load(source)
                                player.play()
                                selectedFileUri = Uri.parse("asset://recordings/$filename")
                            } catch (e: Exception) {
                                errorMessage = "Error loading sample: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
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

        // Terminal canvas
        if (selectedFileUri != null) {
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

            // Markers list
            if (markers.isNotEmpty()) {
                MarkerList(
                    markers = markers,
                    onMarkerClick = { marker ->
                        // TODO: Implement seeking when supported
                        // player.seekTo(marker.timeMicros)
                    }
                )
            }

            // Player controls
            PlayerControlsBar(
                state = state,
                onPlay = { player.play() },
                onPause = { player.pause() },
                onStop = {
                    player.stop()
                    selectedFileUri = null
                },
                onSpeedChange = { speed -> player.setSpeed(speed) }
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
