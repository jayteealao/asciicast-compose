package uk.adedamola.asciicast.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Screen for playing live asciinema streams.
 *
 * TODO: Implement WebSocket connection, player controls, and terminal rendering.
 */
@Composable
fun LivePlayerScreen() {
    var streamUrl by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Live Stream Player",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = streamUrl,
            onValueChange = { streamUrl = it },
            label = { Text("Stream URL or Token") },
            placeholder = { Text("wss://asciinema.org/ws/s/TOKEN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                // TODO: Connect to live stream
                // val url = if (streamUrl.startsWith("wss://")) {
                //     streamUrl
                // } else {
                //     "wss://asciinema.org/ws/s/$streamUrl"
                // }
                //
                // val source = LiveSource(url)
                // player.load(source)
                // player.play()
                // isConnected = true
            },
            enabled = streamUrl.isNotBlank() && !isConnected
        ) {
            Text("Connect")
        }

        if (isConnected) {
            Button(onClick = {
                // TODO: Disconnect from stream
                // player.stop()
                // source.close()
                // isConnected = false
            }) {
                Text("Disconnect")
            }

            // TODO: Show terminal and status
            // TerminalCanvas(
            //     frame = player.frame.collectAsState().value,
            //     modifier = Modifier
            //         .fillMaxWidth()
            //         .weight(1f)
            // )
            //
            // ConnectionStatus(state = player.state.collectAsState().value)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Not connected")
            }
        }
    }
}
