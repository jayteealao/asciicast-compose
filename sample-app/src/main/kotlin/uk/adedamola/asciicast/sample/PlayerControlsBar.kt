package uk.adedamola.asciicast.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.adedamola.asciicast.player.PlayerState

/**
 * Player controls bar with play/pause/stop and speed control.
 *
 * @param state Current player state
 * @param onPlay Called when play button is clicked
 * @param onPause Called when pause button is clicked
 * @param onStop Called when stop button is clicked
 * @param onSpeedChange Called when speed slider changes
 * @param modifier Modifier for the controls bar
 */
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button
            IconButton(
                onClick = {
                    when (state) {
                        is PlayerState.Playing -> onPause()
                        is PlayerState.Paused, PlayerState.Idle -> onPlay()
                        else -> {}
                    }
                },
                enabled = state !is PlayerState.Loading && state !is PlayerState.Error
            ) {
                Icon(
                    imageVector = when (state) {
                        is PlayerState.Playing -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = if (state is PlayerState.Playing) "Pause" else "Play"
                )
            }

            // Stop button
            IconButton(
                onClick = onStop,
                enabled = state !is PlayerState.Idle && state !is PlayerState.Loading
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            }

            // Speed control
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(150.dp)
            ) {
                Text(
                    "Speed: ${String.format("%.1f", speed)}x",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        onSpeedChange(it)
                    },
                    valueRange = 0.5f..3f,
                    steps = 4, // 0.5, 1.0, 1.5, 2.0, 2.5, 3.0
                    enabled = state is PlayerState.Playing || state is PlayerState.Paused
                )
            }

            // State indicator
            StateChip(state = state)
        }
    }
}

/**
 * Chip showing current player state.
 */
@Composable
private fun StateChip(state: PlayerState) {
    val (text, color) = when (state) {
        PlayerState.Idle -> "Idle" to MaterialTheme.colorScheme.surfaceVariant
        PlayerState.Loading -> "Loading" to MaterialTheme.colorScheme.primary
        is PlayerState.Playing -> "Playing" to MaterialTheme.colorScheme.primary
        is PlayerState.Paused -> "Paused" to MaterialTheme.colorScheme.secondary
        PlayerState.Ended -> "Ended" to MaterialTheme.colorScheme.tertiary
        is PlayerState.Error -> "Error" to MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state is PlayerState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
