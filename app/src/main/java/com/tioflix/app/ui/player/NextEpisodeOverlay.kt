package com.tioflix.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NextEpisodeOverlay(
    prompt: NextEpisodePrompt,
    onPlayNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, tonalElevation = 12.dp, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Up next", style = MaterialTheme.typography.labelLarge)
            Text(
                "E${prompt.episode.episodeNumber} • ${prompt.episode.title}",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Playing in ${prompt.secondsRemaining} seconds")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPlayNext) { Text("Play Next") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}
