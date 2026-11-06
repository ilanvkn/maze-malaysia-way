package my.maze.malaysia

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Real setup state, not a simulated map. Only this outer column scrolls. */
@Composable internal fun MazeWelcome(
    state: MazeState,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    onCommunity: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(painterResource(R.drawable.maze_icon), "Maze", Modifier.size(56.dp))
                Text("Selamat datang.", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                Text("Your Malaysia.\nOffline.", style = MaterialTheme.typography.headlineLarge)
                Text("Download once. Find places, plan a drive and keep your map with you when the connection drops.")
                Text("No sign-up. Location is optional.", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                state.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodySmall) }
                if (state.packChecking) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Checking your offline map…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                } else if (state.packProgress != null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Getting your map ready", style = MaterialTheme.typography.titleLarge)
                            LinearProgressIndicator(progress = { state.packProgress }, modifier = Modifier.fillMaxWidth())
                            Text(state.packStatus, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            Text("Keep Maze open while the file is checked. You can cancel and try again.", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel import") }
                        }
                    }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Get the Malaysia pack", style = MaterialTheme.typography.titleLarge)
                            Text("1  Download the Malaysia ZIP · 662 MB\n2  Choose that ZIP below · keep it zipped\n3  Search, save a trip, try airplane mode")
                            Text("Use Wi-Fi if you prefer. Allow at least 3 GB free while importing. The regional pack also includes Singapore and Brunei.", style = MaterialTheme.typography.bodySmall)
                            state.packError?.let { error ->
                                Surface(color = colors.errorContainer, shape = MaterialTheme.shapes.medium) {
                                    Text(error, Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite }, color = colors.onErrorContainer)
                                }
                            }
                            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Get Malaysia map") }
                            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Choose downloaded ZIP") }
                            Text("Download opens GitHub in your browser. Import checks the matching file before it becomes your map.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (state.savedTrips.isNotEmpty()) TextButton(onClick = onSaved, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open saved trips") }
                HorizontalDivider()
                Text("Peta kita. Bina bersama.", style = MaterialTheme.typography.titleMedium)
                Text("Maze is built in the open. Help with a place, a test, or a better word.")
                TextButton(onClick = onCommunity, modifier = Modifier.heightIn(min = 48.dp)) { Text("Meet the Maze community") }
                Text("Early preview · routes can detour or be unavailable. No live traffic or active navigation. © OpenStreetMap contributors · ODbL 1.0 · Geofabrik", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
