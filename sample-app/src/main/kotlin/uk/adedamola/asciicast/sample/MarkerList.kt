package uk.adedamola.asciicast.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.adedamola.asciicast.player.Marker

/**
 * List of markers/chapters in the recording.
 *
 * @param markers List of markers to display
 * @param onMarkerClick Called when a marker is clicked
 * @param modifier Modifier for the list
 */
@Composable
fun MarkerList(
    markers: List<Marker>,
    onMarkerClick: (Marker) -> Unit,
    modifier: Modifier = Modifier
) {
    if (markers.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Chapters",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            markers.forEach { marker ->
                OutlinedButton(
                    onClick = { onMarkerClick(marker) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            marker.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatTime(marker.timeMicros),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format microseconds as MM:SS time string.
 */
private fun formatTime(micros: Long): String {
    val seconds = micros / 1_000_000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}
