package my.maze.malaysia

/** Presentation only. Original OSM identifiers and tags stay in the local database/result metadata. */
fun accessDisplayName(name:String,details:String):String = when {
 name=="entrance yes" -> "Mapped entrance"
 name=="entrance exit" -> "Mapped exit"
 name.startsWith("entrance ") -> name.removePrefix("entrance ").replace('_',' ').replaceFirstChar{it.uppercase()}+" entrance"
 details.split(" · ").contains("motorway exit") && !name.contains("exit",ignoreCase=true) -> "Motorway exit · $name"
 else -> name
}
fun placeDetailsLabel(details:String)=details.split(" · ").filterNot{it.startsWith("OSM ")}.joinToString(" · ")
