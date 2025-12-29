package uk.adedamola.asciicast.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Screen for playing asciicast recordings.
 *
 * TODO: Implement file picker, player controls, and terminal rendering.
 */
@Composable
fun RecordingPlayerScreen() {
    var selectedFile by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Recording Player",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = {
            // TODO: Launch file picker (SAF)
            // val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            //     type = "*/*"
            //     addCategory(Intent.CATEGORY_OPENABLE)
            // }
            // startActivityForResult(intent, REQUEST_CODE_OPEN_FILE)
        }) {
            Text("Open .cast file")
        }

        selectedFile?.let { file ->
            Text("Selected: $file")

            // TODO: Create AsciinemaPlayer instance
            // val player = remember {
            //     AsciinemaPlayer(
            //         virtualTerminal = AvtVirtualTerminal(),
            //         scope = rememberCoroutineScope()
            //     )
            // }
            //
            // LaunchedEffect(file) {
            //     val source = RecordingSource(...)
            //     player.load(source)
            // }
            //
            // TerminalCanvas(
            //     frame = player.frame.collectAsState().value,
            //     modifier = Modifier
            //         .fillMaxWidth()
            //         .weight(1f)
            // )
            //
            // PlayerControls(player = player)
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No file selected")
            }
        }
    }
}
