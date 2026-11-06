package my.maze.malaysia

import java.net.URLEncoder

internal const val MAZE_HOME = "https://github.com/ilanvkn/maze-malaysia-way"
internal const val MAZE_DISCUSSIONS = "$MAZE_HOME/discussions/4"
internal const val MAZE_PACK_DOWNLOAD = "$MAZE_HOME/releases/download/v0.4.0-community.1/Maze-Malaysia-offline-2026-11-04-r2.zip"
internal const val MAZE_INVITATION = "Let's crack the maze code.\n\nMaze is an open-source Malaysia map project. Download the offline preview, help improve a place, or build with us.\n\nPeta kita. Bina bersama.\nhttps://github.com/ilanvkn/maze-malaysia-way"

/** Only an allowlisted public OSM identity is eligible for prefill. No names/GPS/trips. */
internal fun publicOsmLink(placeId: String): String? {
    val match = Regex("osm-(node|way)/(\\d{1,19})").matchEntire(placeId) ?: return null
    val id = match.groupValues[2].toLongOrNull()?.takeIf { it > 0 } ?: return null
    return "https://www.openstreetmap.org/${match.groupValues[1]}/$id"
}
internal fun placeReportUrl(placeId: String, appVersion: String): String {
    val fields = linkedMapOf("template" to "data.yml")
    // Limit version to machine metadata; callers cannot inject free-form report text.
    if (appVersion.matches(Regex("[A-Za-z0-9._-]{1,60}"))) fields["environment"] = "Maze $appVersion; add your pack date before posting."
    publicOsmLink(placeId)?.let { fields["place"] = it }
    return "$MAZE_HOME/issues/new?" + fields.entries.joinToString("&") {
        URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
    }
}
